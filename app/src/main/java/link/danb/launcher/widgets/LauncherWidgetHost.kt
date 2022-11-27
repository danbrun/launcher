package link.danb.launcher.widgets

import android.appwidget.AppWidgetHost
import android.appwidget.AppWidgetHostView
import android.appwidget.AppWidgetProviderInfo
import android.content.Context

/** An [AppWidgetHost] that builds custom [AppWidgetHostView] implementations. */
class LauncherWidgetHost(context: Context?, hostId: Int) : AppWidgetHost(context, hostId) {

    override fun onCreateView(
        context: Context?,
        appWidgetId: Int,
        appWidget: AppWidgetProviderInfo?
    ): AppWidgetHostView {
        return LauncherWidgetHostView(context)
    }
}
