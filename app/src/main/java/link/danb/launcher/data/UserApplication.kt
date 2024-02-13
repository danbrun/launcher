package link.danb.launcher.data

import android.os.UserHandle

data class UserApplication(val packageName: String, override val userHandle: UserHandle) :
  UserComponent
