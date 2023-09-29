package link.danb.launcher.apps

import android.content.pm.LauncherApps
import android.os.UserHandle

class LauncherAppsCallback(
  private val onPackagesChanged: (packageName: List<String>, user: UserHandle) -> Unit,
) : LauncherApps.Callback() {

  override fun onPackageRemoved(packageName: String, user: UserHandle) {
    onPackagesChanged(listOf(packageName), user)
  }

  override fun onPackageAdded(packageName: String, user: UserHandle) {
    onPackagesChanged(listOf(packageName), user)
  }

  override fun onPackageChanged(packageName: String, user: UserHandle) {
    onPackagesChanged(listOf(packageName), user)
  }

  override fun onPackagesAvailable(
    packageNames: Array<out String>,
    user: UserHandle,
    replacing: Boolean,
  ) {
    onPackagesChanged(packageNames.toList(), user)
  }

  override fun onPackagesUnavailable(
    packageNames: Array<out String>,
    user: UserHandle,
    replacing: Boolean,
  ) {
    onPackagesChanged(packageNames.toList(), user)
  }

  override fun onPackagesSuspended(packageNames: Array<out String>, user: UserHandle) {
    onPackagesChanged(packageNames.toList(), user)
  }

  override fun onPackagesUnsuspended(packageNames: Array<out String>, user: UserHandle) {
    onPackagesChanged(packageNames.toList(), user)
  }
}
