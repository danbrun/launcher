package link.danb.launcher.ui

import android.graphics.drawable.AdaptiveIconDrawable
import android.graphics.drawable.Drawable
import android.os.Build
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.clipPath
import link.danb.launcher.LocalUseMonochromeIcons
import link.danb.launcher.extensions.drawAdaptiveIcon
import link.danb.launcher.extensions.drawDrawable
import link.danb.launcher.extensions.drawMonochromeIcon

data class LauncherIconData(val icon: AdaptiveIconDrawable, val badge: Drawable)

@Composable
fun LauncherIcon(
  data: LauncherIconData,
  modifier: Modifier = Modifier,
  insetMultiplier: Float = 1f,
  monochromeIconTheme: MonochromeIconTheme = MonochromeIconTheme.theme,
  useMonochromeIcons: Boolean = LocalUseMonochromeIcons.current,
) {
  Canvas(modifier) {
    drawLauncherIcon(data, monochromeIconTheme, insetMultiplier, useMonochromeIcons)
  }
}

fun DrawScope.drawLauncherIcon(
  data: LauncherIconData,
  monochromeIconTheme: MonochromeIconTheme,
  insetMultiplier: Float = 1f,
  useMonochromeIcons: Boolean = false,
) {
  clipPath(
    Path().apply {
      addRoundRect(RoundRect(Rect(Offset.Zero, size), CornerRadius(size.width * 0.25f)))
    }
  ) {
    if (useMonochromeIcons && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
      if (data.icon.monochrome != null) {
        drawMonochromeIcon(data.icon, monochromeIconTheme, insetMultiplier)
      } else {
        drawAdaptiveIcon(data.icon, insetMultiplier)
        drawRect(monochromeIconTheme.background, Offset.Zero, size, blendMode = BlendMode.Color)
      }
    } else {
      drawAdaptiveIcon(data.icon, insetMultiplier)
    }
    drawDrawable(data.badge)
  }
}
