package link.danb.launcher

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
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.toAndroidRectF
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.util.Consumer
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlin.getValue
import link.danb.launcher.activities.ActivityManager
import link.danb.launcher.activities.details.ActivityDetailsDialog
import link.danb.launcher.activities.hidden.HiddenAppsDialog
import link.danb.launcher.apps.rememberAppsLauncher
import link.danb.launcher.components.UserActivity
import link.danb.launcher.components.UserShortcutCreator
import link.danb.launcher.gestures.GestureActivityIconStore
import link.danb.launcher.gestures.GestureContract
import link.danb.launcher.gestures.GestureIconView
import link.danb.launcher.profiles.Profile
import link.danb.launcher.profiles.ProfileManager
import link.danb.launcher.shortcuts.PinShortcutsDialog
import link.danb.launcher.shortcuts.ShortcutManager
import link.danb.launcher.ui.LauncherIcon
import link.danb.launcher.ui.LauncherTile
import link.danb.launcher.ui.Widget
import link.danb.launcher.ui.predictiveBackScaling
import link.danb.launcher.ui.theme.LauncherTheme
import link.danb.launcher.widgets.WidgetManager
import link.danb.launcher.widgets.WidgetSizeUtil
import link.danb.launcher.widgets.WidgetsViewModel
import link.danb.launcher.widgets.dialog.PinWidgetsDialog

@AndroidEntryPoint
class LauncherFragment : Fragment() {

  private val launcherViewModel: LauncherViewModel by activityViewModels()

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
      GestureContract.fromIntent(intent) { checkNotNull(profileManager.getProfile(it)) }
        ?: return@Consumer

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
        modifier = Modifier.predictiveBackScaling(48.dp),
        launcherViewModel = launcherViewModel,
        changeProfile = { newProfile, isEnabled ->
          profileManager.setProfileEnabled(newProfile, isEnabled)
          launcherViewModel.setProfile(newProfile)
        },
        configureWidget = { view, widgetId ->
          widgetManager.startConfigurationActivity(requireActivity(), view, widgetId)
        },
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
        launchShortcutCreator = this::launchShortcutCreator,
        gestureActivityProvider = { gestureActivity },
      )
    }
    return view
  }

  override fun onDestroy() {
    super.onDestroy()

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
      requireActivity().removeOnNewIntentListener(onNewIntentListener)
    }
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
  modifier: Modifier = Modifier,
  launcherViewModel: LauncherViewModel = hiltViewModel(),
  widgetsViewModel: WidgetsViewModel = hiltViewModel(),
  changeProfile: (Profile, Boolean) -> Unit,
  configureWidget: (View, Int) -> Unit,
  onPlaceTile: (Rect?, ActivityViewItem) -> Unit,
  launchShortcutCreator: (UserShortcutCreator) -> Unit,
  gestureActivityProvider: () -> UserActivity?,
) {
  val useMonochromeIcons by launcherViewModel.useMonochromeIcons.collectAsStateWithLifecycle()
  LauncherTheme(useMonochromeIcons = useMonochromeIcons) {
    var showPinShortcuts by remember { mutableStateOf(false) }
    var showPinWidgets by remember { mutableStateOf(false) }
    var showHiddenApps by remember { mutableStateOf(false) }
    var showMoreActions by remember { mutableStateOf(false) }
    var showActivityDetailsFor: UserActivity? by remember { mutableStateOf(null) }

    val appsLauncher = rememberAppsLauncher()

    val profile by launcherViewModel.profile.collectAsStateWithLifecycle()
    Scaffold(
      modifier = modifier,
      bottomBar = {
        val searchItemBounds =
          with(LocalDensity.current) {
            val iconSize = dimensionResource(R.dimen.launcher_icon_size).toPx()
            Rect(8.dp.toPx(), 32.dp.toPx(), iconSize, iconSize)
          }
        LauncherBottomBar(
          launcherViewModel,
          onChangeProfile = changeProfile,
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
                    Box(Modifier.onGloballyPositioned { onPlaceTile(it.boundsInRoot(), item) }) {
                      if (item.userActivity == gestureActivityProvider()) {
                        Spacer(Modifier.size(dimensionResource(R.dimen.launcher_icon_size)))
                      } else {
                        LauncherIcon(
                          item.launcherTileData.launcherIconData,
                          Modifier.size(dimensionResource(R.dimen.launcher_icon_size)),
                          isPressed = isPressed,
                        )
                      }
                    }
                    DisposableEffect("clear_position") { onDispose { onPlaceTile(null, item) } }
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
        onShortcutCreatorClick = { _, item -> launchShortcutCreator(item) },
      )
    }
  }
}
