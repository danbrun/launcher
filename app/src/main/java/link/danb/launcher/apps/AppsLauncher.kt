package link.danb.launcher.apps

import android.app.Activity
import android.app.ActivityOptions
import android.content.pm.LauncherApps
import android.view.View
import android.widget.Toast
import androidx.activity.compose.LocalActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
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
import link.danb.launcher.R
import link.danb.launcher.apps.AppsLauncher.AppsLauncherEntryPoint
import link.danb.launcher.components.UserActivity
import link.danb.launcher.components.UserShortcut
import link.danb.launcher.components.UserShortcutCreator
import link.danb.launcher.profiles.ProfileManager
import link.danb.launcher.shortcuts.ShortcutManager
import link.danb.launcher.widgets.WidgetManager

@Composable
fun rememberAppsLauncher(): AppsLauncher {
  val activity = checkNotNull(LocalActivity.current)
  val entryPoint = EntryPoints.get(activity, AppsLauncherEntryPoint::class.java)
  val shortcutActivityLauncher =
    rememberLauncherForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) {
      val data = it.data ?: return@rememberLauncherForActivityResult
      entryPoint.shortcutManager().acceptPinRequest(data)
      Toast.makeText(activity, R.string.pinned_shortcut, Toast.LENGTH_SHORT).show()
    }
  return remember(activity) { AppsLauncher(activity, entryPoint, shortcutActivityLauncher) }
}

class AppsLauncher(
  private val activity: Activity,
  private val entryPoint: AppsLauncherEntryPoint,
  private val shortcutActivityLauncher: ActivityResultLauncher<IntentSenderRequest>,
) {

  private val launcherApps: LauncherApps by lazy { checkNotNull(activity.getSystemService()) }

  fun startMainActivity(userActivity: UserActivity, rect: Rect) {
    launcherApps.startMainActivity(
      userActivity.componentName,
      entryPoint.profileManager().getUserHandle(userActivity.profile),
      rect.roundToIntRect().toAndroidRect(),
      makeScaleUpAnimation(rect).toBundle(),
    )
  }

  fun startAppDetailsActivity(userActivity: UserActivity, rect: Rect) {
    launcherApps.startAppDetailsActivity(
      userActivity.componentName,
      entryPoint.profileManager().getUserHandle(userActivity.profile),
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
      entryPoint.profileManager().getUserHandle(userShortcut.profile)!!,
    )
  }

  fun startShortcutCreator(userShortcutCreator: UserShortcutCreator) {
    shortcutActivityLauncher.launch(
      IntentSenderRequest.Builder(
          entryPoint.shortcutManager().getShortcutCreatorIntent(userShortcutCreator)
        )
        .build()
    )
  }

  fun configureWidget(view: View, widgetId: Int) {
    entryPoint.widgetManager().startConfigurationActivity(activity, view, widgetId)
  }

  private fun makeScaleUpAnimation(rect: Rect) =
    with(rect.roundToIntRect()) {
      ActivityOptions.makeScaleUpAnimation(View(activity), left, top, width, height)
    }

  @EntryPoint
  @InstallIn(ActivityComponent::class)
  interface AppsLauncherEntryPoint {
    fun profileManager(): ProfileManager

    fun shortcutManager(): ShortcutManager

    fun widgetManager(): WidgetManager
  }
}
