package link.danb.launcher

import android.os.UserHandle
import link.danb.launcher.database.ActivityData
import link.danb.launcher.profiles.Profile
import link.danb.launcher.profiles.ProfileState

data class BottomBarState(
  val filters: List<BottomBarFilter>,
  val actions: List<BottomBarAction>,
  val workProfileToggle: Boolean?,
  val isSearching: Boolean,
)

data class BottomBarFilter(
  val icon: Int,
  val name: Int,
  val isChecked: Boolean,
  val filter: Filter,
)

data class BottomBarAction(val icon: Int, val name: Int, val type: Type, val profile: Profile) {
  enum class Type {
    PIN_SHORTCUT,
    PIN_WIDGET,
    SHOW_HIDDEN_APPS,
    TOGGLE_MONOCHROME,
  }
}

object BottomBarStateProducer {
  fun getBottomBarState(
    filter: Filter,
    profileStates: List<ProfileState>,
    activities: List<ActivityData>,
    userHandleProvider: (Profile) -> UserHandle,
  ): BottomBarState =
    BottomBarState(
      getBottomBarFilters(filter, profileStates),
      if (filter is ProfileFilter) {
        getBottomBarActions(filter, profileStates, activities, userHandleProvider)
      } else {
        emptyList()
      },
      workProfileToggle =
        if (filter is ProfileFilter && filter.profile == Profile.WORK) {
          profileStates.firstOrNull { it.profile == filter.profile }?.isEnabled
        } else {
          null
        },
      isSearching = filter is SearchFilter,
    )

  private fun getBottomBarFilters(filter: Filter, profileStates: List<ProfileState>) = buildList {
    if (profileStates.size == 1) {
      add(
        BottomBarFilter(
          R.drawable.baseline_apps_24,
          R.string.show_home,
          filter is ProfileFilter,
          ProfileFilter(Profile.PERSONAL),
        )
      )
    } else {
      add(
        BottomBarFilter(
          R.drawable.baseline_person_24,
          R.string.show_personal,
          filter is ProfileFilter && filter.profile == Profile.PERSONAL,
          ProfileFilter(Profile.PERSONAL),
        )
      )
      add(
        BottomBarFilter(
          R.drawable.ic_baseline_work_24,
          R.string.show_work,
          filter is ProfileFilter && filter.profile == Profile.WORK,
          ProfileFilter(Profile.WORK),
        )
      )
    }

    add(
      BottomBarFilter(
        R.drawable.ic_baseline_search_24,
        R.string.search,
        filter is SearchFilter,
        SearchFilter(""),
      )
    )
  }

  private fun getBottomBarActions(
    filter: ProfileFilter,
    profileStates: List<ProfileState>,
    activities: List<ActivityData>,
    userHandleProvider: (Profile) -> UserHandle,
  ) = buildList {
    if (
      when (filter.profile) {
        Profile.PERSONAL -> true
        Profile.WORK -> profileStates.first { it.profile == Profile.WORK }.isEnabled
      }
    ) {
      add(
        BottomBarAction(
          R.drawable.baseline_shortcut_24,
          R.string.pin_shortcut,
          BottomBarAction.Type.PIN_SHORTCUT,
          filter.profile,
        )
      )
      add(
        BottomBarAction(
          R.drawable.ic_baseline_widgets_24,
          R.string.pin_widget,
          BottomBarAction.Type.PIN_WIDGET,
          filter.profile,
        )
      )
    }

    add(
      BottomBarAction(
        R.drawable.baseline_style_24,
        R.string.toggle_monochrome,
        BottomBarAction.Type.TOGGLE_MONOCHROME,
        filter.profile,
      )
    )

    if (activities.any { it.isHidden && it.userActivity.profile == filter.profile }) {
      add(
        BottomBarAction(
          R.drawable.ic_baseline_visibility_24,
          R.string.show_hidden,
          BottomBarAction.Type.SHOW_HIDDEN_APPS,
          filter.profile,
        )
      )
    }
  }
}
