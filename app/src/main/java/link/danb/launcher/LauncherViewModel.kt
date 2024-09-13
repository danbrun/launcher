package link.danb.launcher

import android.app.Application
import android.appwidget.AppWidgetManager
import android.os.Build
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlin.math.max
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
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
import link.danb.launcher.profiles.Profile
import link.danb.launcher.profiles.ProfileManager
import link.danb.launcher.shortcuts.ShortcutManager
import link.danb.launcher.ui.LauncherTileData
import link.danb.launcher.widgets.WidgetManager

sealed interface ViewItem {
  val key: String
}

data class WidgetViewItem(val widgetData: WidgetData, val sizeRange: IntRange) : ViewItem {
  override val key: String = widgetData.widgetId.toString()
}

data class GroupHeaderViewItem(val name: String) : ViewItem {
  override val key: String = name
}

sealed interface IconTileViewItem : ViewItem {
  val launcherTileData: LauncherTileData
}

data class ShortcutViewItem(
  val userShortcut: UserShortcut,
  override val launcherTileData: LauncherTileData,
) : IconTileViewItem {
  override val key: String = userShortcut.toString()
}

data class ActivityViewItem(
  val userActivity: UserActivity,
  override val launcherTileData: LauncherTileData,
  val isPinned: Boolean,
) : IconTileViewItem {
  override val key: String = "$userActivity,$isPinned"
}

@HiltViewModel
class LauncherViewModel
@Inject
constructor(
  activityManager: ActivityManager,
  private val application: Application,
  private val appWidgetManager: AppWidgetManager,
  private val launcherResourceProvider: LauncherResourceProvider,
  private val profileManager: ProfileManager,
  shortcutManager: ShortcutManager,
  widgetManager: WidgetManager,
) : AndroidViewModel(application) {

  private val _filter = MutableStateFlow<Filter>(ProfileFilter(Profile.PERSONAL))

  val filter: StateFlow<Filter> = _filter.asStateFlow()

  @OptIn(FlowPreview::class)
  val viewItems: StateFlow<ImmutableList<ViewItem>> =
    combine(
        activityManager.data,
        shortcutManager.shortcuts,
        widgetManager.data,
        filter,
        ::CombinedData,
      )
      .debounce(100)
      .map {
        buildList {
            addWidgetListViewItems(it.widgets, it.filter)
            addPinnedListViewItems(it.activities, it.shortcuts, it.filter)
            addAppListViewItems(it.activities, it.filter)
          }
          .toImmutableList()
      }
      .stateIn(
        viewModelScope + Dispatchers.IO,
        SharingStarted.WhileSubscribed(),
        persistentListOf(),
      )

  val bottomBarState: StateFlow<BottomBarState> =
    combine(activityManager.data, filter, profileManager.profileStates) {
        activities,
        filter,
        profileStates ->
        BottomBarStateProducer.getBottomBarState(filter, profileStates, activities) {
          profileManager.getUserHandle(it)!!
        }
      }
      .stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(),
        BottomBarState(emptyList(), emptyList(), workProfileToggle = null, isSearching = false),
      )

  fun setFilter(filter: Filter) {
    _filter.value = filter
  }

  private fun MutableList<ViewItem>.addWidgetListViewItems(
    widgets: List<WidgetData>,
    filter: Filter,
  ) {
    if (filter is ProfileFilter) {
      for (widget in widgets) {
        val providerInfo = appWidgetManager.getAppWidgetInfo(widget.widgetId)
        if (providerInfo.profile == profileManager.getUserHandle(filter.profile)) {
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

  private suspend fun MutableList<ViewItem>.addPinnedListViewItems(
    activities: List<ActivityData>,
    shortcuts: List<UserShortcut>,
    filter: Filter,
  ) {
    if (filter is ProfileFilter) {
      val pinnedItems =
        merge(
            activities
              .asFlow()
              .filter {
                it.isPinned &&
                  it.userActivity.userHandle == profileManager.getUserHandle(filter.profile)
              }
              .map { getActivityTileItem(it, isPinned = true) },
            shortcuts
              .asFlow()
              .filter { it.userHandle == profileManager.getUserHandle(filter.profile) }
              .map {
                ShortcutViewItem(it, launcherResourceProvider.getTileDataWithCache(it).await())
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

  private suspend fun MutableList<ViewItem>.addAppListViewItems(
    activities: List<ActivityData>,
    filter: Filter,
  ) {
    val (alphabetical, miscellaneous) =
      activities
        .asFlow()
        .filter {
          when (filter) {
            is ProfileFilter ->
              !it.isHidden &&
                it.userActivity.userHandle == profileManager.getUserHandle(filter.profile)
            is SearchFilter -> true
          }
        }
        .map { getActivityTileItem(it, isPinned = false) }
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

  private suspend fun getActivityTileItem(activityData: ActivityData, isPinned: Boolean) =
    ActivityViewItem(
      activityData.userActivity,
      launcherResourceProvider.getTileDataWithCache(activityData.userActivity).await(),
      isPinned,
    )

  private data class CombinedData(
    val activities: List<ActivityData>,
    val shortcuts: List<UserShortcut>,
    val widgets: List<WidgetData>,
    val filter: Filter,
  )
}
