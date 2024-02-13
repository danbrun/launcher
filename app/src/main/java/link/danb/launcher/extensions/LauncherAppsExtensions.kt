package link.danb.launcher.extensions

import android.content.ComponentName
import android.content.Intent
import android.content.pm.LauncherActivityInfo
import android.content.pm.LauncherApps
import android.content.pm.LauncherApps.ShortcutQuery
import android.content.pm.ShortcutInfo
import android.os.UserHandle
import link.danb.launcher.data.UserActivity
import link.danb.launcher.shortcuts.ConfigurableShortcutData
import link.danb.launcher.data.UserShortcut

fun LauncherApps.resolveActivity(
  componentName: ComponentName,
  userHandle: UserHandle,
): LauncherActivityInfo = resolveActivity(Intent().setComponent(componentName), userHandle)

fun LauncherApps.resolveActivity(userActivity: UserActivity): LauncherActivityInfo =
  resolveActivity(userActivity.componentName, userActivity.userHandle)

fun LauncherApps.getShortcuts(
  userHandle: UserHandle,
  queryBuilder: ShortcutQuery.() -> Unit,
): List<ShortcutInfo> =
  if (hasShortcutHostPermission()) {
    getShortcuts(ShortcutQuery().apply(queryBuilder), userHandle) ?: listOf()
  } else {
    listOf()
  }

fun LauncherApps.resolveShortcut(packageName: String, shortcutId: String, userHandle: UserHandle) =
  getShortcuts(userHandle) {
      setQueryFlags(
        ShortcutQuery.FLAG_MATCH_DYNAMIC or
          ShortcutQuery.FLAG_MATCH_MANIFEST or
          ShortcutQuery.FLAG_MATCH_PINNED
      )
      setPackage(packageName)
      setShortcutIds(listOf(shortcutId))
    }
    .first()

fun LauncherApps.resolveShortcut(userShortcut: UserShortcut): ShortcutInfo =
  resolveShortcut(userShortcut.packageName, userShortcut.shortcutId, userShortcut.userHandle)

fun LauncherApps.getConfigurableShortcuts(
  packageName: String,
  userHandle: UserHandle,
): List<LauncherActivityInfo> =
  if (hasShortcutHostPermission()) {
    getShortcutConfigActivityList(packageName, userHandle)
  } else {
    listOf()
  }

fun LauncherApps.resolveConfigurableShortcut(
  shortcutData: ConfigurableShortcutData
): LauncherActivityInfo =
  getConfigurableShortcuts(shortcutData.componentName.packageName, shortcutData.userHandle).first {
    it.componentName == shortcutData.componentName
  }
