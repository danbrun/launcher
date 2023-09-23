package link.danb.launcher

import android.app.SearchManager
import android.appwidget.AppWidgetHost
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.LauncherApps
import android.content.pm.ShortcutInfo
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
import com.google.android.material.floatingactionbutton.FloatingActionButton
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import link.danb.launcher.activities.ActivitiesViewModel
import link.danb.launcher.activities.ActivityDetailsDialogFragment
import link.danb.launcher.activities.ActivityInfoWithData
import link.danb.launcher.database.WidgetData
import link.danb.launcher.extensions.allowPendingIntentBackgroundActivityStart
import link.danb.launcher.extensions.getBoundsOnScreen
import link.danb.launcher.extensions.makeScaleUpAnimation
import link.danb.launcher.extensions.setSpanSizeProvider
import link.danb.launcher.gestures.GestureContractModel
import link.danb.launcher.icons.LauncherIconCache
import link.danb.launcher.profiles.ProfilesModel
import link.danb.launcher.shortcuts.ShortcutsViewModel
import link.danb.launcher.tiles.ActivityTileData
import link.danb.launcher.tiles.ShortcutTileData
import link.danb.launcher.tiles.TileData
import link.danb.launcher.tiles.TileViewItem
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
import link.danb.launcher.widgets.WidgetEditorViewListener
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
  @Inject lateinit var launcherIconCache: LauncherIconCache
  @Inject lateinit var launcherMenuProvider: LauncherMenuProvider
  @Inject lateinit var profilesModel: ProfilesModel
  @Inject lateinit var widgetSizeUtil: WidgetSizeUtil

  private lateinit var recyclerView: RecyclerView

  private val recyclerAdapter: ViewBinderAdapter by lazy {
    ViewBinderAdapter(
      GroupHeaderViewBinder(),
      TransparentTileViewBinder(this::onTileClick) { _, it -> onTileLongClick(it) },
      WidgetViewBinder(appWidgetViewProvider) { widgetsViewModel.startEditing(it.widgetId) },
      WidgetEditorViewBinder(appWidgetViewProvider, widgetSizeUtil, widgetEditorViewListener),
    )
  }

  private val ellipses: CharSequence by lazy { requireContext().getString(R.string.ellipses) }

  private val widgetEditorViewListener =
    object : WidgetEditorViewListener {
      override fun onConfigure(widgetData: WidgetData, view: View) {
        appWidgetHost.startAppWidgetConfigureActivityForResult(
          this@LauncherFragment.requireActivity(),
          widgetData.widgetId,
          /* intentFlags = */ 0,
          R.id.app_widget_configure_request_id,
          view.makeScaleUpAnimation().allowPendingIntentBackgroundActivityStart().toBundle()
        )
      }

      override fun onDelete(widgetData: WidgetData) {
        widgetsViewModel.delete(widgetData.widgetId)
      }

      override fun onMoveUp(widgetData: WidgetData) {
        widgetsViewModel.moveUp(widgetData.widgetId)
      }

      override fun onResize(widgetData: WidgetData, height: Int) {
        widgetsViewModel.setHeight(widgetData.widgetId, height)
      }

      override fun onMoveDown(widgetData: WidgetData) {
        widgetsViewModel.moveDown(widgetData.widgetId)
      }

      override fun onDone(widgetData: WidgetData) {
        widgetsViewModel.finishEditing()
      }
    }

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

    recyclerAdapter.onBindViewHolderListener = { maybeAnimateNewIntent() }

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
      viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
        launch {
          val widgetsFlow =
            combine(
              widgetsViewModel.widgets,
              widgetsViewModel.widgetToEdit,
              profilesModel.activeProfile,
              ::getWidgetListViewItems
            )

          val shortcutsFlow =
            combine(
              shortcutsViewModel.pinnedShortcuts,
              profilesModel.activeProfile,
              ::getShortcutListViewItems
            )

          val appsFlow =
            combine(
              activitiesViewModel.activities,
              profilesModel.activeProfile,
              activitiesViewModel.sortByCategory,
              ::getAppListViewItems
            )

          combine(widgetsFlow, shortcutsFlow, appsFlow) { widgets, shortcuts, apps ->
              widgets + shortcuts + apps
            }
            .collectLatest { recyclerAdapter.submitList(it) }
        }

        launch { gestureContractModel.gestureContract.collect { maybeAnimateNewIntent() } }
      }
    }

    return view
  }

  override fun onStart() {
    super.onStart()

    widgetsViewModel.refresh()
  }

  private fun getWidgetListViewItems(
    widgets: List<WidgetData>,
    widgetToEdit: Int?,
    activeProfile: UserHandle,
  ): List<ViewItem> =
    widgets.flatMap {
      val info = appWidgetManager.getAppWidgetInfo(it.widgetId)
      if (info.profile != activeProfile) return@flatMap listOf()

      if (it.widgetId == widgetToEdit) {
        listOf(
          WidgetViewItem(it),
          WidgetEditorViewItem(it, appWidgetManager.getAppWidgetInfo(it.widgetId))
        )
      } else {
        listOf(WidgetViewItem(it))
      }
    }

  private suspend fun getShortcutListViewItems(
    shortcuts: List<ShortcutInfo>,
    activeProfile: UserHandle,
  ): List<ViewItem> =
    withContext(Dispatchers.IO) {
      shortcuts
        .filter { it.userHandle == activeProfile }
        .map {
          async {
            TileViewItem.transparentTileViewItem(
              ShortcutTileData(it),
              it.shortLabel!!,
              launcherIconCache.get(it)
            )
          }
        }
        .awaitAll()
        .sortedBy { it.name.toString().lowercase() }
        .takeIf { it.isNotEmpty() }
        ?.let { listOf(GroupHeaderViewItem(requireContext().getString(R.string.shortcuts))) + it }
        ?: listOf()
    }

  private suspend fun getAppListViewItems(
    launcherActivities: List<ActivityInfoWithData>,
    activeProfile: UserHandle,
    sortByCategory: Boolean,
  ): List<ViewItem> =
    withContext(Dispatchers.IO) {
      launcherActivities
        .filter { !it.data.isHidden && it.info.user == activeProfile }
        .map {
          async {
            Pair(
              it,
              TileViewItem.transparentTileViewItem(
                ActivityTileData(it.info),
                it.info.label,
                launcherIconCache.get(it.info)
              )
            )
          }
        }
        .awaitAll()
        .groupBy {
          if (sortByCategory) {
            getStringForCategory(it.first.info.applicationInfo.category)
          } else {
            val initial = it.second.name.first().uppercaseChar()
            when {
              initial.isLetter() -> initial.toString()
              else -> ellipses
            }
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
            activityItems.map { it.second }.sortedBy { it.name.toString().lowercase() }
        }
    }

  private fun maybeAnimateNewIntent() {
    val gestureContract = gestureContractModel.gestureContract.value ?: return

    val firstMatchingIndex =
      getFirstMatchingIndex(gestureContract.componentName, gestureContract.userHandle)

    if (firstMatchingIndex < 0) return

    val view = recyclerView.findViewHolderForAdapterPosition(firstMatchingIndex)?.itemView

    if (view != null) {
      gestureContractModel.setBounds(view.getBoundsOnScreen().toRectF())
    } else {
      recyclerView.scrollToPosition(firstMatchingIndex)
    }
  }

  private fun getFirstMatchingIndex(component: ComponentName, user: UserHandle): Int {
    // If there is an exact component match, returns that icon view. Otherwise returns the icon
    // view of the first package match.

    var firstMatchIndex = -1

    for (index in recyclerAdapter.currentList.indices) {
      val item = recyclerAdapter.currentList[index]

      if (item !is TileViewItem || item.data !is ActivityTileData || item.data.info.user != user)
        continue

      if (item.data.info.componentName == component) {
        return index
      }

      if (item.data.info.componentName.packageName == component.packageName) {
        firstMatchIndex = index
      }
    }

    return firstMatchIndex
  }

  private fun getStringForCategory(category: Int): String =
    requireContext()
      .getString(
        when (category) {
          ApplicationInfo.CATEGORY_ACCESSIBILITY -> R.string.accessibility
          ApplicationInfo.CATEGORY_AUDIO -> R.string.audio
          ApplicationInfo.CATEGORY_GAME -> R.string.games
          ApplicationInfo.CATEGORY_IMAGE -> R.string.images
          ApplicationInfo.CATEGORY_MAPS -> R.string.maps
          ApplicationInfo.CATEGORY_NEWS -> R.string.news
          ApplicationInfo.CATEGORY_PRODUCTIVITY -> R.string.productivity
          ApplicationInfo.CATEGORY_SOCIAL -> R.string.social
          ApplicationInfo.CATEGORY_VIDEO -> R.string.video
          else -> R.string.other
        }
      )

  private fun onTileClick(view: View, tileViewData: TileData) {
    when (tileViewData) {
      is ActivityTileData -> {
        launcherApps.startMainActivity(
          tileViewData.info.componentName,
          tileViewData.info.user,
          view.getBoundsOnScreen(),
          view.makeScaleUpAnimation().toBundle()
        )
      }
      is ShortcutTileData -> {
        launcherApps.startShortcut(
          tileViewData.info,
          view.getBoundsOnScreen(),
          view.makeScaleUpAnimation().toBundle()
        )
      }
    }
  }

  private fun onTileLongClick(tileViewData: TileData) {
    when (tileViewData) {
      is ActivityTileData -> {
        ActivityDetailsDialogFragment.newInstance(tileViewData.info)
          .show(parentFragmentManager, ActivityDetailsDialogFragment.TAG)
      }
      is ShortcutTileData -> {
        Toast.makeText(context, R.string.unpinned_shortcut, Toast.LENGTH_SHORT).show()
        shortcutsViewModel.unpinShortcut(tileViewData.info)
      }
    }
  }
}
