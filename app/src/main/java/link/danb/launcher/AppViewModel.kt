package link.danb.launcher

import android.app.Application
import android.content.Context
import android.content.pm.LauncherApps
import android.graphics.Rect
import android.os.UserHandle
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch

class AppViewModel(application: Application) : AndroidViewModel(application) {

    private val launcherApps =
        getApplication<Application>()
            .applicationContext
            .getSystemService(Context.LAUNCHER_APPS_SERVICE) as LauncherApps

    private fun getAppItems() = launcherApps.profiles.flatMap { getAppItems(it) }

    private fun getAppItems(user: UserHandle) = getAppItems(null, user)

    private fun getAppItems(packageName: String?, user: UserHandle) =
        launcherApps.getActivityList(packageName, user).map { AppItem(it) }

    private val mutableApps: MutableLiveData<List<AppItem>> = MutableLiveData(ArrayList())
    val apps: LiveData<List<AppItem>> = mutableApps

    init {
        viewModelScope.launch {
            mutableApps.value = getAppItems()
            launcherApps.registerCallback(LauncherAppsCallback())
        }
    }

    fun openApp(appItem: AppItem, bounds: Rect) =
        launcherApps.startMainActivity(
            appItem.info.componentName,
            appItem.info.user,
            bounds,
            null
        )

    fun openAppInfo(appItem: AppItem, bounds: Rect) =
        launcherApps.startAppDetailsActivity(
            appItem.info.componentName,
            appItem.info.user,
            bounds,
            null
        )

    private fun isSamePackage(appItem: AppItem, packageName: String?, user: UserHandle?) =
        appItem.info.applicationInfo.packageName == packageName && appItem.info.user == user

    inner class LauncherAppsCallback : LauncherApps.Callback() {
        override fun onPackageRemoved(packageName: String?, user: UserHandle?) {
            mutableApps.value = mutableApps.value!!.filter { !isSamePackage(it, packageName, user) }
        }

        override fun onPackageAdded(packageName: String?, user: UserHandle?) {
            if (packageName != null && user != null) {
                mutableApps.value =
                    listOf(mutableApps.value!!, getAppItems(packageName, user)).flatten()
            }
        }

        override fun onPackageChanged(packageName: String?, user: UserHandle?) {
            if (packageName != null && user != null) {
                mutableApps.value =
                    listOf(
                        mutableApps.value!!.filter { !isSamePackage(it, packageName, user) },
                        getAppItems(packageName, user)
                    ).flatten()
            }
        }

        override fun onPackagesAvailable(
            packageName: Array<out String>?, user: UserHandle?, replacing: Boolean
        ) {
            viewModelScope.launch {
                mutableApps.value = getAppItems()
            }
        }

        override fun onPackagesUnavailable(
            packageName: Array<out String>?, user: UserHandle?, replacing: Boolean
        ) {
            viewModelScope.launch {
                mutableApps.value = getAppItems()
            }
        }
    }
}