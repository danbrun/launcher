package link.danb.launcher

import android.appwidget.AppWidgetHost
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.compose.ui.platform.ComposeView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.util.Consumer
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import link.danb.launcher.activities.ActivityDetailsDialogFragment
import link.danb.launcher.activities.ActivityManager
import link.danb.launcher.components.UserShortcut
import link.danb.launcher.database.ActivityData
import link.danb.launcher.database.WidgetData
import link.danb.launcher.extensions.boundsOnScreen
import link.danb.launcher.extensions.makeScaleUpAnimation
import link.danb.launcher.extensions.setSpanSizeProvider
import link.danb.launcher.gestures.GestureContract
import link.danb.launcher.gestures.GestureIconView
import link.danb.launcher.profiles.ProfilesModel
import link.danb.launcher.profiles.WorkProfileManager
import link.danb.launcher.shortcuts.ShortcutManager
import link.danb.launcher.tiles.TileViewItem
import link.danb.launcher.tiles.TransparentTileViewBinder
import link.danb.launcher.tiles.TransparentTileViewHolder
import link.danb.launcher.ui.DynamicGridLayoutManager
import link.danb.launcher.ui.GroupHeaderViewBinder
import link.danb.launcher.ui.GroupHeaderViewItem
import link.danb.launcher.ui.ViewBinderAdapter
import link.danb.launcher.ui.theme.LauncherTheme
import link.danb.launcher.widgets.AppWidgetViewProvider
import link.danb.launcher.widgets.WidgetEditorViewBinder
import link.danb.launcher.widgets.WidgetEditorViewItem
import link.danb.launcher.widgets.WidgetManager
import link.danb.launcher.widgets.WidgetSizeUtil
import link.danb.launcher.widgets.WidgetViewBinder
import link.danb.launcher.widgets.WidgetViewItem
import link.danb.launcher.widgets.WidgetsViewModel

@AndroidEntryPoint
class LauncherFragment : Fragment() {

  private val launcherViewModel: LauncherViewModel by activityViewModels()
  private val widgetsViewModel: WidgetsViewModel by activityViewModels()

  @Inject lateinit var activityManager: ActivityManager
  @Inject lateinit var appWidgetHost: AppWidgetHost
  @Inject lateinit var appWidgetViewProvider: AppWidgetViewProvider
  @Inject lateinit var profilesModel: ProfilesModel
  @Inject lateinit var shortcutManager: ShortcutManager
  @Inject lateinit var widgetManager: WidgetManager
  @Inject lateinit var widgetSizeUtil: WidgetSizeUtil
  @Inject lateinit var workProfileManager: WorkProfileManager

  private lateinit var recyclerView: RecyclerView
  private lateinit var gestureIconView: GestureIconView

  private val recyclerAdapter: ViewBinderAdapter by lazy {
    ViewBinderAdapter(
      GroupHeaderViewBinder(),
      TransparentTileViewBinder(this::onTileClick) { _, it -> onTileLongClick(it) },
      WidgetViewBinder(appWidgetViewProvider) { widgetManager.isInEditMode.value = true },
      WidgetEditorViewBinder(
        appWidgetViewProvider,
        widgetSizeUtil,
        { widgetData: WidgetData, view: View ->
          widgetManager.startConfigurationActivity(requireActivity(), view, widgetData.widgetId)
        },
        { widgetsViewModel.delete(it.widgetId) },
        { widgetsViewModel.moveUp(it.widgetId) },
        { widgetData: WidgetData, height: Int ->
          widgetsViewModel.setHeight(widgetData.widgetId, height)
        },
        { widgetsViewModel.moveDown(it.widgetId) },
        { widgetManager.isInEditMode.value = false },
      ),
    )
  }

