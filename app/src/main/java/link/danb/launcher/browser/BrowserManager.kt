package link.danb.launcher.browser

import android.content.Context
import android.graphics.Bitmap
import android.os.Handler
import android.os.Looper
import androidx.core.graphics.scale
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.system.exitProcess
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import link.danb.launcher.browser.database.BrowserBitmap
import link.danb.launcher.browser.database.BrowserDatabase
import link.danb.launcher.browser.database.BrowserProgress
import link.danb.launcher.browser.database.BrowserState
import link.danb.launcher.browser.database.BrowserTab
import link.danb.launcher.browser.database.BrowserTitle
import link.danb.launcher.browser.database.BrowserUrl
import org.mozilla.geckoview.GeckoResult
import org.mozilla.geckoview.GeckoRuntime
import org.mozilla.geckoview.GeckoSession

@Singleton
class BrowserManager
@Inject
constructor(
  private val browserDatabase: BrowserDatabase,
  @param:ApplicationContext private val context: Context,
) {

  private val coroutineScope = CoroutineScope(Dispatchers.Main)

  private val geckoRuntime: GeckoRuntime by lazy {
    GeckoRuntime.create(context).apply { delegate = { exitProcess(0) } }
  }

  private val sessions: MutableMap<Int, GeckoSession> = mutableMapOf()

  @OptIn(DelicateCoroutinesApi::class)
  fun getSession(tab: BrowserTab): GeckoSession {
    val session = sessions[tab.tabId]
    if (session != null) return session

    val newSession =
      GeckoSession().apply {
        open(geckoRuntime)

        val delegates = Delegates(this@BrowserManager, coroutineScope, browserDatabase, tab.tabId)
        contentDelegate = delegates
        navigationDelegate = delegates
        progressDelegate = delegates

        coroutineScope.launch {
          val state =
            withContext(Dispatchers.IO) { browserDatabase.browserSessionDao().get(tab.tabId) }
          if (state != null) {
            restoreState(state.state)
          } else {
            loadUri(tab.url)
          }
        }
      }
    sessions[tab.tabId] = newSession
    return newSession
  }

  fun updateBitmap(tab: BrowserTab, bitmap: Bitmap) {
    coroutineScope.launch(Dispatchers.IO) {
      browserDatabase
        .browserTabDao()
        .update(BrowserBitmap(tab.tabId, bitmap.scale(bitmap.width / 8, bitmap.height / 8, false)))
    }
  }

  fun closeSession(tabId: Int) {
    val session = sessions[tabId] ?: return
    session.close()
    sessions.remove(tabId)
  }

  class Delegates(
    private val browserManager: BrowserManager,
    private val coroutineScope: CoroutineScope,
    private val browserDatabase: BrowserDatabase,
    private val tabId: Int,
  ) : GeckoSession.ContentDelegate, GeckoSession.NavigationDelegate, GeckoSession.ProgressDelegate {

    override fun onTitleChange(session: GeckoSession, title: String?) {
      if (title != null) {
        coroutineScope.launch(Dispatchers.IO) {
          browserDatabase.browserTabDao().update(BrowserTitle(tabId, title))
        }
      }
    }

    override fun onLocationChange(
      session: GeckoSession,
      url: String?,
      perms: List<GeckoSession.PermissionDelegate.ContentPermission?>,
      hasUserGesture: Boolean,
    ) {
      if (url != null) {
        coroutineScope.launch(Dispatchers.IO) {
          browserDatabase.browserTabDao().update(BrowserUrl(tabId, url))
        }
      }
      session.flushSessionState()
    }

    override fun onProgressChange(session: GeckoSession, progress: Int) {
      coroutineScope.launch(Dispatchers.IO) {
        browserDatabase.browserTabDao().update(BrowserProgress(tabId, progress / 100f))
      }
    }

    override fun onSessionStateChange(
      session: GeckoSession,
      sessionState: GeckoSession.SessionState,
    ) {
      coroutineScope.launch(Dispatchers.IO) {
        browserDatabase.browserSessionDao().upsert(BrowserState(tabId, sessionState))
      }
    }

    override fun onNewSession(session: GeckoSession, uri: String): GeckoResult<GeckoSession> {
      val result = GeckoResult<GeckoSession>(Handler(Looper.getMainLooper()))
      coroutineScope.launch(Dispatchers.IO) {
        val tabId = browserDatabase.browserTabDao().upsert(BrowserTab(url = uri)).toInt()
        val tab = checkNotNull(browserDatabase.browserTabDao().get(tabId))
        result.complete(browserManager.getSession(tab))
      }
      return result
    }
  }
}
