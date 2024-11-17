package link.danb.launcher.apps

import android.app.ActivityOptions
import android.content.Context
import android.content.pm.LauncherApps
import android.view.View
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.toAndroidRect
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.roundToIntRect
import androidx.core.content.getSystemService
import dagger.hilt.EntryPoint
import dagger.hilt.EntryPoints
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import link.danb.launcher.components.UserActivity
import link.danb.launcher.components.UserShortcut
import link.danb.launcher.profiles.ProfileManager

@Composable
fun rememberAppsLauncher(): AppsLauncher {
  val context = LocalContext.current
  return remember(context) { AppsLauncher(context) }
}

class AppsLauncher(private val context: Context) {

  private val launcherApps: LauncherApps by lazy { checkNotNull(context.getSystemService()) }
  private val appsLauncherEntryPoint: AppsLauncherEntryPoint by lazy {
    EntryPoints.get(context.applicationContext, AppsLauncherEntryPoint::class.java)
  }
  private val profileManager: ProfileManager by lazy { appsLauncherEntryPoint.profileManager() }

  fun startMainActivity(userActivity: UserActivity, rect: Rect) {
    launcherApps.startMainActivity(
      userActivity.componentName,
      profileManager.getUserHandle(userActivity.profile),
      rect.roundToIntRect().toAndroidRect(),
      makeScaleUpAnimation(rect).toBundle(),
    )
  }

  fun startAppDetailsActivity(userActivity: UserActivity, rect: Rect) {
    launcherApps.startAppDetailsActivity(
      userActivity.componentName,
      profileManager.getUserHandle(userActivity.profile),
      rect.roundToIntRect().toAndroidRect(),
      makeScaleUpAnimation(rect).toBundle(),
    )
  }

  fun startShortcut(userShortcut: UserShortcut, rect: Rect) {
    launcherApps.startShortcut(
      userShortcut.packageName,
      userShortcut.shortcutId,
      rect.roundToIntRect().toAndroidRect(),
      makeScaleUpAnimation(rect).toBundle(),
      profileManager.getUserHandle(userShortcut.profile)!!,
    )
  }

  private fun makeScaleUpAnimation(rect: Rect) =
    with(rect.roundToIntRect()) {
      ActivityOptions.makeScaleUpAnimation(View(context), left, top, width, height)
    }

  @EntryPoint
  @InstallIn(SingletonComponent::class)
  interface AppsLauncherEntryPoint {
    fun profileManager(): ProfileManager
  }
}
