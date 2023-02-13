package link.danb.launcher.utils

import android.app.ActivityOptions
import android.content.Intent
import android.graphics.Rect
import android.os.Bundle
import android.view.View

/** Gets the location of the [View] on screen as a [Rect]. */
fun View.getLocationOnScreen(): Rect {
    val pos = IntArray(2).apply { getLocationOnScreen(this) }
    return Rect(pos[0], pos[1], pos[0] + width, pos[1] + height)
}

/** Creates clip reveal animation [Intent] options for the [View]. */
fun View.makeClipRevealAnimation(): Bundle {
    return ActivityOptions.makeClipRevealAnimation(this, 0, 0, width, height).toBundle()
}
