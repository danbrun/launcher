package link.danb.launcher.work

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

@HiltViewModel
class WorkProfileViewModel @Inject constructor(application: Application) :
    AndroidViewModel(application) {

    private val _showWorkActivities: MutableStateFlow<Boolean> = MutableStateFlow(false)

    val showWorkActivities: StateFlow<Boolean> = _showWorkActivities.asStateFlow()

    fun toggleWorkActivities() {
        _showWorkActivities.value = !_showWorkActivities.value
    }
}
