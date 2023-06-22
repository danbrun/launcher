package link.danb.launcher.widgets

import android.app.Activity
import android.appwidget.AppWidgetHost
import android.appwidget.AppWidgetHostView
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProviderInfo
import android.content.res.Resources
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import dagger.hilt.android.scopes.ActivityScoped
import javax.inject.Inject

/** Creates [AppWidgetHostView] instances and listens for updates. */
@ActivityScoped
class AppWidgetViewProvider @Inject constructor(
    private val activity: Activity,
    private val appWidgetHost: AppWidgetHost,
    private val appWidgetManager: AppWidgetManager
) {
    private val lifecycleObserver = object : DefaultLifecycleObserver {
        override fun onStart(owner: LifecycleOwner) {
            appWidgetHost.startListening()
        }

        override fun onStop(owner: LifecycleOwner) {
            appWidgetHost.stopListening()
        }
    }

    private val widgetViews: MutableMap<Int, AppWidgetHostView> = mutableMapOf()

    init {
        (activity as AppCompatActivity).lifecycle.addObserver(lifecycleObserver)
    }

    fun getView(widgetId: Int): AppWidgetHostView {
        if (!widgetViews.containsKey(widgetId)) {
            widgetViews[widgetId] = appWidgetHost.createView(
                activity.applicationContext, widgetId, appWidgetManager.getAppWidgetInfo(widgetId)
            )
        }
        return widgetViews[widgetId]!!
    }

    @RequiresApi(Build.VERSION_CODES.S)
    fun createPreview(appWidgetProviderInfo: AppWidgetProviderInfo): AppWidgetHostView {
        return appWidgetHost.createView(activity.applicationContext,
            Resources.ID_NULL,
            appWidgetProviderInfo.clone().apply { initialLayout = previewLayout }).apply {
            updateAppWidget(null)
        }
    }
}
