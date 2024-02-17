package link.danb.launcher.components

import android.os.UserHandle

sealed interface UserComponent {
  val packageName: String
  val userHandle: UserHandle
}
