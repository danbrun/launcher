package link.danb.launcher.browser

import android.content.Context
import android.widget.FrameLayout
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import link.danb.launcher.browser.database.BrowserTab
import org.mozilla.geckoview.GeckoView

@AndroidEntryPoint
class BrowserView(context: Context) : FrameLayout(context) {

  @Inject lateinit var browserManager: BrowserManager

  private val geckoView: GeckoView = GeckoView(context)

  private var tab: BrowserTab? = null
  private var job: Job = SupervisorJob()

  init {
    addView(geckoView)
  }

  fun setTab(tab: BrowserTab) {
    this.tab = tab
    geckoView.setSession(browserManager.getSession(tab))
  }

  fun clearTab() {
    this.tab = null
    geckoView.releaseSession()
  }

  override fun onAttachedToWindow() {
    super.onAttachedToWindow()

    CoroutineScope(job + Dispatchers.Main).launch {
      while (isActive) {
        delay(10.seconds)
        val tab = tab
        if (tab != null) {
          geckoView.capturePixels().accept { bitmap ->
            if (bitmap != null) {
              browserManager.updateBitmap(tab, bitmap)
            }
          }
        }
      }
    }
  }

  override fun onDetachedFromWindow() {
    super.onDetachedFromWindow()

    job.cancel()
    job = SupervisorJob()
  }
}
