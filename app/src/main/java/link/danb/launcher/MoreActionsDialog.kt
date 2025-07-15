package link.danb.launcher

import android.content.Intent
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import link.danb.launcher.profiles.Profile

@Composable
fun MoreActionsMenu(
  isShowing: Boolean,
  profile: Profile,
  moreActionsViewModel: MoreActionsViewModel = hiltViewModel(),
  pinShortcuts: () -> Unit,
  pinWidgets: () -> Unit,
  shownHiddenApps: () -> Unit,
  dismiss: () -> Unit,
) {
  val canPinItems by
    remember(profile) { moreActionsViewModel.getCanPinItems(profile) }.collectAsStateWithLifecycle()
  val hasHiddenApps by
    remember(profile) { moreActionsViewModel.getHasHiddenApps(profile) }
      .collectAsStateWithLifecycle()

  if (!isShowing) return

  DropdownMenu(expanded = true, onDismissRequest = dismiss) {
    AnimatedVisibility(visible = canPinItems) {
      Column {
        DropdownMenuItem(
          text = { Text(stringResource(R.string.pin_shortcut)) },
          leadingIcon = {
            Icon(painterResource(R.drawable.baseline_shortcut_24), contentDescription = null)
          },
          onClick = {
            pinShortcuts()
            dismiss()
          },
        )

        DropdownMenuItem(
          text = { Text(stringResource(R.string.pin_widget)) },
          leadingIcon = {
            Icon(painterResource(R.drawable.ic_baseline_widgets_24), contentDescription = null)
          },
          onClick = {
            pinWidgets()
            dismiss()
          },
        )
      }
    }

    AnimatedVisibility(visible = hasHiddenApps) {
      DropdownMenuItem(
        text = { Text(stringResource(R.string.show_hidden)) },
        leadingIcon = {
          Icon(painterResource(R.drawable.ic_baseline_visibility_24), contentDescription = null)
        },
        onClick = {
          shownHiddenApps()
          dismiss()
        },
      )
    }

    DropdownMenuItem(
      text = { Text(stringResource(R.string.monochrome_icons)) },
      leadingIcon = {
        Icon(painterResource(R.drawable.baseline_style_24), contentDescription = null)
      },
      onClick = { moreActionsViewModel.toggleMonochromeIcons() },
      trailingIcon = {
        Switch(
          checked = moreActionsViewModel.monochromeIcons.collectAsStateWithLifecycle(false).value,
          onCheckedChange = { moreActionsViewModel.toggleMonochromeIcons() },
        )
      },
    )

    val canRequestHomeRole by moreActionsViewModel.canRequestHomeRole.collectAsStateWithLifecycle()
    AnimatedVisibility(visible = canRequestHomeRole) {
      val context = LocalContext.current
      val setHomeActivityResultLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) {
          if (moreActionsViewModel.shouldLaunchDefaultAppsSettings) {
            context.startActivity(Intent(Settings.ACTION_MANAGE_DEFAULT_APPS_SETTINGS))
          }
        }
      DropdownMenuItem(
        text = { Text(stringResource(R.string.request_home_role)) },
        leadingIcon = {
          Icon(painterResource(R.drawable.baseline_home_filled_24), contentDescription = null)
        },
        onClick = { setHomeActivityResultLauncher.launch(moreActionsViewModel.homeRoleIntent!!) },
      )
    }
  }
}
