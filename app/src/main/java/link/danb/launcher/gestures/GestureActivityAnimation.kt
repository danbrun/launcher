package link.danb.launcher.gestures

import android.content.Intent
import android.os.Build
import android.view.ViewGroup
import androidx.activity.ComponentActivity
import androidx.activity.compose.LocalActivity
import androidx.annotation.RequiresApi
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.toAndroidRectF
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.core.util.Consumer
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import link.danb.launcher.ActivityViewItem
import link.danb.launcher.LauncherViewModel
import link.danb.launcher.components.UserActivity
import link.danb.launcher.profiles.rememberProfileManager

@Composable
fun GestureActivityAnimation(
  launcherViewModel: LauncherViewModel = viewModel(),
  content: @Composable GestureActivityAnimationScope.() -> Unit,
) {
  if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
    val scope = remember { GestureActivityAnimationScopeImpl() }
    scope.GestureAnimationView(launcherViewModel)
    scope.content()
  } else {
    remember { GestureActivityAnimationScopeNoop() }.content()
  }
}

interface GestureActivityAnimationScope {
  fun Modifier.gestureIcon(item: ActivityViewItem): Modifier = this
}

class GestureActivityAnimationScopeNoop : GestureActivityAnimationScope

@RequiresApi(Build.VERSION_CODES.Q)
class GestureActivityAnimationScopeImpl : GestureActivityAnimationScope {

  private val boundsMap: MutableMap<UserActivity, Rect> = mutableMapOf()

  private var currentUserActivity: UserActivity? by mutableStateOf(null)

  override fun Modifier.gestureIcon(item: ActivityViewItem): Modifier = composed {
    DisposableEffect(item) { onDispose { boundsMap.remove(item.userActivity) } }

    this then
      Modifier.onGloballyPositioned { boundsMap[item.userActivity] = it.boundsInRoot() }
        .alpha(if (currentUserActivity == item.userActivity) 0f else 1f)
  }

  @Composable
  fun GestureAnimationView(launcherViewModel: LauncherViewModel = viewModel()) {
    val activity = LocalActivity.current as ComponentActivity
    val profileManager = rememberProfileManager()
    val useMonochrome by launcherViewModel.useMonochromeIcons.collectAsStateWithLifecycle()

    DisposableEffect(activity) {
      val view = GestureIconView(activity)
      view.onFinishGestureAnimation = { currentUserActivity = null }

      val onNewIntentObserver: Consumer<Intent> = Consumer { intent ->
        val gestureContract =
          GestureContract.fromIntent(intent) { checkNotNull(profileManager.getProfile(it)) }
            ?: return@Consumer

        val gestureActivityData =
          getGestureData(gestureContract.userActivity)
            ?: getGestureData(gestureContract.userActivity.packageName)
            ?: return@Consumer

        currentUserActivity = gestureActivityData.userActivity
        view.animateNavigationGesture(
          gestureContract,
          gestureActivityData.bounds.toAndroidRectF(),
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

  private fun getGestureData(userActivity: UserActivity): GestureActivityData? {
    val bounds = boundsMap[userActivity] ?: return null
    return GestureActivityData(userActivity, bounds)
  }

  private fun getGestureData(packageName: String): GestureActivityData? {
    val userActivity = boundsMap.keys.firstOrNull { it.packageName == packageName } ?: return null
    return getGestureData(userActivity)
  }

  private data class GestureActivityData(val userActivity: UserActivity, val bounds: Rect)
}
