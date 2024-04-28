package link.danb.launcher.gestures

import android.content.ComponentName
import android.content.Intent
import android.graphics.RectF
import android.os.Build
import android.os.Message
import android.os.UserHandle
import android.view.SurfaceControl
import androidx.annotation.RequiresApi
import androidx.core.os.bundleOf
import link.danb.launcher.components.UserActivity
import link.danb.launcher.extensions.getParcelableCompat

@RequiresApi(Build.VERSION_CODES.Q)
data class GestureContract(val userActivity: UserActivity, val message: Message) {

  fun sendBounds(bounds: RectF, surfaceControl: SurfaceControl, onFinishCallback: Message) {
    val message = Message.obtain(message)
    message.data =
      bundleOf(
        EXTRA_ICON_POSITION to bounds,
        EXTRA_ICON_SURFACE to surfaceControl,
        EXTRA_ON_FINISH_CALLBACK to onFinishCallback,
      )
    message.replyTo.send(message)
  }

  companion object {
    private const val EXTRA_GESTURE_CONTRACT = "gesture_nav_contract_v1"
    private const val EXTRA_ICON_POSITION = "gesture_nav_contract_icon_position"
    private const val EXTRA_ICON_SURFACE = "gesture_nav_contract_surface_control"
    private const val EXTRA_REMOTE_CALLBACK = "android.intent.extra.REMOTE_CALLBACK"
    private const val EXTRA_ON_FINISH_CALLBACK = "gesture_nav_contract_finish_callback"

    fun fromIntent(intent: Intent): GestureContract? {
      val extras = intent.getBundleExtra(EXTRA_GESTURE_CONTRACT) ?: return null
      intent.removeExtra(EXTRA_GESTURE_CONTRACT)

      val component: ComponentName? = extras.getParcelableCompat(Intent.EXTRA_COMPONENT_NAME)
      val user: UserHandle? = extras.getParcelableCompat(Intent.EXTRA_USER)
      val message: Message? = extras.getParcelableCompat(EXTRA_REMOTE_CALLBACK)

      if (component != null && user != null && message != null && message.replyTo != null) {
        return GestureContract(UserActivity(component, user), message)
      }
      return null
    }
  }
}
