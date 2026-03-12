package link.danb.launcher.browser

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.addCallback
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import dagger.hilt.android.AndroidEntryPoint
import link.danb.launcher.ui.theme.LauncherTheme

@AndroidEntryPoint
class BrowserActivity : ComponentActivity() {

  val browserViewModel: BrowserViewModel by viewModels()

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()
    setContent { LauncherTheme { BrowserScreen(browserViewModel) } }
    onBackPressedDispatcher.addCallback(this) { browserViewModel.goBack() }
    setTab()
  }

  override fun onNewIntent(intent: Intent) {
    super.onNewIntent(intent)
    setIntent(intent)
    setTab()
  }

  private fun setTab() {
    val tabId = intent.getIntExtra("tab_id", -1)
    if (tabId != -1) {
      browserViewModel.changeTab(tabId)
    }
  }
}
