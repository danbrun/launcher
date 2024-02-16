package link.danb.launcher.shortcuts

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import link.danb.launcher.R

@AndroidEntryPoint
class ConfirmPinShortcutActivity : AppCompatActivity() {

  @Inject lateinit var shortcutManager: ShortcutManager

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    shortcutManager.acceptPinRequest(intent)
    Toast.makeText(this, R.string.pinned_shortcut, Toast.LENGTH_SHORT).show()
    finish()
  }
}
