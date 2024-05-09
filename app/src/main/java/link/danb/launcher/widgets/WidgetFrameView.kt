package link.danb.launcher.widgets

import android.app.Activity
import android.app.Application
import android.appwidget.AppWidgetHost
import android.appwidget.AppWidgetHostView
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProviderInfo
import android.content.Context
import android.os.Build
import android.util.AttributeSet
import android.widget.FrameLayout
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.res.ResourcesCompat
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import dagger.hilt.android.AndroidEntryPoint
import dagger.hilt.android.scopes.ActivityScoped
import javax.inject.Inject
import link.danb.launcher.extensions.updateAppWidgetSize

@AndroidEntryPoint
class WidgetFrameView @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null) :
  FrameLayout(context, attrs) {

  @Inject lateinit var appWidgetManager: AppWidgetManager
  @Inject lateinit var appWidgetViewProvider: AppWidgetViewProvider

  private var appWidgetHostView: AppWidgetHostView? = null

  fun setAppWidget(widgetId: Int) {
    createOrUpdateView(widgetId, appWidgetManager.getAppWidgetInfo(widgetId))
  }

  @RequiresApi(Build.VERSION_CODES.S)
  fun setAppWidgetPreview(providerInfo: AppWidgetProviderInfo) {
    createOrUpdateView(
      ResourcesCompat.ID_NULL,
      providerInfo.clone().apply { initialLayout = previewLayout },
    )
  }

  fun clearAppWidget() {
    removeAllViews()
    appWidgetHostView = null
  }

  fun updateSize() {
    appWidgetHostView?.updateAppWidgetSize(width, height)
  }

  private fun createOrUpdateView(widgetId: Int, providerInfo: AppWidgetProviderInfo) {
    if (appWidgetHostView == null) {
      appWidgetHostView = appWidgetViewProvider.createView(widgetId, providerInfo)
      addView(appWidgetHostView)
    } else {
      appWidgetHostView?.setAppWidget(widgetId, providerInfo)
    }
  }
}

@ActivityScoped
class AppWidgetViewProvider
@Inject
constructor(
  private val application: Application,
  activity: Activity,
  private val appWidgetHost: AppWidgetHost,
) {

  private val lifecycleObserver =
    object : DefaultLifecycleObserver {
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

  fun createView(widgetId: Int, providerInfo: AppWidgetProviderInfo): AppWidgetHostView =
    appWidgetHost.createView(application, widgetId, providerInfo)
}
