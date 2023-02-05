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
) {
    val component: ComponentName = launcherActivityInfo.componentName
    val user: UserHandle = launcherActivityInfo.user
    val timestamp = System.currentTimeMillis()

    val name: CharSequence by lazy { launcherActivityInfo.label }
    val icon: Drawable by lazy {
        application.packageManager.getUserBadgedIcon(
            LauncherIconDrawable(launcherActivityInfo.getIcon(0)), user
        )
    }
    val tags = launcherActivityMetadata.tags
}
