package link.danb.launcher

import android.app.ActivityOptions
import android.app.Application
import android.content.Context
import android.content.pm.LauncherApps
import android.graphics.Rect
import android.os.UserHandle
import android.view.View
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

    private val mutableFilter: MutableLiveData<AppFilter> = MutableLiveData(AppFilter.PERSONAL)
    val filter: LiveData<AppFilter> = mutableFilter

    init {
        viewModelScope.launch {
            mutableApps.value = getAppItems()
            launcherApps.registerCallback(LauncherAppsCallback())
        }
    }

    fun setFilter(filter: AppFilter) {
        mutableFilter.value = filter
    }

    private fun getBounds(view: View): Rect {
        val pos = IntArray(2).apply { view.getLocationOnScreen(this) }
        return Rect(pos[0], pos[1], view.width, view.height)
    }

    private fun getAnimation(view: View): ActivityOptions {
        return ActivityOptions.makeClipRevealAnimation(view, 0, 0, view.width, view.height)
    }

    fun openApp(appItem: AppItem, view: View) =
        launcherApps.startMainActivity(
            appItem.info.componentName,
            appItem.info.user,
            getBounds(view),
            getAnimation(view).toBundle()
        )

    fun openAppInfo(appItem: AppItem, view: View) =
        launcherApps.startAppDetailsActivity(
            appItem.info.componentName,
            appItem.info.user,
            getBounds(view),
            getAnimation(view).toBundle()
        )

    private fun isSamePackage(appItem: AppItem, packageName: String?, user: UserHandle?) =
        appItem.info.applicationInfo.packageName == packageName && appItem.info.user == user

    inner class LauncherAppsCallback : LauncherApps.Callback() {
        override fun onPackageRemoved(packageName: String?, user: UserHandle?) {
            viewModelScope.launch {
                mutableApps.value =
                    mutableApps.value!!.filter { !isSamePackage(it, packageName, user) }
            }
        }

        override fun onPackageAdded(packageName: String?, user: UserHandle?) {
            if (packageName != null && user != null) {
                viewModelScope.launch {
                    mutableApps.value =
                        listOf(mutableApps.value!!, getAppItems(packageName, user)).flatten()
                }
            }
        }

        override fun onPackageChanged(packageName: String?, user: UserHandle?) {
            if (packageName != null && user != null) {
                viewModelScope.launch {
                    mutableApps.value =
                        listOf(
                            mutableApps.value!!.filter { !isSamePackage(it, packageName, user) },
                            getAppItems(packageName, user)
                        ).flatten()
                }
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
