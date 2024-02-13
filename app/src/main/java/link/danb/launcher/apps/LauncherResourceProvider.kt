package link.danb.launcher.apps

import android.content.Context
import android.content.pm.LauncherApps
import android.graphics.drawable.AdaptiveIconDrawable
import android.graphics.drawable.Drawable
import android.os.UserHandle
import androidx.core.content.getSystemService
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import link.danb.launcher.data.UserActivity
import link.danb.launcher.data.UserApplication
import link.danb.launcher.data.UserComponent
import link.danb.launcher.data.UserShortcut
import link.danb.launcher.data.UserShortcutCreator
import link.danb.launcher.extensions.resolveActivity
import link.danb.launcher.extensions.resolveApplication
import link.danb.launcher.extensions.resolveShortcut
import link.danb.launcher.icons.AdaptiveLauncherIconDrawable
import link.danb.launcher.icons.LegacyLauncherIconDrawable

@Singleton
class LauncherResourceProvider
@Inject
constructor(@ApplicationContext private val context: Context) {

  private val launcherApps: LauncherApps by lazy { checkNotNull(context.getSystemService()) }
  private val density: Int by lazy { context.resources.displayMetrics.densityDpi }
  private val icons: MutableMap<UserComponent, Deferred<Drawable>> = mutableMapOf()

  init {
    launcherApps.registerCallback(
      LauncherAppsCallback { packageNames: List<String>, userHandle: UserHandle ->
        icons.keys.removeIf {
          it.userHandle == userHandle &&
            when (it) {
              is UserApplication -> it.packageName in packageNames
              is UserActivity -> it.componentName.packageName in packageNames
              is UserShortcut -> it.packageName in packageNames
              is UserShortcutCreator -> it.componentName.packageName in packageNames
            }
        }
      }
    )
  }

  fun getLabel(userComponent: UserComponent): String =
    when (userComponent) {
      is UserApplication -> {
        launcherApps.resolveApplication(userComponent).loadLabel(context.packageManager).toString()
      }
      is UserActivity -> {
        launcherApps.resolveActivity(userComponent).label.toString()
      }
      is UserShortcut -> {
        launcherApps.resolveShortcut(userComponent).shortLabel.toString()
      }
      is UserShortcutCreator -> {
        getLabel(UserActivity(userComponent.componentName, userComponent.userHandle))
      }
    }

  fun getIcon(userComponent: UserComponent): Deferred<Drawable> =
    synchronized(this) {
      icons.getOrPut(userComponent) {
        CoroutineScope(Dispatchers.IO).async {
          userComponent.getSourceIcon().toLauncherIcon().getBadged(userComponent.userHandle)
        }
      }
    }

  private fun UserComponent.getSourceIcon(): Drawable =
    when (this) {
      is UserApplication -> {
        launcherApps.resolveApplication(this).loadUnbadgedIcon(context.packageManager)
      }
      is UserActivity -> {
        launcherApps.resolveActivity(this).getIcon(density)
      }
      is UserShortcut -> {
        launcherApps.getShortcutIconDrawable(launcherApps.resolveShortcut(this), density)
      }
      is UserShortcutCreator -> {
        UserActivity(componentName, userHandle).getSourceIcon()
      }
    }

  private suspend fun Drawable.toLauncherIcon(): Drawable =
    if (this is AdaptiveIconDrawable) {
      AdaptiveLauncherIconDrawable(this)
    } else {
      LegacyLauncherIconDrawable.create(this)
    }

  private fun Drawable.getBadged(user: UserHandle) =
    context.packageManager.getUserBadgedIcon(this, user)
}
