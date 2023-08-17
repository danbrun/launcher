package link.danb.launcher.model

import android.app.Application
import android.content.ComponentName
import android.content.pm.LauncherActivityInfo
import android.graphics.drawable.Drawable
import android.os.UserHandle
import link.danb.launcher.ui.LauncherIconDrawable

class LauncherActivityData(
    application: Application,
    launcherActivityInfo: LauncherActivityInfo,
    launcherActivityMetadata: LauncherActivityMetadata
) : TileViewData {
    val component: ComponentName = launcherActivityInfo.componentName
    val user: UserHandle = launcherActivityInfo.user
    val timestamp = System.currentTimeMillis()

    val tags = launcherActivityMetadata.tags

    override val name: CharSequence by lazy { launcherActivityInfo.label }
    override val icon: Drawable by lazy {
        application.packageManager.getUserBadgedIcon(
            LauncherIconDrawable(launcherActivityInfo.getIcon(0)), user
        )
    }

    override fun areItemsTheSame(other: TileViewData): Boolean =
        other is LauncherActivityData && component == other.component && user == other.user

    override fun areContentsTheSame(other: TileViewData): Boolean =
        other is LauncherActivityData && timestamp == other.timestamp
}
