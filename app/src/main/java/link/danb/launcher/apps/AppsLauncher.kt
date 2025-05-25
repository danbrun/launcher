package link.danb.launcher.apps

import android.app.Activity
import android.app.ActivityOptions
import android.content.pm.LauncherApps
import android.view.View
import androidx.activity.compose.LocalActivity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.toAndroidRect
import androidx.compose.ui.unit.roundToIntRect
import androidx.core.content.getSystemService
import dagger.hilt.EntryPoint
import dagger.hilt.EntryPoints
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ActivityComponent
import link.danb.launcher.components.UserActivity
import link.danb.launcher.components.UserShortcut
import link.danb.launcher.profiles.ProfileManager
import link.danb.launcher.widgets.WidgetManager

@Composable
fun rememberAppsLauncher(): AppsLauncher {
  val activity = checkNotNull(LocalActivity.current)
  return remember(activity) { AppsLauncher(activity) }
}

class AppsLauncher(private val activity: Activity) {

  private val launcherApps: LauncherApps by lazy { checkNotNull(activity.getSystemService()) }
  private val appsLauncherEntryPoint: AppsLauncherEntryPoint by lazy {
    EntryPoints.get(activity, AppsLauncherEntryPoint::class.java)
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

  fun configureWidget(view: View, widgetId: Int) {
    appsLauncherEntryPoint.widgetManager().startConfigurationActivity(activity, view, widgetId)
  }

  private fun makeScaleUpAnimation(rect: Rect) =
    with(rect.roundToIntRect()) {
      ActivityOptions.makeScaleUpAnimation(View(activity), left, top, width, height)
    }

  @EntryPoint
  @InstallIn(ActivityComponent::class)
  interface AppsLauncherEntryPoint {
    fun profileManager(): ProfileManager

    fun widgetManager(): WidgetManager
  }
}
