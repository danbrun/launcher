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
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.plus
import kotlinx.coroutines.withContext
import link.danb.launcher.apps.LauncherResourceProvider
import link.danb.launcher.components.UserApplication
import link.danb.launcher.profiles.Profile
import link.danb.launcher.profiles.ProfileManager
import link.danb.launcher.ui.LauncherTileData
import link.danb.launcher.ui.WidgetPreviewData

@HiltViewModel
class PinWidgetsViewModel
@Inject
constructor(
  private val application: Application,
  private val appWidgetManager: AppWidgetManager,
  private val launcherResourceProvider: LauncherResourceProvider,
  private val profileManager: ProfileManager,
) : AndroidViewModel(application) {

  private val _expandedApplications: MutableStateFlow<List<UserApplication>> =
    MutableStateFlow(listOf())

  fun getState(profile: Profile): StateFlow<State> =
    _expandedApplications
      .map { State.Loaded(getWidgetViewItems(profile, it)) }
      .stateIn(viewModelScope + Dispatchers.IO, SharingStarted.WhileSubscribed(), State.Loading)

  fun toggle(userApplication: UserApplication) {
    if (userApplication in _expandedApplications.value) {
      _expandedApplications.value -= userApplication
    } else {
      _expandedApplications.value += userApplication
    }
  }

  private suspend fun getWidgetViewItems(
    profile: Profile,
    expandedApplications: List<UserApplication>,
  ): ImmutableList<State.Item> =
    appWidgetManager
      .getInstalledProvidersForProfile(profileManager.getUserHandle(profile))
      .groupBy { UserApplication(it.provider.packageName, profile) }
      .toSortedMap(compareBy { launcherResourceProvider.getLabel(it) })
      .flatMap { entry ->
        buildList {
          val isExpanded = entry.key in expandedApplications
          add(
            State.Item.Header(
              entry.key,
              launcherResourceProvider.getTileData(entry.key),
              isExpanded,
            )
          )

          if (isExpanded) {
            for (widget in entry.value) {
              add(
                State.Item.Entry(
                  entry.key,
                  WidgetPreviewData(
                    widget,
                    withContext(Dispatchers.IO) { widget.loadPreviewImage(application, 0) }
                      ?: launcherResourceProvider.getIcon(
                        UserApplication(
                          widget.provider.packageName,
                          checkNotNull(profileManager.getProfile(widget.profile)),
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
      }
      .toImmutableList()

  sealed interface State {

    data object Loading : State

    data class Loaded(val items: ImmutableList<Item>) : State

    sealed interface Item {

      data class Header(
        val userApplication: UserApplication,
        val launcherTileData: LauncherTileData,
        val isExpanded: Boolean,
      ) : Item

      data class Entry(
        val userApplication: UserApplication,
        val widgetPreviewData: WidgetPreviewData,
      ) : Item
    }
  }
}
