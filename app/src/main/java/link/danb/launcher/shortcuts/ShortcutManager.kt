package link.danb.launcher.shortcuts

import android.content.Context
import android.content.Intent
import android.content.IntentSender
import android.content.pm.LauncherApps
import android.content.pm.LauncherApps.PinItemRequest
import android.content.pm.LauncherApps.ShortcutQuery
import android.graphics.Rect
import android.os.Bundle
import androidx.core.content.getSystemService
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import link.danb.launcher.components.UserComponent
import link.danb.launcher.components.UserShortcut
import link.danb.launcher.components.UserShortcutCreator
import link.danb.launcher.extensions.getConfigurableShortcuts
import link.danb.launcher.extensions.getShortcuts
import link.danb.launcher.extensions.resolveConfigurableShortcut

class ShortcutManager @Inject constructor(@ApplicationContext private val context: Context) {

  private val launcherApps: LauncherApps by lazy { checkNotNull(context.getSystemService()) }

  fun getShortcuts(userComponent: UserComponent): List<UserShortcut> =
    launcherApps
      .getShortcuts(userComponent.userHandle) {
        setQueryFlags(
          ShortcutQuery.FLAG_MATCH_DYNAMIC or
            ShortcutQuery.FLAG_MATCH_MANIFEST or
            ShortcutQuery.FLAG_MATCH_PINNED
        )
        setPackage(userComponent.packageName)
      }
      .map { UserShortcut(it) }

  fun launchShortcut(userShortcut: UserShortcut, sourceBounds: Rect, startActivityOptions: Bundle) {
    launcherApps.startShortcut(
      userShortcut.packageName,
      userShortcut.shortcutId,
      sourceBounds,
      startActivityOptions,
      userShortcut.userHandle,
    )
  }

  fun getShortcutCreators(userComponent: UserComponent): List<UserShortcutCreator> =
    launcherApps.getConfigurableShortcuts(userComponent.packageName, userComponent.userHandle).map {
      UserShortcutCreator(it)
    }

  fun getShortcutCreatorIntent(userShortcutCreator: UserShortcutCreator): IntentSender =
    checkNotNull(
      launcherApps.getShortcutConfigActivityIntent(
        launcherApps.resolveConfigurableShortcut(userShortcutCreator)
      )
    )

  fun pinShortcut(userShortcut: UserShortcut, isPinned: Boolean) {
    val currentPinnedShortcuts =
      launcherApps
        .getShortcuts(userShortcut.userHandle) {
          setQueryFlags(ShortcutQuery.FLAG_MATCH_PINNED)
          setPackage(userShortcut.packageName)
        }
        .map { it.id }

    val newPinnedShortcuts =
      if (isPinned) {
        currentPinnedShortcuts + userShortcut.shortcutId
      } else {
        currentPinnedShortcuts - userShortcut.shortcutId
      }

    launcherApps.pinShortcuts(userShortcut.packageName, newPinnedShortcuts, userShortcut.userHandle)
    context.sendBroadcast(Intent(ACTION_PINNED_SHORTCUTS_CHANGED).setPackage(context.packageName))
  }

  fun acceptPinRequest(intent: Intent) {
    val request = launcherApps.getPinItemRequest(intent) ?: return
    if (request.isValid && request.requestType == PinItemRequest.REQUEST_TYPE_SHORTCUT) {
      val shortcutInfo = request.shortcutInfo ?: return
      pinShortcut(UserShortcut(shortcutInfo), isPinned = true)
      request.accept()
    }
  }

  companion object {
    const val ACTION_PINNED_SHORTCUTS_CHANGED = "link.danb.launcher.ACTION_PINNED_SHORTCUTS_CHANGED"
  }
}
