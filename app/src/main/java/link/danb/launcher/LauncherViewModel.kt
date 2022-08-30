package link.danb.launcher

import android.app.Application
import android.content.ComponentName
import android.content.Context
import android.content.pm.LauncherActivityInfo
import android.content.pm.LauncherApps
import android.graphics.drawable.Drawable
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

    private val mutableLauncherActivities = MutableStateFlow<List<LauncherActivity>>(listOf())
    val launcherActivities: StateFlow<List<LauncherActivity>> = mutableLauncherActivities

    val filter: MutableStateFlow<LauncherFilter> = MutableStateFlow(LauncherFilter.PERSONAL)

    val filteredLauncherActivities =
        launcherActivities.combine(filter) { launcherActivities, filter ->
            launcherActivities.filter { filter.function(it) }
        }

    init {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                mutableLauncherActivities.emit(
                    launcherApps.profiles.flatMap { launcherApps.getActivityList(null, it) }
                        .map { LauncherActivity(getApplication(), it) }
                )
            }
        }

        launcherApps.registerCallback(launcherAppsCallback)
    }

    override fun onCleared() {
        super.onCleared()

        launcherApps.unregisterCallback(launcherAppsCallback)
    }

    fun openActivity(launcherActivity: LauncherActivity, view: View) {
        launcherApps.startMainActivity(
            launcherActivity.component,
            launcherActivity.user,
            view.getLocationOnScreen(),
            view.makeClipRevealAnimation()
        )
    }

    fun openDetailsActivity(launcherActivity: LauncherActivity, view: View) {
        launcherApps.startAppDetailsActivity(
            launcherActivity.component,
            launcherActivity.user,
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
                            .map { LauncherActivity(getApplication(), it) })
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

class LauncherActivity(application: Application, launcherActivityInfo: LauncherActivityInfo) {
    val component: ComponentName = launcherActivityInfo.componentName
    val user: UserHandle = launcherActivityInfo.user
    val timestamp = System.currentTimeMillis()

    val name: CharSequence by lazy { launcherActivityInfo.label }
    val icon: Drawable by lazy {
        application.packageManager.getUserBadgedIcon(
            LauncherIconDrawable(launcherActivityInfo.getIcon(0)),
            user
        )
    }
}
