package link.danb.launcher.shortcuts

import android.content.pm.LauncherApps
import android.os.Bundle
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import link.danb.launcher.R
import link.danb.launcher.data.UserShortcut

@AndroidEntryPoint
class ConfirmPinShortcutActivity : AppCompatActivity() {

  private val shortcutsViewModel: ShortcutsViewModel by viewModels()

  @Inject lateinit var launcherApps: LauncherApps

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    val pinItemRequest = launcherApps.getPinItemRequest(intent)

    if (
      !pinItemRequest.isValid ||
        pinItemRequest.requestType != LauncherApps.PinItemRequest.REQUEST_TYPE_SHORTCUT
    )
      return

    val info = pinItemRequest.shortcutInfo ?: return
    pinItemRequest.accept()
    shortcutsViewModel.pinShortcut(UserShortcut(info))

    Toast.makeText(this, R.string.pinned_shortcut, Toast.LENGTH_SHORT).show()
    finish()
  }
}
