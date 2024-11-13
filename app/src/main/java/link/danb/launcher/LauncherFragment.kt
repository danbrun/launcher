package link.danb.launcher

import android.app.ActivityOptions
import android.app.SearchManager
import android.app.role.RoleManager
import android.content.Intent
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
import androidx.compose.runtime.Composable
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
import androidx.core.content.getSystemService
import androidx.core.graphics.toRect
import androidx.core.util.Consumer
import androidx.core.view.updateLayoutParams
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlin.getValue
import kotlinx.serialization.Serializable
import link.danb.launcher.activities.ActivityManager
import link.danb.launcher.activities.details.ActivityDetailsDialog
import link.danb.launcher.activities.hidden.HiddenAppsDialog
import link.danb.launcher.components.UserActivity
import link.danb.launcher.components.UserShortcut
import link.danb.launcher.components.UserShortcutCreator
import link.danb.launcher.extensions.boundsOnScreen
import link.danb.launcher.extensions.makeScaleUpAnimation
import link.danb.launcher.gestures.GestureActivityIconStore
import link.danb.launcher.gestures.GestureContract
import link.danb.launcher.gestures.GestureIconView
import link.danb.launcher.profiles.Profile
import link.danb.launcher.profiles.ProfileManager
import link.danb.launcher.shortcuts.PinShortcutsDialog
import link.danb.launcher.shortcuts.ShortcutManager
import link.danb.launcher.ui.LauncherTile
import link.danb.launcher.ui.Widget
import link.danb.launcher.ui.theme.LauncherTheme
import link.danb.launcher.widgets.WidgetManager
import link.danb.launcher.widgets.WidgetSizeUtil
import link.danb.launcher.widgets.WidgetsViewModel
import link.danb.launcher.widgets.dialog.PinWidgetsDialog

@Serializable data object Home

@AndroidEntryPoint
class LauncherFragment : Fragment() {

  private val launcherViewModel: LauncherViewModel by activityViewModels()

  @Inject lateinit var activityManager: ActivityManager
  @Inject lateinit var gestureActivityIconStore: GestureActivityIconStore
  @Inject lateinit var profileManager: ProfileManager
  @Inject lateinit var shortcutManager: ShortcutManager
  @Inject lateinit var widgetManager: WidgetManager
  @Inject lateinit var widgetSizeUtil: WidgetSizeUtil

