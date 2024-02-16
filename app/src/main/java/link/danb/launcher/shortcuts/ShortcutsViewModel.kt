package link.danb.launcher.shortcuts

import android.app.Application
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.LauncherApps
import android.content.pm.LauncherApps.ShortcutQuery
import android.os.Build
import androidx.lifecycle.AndroidViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import link.danb.launcher.apps.LauncherAppsCallback
import link.danb.launcher.data.UserShortcut
import link.danb.launcher.extensions.getShortcuts

@HiltViewModel
class ShortcutsViewModel
@Inject
constructor(application: Application, private val launcherApps: LauncherApps) :
  AndroidViewModel(application) {

  val shortcuts: Flow<List<UserShortcut>> = callbackFlow {
    trySend(getPinnedShortcuts())

    val launcherAppsCallback = LauncherAppsCallback { _, _ -> trySend(getPinnedShortcuts()) }
    val broadcastReceiver =
      object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
          trySend(getPinnedShortcuts())
        }
      }

    launcherApps.registerCallback(launcherAppsCallback)
    application.registerReceiver(
      broadcastReceiver,
      IntentFilter().apply {
        addAction(ShortcutManager.ACTION_PINNED_SHORTCUTS_CHANGED)

        addAction(Intent.ACTION_MANAGED_PROFILE_ADDED)
        addAction(Intent.ACTION_MANAGED_PROFILE_REMOVED)
        addAction(Intent.ACTION_MANAGED_PROFILE_UNAVAILABLE)
        addAction(Intent.ACTION_MANAGED_PROFILE_UNLOCKED)
      },
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        Context.RECEIVER_NOT_EXPORTED
      } else {
        0
      },
    )

    awaitClose {
      launcherApps.unregisterCallback(launcherAppsCallback)
      application.unregisterReceiver(broadcastReceiver)
    }
  }

  private fun getPinnedShortcuts(): List<UserShortcut> =
    launcherApps.profiles
      .flatMap {
        try {
          launcherApps.getShortcuts(it) { setQueryFlags(ShortcutQuery.FLAG_MATCH_PINNED) }
        } catch (exception: Exception) {
          listOf()
        }
      }
      .map { UserShortcut(it) }
}
