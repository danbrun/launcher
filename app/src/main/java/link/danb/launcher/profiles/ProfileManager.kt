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

  val profileStates: StateFlow<List<ProfileState>> =
    callbackFlow {
        val broadcastReceiver =
          object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
              trySend(getProfileStates())
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
      .stateIn(MainScope(), SharingStarted.WhileSubscribed(), initialValue = getProfileStates())

  fun getUserHandle(profile: Profile): UserHandle? =
    when (profile) {
      Profile.PERSONAL -> Process.myUserHandle()
      Profile.WORK -> userManager.userProfiles.firstOrNull { it != Process.myUserHandle() }
    }

  fun getProfile(userHandle: UserHandle): Profile =
    when (userHandle) {
      Process.myUserHandle() -> Profile.PERSONAL
      else -> Profile.WORK
    }

  fun setProfileEnabled(profile: Profile, isEnabled: Boolean) {
    when (profile) {
      Profile.PERSONAL -> {}
      Profile.WORK -> {
        userManager.requestQuietModeEnabled(!isEnabled, getUserHandle(profile)!!)
      }
    }
  }

  private fun getProfileStates(): List<ProfileState> =
    userManager.userProfiles.map {
      when (getProfile(it)) {
        Profile.PERSONAL -> ProfileState(Profile.PERSONAL, true)
        Profile.WORK -> {
          val isWorkProfileEnabled =
            !userManager.isQuietModeEnabled(it) && userManager.isUserUnlocked(it)
          ProfileState(Profile.WORK, isWorkProfileEnabled)
        }
      }
    }
}

data class ProfileState(val profile: Profile, val isEnabled: Boolean)
