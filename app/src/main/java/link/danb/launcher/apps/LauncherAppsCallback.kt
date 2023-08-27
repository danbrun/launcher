package link.danb.launcher.apps

import android.content.pm.LauncherApps
import android.os.UserHandle

class LauncherAppsCallback(private val onChange: (packageName: String, user: UserHandle) -> Unit) :
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
            onChange(packageName, user)
        }
    }

    private fun updateMultiplePackages(packageName: Array<out String>?, user: UserHandle?) {
        if (user != null) {
            packageName?.forEach {
                onChange(it, user)
            }
        }
    }
}
