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
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.stateIn

@Singleton
class ProfileManager
@Inject
constructor(@ApplicationContext context: Context, private val userManager: UserManager) {

  val profiles: Flow<Map<Profile, ProfileState>> =
    callbackFlow {
        send(getProfiles())

        val broadcastReceiver =
          object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
              trySend(getProfiles())
            }
          }

        context.registerReceiver(
          broadcastReceiver,
          IntentFilter().apply {
            addAction(Intent.ACTION_MANAGED_PROFILE_ADDED)
            addAction(Intent.ACTION_MANAGED_PROFILE_REMOVED)
            addAction(Intent.ACTION_MANAGED_PROFILE_AVAILABLE)
            addAction(Intent.ACTION_MANAGED_PROFILE_UNAVAILABLE)
            addAction(Intent.ACTION_MANAGED_PROFILE_UNLOCKED)
          },
        )

        awaitClose { context.unregisterReceiver(broadcastReceiver) }
      }
      .stateIn(MainScope(), SharingStarted.WhileSubscribed(), getProfiles())

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

  fun setProfileState(profile: Profile, profileState: ProfileState) {
    when (profile) {
      Profile.PERSONAL -> {}
      Profile.WORK -> {
        val userHandle = getUserHandle(profile)
        if (userHandle != null) {
          userManager.requestQuietModeEnabled(
            when (profileState) {
              ProfileState.ENABLED -> false
              ProfileState.DISABLED -> true
            },
            userHandle,
          )
        }
      }
    }
  }

  private fun getProfiles(): Map<Profile, ProfileState> =
    userManager.userProfiles.associate { Pair(getProfile(it), getProfileState(it)) }

  private fun getProfileState(userHandle: UserHandle): ProfileState =
    when (getProfile(userHandle)) {
      Profile.PERSONAL -> ProfileState.ENABLED
      Profile.WORK ->
        if (isEnabled(userHandle)) {
          ProfileState.ENABLED
        } else {
          ProfileState.DISABLED
        }
    }

  fun isEnabled(userHandle: UserHandle) =
    !userManager.isQuietModeEnabled(userHandle) && userManager.isUserUnlocked(userHandle)
}
