package link.danb.launcher.utils

import android.graphics.drawable.Drawable

/** Convenience method to apply square bounds to a [Drawable]. */
fun Drawable.applySize(size: Int): Drawable {
    setBounds(0, 0, size, size)
    return this
}
