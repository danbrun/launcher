package link.danb.launcher.ui

import android.graphics.drawable.AdaptiveIconDrawable
import android.graphics.drawable.Drawable
import android.os.Build
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Indication
import androidx.compose.foundation.IndicationNodeFactory
import androidx.compose.foundation.indication
import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawscope.ContentDrawScope
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.node.DelegatableNode
import androidx.compose.ui.node.DrawModifierNode
import androidx.compose.ui.res.painterResource
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
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
  shape: Shape = RoundedCornerShape(25),
  interactionSource: InteractionSource = remember { MutableInteractionSource() },
  indication: Indication = LauncherIconIndication,
) {
  val icon = componentIcon(userComponent)
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

  Box(
    modifier.clip(shape).indication(interactionSource, indication).drawBehind {
      drawLauncherIcon(icon, theme.takeIf { useMonochromeIcons })
    }
  ) {
    AnimatedContent(badge, Modifier.matchParentSize()) {
      if (it != null) {
        Canvas(Modifier.fillMaxSize()) { drawBadge(it, theme) }
      }
    }
  }
}

fun DrawScope.drawLauncherIcon(
  icon: AdaptiveIconDrawable,
  monochromeIconTheme: MonochromeIconTheme?,
) {
  if (monochromeIconTheme != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
    if (icon.monochrome != null) {
      drawMonochromeIcon(icon, monochromeIconTheme)
    } else {
      drawAdaptiveIcon(icon)
      drawRect(monochromeIconTheme.background, Offset.Zero, size, blendMode = BlendMode.Color)
    }
  } else {
    drawAdaptiveIcon(icon)
  }
}

fun DrawScope.drawBadge(badge: Painter, theme: MonochromeIconTheme) {
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

private class LauncherIconIndicationNode(private val interactionSource: InteractionSource) :
  Modifier.Node(), DrawModifierNode {

  private val animatableScale = Animatable(1f)

  override fun onAttach() {
    coroutineScope.launch {
      interactionSource.interactions.collectLatest { interaction ->
        when (interaction) {
          is PressInteraction.Press -> animatableScale.animateTo(pressedScale)
          is PressInteraction.Release,
          is PressInteraction.Cancel -> animatableScale.animateTo(1f)
        }
      }
    }
  }

  override fun ContentDrawScope.draw() {
    scale(animatableScale.value) { this@draw.drawContent() }
  }

  companion object {
    private val pressedScale = 1 / (2 * AdaptiveIconDrawable.getExtraInsetFraction() + 1)
  }
}

data object LauncherIconIndication : IndicationNodeFactory {
  override fun create(interactionSource: InteractionSource): DelegatableNode =
    LauncherIconIndicationNode(interactionSource)
}
