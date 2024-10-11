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
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.compositionLocalOf
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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.dialog
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlin.reflect.typeOf
import kotlinx.collections.immutable.persistentListOf
import kotlinx.serialization.Serializable
import link.danb.launcher.activities.ActivitiesViewModel
import link.danb.launcher.activities.ActivityManager
import link.danb.launcher.activities.details.ActivityDetailsDialog
import link.danb.launcher.activities.details.ActivityDetailsViewModel
import link.danb.launcher.activities.hidden.HiddenAppsDialog
import link.danb.launcher.activities.hidden.HiddenAppsViewModel
import link.danb.launcher.components.UserActivity
import link.danb.launcher.components.UserActivityNavType
import link.danb.launcher.components.UserShortcut
import link.danb.launcher.components.UserShortcutCreator
import link.danb.launcher.database.ActivityData
import link.danb.launcher.extensions.boundsOnScreen
import link.danb.launcher.extensions.makeScaleUpAnimation
import link.danb.launcher.gestures.GestureActivityIconStore
import link.danb.launcher.gestures.GestureContract
import link.danb.launcher.gestures.GestureIconView
import link.danb.launcher.profiles.Profile
import link.danb.launcher.profiles.ProfileManager
import link.danb.launcher.settings.SettingsViewModel
import link.danb.launcher.shortcuts.PinShortcutsDialog
import link.danb.launcher.shortcuts.PinShortcutsViewModel
import link.danb.launcher.shortcuts.ShortcutManager
import link.danb.launcher.ui.LauncherTile
import link.danb.launcher.ui.Widget
import link.danb.launcher.ui.theme.LauncherTheme
import link.danb.launcher.widgets.AppWidgetSetupActivityResultContract
import link.danb.launcher.widgets.WidgetManager
import link.danb.launcher.widgets.WidgetSizeUtil
import link.danb.launcher.widgets.WidgetsViewModel
import link.danb.launcher.widgets.dialog.PinWidgetsDialog
import link.danb.launcher.widgets.dialog.PinWidgetsViewModel

val LocalUseMonochromeIcons: ProvidableCompositionLocal<Boolean> = compositionLocalOf { false }

@Serializable data object Home

@Serializable data class MoreActions(val profile: Profile)

@Serializable data class ActivityDetails(val userActivity: UserActivity)

@Serializable data class PinShortcuts(val profile: Profile)

@Serializable data class PinWidgets(val profile: Profile)

@Serializable data class HiddenApps(val profile: Profile)

@AndroidEntryPoint
class LauncherFragment : Fragment() {

  private val activitiesViewModel: ActivitiesViewModel by activityViewModels()
  private val activityDetailsViewModel: ActivityDetailsViewModel by activityViewModels()
  private val hiddenAppsViewModel: HiddenAppsViewModel by activityViewModels()
  private val launcherViewModel: LauncherViewModel by activityViewModels()
  private val pinShortcutsViewModel: PinShortcutsViewModel by activityViewModels()
  private val pinWidgetsViewModel: PinWidgetsViewModel by activityViewModels()
  private val settingsViewModel: SettingsViewModel by activityViewModels()
  private val widgetsViewModel: WidgetsViewModel by activityViewModels()

  @Inject lateinit var activityManager: ActivityManager
  @Inject lateinit var gestureActivityIconStore: GestureActivityIconStore
  @Inject lateinit var profileManager: ProfileManager
  @Inject lateinit var shortcutManager: ShortcutManager
  @Inject lateinit var widgetManager: WidgetManager
  @Inject lateinit var widgetSizeUtil: WidgetSizeUtil

  private lateinit var iconLaunchView: View
  private lateinit var gestureIconView: GestureIconView

  private var gestureActivity: UserActivity? by mutableStateOf(null)

  @RequiresApi(Build.VERSION_CODES.Q)
  private val onNewIntentListener: Consumer<Intent> = Consumer { intent ->
    val gestureContract =
      GestureContract.fromIntent(intent) { profileManager.getProfile(it) } ?: return@Consumer

    val data =
      gestureActivityIconStore.getActivityIconState(gestureContract.userActivity) ?: return@Consumer

    gestureActivity = data.userActivity
    gestureIconView.animateNavigationGesture(
      gestureContract,
      data.boundsInRoot.toAndroidRectF(),
      data.launcherIconData,
      settingsViewModel.useMonochromeIcons.value,
    )
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
      view.addView(gestureIconView, 0)
    }

