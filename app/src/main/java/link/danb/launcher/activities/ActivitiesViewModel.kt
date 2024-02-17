package link.danb.launcher.activities

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import link.danb.launcher.database.ActivityData
import link.danb.launcher.database.LauncherDatabase

/** View model for launch icons. */
@HiltViewModel
class ActivitiesViewModel @Inject constructor(private val launcherDatabase: LauncherDatabase) :
  ViewModel() {

  fun setMetadata(activityMetadata: ActivityData) =
    viewModelScope.launch(Dispatchers.IO) { launcherDatabase.activityData().put(activityMetadata) }
}
