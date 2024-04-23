package link.danb.launcher.activities.hidden

import android.app.Application
import android.os.UserHandle
import androidx.lifecycle.AndroidViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.combineTransform
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.toList
import link.danb.launcher.activities.ActivityManager
import link.danb.launcher.activities.details.ActivityDetailsViewModel
import link.danb.launcher.apps.LauncherResourceProvider

sealed interface HiddenAppsViewData {
  data object Loading : HiddenAppsViewData

  data class Loaded(val apps: List<ActivityDetailsViewModel.ActivityViewData>) : HiddenAppsViewData
}

@HiltViewModel
class HiddenAppsViewModel
@Inject
constructor(
  application: Application,
  activityManager: ActivityManager,
  private val launcherResourceProvider: LauncherResourceProvider,
) : AndroidViewModel(application) {

  private val showHiddenApps: MutableStateFlow<UserHandle?> = MutableStateFlow(null)

  val hiddenApps: Flow<HiddenAppsViewData?> =
    combineTransform(showHiddenApps, activityManager.data) { user, data ->
      if (user != null) {
        emit(HiddenAppsViewData.Loading)

        emit(
          HiddenAppsViewData.Loaded(
            data
              .asFlow()
              .filter { it.isHidden && it.userActivity.userHandle == user }
              .map {
                ActivityDetailsViewModel.ActivityViewData(
                  it,
                  launcherResourceProvider.getSourceIcon(it.userActivity),
                  launcherResourceProvider.getBadge(it.userActivity.userHandle),
                  launcherResourceProvider.getLabel(it.userActivity),
                )
              }
              .toList()
          )
        )
      } else {
        emit(null)
      }
    }

  fun showHiddenApps(userHandle: UserHandle) {
    showHiddenApps.value = userHandle
  }

  fun hideHiddenApps() {
    showHiddenApps.value = null
  }
}
