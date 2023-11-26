package link.danb.launcher.shortcuts

import android.os.UserHandle

data class ShortcutData(
  val packageName: String,
  val shortcutId: String,
  val userHandle: UserHandle
)
