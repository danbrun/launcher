package link.danb.launcher.activities

import android.content.Context
import android.content.pm.LauncherApps
import android.graphics.Rect
import android.os.Bundle
import androidx.core.content.getSystemService
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import link.danb.launcher.apps.LauncherAppsCallback
import link.danb.launcher.data.UserActivity
import javax.inject.Inject

class ActivityManager @Inject constructor(@ApplicationContext context: Context) {

    private val launcherApps: LauncherApps by lazy { checkNotNull(context.getSystemService()) }

    val activities: Flow<List<UserActivity>> = callbackFlow {
        val components =
            launcherApps.profiles
                .flatMap { launcherApps.getActivityList(null, it) }
                .filter { it.componentName.packageName != context.packageName }
                .map { UserActivity(it.componentName, it.user) }
                .toMutableList()
        trySend(components)

        val callback = LauncherAppsCallback { packageNames, userHandle ->
            synchronized(this) {
                components.removeIf {
                    it.componentName.packageName in packageNames && it.userHandle == userHandle
                }
                components.addAll(
                    packageNames
                        .flatMap { launcherApps.getActivityList(it, userHandle) }
                        .map { UserActivity(it.componentName, it.user) }
                )
                trySend(components.toList())
            }
        }

        launcherApps.registerCallback(callback)
        awaitClose { launcherApps.unregisterCallback(callback) }
    }

    fun launchActivity(userActivity: UserActivity, sourceBounds: Rect, opts: Bundle) {
        launcherApps.startMainActivity(
            userActivity.componentName,
            userActivity.userHandle,
            sourceBounds,
            opts,
        )
    }

    fun launchAppDetails(userActivity: UserActivity, sourceBounds: Rect, opts: Bundle) {
        launcherApps.startAppDetailsActivity(
            userActivity.componentName,
            userActivity.userHandle,
            sourceBounds,
            opts,
        )
    }
}
