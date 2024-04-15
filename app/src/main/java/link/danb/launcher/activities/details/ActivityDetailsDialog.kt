package link.danb.launcher.activities.details

import android.appwidget.AppWidgetHost
import android.appwidget.AppWidgetProviderInfo
import android.content.res.Resources
import android.graphics.drawable.Drawable
import android.os.Build
import android.view.View
import android.widget.ImageView
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import link.danb.launcher.R
import link.danb.launcher.components.UserShortcut
import link.danb.launcher.components.UserShortcutCreator
import link.danb.launcher.database.ActivityData
import link.danb.launcher.ui.BottomSheet

@Composable
fun ActivityDetailsDialog(
  activityDetailsData: ActivityDetailsViewModel.ActivityDetails?,
  appWidgetHost: AppWidgetHost,
  onDismissRequest: () -> Unit,
  onToggledPinned: (ActivityData) -> Unit,
  onToggleHidden: (ActivityData) -> Unit,
  onUninstall: (ActivityData) -> Unit,
  onSettings: (ActivityData) -> Unit,
  onShortcutClick: (View, UserShortcut) -> Unit,
  onShortcutLongClick: (View, UserShortcut) -> Unit,
  onShortcutCreatorClick: (View, UserShortcutCreator) -> Unit,
  onShortcutCreatorLongClick: (View, UserShortcutCreator) -> Unit,
  onWidgetPreviewClick: (AppWidgetProviderInfo) -> Unit,
) {
  BottomSheet(isShowing = activityDetailsData != null, onDismissRequest = onDismissRequest) {
    dismiss ->
    LazyVerticalGrid(columns = GridCells.Adaptive(dimensionResource(R.dimen.min_column_width))) {
      val activityData = checkNotNull(activityDetailsData).activityData

      item(span = { GridItemSpan(maxLineSpan) }) {
        ActivityHeader(activityDetailsData.icon, activityDetailsData.name)
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
            Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
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
              }
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
            Tile(
              icon = item.icon,
              name = item.name,
              onClick = { view ->
                onShortcutClick(view, item.userShortcut)
                dismiss()
              },
              onLongClick = { view -> onShortcutLongClick(view, item.userShortcut) },
            )
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
            Tile(
              icon = item.icon,
              name = item.name,
              onClick = { view ->
                onShortcutCreatorClick(view, item.userShortcutCreator)
                dismiss()
              },
              onLongClick = { view -> onShortcutCreatorLongClick(view, item.userShortcutCreator) },
            )
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
            WidgetPreview(item, appWidgetHost) { onWidgetPreviewClick(item.providerInfo) }
          }
        }
      }

      item(span = { GridItemSpan(maxLineSpan) }) { Spacer(Modifier.safeDrawingPadding()) }
    }
  }
}

@Composable
private fun ActivityHeader(icon: Drawable, name: String) {
  ListItem(
    headlineContent = { Text(name, style = MaterialTheme.typography.headlineMedium) },
    leadingContent = {
      Box(
        Modifier.size(dimensionResource(R.dimen.launcher_icon_size)).drawWithContent {
          drawIntoCanvas {
            // This transform is to avoid changing the bounds which causes other uses of the
            // icon to change size.
            withTransform({ scale(size.width / icon.bounds.width(), Offset.Zero) }) {
              icon.draw(it.nativeCanvas)
            }
          }
        }
      )
    },
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
  )
}

@Composable
private fun SectionHeader(text: String) {
  ListItem(headlineContent = { Text(text, style = MaterialTheme.typography.titleMedium) })
}

@Composable
@OptIn(ExperimentalFoundationApi::class)
private fun Tile(
  icon: Drawable,
  name: String,
  onClick: (View) -> Unit,
  onLongClick: (View) -> Unit,
) {
  var imageView: ImageView? by remember { mutableStateOf(null) }
  Card(
    Modifier.padding(4.dp)
      .combinedClickable(
        onClick = { onClick(checkNotNull(imageView)) },
        onLongClick = { onLongClick(checkNotNull(imageView)) },
      )
  ) {
    Row(modifier = Modifier.padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
      AndroidView(
        factory = { ImageView(it).apply { imageView = this } },
        update = { it.setImageDrawable(icon) },
        onReset = { it.setImageDrawable(null) },
        modifier = Modifier.size(dimensionResource(R.dimen.launcher_icon_size)),
      )
      Text(
        name,
        maxLines = 2,
        overflow = TextOverflow.Ellipsis,
        modifier = Modifier.padding(start = 8.dp),
      )
    }
  }
}

@Composable
@OptIn(ExperimentalFoundationApi::class)
fun WidgetPreview(
  item: ActivityDetailsViewModel.WidgetPreviewViewData,
  appWidgetHost: AppWidgetHost,
  onClick: () -> Unit,
) {
  Card(Modifier.padding(4.dp).combinedClickable(onClick = onClick)) {
    if (
      item.previewLayout != null &&
        item.previewLayout != Resources.ID_NULL &&
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
    ) {
      AndroidView(
        factory = {
          appWidgetHost.createView(
            it,
            Resources.ID_NULL,
            item.providerInfo.clone().apply { initialLayout = previewLayout },
          )
        },
        update = {
          it.setAppWidget(
            Resources.ID_NULL,
            item.providerInfo.clone().apply { initialLayout = previewLayout },
          )
        },
      )
    } else {
      Box(Modifier.fillMaxWidth().padding(8.dp), contentAlignment = Alignment.Center) {
        AndroidView(
          factory = { ImageView(it) },
          update = { it.setImageDrawable(item.previewImage) },
          onReset = { it.setImageDrawable(null) },
        )
      }
    }

    Text(item.label, Modifier.fillMaxWidth().padding(8.dp), textAlign = TextAlign.Center)

    if (item.description != null) {
      Text(item.description, Modifier.fillMaxWidth().padding(8.dp), textAlign = TextAlign.Center)
    }
  }
}
