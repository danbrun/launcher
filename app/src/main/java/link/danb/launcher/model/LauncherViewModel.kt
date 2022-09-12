package link.danb.launcher.model

import android.app.Application
import android.content.Context
import android.content.pm.LauncherApps
import android.os.UserHandle
import android.view.View
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import link.danb.launcher.utils.getLocationOnScreen
import link.danb.launcher.utils.makeClipRevealAnimation

/** View model for launch icons. */
class LauncherViewModel(application: Application) : AndroidViewModel(application) {

    private val launcherApps =
        application.getSystemService(Context.LAUNCHER_APPS_SERVICE) as LauncherApps
    private val launcherAppsCallback = LauncherAppsCallback()

    private val mutableLauncherActivities = MutableStateFlow<List<LauncherActivityData>>(listOf())
    val launcherActivities: StateFlow<List<LauncherActivityData>> = mutableLauncherActivities

    val filter: MutableStateFlow<LauncherActivityFilter> = MutableStateFlow(LauncherActivityFilter.PERSONAL)

    val filteredLauncherActivities =
        launcherActivities.combine(filter) { launcherActivities, filter ->
            launcherActivities.filter { filter.function(it) }
        }

    init {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                mutableLauncherActivities.emit(
                    launcherApps.profiles.flatMap { launcherApps.getActivityList(null, it) }
                        .map { LauncherActivityData(getApplication(), it) }
                )
            }
        }

        launcherApps.registerCallback(launcherAppsCallback)
    }

    override fun onCleared() {
        super.onCleared()

        launcherApps.unregisterCallback(launcherAppsCallback)
    }

    fun openActivity(launcherActivityData: LauncherActivityData, view: View) {
        launcherApps.startMainActivity(
            launcherActivityData.component,
            launcherActivityData.user,
            view.getLocationOnScreen(),
            view.makeClipRevealAnimation()
        )
    }

    fun openDetailsActivity(launcherActivityData: LauncherActivityData, view: View) {
        launcherApps.startAppDetailsActivity(
            launcherActivityData.component,
            launcherActivityData.user,
            view.getLocationOnScreen(),
            view.makeClipRevealAnimation()
        )
    }

    private fun update(packageName: String, user: UserHandle) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                mutableLauncherActivities.value.toMutableList().apply {
                    removeIf { it.component.packageName == packageName && it.user == user }
                    addAll(
                        launcherApps.getActivityList(packageName, user)
                            .map { LauncherActivityData(getApplication(), it) })
                    mutableLauncherActivities.emit(toList())
                }
            }
        }
    }

    inner class LauncherAppsCallback : LauncherApps.Callback() {
        override fun onPackageRemoved(packageName: String?, user: UserHandle?) {
            updateSinglePackage(packageName, user)
        }

        override fun onPackageAdded(packageName: String?, user: UserHandle?) {
            updateSinglePackage(packageName, user)
        }

        override fun onPackageChanged(packageName: String?, user: UserHandle?) {
            updateSinglePackage(packageName, user)
        }

        override fun onPackagesAvailable(
            packageName: Array<out String>?, user: UserHandle?, replacing: Boolean
        ) {
            updateMultiplePackages(packageName, user)
        }

        override fun onPackagesUnavailable(
            packageName: Array<out String>?, user: UserHandle?, replacing: Boolean
        ) {
            updateMultiplePackages(packageName, user)
        }

        private fun updateSinglePackage(packageName: String?, user: UserHandle?) {
            if (packageName != null && user != null) {
                update(packageName, user)
            }
        }

        private fun updateMultiplePackages(packageName: Array<out String>?, user: UserHandle?) {
            if (user != null) {
                packageName?.forEach {
                    update(it, user)
                }
            }
        }
    }
}
