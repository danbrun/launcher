package link.danb.launcher.shortcuts

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import link.danb.launcher.R
import link.danb.launcher.components.UserShortcutCreator
import link.danb.launcher.ui.BottomSheet
import link.danb.launcher.ui.LauncherTile

@Composable
fun PinShortcutsDialog(
  isShowing: Boolean,
  viewData: PinShortcutsViewModel.PinShortcutsViewData?,
  onClick: (Offset, UserShortcutCreator) -> Unit,
  onDismissRequest: () -> Unit,
) {
  BottomSheet(isShowing, onDismissRequest) { dismiss ->
    LazyVerticalGrid(GridCells.Adaptive(dimensionResource(R.dimen.min_column_width))) {
      item(span = { GridItemSpan(maxLineSpan) }) {
        ListItem(
          headlineContent = {
            Text(
              stringResource(R.string.pin_shortcut),
              style = MaterialTheme.typography.headlineMedium,
            )
          },
          leadingContent = {
            Icon(
              painter = painterResource(R.drawable.baseline_shortcut_24),
              contentDescription = null,
            )
          },
        )
      }

      when (viewData) {
        is PinShortcutsViewModel.PinShortcutsViewData.Loading -> {
          item(span = { GridItemSpan(maxLineSpan) }) {
            Box(Modifier.fillMaxWidth().padding(8.dp), contentAlignment = Alignment.Center) {
              CircularProgressIndicator(Modifier.size(64.dp))
            }
          }
        }
        is PinShortcutsViewModel.PinShortcutsViewData.Loaded -> {
          items(items = viewData.shortcutCreators) { shortcutCreator ->
            Card(Modifier.padding(4.dp)) {
              LauncherTile(
                shortcutCreator.launcherTileData,
                onClick = {
                  onClick(it, shortcutCreator.userShortcutCreator)
                  dismiss()
                },
                onLongClick = {},
              )
            }
          }
        }
        null -> {}
      }

      item(span = { GridItemSpan(maxLineSpan) }) { Spacer(Modifier.safeDrawingPadding()) }
    }
  }
}
