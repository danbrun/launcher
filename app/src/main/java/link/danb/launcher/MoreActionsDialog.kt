package link.danb.launcher

import android.os.UserHandle
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsBottomHeight
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.fragment.app.FragmentManager
import kotlinx.coroutines.launch
import link.danb.launcher.activities.HiddenActivitiesDialogFragment
import link.danb.launcher.shortcuts.PinShortcutsDialogFragment
import link.danb.launcher.widgets.PinWidgetsDialogFragment

@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun MoreActionsDialog(
  isShowing: Boolean,
  userHandle: UserHandle,
  hasHiddenApps: Boolean,
  fragmentManager: FragmentManager,
  onDismiss: () -> Unit,
) {
  val sheetState = rememberModalBottomSheetState()
  val coroutineScope = rememberCoroutineScope()

  val dismiss: () -> Unit = {
    coroutineScope.launch { sheetState.hide() }.invokeOnCompletion { onDismiss() }
  }

  if (isShowing) {
    ModalBottomSheet(
      onDismissRequest = { onDismiss() },
      sheetState = sheetState,
      windowInsets = WindowInsets(0.dp, 0.dp, 0.dp, 0.dp),
    ) {
      PinShortcutItem(fragmentManager, userHandle, dismiss)

      PinWidgetItem(fragmentManager, userHandle, dismiss)

      if (hasHiddenApps) {
        HiddenAppsItem(fragmentManager, userHandle, dismiss)
      }

      Spacer(modifier = Modifier.windowInsetsBottomHeight(WindowInsets.safeDrawing))
    }
  }
}

@Composable
fun PinShortcutItem(
  fragmentManager: FragmentManager,
  userHandle: UserHandle,
  dismiss: () -> Unit,
) {
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
fun HiddenAppsItem(
  fragmentManager: FragmentManager,
  userHandle: UserHandle,
  dismiss: () -> Unit,
) {
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
