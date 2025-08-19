package link.danb.launcher.activities.details

import android.app.Application
import android.content.Intent
import androidx.core.net.toUri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.plus
import link.danb.launcher.activities.ActivityManager
import link.danb.launcher.apps.LauncherResourceProvider
import link.danb.launcher.components.UserActivity
import link.danb.launcher.components.UserShortcut
import link.danb.launcher.database.ActivityData
import link.danb.launcher.database.LauncherDatabase
import link.danb.launcher.profiles.ProfileManager
import link.danb.launcher.profiles.ProfileState
import link.danb.launcher.shortcuts.ShortcutManager

@HiltViewModel
class ActivityDetailsViewModel
@Inject
constructor(
  application: Application,
  private val activityManager: ActivityManager,
  private val launcherDatabase: LauncherDatabase,
  private val launcherResourceProvider: LauncherResourceProvider,
  private val profileManager: ProfileManager,
  private val shortcutManager: ShortcutManager,
) : AndroidViewModel(application) {

  fun getActivityDetails(userActivity: UserActivity): StateFlow<ActivityDetailsData> =
    combine(activityManager.data, profileManager.profiles) { activityDataList, profiles ->
        val activityData = activityDataList.firstOrNull { it.userActivity == userActivity }
        if (activityData != null) {
          Loaded(activityData, getShortcuts(profiles, userActivity))
        } else {
          Missing
        }
      }
      .stateIn(viewModelScope + Dispatchers.IO, SharingStarted.WhileSubscribed(), Loading)

  fun toggleAppPinned(activityData: ActivityData) {
    putActivityData(activityData.copy(isPinned = !activityData.isPinned))
  }

  fun toggleAppHidden(activityData: ActivityData) {
    putActivityData(activityData.copy(isHidden = !activityData.isHidden))
  }

  fun getUninstallIntent(userActivity: UserActivity): Intent =
    Intent(Intent.ACTION_DELETE)
      .setData("package:${userActivity.componentName.packageName}".toUri())
      .putExtra(Intent.EXTRA_USER, profileManager.getUserHandle(userActivity.profile))

  fun pinShortcut(userShortcut: UserShortcut) {
    shortcutManager.pinShortcut(userShortcut, isPinned = true)
  }

  private fun putActivityData(activityData: ActivityData) {
    viewModelScope.launch(Dispatchers.IO) { launcherDatabase.activityData().put(activityData) }
  }

  private fun getShortcuts(
    profiles: List<ProfileState>,
    userActivity: UserActivity,
  ): ImmutableList<UserShortcut> =
    if (profiles.single { it.profile == userActivity.profile }.isEnabled) {
      shortcutManager
        .getShortcuts(userActivity)
        .sortedBy { launcherResourceProvider.getLabel(it).lowercase() }
        .toImmutableList()
    } else {
      persistentListOf()
    }

  sealed interface ActivityDetailsData

  data object Loading : ActivityDetailsData

  data object Missing : ActivityDetailsData

  data class Loaded(val activityData: ActivityData, val shortcuts: ImmutableList<UserShortcut>) :
    ActivityDetailsData
}
