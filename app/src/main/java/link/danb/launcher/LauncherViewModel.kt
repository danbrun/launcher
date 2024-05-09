package link.danb.launcher

import android.app.Application
import android.appwidget.AppWidgetManager
import android.os.Build
import android.os.Process
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlin.math.max
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.plus
import link.danb.launcher.activities.ActivityManager
import link.danb.launcher.apps.LauncherResourceProvider
import link.danb.launcher.components.UserActivity
import link.danb.launcher.components.UserShortcut
import link.danb.launcher.database.ActivityData
import link.danb.launcher.database.WidgetData
import link.danb.launcher.profiles.ProfileManager
import link.danb.launcher.shortcuts.ShortcutManager
import link.danb.launcher.ui.LauncherIconData
import link.danb.launcher.ui.LauncherTileData
import link.danb.launcher.widgets.WidgetManager

sealed interface ViewItem

data class WidgetViewItem(val widgetData: WidgetData, val sizeRange: IntRange) : ViewItem

data class GroupHeaderViewItem(val name: String) : ViewItem

sealed interface IconTileViewItem : ViewItem {
  val launcherTileData: LauncherTileData
}

data class ShortcutViewItem(
  val userShortcut: UserShortcut,
  override val launcherTileData: LauncherTileData,
) : IconTileViewItem

data class ActivityViewItem(
  val userActivity: UserActivity,
  override val launcherTileData: LauncherTileData,
) : IconTileViewItem

@HiltViewModel
class LauncherViewModel
@Inject
constructor(
  activityManager: ActivityManager,
  private val application: Application,
  private val appWidgetManager: AppWidgetManager,
  private val launcherResourceProvider: LauncherResourceProvider,
  profileManager: ProfileManager,
  shortcutManager: ShortcutManager,
  widgetManager: WidgetManager,
) : AndroidViewModel(application) {

  private val _filter = MutableStateFlow<Filter>(ProfileFilter(Process.myUserHandle()))

  val filter: StateFlow<Filter> = _filter.asStateFlow()

  @OptIn(FlowPreview::class)
  val viewItems: StateFlow<List<ViewItem>> =
    combine(
        activityManager.data,
        shortcutManager.shortcuts,
        widgetManager.data,
        filter,
        ::CombinedData,
      )
      .debounce(100)
      .map {
        getWidgetListViewItems(it.widgets, it.filter) +
          getPinnedListViewItems(it.activities, it.shortcuts, it.filter) +
          getAppListViewItems(it.activities, it.filter)
      }
      .stateIn(viewModelScope + Dispatchers.IO, SharingStarted.WhileSubscribed(), listOf())

  val bottomBarState: StateFlow<BottomBarState> =
    combine(activityManager.data, filter, profileManager.profiles) { activities, filter, profiles ->
        BottomBarStateProducer.getBottomBarState(filter, profiles, activities)
      }
      .stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(),
        BottomBarState(emptyList(), emptyList(), workProfileToggle = null, isSearching = false),
      )

  fun setFilter(filter: Filter) {
    _filter.value = filter
  }

  private fun getWidgetListViewItems(widgets: List<WidgetData>, filter: Filter): List<ViewItem> =
    buildList {
      if (filter is ProfileFilter) {
        for (widget in widgets) {
          val providerInfo = appWidgetManager.getAppWidgetInfo(widget.widgetId)
          if (providerInfo.profile == filter.profile) {
            val minHeight =
              max(
                providerInfo.minHeight,
                application.resources.getDimensionPixelSize(R.dimen.widget_min_height),
              )
            val maxHeight =
              max(
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                  providerInfo.maxResizeHeight
                } else {
                  0
                },
                application.resources.getDimensionPixelSize(R.dimen.widget_max_height),
              )

            add(WidgetViewItem(widget, minHeight..maxHeight))
          }
        }
      }
    }

  private suspend fun getPinnedListViewItems(
    activities: List<ActivityData>,
    shortcuts: List<UserShortcut>,
    filter: Filter,
  ): List<ViewItem> = buildList {
    if (filter is ProfileFilter) {
      val pinnedItems =
        merge(
            activities
              .asFlow()
              .filter { it.isPinned && it.userActivity.userHandle == filter.profile }
              .map { getActivityTileItem(it) },
            shortcuts
              .asFlow()
              .filter { it.userHandle == filter.profile }
              .map {
                ShortcutViewItem(
                  it,
                  LauncherTileData(
                    LauncherIconData(
                      launcherResourceProvider.getIconWithCache(it).await(),
                      launcherResourceProvider.getBadge(it.userHandle),
                    ),
                    launcherResourceProvider.getLabel(it),
                  ),
                )
              },
          )
          .toList()
          .sortedBy { it.launcherTileData.name.lowercase() }

      if (pinnedItems.isNotEmpty()) {
        add(GroupHeaderViewItem(application.getString(R.string.pinned_items)))
        addAll(pinnedItems)
      }
    }
  }

  private suspend fun getAppListViewItems(
    activities: List<ActivityData>,
    filter: Filter,
  ): List<ViewItem> = buildList {
    val (alphabetical, miscellaneous) =
      activities
        .asFlow()
        .filter {
          when (filter) {
            is ProfileFilter -> !it.isHidden && it.userActivity.userHandle == filter.profile
            is SearchFilter -> true
          }
        }
        .map { getActivityTileItem(it) }
        .filter {
          when (filter) {
            is ProfileFilter -> true
            is SearchFilter ->
              it.launcherTileData.name.lowercase().contains(filter.query.lowercase().trim())
          }
        }
        .toList()
        .partition { it.launcherTileData.name.first().isLetter() }

    if (miscellaneous.isNotEmpty()) {
      add(GroupHeaderViewItem(application.getString(R.string.ellipses)))
      addAll(miscellaneous.sortedBy { it.launcherTileData.name.lowercase() })
    }

    for ((groupName, activityItems) in
      alphabetical.groupBy { it.launcherTileData.name.first().uppercaseChar() }.toSortedMap()) {

      add(GroupHeaderViewItem(groupName.toString()))
      addAll(activityItems.sortedBy { it.launcherTileData.name.lowercase() })
    }
  }

  private suspend fun getActivityTileItem(activityData: ActivityData) =
    ActivityViewItem(
      activityData.userActivity,
      LauncherTileData(
        LauncherIconData(
          launcherResourceProvider.getIconWithCache(activityData.userActivity).await(),
          launcherResourceProvider.getBadge(activityData.userActivity.userHandle),
        ),
        launcherResourceProvider.getLabel(activityData.userActivity),
      ),
    )

  private data class CombinedData(
    val activities: List<ActivityData>,
    val shortcuts: List<UserShortcut>,
    val widgets: List<WidgetData>,
    val filter: Filter,
  )
}
