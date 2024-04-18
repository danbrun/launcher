package link.danb.launcher

import android.os.UserHandle
import link.danb.launcher.database.ActivityData
import link.danb.launcher.profiles.PersonalAndWorkProfiles
import link.danb.launcher.profiles.PersonalProfile
import link.danb.launcher.profiles.Profiles

data class BottomBarState(
  val filters: List<BottomBarFilter>,
  val actions: List<BottomBarAction>,
  val workProfileToggle: Boolean?,
  val searchQuery: String?,
)

data class BottomBarFilter(
  val icon: Int,
  val name: Int,
  val isChecked: Boolean,
  val filter: Filter,
)

data class BottomBarAction(val icon: Int, val name: Int, val type: Type, val user: UserHandle) {
  enum class Type {
    PIN_SHORTCUT,
    PIN_WIDGET,
    SHOW_HIDDEN_APPS,
  }
}

object BottomBarStateProducer {
  fun getBottomBarState(
    filter: Filter,
    profiles: Profiles,
    activities: List<ActivityData>,
  ): BottomBarState =
    BottomBarState(
      getBottomBarFilters(filter, profiles),
      if (filter is ProfileFilter) {
        getBottomBarActions(filter, profiles, activities)
      } else {
        emptyList()
      },
      workProfileToggle =
        when (profiles) {
          is PersonalProfile -> null
          is PersonalAndWorkProfiles ->
            if (filter is ProfileFilter && filter.profile == profiles.workProfile)
              profiles.isWorkEnabled
            else null
        },
      searchQuery = if (filter is SearchFilter) filter.query else null,
    )

  private fun getBottomBarFilters(filter: Filter, profiles: Profiles) = buildList {
    when (profiles) {
      is PersonalProfile -> {
        add(
          BottomBarFilter(
            R.drawable.baseline_apps_24,
            R.string.show_home,
            filter is ProfileFilter,
            ProfileFilter(profiles.personal),
          )
        )
      }
      is PersonalAndWorkProfiles -> {
        add(
          BottomBarFilter(
            R.drawable.baseline_person_24,
            R.string.show_personal,
            filter is ProfileFilter && filter.profile == profiles.personal,
            ProfileFilter(profiles.personal),
          )
        )
        add(
          BottomBarFilter(
            R.drawable.ic_baseline_work_24,
            R.string.show_work,
            filter is ProfileFilter && filter.profile == profiles.workProfile,
            ProfileFilter(profiles.workProfile),
          )
        )
      }
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
    profiles: Profiles,
    activities: List<ActivityData>,
  ) = buildList {
    if (
      filter.profile == profiles.personal ||
        (profiles is PersonalAndWorkProfiles &&
          filter.profile == profiles.workProfile &&
          profiles.isWorkEnabled)
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

    if (activities.any { it.isHidden && it.userActivity.userHandle == filter.profile }) {
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
