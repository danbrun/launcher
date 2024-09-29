package link.danb.launcher.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.FilledIconToggleButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Composable
fun <T> FilledIconSelector(
  items: List<T>,
  selected: T,
  isChecked: Boolean,
  onClick: (T) -> Unit,
  icon: @Composable (T) -> Unit,
) {
  Box {
    Box(
      Modifier.matchParentSize()
        .padding(4.dp)
        .clip(RoundedCornerShape(50))
        .background(MaterialTheme.colorScheme.surfaceContainer)
    )

    Row {
      for (item in items) {
        AnimatedVisibility(
          isChecked || item == selected,
          enter = expandHorizontally(),
          exit = shrinkHorizontally(),
        ) {
          FilledIconToggleButton(
            isChecked && item == selected,
            { onClick(item) },
            colors =
              IconButtonDefaults.filledIconToggleButtonColors().let {
                if (item != selected) {
                  it.copy(containerColor = Color.Transparent)
                } else {
                  it
                }
              },
          ) {
            icon(item)
          }
        }
      }
    }
  }
}
