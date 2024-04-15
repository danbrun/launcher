package link.danb.launcher.activities.details

import android.app.Application
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProviderInfo
import android.graphics.drawable.Drawable
import android.os.Build
import androidx.annotation.IdRes
import androidx.lifecycle.AndroidViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combineTransform
import link.danb.launcher.activities.ActivityManager
import link.danb.launcher.apps.LauncherResourceProvider
import link.danb.launcher.components.UserActivity
import link.danb.launcher.components.UserShortcut
import link.danb.launcher.components.UserShortcutCreator
import link.danb.launcher.database.ActivityData
import link.danb.launcher.profiles.PersonalAndWorkProfiles
import link.danb.launcher.profiles.ProfileManager
import link.danb.launcher.shortcuts.ShortcutManager
import link.danb.launcher.tiles.TileViewItem
import link.danb.launcher.tiles.TileViewItemFactory

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
  private val tileViewItemFactory: TileViewItemFactory,
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
            launcherResourceProvider.getIcon(userActivity).await(),
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
      .map { tileViewItemFactory.getTileViewItem(it, TileViewItem.Style.CARD) }
      .map { ShortcutViewData(it.data as UserShortcut, it.icon, it.name.toString()) }
      .sortedBy { it.name }

  private suspend fun getShortcutCreators(userActivity: UserActivity) =
    shortcutManager
      .getShortcutCreators(userActivity)
      .map { tileViewItemFactory.getTileViewItem(it, TileViewItem.Style.CARD) }
      .map { ShortcutCreatorViewData(it.data as UserShortcutCreator, it.icon, it.name.toString()) }
      .sortedBy { it.name }

  private fun getWidgets(userActivity: UserActivity) =
    appWidgetManager
      .getInstalledProvidersForPackage(
        userActivity.componentName.packageName,
        userActivity.userHandle,
      )
      .map {
        WidgetPreviewViewData(
          it,
          it.loadPreviewImage(application, 0) ?: it.loadIcon(application, 0),
          if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            it.previewLayout
          } else {
            null
          },
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
      val widgets: List<WidgetPreviewViewData>,
    ) : ShortcutsAndWidgets
  }

  data class ShortcutViewData(val userShortcut: UserShortcut, val icon: Drawable, val name: String)

  data class ShortcutCreatorViewData(
    val userShortcutCreator: UserShortcutCreator,
    val icon: Drawable,
    val name: String,
  )

  data class WidgetPreviewViewData(
    val providerInfo: AppWidgetProviderInfo,
    val previewImage: Drawable,
    @IdRes val previewLayout: Int?,
    val label: String,
    val description: String?,
  )
}
