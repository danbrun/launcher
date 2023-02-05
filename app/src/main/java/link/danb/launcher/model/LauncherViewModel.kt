package link.danb.launcher.model

import android.app.Application
import android.content.pm.LauncherApps
import android.os.Process.myUserHandle
import android.os.UserHandle
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

/** View model for launch icons. */
@HiltViewModel
class LauncherViewModel @Inject constructor(
    application: Application,
    private val launcherApps: LauncherApps,
    private val launcherDatabase: LauncherDatabase
) : AndroidViewModel(application) {

    private val launcherActivityMetadata by lazy {
        launcherDatabase.launcherActivityMetadata()
    }

    private val launcherAppsCallback = LauncherAppsCallback()

    private val mutableLauncherActivities = MutableStateFlow<List<LauncherActivityData>>(listOf())
    val launcherActivities: StateFlow<List<LauncherActivityData>> = mutableLauncherActivities

    val activitiesMetadata: StateFlow<ActivitiesMetadata> = launcherActivities.map { activities ->
        ActivitiesMetadata(activities.any { it.user != myUserHandle() },
            activities.any { it.tags.contains(HIDDEN_TAG) })
    }.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(), ActivitiesMetadata(
            hasWorkActivities = false, hasHiddenActivities = false
        )
    )

    val activitiesFilter: MutableStateFlow<ActivitiesFilter> =
        MutableStateFlow(ActivitiesFilter(showWorkActivities = false, showHiddenActivities = false))

    init {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                mutableLauncherActivities.emit(launcherApps.profiles.flatMap {
                    launcherApps.getActivityList(
                        null, it
                    )
                }.map {
                    LauncherActivityData(getApplication(), it, launcherActivityMetadata.get(it))
                })
            }
        }

        launcherApps.registerCallback(launcherAppsCallback)
    }

    override fun onCleared() {
        super.onCleared()

        launcherApps.unregisterCallback(launcherAppsCallback)
    }

    /** Sets the list of tags to associate with the given [LauncherActivityData] */
    fun updateTags(launcherActivityData: LauncherActivityData, tags: Set<String>) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                launcherActivityMetadata.put(
                    LauncherActivityMetadata(
                        launcherActivityData.component, launcherActivityData.user, tags
                    )
                )
                update(launcherActivityData.component.packageName, launcherActivityData.user)
            }
        }
    }

    /** Sets the visibility of the given app. */
    fun setVisibility(launcherActivityData: LauncherActivityData, isVisible: Boolean) {
        if (isVisible) {
            updateTags(launcherActivityData, launcherActivityData.tags.minus(HIDDEN_TAG))
        } else {
            updateTags(launcherActivityData, launcherActivityData.tags.plus(HIDDEN_TAG))
        }
    }

    /** Returns true if the given app is visible. */
    fun isVisible(launcherActivityData: LauncherActivityData): Boolean {
        return !launcherActivityData.tags.contains(HIDDEN_TAG)
    }

    private fun update(packageName: String, user: UserHandle) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                mutableLauncherActivities.value.toMutableList().apply {
                    removeIf { it.component.packageName == packageName && it.user == user }
                    addAll(launcherApps.getActivityList(packageName, user).map {
                        LauncherActivityData(
                            getApplication(), it, launcherActivityMetadata.get(it)
                        )
                    })
                    mutableLauncherActivities.emit(toList())
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

    data class ActivitiesMetadata(val hasWorkActivities: Boolean, val hasHiddenActivities: Boolean)

    data class ActivitiesFilter(val showWorkActivities: Boolean, val showHiddenActivities: Boolean)

    companion object {
        private const val HIDDEN_TAG = "hidden"
    }
}
