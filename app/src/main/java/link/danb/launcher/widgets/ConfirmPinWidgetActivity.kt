package link.danb.launcher.widgets

import android.content.pm.LauncherApps
import android.content.pm.LauncherApps.PinItemRequest
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.getSystemService
import dagger.hilt.android.AndroidEntryPoint
import link.danb.launcher.widgets.AppWidgetSetupActivityResultContract.AppWidgetSetupInput

@AndroidEntryPoint
class ConfirmPinWidgetActivity : AppCompatActivity() {

  private val launcherApps: LauncherApps by lazy { checkNotNull(getSystemService()) }

  private val bindWidgetActivityLauncher =
    registerForActivityResult(AppWidgetSetupActivityResultContract()) { finish() }

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    val pinItemRequest = launcherApps.getPinItemRequest(intent)

    if (
      !pinItemRequest.isValid || pinItemRequest.requestType != PinItemRequest.REQUEST_TYPE_APPWIDGET
    )
      return

    val info = pinItemRequest.getAppWidgetProviderInfo(this) ?: return

    pinItemRequest.accept()

    bindWidgetActivityLauncher.launch(AppWidgetSetupInput(info.provider, info.profile))
  }
}
