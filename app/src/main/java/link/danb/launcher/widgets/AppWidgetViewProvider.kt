package link.danb.launcher.widgets

import android.app.Activity
import android.appwidget.AppWidgetHost
import android.appwidget.AppWidgetHostView
import android.appwidget.AppWidgetManager
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

    init {
        (activity as AppCompatActivity).lifecycle.addObserver(lifecycleObserver)
    }

    fun createView(widgetId: Int): AppWidgetHostView {
        return appWidgetHost.createView(
            activity.applicationContext, widgetId, appWidgetManager.getAppWidgetInfo(widgetId)
        )
    }
}
