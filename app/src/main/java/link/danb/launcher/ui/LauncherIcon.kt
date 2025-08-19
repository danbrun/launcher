package link.danb.launcher.ui

import android.graphics.drawable.AdaptiveIconDrawable
import android.graphics.drawable.Drawable
import android.os.Build
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.res.painterResource
import link.danb.launcher.LocalUseMonochromeIcons
import link.danb.launcher.apps.LauncherResourceProvider
import link.danb.launcher.apps.componentIcon
import link.danb.launcher.components.UserComponent
import link.danb.launcher.extensions.drawAdaptiveIcon
import link.danb.launcher.extensions.drawDrawable
import link.danb.launcher.extensions.drawMonochromeIcon

@Composable
fun LauncherIcon(
  userComponent: UserComponent,
  modifier: Modifier = Modifier,
  isPressed: Boolean = false,
) {
  val icon = componentIcon(userComponent)
  Box(modifier.clip(RoundedCornerShape(25))) {
    if (icon == null) return

    val badge =
      remember(userComponent.profile) { LauncherResourceProvider.getBadge(userComponent.profile) }
        ?.let { painterResource(it) }

    val useMonochromeIcons = LocalUseMonochromeIcons.current
    val theme =
      if (useMonochromeIcons) {
        MonochromeIconTheme.theme
      } else {
        MonochromeIconTheme(Color.White, Color.Blue)
      }
    val insetMultiplier by animateFloatAsState(if (isPressed) 0f else 1f, label = "scale")

    Canvas(Modifier.fillMaxSize()) {
      drawLauncherIcon(icon, theme.takeIf { useMonochromeIcons }, insetMultiplier)
    }
    AnimatedContent(badge) { Canvas(Modifier.fillMaxSize()) { drawBadge(it, theme) } }
  }
}

fun DrawScope.drawLauncherIcon(
  icon: AdaptiveIconDrawable,
  monochromeIconTheme: MonochromeIconTheme?,
  insetMultiplier: Float = 1f,
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

fun DrawScope.drawBadge(badge: Painter?, theme: MonochromeIconTheme) {
  if (badge == null) return

  withTransform({ scale(0.4f, 0.4f, Offset(size.width, size.height)) }) {
    drawCircle(theme.foreground)
    withTransform({ scale(0.7f, 0.7f, Offset(size.width / 2, size.height / 2)) }) {
      with(badge) { draw(size, colorFilter = ColorFilter.tint(theme.background)) }
    }
  }
}

fun DrawScope.drawBadge(badge: Drawable?, theme: MonochromeIconTheme) {
  if (badge == null) return

  withTransform({ scale(0.4f, 0.4f, Offset(size.width, size.height)) }) {
    drawCircle(theme.foreground)
    withTransform({ scale(0.7f, 0.7f, Offset(size.width / 2, size.height / 2)) }) {
      drawDrawable(badge.apply { setTint(theme.background.toArgb()) })
    }
  }
}
