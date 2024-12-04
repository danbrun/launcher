package link.danb.launcher

import android.app.Activity
import android.app.Application
import android.app.role.RoleManager
import android.content.Intent
import android.os.Build
import androidx.core.content.getSystemService
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.plus
import link.danb.launcher.activities.ActivityManager
import link.danb.launcher.activities.DefaultActivityLifecycleCallbacks
import link.danb.launcher.profiles.Profile
import link.danb.launcher.profiles.ProfileManager
import link.danb.launcher.settings.SettingsRepository

@HiltViewModel
class MoreActionsViewModel
@Inject
constructor(
  application: Application,
  private val activityManager: ActivityManager,
  private val profileManager: ProfileManager,
  private val settingsRepository: SettingsRepository,
) : AndroidViewModel(application) {

  private val roleManager: RoleManager by lazy { checkNotNull(application.getSystemService()) }

  private val _canRequestHomeRole: Boolean
    get() =
      Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q &&
        roleManager.isRoleAvailable(RoleManager.ROLE_HOME) &&
        !roleManager.isRoleHeld(RoleManager.ROLE_HOME)

  fun getCanPinItems(profile: Profile): StateFlow<Boolean> =
    profileManager.profiles
      .map { profiles -> profiles.single { it.profile == profile }.isEnabled }
      .stateIn(
        viewModelScope + Dispatchers.IO,
        SharingStarted.WhileSubscribed(),
        initialValue = false,
      )

  fun getHasHiddenApps(profile: Profile): StateFlow<Boolean> =
    activityManager.data
      .map { activities -> activities.any { it.userActivity.profile == profile && it.isHidden } }
      .stateIn(
        viewModelScope + Dispatchers.IO,
        SharingStarted.WhileSubscribed(),
        initialValue = false,
      )

  val canRequestHomeRole: StateFlow<Boolean> =
    callbackFlow {
        send(_canRequestHomeRole)

        val callback =
          object : DefaultActivityLifecycleCallbacks {
            override fun onActivityResumed(p0: Activity) {
              trySend(_canRequestHomeRole)
            }
          }

        application.registerActivityLifecycleCallbacks(callback)

        awaitClose { application.unregisterActivityLifecycleCallbacks(callback) }
      }
      .stateIn(
        viewModelScope + Dispatchers.IO,
        SharingStarted.WhileSubscribed(),
        initialValue = false,
      )

  fun toggleMonochromeIcons() {
    viewModelScope.launch { settingsRepository.toggleUseMonochromeIcons() }
  }

  val shouldLaunchDefaultAppsSettings: Boolean
    get() =
      Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q &&
        roleManager.isRoleAvailable(RoleManager.ROLE_HOME) &&
        !roleManager.isRoleHeld(RoleManager.ROLE_HOME)

  val homeRoleIntent: Intent?
    get() =
      if (_canRequestHomeRole) {
        roleManager.createRequestRoleIntent(RoleManager.ROLE_HOME)
      } else {
        null
      }
}
