package link.danb.launcher.icons

import android.app.Application
import android.content.pm.LauncherApps
import android.graphics.drawable.AdaptiveIconDrawable
import android.graphics.drawable.Drawable
import android.os.UserHandle
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import link.danb.launcher.apps.LauncherAppsCallback
import link.danb.launcher.extensions.resolveActivity
import link.danb.launcher.extensions.resolveShortcut

@Singleton
class LauncherIconCache
@Inject
constructor(private val application: Application, private val launcherApps: LauncherApps) {

  private val icons: MutableMap<IconHandle, Deferred<Drawable>> = mutableMapOf()

  private val density: Int
    get() = application.resources.displayMetrics.densityDpi

  init {
    launcherApps.registerCallback(
      LauncherAppsCallback { packageNames: List<String>, userHandle: UserHandle ->
        icons.keys.removeIf { it.packageName in packageNames && it.userHandle == userHandle }
      }
    )
  }

  @Synchronized
  fun getIcon(iconHandle: IconHandle): Deferred<Drawable> =
    icons.getOrPut(iconHandle) {
      CoroutineScope(Dispatchers.IO).async {
        iconHandle.getSourceIcon().toLauncherIcon().getBadged(iconHandle.userHandle)
      }
    }

  private fun IconHandle.getSourceIcon(): Drawable =
    when (this) {
      is ApplicationHandle ->
        launcherApps
          .getApplicationInfo(packageName, 0, userHandle)
          .loadUnbadgedIcon(application.packageManager)
      is ComponentHandle -> launcherApps.resolveActivity(componentName, userHandle).getIcon(density)
      is ShortcutHandle ->
        launcherApps.getShortcutIconDrawable(
          launcherApps.resolveShortcut(packageName, shortcutId, userHandle),
          density,
        ) ?: ApplicationHandle(packageName, userHandle).getSourceIcon()
    }

  private suspend fun Drawable.toLauncherIcon(): Drawable =
    if (this is AdaptiveIconDrawable) {
      AdaptiveLauncherIconDrawable(this)
    } else {
      LegacyLauncherIconDrawable.create(this)
    }

  private fun Drawable.getBadged(user: UserHandle) =
    application.packageManager.getUserBadgedIcon(this, user)
}
