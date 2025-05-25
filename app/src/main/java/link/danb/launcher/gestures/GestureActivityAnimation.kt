package link.danb.launcher.gestures

import android.content.Intent
import android.os.Build
import android.view.ViewGroup
import androidx.activity.ComponentActivity
import androidx.activity.compose.LocalActivity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.toAndroidRectF
import androidx.core.util.Consumer
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import dagger.hilt.EntryPoint
import dagger.hilt.EntryPoints
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ActivityComponent
import link.danb.launcher.LauncherViewModel
import link.danb.launcher.components.UserActivity
import link.danb.launcher.profiles.ProfileManager

@Composable
fun GestureActivityAnimation(
  launcherViewModel: LauncherViewModel = viewModel(),
  onGestureActivityChange: (UserActivity?) -> Unit,
) {
  if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return

  val activity = LocalActivity.current as ComponentActivity
  val entryPoint =
    remember(activity) { EntryPoints.get(activity, GestureActivityAnimationEntryPoint::class.java) }
  val useMonochrome by launcherViewModel.useMonochromeIcons.collectAsStateWithLifecycle()

  DisposableEffect(activity) {
    val view = GestureIconView(activity)
    view.onFinishGestureAnimation = { onGestureActivityChange(null) }

    val onNewIntentObserver: Consumer<Intent> = Consumer { intent ->
      val gestureContract =
        GestureContract.fromIntent(intent) {
          checkNotNull(entryPoint.profileManager().getProfile(it))
        } ?: return@Consumer
      val data =
        entryPoint.gestureActivityIconStore().getActivityIconState(gestureContract.userActivity)
          ?: return@Consumer

      onGestureActivityChange(data.userActivity)
      view.animateNavigationGesture(
        gestureContract,
        data.boundsInRoot.toAndroidRectF(),
        data.launcherIconData,
        useMonochrome,
      )
    }

    val root = activity.window.decorView.rootView as ViewGroup

    root.addView(view)
    activity.addOnNewIntentListener(onNewIntentObserver)

    onDispose {
      root.removeView(view)
      activity.removeOnNewIntentListener(onNewIntentObserver)
    }
  }
}

@EntryPoint
@InstallIn(ActivityComponent::class)
interface GestureActivityAnimationEntryPoint {
  fun profileManager(): ProfileManager

  fun gestureActivityIconStore(): GestureActivityIconStore
}
