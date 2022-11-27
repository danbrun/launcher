package link.danb.launcher.widgets

import android.app.Activity
import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.Intent
import androidx.activity.result.contract.ActivityResultContract

/** Activity result contract for launching a widget configuration activity. */
class WidgetConfigurationResultContract : ActivityResultContract<WidgetHandle, Boolean>() {

    override fun createIntent(context: Context, input: WidgetHandle): Intent {
        return Intent(context, WidgetConfigurationLauncherActivity::class.java)
            .putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, input.id)
    }

    override fun parseResult(resultCode: Int, intent: Intent?): Boolean {
        return resultCode == Activity.RESULT_OK
    }
}
