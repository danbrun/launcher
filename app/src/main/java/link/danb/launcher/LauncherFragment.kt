package link.danb.launcher

import android.app.SearchManager
import android.appwidget.AppWidgetHost
import android.appwidget.AppWidgetManager
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.UserHandle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.util.Consumer
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomappbar.BottomAppBar
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.floatingactionbutton.FloatingActionButton
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import link.danb.launcher.activities.ActivityDetailsDialogFragment
import link.danb.launcher.activities.ActivityManager
import link.danb.launcher.components.UserShortcut
import link.danb.launcher.database.ActivityData
import link.danb.launcher.database.WidgetData
import link.danb.launcher.extensions.allowPendingIntentBackgroundActivityStart
import link.danb.launcher.extensions.boundsOnScreen
import link.danb.launcher.extensions.makeScaleUpAnimation
import link.danb.launcher.extensions.setSpanSizeProvider
import link.danb.launcher.gestures.GestureContract
import link.danb.launcher.gestures.GestureIconView
import link.danb.launcher.profiles.ProfilesModel
import link.danb.launcher.shortcuts.ShortcutManager
import link.danb.launcher.tiles.TileViewItem
import link.danb.launcher.tiles.TileViewItemFactory
import link.danb.launcher.tiles.TransparentTileViewBinder
import link.danb.launcher.tiles.TransparentTileViewHolder
import link.danb.launcher.ui.DynamicGridLayoutManager
import link.danb.launcher.ui.GroupHeaderViewBinder
import link.danb.launcher.ui.GroupHeaderViewItem
import link.danb.launcher.ui.ViewBinderAdapter
import link.danb.launcher.ui.ViewItem
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

  private val widgetsViewModel: WidgetsViewModel by activityViewModels()

  @Inject lateinit var activityManager: ActivityManager
  @Inject lateinit var appWidgetHost: AppWidgetHost
  @Inject lateinit var appWidgetManager: AppWidgetManager
  @Inject lateinit var appWidgetViewProvider: AppWidgetViewProvider
  @Inject lateinit var launcherMenuProvider: LauncherMenuProvider
  @Inject lateinit var profilesModel: ProfilesModel
  @Inject lateinit var tileViewItemFactory: TileViewItemFactory
  @Inject lateinit var widgetManager: WidgetManager
  @Inject lateinit var widgetSizeUtil: WidgetSizeUtil
  @Inject lateinit var shortcutManager: ShortcutManager

  private lateinit var recyclerView: RecyclerView
  private lateinit var gestureIconView: GestureIconView

  private val isInEditMode: MutableStateFlow<Boolean> = MutableStateFlow(false)

  private val recyclerAdapter: ViewBinderAdapter by lazy {
    ViewBinderAdapter(
      GroupHeaderViewBinder(),
      TransparentTileViewBinder(this::onTileClick) { _, it -> onTileLongClick(it) },
      WidgetViewBinder(appWidgetViewProvider) { isInEditMode.value = true },
      WidgetEditorViewBinder(
        appWidgetViewProvider,
        widgetSizeUtil,
        { widgetData: WidgetData, view: View ->
          appWidgetHost.startAppWidgetConfigureActivityForResult(
            this@LauncherFragment.requireActivity(),
            widgetData.widgetId,
            /* intentFlags = */ 0,
            R.id.app_widget_configure_request_id,
            view.makeScaleUpAnimation().allowPendingIntentBackgroundActivityStart().toBundle(),
          )
        },
        { widgetsViewModel.delete(it.widgetId) },
        { widgetsViewModel.moveUp(it.widgetId) },
        { widgetData: WidgetData, height: Int ->
          widgetsViewModel.setHeight(widgetData.widgetId, height)
        },
        { widgetsViewModel.moveDown(it.widgetId) },
        { isInEditMode.value = false },
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
    val view = inflater.inflate(R.layout.launcher_fragment, container, false) as CoordinatorLayout

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

    view.findViewById<BottomAppBar>(R.id.bottom_app_bar).addMenuProvider(launcherMenuProvider)

    view.findViewById<FloatingActionButton>(R.id.floating_action).setOnClickListener { button ->
      startActivity(
        Intent().apply {
          action = Intent.ACTION_WEB_SEARCH
          putExtra(SearchManager.EXTRA_NEW_SEARCH, true)
          // This extra is for Firefox to open a new tab.
          putExtra("open_to_search", "static_shortcut_new_tab")
        },
        button.makeScaleUpAnimation().toBundle(),
      )
    }

    viewLifecycleOwner.lifecycleScope.launch {
      viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.CREATED) {
        combine(
            profilesModel.activeProfile,
            isInEditMode,
            widgetManager.data,
            activityManager.data,
            shortcutManager.shortcuts,
            ::getViewItems,
          )
          .collectLatest { recyclerAdapter.submitList(it) }
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

  private suspend fun getViewItems(
    activeProfile: UserHandle,
    isInEditMode: Boolean,
    widgets: List<WidgetData>,
    launcherActivities: List<ActivityData>,
    shortcuts: List<UserShortcut>,
  ): List<ViewItem> =
    getWidgetListViewItems(widgets, activeProfile, isInEditMode) +
      getPinnedListViewItems(launcherActivities, shortcuts, activeProfile) +
      getAppListViewItems(launcherActivities, activeProfile)

  private fun getWidgetListViewItems(
    widgets: List<WidgetData>,
    activeProfile: UserHandle,
    isInEditMode: Boolean,
  ): List<ViewItem> = buildList {
    for (widget in widgets) {
      if (appWidgetManager.getAppWidgetInfo(widget.widgetId).profile == activeProfile) {
        add(WidgetViewItem(widget))
        if (isInEditMode) {
          add(WidgetEditorViewItem(widget, appWidgetManager.getAppWidgetInfo(widget.widgetId)))
        }
      }
    }
  }

  private suspend fun getPinnedListViewItems(
    launcherActivities: List<ActivityData>,
    shortcuts: List<UserShortcut>,
    activeProfile: UserHandle,
  ): List<ViewItem> =
    withContext(Dispatchers.IO) {
      val pinnedItems =
        merge(
            launcherActivities
              .asFlow()
              .filter { it.isPinned && it.userActivity.userHandle == activeProfile }
              .map { tileViewItemFactory.getTileViewItem(it, TileViewItem.Style.TRANSPARENT) },
            shortcuts
              .asFlow()
              .filter { it.userHandle == activeProfile }
              .map { tileViewItemFactory.getTileViewItem(it, TileViewItem.Style.TRANSPARENT) },
          )
          .toList()
          .sortedBy { it.name.toString().lowercase() }

      buildList {
        if (pinnedItems.isNotEmpty()) {
          add(GroupHeaderViewItem(requireContext().getString(R.string.pinned_items)))
          addAll(pinnedItems)
        }
      }
    }

  private suspend fun getAppListViewItems(
    launcherActivities: List<ActivityData>,
    activeProfile: UserHandle,
  ): List<ViewItem> =
    withContext(Dispatchers.IO) {
      val (alphabetical, miscellaneous) =
        launcherActivities
          .asFlow()
          .filter { !it.isHidden && it.userActivity.userHandle == activeProfile }
          .map { tileViewItemFactory.getTileViewItem(it, TileViewItem.Style.TRANSPARENT) }
          .toList()
          .partition { it.name.first().isLetter() }

      val groupedMiscellaneous = buildList {
        if (miscellaneous.isNotEmpty()) {
          add(GroupHeaderViewItem(requireContext().getString(R.string.ellipses)))
          addAll(miscellaneous.sortedBy { it.name.toString().lowercase() })
        }
      }

      val groupedAlphabetical =
        alphabetical
          .groupBy { it.name.first().uppercaseChar() }
          .toSortedMap()
          .flatMap { (groupName, activityItems) ->
            buildList {
              add(GroupHeaderViewItem(groupName.toString()))
              addAll(activityItems.sortedBy { it.name.toString().lowercase() })
            }
          }

      groupedMiscellaneous + groupedAlphabetical
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
