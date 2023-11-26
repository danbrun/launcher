package link.danb.launcher.extensions

import android.content.pm.LauncherActivityInfo
import link.danb.launcher.database.ActivityData
import link.danb.launcher.shortcuts.ConfigurableShortcutData

fun LauncherActivityInfo.toDefaultActivityData(): ActivityData =
  ActivityData(componentName, user, isPinned = false, isHidden = false, tags = setOf())

fun LauncherActivityInfo.toConfigurableShortcutData(): ConfigurableShortcutData =
  ConfigurableShortcutData(componentName, user)
