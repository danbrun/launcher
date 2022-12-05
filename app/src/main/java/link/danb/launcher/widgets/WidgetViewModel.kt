package link.danb.launcher.widgets

import android.appwidget.AppWidgetHost
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class WidgetViewModel : ViewModel() {

    private val _widgetIds: MutableLiveData<List<Int>> = MutableLiveData()

    val widgetIds: LiveData<List<Int>> = _widgetIds

    fun refresh(appWidgetHost: AppWidgetHost) {
        _widgetIds.postValue(appWidgetHost.appWidgetIds.toList())
    }
}
