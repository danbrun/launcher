package link.danb.launcher.extensions

import android.app.ActivityOptions
import android.content.Intent
import android.graphics.Point
import android.graphics.Rect
import android.os.Bundle
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.ViewGroup.LayoutParams

/** Gets the location of the [View] on screen as a [Point]. */
fun View.getLocationOnScreen(): Point =
  IntArray(2).let {
    getLocationOnScreen(it)
    Point(it[0], it[1])
  }

/** Gets the bounds of the [View] on screen as a [Rect]. */
fun View.getBoundsOnScreen(): Rect =
  getLocationOnScreen().let { Rect(it.x, it.y, it.x + width, it.y + height) }

/** Checks if the coordinates of the given [MotionEvent] are within the bounds of the [View]. */
fun View.isTouchWithinBounds(motionEvent: MotionEvent): Boolean =
  getBoundsOnScreen().contains(motionEvent.rawX.toInt(), motionEvent.rawY.toInt())

/** Creates clip reveal animation [Intent] options for the [View]. */
fun View.makeClipRevealAnimation(): Bundle =
  ActivityOptions.makeClipRevealAnimation(this, 0, 0, width, height).toBundle()

/** Updates the [LayoutParams] of the view to force specific dimensions. */
fun View.setLayoutSize(width: Int? = null, height: Int? = null) {
  layoutParams =
    layoutParams.apply {
      this.width = width ?: this.width
      this.height = height ?: this.height
    }
}

/** Removes the view from its parent if it is currently attached to a parent view. */
fun View.removeFromParent() {
  (parent as ViewGroup?)?.removeView(this)
}