    view.findViewById<ComposeView>(R.id.compose_view).setContent {
      LauncherTheme {
        val useMonochromeIcons by settingsViewModel.useMonochromeIcons.collectAsStateWithLifecycle()
        CompositionLocalProvider(LocalUseMonochromeIcons provides useMonochromeIcons) {
          val bottomBarActions by launcherViewModel.bottomBarActions.collectAsStateWithLifecycle()

          val navController = rememberNavController()

          NavHost(navController, startDestination = Home) {
            composable<Home> {
              Scaffold(
                bottomBar = {
                  val profile by launcherViewModel.profile.collectAsStateWithLifecycle()
                  val profiles by profileManager.profiles.collectAsStateWithLifecycle(emptyMap())
                  val searchQuery by launcherViewModel.searchQuery.collectAsStateWithLifecycle()
                  LauncherBottomBar(
                    profile,
                    profiles,
                    bottomBarActions,
                    onChangeProfile = { newProfile, profileState ->
                      profileManager.setProfileState(newProfile, profileState)
                      launcherViewModel.setProfile(newProfile)
                    },
                    searchQuery = searchQuery,
                    onSearchGo = { launchFirstItem() },
                    onMoreActionsClick = { navController.navigate(MoreActions(profile)) },
                    onSearchChange = { launcherViewModel.setSearchQuery(it) },
                    onSearchCancel = { launcherViewModel.setSearchQuery(null) },
                    onSearchFabClick = { onFabClick() },
                  )
                },
                containerColor = Color.Transparent,
                content = { paddingValues ->
                  var isScrollEnabled by remember { mutableStateOf(true) }
                  val items by
                    launcherViewModel.viewItems.collectAsStateWithLifecycle(persistentListOf())
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
                          is GroupHeaderViewItem -> GridItemSpan(maxLineSpan)
                          else -> GridItemSpan(1)
                        }
                      },
                      key = { item -> "${item::class.qualifiedName}:${item.key}" },
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
                            setHeight = {
                              widgetsViewModel.setHeight(item.widgetData.widgetId, it)
                            },
                            configure = {
                              widgetManager.startConfigurationActivity(
                                requireActivity(),
                                it,
                                item.widgetData.widgetId,
                              )
                            },
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
                          LauncherTile(
                            data = item.launcherTileData,
                            modifier = Modifier.animateItem(),
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
                          LauncherTile(
                            data = item.launcherTileData,
                            modifier = Modifier.animateItem(),
                            style =
                              MaterialTheme.typography.labelMedium.copy(
                                color = Color.White,
                                shadow = Shadow(color = Color.Black, blurRadius = 8f),
                              ),
                            onClick = { launchActivity(it, item.userActivity) },
                            onLongClick = {
                              navController.navigate(ActivityDetails(item.userActivity))
                            },
                            hide = item.userActivity == gestureActivity,
                            onPlace = {
                              if (it == null) {
                                (item.userActivity)
                                gestureActivityIconStore.clearActivityState(item.userActivity)
                              } else {
                                gestureActivityIconStore.setActivityState(
                                  item.userActivity,
                                  item.launcherTileData.launcherIconData,
                                  it,
                                )
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
            }

            dialog<MoreActions> { backStackEntry ->
              val profile = backStackEntry.toRoute<MoreActions>().profile

              MoreActionsDialog(
                isShowing = true,
                actions = bottomBarActions,
                onActionClick = { action ->
                  when (action) {
                    BottomBarAction.Type.PIN_SHORTCUT -> {
                      navController.navigateUp()
                      navController.navigate(PinShortcuts(profile))
                    }
                    BottomBarAction.Type.PIN_WIDGET -> {
                      navController.navigateUp()
                      navController.navigate(PinWidgets(profile))
                    }
                    BottomBarAction.Type.SHOW_HIDDEN_APPS -> {
                      navController.navigateUp()
                      navController.navigate(HiddenApps(profile))
                    }
                    BottomBarAction.Type.TOGGLE_MONOCHROME -> {
                      settingsViewModel.setUseMonochromeIcons(!useMonochromeIcons)
                    }
                  }
                },
                onDismissRequest = { navController.navigateUp() },
              )
            }

            dialog<ActivityDetails>(
              typeMap = mapOf(typeOf<UserActivity>() to UserActivityNavType)
            ) { backStackEntry ->
              val userActivity = backStackEntry.toRoute<ActivityDetails>().userActivity
              val activityDetails by
                remember { activityDetailsViewModel.getActivityDetails(userActivity) }
                  .collectAsStateWithLifecycle(null)

              ActivityDetailsDialog(
                activityDetails,
                onDismissRequest = { navController.navigateUp() },
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

            dialog<HiddenApps> { backStackEntry ->
              val profile = backStackEntry.toRoute<HiddenApps>().profile
              val viewData by
                remember { hiddenAppsViewModel.getHiddenApps(profile) }
                  .collectAsStateWithLifecycle(null)

              HiddenAppsDialog(
                isShowing = true,
                viewData = viewData,
                onClick = { offset, item -> launchActivity(offset, item) },
                onLongClick = { _, item ->
                  navController.navigateUp()
                  navController.navigate(ActivityDetails(item))
                },
                onDismissRequest = { navController.navigateUp() },
              )
            }

            dialog<PinShortcuts> { backStackEntry ->
              val profile = backStackEntry.toRoute<PinShortcuts>().profile
              val viewData by
                remember { pinShortcutsViewModel.getPinShortcutsViewData(profile) }
                  .collectAsStateWithLifecycle(null)

              PinShortcutsDialog(
                isShowing = true,
                viewData = viewData,
                onClick = { _, item -> launchShortcutCreator(item) },
                onDismissRequest = { navController.navigateUp() },
              )
            }

            dialog<PinWidgets> { backStackEntry ->
              val profile = backStackEntry.toRoute<PinWidgets>().profile
              val viewData by
                remember { pinWidgetsViewModel.getPinWidgetsViewData(profile) }
                  .collectAsStateWithLifecycle(null)

              PinWidgetsDialog(
                isShowing = true,
                viewData = viewData,
                onClick = { item -> bindWidget(item) },
                onDismissRequest = { navController.navigateUp() },
              )
            }
          }
        }
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
      launcherViewModel.viewItems.value.filterIsInstance<ActivityViewItem>().firstOrNull()
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
        .putExtra(Intent.EXTRA_USER, profileManager.getUserHandle(userActivity.profile))
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
    if (launcherViewModel.searchQuery.value != null) {
      launcherViewModel.setSearchQuery(null)
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
