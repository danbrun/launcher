package link.danb.launcher.activities

import android.app.Application
import android.content.pm.LauncherActivityInfo
import android.content.pm.LauncherApps
import android.os.UserHandle
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import link.danb.launcher.apps.LauncherAppsCallback
import link.danb.launcher.database.ActivityData
import link.danb.launcher.database.LauncherDatabase

/** View model for launch icons. */
@HiltViewModel
class ActivitiesViewModel
@Inject
constructor(
  application: Application,
  launcherDatabase: LauncherDatabase,
  private val launcherApps: LauncherApps,
) : AndroidViewModel(application) {

  private val activityData = launcherDatabase.activityData()
  private val launcherAppsCallback = LauncherAppsCallback { packageNames, user ->
    viewModelScope.launch { update(packageNames, user) }
  }

  private val _activities = MutableStateFlow<List<ActivityData>>(listOf())

  val activities: StateFlow<List<ActivityData>> = _activities.asStateFlow()

  init {
    viewModelScope.launch { replace() }

    launcherApps.registerCallback(launcherAppsCallback)
  }

  override fun onCleared() {
    super.onCleared()

    launcherApps.unregisterCallback(launcherAppsCallback)
  }

  fun putMetadataInBackground(activityMetadata: ActivityData) =
    viewModelScope.launch { putMetadata(activityMetadata) }

  fun getInfo(activityData: ActivityData): LauncherActivityInfo =
    launcherApps
      .getActivityList(activityData.componentName.packageName, activityData.userHandle)
      .first { it.componentName == activityData.componentName }

  private suspend fun putMetadata(activityMetadata: ActivityData) =
    withContext(Dispatchers.IO) {
      activityData.put(activityMetadata)
      update(listOf(activityMetadata.componentName.packageName), activityMetadata.userHandle)
    }

  private suspend fun getMetadata(info: LauncherActivityInfo): ActivityData =
    activityData.get(info.componentName, info.user)
      ?: ActivityData(info.componentName, info.user, isHidden = false, tags = setOf())

  private suspend fun update(packageNames: List<String>, user: UserHandle) =
    withContext(Dispatchers.IO) {
      _activities.emit(
        buildList {
          addAll(
            _activities.value.filter {
              it.componentName.packageName !in packageNames || it.userHandle != user
            }
          )
          addAll(
            packageNames.flatMap { launcherApps.getActivityList(it, user) }.map { getMetadata(it) }
          )
        }
      )
    }

  private suspend fun replace() =
    withContext(Dispatchers.IO) {
      _activities.emit(
        launcherApps.profiles
          .flatMap { launcherApps.getActivityList(null, it) }
          .map { getMetadata(it) }
      )
    }
}
