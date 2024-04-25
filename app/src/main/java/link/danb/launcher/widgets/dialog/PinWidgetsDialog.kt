package link.danb.launcher.shortcuts

import android.appwidget.AppWidgetProviderInfo
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import link.danb.launcher.R
import link.danb.launcher.components.UserApplication
import link.danb.launcher.icons.LauncherIcon
import link.danb.launcher.ui.BottomSheet
import link.danb.launcher.ui.WidgetPreview
import link.danb.launcher.widgets.dialog.PinWidgetsViewData

@Composable
fun PinWidgetsDialog(
  isShowing: Boolean,
  viewData: PinWidgetsViewData?,
  onClick: (AppWidgetProviderInfo) -> Unit,
  onDismissRequest: () -> Unit,
) {
  BottomSheet(isShowing, onDismissRequest) { dismiss ->
    val expandedApplications = remember { mutableStateListOf<UserApplication>() }

    LazyColumn {
      item {
        ListItem(
          headlineContent = {
            Text(
              stringResource(R.string.pin_widget),
              style = MaterialTheme.typography.headlineMedium,
            )
          },
          leadingContent = {
            Icon(
              painter = painterResource(R.drawable.ic_baseline_widgets_24),
              contentDescription = null,
            )
          },
        )
      }

      when (viewData) {
        is PinWidgetsViewData.Loading -> {
          item {
            Box(Modifier.fillMaxWidth().padding(8.dp), contentAlignment = Alignment.Center) {
              CircularProgressIndicator(Modifier.size(64.dp))
            }
          }
        }
        is PinWidgetsViewData.Loaded -> {
          items(items = viewData.viewItems) { item ->
            when (item) {
              is PinWidgetsViewData.PinWidgetViewItem.PinWidgetHeader -> {
                ListItem(
                  headlineContent = {
                    Text(
                      item.iconTileViewData.name,
                      style = MaterialTheme.typography.headlineMedium,
                    )
                  },
                  Modifier.clickable {
                    if (expandedApplications.contains(item.userApplication)) {
                      expandedApplications.remove(item.userApplication)
                    } else {
                      expandedApplications.add(item.userApplication)
                    }
                  },
                  leadingContent = {
                    LauncherIcon(
                      item.iconTileViewData.icon,
                      item.iconTileViewData.badge,
                      Modifier.size(dimensionResource(R.dimen.launcher_icon_size)),
                    )
                  },
                  trailingContent = {
                    Icon(
                      painterResource(
                        if (expandedApplications.contains(item.userApplication)) {
                          R.drawable.baseline_expand_less_24
                        } else {
                          R.drawable.baseline_expand_more_24
                        }
                      ),
                      contentDescription = null,
                    )
                  },
                )
              }
              is PinWidgetsViewData.PinWidgetViewItem.PinWidgetEntry -> {
                AnimatedVisibility(visible = expandedApplications.contains(item.userApplication)) {
                  Card(Modifier.padding(4.dp)) {
                    WidgetPreview(
                      item.widgetPreviewData,
                      onClick = { onClick(item.widgetPreviewData.providerInfo) },
                    )
                  }
                }
              }
            }
          }
        }
        null -> {}
      }

      item { Spacer(Modifier.safeDrawingPadding()) }
    }
  }
}
