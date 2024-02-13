package link.danb.launcher.data

import android.content.ComponentName
import android.content.pm.LauncherActivityInfo
import android.os.UserHandle

data class UserShortcutCreator(
  val componentName: ComponentName,
  override val userHandle: UserHandle,
) : UserComponent {

  constructor(info: LauncherActivityInfo) : this(info.componentName, info.user)
}
