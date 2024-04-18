package link.danb.launcher.activities.details

import android.app.Application
import android.appwidget.AppWidgetManager
import android.graphics.drawable.Drawable
import android.os.Build
import androidx.lifecycle.AndroidViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
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

  val activityDetails: Flow<ActivityDetails?> =
    combineTransform(_details, activityManager.data, profileManager.profiles) {
      userActivity,
      activityDataList,
      profiles ->
      val activityData = activityDataList.firstOrNull { it.userActivity == userActivity }
      if (userActivity != null && activityData != null) {
        val isProfileEnabled =
          activityData.userActivity.userHandle == profiles.personal ||
            (profiles is PersonalAndWorkProfiles && profiles.isWorkEnabled)

        val data =
          ActivityDetails(
            activityData,
            launcherResourceProvider.getIconWithCache(userActivity).await(),
            launcherResourceProvider.getLabel(userActivity),
            shortcutsAndWidgets =
              if (isProfileEnabled) ShortcutsAndWidgets.Loading
              else ShortcutsAndWidgets.ProfileDisabled,
          )
        emit(data)

        if (isProfileEnabled) {
          emit(
            data.copy(
              shortcutsAndWidgets =
                ShortcutsAndWidgets.Loaded(
                  getShortcuts(userActivity),
                  getShortcutCreators(userActivity),
                  getWidgets(userActivity),
                )
            )
          )
        }
      } else {
        emit(null)
      }
    }

  fun showActivityDetails(activity: UserActivity) {
    _details.value = activity
  }

  fun hideActivityDetails() {
    _details.value = null
  }

  private suspend fun getShortcuts(userActivity: UserActivity) =
    shortcutManager
      .getShortcuts(userActivity)
      .map {
        ShortcutViewData(
          it,
          launcherResourceProvider.getIcon(it),
          launcherResourceProvider.getLabel(it),
        )
      }
      .sortedBy { it.name }

  private suspend fun getShortcutCreators(userActivity: UserActivity) =
    shortcutManager
      .getShortcutCreators(userActivity)
      .map {
        ShortcutCreatorViewData(
          it,
          launcherResourceProvider.getIcon(it),
          launcherResourceProvider.getLabel(it),
        )
      }
      .sortedBy { it.name }

  private suspend fun getWidgets(userActivity: UserActivity) =
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

  data class ActivityDetails(
    val activityData: ActivityData,
    val icon: Drawable,
    val name: String,
    val shortcutsAndWidgets: ShortcutsAndWidgets,
  )

  sealed interface ShortcutsAndWidgets {
    data object Loading : ShortcutsAndWidgets

    data object ProfileDisabled : ShortcutsAndWidgets

    data class Loaded(
      val shortcuts: List<ShortcutViewData>,
      val configurableShortcuts: List<ShortcutCreatorViewData>,
      val widgets: List<WidgetPreviewData>,
    ) : ShortcutsAndWidgets
  }

  data class ShortcutViewData(val userShortcut: UserShortcut, val icon: Drawable, val name: String)

  data class ShortcutCreatorViewData(
    val userShortcutCreator: UserShortcutCreator,
    val icon: Drawable,
    val name: String,
  )
}
