package link.danb.launcher.components

import android.content.ComponentName
import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import link.danb.launcher.profiles.Profile

@Parcelize
data class UserShortcutCreator(val componentName: ComponentName, override val profile: Profile) :
  UserComponent, Parcelable {

  override val packageName: String
    get() = componentName.packageName
}
