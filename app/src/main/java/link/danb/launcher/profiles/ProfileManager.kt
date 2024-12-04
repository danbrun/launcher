package link.danb.launcher.profiles

import android.app.role.RoleManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.LauncherApps
import android.os.Build
import android.os.Process
import android.os.UserHandle
import android.os.UserManager
import androidx.core.content.getSystemService
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@Singleton
class ProfileManager @Inject constructor(@ApplicationContext context: Context) {

  private val launcherApps: LauncherApps = checkNotNull(context.getSystemService())
  private val userManager: UserManager = checkNotNull(context.getSystemService())
  private val roleManager: RoleManager = checkNotNull(context.getSystemService())

  val profiles: Flow<ImmutableList<ProfileState>> =
    callbackFlow {
        send(getProfileStates())

        val broadcastReceiver =
          object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
              launch { send(getProfileStates()) }
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
      .stateIn(MainScope(), SharingStarted.WhileSubscribed(), getProfileStates())

  fun getProfile(userHandle: UserHandle): Profile? =
    if (userHandle == Process.myUserHandle()) {
      Profile.PERSONAL
    } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.VANILLA_ICE_CREAM) {
      when (launcherApps.getLauncherUserInfo(userHandle)?.userType) {
        UserManager.USER_TYPE_PROFILE_MANAGED -> Profile.WORK
        UserManager.USER_TYPE_PROFILE_PRIVATE -> Profile.PRIVATE
        else -> null
      }
    } else {
      Profile.WORK
    }

  fun getUserHandle(profile: Profile): UserHandle? =
    launcherApps.profiles.associateBy { getProfile(it) }[profile]

  fun setProfileEnabled(profile: Profile, isEnabled: Boolean) {
    when (profile) {
      Profile.PERSONAL -> {
        check(isEnabled) { "Attempted to disable personal profile." }
      }
      Profile.WORK,
      Profile.PRIVATE -> {
        val userHandle = getUserHandle(profile)
        if (userHandle != null && isEnabled(userHandle) != isEnabled) {
          userManager.requestQuietModeEnabled(!isEnabled, userHandle)
        }
      }
    }
  }

  private fun getProfileStates(): ImmutableList<ProfileState> =
    launcherApps.profiles.mapNotNull { getProfileState(it) }.toImmutableList()

  private fun getProfileState(userHandle: UserHandle): ProfileState? {
    val profile = getProfile(userHandle)
    return when (profile) {
      Profile.PERSONAL -> ProfileState(profile, isEnabled = true, canToggle = false)
      Profile.WORK,
      Profile.PRIVATE ->
        ProfileState(
          profile,
          isEnabled(userHandle),
          Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q &&
            roleManager.isRoleHeld(RoleManager.ROLE_HOME),
        )
      null -> null
    }
  }

  fun isEnabled(userHandle: UserHandle) =
    !userManager.isQuietModeEnabled(userHandle) && userManager.isUserUnlocked(userHandle)
}
