package link.danb.launcher.activities

import android.content.Context
import android.content.pm.LauncherApps
import android.graphics.Rect
import android.os.Bundle
import androidx.core.content.getSystemService
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import link.danb.launcher.apps.LauncherAppsCallback
import link.danb.launcher.components.UserActivity
import link.danb.launcher.database.ActivityData
import link.danb.launcher.database.LauncherDatabase
import link.danb.launcher.profiles.ProfileManager

@Singleton
class ActivityManager
@Inject
constructor(
  @ApplicationContext context: Context,
  launcherDatabase: LauncherDatabase,
  private val profileManager: ProfileManager,
) {

  private val launcherApps: LauncherApps by lazy { checkNotNull(context.getSystemService()) }

  val activities: Flow<List<UserActivity>> =
    callbackFlow {
        var components =
          launcherApps.profiles
            .flatMap { launcherApps.getActivityList(null, it) }
            .filter { it.componentName.packageName != context.packageName }
            .map { UserActivity(it.componentName, profileManager.getProfile(it.user)) }
        trySend(components)

        val callback = LauncherAppsCallback { packageNames, userHandle ->
          synchronized(this) {
            components = buildList {
              addAll(
                components.filter {
                  it.componentName.packageName !in packageNames ||
                    it.profile != profileManager.getProfile(userHandle)
                }
              )
              addAll(
                packageNames
                  .flatMap { launcherApps.getActivityList(it, userHandle) }
                  .filter { it.componentName.packageName != context.packageName }
                  .map { UserActivity(it.componentName, profileManager.getProfile(it.user)) }
              )
            }
            trySend(components)
          }
        }

        launcherApps.registerCallback(callback)
        awaitClose { launcherApps.unregisterCallback(callback) }
      }
      .stateIn(MainScope(), SharingStarted.WhileSubscribed(), listOf())

  val data: Flow<List<ActivityData>> =
    combine(activities, launcherDatabase.activityData().get()) { activities, data ->
        val dataMap =
          data
            .associateBy { it.userActivity }
            .withDefault { ActivityData(it, isPinned = false, isHidden = false) }

        activities.map { component -> dataMap.getValue(component) }
      }
      .stateIn(MainScope(), SharingStarted.WhileSubscribed(replayExpirationMillis = 0), listOf())

  fun launchActivity(userActivity: UserActivity, sourceBounds: Rect, opts: Bundle) {
    launcherApps.startMainActivity(
      userActivity.componentName,
      profileManager.getUserHandle(userActivity.profile),
      sourceBounds,
      opts,
    )
  }

  fun launchAppDetails(userActivity: UserActivity, sourceBounds: Rect, opts: Bundle) {
    launcherApps.startAppDetailsActivity(
      userActivity.componentName,
      profileManager.getUserHandle(userActivity.profile),
      sourceBounds,
      opts,
    )
  }
}
