package link.danb.launcher.profiles

enum class Profile {
  PERSONAL,
  WORK,
}

data class ProfileState(val isEnabled: Boolean, val canToggle: Boolean)
