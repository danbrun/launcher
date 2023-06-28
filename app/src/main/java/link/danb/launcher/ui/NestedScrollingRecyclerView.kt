package link.danb.launcher.ui

import android.content.Context
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.ViewGroup
import android.widget.ListView
import androidx.core.view.children
import androidx.recyclerview.widget.RecyclerView
import link.danb.launcher.utils.getLocationOnScreen

class NestedScrollingRecyclerView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null
) : RecyclerView(context, attrs) {

    override fun onInterceptTouchEvent(event: MotionEvent): Boolean =
        !isTouchWithinListView(event) && super.onInterceptTouchEvent(event)

    companion object {
        private fun ViewGroup.isTouchWithinListView(motionEvent: MotionEvent): Boolean =
            if (this is ListView) {
                isTouchWithinBounds(motionEvent)
            } else {
                children.any { it is ViewGroup && it.isTouchWithinListView(motionEvent) }
            }

        private fun ViewGroup.isTouchWithinBounds(motionEvent: MotionEvent): Boolean =
            getLocationOnScreen().contains(motionEvent.rawX.toInt(), motionEvent.rawY.toInt())
    }
}
