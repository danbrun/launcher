package link.danb.launcher.icons

import android.graphics.drawable.AdaptiveIconDrawable
import android.graphics.drawable.Drawable
import androidx.compose.foundation.layout.Box
import androidx.compose.material3.CardDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import link.danb.launcher.extensions.drawAdaptiveIcon
import link.danb.launcher.extensions.drawDrawable

@Composable
fun LauncherIcon(
  icon: AdaptiveIconDrawable,
  badge: Drawable,
  modifier: Modifier = Modifier,
  insetMultiplier: Float = 1f,
  content: @Composable () -> Unit = {},
) {
  Box(
    modifier.clip(CardDefaults.shape).drawWithContent {
      drawAdaptiveIcon(icon, insetMultiplier)
      drawDrawable(badge)
    }
  ) {
    content()
  }
}
