package link.danb.launcher.extensions

import android.graphics.drawable.AdaptiveIconDrawable
import android.graphics.drawable.Drawable
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.drawscope.inset
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import link.danb.launcher.ui.MonochromeIconTheme

/** Draws a drawable into a Compose DrawScope. Defaults to filling the available space. */
fun DrawScope.drawDrawable(
  drawable: Drawable,
  offset: Offset = Offset.Zero,
  size: Size = this.size,
) {
  drawIntoCanvas {
    drawable.setBounds(offset.x.toInt(), offset.y.toInt(), size.width.toInt(), size.height.toInt())
    drawable.draw(it.nativeCanvas)
  }
}

/**
 * Draws an adaptive icon into a Compose DrawScope. Applies an inset to account for extra space
 * around adaptive icon drawables.
 */
fun DrawScope.drawAdaptiveIcon(icon: AdaptiveIconDrawable, insetMultiplier: Float = 1f) {
  val insetScale = AdaptiveIconDrawable.getExtraInsetFraction() * insetMultiplier * -1
  inset(size.width * insetScale, size.height * insetScale) {
    drawDrawable(icon.background)
    drawDrawable(icon.foreground)
  }
}

@RequiresApi(Build.VERSION_CODES.TIRAMISU)
fun DrawScope.drawMonochromeIcon(
  icon: AdaptiveIconDrawable,
  monochromeIconTheme: MonochromeIconTheme,
  insetMultiplier: Float = 1f,
) {
  val insetScale = AdaptiveIconDrawable.getExtraInsetFraction() * insetMultiplier * -1
  inset(size.width * insetScale, size.height * insetScale) {
    drawRect(monochromeIconTheme.background, Offset.Zero, size)
    icon.monochrome!!.setTint(monochromeIconTheme.foreground.toArgb())
    drawDrawable(icon.monochrome!!)
  }
}
