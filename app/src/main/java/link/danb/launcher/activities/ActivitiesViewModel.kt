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
  private val launcherAppsCallback = LauncherAppsCallback { packageName, user ->
    viewModelScope.launch { update(packageName, user) }
  }

  private val _activities = MutableStateFlow<List<ActivityInfoWithData>>(listOf())
  private val _sortByCategory = MutableStateFlow<Boolean>(false)

  val activities: StateFlow<List<ActivityInfoWithData>> = _activities.asStateFlow()
  val sortByCategory: StateFlow<Boolean> = _sortByCategory.asStateFlow()

  init {
    viewModelScope.launch { replace() }

    launcherApps.registerCallback(launcherAppsCallback)
  }

  override fun onCleared() {
    super.onCleared()

    launcherApps.unregisterCallback(launcherAppsCallback)
  }

  fun toggleSortByCategory() {
    _sortByCategory.value = !_sortByCategory.value
  }

  fun putMetadataInBackground(activityMetadata: ActivityData) =
    viewModelScope.launch { putMetadata(activityMetadata) }

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
      _activities.value.toMutableList().apply {
        removeIf { it.info.componentName.packageName in packageNames && it.info.user == user }
        addAll(
          packageNames
            .flatMap { launcherApps.getActivityList(it, user) }
            .map { ActivityInfoWithData(it, getMetadata(it)) }
        )
        _activities.emit(toList())
      }
    }

  private suspend fun replace() =
    withContext(Dispatchers.IO) {
      _activities.emit(
        launcherApps.profiles
          .flatMap { launcherApps.getActivityList(null, it) }
          .map { ActivityInfoWithData(it, getMetadata(it)) }
      )
    }
}
