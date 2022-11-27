package link.danb.launcher.widgets

import android.app.Activity
import android.app.Application
import android.appwidget.AppWidgetHost
import android.appwidget.AppWidgetHostView
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProviderInfo
import android.content.ActivityNotFoundException
import android.content.ComponentName
import android.os.UserHandle
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import link.danb.launcher.R

/** View model for widgets in launcher. */
class WidgetViewModel(application: Application) : AndroidViewModel(application) {

    private val appWidgetHost: AppWidgetHost by lazy {
        LauncherWidgetHost(application, R.id.app_widget_host_id)
    }

    private val appWidgetManager: AppWidgetManager by lazy {
        AppWidgetManager.getInstance(application)
    }

    private val _widgetIds = MutableLiveData<List<Int>>()

    /** The currently bound widget in the launcher. */
    val widgetIds: LiveData<List<Int>> = _widgetIds

    init {
        appWidgetHost.startListening()

        refresh()
    }

    override fun onCleared() {
        appWidgetHost.stopListening()
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

    /**
     * Binds a new widget ID.
     *
     * This should only be called by [WidgetBindHelper].
     */
    fun allocateWidgetId(): Int {
        return appWidgetHost.allocateAppWidgetId()
    }

    /** Unbind all currently bound widgets. */
    fun deleteWidgets() {
        appWidgetHost.appWidgetIds.forEach { appWidgetHost.deleteAppWidgetId(it) }
        refresh()
    }

    /** Unbind the widget with the given ID. */
    fun deleteWidgetId(widgetId: Int) {
        appWidgetHost.deleteAppWidgetId(widgetId)
        refresh()
    }

    /**
     * Attempts to bind the widget to the widget ID and returns true if successful.
     *
     * This should only be called by [WidgetBindHelper].
     */
    fun bindWidgetIfAllowed(widgetHandle: WidgetHandle): Boolean {
        return appWidgetManager.bindAppWidgetIdIfAllowed(
            widgetHandle.id, widgetHandle.user, widgetHandle.info.provider, null
        )
    }

    /** Reload all bound widgets. */
    fun refresh() {
        // Remove any existing widgets that cannot be accessed.
        appWidgetHost.appWidgetIds
            .filter { appWidgetManager.getAppWidgetInfo(it) == null }
            .forEach { appWidgetHost.deleteAppWidgetId(it) }

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

    /** Launches the configuration activity for the given widget if it has one. */
    fun startConfiguration(activity: Activity, widgetHandle: WidgetHandle) {
        try {
            appWidgetHost.startAppWidgetConfigureActivityForResult(
                activity,
                widgetHandle.id,
                0,
                R.id.app_widget_configure_request_id,
                null
            )
        } catch (exception: ActivityNotFoundException) {
            // Skip configuring this activity.
        }
    }
}
