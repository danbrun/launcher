package link.danb.launcher.browser

import android.net.Uri
import android.webkit.URLUtil
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
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
          .stateIn(
              viewModelScope,
              SharingStarted.WhileSubscribed(),
              ViewState(),
          )

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

  fun newTab() {
    viewModelScope.launch(Dispatchers.IO) {
      _tabId.value = browserDatabase.browserTabDao().upsert(BrowserTab()).toInt()
    }
  }

  fun changeTab(tabId: Int) {
    _tabId.value = tabId
  }

  fun closeTab(tab: BrowserTab) {
    viewModelScope.launch {
      withContext(Dispatchers.IO) { browserDatabase.browserTabDao().delete(tab) }
      browserManager.closeSession(tab.tabId)
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

data class ViewState(
    val currentTab: BrowserTab? = null,
    val allTabs: List<BrowserTab> = listOf(),
)
