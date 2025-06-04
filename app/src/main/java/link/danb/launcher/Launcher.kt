package link.danb.launcher

import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import link.danb.launcher.activities.details.ActivityDetailsDialog
import link.danb.launcher.activities.hidden.HiddenAppsDialog
import link.danb.launcher.apps.rememberAppsLauncher
import link.danb.launcher.components.UserActivity
import link.danb.launcher.gestures.GestureActivityAnimation
import link.danb.launcher.shortcuts.PinShortcutsDialog
import link.danb.launcher.ui.LauncherIcon
import link.danb.launcher.ui.LauncherTile
import link.danb.launcher.ui.Widget
import link.danb.launcher.ui.predictiveBackScaling
import link.danb.launcher.widgets.WidgetsViewModel
import link.danb.launcher.widgets.dialog.PinWidgetsDialog

val LocalUseMonochromeIcons: ProvidableCompositionLocal<Boolean> = compositionLocalOf { false }

@Composable
fun Launcher(
  launcherViewModel: LauncherViewModel = hiltViewModel(),
  widgetsViewModel: WidgetsViewModel = hiltViewModel(),
) {
  val useMonochromeIcons by launcherViewModel.useMonochromeIcons.collectAsStateWithLifecycle()
  CompositionLocalProvider(LocalUseMonochromeIcons provides useMonochromeIcons) {
    var showPinShortcuts by remember { mutableStateOf(false) }
    var showPinWidgets by remember { mutableStateOf(false) }
    var showHiddenApps by remember { mutableStateOf(false) }
    var showMoreActions by remember { mutableStateOf(false) }
    var showActivityDetailsFor: UserActivity? by remember { mutableStateOf(null) }

    val appsLauncher = rememberAppsLauncher()

    GestureActivityAnimation {
      val profile by launcherViewModel.profile.collectAsStateWithLifecycle()
      Scaffold(
        modifier = Modifier.predictiveBackScaling(48.dp),
        bottomBar = {
          val searchItemBounds =
            with(LocalDensity.current) {
              val iconSize = dimensionResource(R.dimen.launcher_icon_size).toPx()
              Rect(8.dp.toPx(), 32.dp.toPx(), iconSize, iconSize)
            }
          LauncherBottomBar(
            launcherViewModel,
            onChangeProfile = launcherViewModel::setProfile,
            onSearchGo = {
              val userActivity =
                launcherViewModel.viewItems.value
                  .filterIsInstance<ActivityViewItem>()
                  .firstOrNull()
                  ?.userActivity
              if (userActivity != null) {
                appsLauncher.startMainActivity(userActivity, searchItemBounds)
              }
            },
            onMoreActionsClick = { showMoreActions = true },
          )
        },
        containerColor = Color.Transparent,
        content = { paddingValues ->
          var isScrollEnabled by remember { mutableStateOf(true) }
          val items by launcherViewModel.viewItems.collectAsStateWithLifecycle()
          val textStyle =
            MaterialTheme.typography.labelMedium.copy(
              color = Color.White,
              shadow = Shadow(color = Color.Black, blurRadius = 8f),
            )

          LazyVerticalGrid(
            columns = GridCells.Adaptive(dimensionResource(R.dimen.min_column_width)),
            modifier =
              Modifier.windowInsetsPadding(
                WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal)
              ),
            userScrollEnabled = isScrollEnabled,
          ) {
            item(span = { GridItemSpan(maxLineSpan) }) {
              Spacer(Modifier.height(paddingValues.calculateTopPadding()))
            }

            items(
              items,
              span = { item ->
                when (item) {
                  is WidgetViewItem,
                  is GroupHeaderViewItem,
                  is TabViewItem -> GridItemSpan(maxLineSpan)

                  else -> GridItemSpan(1)
                }
              },
              key = { item -> "${item::class.qualifiedName}:${item.key}" },
              contentType = { item -> item::class },
            ) { item ->
              when (item) {
                is WidgetViewItem -> {
                  Widget(
                    widgetData = item.widgetData,
                    sizeRange = item.sizeRange,
                    isConfigurable = item.isConfigurable,
                    modifier = Modifier.animateItem(),
                    setScrollEnabled = { isScrollEnabled = it },
                    moveUp = { widgetsViewModel.moveUp(item.widgetData.widgetId) },
                    moveDown = { widgetsViewModel.moveDown(item.widgetData.widgetId) },
                    remove = { widgetsViewModel.delete(item.widgetData.widgetId) },
                    setHeight = { widgetsViewModel.setHeight(item.widgetData.widgetId, it) },
                    configure = { appsLauncher.configureWidget(it, item.widgetData.widgetId) },
                  )
                }

                is GroupHeaderViewItem -> {
                  Text(
                    item.name,
                    Modifier.padding(8.dp).animateItem(),
                    style =
                      MaterialTheme.typography.titleMedium.copy(
                        color = Color.White,
                        shadow = Shadow(color = Color.Black, blurRadius = 8f),
                      ),
                  )
                }

                is ShortcutViewItem -> {
                  val context = LocalContext.current
                  LauncherTile(
                    icon = { isPressed ->
                      LauncherIcon(
                        item.launcherTileData.launcherIconData,
                        Modifier.size(dimensionResource(R.dimen.launcher_icon_size)),
                        isPressed = isPressed,
                      )
                    },
                    text = {
                      Text(
                        item.launcherTileData.name,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        style = textStyle,
                      )
                    },
                    modifier = Modifier.animateItem(),
                    onClick = { appsLauncher.startShortcut(item.userShortcut, it) },
                    onLongClick = {
                      MaterialAlertDialogBuilder(context)
                        .setTitle(R.string.unpin_shortcut)
                        .setPositiveButton(android.R.string.ok) { _, _ ->
                          Toast.makeText(context, R.string.unpinned_shortcut, Toast.LENGTH_SHORT)
                            .show()
                          launcherViewModel.unpinShortcut(item.userShortcut)
                        }
                        .setNegativeButton(android.R.string.cancel, null)
                        .show()
                    },
                  )
                }

                is ActivityViewItem -> {
                  LauncherTile(
                    icon = { isPressed ->
                      LauncherIcon(
                        item.launcherTileData.launcherIconData,
                        Modifier.gestureIcon(item)
                          .size(dimensionResource(R.dimen.launcher_icon_size)),
                        isPressed = isPressed,
                      )
                    },
                    text = {
                      Text(
                        item.launcherTileData.name,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        style = textStyle,
                      )
                    },
                    modifier = Modifier.animateItem(),
                    onClick = { appsLauncher.startMainActivity(item.userActivity, it) },
                    onLongClick = { showActivityDetailsFor = item.userActivity },
                  )
                }

                is TabViewItem -> {
                  val context = LocalContext.current
                  LauncherTile(
                    icon = {
                      if (item.icon != null) {
                        Image(
                          item.icon,
                          contentDescription = null,
                          Modifier.size(width = 80.dp, height = 60.dp).clip(RoundedCornerShape(25)),
                          alignment = Alignment.TopCenter,
                          contentScale = ContentScale.FillWidth,
                        )
                      }
                    },
                    text = {
                      Text(
                        item.name,
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis,
                        style = textStyle,
                      )
                    },
                    modifier = Modifier.animateItem(),
                    onClick = {
                      if (item.uri != null) {
                        context.startActivity(Intent(Intent.ACTION_VIEW, item.uri))
                      }
                    },
                    onLongClick = { launcherViewModel.clearTab(item.id) },
                  )
                }
              }
            }

            item(span = { GridItemSpan(maxLineSpan) }) {
              Spacer(Modifier.height(paddingValues.calculateBottomPadding()))
            }
          }
        },
      )

      if (showPinShortcuts) {
        PinShortcutsDialog(profile) { showPinShortcuts = false }
      }

      if (showPinWidgets) {
        PinWidgetsDialog(profile) { showPinWidgets = false }
      }

      if (showHiddenApps) {
        HiddenAppsDialog(
          profile = profile,
          navigateToDetails = { showActivityDetailsFor = it },
          dismiss = { showHiddenApps = false },
        )
      }

      MoreActionsDialog(
        showMoreActions,
        profile = profile,
        pinShortcuts = { showPinShortcuts = true },
        pinWidgets = { showPinWidgets = true },
        shownHiddenApps = { showHiddenApps = true },
      ) {
        showMoreActions = false
      }

      if (showActivityDetailsFor != null) {
        ActivityDetailsDialog(
          showActivityDetailsFor!!,
          dismiss = { showActivityDetailsFor = null },
          onShortcutCreatorClick = { _, item -> appsLauncher.startShortcutCreator(item) },
        )
      }
    }
  }
}
