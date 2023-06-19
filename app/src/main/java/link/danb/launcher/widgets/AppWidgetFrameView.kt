package link.danb.launcher.widgets

import android.annotation.SuppressLint
import android.appwidget.AppWidgetHostView
import android.content.Context
import android.util.AttributeSet
import android.view.GestureDetector
import android.view.GestureDetector.SimpleOnGestureListener
import android.view.MotionEvent
import android.widget.FrameLayout
import dagger.hilt.android.AndroidEntryPoint
import link.danb.launcher.R
import link.danb.launcher.model.WidgetMetadata
import javax.inject.Inject

@AndroidEntryPoint
class AppWidgetFrameView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null
) : FrameLayout(context, attrs) {

    @Inject
    lateinit var appWidgetViewProvider: AppWidgetViewProvider

    private val gestureDetector = GestureDetector(context, object : SimpleOnGestureListener() {
        override fun onDown(e: MotionEvent): Boolean {
            hasLongPressed = false
            return true
        }

        override fun onLongPress(e: MotionEvent) {
            hasLongPressed = true
            performLongClick()
        }
    })

    private var appWidgetHostView: AppWidgetHostView? = null
    private var hasLongPressed = false

    var widgetMetadata: WidgetMetadata? = null
        set(value) {
            val old = field
            value!!

            if (old == null || old.widgetId != value.widgetId) {
                removeAllViews()
                appWidgetHostView = appWidgetViewProvider.createView(value.widgetId)
                addView(appWidgetHostView)
            }

            if (old == null || old.height != value.height) {
                layoutParams = layoutParams.apply {
                    val heightMultiplier =
                        context.resources.getDimensionPixelSize(R.dimen.widget_height_multiplier)
                    height = value.height * heightMultiplier
                }
            }

            field = value
        }

    override fun onInterceptTouchEvent(event: MotionEvent): Boolean {
        gestureDetector.onTouchEvent(event)
        return hasLongPressed
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {
        gestureDetector.onTouchEvent(event)
        return true
    }
}
