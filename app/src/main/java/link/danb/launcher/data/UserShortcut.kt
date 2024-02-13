package link.danb.launcher.data

import android.content.pm.ShortcutInfo
import android.os.UserHandle

data class UserShortcut(
  val packageName: String,
  val shortcutId: String,
  val userHandle: UserHandle,
) {

  constructor(info: ShortcutInfo) : this(info.`package`, info.id, info.userHandle)
}
