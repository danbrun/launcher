package link.danb.launcher.utils

import android.os.UserHandle
import link.danb.launcher.profiles.ProfilesModel

fun UserHandle.isPersonalProfile(): Boolean = this == ProfilesModel.personalProfile
