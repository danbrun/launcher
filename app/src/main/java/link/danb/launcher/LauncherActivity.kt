package link.danb.launcher

import android.content.ComponentName
import android.content.Intent
import android.os.Bundle
import android.os.Message
import android.os.UserHandle
import android.view.View
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.core.graphics.toRectF
import androidx.core.os.bundleOf
import androidx.lifecycle.lifecycleScope
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import link.danb.launcher.utils.getBoundsOnScreen
import link.danb.launcher.utils.getParcelableCompat

@AndroidEntryPoint
class LauncherActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.launcher_activity)

        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                .replace(R.id.activity_frame, LauncherFragment()).commit()
        }

        onBackPressedDispatcher.addCallback(object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                // Do not close app.
            }
        })
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)

        val extras = intent.getBundleExtra(EXTRA_GESTURE_CONTRACT) ?: return

        val component: ComponentName? = extras.getParcelableCompat(Intent.EXTRA_COMPONENT_NAME)
        val user: UserHandle? = extras.getParcelableCompat(Intent.EXTRA_USER)
        val message: Message? = extras.getParcelableCompat(EXTRA_REMOTE_CALLBACK)

        if (component != null && user != null && message != null && message.replyTo != null) {
            var iconViewProvider: IconViewProvider? = null
            for (fragment in supportFragmentManager.fragments) {
                if (fragment is IconViewProvider) {
                    iconViewProvider = fragment
                    break
                }
            }

            lifecycleScope.launch {
                val view = iconViewProvider?.getIconView(component, user) ?: return@launch

                message.data = bundleOf(EXTRA_ICON_POSITION to view.getBoundsOnScreen().toRectF())
                message.replyTo.send(message)
            }
        }
    }

    fun interface IconViewProvider {
        suspend fun getIconView(component: ComponentName, user: UserHandle): View?
    }

    companion object {
        private const val EXTRA_GESTURE_CONTRACT = "gesture_nav_contract_v1"
        private const val EXTRA_ICON_POSITION = "gesture_nav_contract_icon_position"
        private const val EXTRA_REMOTE_CALLBACK = "android.intent.extra.REMOTE_CALLBACK"
    }
}
