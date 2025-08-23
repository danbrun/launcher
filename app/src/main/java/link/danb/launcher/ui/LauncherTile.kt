package link.danb.launcher.ui

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.dp

@Composable
@OptIn(ExperimentalFoundationApi::class)
fun LauncherTile(
  icon: @Composable () -> Unit,
  text: @Composable () -> Unit,
  modifier: Modifier = Modifier,
  onClick: () -> Unit,
  onLongClick: () -> Unit,
  interactionSource: MutableInteractionSource? = null,
) {
  val hapticFeedback = LocalHapticFeedback.current
  Row(
    modifier =
      modifier
        .clip(CardDefaults.shape)
        .combinedClickable(
          onClick = { onClick() },
          onLongClick = {
            hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
            onLongClick()
          },
          interactionSource = interactionSource,
        )
        .fillMaxSize()
        .padding(8.dp),
    verticalAlignment = Alignment.CenterVertically,
  ) {
    Box { icon() }

    Spacer(Modifier.width(8.dp))

    CompositionLocalProvider(LocalTextStyle provides MaterialTheme.typography.labelMedium) {
      text()
    }
  }
}
