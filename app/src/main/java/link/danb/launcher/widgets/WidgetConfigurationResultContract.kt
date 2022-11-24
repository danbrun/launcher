package link.danb.launcher.widgets

import android.app.Activity
import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager.ComponentInfoFlags
import android.os.Build
import androidx.activity.result.contract.ActivityResultContract

/** Activity result contract for launching a widget configuration activity. */
class WidgetConfigurationResultContract : ActivityResultContract<WidgetHandle, Boolean>() {

    override fun createIntent(context: Context, input: WidgetHandle): Intent {
        return Intent().apply {
            action = AppWidgetManager.ACTION_APPWIDGET_CONFIGURE
            component = input.info.configure
            putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, input.id)
        }
    }

    override fun parseResult(resultCode: Int, intent: Intent?): Boolean {
        return resultCode == Activity.RESULT_OK
    }

    /** Check that widget has an exported configuration activity before launching. */
    override fun getSynchronousResult(
        context: Context,
        input: WidgetHandle
    ): SynchronousResult<Boolean>? {
        val configure = input.info.configure
        if (configure != null) {
            val activityInfo = if (Build.VERSION.SDK_INT >= 33) {
                context.packageManager.getActivityInfo(configure, ComponentInfoFlags.of(0))
            } else {
                @Suppress("DEPRECATION")
                context.packageManager.getActivityInfo(configure, 0)
            }
            if (activityInfo.exported) {
                return null
            }
        }
        return SynchronousResult(true)
    }
}
