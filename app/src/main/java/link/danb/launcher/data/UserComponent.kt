package link.danb.launcher.data

import android.os.UserHandle

sealed interface UserComponent {
  val packageName: String
  val userHandle: UserHandle
}
