package link.danb.launcher

import android.app.ActivityOptions
import android.app.Application
import android.content.Context
import android.content.pm.LauncherActivityInfo
import android.content.pm.LauncherApps
import android.graphics.Rect
import android.os.UserHandle
import android.view.View
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class AppViewModel(application: Application) : AndroidViewModel(application) {

    private val launcherApps =
        getApplication<Application>()
            .applicationContext
            .getSystemService(Context.LAUNCHER_APPS_SERVICE) as LauncherApps
    private val activityList: ArrayList<LauncherActivityInfo> = ArrayList()
    private val mutableApps: MutableLiveData<List<AppItem>> = MutableLiveData(ArrayList())
    private val mutableFilter: MutableLiveData<AppFilter> = MutableLiveData(AppFilter.PERSONAL)

    val apps: LiveData<List<AppItem>> = mutableApps
    val filter: LiveData<AppFilter> = mutableFilter

    init {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                updateActivityList()
                postActivityList()
            }
            launcherApps.registerCallback(LauncherAppsCallback())
        }
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

    fun setFilter(filter: AppFilter) {
        mutableFilter.value = filter
    }

    private fun updateActivityList(user: UserHandle? = null, packageName: String? = null) {
        if (user != null) {
            if (packageName != null) {
                activityList.removeIf {
                    it.user == user && it.applicationInfo.packageName == packageName
                }
            } else {
                activityList.removeIf {
                    it.user == user
                }
            }
            activityList.addAll(launcherApps.getActivityList(packageName, user))
        } else {
            activityList.clear()
            activityList.addAll(
                launcherApps.profiles.flatMap {
                    launcherApps.getActivityList(null, it)
                }
            )
        }
    }

    private fun postActivityList() {
        mutableApps.postValue(activityList.map { AppItem(it) })
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
            viewModelScope.launch {
                withContext(Dispatchers.IO) {
                    updateActivityList(user, packageName)
                    postActivityList()
                }
            }
        }

        private fun updateMultiplePackages(packageName: Array<out String>?, user: UserHandle?) {
            viewModelScope.launch {
                withContext(Dispatchers.IO) {
                    packageName?.forEach {
                        updateActivityList(user, it)
                    }
                    postActivityList()
                }
            }
        }
    }

    companion object {
        private fun getBounds(view: View): Rect {
            val pos = IntArray(2).apply { view.getLocationOnScreen(this) }
            return Rect(pos[0], pos[1], view.width, view.height)
        }

        private fun getAnimation(view: View): ActivityOptions {
            return ActivityOptions.makeClipRevealAnimation(view, 0, 0, view.width, view.height)
        }
    }
}
