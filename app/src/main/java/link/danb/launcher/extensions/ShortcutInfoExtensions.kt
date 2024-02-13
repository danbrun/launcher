package link.danb.launcher.extensions

import android.content.pm.ShortcutInfo
import link.danb.launcher.data.UserShortcut

fun ShortcutInfo.toShortcutData(): UserShortcut =
  UserShortcut(`package`, id, userHandle)
