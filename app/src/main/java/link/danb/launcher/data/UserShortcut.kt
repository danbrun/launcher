package link.danb.launcher.data

import android.os.UserHandle

data class UserShortcut(
  val packageName: String,
  val shortcutId: String,
  val userHandle: UserHandle
)
