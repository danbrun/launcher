package link.danb.launcher.ui

import android.graphics.drawable.AdaptiveIconDrawable
import android.graphics.drawable.Drawable
import androidx.compose.foundation.Canvas
import androidx.compose.material3.CardDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import link.danb.launcher.extensions.drawAdaptiveIcon
import link.danb.launcher.extensions.drawDrawable

data class LauncherIconData(val icon: AdaptiveIconDrawable, val badge: Drawable)

@Composable
fun LauncherIcon(
  data: LauncherIconData,
  modifier: Modifier = Modifier,
  insetMultiplier: Float = 1f,
) {
  Canvas(modifier.clip(CardDefaults.shape)) {
    drawAdaptiveIcon(data.icon, insetMultiplier)
    drawDrawable(data.badge)
  }
}
