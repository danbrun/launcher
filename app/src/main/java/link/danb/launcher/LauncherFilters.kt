package link.danb.launcher

import android.os.UserHandle

sealed interface Filter

data class ProfileFilter(val profile: UserHandle, val isInEditMode: Boolean = false) : Filter

data class SearchFilter(val query: String) : Filter
