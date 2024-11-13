package link.danb.launcher.components

import androidx.compose.runtime.Immutable
import link.danb.launcher.profiles.Profile

@Immutable
sealed interface UserComponent {
  val packageName: String
  val profile: Profile
}
