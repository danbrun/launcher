package link.danb.launcher.profiles

enum class Profile {
  PERSONAL,
  WORK,
  PRIVATE,
}

data class ProfileState(val profile: Profile, val isEnabled: Boolean, val canToggle: Boolean)
