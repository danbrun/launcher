package link.danb.launcher

import link.danb.launcher.database.ActivityData
import link.danb.launcher.profiles.Profile
import link.danb.launcher.profiles.ProfileState

data class BottomBarAction(val icon: Int, val name: Int, val type: Type) {
  enum class Type {
    PIN_SHORTCUT,
    PIN_WIDGET,
    SHOW_HIDDEN_APPS,
    TOGGLE_MONOCHROME,
  }
}

object BottomBarStateProducer {
  fun getBottomBarActions(
    profile: Profile,
    profileState: ProfileState,
    activities: List<ActivityData>,
  ) = buildList {
    if (profileState.isEnabled) {
      add(
        BottomBarAction(
          R.drawable.baseline_shortcut_24,
          R.string.pin_shortcut,
          BottomBarAction.Type.PIN_SHORTCUT,
        )
      )
      add(
        BottomBarAction(
          R.drawable.ic_baseline_widgets_24,
          R.string.pin_widget,
          BottomBarAction.Type.PIN_WIDGET,
        )
      )
    }

    add(
      BottomBarAction(
        R.drawable.baseline_style_24,
        R.string.toggle_monochrome,
        BottomBarAction.Type.TOGGLE_MONOCHROME,
      )
    )

    if (activities.any { it.isHidden && it.userActivity.profile == profile }) {
      add(
        BottomBarAction(
          R.drawable.ic_baseline_visibility_24,
          R.string.show_hidden,
          BottomBarAction.Type.SHOW_HIDDEN_APPS,
        )
      )
    }
  }
}
