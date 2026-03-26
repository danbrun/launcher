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
    handleIntent()

    enableEdgeToEdge()
    setContent { LauncherTheme { BrowserScreen(browserViewModel) } }

    onBackPressedDispatcher.addCallback(this) {
      when (browserViewModel.backState.value) {
        BackState.BROWSER_BACK -> {
          browserViewModel.goBack()
        }
        BackState.FINISH_ACTIVITY -> {
          finish()
          browserViewModel.closeTab()
        }
      }
    }
  }

  override fun onNewIntent(intent: Intent) {
    super.onNewIntent(intent)
    setIntent(intent)
    handleIntent()
  }

  private fun handleIntent() {
    val tabId = intent.getIntExtra("tab_id", -1)
    if (tabId != -1) {
      browserViewModel.changeTab(tabId)
    } else {
      val url = intent.dataString ?: return
      browserViewModel.newTab(url)
    }
  }
}
