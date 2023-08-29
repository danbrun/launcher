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
import link.danb.launcher.apps.LauncherAppsCallback
import link.danb.launcher.database.ActivityData
import link.danb.launcher.database.LauncherDatabase
import javax.inject.Inject

/** View model for launch icons. */
@HiltViewModel
class ActivitiesViewModel @Inject constructor(
    application: Application,
    private val launcherApps: LauncherApps,
    private val launcherDatabase: LauncherDatabase
) : AndroidViewModel(application) {

    private val activityData by lazy { launcherDatabase.activityData() }

    private val launcherAppsCallback = LauncherAppsCallback(this::update)

    private val _launcherActivities = MutableStateFlow<List<ActivityInfoWithData>>(listOf())

    val launcherActivities: StateFlow<List<ActivityInfoWithData>> = _launcherActivities.asStateFlow()

    init {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                _launcherActivities.emit(launcherApps.profiles.flatMap {
                    launcherApps.getActivityList(
                        null, it
                    )
                }.map { ActivityInfoWithData(it, getMetadata(it)) })
            }
        }

        launcherApps.registerCallback(launcherAppsCallback)
    }

    override fun onCleared() {
        super.onCleared()

        launcherApps.unregisterCallback(launcherAppsCallback)
    }

    /** Sets the list of tags to associate with the given [ActivityInfoWithData] */
    // May use this again soon so leaving it for now.
    @Suppress("unused")
    fun updateTags(info: LauncherActivityInfo, tags: Set<String>) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                activityData.put(getMetadata(info).copy(tags = tags))
                update(info)
            }
        }
    }

    /** Sets the visibility of the given app. */
    fun setIsHidden(info: LauncherActivityInfo, isHidden: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            activityData.put(getMetadata(info).copy(isHidden = isHidden))
            update(info)
        }
    }

    private fun getMetadata(info: LauncherActivityInfo): ActivityData =
        activityData.get(info.componentName, info.user) ?: ActivityData(
            info.componentName, info.user, isHidden = false, tags = setOf()
        )

    private fun update(info: LauncherActivityInfo) {
        update(info.componentName.packageName, info.user)
    }

    private fun update(packageName: String, user: UserHandle) {
        viewModelScope.launch(Dispatchers.IO) {
            _launcherActivities.value.toMutableList().apply {
                removeIf { it.info.componentName.packageName == packageName && it.info.user == user }
                addAll(launcherApps.getActivityList(packageName, user)
                    .map { ActivityInfoWithData(it, getMetadata(it)) })
                _launcherActivities.emit(toList())
            }
        }
    }

}
