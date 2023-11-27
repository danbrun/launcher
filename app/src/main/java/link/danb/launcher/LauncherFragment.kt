package link.danb.launcher

import android.app.SearchManager
import android.appwidget.AppWidgetHost
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Intent
import android.content.pm.LauncherApps
import android.os.Bundle
import android.os.UserHandle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.graphics.toRectF
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomappbar.BottomAppBar
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.floatingactionbutton.FloatingActionButton
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import link.danb.launcher.activities.ActivitiesViewModel
import link.danb.launcher.activities.ActivityDetailsDialogFragment
import link.danb.launcher.database.ActivityData
import link.danb.launcher.database.WidgetData
import link.danb.launcher.extensions.allowPendingIntentBackgroundActivityStart
import link.danb.launcher.extensions.boundsOnScreen
import link.danb.launcher.extensions.makeScaleUpAnimation
import link.danb.launcher.extensions.setSpanSizeProvider
import link.danb.launcher.gestures.GestureContract
import link.danb.launcher.gestures.GestureContractModel
import link.danb.launcher.profiles.ProfilesModel
import link.danb.launcher.shortcuts.ShortcutData
import link.danb.launcher.shortcuts.ShortcutsViewModel
import link.danb.launcher.tiles.TileViewItem
import link.danb.launcher.tiles.TileViewItemFactory
import link.danb.launcher.tiles.TransparentTileViewBinder
import link.danb.launcher.ui.GroupHeaderViewBinder
import link.danb.launcher.ui.GroupHeaderViewItem
import link.danb.launcher.ui.InvertedCornerDrawable
import link.danb.launcher.ui.RoundedCornerOutlineProvider
import link.danb.launcher.ui.ViewBinderAdapter
import link.danb.launcher.ui.ViewItem
import link.danb.launcher.widgets.AppWidgetViewProvider
import link.danb.launcher.widgets.WidgetEditorViewBinder
import link.danb.launcher.widgets.WidgetEditorViewItem
import link.danb.launcher.widgets.WidgetSizeUtil
import link.danb.launcher.widgets.WidgetViewBinder
import link.danb.launcher.widgets.WidgetViewItem
import link.danb.launcher.widgets.WidgetsViewModel

@AndroidEntryPoint
class LauncherFragment : Fragment() {

  private val activitiesViewModel: ActivitiesViewModel by activityViewModels()
  private val gestureContractModel: GestureContractModel by activityViewModels()
  private val shortcutsViewModel: ShortcutsViewModel by activityViewModels()
  private val widgetsViewModel: WidgetsViewModel by activityViewModels()

  @Inject lateinit var appWidgetHost: AppWidgetHost
  @Inject lateinit var appWidgetManager: AppWidgetManager
  @Inject lateinit var appWidgetViewProvider: AppWidgetViewProvider
  @Inject lateinit var launcherApps: LauncherApps
  @Inject lateinit var launcherMenuProvider: LauncherMenuProvider
  @Inject lateinit var profilesModel: ProfilesModel
  @Inject lateinit var tileViewItemFactory: TileViewItemFactory
  @Inject lateinit var widgetSizeUtil: WidgetSizeUtil

