package link.danb.launcher.widgets

import android.app.Activity
import android.app.Application
import android.appwidget.AppWidgetHost
import android.appwidget.AppWidgetHostView
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProviderInfo
import android.content.Context
import android.content.Intent
import androidx.activity.result.contract.ActivityResultContract
import androidx.lifecycle.*
import link.danb.launcher.R

/** Represents an allocated widget ID and corresponding widget provider. */
data class WidgetHandle(val id: Int, val info: AppWidgetProviderInfo)

/** View model for widgets in launcher. */
class WidgetViewModel(application: Application) : AndroidViewModel(application),
    DefaultLifecycleObserver {

    private val appWidgetManager: AppWidgetManager by lazy {
        AppWidgetManager.getInstance(application)
    }

    private val appWidgetHost: AppWidgetHost by lazy {
        AppWidgetHost(application, R.id.app_widget_host_id)
    }

    private val _widgetHandles = MutableLiveData<List<WidgetHandle>>()

    /** The currently bound widget in the launcher. */
    val widgetHandles: LiveData<List<WidgetHandle>> = _widgetHandles

    override fun onCreate(owner: LifecycleOwner) {
        super.onCreate(owner)

        appWidgetHost.startListening()

        update()
    }

    override fun onDestroy(owner: LifecycleOwner) {
        super.onDestroy(owner)

        appWidgetHost.stopListening()
    }

    /** Return all widget providers. */
    fun getProviders(): List<AppWidgetProviderInfo> {
        return appWidgetManager.installedProviders.toList()
    }

    /** Create a WidgetHandle with a newly allocated widget ID for the given provider. */
    fun newHandle(info: AppWidgetProviderInfo): WidgetHandle {
        return WidgetHandle(appWidgetHost.allocateAppWidgetId(), info)
    }

    /** Attempt to bind the widget referenced by the handle, returning true if successful. */
    fun bind(widgetHandle: WidgetHandle): Boolean {
        val success = appWidgetManager.bindAppWidgetIdIfAllowed(
            widgetHandle.id,
            widgetHandle.info.provider
        )
        if (success) {
            update()
        }
        return success
    }

    /** Unbind the current widget. */
    fun unbind() {
        _widgetHandles.postValue(listOf())
        reset()
    }

    /** Create a view for the widget to add to the launcher view hierarchy. */
    fun getView(widgetHandle: WidgetHandle): AppWidgetHostView {
        return appWidgetHost
            .createView(getApplication(), widgetHandle.id, widgetHandle.info)
            .apply { setAppWidget(widgetHandle.id, widgetHandle.info) }
    }

    private fun reset() {
        appWidgetHost.appWidgetIds.forEach { appWidgetHost.deleteAppWidgetId(it) }
    }

    private fun update() {
        _widgetHandles.postValue(appWidgetHost.appWidgetIds.map {
            appWidgetManager.getAppWidgetInfo(it)?.let { info -> WidgetHandle(it, info) }
        }.filterNotNull())
    }

    /** Activity contract that launches the widgets permission dialog and handles the result. */
    inner class WidgetPermissionResultHandler : ActivityResultContract<WidgetHandle, Boolean>() {

        private var widgetHandle: WidgetHandle? = null

        override fun createIntent(context: Context, input: WidgetHandle): Intent {
            widgetHandle = input

            return Intent(Intent(AppWidgetManager.ACTION_APPWIDGET_BIND).apply {
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, input.id)
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_PROVIDER, input.info.provider)
            })
        }

        override fun parseResult(resultCode: Int, intent: Intent?): Boolean {
            val success = resultCode == Activity.RESULT_OK
            if (success) {
                update()
            } else {
                appWidgetHost.deleteAppWidgetId(widgetHandle!!.id)
            }
            return success
        }
    }

}
