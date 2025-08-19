package link.danb.launcher.ui

import androidx.activity.compose.PredictiveBackHandler
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp
import kotlin.coroutines.cancellation.CancellationException

@Composable
fun Modifier.predictiveBackScaling(shrinkSize: Dp): Modifier {
  val predictiveBackProgress = remember { Animatable(0f) }
  val predictiveBackEasing = CubicBezierEasing(0.1f, 0.1f, 0f, 1f)
  PredictiveBackHandler { progress ->
    try {
      progress.collect { predictiveBackProgress.snapTo(it.progress) }
      predictiveBackProgress.animateTo(0f)
    } catch (_: CancellationException) {
      predictiveBackProgress.animateTo(0f)
    }
  }
  return graphicsLayer {
    val multiplier = predictiveBackEasing.transform(predictiveBackProgress.value)
    val scale = 1 - (multiplier * shrinkSize.toPx() / size.width)
    scaleX = scale
    scaleY = scale
  }
}
