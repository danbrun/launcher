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
        return object : AppWidgetHost(application, R.id.app_widget_host_id) {
            override fun onCreateView(
                context: Context, appWidgetId: Int, appWidget: AppWidgetProviderInfo
            ): AppWidgetHostView = LauncherAppWidgetHostView(context)
        }
    }

    private class LauncherAppWidgetHostView(context: Context) : AppWidgetHostView(context) {

        private var isDown = false
        private var hasLongPressed = false

        override fun onInterceptTouchEvent(event: MotionEvent): Boolean {
            handleTouchEvent(event)
            return hasLongPressed
        }

        @SuppressLint("ClickableViewAccessibility")
        override fun onTouchEvent(event: MotionEvent): Boolean {
            handleTouchEvent(event)
            return true
        }

        private fun handleTouchEvent(event: MotionEvent) {
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    isDown = true
                    hasLongPressed = false
                    postDelayed(LONG_PRESS_TIMEOUT) {
                        val finishTime = event.downTime + LONG_PRESS_TIMEOUT
                        if (isDown && SystemClock.uptimeMillis() > finishTime) {
                            performLongClick()
                            hasLongPressed = true
                        }
                    }
                }
                MotionEvent.ACTION_CANCEL, MotionEvent.ACTION_UP -> {
                    isDown = false
                    hasLongPressed = false
                }
            }
        }

        companion object {
            private val LONG_PRESS_TIMEOUT = ViewConfiguration.getLongPressTimeout().toLong()
        }
    }
}
