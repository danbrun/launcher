package link.danb.launcher.icons

import android.app.Application
import android.content.pm.ApplicationInfo
import android.content.pm.LauncherActivityInfo
import android.content.pm.LauncherApps
import android.content.pm.ShortcutInfo
import android.graphics.drawable.Drawable
import android.os.UserHandle
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import link.danb.launcher.apps.LauncherAppsCallback
import link.danb.launcher.extensions.packageName
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LauncherIconCache @Inject constructor(
    private val application: Application, private val launcherApps: LauncherApps
) {

    private val icons: MutableMap<IconHandle, Drawable> = mutableMapOf()

    private val density: Int
        get() = application.resources.displayMetrics.densityDpi

    init {
        launcherApps.registerCallback(LauncherAppsCallback { packageName: String, user: UserHandle ->
            icons.keys.removeIf { it.packageName == packageName && it.user == user }
        })
    }

    suspend fun get(info: ApplicationInfoWithUser): Drawable = getIcon(info)

    suspend fun get(info: LauncherActivityInfo): Drawable = getIcon(info)

    suspend fun get(info: ShortcutInfo): Drawable = getIcon(info)

    private suspend fun getIcon(info: Any) = coroutineScope {
        val iconHandle = getIconHandle(info)
        icons.getOrPut(iconHandle) {
            async(Dispatchers.IO) {
                application.packageManager.getUserBadgedIcon(
                    LauncherIconDrawable.create(loadIcon(info)), iconHandle.user
                )
            }.await()
        }
    }

    private fun getIconHandle(info: Any): IconHandle = when (info) {
        is ApplicationInfoWithUser -> IconHandle(info.packageName, info.user, null)
        is LauncherActivityInfo -> IconHandle(
            info.componentName.packageName, info.user, info.componentName.className
        )

        is ShortcutInfo -> IconHandle(info.packageName, info.userHandle, info.shortLabel)
        else -> throw IllegalArgumentException()
    }

    private fun loadIcon(info: Any): Drawable = when (info) {
        is ApplicationInfoWithUser -> application.packageManager.getApplicationIcon(info.packageName)
        is LauncherActivityInfo -> info.getIcon(density)
        is ShortcutInfo -> launcherApps.getShortcutIconDrawable(info, density)
        else -> throw IllegalArgumentException()
    }

    data class ApplicationInfoWithUser(val packageName: String, val user: UserHandle) {
        constructor(info: ApplicationInfo, user: UserHandle) : this(info.packageName, user)
    }

    private data class IconHandle(
        val packageName: String, val user: UserHandle, val additionalData: Any?
    )
}
