package link.danb.launcher.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import link.danb.launcher.components.UserComponent

private val LocalMutableLauncherIconBoundsBoundsMap =
  staticCompositionLocalOf<MutableMap<UserComponent, Rect>> {
    error("Missing LauncherIconPositionStore")
  }

val LocalLauncherIconBoundsMap =
  staticCompositionLocalOf<Map<UserComponent, Rect>> { error("Missing LauncherIconPositionStore") }

@Composable
fun ProvideLauncherIconBoundsMap(content: @Composable () -> Unit) {
  val iconBoundsMaps = remember { mutableMapOf<UserComponent, Rect>() }
  CompositionLocalProvider(
    LocalMutableLauncherIconBoundsBoundsMap provides iconBoundsMaps,
    LocalLauncherIconBoundsMap provides iconBoundsMaps,
  ) {
    content()
  }
}

@Composable
fun Modifier.saveIconBounds(userComponent: UserComponent): Modifier {
  val launcherIconBoundsMap = LocalMutableLauncherIconBoundsBoundsMap.current
  DisposableEffect(Unit) { onDispose { launcherIconBoundsMap.remove(userComponent) } }
  return onGloballyPositioned { launcherIconBoundsMap[userComponent] = it.boundsInRoot() }
}
