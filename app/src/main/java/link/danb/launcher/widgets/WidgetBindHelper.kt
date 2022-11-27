package link.danb.launcher.widgets

import android.appwidget.AppWidgetProviderInfo
import android.os.UserHandle
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.annotation.StringRes
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import link.danb.launcher.R
import link.danb.launcher.utils.getParcelableCompat

/** Binds new widgets and launches permissions and configuration activities. */
class WidgetBindHelper(
    private val fragment: Fragment,
    private val widgetBindListener: WidgetBindListener
) : DefaultLifecycleObserver {

    private val widgetViewModel: WidgetViewModel by fragment.activityViewModels()

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
                    onBindFailed(R.string.widget_permission_denied)
                }
            }

        widgetConfigurationLauncher =
            fragment.registerForActivityResult(WidgetConfigurationResultContract()) { success ->
                if (success) {
                    widgetViewModel.refresh()
                    widgetBindListener.onWidgetBind(true)
                    widgetHandle = null
                } else {
                    onBindFailed(R.string.widget_configuration_failed)
                }
            }
    }

    /** Bind a new widget from the given provider. */
    fun bindWidget(providerInfo: AppWidgetProviderInfo, userHandle: UserHandle) {
        if (widgetHandle != null) {
            throw IllegalStateException("Attempted to bind app widget when binding is in progress")
        }

        widgetHandle = WidgetHandle(widgetViewModel.allocateWidgetId(), providerInfo, userHandle)
        if (widgetViewModel.bindWidgetIfAllowed(widgetHandle!!)) {
            widgetConfigurationLauncher.launch(widgetHandle)
        } else {
            widgetPermissionLauncher.launch(widgetHandle)
        }
    }

    private fun onBindFailed(@StringRes errorMessage: Int) {
        Toast.makeText(fragment.context, errorMessage, Toast.LENGTH_SHORT).show()
        widgetViewModel.deleteWidgetId(widgetHandle!!.id)
        widgetBindListener.onWidgetBind(false)
        widgetHandle = null
    }

    companion object {
        private const val SAVED_STATE_KEY = "widget_saved_state_key"
        private const val WIDGET_HANDLE_KEY = "widget_handle_key"
    }
}

fun interface WidgetBindListener {
    fun onWidgetBind(success: Boolean)
}
