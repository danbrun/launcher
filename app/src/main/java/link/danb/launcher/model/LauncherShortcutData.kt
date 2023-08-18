package link.danb.launcher.model

import android.content.pm.LauncherApps
import android.content.pm.ShortcutInfo
import android.graphics.drawable.Drawable
import link.danb.launcher.ui.LauncherIconDrawable

class LauncherShortcutData(launcherApps: LauncherApps, val shortcutInfo: ShortcutInfo) :
    TileViewData {

    override val name: CharSequence by lazy {
        shortcutInfo.shortLabel!!
    }

    override val icon: Drawable? by lazy {
        launcherApps.getShortcutBadgedIconDrawable(shortcutInfo, 0)
            ?.let { LauncherIconDrawable(it) }
    }

    override fun areItemsTheSame(other: TileViewData): Boolean =
        other is LauncherShortcutData && shortcutInfo.`package` == other.shortcutInfo.`package` && shortcutInfo.id == other.shortcutInfo.id

    override fun areContentsTheSame(other: TileViewData): Boolean = false
}
