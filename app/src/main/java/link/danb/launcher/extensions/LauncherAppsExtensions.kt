package link.danb.launcher.extensions

import android.content.ComponentName
import android.content.Intent
import android.content.pm.LauncherActivityInfo
import android.content.pm.LauncherApps
import android.content.pm.LauncherApps.ShortcutQuery
import android.content.pm.ShortcutInfo
import android.os.UserHandle
import link.danb.launcher.data.UserComponent
import link.danb.launcher.shortcuts.ConfigurableShortcutData
import link.danb.launcher.shortcuts.ShortcutData

fun LauncherApps.resolveActivity(
  componentName: ComponentName,
  userHandle: UserHandle,
): LauncherActivityInfo = resolveActivity(Intent().setComponent(componentName), userHandle)

fun LauncherApps.resolveActivity(userComponent: UserComponent): LauncherActivityInfo =
  resolveActivity(userComponent.componentName, userComponent.userHandle)

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

fun LauncherApps.resolveShortcut(shortcutData: ShortcutData): ShortcutInfo =
  resolveShortcut(shortcutData.packageName, shortcutData.shortcutId, shortcutData.userHandle)

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
