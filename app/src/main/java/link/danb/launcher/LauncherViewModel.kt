package link.danb.launcher

import android.app.Application
import android.appwidget.AppWidgetManager
import android.os.Process
import android.os.UserHandle
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.Flow
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
import link.danb.launcher.components.UserShortcut
import link.danb.launcher.database.ActivityData
import link.danb.launcher.database.WidgetData
import link.danb.launcher.shortcuts.ShortcutManager
import link.danb.launcher.tiles.TileViewItem
import link.danb.launcher.tiles.TileViewItemFactory
import link.danb.launcher.ui.GroupHeaderViewItem
import link.danb.launcher.ui.ViewItem
import link.danb.launcher.widgets.WidgetEditorViewItem
import link.danb.launcher.widgets.WidgetManager
import link.danb.launcher.widgets.WidgetViewItem

@HiltViewModel
class LauncherViewModel
@Inject
constructor(
  activityManager: ActivityManager,
  private val application: Application,
  private val appWidgetManager: AppWidgetManager,
  shortcutManager: ShortcutManager,
  private val tileViewItemFactory: TileViewItemFactory,
  widgetManager: WidgetManager,
) : AndroidViewModel(application) {

  private val _filter = MutableStateFlow<Filter>(ProfileFilter(Process.myUserHandle()))

  val filter: StateFlow<Filter> = _filter.asStateFlow()

  @OptIn(FlowPreview::class)
  val viewItems: Flow<List<ViewItem>> =
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

  fun toggleEditMode() {
    val filter = filter.value
    if (filter is ProfileFilter) {
      _filter.value = filter.copy(isInEditMode = !filter.isInEditMode)
    }
  }

  fun setSearchFilter(query: String) {
    _filter.value = SearchFilter(query)
  }

  fun setProfileFilter(userHandle: UserHandle) {
    _filter.value = ProfileFilter(userHandle)
  }

  private fun getWidgetListViewItems(widgets: List<WidgetData>, filter: Filter): List<ViewItem> =
    buildList {
      if (filter is ProfileFilter) {
        for (widget in widgets) {
          if (appWidgetManager.getAppWidgetInfo(widget.widgetId).profile == filter.profile) {
            add(WidgetViewItem(widget))
            if (filter.isInEditMode) {
              add(WidgetEditorViewItem(widget, appWidgetManager.getAppWidgetInfo(widget.widgetId)))
            }
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
              .map { tileViewItemFactory.getTileViewItem(it, TileViewItem.Style.TRANSPARENT) },
            shortcuts
              .asFlow()
              .filter { it.userHandle == filter.profile }
              .map { tileViewItemFactory.getTileViewItem(it, TileViewItem.Style.TRANSPARENT) },
          )
          .toList()
          .sortedBy { it.name.toString().lowercase() }

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
        .map { tileViewItemFactory.getTileViewItem(it, TileViewItem.Style.TRANSPARENT) }
        .filter {
          when (filter) {
            is ProfileFilter -> true
            is SearchFilter ->
              it.name.toString().lowercase().contains(filter.query.lowercase().trim())
          }
        }
        .toList()
        .partition { it.name.first().isLetter() }

    if (miscellaneous.isNotEmpty()) {
      add(GroupHeaderViewItem(application.getString(R.string.ellipses)))
      addAll(miscellaneous.sortedBy { it.name.toString().lowercase() })
    }

    for ((groupName, activityItems) in
      alphabetical.groupBy { it.name.first().uppercaseChar() }.toSortedMap()) {

      add(GroupHeaderViewItem(groupName.toString()))
      addAll(activityItems.sortedBy { it.name.toString().lowercase() })
    }
  }

  private data class CombinedData(
    val activities: List<ActivityData>,
    val shortcuts: List<UserShortcut>,
    val widgets: List<WidgetData>,
    val filter: Filter,
  )
}
