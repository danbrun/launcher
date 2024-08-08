package link.danb.launcher.ui

import android.graphics.drawable.AdaptiveIconDrawable
import android.graphics.drawable.Drawable
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.clipPath
import link.danb.launcher.extensions.drawAdaptiveIcon
import link.danb.launcher.extensions.drawDrawable

data class LauncherIconData(val icon: AdaptiveIconDrawable, val badge: Drawable)

@Composable
fun LauncherIcon(
  data: LauncherIconData,
  modifier: Modifier = Modifier,
  insetMultiplier: Float = 1f,
) {
  Canvas(modifier) { drawLauncherIcon(data, insetMultiplier) }
}

fun DrawScope.drawLauncherIcon(data: LauncherIconData, insetMultiplier: Float = 1f) {
  clipPath(
    Path().apply {
      addRoundRect(RoundRect(Rect(Offset.Zero, size), CornerRadius(size.width * 0.25f)))
    }
  ) {
    drawAdaptiveIcon(data.icon, insetMultiplier)
    drawDrawable(data.badge)
  }
}
