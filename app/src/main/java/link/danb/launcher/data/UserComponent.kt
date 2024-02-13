package link.danb.launcher.data

import android.os.UserHandle

sealed interface UserComponent {
  val userHandle: UserHandle
}