  @RequiresApi(Build.VERSION_CODES.Q)
  private val onNewIntentListener: Consumer<Intent> = Consumer { intent ->
    GestureContract.fromIntent(intent)?.let { maybeAnimateGestureContract(it) }
  }

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
      requireActivity().addOnNewIntentListener(onNewIntentListener)
    }
  }

  override fun onCreateView(
    inflater: LayoutInflater,
    container: ViewGroup?,
    savedInstanceState: Bundle?,
  ): View {
    val view = inflater.inflate(R.layout.launcher_fragment, container, false) as ConstraintLayout

    recyclerView = view.findViewById(R.id.app_list)
    recyclerView.apply {
      this.adapter = recyclerAdapter
      layoutManager =
        DynamicGridLayoutManager(context, R.dimen.min_column_width).apply {
          setSpanSizeProvider { position, spanCount ->
            when (recyclerAdapter.currentList[position]) {
              is WidgetViewItem,
              is WidgetEditorViewItem,
              is GroupHeaderViewItem -> spanCount
              else -> 1
            }
          }
        }
    }

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
      gestureIconView = GestureIconView(view.context)
      view.addView(gestureIconView)
    }

    view.findViewById<ComposeView>(R.id.bottom_bar).apply {
      setContent {
        LauncherTheme {
          BottomBar(
            tabButtonGroups = {
              ProfileTabs(profilesModel, workProfileManager, childFragmentManager)
            }
          ) {
            SearchFab()
          }
        }
      }

      addOnLayoutChangeListener { _, _, _, _, _, _, _, _, _ ->
        recyclerView.setPadding(
          recyclerView.paddingLeft,
          recyclerView.paddingTop,
          recyclerView.paddingRight,
          height,
        )
      }
    }

    viewLifecycleOwner.lifecycleScope.launch {
      viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.CREATED) {
        launcherViewModel.viewItems.collectLatest { recyclerAdapter.submitList(it) }
      }
    }

    return view
  }

  override fun onDestroy() {
    super.onDestroy()

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
      requireActivity().removeOnNewIntentListener(onNewIntentListener)
    }
  }

  @RequiresApi(Build.VERSION_CODES.Q)
  private fun maybeAnimateGestureContract(gestureContract: GestureContract) {
    for ((index, item) in recyclerAdapter.currentList.withIndex()) {
      if (
        item is TileViewItem &&
          item.data is ActivityData &&
          item.data.userActivity.componentName.packageName ==
            gestureContract.componentName.packageName &&
          item.data.userActivity.userHandle == gestureContract.userHandle
      ) {
        val viewHolder = recyclerView.findViewHolderForAdapterPosition(index)
        if (viewHolder != null && viewHolder is TransparentTileViewHolder) {
          gestureIconView.animateNavigationGesture(gestureContract, viewHolder.iconView)
          return
        }
      }
    }
  }

  private fun onTileClick(view: View, data: Any) {
    when (data) {
      is ActivityData -> {
        activityManager.launchActivity(
          data.userActivity,
          view.boundsOnScreen,
          view.makeScaleUpAnimation().toBundle(),
        )
      }
      is UserShortcut -> {
        shortcutManager.launchShortcut(
          data,
          view.boundsOnScreen,
          view.makeScaleUpAnimation().toBundle(),
        )
      }
      else -> throw NotImplementedError()
    }
  }

  private fun onTileLongClick(tileViewData: Any) {
    when (tileViewData) {
      is ActivityData -> {
        ActivityDetailsDialogFragment.newInstance(tileViewData)
          .show(parentFragmentManager, ActivityDetailsDialogFragment.TAG)
      }
      is UserShortcut -> {
        MaterialAlertDialogBuilder(requireContext())
          .setTitle(R.string.unpin_shortcut)
          .setPositiveButton(R.string.unpin) { _, _ ->
            Toast.makeText(context, R.string.unpinned_shortcut, Toast.LENGTH_SHORT).show()
            shortcutManager.pinShortcut(tileViewData, isPinned = false)
          }
          .setNegativeButton(android.R.string.cancel, null)
          .show()
      }
      else -> throw NotImplementedError()
    }
  }
}
