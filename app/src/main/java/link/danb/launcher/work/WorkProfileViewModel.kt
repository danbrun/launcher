package link.danb.launcher.work

import android.app.Application
import android.content.pm.LauncherApps
import android.os.Process.myUserHandle
import android.os.UserHandle
import androidx.lifecycle.AndroidViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

@HiltViewModel
class WorkProfileViewModel @Inject constructor(
    application: Application, private val launcherApps: LauncherApps
) : AndroidViewModel(application) {

    private val _currentUser: MutableStateFlow<UserHandle> = MutableStateFlow(myUserHandle())

    val currentUser: StateFlow<UserHandle> = _currentUser.asStateFlow()

    fun toggleWorkActivities() {
        _currentUser.value =
            launcherApps.profiles.firstOrNull { it != _currentUser.value } ?: myUserHandle()
    }
}
