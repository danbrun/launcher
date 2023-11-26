package link.danb.launcher.extensions

import android.content.pm.ShortcutInfo
import link.danb.launcher.shortcuts.ShortcutData

fun ShortcutInfo.toShortcutData(): ShortcutData =
  ShortcutData(`package`, id, userHandle)
