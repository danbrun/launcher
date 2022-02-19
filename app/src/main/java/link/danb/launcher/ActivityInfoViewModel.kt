package link.danb.launcher

import android.app.Application
import android.content.ComponentName
import android.content.Context
import android.content.pm.LauncherActivityInfo
import android.content.pm.LauncherApps
import android.os.UserHandle
import android.view.View
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ActivityInfoViewModel(application: Application) : AndroidViewModel(application) {

    private val launcherApps: LauncherApps by lazy {
        getApplication<Application>()
            .applicationContext
            .getSystemService(Context.LAUNCHER_APPS_SERVICE) as LauncherApps
    }

    private val internalActivities: ArrayList<LauncherActivityInfo> = ArrayList()
    private val mutableActivities: MutableLiveData<List<LauncherActivityInfo>> =
        MutableLiveData(internalActivities)
    val activities: LiveData<List<LauncherActivityInfo>> = mutableActivities

    init {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                updateActivityList()
                postActivityList()
            }
            launcherApps.registerCallback(LauncherAppsCallback())
        }
    }

    fun openApp(componentName: ComponentName, userHandle: UserHandle, view: View) =
        launcherApps.startMainActivity(
            componentName,
            userHandle,
            view.getLocationOnScreen(),
            view.makeClipRevealAnimation()
        )

    fun openAppInfo(componentName: ComponentName, userHandle: UserHandle, view: View) =
        launcherApps.startAppDetailsActivity(
            componentName,
            userHandle,
            view.getLocationOnScreen(),
            view.makeClipRevealAnimation()
        )

    private fun updateActivityList(user: UserHandle? = null, packageName: String? = null) {
        if (user != null) {
            if (packageName != null) {
                internalActivities.removeIf {
                    it.user == user && it.applicationInfo.packageName == packageName
                }
            } else {
                internalActivities.removeIf {
                    it.user == user
                }
            }
            internalActivities.addAll(launcherApps.getActivityList(packageName, user))
        } else {
            internalActivities.clear()
            internalActivities.addAll(
                launcherApps.profiles.flatMap {
                    launcherApps.getActivityList(null, it)
                }
            )
        }
    }

    private fun postActivityList() {
        mutableActivities.postValue(internalActivities)
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
}
