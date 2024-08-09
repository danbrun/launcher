package link.danb.launcher.gestures

import androidx.compose.ui.geometry.Rect
import dagger.hilt.android.scopes.ActivityScoped
import javax.inject.Inject
import link.danb.launcher.components.UserActivity
import link.danb.launcher.ui.LauncherIconData

data class GestureActivityIconState(
  val userActivity: UserActivity,
  val launcherIconData: LauncherIconData,
  val boundsInRoot: Rect,
)

@ActivityScoped
class GestureActivityIconStore @Inject constructor() {

  private val activityStates: MutableMap<UserActivity, MutableGestureActivityIconState> =
    mutableMapOf()

  fun getActivityIconState(userActivity: UserActivity): GestureActivityIconState? {
    if (activityStates.containsKey(userActivity)) {
      return activityStates.getValue(userActivity).toImmutableState(userActivity)
    }

    for (entry in activityStates) {
      if (entry.key.packageName == userActivity.packageName) {
        return entry.value.toImmutableState(entry.key)
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
    fun toImmutableState(userActivity: UserActivity): GestureActivityIconState =
      GestureActivityIconState(userActivity, launcherIconData, boundsInRoot)
  }
}
