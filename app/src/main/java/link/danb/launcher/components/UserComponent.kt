package link.danb.launcher.components

import link.danb.launcher.profiles.Profile

sealed interface UserComponent {
  val packageName: String
  val profile: Profile
}
