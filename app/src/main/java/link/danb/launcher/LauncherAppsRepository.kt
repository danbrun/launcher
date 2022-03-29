package link.danb.launcher

import android.app.Application
import android.content.Context
import android.content.pm.LauncherActivityInfo
import android.content.pm.LauncherApps
import android.os.UserHandle
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** Repository for activity info from the launcher API. */
class LauncherAppsRepository(application: Application, private val externalScope: CoroutineScope) {

    private val launcherApps: LauncherApps by lazy {
        application.getSystemService(Context.LAUNCHER_APPS_SERVICE) as LauncherApps
    }

    private val _infoList: MutableStateFlow<List<LauncherActivityInfo>> = MutableStateFlow(listOf())

    /** List of current available launcher activities. */
    val infoList: StateFlow<List<LauncherActivityInfo>> = _infoList

    init {
        externalScope.launch {
            withContext(Dispatchers.IO) {
                _infoList.emit(launcherApps.getActivityList())
            }
        }

        launcherApps.registerCallback(LauncherAppsCallback(this))
    }

    private fun update(packageName: String, user: UserHandle) {
        externalScope.launch {
            withContext(Dispatchers.IO) {
                _infoList.value.toMutableList().apply {
                    removeIf { it.componentName.packageName == packageName && it.user == user }
                    addAll(launcherApps.getActivityList(packageName, user))
                    _infoList.emit(toList())
                }
            }
        }
    }

    private class LauncherAppsCallback(private val repository: LauncherAppsRepository) :
        LauncherApps.Callback() {

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
                repository.update(packageName, user)
            }
        }

        private fun updateMultiplePackages(packageName: Array<out String>?, user: UserHandle?) {
            if (user != null) {
                packageName?.forEach {
                    repository.update(it, user)
                }
            }
        }
    }

    companion object {
        /** Get all activity info if no parameters are passed to getActivityList. */
        private fun LauncherApps.getActivityList(): List<LauncherActivityInfo> {
            return profiles.flatMap { getActivityList(null, it) }
        }
    }
}
