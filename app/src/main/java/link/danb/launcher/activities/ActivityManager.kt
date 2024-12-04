package link.danb.launcher.activities

import android.content.Context
import android.content.pm.LauncherApps
import androidx.core.content.getSystemService
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
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
import link.danb.launcher.profiles.Profile
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

  val activities: Flow<ImmutableList<UserActivity>> =
    combine(
        profileManager.profiles,
        callbackFlow<Unit> {
          send(Unit)
          val callback = LauncherAppsCallback { _, _ -> trySend(Unit) }

          launcherApps.registerCallback(callback)
          awaitClose { launcherApps.unregisterCallback(callback) }
        },
      ) { profiles, _ ->
        profiles
          .asSequence()
          .filter { it.isEnabled || it.profile != Profile.PRIVATE }
          .map { profileManager.getUserHandle(it.profile) }
          .flatMap { launcherApps.getActivityList(null, it) }
          .filter { it.componentName.packageName != context.packageName }
          .map { UserActivity(it.componentName, checkNotNull(profileManager.getProfile(it.user))) }
          .toImmutableList()
      }
      .stateIn(MainScope(), SharingStarted.WhileSubscribed(), persistentListOf())

  val data: Flow<ImmutableList<ActivityData>> =
    combine(activities, launcherDatabase.activityData().get()) { activities, data ->
        val dataMap =
          data
            .associateBy { it.userActivity }
            .withDefault { ActivityData(it, isPinned = false, isHidden = false) }

        activities.asSequence().map { component -> dataMap.getValue(component) }.toImmutableList()
      }
      .stateIn(
        MainScope(),
        SharingStarted.WhileSubscribed(replayExpirationMillis = 0),
        persistentListOf(),
      )
}
