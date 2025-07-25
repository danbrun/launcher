package link.danb.launcher

import android.app.Application
import android.appwidget.AppWidgetManager
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.util.Base64
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.core.net.toUri
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
import kotlinx.coroutines.launch
import kotlinx.coroutines.plus
import link.danb.launcher.activities.ActivityManager
import link.danb.launcher.apps.LauncherResourceProvider
import link.danb.launcher.components.UserActivity
import link.danb.launcher.components.UserShortcut
import link.danb.launcher.database.ActivityData
import link.danb.launcher.database.LauncherDatabase
import link.danb.launcher.database.TabData
import link.danb.launcher.database.WidgetData
import link.danb.launcher.profiles.Profile
import link.danb.launcher.profiles.ProfileManager
import link.danb.launcher.profiles.ProfileState
import link.danb.launcher.settings.SettingsRepository
import link.danb.launcher.shortcuts.ShortcutManager
import link.danb.launcher.ui.LauncherTileData
import link.danb.launcher.widgets.WidgetManager

sealed interface ViewItem {
  val key: String
}

data class WidgetViewItem(
  val widgetData: WidgetData,
  val sizeRange: IntRange,
  val isConfigurable: Boolean,
) : ViewItem {
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
  val isSearching: Boolean,
) : IconTileViewItem {
  override val key: String
    get() {
      val component =
        if (isSearching && userActivity.profile != Profile.PERSONAL) userActivity
        else userActivity.componentName
      return "$component,$isPinned"
    }
}

data class TabViewItem(val id: Int, val uri: Uri?, val name: String, val icon: ImageBitmap?) :
  ViewItem {
  override val key: String = id.toString()
}

@HiltViewModel
class LauncherViewModel
@Inject
constructor(
  activityManager: ActivityManager,
  private val application: Application,
  private val appWidgetManager: AppWidgetManager,
  private val launcherDatabase: LauncherDatabase,
  private val launcherResourceProvider: LauncherResourceProvider,
  private val profileManager: ProfileManager,
  private val shortcutManager: ShortcutManager,
  widgetManager: WidgetManager,
  settingsRepository: SettingsRepository,
) : AndroidViewModel(application) {

  private val _searchQuery = MutableStateFlow<String?>(null)
  private val _profile = MutableStateFlow(Profile.PERSONAL)

  val searchQuery: StateFlow<String?> = _searchQuery.asStateFlow()
  val profile: StateFlow<Profile> = _profile.asStateFlow()

  val profiles: StateFlow<ImmutableList<ProfileState>> =
    profileManager.profiles.stateIn(
      viewModelScope + Dispatchers.IO,
      SharingStarted.WhileSubscribed(),
      persistentListOf(),
    )

  @OptIn(FlowPreview::class)
  val viewItems: StateFlow<ImmutableList<ViewItem>> =
    combine(
        activityManager.data,
        shortcutManager.shortcuts,
        widgetManager.data,
        launcherDatabase.tabData().get(),
        combine(searchQuery, profile, ::Filter),
        ::CombinedData,
      )
      .debounce(100)
      .map {
        buildList {
            if (it.filter.searchQuery == null) {
              addWidgetListViewItems(it.widgets, it.filter.profile)
              if (it.filter.profile == Profile.PERSONAL) {
                addTabTileViewItems(it.tabState)
              }
              addPinnedListViewItems(it.activities, it.shortcuts, it.filter.profile)
            }
            addAppListViewItems(it.activities, it.filter.searchQuery, it.filter.profile)
          }
          .toImmutableList()
      }
      .stateIn(
        viewModelScope + Dispatchers.IO,
        SharingStarted.WhileSubscribed(),
        persistentListOf(),
      )

  val useMonochromeIcons: StateFlow<Boolean> =
    settingsRepository.useMonochromeIcons.stateIn(
      viewModelScope,
      SharingStarted.WhileSubscribed(),
      initialValue = false,
    )

  fun setSearchQuery(value: String?) {
    _searchQuery.value = value
  }

  fun setProfile(profile: Profile, isEnabled: Boolean) {
    profileManager.setProfileEnabled(profile, isEnabled)
    _profile.value = profile
  }

  fun unpinShortcut(userShortcut: UserShortcut) {
    shortcutManager.pinShortcut(userShortcut, isPinned = false)
  }

  fun clearTab(tabId: Int) {
    viewModelScope.launch { launcherDatabase.tabData().delete(tabId) }
  }

  private fun MutableList<ViewItem>.addWidgetListViewItems(
    widgets: List<WidgetData>,
    profile: Profile,
  ) {
    for (widget in widgets) {
      val providerInfo = appWidgetManager.getAppWidgetInfo(widget.widgetId)
      if (providerInfo.profile == profileManager.getUserHandle(profile)) {
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

        add(WidgetViewItem(widget, minHeight..maxHeight, providerInfo.configure != null))
      }
    }
  }

  private fun MutableList<ViewItem>.addTabTileViewItems(tabs: List<TabData>) {
    if (tabs.isEmpty()) return
    add(GroupHeaderViewItem(application.getString(R.string.tabs)))
    for (tab in tabs) {
      val uri = tab.url.takeIf { it.startsWith("http") }?.toUri()
      val icon =
        tab.capture?.let {
          val bytes = Base64.decode(it.split(",")[1], Base64.DEFAULT)
          BitmapFactory.decodeByteArray(bytes, 0, bytes.size).asImageBitmap()
        }
      add(TabViewItem(tab.id, uri, tab.title, icon))
    }
  }

  private suspend fun MutableList<ViewItem>.addPinnedListViewItems(
    activities: List<ActivityData>,
    shortcuts: List<UserShortcut>,
    profile: Profile,
  ) {
    val pinnedItems =
      merge(
          activities
            .asFlow()
            .filter { it.isPinned && it.userActivity.profile == profile }
            .map { getActivityTileItem(it, isPinned = true, isSearching = false) },
          shortcuts
            .asFlow()
            .filter { it.profile == profile }
            .map { ShortcutViewItem(it, launcherResourceProvider.getTileDataWithCache(it)) },
        )
        .toList()
        .sortedBy { it.launcherTileData.name.lowercase() }

    if (pinnedItems.isNotEmpty()) {
      add(GroupHeaderViewItem(application.getString(R.string.pinned_items)))
      addAll(pinnedItems)
    }
  }

  private suspend fun MutableList<ViewItem>.addAppListViewItems(
    activities: List<ActivityData>,
    searchQuery: String?,
    profile: Profile,
  ) {
    val (alphabetical, miscellaneous) =
      activities
        .asFlow()
        .filter {
          if (searchQuery == null) {
            !it.isHidden && it.userActivity.profile == profile
          } else {
            true
          }
        }
        .map { getActivityTileItem(it, isPinned = false, searchQuery != null) }
        .filter {
          if (searchQuery != null) {
            it.launcherTileData.name.lowercase().contains(searchQuery.lowercase().trim())
          } else {
            true
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

  private suspend fun getActivityTileItem(
    activityData: ActivityData,
    isPinned: Boolean,
    isSearching: Boolean,
  ) =
    ActivityViewItem(
      activityData.userActivity,
      launcherResourceProvider.getTileDataWithCache(activityData.userActivity),
      isPinned,
      isSearching,
    )

  private data class CombinedData(
    val activities: List<ActivityData>,
    val shortcuts: List<UserShortcut>,
    val widgets: List<WidgetData>,
    val tabState: List<TabData>,
    val filter: Filter,
  )

  private data class Filter(val searchQuery: String?, val profile: Profile)
}
