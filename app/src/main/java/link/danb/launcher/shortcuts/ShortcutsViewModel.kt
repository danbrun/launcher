package link.danb.launcher.shortcuts

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.LauncherApps
import android.content.pm.LauncherApps.ShortcutQuery
import androidx.core.content.ContextCompat
import androidx.core.content.getSystemService
import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import link.danb.launcher.apps.LauncherAppsCallback
import link.danb.launcher.data.UserShortcut
import link.danb.launcher.extensions.getShortcuts

@HiltViewModel
class ShortcutsViewModel @Inject constructor(@ApplicationContext context: Context) : ViewModel() {

  private val launcherApps: LauncherApps by lazy { checkNotNull(context.getSystemService()) }

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
    ContextCompat.registerReceiver(
      context,
      broadcastReceiver,
      IntentFilter().apply {
        addAction(ShortcutManager.ACTION_PINNED_SHORTCUTS_CHANGED)

        addAction(Intent.ACTION_MANAGED_PROFILE_ADDED)
        addAction(Intent.ACTION_MANAGED_PROFILE_REMOVED)
        addAction(Intent.ACTION_MANAGED_PROFILE_UNAVAILABLE)
        addAction(Intent.ACTION_MANAGED_PROFILE_UNLOCKED)
      },
      ContextCompat.RECEIVER_NOT_EXPORTED,
    )

    awaitClose {
      launcherApps.unregisterCallback(launcherAppsCallback)
      context.unregisterReceiver(broadcastReceiver)
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
