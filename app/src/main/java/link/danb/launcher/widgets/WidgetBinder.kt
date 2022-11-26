package link.danb.launcher.widgets

import android.appwidget.AppWidgetHost
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProviderInfo
import android.os.UserHandle
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import link.danb.launcher.R
import link.danb.launcher.utils.getParcelableCompat

/** Binds new widgets and launches permissions and configuration activities. */
class WidgetBinder(
    private val fragment: Fragment,
    private val widgetBindListener: WidgetBindListener
) : DefaultLifecycleObserver {

    private val appWidgetHost: AppWidgetHost by lazy {
        AppWidgetHost(fragment.requireContext().applicationContext, R.id.app_widget_host_id)
    }

    private val appWidgetManager: AppWidgetManager by lazy {
        AppWidgetManager.getInstance(fragment.requireContext().applicationContext)
    }

    private lateinit var widgetPermissionLauncher: ActivityResultLauncher<WidgetHandle>
    private lateinit var widgetConfigurationLauncher: ActivityResultLauncher<WidgetHandle>

    private var widgetHandle: WidgetHandle? = null

    init {
        fragment.lifecycle.addObserver(this)
    }

    override fun onCreate(owner: LifecycleOwner) {
        fragment.savedStateRegistry.registerSavedStateProvider(SAVED_STATE_KEY) {
            if (widgetHandle != null) {
                bundleOf(Pair(WIDGET_HANDLE_KEY, widgetHandle))
            } else {
                bundleOf()
            }
        }

        if (fragment.savedStateRegistry.isRestored) {
            widgetHandle =
                fragment.savedStateRegistry
                    .consumeRestoredStateForKey(SAVED_STATE_KEY)
                    ?.getParcelableCompat(WIDGET_HANDLE_KEY)
        }

        widgetPermissionLauncher =
            fragment.registerForActivityResult(WidgetPermissionResultContract()) { success ->
                if (success) {
                    widgetConfigurationLauncher.launch(widgetHandle)
                } else {
                    onBindFailed()
                    widgetBindListener.onWidgetBind(false)
                    widgetHandle = null
                }
            }

        widgetConfigurationLauncher =
            fragment.registerForActivityResult(WidgetConfigurationResultContract()) { success ->
                if (!success) {
                    onBindFailed()
                }
                widgetBindListener.onWidgetBind(success)
                widgetHandle = null
            }
    }

    /** Bind a new widget from the given provider. */
    fun bindWidget(providerInfo: AppWidgetProviderInfo, userHandle: UserHandle) {
        if (widgetHandle != null) {
            throw IllegalStateException("Attempted to bind app widget when binding is in progress")
        }

        widgetHandle = WidgetHandle(appWidgetHost.allocateAppWidgetId(), providerInfo, userHandle)
        if (bindAppWidgetIfAllowed()) {
            widgetConfigurationLauncher.launch(widgetHandle)
        } else {
            widgetPermissionLauncher.launch(widgetHandle)
        }
    }

    private fun bindAppWidgetIfAllowed(): Boolean {
        return appWidgetManager.bindAppWidgetIdIfAllowed(
            widgetHandle!!.id,
            widgetHandle!!.user,
            widgetHandle!!.info.provider,
            null
        )
    }

    private fun onBindFailed() {
        Toast.makeText(
            fragment.context,
            R.string.widget_bind_failure,
            Toast.LENGTH_SHORT
        ).show()
        appWidgetHost.deleteAppWidgetId(widgetHandle!!.id)
    }

    companion object {
        private const val SAVED_STATE_KEY = "widget_saved_state_key"
        private const val WIDGET_HANDLE_KEY = "widget_handle_key"
    }
}

fun interface WidgetBindListener {
    fun onWidgetBind(success: Boolean)
}
