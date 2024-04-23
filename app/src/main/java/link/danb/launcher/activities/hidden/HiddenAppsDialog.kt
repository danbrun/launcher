package link.danb.launcher.activities.hidden

import android.view.View
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
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import link.danb.launcher.R
import link.danb.launcher.components.UserActivity
import link.danb.launcher.ui.BottomSheet
import link.danb.launcher.ui.IconTile

@Composable
fun HiddenAppsDialog(
  isShowing: Boolean,
  hiddenApps: HiddenAppsViewModel.HiddenAppsViewData?,
  onClick: (view: View, item: UserActivity) -> Unit,
  onLongClick: (view: View, item: UserActivity) -> Unit,
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
        )
      }

      when (hiddenApps) {
        is HiddenAppsViewModel.HiddenAppsViewData.Loading -> {
          item(span = { GridItemSpan(maxLineSpan) }) {
            Box(Modifier.fillMaxWidth().padding(8.dp), contentAlignment = Alignment.Center) {
              CircularProgressIndicator(Modifier.size(64.dp))
            }
          }
        }
        is HiddenAppsViewModel.HiddenAppsViewData.Loaded -> {
          items(items = hiddenApps.apps) { app ->
            Card(Modifier.padding(4.dp)) {
              IconTile(
                app.icon,
                app.badge,
                app.name,
                onClick = {
                  onClick(it, app.activityData.userActivity)
                  dismiss()
                },
                onLongClick = {
                  onLongClick(it, app.activityData.userActivity)
                  dismiss()
                },
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
