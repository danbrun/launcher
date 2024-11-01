package link.danb.launcher.widgets.dialog

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
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.plus
import kotlinx.coroutines.withContext
import link.danb.launcher.apps.LauncherResourceProvider
import link.danb.launcher.components.UserApplication
import link.danb.launcher.profiles.Profile
import link.danb.launcher.profiles.ProfileManager
import link.danb.launcher.ui.LauncherTileData
import link.danb.launcher.ui.WidgetPreviewData

sealed interface PinWidgetsViewData {

  data object Loading : PinWidgetsViewData

  data class Loaded(val viewItems: ImmutableList<PinWidgetViewItem>) : PinWidgetsViewData

  sealed interface PinWidgetViewItem {

    data class PinWidgetHeader(
      val userApplication: UserApplication,
      val launcherTileData: LauncherTileData,
    ) : PinWidgetViewItem

    data class PinWidgetEntry(
      val userApplication: UserApplication,
      val widgetPreviewData: WidgetPreviewData,
    ) : PinWidgetViewItem
  }
}

@HiltViewModel
class PinWidgetsViewModel
@Inject
constructor(
  private val application: Application,
  private val appWidgetManager: AppWidgetManager,
  private val launcherResourceProvider: LauncherResourceProvider,
  private val profileManager: ProfileManager,
) : AndroidViewModel(application) {

  fun getPinWidgetsViewData(profile: Profile): StateFlow<PinWidgetsViewData> =
    flow {
        emit(PinWidgetsViewData.Loading)

        emit(PinWidgetsViewData.Loaded(getWidgetViewItems(profile)))
      }
      .stateIn(
        viewModelScope + Dispatchers.IO,
        SharingStarted.WhileSubscribed(),
        PinWidgetsViewData.Loading,
      )

  private suspend fun getWidgetViewItems(
    profile: Profile
  ): ImmutableList<PinWidgetsViewData.PinWidgetViewItem> =
    appWidgetManager
      .getInstalledProvidersForProfile(profileManager.getUserHandle(profile))
      .groupBy { UserApplication(it.provider.packageName, profile) }
      .toSortedMap(compareBy { launcherResourceProvider.getLabel(it) })
      .flatMap { entry ->
        buildList {
          add(
            PinWidgetsViewData.PinWidgetViewItem.PinWidgetHeader(
              entry.key,
              launcherResourceProvider.getTileData(entry.key),
            )
          )

          for (widget in entry.value) {
            add(
              PinWidgetsViewData.PinWidgetViewItem.PinWidgetEntry(
                entry.key,
                WidgetPreviewData(
                  widget,
                  withContext(Dispatchers.IO) { widget.loadPreviewImage(application, 0) }
                    ?: launcherResourceProvider.getIcon(
                      UserApplication(
                        widget.provider.packageName,
                        profileManager.getProfile(widget.profile),
                      )
                    ),
                  widget.loadLabel(application.packageManager),
                  if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    widget.loadDescription(application)?.toString()
                  } else {
                    null
                  },
                ),
              )
            )
          }
        }
      }
      .toImmutableList()
}
