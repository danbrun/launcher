package link.danb.launcher.model

import android.app.Application
import android.content.pm.LauncherActivityInfo
import android.graphics.drawable.Drawable
import android.os.UserHandle
import link.danb.launcher.ui.LauncherIconDrawable

class ShortcutActivityData(
    val application: Application,
    val launcherActivityInfo: LauncherActivityInfo,
    val user: UserHandle
) : TileViewData {

    override val name: CharSequence by lazy {
        launcherActivityInfo.label
    }

    override val icon: Drawable? by lazy {
        launcherActivityInfo.getIcon(0)?.let {
            application.packageManager.getUserBadgedIcon(LauncherIconDrawable(it), user)
        }
    }

    override fun areItemsTheSame(other: TileViewData): Boolean =
        other is ShortcutActivityData && launcherActivityInfo.componentName == other.launcherActivityInfo.componentName

    override fun areContentsTheSame(other: TileViewData): Boolean =
        other is ShortcutActivityData && name == other.name && icon == other.icon
}