  private lateinit var recyclerView: RecyclerView

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
            view.makeScaleUpAnimation().allowPendingIntentBackgroundActivityStart().toBundle()
          )
        },
        { widgetsViewModel.delete(it.widgetId) },
        { widgetsViewModel.moveUp(it.widgetId) },
        { widgetData: WidgetData, height: Int ->
          widgetsViewModel.setHeight(widgetData.widgetId, height)
        },
        { widgetsViewModel.moveDown(it.widgetId) },
        { isInEditMode.value = false }
      ),
    )
  }

  private val ellipses: CharSequence by lazy { requireContext().getString(R.string.ellipses) }

  override fun onCreateView(
    inflater: LayoutInflater,
    container: ViewGroup?,
    savedInstanceState: Bundle?
  ): View? {
    val view = inflater.inflate(R.layout.launcher_fragment, container, false)

    val radius = resources.getDimensionPixelSize(R.dimen.apps_list_corner_radius)

    recyclerView = view.findViewById(R.id.app_list)
    recyclerView.apply {
      this.adapter = recyclerAdapter
      layoutManager =
        GridLayoutManager(
            context,
            requireContext().resources.getInteger(R.integer.launcher_columns)
          )
          .apply {
            setSpanSizeProvider { position, spanCount ->
              when (recyclerAdapter.currentList[position]) {
                is WidgetViewItem,
                is WidgetEditorViewItem,
                is GroupHeaderViewItem -> spanCount
                else -> 1
              }
            }
          }
      clipToOutline = true
      outlineProvider = RoundedCornerOutlineProvider(radius)
    }

    view.findViewById<View>(R.id.app_list_background).background = InvertedCornerDrawable(radius)

    view.findViewById<BottomAppBar>(R.id.bottom_app_bar).addMenuProvider(launcherMenuProvider)

    view.findViewById<FloatingActionButton>(R.id.floating_action).setOnClickListener { button ->
      startActivity(
        Intent().apply {
          action = Intent.ACTION_WEB_SEARCH
          putExtra(SearchManager.EXTRA_NEW_SEARCH, true)
          // This extra is for Firefox to open a new tab.
          putExtra("open_to_search", "static_shortcut_new_tab")
        },
        button.makeScaleUpAnimation().toBundle()
      )
    }

    viewLifecycleOwner.lifecycleScope.launch {
      viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.CREATED) {
        launch {
          val widgetsFlow =
            combine(
              widgetsViewModel.widgets,
              profilesModel.activeProfile,
              isInEditMode,
              ::getWidgetListViewItems
            )

          val pinnedFlow =
            combine(
              activitiesViewModel.activities,
              shortcutsViewModel.pinnedShortcuts,
              profilesModel.activeProfile,
              ::getPinnedListViewItems
            )

          val appsFlow =
            combine(
              activitiesViewModel.activities,
              profilesModel.activeProfile,
              ::getAppListViewItems
            )

          @OptIn(FlowPreview::class)
          combine(widgetsFlow, pinnedFlow, appsFlow) { widgets, pins, apps ->
              widgets + pins + apps
            }
            .debounce(250)
            .collectLatest { recyclerAdapter.submitList(it) }
        }

        launch { gestureContractModel.gestureContract.collect { maybeAnimateGestureContract(it) } }
      }
    }

    return view
  }

  private fun getWidgetListViewItems(
    widgets: List<WidgetData>,
    activeProfile: UserHandle,
    isInEditMode: Boolean,
  ): List<ViewItem> =
    widgets.flatMap {
      val info = appWidgetManager.getAppWidgetInfo(it.widgetId)
      if (info.profile != activeProfile) return@flatMap listOf()

      if (isInEditMode) {
        listOf(
          WidgetViewItem(it),
          WidgetEditorViewItem(it, appWidgetManager.getAppWidgetInfo(it.widgetId))
        )
      } else {
        listOf(WidgetViewItem(it))
      }
    }

  private suspend fun getPinnedListViewItems(
    launcherActivities: List<ActivityData>,
    shortcuts: List<ShortcutData>,
    activeProfile: UserHandle,
  ): List<ViewItem> =
    withContext(Dispatchers.IO) {
      (launcherActivities
          .filter { it.isPinned && it.userHandle == activeProfile }
          .map {
            async { tileViewItemFactory.getTileViewItem(it, TileViewItem.Style.TRANSPARENT) }
          } +
          shortcuts
            .filter { it.userHandle == activeProfile }
            .map {
              async { tileViewItemFactory.getTileViewItem(it, TileViewItem.Style.TRANSPARENT) }
            })
        .awaitAll()
        .sortedBy { it.name.toString().lowercase() }
        .takeIf { it.isNotEmpty() }
        ?.let {
          listOf(GroupHeaderViewItem(requireContext().getString(R.string.pinned_items))) + it
        } ?: listOf()
    }

  private suspend fun getAppListViewItems(
    launcherActivities: List<ActivityData>,
    activeProfile: UserHandle,
  ): List<ViewItem> =
    withContext(Dispatchers.IO) {
      launcherActivities
        .filter { !it.isHidden && it.userHandle == activeProfile }
        .map { async { tileViewItemFactory.getTileViewItem(it, TileViewItem.Style.TRANSPARENT) } }
        .awaitAll()
        .groupBy {
          val initial = it.name.first().uppercaseChar()
          when {
            initial.isLetter() -> initial.toString()
            else -> ellipses
          }
        }
        .toSortedMap { first, second ->
          if (first == ellipses) {
            -1
          } else {
            first.toString().compareTo(second.toString())
          }
        }
        .flatMap { (groupName, activityItems) ->
          listOf(GroupHeaderViewItem(groupName.toString())) +
            activityItems.sortedBy { it.name.toString().lowercase() }
        }
    }

  private fun maybeAnimateGestureContract(gestureContract: GestureContract) {
    val firstMatchingIndex =
      getFirstMatchingIndex(gestureContract.componentName, gestureContract.userHandle)

    if (firstMatchingIndex < 0) {
      recyclerAdapter.onBindViewHolderListener = null
      return
    }

    val view = recyclerView.findViewHolderForAdapterPosition(firstMatchingIndex)?.itemView

    if (view != null) {
      recyclerAdapter.onBindViewHolderListener = null
      gestureContract.sendBounds(view.boundsOnScreen.toRectF())
    } else {
      recyclerAdapter.onBindViewHolderListener = { maybeAnimateGestureContract(gestureContract) }
      recyclerView.scrollToPosition(firstMatchingIndex)
    }
  }

  private fun getFirstMatchingIndex(component: ComponentName, user: UserHandle): Int {
    // If there is an exact component match, returns that icon view. Otherwise returns the icon
    // view of the first package match.

    var firstMatchIndex = -1

    for (index in recyclerAdapter.currentList.indices) {
      val item = recyclerAdapter.currentList[index]

      if (item !is TileViewItem || item.data !is ActivityData || item.data.userHandle != user)
        continue

      if (item.data.componentName == component) {
        return index
      }

      if (item.data.componentName.packageName == component.packageName) {
        firstMatchIndex = index
      }
    }

    return firstMatchIndex
  }

  private fun onTileClick(view: View, data: Any) {
    when (data) {
      is ActivityData -> {
        activitiesViewModel.launchActivity(
          data,
          view.boundsOnScreen,
          view.makeScaleUpAnimation().toBundle()
        )
      }
      is ShortcutData -> {
        shortcutsViewModel.launchShortcut(
          data,
          view.boundsOnScreen,
          view.makeScaleUpAnimation().toBundle()
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
      is ShortcutData -> {
        MaterialAlertDialogBuilder(requireContext())
          .setTitle(R.string.unpin_shortcut)
          .setPositiveButton(R.string.unpin) { _, _ ->
            Toast.makeText(context, R.string.unpinned_shortcut, Toast.LENGTH_SHORT).show()
            shortcutsViewModel.unpinShortcut(tileViewData)
          }
          .setNegativeButton(android.R.string.cancel, null)
          .show()
      }
      else -> throw NotImplementedError()
    }
  }
}
