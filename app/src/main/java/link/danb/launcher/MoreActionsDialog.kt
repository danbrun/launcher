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
import link.danb.launcher.ui.BottomSheet

@Composable
fun MoreActionsDialog(
  isShowing: Boolean,
  actions: List<BottomBarAction>,
  onActionClick: (BottomBarAction.Type, UserHandle) -> Unit,
  onDismissRequest: () -> Unit,
) {
  BottomSheet(isShowing = isShowing, onDismissRequest = onDismissRequest) { dismiss ->
    for (action in actions) {
      ListItem(
        headlineContent = { Text(stringResource(action.name)) },
        leadingContent = { Icon(painterResource(action.icon), contentDescription = null) },
        modifier =
          Modifier.clickable {
            onActionClick(action.type, action.user)
            dismiss()
          },
      )
    }

    Spacer(modifier = Modifier.safeDrawingPadding())
  }
}
