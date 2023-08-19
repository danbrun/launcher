package link.danb.launcher.model

import android.content.pm.LauncherActivityInfo
import android.graphics.drawable.Drawable
import link.danb.launcher.ui.LauncherIconDrawable

class ShortcutActivityData(val launcherActivityInfo: LauncherActivityInfo) : TileViewData {

    override val name: CharSequence by lazy {
        launcherActivityInfo.label
    }

    override val icon: Drawable? by lazy {
        launcherActivityInfo.getIcon(0)?.let { LauncherIconDrawable(it) }
    }

    override fun areItemsTheSame(other: TileViewData): Boolean =
        other is ShortcutActivityData && launcherActivityInfo.componentName == other.launcherActivityInfo.componentName

    override fun areContentsTheSame(other: TileViewData): Boolean = false
}
