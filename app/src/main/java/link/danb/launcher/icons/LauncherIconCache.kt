package link.danb.launcher.icons

import android.app.Application
import android.content.pm.ApplicationInfo
import android.content.pm.LauncherApps
import android.graphics.drawable.AdaptiveIconDrawable
import android.graphics.drawable.Drawable
import android.os.UserHandle
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import link.danb.launcher.apps.LauncherAppsCallback
import link.danb.launcher.database.ActivityData
import link.danb.launcher.extensions.resolveActivity
import link.danb.launcher.extensions.resolveConfigurableShortcut
import link.danb.launcher.extensions.resolveShortcut
import link.danb.launcher.shortcuts.ConfigurableShortcutData
import link.danb.launcher.shortcuts.ShortcutData

@Singleton
class LauncherIconCache
@Inject
constructor(private val application: Application, private val launcherApps: LauncherApps) {

  private val icons: MutableMap<Any, Drawable> = mutableMapOf()

  private val density: Int
    get() = application.resources.displayMetrics.densityDpi

  init {
    launcherApps.registerCallback(
      LauncherAppsCallback { packageNames: List<String>, user: UserHandle ->
        icons.keys.removeIf {
          when (it) {
            is ApplicationWithUser -> it.packageName in packageNames && it.user == user
            is ActivityData -> it.componentName.packageName in packageNames && it.userHandle == user
            is ShortcutData -> it.packageName in packageNames && it.userHandle == user
            is ConfigurableShortcutData ->
              it.componentName.packageName in packageNames && it.userHandle == user
            else -> throw NotImplementedError()
          }
        }
      }
    )
  }

  suspend fun get(info: ApplicationInfo, user: UserHandle): Drawable =
    getIcon(ApplicationWithUser(info.packageName, user), user)

  suspend fun get(info: ActivityData): Drawable = getIcon(info, info.userHandle)

  suspend fun get(info: ShortcutData): Drawable = getIcon(info, info.userHandle)

  suspend fun get(info: ConfigurableShortcutData): Drawable = getIcon(info, info.userHandle)

  private suspend fun getIcon(info: Any, user: UserHandle): Drawable =
    withContext(Dispatchers.IO) {
      icons.getOrPut(info) {
        application.packageManager.getUserBadgedIcon(
          loadIcon(info).let {
            if (it is AdaptiveIconDrawable) {
              AdaptiveLauncherIconDrawable(it)
            } else {
              LegacyLauncherIconDrawable.create(it)
            }
          },
          user
        )
      }
    }

  private fun loadIcon(info: Any): Drawable =
    when (info) {
      is ApplicationWithUser -> application.packageManager.getApplicationIcon(info.packageName)
      is ActivityData ->
        launcherApps.resolveActivity(info.componentName, info.userHandle).getIcon(density)
      is ShortcutData ->
        launcherApps.getShortcutIconDrawable(launcherApps.resolveShortcut(info), density)
          ?: application.packageManager.getApplicationIcon(info.packageName)
      is ConfigurableShortcutData -> launcherApps.resolveConfigurableShortcut(info).getIcon(density)
      else -> throw IllegalArgumentException()
    }

  private data class ApplicationWithUser(val packageName: String, val user: UserHandle)
}
