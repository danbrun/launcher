package link.danb.launcher.extensions

import android.os.UserHandle
import link.danb.launcher.profiles.ProfilesModel

val UserHandle.isPersonalProfile: Boolean
  get() = this == ProfilesModel.personalProfile
