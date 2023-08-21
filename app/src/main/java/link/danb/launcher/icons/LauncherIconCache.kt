package link.danb.launcher.icons

import android.app.Application
import android.content.ComponentName
import android.content.pm.LauncherActivityInfo
import android.content.pm.LauncherApps
import android.content.pm.ShortcutInfo
import android.graphics.drawable.Drawable
import android.os.Process.myUserHandle
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LauncherIconCache @Inject constructor(
    private val application: Application, private val launcherApps: LauncherApps
) {

    private val activityIcons: MutableMap<ActivityHandle, Lazy<Drawable>> = mutableMapOf()
    private val shortcutIcons: MutableMap<ShortcutHandle, Lazy<Drawable>> = mutableMapOf()

    fun get(info: LauncherActivityInfo): Lazy<Drawable> =
        activityIcons.getOrPut(ActivityHandle(info)) {
            lazy {
                application.packageManager.getUserBadgedIcon(
                    LauncherIconDrawable(info.getIcon(0)), info.user
                )
            }
        }

    fun get(info: ShortcutInfo): Lazy<Drawable> = shortcutIcons.getOrPut(ShortcutHandle(info)) {
        lazy {
            application.packageManager.getUserBadgedIcon(
                LauncherIconDrawable(launcherApps.getShortcutIconDrawable(info, 0)), info.userHandle
            )
        }
    }

    data class ActivityHandle(val componentName: ComponentName, val isWorkProfile: Boolean) {
        constructor(info: LauncherActivityInfo) : this(
            info.componentName, info.user == myUserHandle()
        )
    }

    data class ShortcutHandle(val pkg: String, val id: String, val isWorkProfile: Boolean) {
        constructor(info: ShortcutInfo) : this(
            info.`package`, info.id, info.userHandle == myUserHandle()
        )
    }
}
