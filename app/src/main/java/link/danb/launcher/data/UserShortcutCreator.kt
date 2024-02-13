package link.danb.launcher.data

import android.content.ComponentName
import android.content.pm.LauncherActivityInfo
import android.os.UserHandle

data class UserShortcutCreator(val componentName: ComponentName, val userHandle: UserHandle) {

  constructor(info: LauncherActivityInfo) : this(info.componentName, info.user)
}
