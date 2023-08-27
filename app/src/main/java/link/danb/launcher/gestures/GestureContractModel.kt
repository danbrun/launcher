package link.danb.launcher.gestures

import android.app.Application
import android.content.ComponentName
import android.content.Intent
import android.graphics.RectF
import android.os.Message
import android.os.UserHandle
import androidx.core.os.bundleOf
import androidx.lifecycle.AndroidViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import link.danb.launcher.extensions.getParcelableCompat
import javax.inject.Inject

@HiltViewModel
class GestureContractModel @Inject constructor(application: Application) :
    AndroidViewModel(application) {

    private val _gestureContract: MutableStateFlow<GestureContract?> = MutableStateFlow(null)

    val gestureContract: StateFlow<GestureContract?> = _gestureContract.asStateFlow()

    fun onNewIntent(intent: Intent) {
        val extras = intent.getBundleExtra(EXTRA_GESTURE_CONTRACT) ?: return

        val component: ComponentName? = extras.getParcelableCompat(Intent.EXTRA_COMPONENT_NAME)
        val user: UserHandle? = extras.getParcelableCompat(Intent.EXTRA_USER)
        val message: Message? = extras.getParcelableCompat(EXTRA_REMOTE_CALLBACK)

        if (component != null && user != null && message != null && message.replyTo != null) {
            _gestureContract.value = GestureContract(component, user, message)
        }
    }

    fun setBounds(bounds: RectF) {
        val message = _gestureContract.value?.message ?: return

        message.data = bundleOf(EXTRA_ICON_POSITION to bounds)
        message.replyTo.send(message)

        _gestureContract.value = null
    }

    data class GestureContract(
        val componentName: ComponentName,
        val userHandle: UserHandle,
        val message: Message
    )

    companion object {
        private const val EXTRA_GESTURE_CONTRACT = "gesture_nav_contract_v1"
        private const val EXTRA_ICON_POSITION = "gesture_nav_contract_icon_position"
        private const val EXTRA_REMOTE_CALLBACK = "android.intent.extra.REMOTE_CALLBACK"
    }
}
