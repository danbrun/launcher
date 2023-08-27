package link.danb.launcher.widgets

import android.annotation.SuppressLint
import android.app.Application
import android.appwidget.AppWidgetHost
import android.appwidget.AppWidgetHostView
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProviderInfo
import android.content.Context
import android.os.SystemClock
import android.view.MotionEvent
import android.view.ViewConfiguration
import androidx.core.view.postDelayed
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import link.danb.launcher.R
import javax.inject.Singleton
import kotlin.math.pow
import kotlin.math.sqrt

@Module
@InstallIn(SingletonComponent::class)
class WidgetsModule {

    @Provides
    @Singleton
    fun getAppWidgetManager(application: Application): AppWidgetManager {
        return AppWidgetManager.getInstance(application)
    }

    @Provides
    @Singleton
    fun getAppWidgetHost(application: Application): AppWidgetHost {
        return object : AppWidgetHost(application, R.id.app_widget_host_id) {
            override fun onCreateView(
                context: Context, appWidgetId: Int, appWidget: AppWidgetProviderInfo
            ): AppWidgetHostView = LauncherAppWidgetHostView(context)
        }
    }

    private class LauncherAppWidgetHostView(context: Context) : AppWidgetHostView(context) {

        private val touchSlop: Int by lazy { ViewConfiguration.get(context).scaledTouchSlop }

        private var downEvent: MotionEvent? = null
        private var interceptFutureTouches: Boolean = false

        override fun onInterceptTouchEvent(event: MotionEvent): Boolean {
            handleTouchEvent(event)
            return interceptFutureTouches
        }

        @SuppressLint("ClickableViewAccessibility")
        override fun onTouchEvent(event: MotionEvent): Boolean {
            handleTouchEvent(event)
            return true
        }

        private fun handleTouchEvent(event: MotionEvent) {
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    downEvent = MotionEvent.obtain(event)
                    postDelayed(LONG_PRESS_TIMEOUT) {
                        val finishTime = event.downTime + LONG_PRESS_TIMEOUT
                        if (downEvent != null && SystemClock.uptimeMillis() > finishTime) {
                            performLongClick()
                            interceptFutureTouches = true
                        }
                    }
                }

                MotionEvent.ACTION_MOVE -> {
                    val downEvent = downEvent ?: return
                    if (downEvent.getDistanceFromTouchEvent(event) >= touchSlop) {
                        handler.removeCallbacksAndMessages(null)
                    }
                }

                MotionEvent.ACTION_CANCEL, MotionEvent.ACTION_UP -> {
                    downEvent?.recycle()
                    downEvent = null
                    interceptFutureTouches = false
                }
            }
        }

        companion object {
            private val LONG_PRESS_TIMEOUT = ViewConfiguration.getLongPressTimeout().toLong()

            private fun MotionEvent.getDistanceFromTouchEvent(other: MotionEvent): Int =
                sqrt((rawX - other.rawX).pow(2) + (rawY - other.rawY).pow(2)).toInt()
        }
    }
}
