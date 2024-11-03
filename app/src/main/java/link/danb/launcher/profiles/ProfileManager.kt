package link.danb.launcher.profiles

import android.app.role.RoleManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.Process
import android.os.UserHandle
import android.os.UserManager
import androidx.core.content.getSystemService
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.collections.immutable.ImmutableMap
import kotlinx.collections.immutable.toImmutableMap
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.stateIn

@Singleton
class ProfileManager @Inject constructor(@ApplicationContext context: Context) {

  private val userManager: UserManager = checkNotNull(context.getSystemService())
  private val roleManager: RoleManager = checkNotNull(context.getSystemService())

  val profiles: Flow<ImmutableMap<Profile, ProfileState>> =
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

  fun setProfileEnabled(profile: Profile, isEnabled: Boolean) {
    when (profile) {
      Profile.PERSONAL -> {}
      Profile.WORK -> {
        val userHandle = getUserHandle(profile)
        if (userHandle != null && isEnabled(userHandle) != isEnabled) {
          userManager.requestQuietModeEnabled(!isEnabled, userHandle)
        }
      }
    }
  }

  private fun getProfiles(): ImmutableMap<Profile, ProfileState> =
    userManager.userProfiles
      .associate { Pair(getProfile(it), getProfileState(it)) }
      .toImmutableMap()

  private fun getProfileState(userHandle: UserHandle): ProfileState =
    when (getProfile(userHandle)) {
      Profile.PERSONAL -> ProfileState(isEnabled = true, canToggle = false)
      Profile.WORK ->
        ProfileState(
          isEnabled(userHandle),
          Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q &&
            roleManager.isRoleHeld(RoleManager.ROLE_HOME),
        )
    }

  fun isEnabled(userHandle: UserHandle) =
    !userManager.isQuietModeEnabled(userHandle) && userManager.isUserUnlocked(userHandle)
}
