package link.danb.launcher.widgets.dialog

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import link.danb.launcher.R
import link.danb.launcher.apps.componentLabel
import link.danb.launcher.components.UserApplication
import link.danb.launcher.profiles.Profile
import link.danb.launcher.ui.BottomSheet
import link.danb.launcher.ui.LauncherIcon
import link.danb.launcher.ui.WidgetPreview
import link.danb.launcher.widgets.AppWidgetSetupActivityResultContract

@Composable
fun PinWidgetsDialog(
  profile: Profile,
  pinWidgetsViewModel: PinWidgetsViewModel = hiltViewModel(),
  dismiss: () -> Unit,
) {
  BottomSheet(isShowing = true, dismiss) {
    val state by
      remember(profile) { pinWidgetsViewModel.getState(profile) }.collectAsStateWithLifecycle()

    PinWidgetsContent(state) { pinWidgetsViewModel.toggle(it) }
  }
}

@Composable
private fun PinWidgetsContent(state: PinWidgetsViewModel.State, toggle: (UserApplication) -> Unit) {
  val bindWidgetActivityLauncher =
    rememberLauncherForActivityResult(AppWidgetSetupActivityResultContract()) {}

  LazyColumn {
    item {
      ListItem(
        headlineContent = {
          Text(stringResource(R.string.pin_widget), style = MaterialTheme.typography.headlineMedium)
        },
        leadingContent = {
          Icon(
            painter = painterResource(R.drawable.ic_baseline_widgets_24),
            contentDescription = null,
          )
        },
        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
      )
    }

    when (state) {
      is PinWidgetsViewModel.State.Loading -> {
        item {
          Box(Modifier.fillMaxWidth().padding(8.dp), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(Modifier.size(64.dp))
          }
        }
      }
      is PinWidgetsViewModel.State.Loaded -> {
        items(
          items = state.items,
          key = { item ->
            when (item) {
              is PinWidgetsViewModel.State.Item.Header -> {
                item.userApplication.packageName
              }
              is PinWidgetsViewModel.State.Item.Entry -> {
                item.widgetPreviewData.providerInfo.provider
              }
            }
          },
        ) { item ->
          when (item) {
            is PinWidgetsViewModel.State.Item.Header -> {
              Header(item) { toggle(it) }
            }
            is PinWidgetsViewModel.State.Item.Entry -> {
              WidgetPreview(
                item.widgetPreviewData,
                modifier = Modifier.animateItem(),
                onClick = {
                  val info = item.widgetPreviewData.providerInfo
                  bindWidgetActivityLauncher.launch(
                    AppWidgetSetupActivityResultContract.AppWidgetSetupInput(
                      info.provider,
                      info.profile,
                    )
                  )
                },
              )
            }
          }
        }
      }
    }
  }
}

@Composable
private fun Header(
  header: PinWidgetsViewModel.State.Item.Header,
  toggle: (UserApplication) -> Unit,
) {
  val interactionSource = remember { MutableInteractionSource() }
  ListItem(
    headlineContent = {
      Text(
        componentLabel(header.userApplication) ?: "",
        style = MaterialTheme.typography.headlineMedium,
      )
    },
    Modifier.clickable(interactionSource = interactionSource) { toggle(header.userApplication) },
    leadingContent = {
      LauncherIcon(
        header.userApplication,
        Modifier.size(dimensionResource(R.dimen.launcher_icon_size)),
        interactionSource = interactionSource,
      )
    },
    trailingContent = {
      Icon(
        painterResource(
          if (header.isExpanded) {
            R.drawable.baseline_expand_less_24
          } else {
            R.drawable.baseline_expand_more_24
          }
        ),
        contentDescription = null,
      )
    },
    colors = ListItemDefaults.colors(containerColor = Color.Transparent),
  )
}
