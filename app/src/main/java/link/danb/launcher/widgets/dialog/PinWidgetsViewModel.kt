package link.danb.launcher.widgets.dialog

import android.app.Application
import android.appwidget.AppWidgetManager
import android.os.Build
import android.os.UserHandle
import androidx.lifecycle.AndroidViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.transform
import kotlinx.coroutines.withContext
import link.danb.launcher.apps.LauncherResourceProvider
import link.danb.launcher.components.UserApplication
import link.danb.launcher.ui.IconTileViewData
import link.danb.launcher.ui.WidgetPreviewData

sealed interface PinWidgetsViewData {

  data object Loading : PinWidgetsViewData

  data class Loaded(val viewItems: List<PinWidgetViewItem>) : PinWidgetsViewData

  sealed interface PinWidgetViewItem {

    data class PinWidgetHeader(
      val userApplication: UserApplication,
      val iconTileViewData: IconTileViewData,
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
  application: Application,
  private val appWidgetManager: AppWidgetManager,
  private val launcherResourceProvider: LauncherResourceProvider,
) : AndroidViewModel(application) {

  private val showWidgetsForUser: MutableStateFlow<UserHandle?> = MutableStateFlow(null)

  val pinWidgetsViewData: Flow<PinWidgetsViewData?> =
    showWidgetsForUser.transform { user ->
      if (user != null) {
        emit(PinWidgetsViewData.Loading)

        emit(
          PinWidgetsViewData.Loaded(
            appWidgetManager
              .getInstalledProvidersForProfile(user)
              .groupBy { UserApplication(it.provider.packageName, user) }
              .toSortedMap(compareBy { launcherResourceProvider.getLabel(it) })
              .flatMap { entry ->
                buildList {
                  add(
                    PinWidgetsViewData.PinWidgetViewItem.PinWidgetHeader(
                      entry.key,
                      IconTileViewData(
                        launcherResourceProvider.getSourceIcon(entry.key),
                        launcherResourceProvider.getBadge(entry.key.userHandle),
                        launcherResourceProvider.getLabel(entry.key),
                      ),
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
                              UserApplication(widget.provider.packageName, widget.profile)
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
          )
        )
      } else {
        emit(null)
      }
    }

  fun showPinWidgetsDialog(user: UserHandle) {
    showWidgetsForUser.value = user
  }

  fun hidePinWidgetsDialog() {
    showWidgetsForUser.value = null
  }
}
