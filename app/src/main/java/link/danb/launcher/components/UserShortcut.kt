package link.danb.launcher.components

import android.content.pm.ShortcutInfo
import android.os.UserHandle

data class UserShortcut(
  override val packageName: String,
  val shortcutId: String,
  override val userHandle: UserHandle,
) : UserComponent {

  constructor(info: ShortcutInfo) : this(info.`package`, info.id, info.userHandle)
}
