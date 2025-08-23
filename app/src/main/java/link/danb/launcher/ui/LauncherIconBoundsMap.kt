package link.danb.launcher.ui

import android.annotation.SuppressLint
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import link.danb.launcher.components.UserComponent

val LocalLauncherIconBoundsMap =
  staticCompositionLocalOf<LauncherIconBoundsMap> { error("Missing LauncherIconPositionStore") }

class LauncherIconBoundsMap {

  private val map: MutableMap<UserComponent, Rect> = mutableMapOf()

  fun Modifier.saveIconPosition(userComponent: UserComponent) = composed {
    DisposableEffect(Unit) { onDispose { map.remove(userComponent) } }
    onGloballyPositioned { map[userComponent] = it.boundsInRoot() }
  }

  operator fun get(userComponent: UserComponent): Rect? = map[userComponent]

  fun getValue(userComponent: UserComponent): Rect = map.getValue(userComponent)

  fun getComponents(): Set<UserComponent> = map.keys
}

@Composable
fun ProvideLauncherIconBoundsMap(content: @Composable () -> Unit) {
  val launcherIconBoundsMap = remember { LauncherIconBoundsMap() }
  CompositionLocalProvider(LocalLauncherIconBoundsMap provides launcherIconBoundsMap) { content() }
}

@SuppressLint("UnnecessaryComposedModifier")
fun Modifier.saveIconPosition(userComponent: UserComponent): Modifier = composed {
  with(LocalLauncherIconBoundsMap.current) { saveIconPosition(userComponent) }
}
