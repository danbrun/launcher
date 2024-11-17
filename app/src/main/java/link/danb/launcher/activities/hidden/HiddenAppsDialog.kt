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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import link.danb.launcher.R
import link.danb.launcher.apps.rememberAppsLauncher
import link.danb.launcher.components.UserActivity
import link.danb.launcher.profiles.Profile
import link.danb.launcher.ui.BottomSheet
import link.danb.launcher.ui.LauncherIcon
import link.danb.launcher.ui.LauncherTile

@Composable
fun HiddenAppsDialog(
  profile: Profile,
  hiddenAppsViewModel: HiddenAppsViewModel = hiltViewModel(),
  navigateToDetails: (UserActivity) -> Unit,
  dismiss: () -> Unit,
) {
  BottomSheet(isShowing = true, dismiss) { dismiss ->
    val appsLauncher = rememberAppsLauncher()
    val state by remember { hiddenAppsViewModel.getState(profile) }.collectAsStateWithLifecycle()

    HiddenAppsContent(
      state,
      launchActivity = { bounds, userActivity ->
        appsLauncher.startMainActivity(userActivity, bounds)
      },
      navigateToDetails,
    )
  }
}

@Composable
private fun HiddenAppsContent(
  state: HiddenAppsViewModel.State,
  launchActivity: (Rect, UserActivity) -> Unit,
  navigateToDetails: (UserActivity) -> Unit,
) {
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

    when (state) {
      is HiddenAppsViewModel.State.Loading -> {
        item(span = { GridItemSpan(maxLineSpan) }) {
          Box(Modifier.fillMaxWidth().padding(8.dp), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(Modifier.size(64.dp))
          }
        }
      }
      is HiddenAppsViewModel.State.Loaded -> {
        items(items = state.items) { item ->
          Card(Modifier.padding(4.dp)) {
            LauncherTile(
              icon = { isPressed ->
                LauncherIcon(
                  item.launcherTileData.launcherIconData,
                  Modifier.size(dimensionResource(R.dimen.launcher_icon_size)),
                  isPressed = isPressed,
                )
              },
              text = {
                Text(item.launcherTileData.name, maxLines = 2, overflow = TextOverflow.Ellipsis)
              },
              onClick = { launchActivity(it, item.userActivity) },
              onLongClick = { navigateToDetails(item.userActivity) },
            )
          }
        }
      }
    }
  }
}
