package link.danb.launcher.activities.details

import android.app.Application
import android.appwidget.AppWidgetManager
import android.os.Build
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.transform
import kotlinx.coroutines.plus
import kotlinx.coroutines.withContext
import link.danb.launcher.activities.ActivityManager
import link.danb.launcher.apps.LauncherResourceProvider
import link.danb.launcher.components.UserActivity
import link.danb.launcher.components.UserApplication
import link.danb.launcher.components.UserShortcut
import link.danb.launcher.components.UserShortcutCreator
import link.danb.launcher.database.ActivityData
import link.danb.launcher.profiles.ProfileManager
import link.danb.launcher.profiles.ProfileState
import link.danb.launcher.shortcuts.ShortcutManager
import link.danb.launcher.ui.LauncherTileData
import link.danb.launcher.ui.WidgetPreviewData

@HiltViewModel
class ActivityDetailsViewModel
@Inject
constructor(
  private val activityManager: ActivityManager,
  private val application: Application,
  private val appWidgetManager: AppWidgetManager,
  private val launcherResourceProvider: LauncherResourceProvider,
  private val profileManager: ProfileManager,
  private val shortcutManager: ShortcutManager,
) : AndroidViewModel(application) {

  fun getActivityDetails(userActivity: UserActivity): StateFlow<ActivityDetailsData?> =
    combine(activityManager.data, getShortcutsAndWidgets(userActivity)) {
        activityDataList,
        shortcutsAndWidgets ->
        val activityData = activityDataList.firstOrNull { it.userActivity == userActivity }
        if (activityData != null && shortcutsAndWidgets != null) {
          ActivityDetailsData(
            activityData,
            launcherResourceProvider.getTileData(activityData.userActivity),
            shortcutsAndWidgets,
          )
        } else {
          null
        }
      }
      .stateIn(viewModelScope + Dispatchers.IO, SharingStarted.WhileSubscribed(), null)

  private fun getShortcutsAndWidgets(userActivity: UserActivity): Flow<ShortcutsAndWidgets?> =
    profileManager.profiles.transform { profiles ->
      when (profiles[userActivity.profile]) {
        ProfileState.ENABLED -> {
          emit(ShortcutsAndWidgets.Loading)

          emit(
            ShortcutsAndWidgets.Loaded(
              getShortcuts(userActivity),
              getShortcutCreators(userActivity),
              getWidgets(userActivity),
            )
          )
        }
        ProfileState.DISABLED -> {
          emit(ShortcutsAndWidgets.ProfileDisabled)
        }
        null -> {
          emit(null)
        }
      }
    }

  private suspend fun getShortcuts(userActivity: UserActivity): ImmutableList<ShortcutViewData> =
    shortcutManager
      .getShortcuts(userActivity)
      .map { ShortcutViewData(it, launcherResourceProvider.getTileData(it)) }
      .sortedBy { it.launcherTileData.name }
      .toImmutableList()

  private suspend fun getShortcutCreators(
    userActivity: UserActivity
  ): ImmutableList<ShortcutCreatorViewData> =
    shortcutManager
      .getShortcutCreators(userActivity)
      .map { ShortcutCreatorViewData(it, launcherResourceProvider.getTileData(it)) }
      .sortedBy { it.launcherTileData.name }
      .toImmutableList()

  private suspend fun getWidgets(userActivity: UserActivity): ImmutableList<WidgetPreviewData> =
    appWidgetManager
      .getInstalledProvidersForPackage(
        userActivity.componentName.packageName,
        profileManager.getUserHandle(userActivity.profile),
      )
      .map {
        WidgetPreviewData(
          it,
          withContext(Dispatchers.IO) { it.loadPreviewImage(application, 0) }
            ?: launcherResourceProvider.getIcon(
              UserApplication(it.provider.packageName, profileManager.getProfile(it.profile))
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

  data class ActivityDetailsData(
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
