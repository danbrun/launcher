package link.danb.launcher.components

import android.content.ComponentName
import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.Serializable
import link.danb.launcher.profiles.Profile

@Parcelize
@Serializable
data class UserActivity(
  val componentName: @Serializable(with = ComponentNameSerializer::class) ComponentName,
  override val profile: Profile,
) : UserComponent, Parcelable {

  override val packageName: String
    get() = componentName.packageName
}
