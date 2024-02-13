package link.danb.launcher.activities

import android.app.Application
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
import link.danb.launcher.data.UserActivity
import link.danb.launcher.database.ActivityData
import link.danb.launcher.database.LauncherDatabase

/** View model for launch icons. */
@HiltViewModel
class ActivitiesViewModel
@Inject
constructor(
  private val application: Application,
  launcherDatabase: LauncherDatabase,
  private val launcherApps: LauncherApps,
) : AndroidViewModel(application) {

  private val activityData = launcherDatabase.activityData()

  private val activityComponents: Flow<List<UserActivity>> = callbackFlow {
    val components =
      launcherApps.profiles
        .flatMap { launcherApps.getActivityList(null, it) }
        .filter { it.componentName.packageName != application.packageName }
        .map { UserActivity(it.componentName, it.user) }
        .toMutableList()
    trySend(components)

    val callback = LauncherAppsCallback { packageNames, userHandle ->
      synchronized(this) {
        components.removeIf {
          it.componentName.packageName in packageNames && it.userHandle == userHandle
        }
        components.addAll(
          packageNames
            .flatMap { launcherApps.getActivityList(it, userHandle) }
            .map { UserActivity(it.componentName, it.user) }
        )
        trySend(components.toList())
      }
    }

    launcherApps.registerCallback(callback)
    awaitClose { launcherApps.unregisterCallback(callback) }
  }

  val activities: Flow<List<ActivityData>> =
    combine(activityComponents, activityData.get()) { components, data ->
        val dataMap =
          data
            .associateBy { it.userActivity }
            .withDefault { ActivityData(it, isPinned = false, isHidden = false, tags = setOf()) }

        components.map { component -> dataMap.getValue(component) }
      }
      .shareIn(viewModelScope, SharingStarted.WhileSubscribed(), replay = 1)

  fun launchActivity(userActivity: UserActivity, sourceBounds: Rect, opts: Bundle) {
    launcherApps.startMainActivity(
      userActivity.componentName,
      userActivity.userHandle,
      sourceBounds,
      opts,
    )
  }

  fun launchAppDetails(userActivity: UserActivity, sourceBounds: Rect, opts: Bundle) {
    launcherApps.startAppDetailsActivity(
      userActivity.componentName,
      userActivity.userHandle,
      sourceBounds,
      opts,
    )
  }

  fun putMetadataInBackground(activityMetadata: ActivityData) =
    viewModelScope.launch { putMetadata(activityMetadata) }

  private suspend fun putMetadata(activityMetadata: ActivityData) =
    withContext(Dispatchers.IO) { activityData.put(activityMetadata) }
}
