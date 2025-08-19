package link.danb.launcher.shortcuts

import android.app.Application
import android.content.Intent
import android.content.IntentSender
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
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.flow.transform
import kotlinx.coroutines.plus
import link.danb.launcher.activities.ActivityManager
import link.danb.launcher.apps.LauncherResourceProvider
import link.danb.launcher.components.UserShortcutCreator
import link.danb.launcher.profiles.Profile

@HiltViewModel
class PinShortcutsViewModel
@Inject
constructor(
  application: Application,
  private val activityManager: ActivityManager,
  private val launcherResourceProvider: LauncherResourceProvider,
  private val shortcutManager: ShortcutManager,
) : AndroidViewModel(application) {

  fun getState(profile: Profile): StateFlow<State> =
    activityManager.data
      .map { data ->
        State.Loaded(
          data
            .asFlow()
            .filter { it.userActivity.profile == profile }
            .transform { emitAll(shortcutManager.getShortcutCreators(it.userActivity).asFlow()) }
            .map { State.Loaded.Item(it, shortcutManager.getShortcutCreatorIntent(it)) }
            .toList()
            .sortedBy { launcherResourceProvider.getLabel(it.userShortcutCreator).lowercase() }
            .toImmutableList()
        )
      }
      .stateIn(viewModelScope + Dispatchers.IO, SharingStarted.WhileSubscribed(), State.Loading)

  fun acceptPinRequest(intent: Intent) {
    shortcutManager.acceptPinRequest(intent)
  }

  sealed interface State {

    data object Loading : State

    data class Loaded(val items: ImmutableList<Item>) : State {

      data class Item(
        val userShortcutCreator: UserShortcutCreator,
        val creatorIntent: IntentSender,
      )
    }
  }
}
