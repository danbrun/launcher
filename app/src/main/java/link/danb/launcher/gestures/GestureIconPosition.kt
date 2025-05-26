package link.danb.launcher.gestures

import androidx.activity.compose.LocalActivity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.android.components.ActivityComponent
import link.danb.launcher.ActivityViewItem

@Composable
fun Modifier.gestureIconPosition(item: ActivityViewItem): Modifier {
  val activity = checkNotNull(LocalActivity.current)
  val gestureActivityIconStore =
    remember(activity) {
      EntryPointAccessors.fromActivity(activity, GestureIconEntryPoint::class.java)
        .gestureActivityIconStore()
    }

  DisposableEffect(Unit) {
    onDispose { gestureActivityIconStore.clearActivityState(item.userActivity) }
  }

  return onGloballyPositioned {
    gestureActivityIconStore.setActivityState(
      item.userActivity,
      item.launcherTileData.launcherIconData,
      it.boundsInRoot(),
    )
  }
}

@EntryPoint
@InstallIn(ActivityComponent::class)
interface GestureIconEntryPoint {
  fun gestureActivityIconStore(): GestureActivityIconStore
}
