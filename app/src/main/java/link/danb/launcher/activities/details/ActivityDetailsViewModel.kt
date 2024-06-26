package link.danb.launcher.activities.details

import android.app.Application
import android.appwidget.AppWidgetManager
import android.os.Build
import androidx.lifecycle.AndroidViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.combineTransform
import kotlinx.coroutines.withContext
import link.danb.launcher.activities.ActivityManager
import link.danb.launcher.apps.LauncherResourceProvider
import link.danb.launcher.components.UserActivity
import link.danb.launcher.components.UserApplication
import link.danb.launcher.components.UserShortcut
import link.danb.launcher.components.UserShortcutCreator
import link.danb.launcher.database.ActivityData
import link.danb.launcher.profiles.PersonalAndWorkProfiles
import link.danb.launcher.profiles.ProfileManager
import link.danb.launcher.shortcuts.ShortcutManager
import link.danb.launcher.ui.LauncherIconData
import link.danb.launcher.ui.LauncherTileData
import link.danb.launcher.ui.WidgetPreviewData

@HiltViewModel
class ActivityDetailsViewModel
@Inject
constructor(
  activityManager: ActivityManager,
  private val application: Application,
  private val appWidgetManager: AppWidgetManager,
  private val launcherResourceProvider: LauncherResourceProvider,
  profileManager: ProfileManager,
  private val shortcutManager: ShortcutManager,
) : AndroidViewModel(application) {

  private val _details: MutableStateFlow<UserActivity?> = MutableStateFlow(null)

  private val shortcutsAndWidgets: Flow<ShortcutsAndWidgets?> =
    combineTransform(_details, profileManager.profiles) { activity, profiles ->
      if (activity != null) {
        if (
          activity.userHandle == profiles.personal ||
            (profiles is PersonalAndWorkProfiles && profiles.isWorkEnabled)
        ) {
          emit(ShortcutsAndWidgets.Loading)

          emit(
            ShortcutsAndWidgets.Loaded(
              getShortcuts(activity),
              getShortcutCreators(activity),
              getWidgets(activity),
            )
          )
        } else {
          emit(ShortcutsAndWidgets.ProfileDisabled)
        }
      } else {
        emit(null)
      }
    }

  val activityDetails: Flow<ActivityDetails?> =
    combine(_details, activityManager.data, shortcutsAndWidgets) {
      activity,
      activityDataList,
      shortcutsAndWidgets ->
      val activityData = activityDataList.firstOrNull { it.userActivity == activity }
      if (activity != null && activityData != null && shortcutsAndWidgets != null) {
        ActivityDetails(
          activityData,
          LauncherTileData(
            LauncherIconData(
              launcherResourceProvider.getIcon(activity),
              launcherResourceProvider.getBadge(activity.userHandle),
            ),
            launcherResourceProvider.getLabel(activity),
          ),
          shortcutsAndWidgets,
        )
      } else {
        null
      }
    }

  fun showActivityDetails(activity: UserActivity) {
    _details.value = activity
  }

  fun hideActivityDetails() {
    _details.value = null
  }

  private suspend fun getShortcuts(userActivity: UserActivity): ImmutableList<ShortcutViewData> =
    shortcutManager
      .getShortcuts(userActivity)
      .map {
        ShortcutViewData(
          it,
          LauncherTileData(
            LauncherIconData(
              launcherResourceProvider.getIcon(it),
              launcherResourceProvider.getBadge(it.userHandle),
            ),
            launcherResourceProvider.getLabel(it),
          ),
        )
      }
      .sortedBy { it.launcherTileData.name }
      .toImmutableList()

  private suspend fun getShortcutCreators(
    userActivity: UserActivity
  ): ImmutableList<ShortcutCreatorViewData> =
    shortcutManager
      .getShortcutCreators(userActivity)
      .map {
        ShortcutCreatorViewData(
          it,
          LauncherTileData(
            LauncherIconData(
              launcherResourceProvider.getIcon(it),
              launcherResourceProvider.getBadge(it.userHandle),
            ),
            launcherResourceProvider.getLabel(it),
          ),
        )
      }
      .sortedBy { it.launcherTileData.name }
      .toImmutableList()

  private suspend fun getWidgets(userActivity: UserActivity): ImmutableList<WidgetPreviewData> =
    appWidgetManager
      .getInstalledProvidersForPackage(
        userActivity.componentName.packageName,
        userActivity.userHandle,
      )
      .map {
        WidgetPreviewData(
          it,
          withContext(Dispatchers.IO) { it.loadPreviewImage(application, 0) }
            ?: launcherResourceProvider.getIcon(
              UserApplication(it.provider.packageName, it.profile)
            ),
          it.loadLabel(application.packageManager),
          if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            it.loadDescription(application)?.toString()
          } else {
            null
          },
        )
      }
      .toImmutableList()

  data class ActivityDetails(
    val activityData: ActivityData,
    val launcherTileData: LauncherTileData,
    val shortcutsAndWidgets: ShortcutsAndWidgets,
  )

  sealed interface ShortcutsAndWidgets {
    data object Loading : ShortcutsAndWidgets

    data object ProfileDisabled : ShortcutsAndWidgets

    data class Loaded(
      val shortcuts: ImmutableList<ShortcutViewData>,
      val configurableShortcuts: ImmutableList<ShortcutCreatorViewData>,
      val widgets: ImmutableList<WidgetPreviewData>,
    ) : ShortcutsAndWidgets
  }

  data class ShortcutViewData(
    val userShortcut: UserShortcut,
    val launcherTileData: LauncherTileData,
  )

  data class ShortcutCreatorViewData(
    val userShortcutCreator: UserShortcutCreator,
    val launcherTileData: LauncherTileData,
  )
}
