package link.danb.launcher.ui

import android.graphics.drawable.AdaptiveIconDrawable
import android.graphics.drawable.Drawable
import android.view.View
import android.widget.FrameLayout
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import link.danb.launcher.R
import link.danb.launcher.icons.LauncherIcon

@Composable
@OptIn(ExperimentalFoundationApi::class)
fun IconTile(
  icon: AdaptiveIconDrawable,
  badge: Drawable,
  name: String,
  onClick: (View) -> Unit,
  onLongClick: (View) -> Unit,
) {
  var view: View? by remember { mutableStateOf(null) }
  var isPressed by remember { mutableStateOf(false) }
  val insetMultiplier by animateFloatAsState(if (isPressed) 0f else 1f, label = "scale")

  Row(
    modifier =
      Modifier.combinedClickable(
          onClick = { onClick(checkNotNull(view)) },
          onLongClick = { onLongClick(checkNotNull(view)) },
        )
        .pointerInput(isPressed) {
          awaitPointerEventScope {
            isPressed =
              if (isPressed) {
                waitForUpOrCancellation()
                false
              } else {
                awaitFirstDown(false)
                true
              }
          }
        }
        .fillMaxSize()
        .clip(CardDefaults.shape)
        .padding(8.dp),
    verticalAlignment = Alignment.CenterVertically,
  ) {
    LauncherIcon(
      icon,
      badge,
      Modifier.size(dimensionResource(R.dimen.launcher_icon_size)),
      insetMultiplier,
    ) {
      AndroidView(
        factory = { FrameLayout(it).apply { view = this } },
        modifier = Modifier.fillMaxSize(),
        onReset = { view = it },
        onRelease = { view = null },
      )
    }

    Text(
      name,
      maxLines = 2,
      overflow = TextOverflow.Ellipsis,
      modifier = Modifier.padding(start = 8.dp),
    )
  }
}
