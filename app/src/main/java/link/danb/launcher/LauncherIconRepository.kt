package link.danb.launcher

import android.app.Application
import android.content.pm.LauncherActivityInfo
import android.graphics.drawable.Drawable
import kotlinx.coroutines.coroutineScope

/** Repository for caching launcher icons. */
class LauncherIconRepository(private val application: Application) {

    private val iconCache: MutableMap<LauncherActivityInfo, Drawable> = mutableMapOf()

    /** Retrieve the icon for the given activity info. */
    suspend fun get(info: LauncherActivityInfo): Drawable = coroutineScope {
        if (!iconCache.contains(info)) {
            iconCache[info] = application.packageManager.getUserBadgedIcon(
                LauncherIconDrawable(info.getIcon(0)), info.user
            )
        }

        return@coroutineScope iconCache[info]!!
    }
}
