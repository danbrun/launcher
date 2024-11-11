package link.danb.launcher.activities.hidden

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.plus
import link.danb.launcher.activities.ActivityManager
import link.danb.launcher.apps.LauncherResourceProvider
import link.danb.launcher.components.UserActivity
import link.danb.launcher.profiles.Profile
import link.danb.launcher.ui.LauncherTileData

@HiltViewModel
class HiddenAppsViewModel
@Inject
constructor(
  application: Application,
  private val activityManager: ActivityManager,
  private val launcherResourceProvider: LauncherResourceProvider,
) : AndroidViewModel(application) {

  fun getState(profile: Profile): StateFlow<State> =
    activityManager.data
      .map { data ->
        State.Loaded(
          data
            .asFlow()
            .filter { it.isHidden && it.userActivity.profile == profile }
            .map {
              State.Loaded.Item(
                it.userActivity,
                launcherResourceProvider.getTileData(it.userActivity),
              )
            }
            .toList()
            .sortedBy { it.launcherTileData.name.lowercase() }
            .toImmutableList()
        )
      }
      .stateIn(viewModelScope + Dispatchers.IO, SharingStarted.WhileSubscribed(), State.Loading)

  sealed interface State {
    data object Loading : State

    data class Loaded(val items: ImmutableList<Item>) : State {

      data class Item(val userActivity: UserActivity, val launcherTileData: LauncherTileData)
    }
  }
}
