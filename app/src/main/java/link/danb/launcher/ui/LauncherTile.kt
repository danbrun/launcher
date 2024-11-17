package link.danb.launcher.ui

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.dp

data class LauncherTileData(val launcherIconData: LauncherIconData, val name: String)

@Composable
@OptIn(ExperimentalFoundationApi::class)
fun LauncherTile(
  icon: @Composable (isPressed: Boolean) -> Unit,
  text: @Composable () -> Unit,
  modifier: Modifier = Modifier,
  onClick: (Offset) -> Unit,
  onLongClick: (Offset) -> Unit,
) {
  val hapticFeedback = LocalHapticFeedback.current
  var isPressed by remember { mutableStateOf(false) }
  var offset by remember { mutableStateOf(Offset.Zero) }

  Row(
    modifier =
      modifier
        .clip(CardDefaults.shape)
        .combinedClickable(
          onClick = { onClick(offset) },
          onLongClick = {
            hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
            onLongClick(offset)
          },
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
        .padding(8.dp),
    verticalAlignment = Alignment.CenterVertically,
  ) {
    Box(Modifier.onGloballyPositioned { offset = it.positionInRoot() }) { icon(isPressed) }

    Spacer(Modifier.width(8.dp))

    CompositionLocalProvider(LocalTextStyle provides MaterialTheme.typography.labelMedium) {
      text()
    }
  }
}
