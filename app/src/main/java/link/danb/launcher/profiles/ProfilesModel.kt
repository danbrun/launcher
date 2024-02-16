package link.danb.launcher.profiles

import android.app.Application
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Process.myUserHandle
import android.os.UserHandle
import android.os.UserManager
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import link.danb.launcher.extensions.isPersonalProfile

@Singleton
class ProfilesModel
@Inject
constructor(application: Application, private val userManager: UserManager) {

  private val _workProfileData: MutableStateFlow<WorkProfileData> =
    MutableStateFlow(WorkProfileData(null, false))
  private val _activeProfile: MutableStateFlow<UserHandle> = MutableStateFlow(personalProfile)

  private val broadcastReceiver =
    object : BroadcastReceiver() {
      override fun onReceive(context: Context, intent: Intent) {
        onProfileChange()
      }
    }

  private var switchToWorkProfileWhenAvailable: Boolean = false

  val workProfileData: StateFlow<WorkProfileData> = _workProfileData.asStateFlow()
  val activeProfile: StateFlow<UserHandle> = _activeProfile.asStateFlow()

  init {
    application.registerReceiver(
      broadcastReceiver,
      IntentFilter().apply {
        addAction(Intent.ACTION_MANAGED_PROFILE_ADDED)
        addAction(Intent.ACTION_MANAGED_PROFILE_REMOVED)
        addAction(Intent.ACTION_MANAGED_PROFILE_UNAVAILABLE)
        addAction(Intent.ACTION_MANAGED_PROFILE_UNLOCKED)
      },
    )

    onProfileChange()
  }

  fun setWorkProfileEnabled(isEnabled: Boolean) {
    switchToWorkProfileWhenAvailable = isEnabled
    _workProfileData.value.user?.let { userManager.requestQuietModeEnabled(!isEnabled, it) }
  }

  fun toggleActiveProfile(showWorkProfile: Boolean? = null) {
    _activeProfile.value =
      _workProfileData.value.user?.takeIf {
        showWorkProfile ?: _activeProfile.value.isPersonalProfile
      } ?: personalProfile
  }

  @Synchronized
  private fun onProfileChange() {
    val workProfile = userManager.userProfiles.firstOrNull { !it.isPersonalProfile } ?: return
    val isWorkProfileEnabled =
      !userManager.isQuietModeEnabled(workProfile) && userManager.isUserUnlocked(workProfile)
    _workProfileData.value = WorkProfileData(workProfile, isWorkProfileEnabled)

    if (_activeProfile.value == workProfile && !isWorkProfileEnabled) {
      _activeProfile.value = personalProfile
    } else if (isWorkProfileEnabled && switchToWorkProfileWhenAvailable) {
      switchToWorkProfileWhenAvailable = false
      _activeProfile.value = workProfile
    }
  }

  data class WorkProfileData(val user: UserHandle?, val isEnabled: Boolean)

  companion object {
    val personalProfile: UserHandle = myUserHandle()
  }
}
