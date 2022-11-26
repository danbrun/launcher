package link.danb.launcher.widgets

import android.app.Activity
import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.Intent
import androidx.activity.result.contract.ActivityResultContract

/** Activity result contract for launching a widget permission dialog. */
class WidgetPermissionResultContract : ActivityResultContract<WidgetHandle, Boolean>() {

    override fun createIntent(context: Context, input: WidgetHandle): Intent {
        return Intent(AppWidgetManager.ACTION_APPWIDGET_BIND)
            .putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, input.id)
            .putExtra(AppWidgetManager.EXTRA_APPWIDGET_PROVIDER, input.info.provider)
            .putExtra(AppWidgetManager.EXTRA_APPWIDGET_PROVIDER_PROFILE, input.info.profile)
    }

    override fun parseResult(resultCode: Int, intent: Intent?): Boolean {
        return resultCode == Activity.RESULT_OK
    }
}
