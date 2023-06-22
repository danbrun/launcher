package link.danb.launcher.widgets

import android.annotation.SuppressLint
import android.appwidget.AppWidgetHostView
import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.res.Resources
import android.util.AttributeSet
import android.view.GestureDetector
import android.view.GestureDetector.SimpleOnGestureListener
import android.view.MotionEvent
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ListView
import androidx.core.view.children
import dagger.hilt.android.AndroidEntryPoint
import link.danb.launcher.model.WidgetMetadata
import link.danb.launcher.utils.updateAppWidgetSize
import javax.inject.Inject

@AndroidEntryPoint
class AppWidgetFrameView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null
) : FrameLayout(context, attrs) {

    @Inject
    lateinit var appWidgetViewProvider: AppWidgetViewProvider

    @Inject
    lateinit var appWidgetManager: AppWidgetManager

    private val gestureDetector = GestureDetector(context, object : SimpleOnGestureListener() {
        override fun onDown(e: MotionEvent): Boolean {
            if (containsListView) {
                parent.requestDisallowInterceptTouchEvent(true)
            }
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
            field = value!!

            removeAllViews()
            appWidgetHostView = appWidgetViewProvider.getView(value.widgetId).apply {
                (parent as ViewGroup?)?.removeAllViews()
                updateAppWidgetSize(
                    Resources.getSystem().displayMetrics.widthPixels, value.height
                )
            }
            layoutParams = layoutParams.apply {
                height = value.height
            }
            addView(appWidgetHostView)
        }

    override fun onInterceptTouchEvent(event: MotionEvent): Boolean {
        gestureDetector.onTouchEvent(event)
        if (event.action == MotionEvent.ACTION_UP) {
            parent.requestDisallowInterceptTouchEvent(false)
        }
        return hasLongPressed
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {
        gestureDetector.onTouchEvent(event)
        return true
    }

    private val ViewGroup.containsListView: Boolean
        get() = this is ListView || children.any { it is ViewGroup && it.containsListView }
}
