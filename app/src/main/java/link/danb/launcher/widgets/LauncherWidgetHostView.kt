package link.danb.launcher.widgets

import android.annotation.SuppressLint
import android.appwidget.AppWidgetHostView
import android.content.Context
import android.os.SystemClock
import android.view.MotionEvent
import android.view.ViewConfiguration
import androidx.core.view.postDelayed

/** Custom [AppWidgetHostView] that intercepts long presses. */
class LauncherWidgetHostView(context: Context?) : AppWidgetHostView(context) {

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
                requestDisallowInterceptTouchEvent(false)
            }
        }
    }

    companion object {
        private val LONG_PRESS_TIMEOUT = ViewConfiguration.getLongPressTimeout().toLong()
    }
}
