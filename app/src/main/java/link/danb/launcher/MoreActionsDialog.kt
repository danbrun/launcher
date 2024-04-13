package link.danb.launcher

import android.os.UserHandle
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.fragment.app.FragmentManager
import link.danb.launcher.activities.HiddenActivitiesDialogFragment
import link.danb.launcher.shortcuts.PinShortcutsDialogFragment
import link.danb.launcher.ui.BottomSheet
import link.danb.launcher.widgets.PinWidgetsDialogFragment

@Composable
fun MoreActionsDialog(
  isShowing: Boolean,
  userHandle: UserHandle,
  hasHiddenApps: Boolean,
  fragmentManager: FragmentManager,
  onDismissRequest: () -> Unit,
) {
  BottomSheet(isShowing = isShowing, onDismissRequest = onDismissRequest) { hide ->
    PinShortcutItem(fragmentManager, userHandle) { hide() }

    PinWidgetItem(fragmentManager, userHandle) { hide() }

    if (hasHiddenApps) {
      HiddenAppsItem(fragmentManager, userHandle) { hide() }
    }

    Spacer(modifier = Modifier.safeDrawingPadding())
  }
}

@Composable
fun PinShortcutItem(fragmentManager: FragmentManager, userHandle: UserHandle, dismiss: () -> Unit) {
  ListItem(
    headlineContent = { Text(text = stringResource(R.string.pin_shortcut)) },
    leadingContent = {
      Icon(painter = painterResource(R.drawable.baseline_shortcut_24), contentDescription = null)
    },
    modifier =
      Modifier.clickable {
        PinShortcutsDialogFragment.newInstance(userHandle)
          .showNow(fragmentManager, PinShortcutsDialogFragment.TAG)
        dismiss()
      },
  )
}

@Composable
fun PinWidgetItem(fragmentManager: FragmentManager, userHandle: UserHandle, dismiss: () -> Unit) {
  ListItem(
    headlineContent = { Text(text = stringResource(R.string.pin_widget)) },
    leadingContent = {
      Icon(painter = painterResource(R.drawable.ic_baseline_widgets_24), contentDescription = null)
    },
    modifier =
      Modifier.clickable {
        PinWidgetsDialogFragment.newInstance(userHandle)
          .showNow(fragmentManager, PinShortcutsDialogFragment.TAG)
        dismiss()
      },
  )
}

@Composable
fun HiddenAppsItem(fragmentManager: FragmentManager, userHandle: UserHandle, dismiss: () -> Unit) {
  ListItem(
    headlineContent = { Text(text = stringResource(R.string.show_hidden)) },
    leadingContent = {
      Icon(
        painter = painterResource(R.drawable.ic_baseline_visibility_24),
        contentDescription = null,
      )
    },
    modifier =
      Modifier.clickable {
        HiddenActivitiesDialogFragment.newInstance(userHandle)
          .showNow(fragmentManager, HiddenActivitiesDialogFragment.TAG)
        dismiss()
      },
  )
}
