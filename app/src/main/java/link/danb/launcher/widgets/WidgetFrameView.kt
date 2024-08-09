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
import android.view.MotionEvent
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
    private set(value) {
      if (value == null) {
        removeAllViews()
      } else if (field !== value) {
        (value.parent as WidgetFrameView?)?.clearAppWidget()
        addView(value)
      }
      field = value
    }

  private var isPreview: Boolean = false

  override fun onInterceptTouchEvent(ev: MotionEvent?): Boolean = isPreview

  override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
    super.onLayout(changed, left, top, right, bottom)

    appWidgetHostView?.updateAppWidgetSize(width, height)
  }

  fun setAppWidget(widgetId: Int) {
    appWidgetHostView =
      appWidgetViewProvider.getView(widgetId, appWidgetManager.getAppWidgetInfo(widgetId))
    isPreview = false
  }

  @RequiresApi(Build.VERSION_CODES.S)
  fun setAppWidgetPreview(providerInfo: AppWidgetProviderInfo) {
    appWidgetHostView = appWidgetViewProvider.getPreview(providerInfo)
    isPreview = true
  }

  fun clearAppWidget() {
    appWidgetHostView = null
    isPreview = false
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
  private val appWidgetViewCache: MutableMap<Int, AppWidgetHostView> = mutableMapOf()

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

  fun getView(widgetId: Int, providerInfo: AppWidgetProviderInfo): AppWidgetHostView =
    appWidgetViewCache.getOrPut(widgetId) {
      appWidgetHost.createView(application, widgetId, providerInfo)
    }

  @RequiresApi(Build.VERSION_CODES.S)
  fun getPreview(providerInfo: AppWidgetProviderInfo): AppWidgetHostView =
    appWidgetHost.createView(
      application,
      ResourcesCompat.ID_NULL,
      providerInfo.clone().apply { initialLayout = previewLayout },
    )
}