  private val roleManager: RoleManager by lazy {
    checkNotNull(requireContext().getSystemService<RoleManager>())
  }

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
      launcherViewModel.useMonochromeIcons.value,
    )
  }

  private val shortcutActivityLauncher =
    registerForActivityResult(
      ActivityResultContracts.StartIntentSenderForResult(),
      ::onPinShortcutActivityResult,
    )

  private val canRequestHomeRole: Boolean
    get() =
      Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q &&
        roleManager.isRoleAvailable(RoleManager.ROLE_HOME) &&
        !roleManager.isRoleHeld(RoleManager.ROLE_HOME)

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
      Launcher(
        launcherViewModel = launcherViewModel,
        changeProfile = { newProfile, isEnabled ->
          profileManager.setProfileEnabled(newProfile, isEnabled)
          launcherViewModel.setProfile(newProfile)
        },
        configureWidget = { view, widgetId ->
          widgetManager.startConfigurationActivity(requireActivity(), view, widgetId)
        },
        launchFirstItem = this::launchFirstItem,
        onFabClick = this::onFabClick,
        launchShortcut = this::launchShortcut,
        unpinShortcut = this::unpinShortcut,
        launchActivity = this::launchActivity,
        onPlaceTile = { rect, item ->
          if (rect == null) {
            gestureActivityIconStore.clearActivityState(item.userActivity)
          } else {
            gestureActivityIconStore.setActivityState(
              item.userActivity,
              item.launcherTileData.launcherIconData,
              rect,
            )
          }
        },
        openAppSettings = this::openAppSettings,
        pinShortcut = this::pinShortcut,
        launchShortcutCreator = this::launchShortcutCreator,
        gestureActivity = gestureActivity,
      )
    }
    return view
  }

  override fun onResume() {
    super.onResume()

    launcherViewModel.setCanRequestHomeRole(canRequestHomeRole)
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

  private fun openAppSettings(userActivity: UserActivity) {
    activityManager.launchAppDetails(
      userActivity,
      Rect.Zero.toAndroidRectF().toRect(),
      ActivityOptions.makeBasic().toBundle(),
    )
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
}

@Composable
private fun Launcher(
  launcherViewModel: LauncherViewModel = hiltViewModel(),
  widgetsViewModel: WidgetsViewModel = hiltViewModel(),
  changeProfile: (Profile, Boolean) -> Unit,
  configureWidget: (View, Int) -> Unit,
  launchFirstItem: () -> Unit,
  onFabClick: () -> Unit,
  launchShortcut: (Offset, UserShortcut) -> Unit,
  unpinShortcut: (UserShortcut) -> Unit,
  launchActivity: (Offset, UserActivity) -> Unit,
  onPlaceTile: (Rect?, ActivityViewItem) -> Unit,
  openAppSettings: (UserActivity) -> Unit,
  pinShortcut: (UserShortcut) -> Unit,
  launchShortcutCreator: (UserShortcutCreator) -> Unit,
  gestureActivity: UserActivity?,
) {
  val useMonochromeIcons by launcherViewModel.useMonochromeIcons.collectAsStateWithLifecycle()
  LauncherTheme(useMonochromeIcons = useMonochromeIcons) {
    val navController = rememberNavController()

    var showPinShortcuts by remember { mutableStateOf(false) }
    var showPinWidgets by remember { mutableStateOf(false) }
    var showHiddenApps by remember { mutableStateOf(false) }
    var showMoreActions by remember { mutableStateOf(false) }
    var showActivityDetailsFor: UserActivity? by remember { mutableStateOf(null) }

    NavHost(navController, startDestination = Home) {
      composable<Home> {
        val profile by launcherViewModel.profile.collectAsStateWithLifecycle()
        Scaffold(
          bottomBar = {
            val profiles by launcherViewModel.profiles.collectAsStateWithLifecycle()
            val searchQuery by launcherViewModel.searchQuery.collectAsStateWithLifecycle()
            LauncherBottomBar(
              profile,
              profiles,
              onChangeProfile = changeProfile,
              searchQuery = searchQuery,
              onSearchGo = { launchFirstItem() },
              onMoreActionsClick = { showMoreActions = true },
              onSearchChange = { launcherViewModel.setSearchQuery(it) },
              onSearchCancel = { launcherViewModel.setSearchQuery(null) },
              onSearchFabClick = { onFabClick() },
            )
          },
          containerColor = Color.Transparent,
          content = { paddingValues ->
            var isScrollEnabled by remember { mutableStateOf(true) }
            val items by launcherViewModel.viewItems.collectAsStateWithLifecycle()
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
                      setHeight = { widgetsViewModel.setHeight(item.widgetData.widgetId, it) },
                      configure = { configureWidget(it, item.widgetData.widgetId) },
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
                      onLongClick = { showActivityDetailsFor = item.userActivity },
                      hide = item.userActivity == gestureActivity,
                      onPlace = { onPlaceTile(it, item) },
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
            launchActivity = { offset, userActivity ->
              launchActivity(offset, userActivity)
              showHiddenApps = false
            },
            navigateToDetails = { showActivityDetailsFor = it },
            dismiss = { showHiddenApps = false },
          )
        }

        if (showMoreActions) {
          MoreActionsDialog(
            profile = profile,
            pinShortcuts = { showPinShortcuts = true },
            pinWidgets = { showPinWidgets = true },
            shownHiddenApps = { showHiddenApps = true },
          ) {
            showMoreActions = false
          }
        }

        if (showActivityDetailsFor != null) {
          ActivityDetailsDialog(
            showActivityDetailsFor!!,
            dismiss = { showActivityDetailsFor = null },
            onSettings = { openAppSettings(it.userActivity) },
            onShortcutClick = { offset, item -> launchShortcut(offset, item) },
            onShortcutLongClick = { _, item -> pinShortcut(item) },
            onShortcutCreatorClick = { _, item -> launchShortcutCreator(item) },
            onShortcutCreatorLongClick = { _, _ -> },
          )
        }
      }
    }
  }
}
