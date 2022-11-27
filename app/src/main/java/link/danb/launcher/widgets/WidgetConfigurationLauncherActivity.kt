package link.danb.launcher.widgets

import android.app.Activity
import android.appwidget.AppWidgetHost
import android.appwidget.AppWidgetManager
import android.content.ActivityNotFoundException
import android.content.Intent
import android.os.Bundle
import androidx.activity.result.contract.ActivityResultContract
import androidx.appcompat.app.AppCompatActivity
import link.danb.launcher.R

/**
 * Trampoline activity to launch widget configuration using [WidgetConfigurationResultContract].
 *
 * The [AppWidgetHost.startAppWidgetConfigureActivityForResult] method requires overriding
 * [Activity.onActivityResult] to get the activity results. This makes it infeasible to call
 * directly from [WidgetBindHelper], so this activity exists to wrap the widget configuration
 * activity within an [ActivityResultContract].
 */
class WidgetConfigurationLauncherActivity : AppCompatActivity() {

    private val widgetId: Int by lazy {
        intent.getIntExtra(
            AppWidgetManager.EXTRA_APPWIDGET_ID,
            AppWidgetManager.INVALID_APPWIDGET_ID
        )
    }

    private val appWidgetHost: AppWidgetHost by lazy {
        AppWidgetHost(this, R.id.app_widget_host_id)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        try {
            appWidgetHost.startAppWidgetConfigureActivityForResult(
                this, widgetId, 0, R.id.app_widget_configure_request_id, null
            )
        } catch (exception: ActivityNotFoundException) {
            // The widget does not require configuration, so return successfully.
            setResult(Activity.RESULT_OK)
            finish()
        }
    }

    @Deprecated(
        "Deprecated in Java", ReplaceWith(
            "super.onActivityResult(requestCode, resultCode, data)",
            "androidx.appcompat.app.AppCompatActivity"
        )
    )
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        @Suppress("DEPRECATION")
        super.onActivityResult(requestCode, resultCode, data)

        setResult(resultCode, data)
        finish()
    }
}
