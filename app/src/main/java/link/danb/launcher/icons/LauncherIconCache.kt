package link.danb.launcher.icons

import android.app.Application
import android.content.pm.ApplicationInfo
import android.content.pm.LauncherActivityInfo
import android.content.pm.LauncherApps
import android.content.pm.ShortcutInfo
import android.graphics.drawable.AdaptiveIconDrawable
import android.graphics.drawable.Drawable
import android.os.UserHandle
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import link.danb.launcher.apps.LauncherAppsCallback
import link.danb.launcher.extensions.packageName

@Singleton
class LauncherIconCache
@Inject
constructor(private val application: Application, private val launcherApps: LauncherApps) {

  private val icons: MutableMap<IconHandle, Drawable> = mutableMapOf()

  private val density: Int
    get() = application.resources.displayMetrics.densityDpi

  init {
    launcherApps.registerCallback(
      LauncherAppsCallback { packageNames: List<String>, user: UserHandle ->
        icons.keys.removeIf { it.packageName in packageNames && it.user == user }
      }
    )
  }

  suspend fun get(info: ApplicationInfo, user: UserHandle): Drawable =
    getIcon(ApplicationInfoWithUser(info, user))

  suspend fun get(info: LauncherActivityInfo): Drawable = getIcon(info)

  suspend fun get(info: ShortcutInfo): Drawable = getIcon(info)

  private suspend fun getIcon(info: Any) =
    withContext(Dispatchers.IO) {
      val iconHandle = getIconHandle(info)
      icons.getOrPut(iconHandle) {
        application.packageManager.getUserBadgedIcon(
          loadIcon(info).let {
            if (it is AdaptiveIconDrawable) {
              AdaptiveLauncherIconDrawable(it)
            } else {
              LegacyLauncherIconDrawable.create(it)
            }
          },
          iconHandle.user
        )
      }
    }

  private fun getIconHandle(info: Any): IconHandle =
    when (info) {
      is ApplicationInfoWithUser -> IconHandle(info.info.packageName, info.user, null)
      is LauncherActivityInfo ->
        IconHandle(info.componentName.packageName, info.user, info.componentName.className)
      is ShortcutInfo -> IconHandle(info.packageName, info.userHandle, info.shortLabel)
      else -> throw IllegalArgumentException()
    }

  private fun loadIcon(info: Any): Drawable =
    when (info) {
      is ApplicationInfoWithUser -> application.packageManager.getApplicationIcon(info.info)
      is LauncherActivityInfo -> info.getIcon(density)
      is ShortcutInfo ->
        launcherApps.getShortcutIconDrawable(info, density)
          ?: application.packageManager.getApplicationIcon(info.packageName)
      else -> throw IllegalArgumentException()
    }

  data class ApplicationInfoWithUser(val info: ApplicationInfo, val user: UserHandle)

  private data class IconHandle(
    val packageName: String,
    val user: UserHandle,
    val additionalData: Any?
  )
}
