package link.danb.launcher.shortcuts

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
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.flow.transform
import kotlinx.coroutines.plus
import link.danb.launcher.activities.ActivityManager
import link.danb.launcher.activities.details.ActivityDetailsViewModel
import link.danb.launcher.apps.LauncherResourceProvider
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

  fun getPinShortcutsViewData(profile: Profile): StateFlow<PinShortcutsViewData> =
    activityManager.data
      .transform { data ->
        emit(PinShortcutsViewData.Loading)

        emit(
          PinShortcutsViewData.Loaded(
            data
              .asFlow()
              .filter { it.userActivity.profile == profile }
              .transform { emitAll(shortcutManager.getShortcutCreators(it.userActivity).asFlow()) }
              .map {
                ActivityDetailsViewModel.ShortcutCreatorViewData(
                  it,
                  launcherResourceProvider.getTileData(it),
                )
              }
              .toList()
              .sortedBy { it.launcherTileData.name.lowercase() }
              .toImmutableList()
          )
        )
      }
      .stateIn(
        viewModelScope + Dispatchers.IO,
        SharingStarted.WhileSubscribed(),
        PinShortcutsViewData.Loading,
      )

  sealed interface PinShortcutsViewData {

    data object Loading : PinShortcutsViewData

    data class Loaded(
      val shortcutCreators: ImmutableList<ActivityDetailsViewModel.ShortcutCreatorViewData>
    ) : PinShortcutsViewData
  }
}
