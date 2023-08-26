package link.danb.launcher.widgets

import android.content.pm.LauncherApps
import android.content.pm.LauncherApps.PinItemRequest
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import dagger.hilt.android.AndroidEntryPoint
import link.danb.launcher.R
import link.danb.launcher.widgets.AppWidgetSetupActivityResultContract.AppWidgetSetupInput
import javax.inject.Inject

@AndroidEntryPoint
class ConfirmPinWidgetActivity : AppCompatActivity() {

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

        if (!pinItemRequest.isValid || pinItemRequest.requestType != PinItemRequest.REQUEST_TYPE_APPWIDGET) return

        val info = pinItemRequest.getAppWidgetProviderInfo(this) ?: return

        pinItemRequest.accept()
        bindWidgetActivityLauncher.launch(AppWidgetSetupInput(info, info.profile))
    }
}
