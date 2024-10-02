package link.danb.launcher.activities.details

import android.appwidget.AppWidgetProviderInfo
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import link.danb.launcher.R
import link.danb.launcher.components.UserShortcut
import link.danb.launcher.components.UserShortcutCreator
import link.danb.launcher.database.ActivityData
import link.danb.launcher.ui.BottomSheet
import link.danb.launcher.ui.LauncherIcon
import link.danb.launcher.ui.LauncherTile
import link.danb.launcher.ui.LauncherTileData
import link.danb.launcher.ui.WidgetPreview

@Composable
fun ActivityDetailsDialog(
  activityDetailsData: ActivityDetailsViewModel.ActivityDetailsData?,
  onDismissRequest: () -> Unit,
  onToggledPinned: (ActivityData) -> Unit,
  onToggleHidden: (ActivityData) -> Unit,
  onUninstall: (ActivityData) -> Unit,
  onSettings: (ActivityData) -> Unit,
  onShortcutClick: (Offset, UserShortcut) -> Unit,
  onShortcutLongClick: (Offset, UserShortcut) -> Unit,
  onShortcutCreatorClick: (Offset, UserShortcutCreator) -> Unit,
  onShortcutCreatorLongClick: (Offset, UserShortcutCreator) -> Unit,
  onWidgetPreviewClick: (AppWidgetProviderInfo) -> Unit,
) {
  BottomSheet(isShowing = activityDetailsData != null, onDismissRequest = onDismissRequest) {
    dismiss ->
    LazyVerticalGrid(columns = GridCells.Adaptive(dimensionResource(R.dimen.min_column_width))) {
      val activityData = checkNotNull(activityDetailsData).activityData

      item(span = { GridItemSpan(maxLineSpan) }) {
        ActivityHeader(activityDetailsData.launcherTileData)
      }

      item(span = { GridItemSpan(maxLineSpan) }) {
        PinActivityListItem(activityData.isPinned) { onToggledPinned(activityData) }
      }

      item(span = { GridItemSpan(maxLineSpan) }) {
        HideActivityListItem(activityData.isHidden) { onToggleHidden(activityData) }
      }

      item(span = { GridItemSpan(maxLineSpan) }) {
        UninstallActivityListItem { onUninstall(activityData) }
      }

      item(span = { GridItemSpan(maxLineSpan) }) {
        OpenActivitySettingsListItem {
          onSettings(activityData)
          dismiss()
        }
      }

      when (activityDetailsData.shortcutsAndWidgets) {
        is ActivityDetailsViewModel.ShortcutsAndWidgets.Loading -> {
          item(span = { GridItemSpan(maxLineSpan) }) {
            Box(Modifier.fillMaxWidth().padding(8.dp), contentAlignment = Alignment.Center) {
              CircularProgressIndicator(Modifier.size(64.dp))
            }
          }
        }
        is ActivityDetailsViewModel.ShortcutsAndWidgets.ProfileDisabled -> {
          item(span = { GridItemSpan(maxLineSpan) }) {
            ListItem(
              headlineContent = {
                Text(
                  stringResource(R.string.enable_work_profile),
                  style = MaterialTheme.typography.labelMedium,
                )
              },
              colors = ListItemDefaults.colors(containerColor = Color.Transparent),
            )
          }
        }
        is ActivityDetailsViewModel.ShortcutsAndWidgets.Loaded -> {
          if (activityDetailsData.shortcutsAndWidgets.shortcuts.isNotEmpty()) {
            item(span = { GridItemSpan(maxLineSpan) }) {
              SectionHeader(stringResource(R.string.shortcuts))
            }
          }

          items(activityDetailsData.shortcutsAndWidgets.shortcuts, key = { it.userShortcut }) { item
            ->
            Card(Modifier.padding(4.dp)) {
              LauncherTile(
                item.launcherTileData,
                onClick = { view ->
                  onShortcutClick(view, item.userShortcut)
                  dismiss()
                },
                onLongClick = { view -> onShortcutLongClick(view, item.userShortcut) },
              )
            }
          }

          if (activityDetailsData.shortcutsAndWidgets.configurableShortcuts.isNotEmpty()) {
            item(span = { GridItemSpan(maxLineSpan) }) {
              SectionHeader(stringResource(R.string.configurable_shortcuts))
            }
          }

          items(
            activityDetailsData.shortcutsAndWidgets.configurableShortcuts,
            key = { it.userShortcutCreator },
          ) { item ->
            Card(Modifier.padding(4.dp)) {
              LauncherTile(
                item.launcherTileData,
                onClick = { view ->
                  onShortcutCreatorClick(view, item.userShortcutCreator)
                  dismiss()
                },
                onLongClick = { view -> onShortcutCreatorLongClick(view, item.userShortcutCreator) },
              )
            }
          }

          if (activityDetailsData.shortcutsAndWidgets.widgets.isNotEmpty()) {
            item(span = { GridItemSpan(maxLineSpan) }) {
              SectionHeader(stringResource(R.string.widgets))
            }
          }

          items(
            activityDetailsData.shortcutsAndWidgets.widgets,
            key = { it.providerInfo },
            span = { GridItemSpan(maxLineSpan) },
          ) { item ->
            WidgetPreview(item) { onWidgetPreviewClick(item.providerInfo) }
          }
        }
      }
    }
  }
}

