package link.danb.launcher

import android.app.Application
import android.appwidget.AppWidgetManager
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
import kotlinx.coroutines.flow.asFlow
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
import link.danb.launcher.profiles.ProfilesModel
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
  profilesModel: ProfilesModel,
  shortcutManager: ShortcutManager,
  private val tileViewItemFactory: TileViewItemFactory,
  widgetManager: WidgetManager,
) : AndroidViewModel(application) {

  val isInEditMode: MutableStateFlow<Boolean> = MutableStateFlow(false)
  val searchQuery: MutableStateFlow<String?> = MutableStateFlow(null)

  private val userState = combine(isInEditMode, searchQuery, ::UserState)

  @OptIn(FlowPreview::class)
  val viewItems: Flow<List<ViewItem>> =
    combine(
        activityManager.data,
        shortcutManager.shortcuts,
        widgetManager.data,
        profilesModel.activeProfile,
        userState,
        ::CombinedData,
      )
      .debounce(100)
      .map {
        val searchQuery = it.userState.searchQuery?.lowercase()?.trim()
        if (searchQuery == null) {
          getWidgetListViewItems(it.widgets, it.activeProfile, it.userState.isInEditMode) +
            getPinnedListViewItems(it.activities, it.shortcuts, it.activeProfile) +
            getAppListViewItems(it.activities, it.activeProfile, null)
        } else {
          getAppListViewItems(it.activities, it.activeProfile, searchQuery).filter { item ->
            if (item is TileViewItem) {
              item.name.toString().lowercase().contains(searchQuery)
            } else {
              false
            }
          }
        }
      }
      .stateIn(viewModelScope + Dispatchers.IO, SharingStarted.WhileSubscribed(), listOf())

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
    activities: List<ActivityData>,
    shortcuts: List<UserShortcut>,
    activeProfile: UserHandle,
  ): List<ViewItem> = buildList {
    val pinnedItems =
      merge(
          activities
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

    if (pinnedItems.isNotEmpty()) {
      add(GroupHeaderViewItem(application.getString(R.string.pinned_items)))
      addAll(pinnedItems)
    }
  }

  private suspend fun getAppListViewItems(
    activities: List<ActivityData>,
    activeProfile: UserHandle,
    searchQuery: String?,
  ): List<ViewItem> = buildList {
    val (alphabetical, miscellaneous) =
      activities
        .asFlow()
        .filter {
          searchQuery != null || (!it.isHidden && it.userActivity.userHandle == activeProfile)
        }
        .map { tileViewItemFactory.getTileViewItem(it, TileViewItem.Style.TRANSPARENT) }
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
    val activeProfile: UserHandle,
    val userState: UserState,
  )

  private data class UserState(val isInEditMode: Boolean, val searchQuery: String?)
}
