package link.danb.launcher

import link.danb.launcher.profiles.Profile

sealed interface Filter

data class ProfileFilter(val profile: Profile, val isInEditMode: Boolean = false) : Filter

data class SearchFilter(val query: String) : Filter
