package link.danb.launcher.ui

import android.annotation.SuppressLint
import android.content.Context
import android.util.AttributeSet
import android.view.MotionEvent
import android.widget.FrameLayout

class TouchInterceptView @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null) :
  FrameLayout(context, attrs) {

  override fun onInterceptTouchEvent(event: MotionEvent): Boolean {
    return true
  }

  @SuppressLint("ClickableViewAccessibility")
  override fun onTouchEvent(event: MotionEvent?): Boolean {
    return false
  }
}
