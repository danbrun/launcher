package link.danb.launcher.activities

import android.app.Application
import android.content.pm.LauncherActivityInfo
import android.content.pm.LauncherApps
import android.os.UserHandle
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import link.danb.launcher.apps.LauncherAppsCallback
import link.danb.launcher.database.ActivityData
import link.danb.launcher.database.LauncherDatabase
import javax.inject.Inject

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
  private val launcherAppsCallback = LauncherAppsCallback(this::update)

  private val _activities = MutableStateFlow<List<ActivityInfoWithData>>(listOf())

  val activities: StateFlow<List<ActivityInfoWithData>> = _activities.asStateFlow()

  init {
    viewModelScope.launch(Dispatchers.IO) {
      _activities.emit(
        launcherApps.profiles
          .flatMap { launcherApps.getActivityList(null, it) }
          .map { ActivityInfoWithData(it, getMetadata(it)) }
      )
    }

    launcherApps.registerCallback(launcherAppsCallback)
  }

  override fun onCleared() {
    super.onCleared()

    launcherApps.unregisterCallback(launcherAppsCallback)
  }

  /** Sets the list of tags to associate with the given [ActivityInfoWithData] */
  // May use this again soon so leaving it for now.
  @Suppress("unused")
  fun updateTags(info: LauncherActivityInfo, tags: Set<String>) {
    viewModelScope.launch(Dispatchers.IO) {
      activityData.put(getMetadata(info).copy(tags = tags))
      update(info)
    }
  }

  /** Sets the visibility of the given app. */
  fun setIsHidden(info: LauncherActivityInfo, isHidden: Boolean) {
    viewModelScope.launch(Dispatchers.IO) {
      activityData.put(getMetadata(info).copy(isHidden = isHidden))
      update(info)
    }
  }

  private fun getMetadata(info: LauncherActivityInfo): ActivityData =
    activityData.get(info.componentName, info.user)
      ?: ActivityData(info.componentName, info.user, isHidden = false, tags = setOf())

  private fun update(info: LauncherActivityInfo) {
    update(info.componentName.packageName, info.user)
  }

  private fun update(packageName: String, user: UserHandle) {
    viewModelScope.launch(Dispatchers.IO) {
      _activities.value.toMutableList().apply {
        removeIf { it.info.componentName.packageName == packageName && it.info.user == user }
        addAll(
          launcherApps.getActivityList(packageName, user).map {
            ActivityInfoWithData(it, getMetadata(it))
          }
        )
        _activities.emit(toList())
      }
    }
  }
}
