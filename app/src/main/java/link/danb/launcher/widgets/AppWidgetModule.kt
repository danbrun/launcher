package link.danb.launcher.widgets

import android.annotation.SuppressLint
import android.app.Application
import android.appwidget.AppWidgetHost
import android.appwidget.AppWidgetHostView
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProviderInfo
import android.content.Context
import android.view.GestureDetector
import android.view.GestureDetector.SimpleOnGestureListener
import android.view.MotionEvent
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import link.danb.launcher.R
import link.danb.launcher.utils.updateAppWidgetSize
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
class AppWidgetModule {

    @Provides
    @Singleton
    fun getAppWidgetManager(application: Application): AppWidgetManager {
        return AppWidgetManager.getInstance(application)
    }

    @Provides
    @Singleton
    fun getAppWidgetHost(application: Application): AppWidgetHost {
        return LauncherWidgetHost(application, R.id.app_widget_host_id)
    }

    /** An [AppWidgetHost] that builds custom [AppWidgetHostView] implementations. */
    private class LauncherWidgetHost(context: Context, hostId: Int) :
        AppWidgetHost(context, hostId) {

        override fun onCreateView(
            context: Context?, appWidgetId: Int, appWidget: AppWidgetProviderInfo?
        ): AppWidgetHostView {
            return LauncherWidgetHostView(context)
        }
    }

    /** Custom [AppWidgetHostView] that intercepts long presses. */
    private class LauncherWidgetHostView(context: Context?) : AppWidgetHostView(context) {

        private val gestureDetector = GestureDetector(context, object : SimpleOnGestureListener() {
            init {
                isLongClickable = true
            }

            override fun onLongPress(e: MotionEvent) {
                performLongClick()
            }
        })

        var customHeight: Int? = null
            set(value) {
                field = value
                invalidate()
            }

        override fun onInterceptTouchEvent(event: MotionEvent): Boolean {
            return gestureDetector.onTouchEvent(event)
        }

        @SuppressLint("ClickableViewAccessibility")
        override fun onTouchEvent(event: MotionEvent): Boolean {
            return gestureDetector.onTouchEvent(event)
        }

        override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
            if (customHeight != null) {
                super.onMeasure(
                    widthMeasureSpec,
                    MeasureSpec.makeMeasureSpec(customHeight!!, MeasureSpec.EXACTLY)
                )
            } else {
                super.onMeasure(widthMeasureSpec, heightMeasureSpec)
            }
        }

        override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
            updateAppWidgetSize(right - left, top - bottom)
            super.onLayout(changed, left, top, right, bottom)
        }
    }
}
