package link.danb.launcher.widgets

import android.app.Application
import android.appwidget.AppWidgetHost
import android.appwidget.AppWidgetHostView
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProviderInfo
import android.content.ComponentName
import android.os.UserHandle
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import link.danb.launcher.R

/** View model for widgets in launcher. */
class WidgetViewModel(application: Application) : AndroidViewModel(application) {

    private val appWidgetHost: AppWidgetHost by lazy {
        AppWidgetHost(application, R.id.app_widget_host_id)
    }

    private val appWidgetManager: AppWidgetManager by lazy {
        AppWidgetManager.getInstance(application)
    }

    private val _widgetIds = MutableLiveData<List<Int>>()

    /** The currently bound widget in the launcher. */
    val widgetIds: LiveData<List<Int>> = _widgetIds

    init {
        appWidgetHost.startListening()

        // Remove any existing widgets that cannot be accessed.
        appWidgetHost.appWidgetIds
            .filter { appWidgetManager.getAppWidgetInfo(it) == null }
            .forEach { appWidgetHost.deleteAppWidgetId(it) }

        refresh()
    }

    override fun onCleared() {
        appWidgetHost.stopListening()
    }

    /** Unbind all currently bound widgets. */
    fun unbind() {
        appWidgetHost.appWidgetIds.forEach { appWidgetHost.deleteAppWidgetId(it) }
        refresh()
    }

    /** Reload all bound widgets. */
    fun refresh() {
        _widgetIds.postValue(appWidgetHost.appWidgetIds.toList())
    }

    /** Create a view for the widget to add to the launcher view hierarchy. */
    fun getView(widgetId: Int): AppWidgetHostView {
        return appWidgetHost.createView(
            getApplication(),
            widgetId,
            appWidgetManager.getAppWidgetInfo(widgetId)
        )
    }

    /** Get all available widget providers. */
    val providers: List<AppWidgetProviderInfo>
        get() = appWidgetManager.installedProviders

    /** Get all widget providers for the given package and user. */
    fun getProvidersForPackage(
        componentName: ComponentName,
        userHandle: UserHandle
    ): List<AppWidgetProviderInfo> {
        return appWidgetManager.getInstalledProvidersForPackage(
            componentName.packageName,
            userHandle
        )
    }
}
