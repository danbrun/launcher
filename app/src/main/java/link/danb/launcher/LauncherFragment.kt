package link.danb.launcher

import android.app.ActivityOptions
import android.app.SearchManager
import android.appwidget.AppWidgetProviderInfo
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.Toast
import androidx.activity.result.ActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.toAndroidRectF
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.unit.dp
import androidx.core.graphics.toRect
import androidx.core.util.Consumer
import androidx.core.view.updateLayoutParams
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import link.danb.launcher.activities.ActivitiesViewModel
import link.danb.launcher.activities.ActivityManager
import link.danb.launcher.activities.details.ActivityDetailsDialog
import link.danb.launcher.activities.details.ActivityDetailsViewModel
import link.danb.launcher.activities.hidden.HiddenAppsDialog
import link.danb.launcher.activities.hidden.HiddenAppsViewModel
import link.danb.launcher.components.UserActivity
import link.danb.launcher.components.UserShortcut
import link.danb.launcher.components.UserShortcutCreator
import link.danb.launcher.database.ActivityData
import link.danb.launcher.extensions.boundsOnScreen
import link.danb.launcher.extensions.makeScaleUpAnimation
import link.danb.launcher.gestures.GestureContract
import link.danb.launcher.gestures.GestureIconView
import link.danb.launcher.profiles.ProfileManager
import link.danb.launcher.shortcuts.PinShortcutsDialog
import link.danb.launcher.shortcuts.PinShortcutsViewModel
import link.danb.launcher.shortcuts.PinWidgetsDialog
import link.danb.launcher.shortcuts.ShortcutManager
import link.danb.launcher.ui.IconTile
import link.danb.launcher.ui.IconTileViewData
import link.danb.launcher.ui.Widget
import link.danb.launcher.ui.theme.LauncherTheme
import link.danb.launcher.widgets.AppWidgetSetupActivityResultContract
import link.danb.launcher.widgets.AppWidgetViewProvider
import link.danb.launcher.widgets.WidgetManager
import link.danb.launcher.widgets.WidgetSizeUtil
import link.danb.launcher.widgets.WidgetsViewModel
import link.danb.launcher.widgets.dialog.PinWidgetsViewModel

@AndroidEntryPoint
class LauncherFragment : Fragment() {

  private val activitiesViewModel: ActivitiesViewModel by activityViewModels()
  private val activityDetailsViewModel: ActivityDetailsViewModel by activityViewModels()
  private val hiddenAppsViewModel: HiddenAppsViewModel by activityViewModels()
  private val launcherViewModel: LauncherViewModel by activityViewModels()
  private val pinShortcutsViewModel: PinShortcutsViewModel by activityViewModels()
  private val pinWidgetsViewModel: PinWidgetsViewModel by activityViewModels()
  private val widgetsViewModel: WidgetsViewModel by activityViewModels()

  @Inject lateinit var activityManager: ActivityManager
  @Inject lateinit var appWidgetViewProvider: AppWidgetViewProvider
  @Inject lateinit var shortcutManager: ShortcutManager
  @Inject lateinit var widgetManager: WidgetManager
  @Inject lateinit var widgetSizeUtil: WidgetSizeUtil
  @Inject lateinit var profileManager: ProfileManager

  private lateinit var iconLaunchView: View
  private lateinit var gestureIconView: GestureIconView

  private val showMoreActionsDialog: MutableStateFlow<Boolean> = MutableStateFlow(false)

  private var gestureActivity: UserActivity? by mutableStateOf(null)

  private val gestureData: MutableMap<UserActivity, Pair<IconTileViewData, Rect>> = mutableMapOf()

  @RequiresApi(Build.VERSION_CODES.Q)
  private val onNewIntentListener: Consumer<Intent> = Consumer { intent ->
    val gestureContract = GestureContract.fromIntent(intent) ?: return@Consumer

    val data = getGestureData(gestureContract.userActivity) ?: return@Consumer

    gestureActivity = gestureContract.userActivity
    gestureIconView.animateNavigationGesture(
      gestureContract,
      data.second.toAndroidRectF(),
      data.first.icon,
      data.first.badge,
    )
  }

