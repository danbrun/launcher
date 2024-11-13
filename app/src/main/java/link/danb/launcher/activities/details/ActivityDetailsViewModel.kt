package link.danb.launcher.activities.details

import android.app.Application
import android.appwidget.AppWidgetManager
import android.content.Intent
import android.net.Uri
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
import kotlinx.coroutines.launch
import kotlinx.coroutines.plus
import kotlinx.coroutines.withContext
import link.danb.launcher.activities.ActivityManager
import link.danb.launcher.apps.LauncherResourceProvider
import link.danb.launcher.components.UserActivity
import link.danb.launcher.components.UserApplication
import link.danb.launcher.components.UserShortcut
import link.danb.launcher.components.UserShortcutCreator
import link.danb.launcher.database.ActivityData
import link.danb.launcher.database.LauncherDatabase
import link.danb.launcher.profiles.ProfileManager
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
  private val launcherDatabase: LauncherDatabase,
  private val launcherResourceProvider: LauncherResourceProvider,
  private val profileManager: ProfileManager,
  private val shortcutManager: ShortcutManager,
) : AndroidViewModel(application) {

  fun getActivityDetails(userActivity: UserActivity): StateFlow<ActivityDetailsData> =
    combine(activityManager.data, getShortcutsAndWidgets(userActivity)) {
        activityDataList,
        shortcutsAndWidgets ->
        val activityData = activityDataList.firstOrNull { it.userActivity == userActivity }
        if (activityData != null && shortcutsAndWidgets != null) {
          Loaded(
            activityData,
            launcherResourceProvider.getTileData(activityData.userActivity),
            shortcutsAndWidgets,
          )
        } else {
          Missing
        }
      }
      .stateIn(viewModelScope + Dispatchers.IO, SharingStarted.WhileSubscribed(), Loading)

  fun toggleAppPinned(activityData: ActivityData) {
    putActivityData(activityData.copy(isPinned = !activityData.isPinned))
  }

  fun toggleAppHidden(activityData: ActivityData) {
    putActivityData(activityData.copy(isHidden = !activityData.isHidden))
  }

  fun getUninstallIntent(userActivity: UserActivity): Intent =
    Intent(Intent.ACTION_DELETE)
      .setData(Uri.parse("package:${userActivity.componentName.packageName}"))
      .putExtra(Intent.EXTRA_USER, profileManager.getUserHandle(userActivity.profile))

  private fun putActivityData(activityData: ActivityData) {
    viewModelScope.launch(Dispatchers.IO) { launcherDatabase.activityData().put(activityData) }
  }

  private fun getShortcutsAndWidgets(userActivity: UserActivity): Flow<ShortcutsAndWidgets?> =
    profileManager.profiles.transform { profiles ->
      when (profiles[userActivity.profile]?.isEnabled) {
        true -> {
          emit(ShortcutsAndWidgets.Loading)

          emit(
            ShortcutsAndWidgets.Loaded(
              getShortcuts(userActivity),
              getShortcutCreators(userActivity),
              getWidgets(userActivity),
            )
          )
        }
        false -> {
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

  sealed interface ActivityDetailsData

  data object Loading : ActivityDetailsData

  data object Missing : ActivityDetailsData

  data class Loaded(
    val activityData: ActivityData,
    val launcherTileData: LauncherTileData,
    val shortcutsAndWidgets: ShortcutsAndWidgets,
  ) : ActivityDetailsData

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
