package link.danb.launcher.ui

import android.content.Context
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.ListView
import androidx.core.view.children
import androidx.recyclerview.widget.RecyclerView
import link.danb.launcher.extensions.isTouchWithinBounds

class NestedScrollingRecyclerView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null
) : RecyclerView(context, attrs) {

    override fun onInterceptTouchEvent(event: MotionEvent): Boolean =
        !isTouchWithinListView(event) && super.onInterceptTouchEvent(event)

    companion object {
        private fun View.isTouchWithinListView(motionEvent: MotionEvent): Boolean = when (this) {
            is ListView -> isTouchWithinBounds(motionEvent)
            is ViewGroup -> children.any { it.isTouchWithinListView(motionEvent) }
            else -> false
        }
    }
}
