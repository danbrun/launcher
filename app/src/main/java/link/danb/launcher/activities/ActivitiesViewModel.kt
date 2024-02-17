package link.danb.launcher.activities

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import link.danb.launcher.database.ActivityData
import link.danb.launcher.database.LauncherDatabase

/** View model for launch icons. */
@HiltViewModel
class ActivitiesViewModel
@Inject
constructor(activityManager: ActivityManager, launcherDatabase: LauncherDatabase) : ViewModel() {

  private val activityData = launcherDatabase.activityData()

  val activities: Flow<List<ActivityData>> =
    combine(activityManager.activities, activityData.get()) { activities, data ->
        val dataMap =
          data
            .associateBy { it.userActivity }
            .withDefault { ActivityData(it, isPinned = false, isHidden = false) }

        activities.map { component -> dataMap.getValue(component) }
      }
      .shareIn(viewModelScope, SharingStarted.WhileSubscribed(), replay = 1)

  fun setMetadata(activityMetadata: ActivityData) =
    viewModelScope.launch(Dispatchers.IO) { activityData.put(activityMetadata) }
}
