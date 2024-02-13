package link.danb.launcher.extensions

import android.content.pm.LauncherActivityInfo
import link.danb.launcher.data.UserShortcutCreator

fun LauncherActivityInfo.toConfigurableShortcutData(): UserShortcutCreator =
  UserShortcutCreator(componentName, user)
