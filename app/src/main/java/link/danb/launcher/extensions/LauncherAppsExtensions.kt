package link.danb.launcher.extensions

import android.content.ComponentName
import android.content.Intent
import android.content.pm.LauncherActivityInfo
import android.content.pm.LauncherApps
import android.content.pm.LauncherApps.ShortcutQuery
import android.content.pm.ShortcutInfo
import android.os.UserHandle
import link.danb.launcher.database.ActivityData
import link.danb.launcher.shortcuts.ConfigurableShortcutData
import link.danb.launcher.shortcuts.ShortcutData

fun LauncherApps.resolveActivity(
  componentName: ComponentName,
  userHandle: UserHandle
): LauncherActivityInfo = resolveActivity(Intent().setComponent(componentName), userHandle)

fun LauncherApps.resolveActivity(activityData: ActivityData): LauncherActivityInfo =
  resolveActivity(activityData.componentName, activityData.userHandle)

fun LauncherApps.resolveShortcut(shortcutData: ShortcutData): ShortcutInfo =
  getShortcuts(
      ShortcutQuery()
        .setQueryFlags(
          ShortcutQuery.FLAG_MATCH_DYNAMIC or
            ShortcutQuery.FLAG_MATCH_MANIFEST or
            ShortcutQuery.FLAG_MATCH_PINNED
        )
        .setPackage(shortcutData.packageName)
        .setShortcutIds(listOf(shortcutData.shortcutId)),
      shortcutData.userHandle
    )!!
    .first()

fun LauncherApps.resolveConfigurableShortcut(
  shortcutData: ConfigurableShortcutData
): LauncherActivityInfo =
  getShortcutConfigActivityList(shortcutData.componentName.packageName, shortcutData.userHandle)
    .first { it.componentName == shortcutData.componentName }
