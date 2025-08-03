package link.danb.launcher.activities.details

import androidx.compose.foundation.layout.size
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import link.danb.launcher.R
import link.danb.launcher.apps.rememberAppsLauncher
import link.danb.launcher.components.UserActivity
import link.danb.launcher.ui.LauncherIcon

@Composable
fun DetailsDialog(
  userActivity: UserActivity,
  expanded: Boolean,
  activityDetailsViewModel: ActivityDetailsViewModel = hiltViewModel(),
  onDismiss: () -> Unit,
) {
  val state =
    remember { activityDetailsViewModel.getActivityDetails(userActivity) }
      .collectAsStateWithLifecycle()
      .value
  when (state) {
    is ActivityDetailsViewModel.Loaded -> {
      DropdownMenu(expanded, onDismissRequest = { onDismiss() }) {
        PinMenuItem(state.activityData.isPinned) {
          activityDetailsViewModel.toggleAppPinned(state.activityData)
          onDismiss()
        }

        HideMenuItem(state.activityData.isHidden) {
          activityDetailsViewModel.toggleAppHidden(state.activityData)
          onDismiss()
        }

        val context = LocalContext.current
        UninstallMenuItem {
          context.startActivity(activityDetailsViewModel.getUninstallIntent(userActivity))
          onDismiss()
        }

        val appsLauncher = rememberAppsLauncher()
        SettingsMenuItem {
          appsLauncher.startAppDetailsActivity(userActivity, it)
          onDismiss()
        }

        for (shortcut in state.shortcuts) {
          ShortcutMenuItem(shortcut) {
            appsLauncher.startShortcut(shortcut.userShortcut, Rect.Zero)
          }
        }
      }
    }
    is ActivityDetailsViewModel.Loading -> {}
    is ActivityDetailsViewModel.Missing -> {
      LaunchedEffect(Unit) { onDismiss() }
    }
  }
}

@Composable
private fun PinMenuItem(isPinned: Boolean, onClick: () -> Unit) {
  DropdownMenuItem(
    text = { Text(stringResource(if (isPinned) R.string.unpin_app else R.string.pin_app)) },
    onClick = onClick,
    leadingIcon = {
      Icon(
        painter =
          painterResource(
            if (isPinned) R.drawable.baseline_push_pin_off_24 else R.drawable.baseline_push_pin_24
          ),
        contentDescription = null,
      )
    },
  )
}

@Composable
private fun HideMenuItem(isHidden: Boolean, onClick: () -> Unit) {
  DropdownMenuItem(
    text = { Text(stringResource(if (isHidden) R.string.show_app else R.string.hide_app)) },
    onClick = onClick,
    leadingIcon = {
      Icon(
        painter =
          painterResource(
            if (isHidden) R.drawable.ic_baseline_visibility_24
            else R.drawable.ic_baseline_visibility_off_24
          ),
        contentDescription = null,
      )
    },
  )
}

@Composable
private fun UninstallMenuItem(onClick: () -> Unit) {
  DropdownMenuItem(
    text = { Text(stringResource(R.string.uninstall)) },
    onClick = onClick,
    leadingIcon = {
      Icon(
        painter = painterResource(R.drawable.ic_baseline_delete_forever_24),
        contentDescription = null,
      )
    },
  )
}

@Composable
private fun SettingsMenuItem(onClick: (Rect) -> Unit) {
  var bounds by remember { mutableStateOf(Rect.Zero) }
  DropdownMenuItem(
    text = { Text(stringResource(R.string.settings)) },
    leadingIcon = {
      Icon(painter = painterResource(R.drawable.ic_baseline_settings_24), contentDescription = null)
    },
    onClick = { onClick(bounds) },
    modifier = Modifier.onGloballyPositioned { bounds = it.boundsInRoot() },
  )
}

@Composable
private fun ShortcutMenuItem(data: ActivityDetailsViewModel.ShortcutViewData, onClick: () -> Unit) {
  DropdownMenuItem(
    text = { Text(data.launcherTileData.name) },
    onClick = onClick,
    leadingIcon = { LauncherIcon(data.launcherTileData.launcherIconData, Modifier.size(24.dp)) },
  )
}
