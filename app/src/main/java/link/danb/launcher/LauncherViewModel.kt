package link.danb.launcher

import android.app.Application
import android.content.ComponentName
import android.content.Context
import android.content.pm.LauncherActivityInfo
import android.content.pm.LauncherApps
import android.os.UserHandle
import android.view.View
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import link.danb.launcher.list.AppItem
import link.danb.launcher.utils.getLocationOnScreen
import link.danb.launcher.utils.makeClipRevealAnimation

/** View model for launch icons. */
class LauncherViewModel(application: Application) : AndroidViewModel(application) {

    private val launcherApps: LauncherApps by lazy {
        application.getSystemService(Context.LAUNCHER_APPS_SERVICE) as LauncherApps
    }

    private val launcherAppsRepository = LauncherAppsRepository(application, viewModelScope)
    private val launcherIconRepository = LauncherIconRepository(application)

    private val _filter: MutableStateFlow<LauncherFilter> = MutableStateFlow(LauncherFilter.NONE)

    /** List of icons to show in the launcher. */
    val iconList: StateFlow<List<AppItem>> =
        launcherAppsRepository.infoList
            .combine(_filter) { infoList, filter ->
                infoList
                    .filter(filter.function)
                    .map { getLauncherIcon(it) }
                    .sortedBy { it.name.toString().lowercase() }
            }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(), mutableListOf())

    /** The current filter applied to the icon list. */
    val filter: StateFlow<LauncherFilter> = _filter

    private suspend fun getLauncherIcon(info: LauncherActivityInfo): AppItem =
        AppItem(info, info.label, launcherIconRepository.get(info))

    /** Update the current filter applied to the launcher icons. */
    fun setFilter(filter: LauncherFilter) {
        viewModelScope.launch {
            _filter.emit(filter)
        }
    }

    /** Open the main activity for the given component. */
    fun openApp(componentName: ComponentName, user: UserHandle, view: View) =
        launcherApps.startMainActivity(
            componentName,
            user,
            view.getLocationOnScreen(),
            view.makeClipRevealAnimation()
        )

    /** Open application info for the given component. */
    fun openAppInfo(componentName: ComponentName, user: UserHandle, view: View) =
        launcherApps.startAppDetailsActivity(
            componentName,
            user,
            view.getLocationOnScreen(),
            view.makeClipRevealAnimation()
        )

    companion object {
        /** List of available filters for the launcher icons. */
        val FILTERS: List<LauncherFilter> =
            listOf(LauncherFilter.ALL, LauncherFilter.PERSONAL, LauncherFilter.WORK)
    }
}
