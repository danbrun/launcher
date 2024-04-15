package link.danb.launcher.components

import android.content.ComponentName
import android.content.pm.LauncherActivityInfo
import android.os.Parcelable
import android.os.UserHandle
import kotlinx.parcelize.Parcelize

@Parcelize
data class UserShortcutCreator(
  val componentName: ComponentName,
  override val userHandle: UserHandle,
) : UserComponent, Parcelable {

  constructor(info: LauncherActivityInfo) : this(info.componentName, info.user)

  override val packageName: String
    get() = componentName.packageName
}
