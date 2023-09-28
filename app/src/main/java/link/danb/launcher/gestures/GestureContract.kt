package link.danb.launcher.gestures

import android.content.ComponentName
import android.content.Intent
import android.graphics.RectF
import android.os.Message
import android.os.UserHandle
import androidx.core.os.bundleOf
import link.danb.launcher.extensions.getParcelableCompat

data class GestureContract(
  val componentName: ComponentName,
  val userHandle: UserHandle,
  val message: Message,
) {

  fun sendBounds(bounds: RectF) {
    val message = Message.obtain(message)
    message.data = bundleOf(EXTRA_ICON_POSITION to bounds)
    message.replyTo.send(message)
  }

  companion object {
    private const val EXTRA_GESTURE_CONTRACT = "gesture_nav_contract_v1"
    private const val EXTRA_ICON_POSITION = "gesture_nav_contract_icon_position"
    private const val EXTRA_REMOTE_CALLBACK = "android.intent.extra.REMOTE_CALLBACK"

    fun fromIntent(intent: Intent): GestureContract? {
      val extras = intent.getBundleExtra(EXTRA_GESTURE_CONTRACT) ?: return null

      val component: ComponentName? = extras.getParcelableCompat(Intent.EXTRA_COMPONENT_NAME)
      val user: UserHandle? = extras.getParcelableCompat(Intent.EXTRA_USER)
      val message: Message? = extras.getParcelableCompat(EXTRA_REMOTE_CALLBACK)

      if (component != null && user != null && message != null && message.replyTo != null) {
        return GestureContract(component, user, message)
      }
      return null
    }
  }
}