@Composable
private fun ActivityHeader(data: LauncherTileData) {
  ListItem(
    headlineContent = { Text(data.name, style = MaterialTheme.typography.headlineMedium) },
    leadingContent = {
      LauncherIcon(
        data.launcherIconData,
        Modifier.size(dimensionResource(R.dimen.launcher_icon_size)),
      )
    },
    colors = ListItemDefaults.colors(containerColor = Color.Transparent),
  )
}

@Composable
private fun PinActivityListItem(isPinned: Boolean, onClick: () -> Unit) {
  ListItem(
    headlineContent = {
      Text(stringResource(if (isPinned) R.string.unpin_app else R.string.pin_app))
    },
    leadingContent = {
      Icon(
        painter =
          painterResource(
            if (isPinned) R.drawable.baseline_push_pin_off_24 else R.drawable.baseline_push_pin_24
          ),
        contentDescription = null,
      )
    },
    modifier = Modifier.clickable(onClick = onClick),
    colors = ListItemDefaults.colors(containerColor = Color.Transparent),
  )
}

@Composable
private fun HideActivityListItem(isHidden: Boolean, onClick: () -> Unit) {
  ListItem(
    headlineContent = {
      Text(stringResource(if (isHidden) R.string.show_app else R.string.hide_app))
    },
    leadingContent = {
      Icon(
        painter =
          painterResource(
            if (isHidden) R.drawable.ic_baseline_visibility_24
            else R.drawable.ic_baseline_visibility_off_24
          ),
        contentDescription = null,
      )
    },
    modifier = Modifier.clickable(onClick = onClick),
    colors = ListItemDefaults.colors(containerColor = Color.Transparent),
  )
}

@Composable
private fun UninstallActivityListItem(onClick: () -> Unit) {
  ListItem(
    headlineContent = { Text(stringResource(R.string.uninstall)) },
    leadingContent = {
      Icon(
        painter = painterResource(R.drawable.ic_baseline_delete_forever_24),
        contentDescription = null,
      )
    },
    modifier = Modifier.clickable(onClick = onClick),
    colors = ListItemDefaults.colors(containerColor = Color.Transparent),
  )
}

@Composable
private fun OpenActivitySettingsListItem(onClick: () -> Unit) {
  ListItem(
    headlineContent = { Text(stringResource(R.string.settings)) },
    leadingContent = {
      Icon(painter = painterResource(R.drawable.ic_baseline_settings_24), contentDescription = null)
    },
    modifier = Modifier.clickable(onClick = onClick),
    colors = ListItemDefaults.colors(containerColor = Color.Transparent),
  )
}

@Composable
private fun SectionHeader(text: String) {
  ListItem(
    headlineContent = { Text(text, style = MaterialTheme.typography.titleMedium) },
    colors = ListItemDefaults.colors(containerColor = Color.Transparent),
  )
}
