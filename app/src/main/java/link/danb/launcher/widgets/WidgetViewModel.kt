package link.danb.launcher.widgets

import android.appwidget.AppWidgetHost
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class WidgetViewModel : ViewModel() {

    private val _widgetIds: MutableStateFlow<List<Int>> = MutableStateFlow(listOf())
    val widgetIds: StateFlow<List<Int>> = _widgetIds

    fun refresh(appWidgetHost: AppWidgetHost) {
        _widgetIds.value = appWidgetHost.appWidgetIds.toList()
    }
}
