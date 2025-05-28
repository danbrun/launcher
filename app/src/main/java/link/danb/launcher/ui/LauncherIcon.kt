package link.danb.launcher.ui

import android.graphics.drawable.AdaptiveIconDrawable
import android.graphics.drawable.Drawable
import android.os.Build
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.clipPath
import link.danb.launcher.extensions.drawAdaptiveIcon
import link.danb.launcher.extensions.drawDrawable
import link.danb.launcher.extensions.drawMonochromeIcon
import link.danb.launcher.profiles.Profile
import link.danb.launcher.ui.theme.LocalIconTheme

data class LauncherIconData(
  val icon: AdaptiveIconDrawable,
  val profile: Profile,
  val badge: Drawable,
)

@Composable
fun LauncherIcon(
  data: LauncherIconData,
  modifier: Modifier = Modifier,
  isPressed: Boolean = false,
) {
  val monochromeIconTheme = LocalIconTheme.current
  val insetMultiplier by animateFloatAsState(if (isPressed) 0f else 1f, label = "scale")
  Canvas(modifier) { drawLauncherIcon(data.icon, monochromeIconTheme, insetMultiplier) }
  AnimatedContent(data, contentKey = { it.profile }) { Canvas(modifier) { drawDrawable(it.badge) } }
}

fun DrawScope.drawLauncherIcon(
  icon: AdaptiveIconDrawable,
  monochromeIconTheme: MonochromeIconTheme?,
  insetMultiplier: Float = 1f,
) {
  clipPath(
    Path().apply {
      addRoundRect(RoundRect(Rect(Offset.Zero, size), CornerRadius(size.width * 0.25f)))
    }
  ) {
    if (monochromeIconTheme != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
      if (icon.monochrome != null) {
        drawMonochromeIcon(icon, monochromeIconTheme, insetMultiplier)
      } else {
        drawAdaptiveIcon(icon, insetMultiplier)
        drawRect(monochromeIconTheme.background, Offset.Zero, size, blendMode = BlendMode.Color)
      }
    } else {
      drawAdaptiveIcon(icon, insetMultiplier)
    }
  }
}
