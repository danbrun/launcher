package link.danb.launcher.extensions

import android.os.Process.myUserHandle
import android.os.UserHandle

val UserHandle.isPersonalProfile: Boolean
  get() = this == myUserHandle()