  private fun getGestureData(userActivity: UserActivity): Pair<IconTileViewData, Rect>? {
    if (gestureData.containsKey(userActivity)) {
      return gestureData[userActivity]
    }

    for (entry in gestureData) {
      if (entry.key.packageName == userActivity.packageName) {
        return entry.value
      }
    }

    return null
  }

  private val shortcutActivityLauncher =
    registerForActivityResult(
      ActivityResultContracts.StartIntentSenderForResult(),
      ::onPinShortcutActivityResult,
    )

  private val bindWidgetActivityLauncher =
    registerForActivityResult(AppWidgetSetupActivityResultContract()) {}

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
      requireActivity().addOnNewIntentListener(onNewIntentListener)
    }
  }

  @OptIn(ExperimentalFoundationApi::class)
  override fun onCreateView(
    inflater: LayoutInflater,
    container: ViewGroup?,
    savedInstanceState: Bundle?,
  ): View {
    val view = inflater.inflate(R.layout.launcher_fragment, container, false) as FrameLayout

    iconLaunchView = view.findViewById(R.id.icon_launch_view)

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
      gestureIconView = GestureIconView(view.context)
      gestureIconView.onFinishGestureAnimation = { gestureActivity = null }
      view.addView(gestureIconView)
    }

    view.findViewById<ComposeView>(R.id.compose_view).setContent {
      LauncherTheme {
        val bottomBarState by launcherViewModel.bottomBarState.collectAsState()
        val activityDetailsData by activityDetailsViewModel.activityDetails.collectAsState(null)
        val hiddenApps by hiddenAppsViewModel.hiddenApps.collectAsState(null)
        val pinShortcuts by pinShortcutsViewModel.pinShortcutsViewData.collectAsState(null)
        val pinWidgets by pinWidgetsViewModel.pinWidgetsViewData.collectAsState(null)
        val isShowing by showMoreActionsDialog.collectAsState()

        val items by launcherViewModel.viewItems.collectAsState(emptyList())

        Scaffold(
          bottomBar = {
            LauncherBottomBar(
              bottomBarState,
              onChangeFilter = { launcherViewModel.setFilter(it) },
              onSearchGo = { launchFirstItem() },
              onWorkProfileToggled = { profileManager.setWorkProfileEnabled(it) },
              onMoreActionsClick = { showMoreActionsDialog.value = true },
              onSearchChange = { launcherViewModel.setFilter(SearchFilter(it)) },
              onSearchFabClick = { onFabClick() },
            )
          },
          containerColor = Color.Transparent,
          content = { paddingValues ->
            var isScrollEnabled by remember { mutableStateOf(true) }
            LazyVerticalGrid(
              columns = GridCells.Adaptive(dimensionResource(R.dimen.min_column_width)),
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
                    is GroupHeaderViewItem -> GridItemSpan(maxLineSpan)
                    else -> GridItemSpan(1)
                  }
                },
                key = { item ->
                  when (item) {
                    is WidgetViewItem -> item.widgetData.widgetId
                    is GroupHeaderViewItem -> item.name
                    is ActivityViewItem -> item.userActivity
                    is ShortcutViewItem -> item.userShortcut
                  }
                },
              ) { item ->
                when (item) {
                  is WidgetViewItem -> {
                    Widget(
                      widgetData = item.widgetData,
                      sizeRange = item.sizeRange,
                      modifier = Modifier.animateItemPlacement(),
                      setScrollEnabled = { isScrollEnabled = it },
                      moveUp = { widgetsViewModel.moveUp(item.widgetData.widgetId) },
                      moveDown = { widgetsViewModel.moveDown(item.widgetData.widgetId) },
                      remove = { widgetsViewModel.delete(item.widgetData.widgetId) },
                      setHeight = { widgetsViewModel.setHeight(item.widgetData.widgetId, it) },
                    )
                  }
                  is GroupHeaderViewItem -> {
                    Text(
                      item.name,
                      Modifier.padding(8.dp).animateItemPlacement(),
                      style =
                        MaterialTheme.typography.titleMedium.copy(
                          color = Color.White,
                          shadow = Shadow(color = Color.Black, blurRadius = 8f),
                        ),
                    )
                  }
                  is ShortcutViewItem -> {
                    IconTile(
                      data = item.iconTileViewData,
                      modifier = Modifier.animateItemPlacement(),
                      style =
                        MaterialTheme.typography.labelMedium.copy(
                          color = Color.White,
                          shadow = Shadow(color = Color.Black, blurRadius = 8f),
                        ),
                      onClick = { launchShortcut(it, item.userShortcut) },
                      onLongClick = { unpinShortcut(item.userShortcut) },
                    )
                  }
                  is ActivityViewItem -> {
                    IconTile(
                      data = item.iconTileViewData,
                      modifier = Modifier.animateItemPlacement(),
                      style =
                        MaterialTheme.typography.labelMedium.copy(
                          color = Color.White,
                          shadow = Shadow(color = Color.Black, blurRadius = 8f),
                        ),
                      onClick = { launchActivity(it, item.userActivity) },
                      onLongClick = {
                        activityDetailsViewModel.showActivityDetails(item.userActivity)
                      },
                      hide = item.userActivity == gestureActivity,
                      onPlace = {
                        if (it == null) {
                          gestureData.remove(item.userActivity)
                        } else {
                          gestureData[item.userActivity] = item.iconTileViewData to it
                        }
                      },
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

        MoreActionsDialog(
          isShowing = isShowing,
          actions = bottomBarState.actions,
          onActionClick = { action, user ->
            when (action) {
              BottomBarAction.Type.PIN_SHORTCUT -> {
                pinShortcutsViewModel.showPinShortcuts(user)
              }
              BottomBarAction.Type.PIN_WIDGET -> {
                pinWidgetsViewModel.showPinWidgetsDialog(user)
              }
              BottomBarAction.Type.SHOW_HIDDEN_APPS -> {
                hiddenAppsViewModel.showHiddenApps(user)
              }
            }
          },
          onDismissRequest = { showMoreActionsDialog.value = false },
        )

        PinShortcutsDialog(
          isShowing = pinShortcuts != null,
          viewData = pinShortcuts,
          onClick = { _, item -> launchShortcutCreator(item) },
          onDismissRequest = { pinShortcutsViewModel.hidePinShortcuts() },
        )

        PinWidgetsDialog(
          isShowing = pinWidgets != null,
          viewData = pinWidgets,
          onClick = { item -> bindWidget(item) },
          onDismissRequest = { pinWidgetsViewModel.hidePinWidgetsDialog() },
        )

        HiddenAppsDialog(
          isShowing = hiddenApps != null,
          viewData = hiddenApps,
          onClick = { offset, item -> launchActivity(offset, item) },
          onLongClick = { _, item -> activityDetailsViewModel.showActivityDetails(item) },
          onDismissRequest = { hiddenAppsViewModel.hideHiddenApps() },
        )

        ActivityDetailsDialog(
          activityDetailsData,
          onDismissRequest = { activityDetailsViewModel.hideActivityDetails() },
          onToggledPinned = { toggleAppPinned(it) },
          onToggleHidden = { toggleAppHidden(it) },
          onUninstall = { uninstallApp(it.userActivity) },
          onSettings = { openAppSettings(it.userActivity) },
          onShortcutClick = { offset, item -> launchShortcut(offset, item) },
          onShortcutLongClick = { _, item -> pinShortcut(item) },
          onShortcutCreatorClick = { _, item -> launchShortcutCreator(item) },
          onShortcutCreatorLongClick = { _, _ -> },
          onWidgetPreviewClick = { bindWidget(it) },
        )
      }
    }

    return view
  }

  override fun onDestroy() {
    super.onDestroy()

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
      requireActivity().removeOnNewIntentListener(onNewIntentListener)
    }
  }

  private fun unpinShortcut(userShortcut: UserShortcut) {
    MaterialAlertDialogBuilder(requireContext())
      .setTitle(R.string.unpin_shortcut)
      .setPositiveButton(android.R.string.ok) { _, _ ->
        Toast.makeText(context, R.string.unpinned_shortcut, Toast.LENGTH_SHORT).show()
        shortcutManager.pinShortcut(userShortcut, isPinned = false)
      }
      .setNegativeButton(android.R.string.cancel, null)
      .show()
  }

  private fun launchFirstItem() {
    val firstActivity =
      launcherViewModel.viewItems.value.filterIsInstance(ActivityViewItem::class.java).firstOrNull()
    if (firstActivity != null) {
      launchActivity(Offset.Zero, firstActivity.userActivity)
    }
  }

  private fun onFabClick() {
    startActivity(
      Intent().apply {
        action = Intent.ACTION_WEB_SEARCH
        putExtra(SearchManager.EXTRA_NEW_SEARCH, true)
        // This extra is for Firefox to open a new tab.
        putExtra("open_to_search", "static_shortcut_new_tab")
      },
      Bundle(),
    )
  }

  private fun uninstallApp(userActivity: UserActivity) {
    startActivity(
      Intent(Intent.ACTION_DELETE)
        .setData(Uri.parse("package:${userActivity.componentName.packageName}"))
        .putExtra(Intent.EXTRA_USER, userActivity.userHandle)
    )
  }

  private fun openAppSettings(userActivity: UserActivity) {
    activityManager.launchAppDetails(
      userActivity,
      Rect.Zero.toAndroidRectF().toRect(),
      ActivityOptions.makeBasic().toBundle(),
    )
  }

  private fun toggleAppPinned(activityData: ActivityData) {
    activitiesViewModel.setMetadata(activityData.copy(isPinned = !activityData.isPinned))
  }

  private fun toggleAppHidden(activityData: ActivityData) {
    activitiesViewModel.setMetadata(activityData.copy(isHidden = !activityData.isHidden))
  }

  private fun launchWithIconView(offset: Offset, onView: View.() -> Unit) {
    iconLaunchView.updateLayoutParams<FrameLayout.LayoutParams> {
      leftMargin = offset.x.toInt()
      topMargin = offset.y.toInt()
    }

    iconLaunchView.post { iconLaunchView.onView() }
  }

  private fun launchActivity(offset: Offset, userActivity: UserActivity) {
    launchWithIconView(offset) {
      activityManager.launchActivity(
        userActivity,
        boundsOnScreen,
        makeScaleUpAnimation().toBundle(),
      )
    }
  }

  private fun launchShortcut(offset: Offset, userShortcut: UserShortcut) {
    launchWithIconView(offset) {
      shortcutManager.launchShortcut(
        userShortcut,
        boundsOnScreen,
        makeScaleUpAnimation().toBundle(),
      )
    }
  }

  private fun pinShortcut(userShortcut: UserShortcut) {
    shortcutManager.pinShortcut(userShortcut, isPinned = true)
    Toast.makeText(context, R.string.pinned_shortcut, Toast.LENGTH_SHORT).show()
  }

  private fun launchShortcutCreator(userShortcutCreator: UserShortcutCreator) {
    shortcutActivityLauncher.launch(
      IntentSenderRequest.Builder(shortcutManager.getShortcutCreatorIntent(userShortcutCreator))
        .build()
    )
  }

  private fun onPinShortcutActivityResult(activityResult: ActivityResult) {
    val data = activityResult.data ?: return
    shortcutManager.acceptPinRequest(data)
    Toast.makeText(context, R.string.pinned_shortcut, Toast.LENGTH_SHORT).show()
  }

  private fun bindWidget(providerInfo: AppWidgetProviderInfo) {
    bindWidgetActivityLauncher.launch(
      AppWidgetSetupActivityResultContract.AppWidgetSetupInput(
        providerInfo.provider,
        providerInfo.profile,
      )
    )
  }
}
