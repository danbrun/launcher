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
import link.danb.launcher.data.UserComponent
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

  private val launcherActivityInfo: Flow<List<UserComponent>> = callbackFlow {
    var activities =
      launcherApps.profiles
        .flatMap { launcherApps.getActivityList(null, it) }
        .filter { it.componentName.packageName != application.packageName }
        .map { UserComponent(it.componentName, it.user) }
    trySend(activities)

    val callback = LauncherAppsCallback { packageNames, userHandle ->
      synchronized(this) {
        activities =
          activities.filter {
            it.componentName.packageName !in packageNames || it.userHandle != userHandle
          } +
            packageNames
              .flatMap { launcherApps.getActivityList(it, userHandle) }
              .map { UserComponent(it.componentName, it.user) }
        trySend(activities)
      }
    }

    launcherApps.registerCallback(callback)
    awaitClose { launcherApps.unregisterCallback(callback) }
  }

  val activities: Flow<List<ActivityData>> =
    combine(launcherActivityInfo, activityData.get()) { components, dataList ->
        dataList.filter { it.userComponent in components } +
          components
            .filter { component -> dataList.none { it.userComponent == component } }
            .map { ActivityData(it, isPinned = false, isHidden = false, tags = setOf()) }
      }
      .shareIn(viewModelScope, SharingStarted.WhileSubscribed(), replay = 1)

  fun launchActivity(userComponent: UserComponent, sourceBounds: Rect, opts: Bundle) {
    launcherApps.startMainActivity(
      userComponent.componentName,
      userComponent.userHandle,
      sourceBounds,
      opts,
    )
  }

  fun launchAppDetails(userComponent: UserComponent, sourceBounds: Rect, opts: Bundle) {
    launcherApps.startAppDetailsActivity(
      userComponent.componentName,
      userComponent.userHandle,
      sourceBounds,
      opts,
    )
  }

  fun putMetadataInBackground(activityMetadata: ActivityData) =
    viewModelScope.launch { putMetadata(activityMetadata) }

  private suspend fun putMetadata(activityMetadata: ActivityData) =
    withContext(Dispatchers.IO) { activityData.put(activityMetadata) }
}
