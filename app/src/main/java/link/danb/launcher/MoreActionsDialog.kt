package link.danb.launcher

import android.content.Intent
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import link.danb.launcher.profiles.Profile
import link.danb.launcher.ui.BottomSheet

@Composable
fun MoreActionsDialog(
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

  BottomSheet(isShowing = true, onDismissRequest = dismiss) { dismiss ->
    Column {
      AnimatedVisibility(visible = canPinItems) {
        Column {
          ListItem(
            headlineContent = { Text(stringResource(R.string.pin_shortcut)) },
            leadingContent = {
              Icon(painterResource(R.drawable.baseline_shortcut_24), contentDescription = null)
            },
            modifier =
              Modifier.clickable {
                pinShortcuts()
                dismiss()
              },
            colors = ListItemDefaults.colors(containerColor = Color.Transparent),
          )

          ListItem(
            headlineContent = { Text(stringResource(R.string.pin_widget)) },
            leadingContent = {
              Icon(painterResource(R.drawable.ic_baseline_widgets_24), contentDescription = null)
            },
            modifier =
              Modifier.clickable {
                pinWidgets()
                dismiss()
              },
            colors = ListItemDefaults.colors(containerColor = Color.Transparent),
          )
        }
      }

      AnimatedVisibility(visible = hasHiddenApps) {
        ListItem(
          headlineContent = { Text(stringResource(R.string.show_hidden)) },
          leadingContent = {
            Icon(painterResource(R.drawable.ic_baseline_visibility_24), contentDescription = null)
          },
          modifier =
            Modifier.clickable {
              shownHiddenApps()
              dismiss()
            },
          colors = ListItemDefaults.colors(containerColor = Color.Transparent),
        )
      }

      ListItem(
        headlineContent = { Text(stringResource(R.string.toggle_monochrome)) },
        leadingContent = {
          Icon(painterResource(R.drawable.baseline_style_24), contentDescription = null)
        },
        modifier = Modifier.clickable { moreActionsViewModel.toggleMonochromeIcons() },
        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
      )

      val canRequestHomeRole by
        moreActionsViewModel.canRequestHomeRole.collectAsStateWithLifecycle()
      AnimatedVisibility(visible = canRequestHomeRole) {
        val context = LocalContext.current
        val setHomeActivityResultLauncher =
          rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            if (moreActionsViewModel.shouldLaunchDefaultAppsSettings) {
              context.startActivity(Intent(Settings.ACTION_MANAGE_DEFAULT_APPS_SETTINGS))
            }
          }
        ListItem(
          headlineContent = { Text(stringResource(R.string.request_home_role)) },
          leadingContent = {
            Icon(painterResource(R.drawable.baseline_home_filled_24), contentDescription = null)
          },
          modifier =
            Modifier.clickable {
              setHomeActivityResultLauncher.launch(moreActionsViewModel.homeRoleIntent!!)
            },
          colors = ListItemDefaults.colors(containerColor = Color.Transparent),
        )
      }
    }
  }
}
