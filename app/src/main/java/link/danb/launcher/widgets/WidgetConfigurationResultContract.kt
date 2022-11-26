package link.danb.launcher.widgets

import android.app.Activity
import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.Intent
import androidx.activity.result.contract.ActivityResultContract

/** Activity result contract for launching a widget configuration activity. */
class WidgetConfigurationResultContract : ActivityResultContract<WidgetHandle, Boolean>() {

    override fun createIntent(context: Context, input: WidgetHandle): Intent {
        return Intent()
            .setAction(AppWidgetManager.ACTION_APPWIDGET_CONFIGURE)
            .setComponent(input.info.configure)
            .putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, input.id)
            .putExtra(AppWidgetManager.EXTRA_APPWIDGET_PROVIDER_PROFILE, input.user)
    }

    override fun parseResult(resultCode: Int, intent: Intent?): Boolean {
        return resultCode == Activity.RESULT_OK
    }

    /** Check that widget has an exported configuration activity before launching. */
    override fun getSynchronousResult(
        context: Context,
        input: WidgetHandle
    ): SynchronousResult<Boolean>? {
        val activityInfo =
            createIntent(context, input).resolveActivityInfo(context.packageManager, 0)
        if (activityInfo != null && activityInfo.exported) {
            return null
        }
        return SynchronousResult(true)
    }
}
