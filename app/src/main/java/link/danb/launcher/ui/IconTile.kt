package link.danb.launcher.ui

import android.graphics.drawable.AdaptiveIconDrawable
import android.graphics.drawable.Drawable
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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import link.danb.launcher.R
import link.danb.launcher.icons.LauncherIcon

data class IconTileViewData(val icon: AdaptiveIconDrawable, val badge: Drawable, val name: String)

@Composable
@OptIn(ExperimentalFoundationApi::class)
fun IconTile(
  data: IconTileViewData,
  style: TextStyle = MaterialTheme.typography.labelLarge,
  onClick: (Offset) -> Unit,
  onLongClick: (Offset) -> Unit,
) {
  var isPressed by remember { mutableStateOf(false) }
  var offset by remember { mutableStateOf(Offset.Zero) }
  val insetMultiplier by animateFloatAsState(if (isPressed) 0f else 1f, label = "scale")

  Row(
    modifier =
      Modifier.clip(CardDefaults.shape)
        .combinedClickable(onClick = { onClick(offset) }, onLongClick = { onLongClick(offset) })
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
        .padding(8.dp),
    verticalAlignment = Alignment.CenterVertically,
  ) {
    LauncherIcon(
      data.icon,
      data.badge,
      Modifier.size(dimensionResource(R.dimen.launcher_icon_size)).onGloballyPositioned {
        offset = it.positionInRoot()
      },
      insetMultiplier,
    )

    Text(
      data.name,
      maxLines = 2,
      overflow = TextOverflow.Ellipsis,
      modifier = Modifier.padding(start = 8.dp),
      style = style,
    )
  }
}
