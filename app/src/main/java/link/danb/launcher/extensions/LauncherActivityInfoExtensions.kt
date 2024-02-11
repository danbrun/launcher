package link.danb.launcher.extensions

import android.content.pm.LauncherActivityInfo
import link.danb.launcher.shortcuts.ConfigurableShortcutData

fun LauncherActivityInfo.toConfigurableShortcutData(): ConfigurableShortcutData =
  ConfigurableShortcutData(componentName, user)
