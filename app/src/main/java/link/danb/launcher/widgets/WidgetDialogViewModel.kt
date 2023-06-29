package link.danb.launcher.widgets

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

@HiltViewModel
class WidgetDialogViewModel @Inject constructor() : ViewModel() {

    private val _expandedPackageNames: MutableStateFlow<Set<String>> = MutableStateFlow(setOf())
    val expandedPackageNames: StateFlow<Set<String>> = _expandedPackageNames

    fun toggleExpandedPackageName(packageName: String) {
        if (_expandedPackageNames.value.contains(packageName)) {
            _expandedPackageNames.value = _expandedPackageNames.value - packageName
        } else {
            _expandedPackageNames.value = _expandedPackageNames.value + packageName
        }
    }
}
