package link.danb.launcher.browser

import android.net.Uri
import android.webkit.URLUtil
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import link.danb.launcher.browser.database.BrowserDatabase
import link.danb.launcher.browser.database.BrowserTab

@HiltViewModel
class BrowserViewModel
@Inject
constructor(
  private var browserDatabase: BrowserDatabase,
  private var browserManager: BrowserManager,
) : ViewModel() {

  private val _tabId: MutableStateFlow<Int?> = MutableStateFlow(null)

  val viewState: StateFlow<ViewState> =
    combine(_tabId, browserDatabase.browserTabDao().getAll()) { tabId, tabs ->
        val currentTab = tabs.firstOrNull { it.tabId == tabId } ?: tabs.firstOrNull()
        ViewState(currentTab, tabs)
      }
      .stateIn(viewModelScope, SharingStarted.WhileSubscribed(), ViewState())

  @OptIn(ExperimentalCoroutinesApi::class)
  val backState: StateFlow<BackState> =
    _tabId
      .flatMapLatest { tabId ->
        if (tabId != null) {
          browserDatabase.browserSessionDao().getFlow(tabId).map { session ->
            if (session != null && session.state.currentIndex > 0) {
              BackState.BROWSER_BACK
            } else {
              BackState.FINISH_ACTIVITY
            }
          }
        } else {
          flowOf(BackState.FINISH_ACTIVITY)
        }
      }
      .stateIn(viewModelScope, SharingStarted.WhileSubscribed(), BackState.FINISH_ACTIVITY)

  init {
    viewModelScope.launch(Dispatchers.IO) { newTabIfNone() }
  }

  fun openUrl(url: String) {
    val url =
      if (URLUtil.isNetworkUrl(url)) {
        url
      } else {
        Uri.Builder()
          .scheme("https")
          .authority("google.com")
          .appendPath("search")
          .appendQueryParameter("q", url)
          .build()
          .toString()
      }
    browserManager.getSession(checkNotNull(viewState.value.currentTab)).loadUri(url)
  }

  fun goBack() {
    browserManager.getSession(checkNotNull(viewState.value.currentTab)).goBack()
  }

  fun newTab(url: String = "https://www.google.com") {
    viewModelScope.launch(Dispatchers.IO) {
      _tabId.value = browserDatabase.browserTabDao().upsert(BrowserTab(url = url)).toInt()
    }
  }

  fun changeTab(tabId: Int) {
    _tabId.value = tabId
  }

  fun closeTab(tabId: Int? = _tabId.value) {
    if (tabId == null) return
    viewModelScope.launch {
      withContext(Dispatchers.IO) { browserDatabase.browserTabDao().delete(tabId) }
      browserManager.closeSession(tabId)
      newTabIfNone()
    }
  }

  private suspend fun newTabIfNone() =
    withContext(Dispatchers.IO) {
      if (browserDatabase.browserTabDao().isEmpty()) {
        newTab()
      }
    }
}

data class ViewState(val currentTab: BrowserTab? = null, val allTabs: List<BrowserTab> = listOf())

enum class BackState {
  BROWSER_BACK,
  FINISH_ACTIVITY,
}
