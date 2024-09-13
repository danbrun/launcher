package link.danb.launcher.shortcuts

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.combineTransform
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.flow.transform
import link.danb.launcher.activities.ActivityManager
import link.danb.launcher.activities.details.ActivityDetailsViewModel
import link.danb.launcher.apps.LauncherResourceProvider
import link.danb.launcher.profiles.Profile
import link.danb.launcher.profiles.ProfileManager

@HiltViewModel
class PinShortcutsViewModel
@Inject
constructor(
  application: Application,
  activityManager: ActivityManager,
  private val launcherResourceProvider: LauncherResourceProvider,
  private val shortcutManager: ShortcutManager,
  private val profileManager: ProfileManager,
) : AndroidViewModel(application) {

  private val showPinShortcuts: MutableStateFlow<Profile?> = MutableStateFlow(null)

  val pinShortcutsViewData: Flow<PinShortcutsViewData?> =
    combineTransform(showPinShortcuts, activityManager.data) { profile, data ->
      if (profile != null) {
        emit(PinShortcutsViewData.Loading)

        emit(
          PinShortcutsViewData.Loaded(
            data
              .asFlow()
              .filter { it.userActivity.userHandle == profileManager.getUserHandle(profile) }
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
      } else {
        emit(null)
      }
    }

  fun showPinShortcuts(profile: Profile) {
    showPinShortcuts.value = profile
  }

  fun hidePinShortcuts() {
    showPinShortcuts.value = null
  }

  sealed interface PinShortcutsViewData {

    data object Loading : PinShortcutsViewData

    data class Loaded(
      val shortcutCreators: ImmutableList<ActivityDetailsViewModel.ShortcutCreatorViewData>
    ) : PinShortcutsViewData
  }
}
