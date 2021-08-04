package link.danb.launcher

import android.app.Application
import android.content.Context
import android.content.pm.LauncherApps
import android.os.UserHandle
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData

class AppViewModel(application: Application) : AndroidViewModel(application) {

    private val launcherApps =
        getApplication<Application>()
            .applicationContext
            .getSystemService(Context.LAUNCHER_APPS_SERVICE) as LauncherApps

    val apps: LiveData<List<AppItem>> by lazy {
        val mutableList = ArrayList<AppItem>()
        val liveData = MutableLiveData<List<AppItem>>(mutableList)

        val addApp = { name: String?, user: UserHandle? ->
            if (name != null && user != null) {
                mutableList.addAll(
                    launcherApps.getActivityList(name, user).map { AppItem(user, it) }
                )
            }
        }

        val removeApp = { name: String?, user: UserHandle? ->
            if (name != null && user != null) {
                mutableList.removeAll {
                    it.info.applicationInfo.packageName == name && it.user == user
                }
            }
        }

        val notify = { liveData.value = mutableList }

        mutableList.addAll(
            launcherApps
                .profiles
                .flatMap { user ->
                    launcherApps
                        .getActivityList(null, user)
                        .map { info -> AppItem(user, info) }
                }
        )

        launcherApps.registerCallback(object : LauncherApps.Callback() {
            override fun onPackageRemoved(packageName: String?, user: UserHandle?) {
                removeApp(packageName, user)
                notify()
            }

            override fun onPackageAdded(packageName: String?, user: UserHandle?) {
                addApp(packageName, user)
                notify()
            }

            override fun onPackageChanged(packageName: String?, user: UserHandle?) {
                removeApp(packageName, user)
                addApp(packageName, user)
                notify()
            }

            override fun onPackagesAvailable(
                packageNames: Array<out String>?,
                user: UserHandle?,
                replacing: Boolean
            ) {
                packageNames?.forEach { addApp(it, user) }
                notify()
            }

            override fun onPackagesUnavailable(
                packageNames: Array<out String>?,
                user: UserHandle?,
                replacing: Boolean
            ) {
                packageNames?.forEach { removeApp(it, user) }
                notify()
            }
        })

        liveData
    }
}