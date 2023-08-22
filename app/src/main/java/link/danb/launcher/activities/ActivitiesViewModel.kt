package link.danb.launcher.activities

import android.app.Application
import android.content.pm.LauncherActivityInfo
import android.content.pm.LauncherApps
import android.os.UserHandle
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import link.danb.launcher.database.ActivityMetadata
import link.danb.launcher.database.LauncherDatabase
import javax.inject.Inject

/** View model for launch icons. */
@HiltViewModel
class ActivitiesViewModel @Inject constructor(
    application: Application,
    private val launcherApps: LauncherApps,
    private val launcherDatabase: LauncherDatabase
) : AndroidViewModel(application) {

    private val launcherActivityMetadata by lazy {
        launcherDatabase.launcherActivityMetadata()
    }

    private val launcherAppsCallback = LauncherAppsCallback()

    private val _launcherActivities = MutableStateFlow<List<ActivityData>>(listOf())

    val launcherActivities: StateFlow<List<ActivityData>> = _launcherActivities.asStateFlow()

    init {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                _launcherActivities.emit(launcherApps.profiles.flatMap {
                    launcherApps.getActivityList(
                        null, it
                    )
                }.map { ActivityData(it, getMetadata(it)) })
            }
        }

        launcherApps.registerCallback(launcherAppsCallback)
    }

    override fun onCleared() {
        super.onCleared()

        launcherApps.unregisterCallback(launcherAppsCallback)
    }

    /** Sets the list of tags to associate with the given [ActivityData] */
    // May use this again soon so leaving it for now.
    @Suppress("unused")
    fun updateTags(info: LauncherActivityInfo, tags: Set<String>) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                launcherActivityMetadata.put(getMetadata(info).copy(tags = tags))
                update(info)
            }
        }
    }

    /** Sets the visibility of the given app. */
    fun setIsHidden(info: LauncherActivityInfo, isHidden: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            launcherActivityMetadata.put(getMetadata(info).copy(isHidden = isHidden))
            update(info)
        }
    }

    private fun getMetadata(info: LauncherActivityInfo): ActivityMetadata =
        launcherActivityMetadata.get(info.componentName, info.user) ?: ActivityMetadata(
            info.componentName, info.user, isHidden = false, tags = setOf()
        )

    private fun update(info: LauncherActivityInfo) {
        update(info.componentName.packageName, info.user)
    }

    private fun update(packageName: String, user: UserHandle) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                _launcherActivities.value.toMutableList().apply {
                    removeIf { it.info.componentName.packageName == packageName && it.info.user == user }
                    addAll(launcherApps.getActivityList(packageName, user).map {
                        ActivityData(it, getMetadata(it))
                    })
                    _launcherActivities.emit(toList())
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

    data class ActivityData(
        val info: LauncherActivityInfo, val metadata: ActivityMetadata
    )
}
