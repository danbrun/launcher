package link.danb.launcher.apps

import android.graphics.drawable.AdaptiveIconDrawable
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.platform.LocalContext
import dagger.hilt.EntryPoints
import link.danb.launcher.apps.LauncherResourceProvider.LauncherResourceProviderEntryPoint
import link.danb.launcher.components.UserComponent

private val LocalLauncherResourceProvider =
  staticCompositionLocalOf<LauncherResourceProvider> {
    error("LocalLauncherResourceProvider not present")
  }

private val LocalLauncherResourceCache =
  staticCompositionLocalOf<LauncherResourceCache> {
    error("LocalLauncherResourceCache not present")
  }

private class LauncherResourceCache {
  val icons: MutableMap<UserComponent, AdaptiveIconDrawable> = mutableMapOf()
}

@Composable
fun ProvideLauncherResources(content: @Composable () -> Unit) {
  val context = LocalContext.current
  val launcherResourceProvider =
    remember(context) {
      EntryPoints.get(context.applicationContext, LauncherResourceProviderEntryPoint::class.java)
        .launcherResourceProvider()
    }
  val launcherResourceCache = remember { LauncherResourceCache() }
  CompositionLocalProvider(
    LocalLauncherResourceProvider provides launcherResourceProvider,
    LocalLauncherResourceCache provides launcherResourceCache,
  ) {
    content()
  }
}

@Composable
fun componentLabel(userComponent: UserComponent): String? {
  val launcherResourceProvider = LocalLauncherResourceProvider.current
  return produceState<String?>(null) { value = launcherResourceProvider.getLabel(userComponent) }
    .value
}

@Composable
fun componentIcon(userComponent: UserComponent): AdaptiveIconDrawable? {
  val launcherResourceProvider = LocalLauncherResourceProvider.current
  val launcherResourceCache = LocalLauncherResourceCache.current
  return produceState(launcherResourceCache.icons[userComponent]) {
      if (value == null) {
        val icon = launcherResourceProvider.getIcon(userComponent)
        launcherResourceCache.icons[userComponent] = icon
        value = icon
      }
    }
    .value
}
