package link.danb.launcher.profiles

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Process
import android.os.UserHandle
import android.os.UserManager
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.stateIn

@Singleton
class ProfileManager
@Inject
constructor(@ApplicationContext context: Context, private val userManager: UserManager) {

  val profiles: StateFlow<Profiles> =
    callbackFlow {
        val broadcastReceiver =
          object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
              trySend(getProfileStatus())
            }
          }

        context.registerReceiver(
          broadcastReceiver,
          IntentFilter().apply {
            addAction(Intent.ACTION_MANAGED_PROFILE_ADDED)
            addAction(Intent.ACTION_MANAGED_PROFILE_REMOVED)
            addAction(Intent.ACTION_MANAGED_PROFILE_UNAVAILABLE)
            addAction(Intent.ACTION_MANAGED_PROFILE_UNLOCKED)
          },
        )
        awaitClose { context.unregisterReceiver(broadcastReceiver) }
      }
      .stateIn(MainScope(), SharingStarted.WhileSubscribed(), initialValue = getProfileStatus())

  fun setWorkProfileEnabled(isEnabled: Boolean) {
    val currentProfiles = profiles.value
    if (currentProfiles is PersonalAndWorkProfiles) {
      userManager.requestQuietModeEnabled(!isEnabled, currentProfiles.workProfile)
    }
  }

  private fun getProfileStatus(): Profiles {
    val workProfile = userManager.userProfiles.firstOrNull { it != Process.myUserHandle() }

    if (workProfile != null) {
      val isWorkProfileEnabled =
        !userManager.isQuietModeEnabled(workProfile) && userManager.isUserUnlocked(workProfile)

      return PersonalAndWorkProfiles(Process.myUserHandle(), workProfile, isWorkProfileEnabled)
    }

    return PersonalProfile(Process.myUserHandle())
  }
}

sealed interface Profiles {
  val personal: UserHandle
}

data class PersonalProfile(override val personal: UserHandle) : Profiles

data class PersonalAndWorkProfiles(
  override val personal: UserHandle,
  val workProfile: UserHandle,
  val isWorkEnabled: Boolean,
) : Profiles
