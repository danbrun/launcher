package link.danb.launcher.utils

import android.app.ActivityOptions
import android.graphics.Rect
import android.os.Bundle
import android.view.View

/** Gets the location of the view on screen as a rect. */
fun View.getLocationOnScreen(): Rect {
    val pos = IntArray(2).apply { getLocationOnScreen(this) }
    return Rect(pos[0], pos[1], width, height)
}

/** Creates a clip reveal animation for the view. */
fun View.makeClipRevealAnimation(): Bundle {
    return ActivityOptions.makeClipRevealAnimation(this, 0, 0, width, height).toBundle()
}
