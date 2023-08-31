package link.danb.launcher.extensions

import android.graphics.drawable.Drawable

/** Convenience method to apply square bounds to a [Drawable]. */
fun Drawable.setSize(size: Int) {
  setBounds(0, 0, size, size)
}
