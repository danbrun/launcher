package link.danb.launcher.profiles

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.UserHandle
import android.os.UserManager
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import link.danb.launcher.extensions.isPersonalProfile

@Singleton
class WorkProfileManager
@Inject
constructor(@ApplicationContext context: Context, private val userManager: UserManager) {

  val status: Flow<WorkProfileStatus> = callbackFlow {
    trySend(getWorkProfileStatus())

    val broadcastReceiver =
      object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
          trySend(getWorkProfileStatus())
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

  val workProfile: UserHandle?
    get() = userManager.userProfiles.firstOrNull { !it.isPersonalProfile }

  fun setWorkProfileEnabled(isEnabled: Boolean) {
    val workProfile = workProfile ?: return
    userManager.requestQuietModeEnabled(!isEnabled, workProfile)
  }

  private fun getWorkProfileStatus(): WorkProfileStatus {
    val workProfile = workProfile

    if (workProfile != null) {
      val isWorkProfileEnabled =
        !userManager.isQuietModeEnabled(workProfile) && userManager.isUserUnlocked(workProfile)

      return WorkProfileInstalled(workProfile, isWorkProfileEnabled)
    }

    return WorkProfileNotInstalled
  }
}

sealed interface WorkProfileStatus

data object WorkProfileNotInstalled : WorkProfileStatus

data class WorkProfileInstalled(val userHandle: UserHandle, val isEnabled: Boolean) :
  WorkProfileStatus
