package link.danb.launcher.ui

import android.graphics.Outline
import android.graphics.Rect
import android.view.View
import android.view.ViewOutlineProvider

class RoundedCornerOutlineProvider(private val radius: Int) : ViewOutlineProvider() {
  override fun getOutline(view: View, outline: Outline) {
    outline.setRoundRect(Rect(0, 0, view.width, view.height), radius.toFloat())
  }
}
