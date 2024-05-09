package link.danb.launcher.gestures

import androidx.compose.ui.geometry.Rect
import dagger.hilt.android.scopes.ActivityScoped
import javax.inject.Inject
import link.danb.launcher.components.UserActivity
import link.danb.launcher.ui.LauncherIconData

data class GestureActivityIconState(val launcherIconData: LauncherIconData, val boundsInRoot: Rect)

@ActivityScoped
class GestureActivityIconStore @Inject constructor() {

  private val activityStates: MutableMap<UserActivity, MutableGestureActivityIconState> =
    mutableMapOf()

  fun getActivityIconState(userActivity: UserActivity): GestureActivityIconState? {
    if (activityStates.containsKey(userActivity)) {
      return activityStates.getValue(userActivity).toImmutableState()
    }

    for (entry in activityStates) {
      if (entry.key.packageName == userActivity.packageName) {
        return entry.value.toImmutableState()
      }
    }

    return null
  }

  fun setActivityState(
    userActivity: UserActivity,
    launcherIconData: LauncherIconData,
    boundsInRoot: Rect,
  ) {
    if (activityStates.containsKey(userActivity)) {
      activityStates.getValue(userActivity).apply {
        this.launcherIconData = launcherIconData
        this.boundsInRoot = boundsInRoot
      }
    } else {
      activityStates[userActivity] = MutableGestureActivityIconState(launcherIconData, boundsInRoot)
    }
  }

  fun clearActivityState(userActivity: UserActivity) {
    activityStates.remove(userActivity)
  }

  private class MutableGestureActivityIconState(
    var launcherIconData: LauncherIconData,
    var boundsInRoot: Rect,
  ) {
    fun toImmutableState(): GestureActivityIconState =
      GestureActivityIconState(launcherIconData, boundsInRoot)
  }
}
