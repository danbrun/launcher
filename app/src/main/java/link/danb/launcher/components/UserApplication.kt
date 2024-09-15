package link.danb.launcher.components

import link.danb.launcher.profiles.Profile

data class UserApplication(override val packageName: String, override val profile: Profile) :
  UserComponent
