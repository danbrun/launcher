package link.danb.launcher.settings

import android.app.Application
import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

val Context.settingsDataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

@HiltViewModel
class SettingsViewModel @Inject constructor(private val application: Application) :
  AndroidViewModel(application) {

  val useMonochromeIcons: StateFlow<Boolean> =
    application.settingsDataStore.data
      .map { it[KEY_USE_MONOCHROME_ICONS] ?: false }
      .stateIn(viewModelScope, SharingStarted.WhileSubscribed(), false)

  fun setUseMonochromeIcons(value: Boolean) {
    viewModelScope.launch(Dispatchers.IO) {
      application.settingsDataStore.edit { it[KEY_USE_MONOCHROME_ICONS] = value }
    }
  }

  companion object {
    private val KEY_USE_MONOCHROME_ICONS = booleanPreferencesKey("A")
  }
}
