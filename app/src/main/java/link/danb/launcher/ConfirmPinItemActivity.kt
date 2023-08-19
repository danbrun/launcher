package link.danb.launcher

import android.content.pm.LauncherApps
import android.content.pm.LauncherApps.PinItemRequest
import android.os.Bundle
import android.os.Process.myUserHandle
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import dagger.hilt.android.AndroidEntryPoint
import link.danb.launcher.model.ShortcutViewModel
import link.danb.launcher.widgets.AppWidgetSetupActivityResultContract
import link.danb.launcher.widgets.AppWidgetSetupActivityResultContract.AppWidgetSetupInput
import javax.inject.Inject

@AndroidEntryPoint
class ConfirmPinItemActivity : AppCompatActivity() {

    private val shortcutViewModel: ShortcutViewModel by viewModels()

    @Inject
    lateinit var launcherApps: LauncherApps

    private val bindWidgetActivityLauncher = registerForActivityResult(
        AppWidgetSetupActivityResultContract()
    ) {
        if (it.success) {
            Toast.makeText(this, R.string.pinned_widget, Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(this, it.errorMessage, Toast.LENGTH_SHORT).show()
        }
        finish()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val pinItemRequest = launcherApps.getPinItemRequest(intent)

        if (!pinItemRequest.isValid) return

        when (pinItemRequest.requestType) {
            PinItemRequest.REQUEST_TYPE_APPWIDGET -> {
                val info = pinItemRequest.getAppWidgetProviderInfo(this) ?: return
                pinItemRequest.accept()
                bindWidgetActivityLauncher.launch(AppWidgetSetupInput(info, myUserHandle()))
            }

            PinItemRequest.REQUEST_TYPE_SHORTCUT -> {
                val info = pinItemRequest.shortcutInfo ?: return
                pinItemRequest.accept()
                shortcutViewModel.pinShortcut(info)
                Toast.makeText(this, R.string.pinned_shortcut, Toast.LENGTH_SHORT).show()
                finish()
            }
        }
    }
}
