package link.danb.launcher

import android.app.Application
import android.appwidget.AppWidgetManager
import android.os.UserHandle
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.withContext
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

  val viewItems: Flow<List<ViewItem>> =
    combine(
        profilesModel.activeProfile,
        widgetManager.isInEditMode,
        widgetManager.data,
        activityManager.data,
        shortcutManager.shortcuts,
      ) { activeProfile, isInEditMode, widgets, activities, shortcuts ->
        getWidgetListViewItems(widgets, activeProfile, isInEditMode) +
          getPinnedListViewItems(activities, shortcuts, activeProfile) +
          getAppListViewItems(activities, activeProfile)
      }
      .shareIn(viewModelScope, SharingStarted.Lazily)

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
          add(GroupHeaderViewItem(application.getString(R.string.pinned_items)))
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
          add(GroupHeaderViewItem(application.getString(R.string.ellipses)))
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
}
