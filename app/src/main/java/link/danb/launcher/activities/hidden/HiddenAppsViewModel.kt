package link.danb.launcher.activities.hidden

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.flow.transform
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

  fun getHiddenApps(profile: Profile): Flow<HiddenAppsViewData?> =
    activityManager.data.transform { data ->
      emit(HiddenAppsViewData.Loading)

      emit(
        HiddenAppsViewData.Loaded(
          data
            .asFlow()
            .filter { it.isHidden && it.userActivity.profile == profile }
            .map {
              ActivityViewData(
                it.userActivity,
                launcherResourceProvider.getTileData(it.userActivity),
              )
            }
            .toList()
            .sortedBy { it.launcherTileData.name.lowercase() }
            .toImmutableList()
        )
      )
    }

  data class ActivityViewData(
    val userActivity: UserActivity,
    val launcherTileData: LauncherTileData,
  )

  sealed interface HiddenAppsViewData {
    data object Loading : HiddenAppsViewData

    data class Loaded(val apps: ImmutableList<ActivityViewData>) : HiddenAppsViewData
  }
}
