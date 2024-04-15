package link.danb.launcher.components

import android.content.pm.ShortcutInfo
import android.os.Parcelable
import android.os.UserHandle
import kotlinx.parcelize.Parcelize

@Parcelize
data class UserShortcut(
  override val packageName: String,
  val shortcutId: String,
  override val userHandle: UserHandle,
) : UserComponent, Parcelable {

  constructor(info: ShortcutInfo) : this(info.`package`, info.id, info.userHandle)
}
