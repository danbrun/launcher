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
                }.map {
                    ActivityData(it, launcherActivityMetadata.get(it))
                })
            }
        }

        launcherApps.registerCallback(launcherAppsCallback)
    }

    override fun onCleared() {
        super.onCleared()

        launcherApps.unregisterCallback(launcherAppsCallback)
    }

    /** Sets the list of tags to associate with the given [ActivityData] */
    fun updateTags(activityData: ActivityData, tags: Set<String>) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                launcherActivityMetadata.put(
                    ActivityMetadata(
                        activityData.info.componentName, activityData.info.user, tags
                    )
                )
                update(
                    activityData.info.componentName.packageName, activityData.info.user
                )
            }
        }
    }

    /** Sets the visibility of the given app. */
    fun setVisibility(info: LauncherActivityInfo, isVisible: Boolean) {
        val activityData = launcherActivities.value.first { it.info == info }
        if (isVisible) {
            updateTags(activityData, activityData.metadata.tags.minus(HIDDEN_TAG))
        } else {
            updateTags(activityData, activityData.metadata.tags.plus(HIDDEN_TAG))
        }
    }

    /** Returns true if the given app is visible. */
    fun isVisible(info: LauncherActivityInfo): Boolean {
        return !launcherActivities.value.first { it.info == info }.metadata.tags.contains(HIDDEN_TAG)
    }

    private fun update(packageName: String, user: UserHandle) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                _launcherActivities.value.toMutableList().apply {
                    removeIf { it.info.componentName.packageName == packageName && it.info.user == user }
                    addAll(launcherApps.getActivityList(packageName, user).map {
                        ActivityData(it, launcherActivityMetadata.get(it))
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

    companion object {
        private const val HIDDEN_TAG = "hidden"
    }
}
