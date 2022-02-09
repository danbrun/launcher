package link.danb.launcher

import android.app.ActivityOptions
import android.graphics.Rect
import android.os.Bundle
import android.view.View

fun View.getLocationOnScreen(): Rect {
    val pos = IntArray(2).apply { getLocationOnScreen(this) }
    return Rect(pos[0], pos[1], width, height)
}

fun View.makeClipRevealAnimation(): Bundle {
    return ActivityOptions.makeClipRevealAnimation(this, 0, 0, width, height).toBundle()
}
