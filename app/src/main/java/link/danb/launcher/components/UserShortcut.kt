package link.danb.launcher.components

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import link.danb.launcher.profiles.Profile

@Parcelize
data class UserShortcut(
  override val packageName: String,
  val shortcutId: String,
  override val profile: Profile,
) : UserComponent, Parcelable
