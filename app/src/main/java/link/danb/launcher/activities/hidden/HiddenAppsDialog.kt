package link.danb.launcher.activities.hidden

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import link.danb.launcher.R
import link.danb.launcher.components.UserActivity
import link.danb.launcher.ui.BottomSheet
import link.danb.launcher.ui.LauncherTile

@Composable
fun HiddenAppsDialog(
  isShowing: Boolean,
  viewData: HiddenAppsViewModel.HiddenAppsViewData,
  onClick: (Offset, UserActivity) -> Unit,
  onLongClick: (Offset, UserActivity) -> Unit,
  onDismissRequest: () -> Unit,
) {
  BottomSheet(isShowing, onDismissRequest) { dismiss ->
    LazyVerticalGrid(GridCells.Adaptive(dimensionResource(R.dimen.min_column_width))) {
      item(span = { GridItemSpan(maxLineSpan) }) {
        ListItem(
          headlineContent = {
            Text(
              stringResource(R.string.hidden_apps),
              style = MaterialTheme.typography.headlineMedium,
            )
          },
          leadingContent = {
            Icon(
              painter = painterResource(R.drawable.ic_baseline_visibility_24),
              contentDescription = null,
            )
          },
          colors = ListItemDefaults.colors(containerColor = Color.Transparent),
        )
      }

      when (viewData) {
        is HiddenAppsViewModel.HiddenAppsViewData.Loading -> {
          item(span = { GridItemSpan(maxLineSpan) }) {
            Box(Modifier.fillMaxWidth().padding(8.dp), contentAlignment = Alignment.Center) {
              CircularProgressIndicator(Modifier.size(64.dp))
            }
          }
        }
        is HiddenAppsViewModel.HiddenAppsViewData.Loaded -> {
          items(items = viewData.apps) { app ->
            Card(Modifier.padding(4.dp)) {
              LauncherTile(
                app.launcherTileData,
                onClick = {
                  onClick(it, app.userActivity)
                  dismiss()
                },
                onLongClick = { onLongClick(it, app.userActivity) },
              )
            }
          }
        }
      }
    }
  }
}
