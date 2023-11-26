package link.danb.launcher.activities

import android.app.Application
import android.content.pm.LauncherActivityInfo
import android.content.pm.LauncherApps
import android.graphics.Rect
import android.os.Bundle
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import link.danb.launcher.apps.LauncherAppsCallback
import link.danb.launcher.database.ActivityData
import link.danb.launcher.database.LauncherDatabase
import link.danb.launcher.extensions.toDefaultActivityData

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

  private val launcherActivityInfo: Flow<List<LauncherActivityInfo>> =
    callbackFlow {
        val callback = LauncherAppsCallback { _, _ -> trySend(getAllActivities()) }
        launcherApps.registerCallback(callback)
        awaitClose { launcherApps.unregisterCallback(callback) }
      }
      .onStart { emit(getAllActivities()) }

  val activities: Flow<List<ActivityData>> =
    combine(launcherActivityInfo, activityData.get()) { infoList, dataList ->
        infoList.map { info ->
          dataList.firstOrNull { data ->
            info.componentName == data.componentName && info.user == data.userHandle
          } ?: info.toDefaultActivityData()
        }
      }
      .shareIn(viewModelScope, SharingStarted.WhileSubscribed(), replay = 1)

  fun launchActivity(activityData: ActivityData, sourceBounds: Rect, opts: Bundle) {
    launcherApps.startMainActivity(
      activityData.componentName,
      activityData.userHandle,
      sourceBounds,
      opts
    )
  }

  fun launchAppDetails(activityData: ActivityData, sourceBounds: Rect, opts: Bundle) {
    launcherApps.startAppDetailsActivity(
      activityData.componentName,
      activityData.userHandle,
      sourceBounds,
      opts
    )
  }

  fun putMetadataInBackground(activityMetadata: ActivityData) =
    viewModelScope.launch { putMetadata(activityMetadata) }

  private suspend fun putMetadata(activityMetadata: ActivityData) =
    withContext(Dispatchers.IO) { activityData.put(activityMetadata) }

  private fun getAllActivities(): List<LauncherActivityInfo> =
    launcherApps.profiles.flatMap { launcherApps.getActivityList(null, it) }
}
