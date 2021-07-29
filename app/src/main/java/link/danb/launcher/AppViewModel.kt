package link.danb.launcher

import android.app.Application
import android.content.Context
import android.content.pm.LauncherApps
import android.os.UserHandle
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData

class AppViewModel(application: Application) : AndroidViewModel(application) {

    private val launcherApps =
        getApplication<Application>()
            .applicationContext
            .getSystemService(Context.LAUNCHER_APPS_SERVICE) as LauncherApps

    val apps = MutableLiveData<List<AppItem>>()

    init {
        updateApps()

        launcherApps.registerCallback(object : LauncherApps.Callback() {
            override fun onPackageRemoved(packageName: String?, user: UserHandle?) {
                updateApps()
            }

            override fun onPackageAdded(packageName: String?, user: UserHandle?) {
                updateApps()
            }

            override fun onPackageChanged(packageName: String?, user: UserHandle?) {
                updateApps()
            }

            override fun onPackagesAvailable(
                packageNames: Array<out String>?,
                user: UserHandle?,
                replacing: Boolean
            ) {
                updateApps()
            }

            override fun onPackagesUnavailable(
                packageNames: Array<out String>?,
                user: UserHandle?,
                replacing: Boolean
            ) {
                updateApps()
            }
        })
    }

    private fun updateApps() {
        apps.value =
            launcherApps
                .profiles
                .flatMap { user ->
                    launcherApps
                        .getActivityList(null, user)
                        .map { info -> AppItem(user, info) }
                }
    }
}