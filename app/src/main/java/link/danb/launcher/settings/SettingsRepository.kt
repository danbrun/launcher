package link.danb.launcher.settings

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

@Singleton
class SettingsRepository
@Inject
constructor(@param:ApplicationContext private val context: Context) {

  private val Context.settingsDataStore: DataStore<Preferences> by preferencesDataStore("settings")

  val useMonochromeIcons: Flow<Boolean> =
    context.settingsDataStore.data.map { it[KEY_USE_MONOCHROME_ICONS] == true }

  suspend fun toggleUseMonochromeIcons() =
    context.settingsDataStore.edit {
      it[KEY_USE_MONOCHROME_ICONS] = it[KEY_USE_MONOCHROME_ICONS] != true
    }

  companion object {
    private val KEY_USE_MONOCHROME_ICONS = booleanPreferencesKey("use_monochrome_icons")
  }
}
