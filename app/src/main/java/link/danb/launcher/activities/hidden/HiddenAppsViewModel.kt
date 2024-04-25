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
import link.danb.launcher.apps.LauncherResourceProvider
import link.danb.launcher.components.UserActivity
import link.danb.launcher.ui.IconTileViewData

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
                ActivityViewData(
                  it.userActivity,
                  IconTileViewData(
                    launcherResourceProvider.getSourceIcon(it.userActivity),
                    launcherResourceProvider.getBadge(it.userActivity.userHandle),
                    launcherResourceProvider.getLabel(it.userActivity),
                  ),
                )
              }
              .toList()
              .sortedBy { it.iconTileViewData.name.lowercase() }
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

  data class ActivityViewData(
    val userActivity: UserActivity,
    val iconTileViewData: IconTileViewData,
  )

  sealed interface HiddenAppsViewData {
    data object Loading : HiddenAppsViewData

    data class Loaded(val apps: List<ActivityViewData>) : HiddenAppsViewData
  }
}
