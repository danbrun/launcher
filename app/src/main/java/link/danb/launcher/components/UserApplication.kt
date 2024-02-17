package link.danb.launcher.components

import android.os.UserHandle

data class UserApplication(override val packageName: String, override val userHandle: UserHandle) :
  UserComponent
