package link.danb.launcher

import android.os.Process.myUserHandle
import android.os.UserHandle
import link.danb.launcher.profiles.WorkProfileInstalled

sealed interface Filter

data class ProfileFilter(val profile: UserHandle, val isInEditMode: Boolean = false) : Filter {
  companion object {
    val personalFilter: ProfileFilter
      get() = ProfileFilter(myUserHandle())

    val WorkProfileInstalled.workFilter: ProfileFilter
      get() = ProfileFilter(userHandle)
  }
}

data class SearchFilter(val query: String) : Filter
