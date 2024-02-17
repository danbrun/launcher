package link.danb.launcher.components

import android.content.ComponentName
import android.content.pm.LauncherActivityInfo
import android.os.UserHandle

data class UserShortcutCreator(
  val componentName: ComponentName,
  override val userHandle: UserHandle,
) : UserComponent {

  constructor(info: LauncherActivityInfo) : this(info.componentName, info.user)

  override val packageName: String
    get() = componentName.packageName
}
