package link.danb.launcher.shortcuts

import android.content.Intent
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import link.danb.launcher.R
import link.danb.launcher.apps.componentLabel
import link.danb.launcher.profiles.Profile
import link.danb.launcher.ui.BottomSheet
import link.danb.launcher.ui.LauncherIcon
import link.danb.launcher.ui.LauncherTile

@Composable
fun PinShortcutsDialog(
  profile: Profile,
  pinShortcutsViewModel: PinShortcutsViewModel = hiltViewModel(),
  dismiss: () -> Unit,
) {
  BottomSheet(isShowing = true, dismiss) { dismiss ->
    val state by remember { pinShortcutsViewModel.getState(profile) }.collectAsStateWithLifecycle()

    PinShortcutsContent(state) { pinShortcutsViewModel.acceptPinRequest(it) }
  }
}

@Composable
private fun PinShortcutsContent(
  state: PinShortcutsViewModel.State,
  acceptPinRequest: (Intent) -> Unit,
) {
  val context = LocalContext.current
  val shortcutActivityLauncher =
    rememberLauncherForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) {
      val data = it.data
      if (data != null) {
        acceptPinRequest(data)
        Toast.makeText(context, R.string.pinned_shortcut, Toast.LENGTH_SHORT).show()
      }
    }

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
        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
      )
    }

    when (state) {
      is PinShortcutsViewModel.State.Loading -> {
        item(span = { GridItemSpan(maxLineSpan) }) {
          Box(Modifier.fillMaxWidth().padding(8.dp), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(Modifier.size(64.dp))
          }
        }
      }
      is PinShortcutsViewModel.State.Loaded -> {
        items(items = state.items) { item ->
          Card(Modifier.padding(4.dp)) {
            LauncherTile(
              icon = { isPressed ->
                LauncherIcon(
                  item.userShortcutCreator,
                  Modifier.size(dimensionResource(R.dimen.launcher_icon_size)),
                  isPressed = isPressed,
                )
              },
              text = {
                Text(
                  componentLabel(item.userShortcutCreator) ?: "",
                  maxLines = 2,
                  overflow = TextOverflow.Ellipsis,
                )
              },
              onClick = {
                shortcutActivityLauncher.launch(
                  IntentSenderRequest.Builder(item.creatorIntent).build()
                )
              },
              onLongClick = {},
            )
          }
        }
      }
    }
  }
}
