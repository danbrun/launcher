package link.danb.launcher.extensions

import android.app.ActivityOptions
import android.content.Intent
import android.graphics.Point
import android.graphics.Rect
import android.view.View

/** Gets the location of the [View] on screen as a [Point]. */
val View.locationOnScreen: Point
  get() =
    IntArray(2).let {
      getLocationOnScreen(it)
      Point(it[0], it[1])
    }

/** Gets the bounds of the [View] on screen as a [Rect]. */
val View.boundsOnScreen: Rect
  get() = locationOnScreen.let { Rect(it.x, it.y, it.x + width, it.y + height) }

/** Creates clip reveal animation [Intent] options for the [View]. */
fun View.makeScaleUpAnimation(): ActivityOptions =
  ActivityOptions.makeScaleUpAnimation(this, 0, 0, width, height)
