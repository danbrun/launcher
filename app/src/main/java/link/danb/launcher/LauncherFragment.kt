package link.danb.launcher

import android.app.ActivityOptions
import android.app.SearchManager
import android.appwidget.AppWidgetProviderInfo
import android.content.Intent
import android.graphics.Rect
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Process
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.Toast
import androidx.activity.result.ActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ComposeView
import androidx.core.os.BundleCompat
import androidx.core.util.Consumer
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import link.danb.launcher.activities.ActivitiesViewModel
import link.danb.launcher.activities.ActivityManager
import link.danb.launcher.activities.HiddenActivitiesDialogFragment
import link.danb.launcher.activities.details.ActivityDetailsDialog
import link.danb.launcher.activities.details.ActivityDetailsViewModel
import link.danb.launcher.components.UserActivity
import link.danb.launcher.components.UserShortcut
import link.danb.launcher.components.UserShortcutCreator
import link.danb.launcher.database.ActivityData
import link.danb.launcher.database.WidgetData
import link.danb.launcher.extensions.boundsOnScreen
import link.danb.launcher.extensions.makeScaleUpAnimation
import link.danb.launcher.gestures.GestureContract
import link.danb.launcher.gestures.GestureIconView
import link.danb.launcher.profiles.ProfileManager
import link.danb.launcher.shortcuts.ShortcutManager
import link.danb.launcher.tiles.TileViewItem
import link.danb.launcher.tiles.TransparentTileViewBinder
import link.danb.launcher.tiles.TransparentTileViewHolder
import link.danb.launcher.ui.GroupHeaderViewBinder
import link.danb.launcher.ui.ViewBinderAdapter
import link.danb.launcher.ui.theme.LauncherTheme
import link.danb.launcher.widgets.AppWidgetSetupActivityResultContract
import link.danb.launcher.widgets.AppWidgetViewProvider
import link.danb.launcher.widgets.WidgetEditorViewBinder
import link.danb.launcher.widgets.WidgetManager
import link.danb.launcher.widgets.WidgetSizeUtil
import link.danb.launcher.widgets.WidgetViewBinder
import link.danb.launcher.widgets.WidgetsViewModel

@AndroidEntryPoint
class LauncherFragment : Fragment() {

  private val activitiesViewModel: ActivitiesViewModel by activityViewModels()
  private val activityDetailsViewModel: ActivityDetailsViewModel by activityViewModels()
  private val launcherViewModel: LauncherViewModel by activityViewModels()
  private val widgetsViewModel: WidgetsViewModel by activityViewModels()

  @Inject lateinit var activityManager: ActivityManager
  @Inject lateinit var appWidgetViewProvider: AppWidgetViewProvider
  @Inject lateinit var shortcutManager: ShortcutManager
  @Inject lateinit var widgetManager: WidgetManager
  @Inject lateinit var widgetSizeUtil: WidgetSizeUtil
  @Inject lateinit var profileManager: ProfileManager

  private lateinit var recyclerView: RecyclerView
  private lateinit var gestureIconView: GestureIconView

  private val showMoreActionsDialog: MutableStateFlow<Boolean> = MutableStateFlow(false)

  private val recyclerAdapter: ViewBinderAdapter by lazy {
    ViewBinderAdapter(
      GroupHeaderViewBinder(),
      TransparentTileViewBinder(this::onTileClick) { _, it -> onTileLongClick(it) },
      WidgetViewBinder(appWidgetViewProvider) { launcherViewModel.toggleEditMode() },
      WidgetEditorViewBinder(
        appWidgetViewProvider,
        widgetSizeUtil,
        { widgetData: WidgetData, view: View ->
          widgetManager.startConfigurationActivity(requireActivity(), view, widgetData.widgetId)
        },
        { widgetsViewModel.delete(it.widgetId) },
        { widgetsViewModel.moveUp(it.widgetId) },
        { widgetData: WidgetData, height: Int ->
          widgetsViewModel.setHeight(widgetData.widgetId, height)
        },
        { widgetsViewModel.moveDown(it.widgetId) },
        { launcherViewModel.toggleEditMode() },
      ),
    )
  }

  @RequiresApi(Build.VERSION_CODES.Q)
  private val onNewIntentListener: Consumer<Intent> = Consumer { intent ->
    GestureContract.fromIntent(intent)?.let { maybeAnimateGestureContract(it) }
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

    childFragmentManager.setFragmentResultListener(HiddenActivitiesDialogFragment.TAG, this) {
      _,
      data ->
      val showDetailsFor =
        BundleCompat.getParcelable(
          data,
          HiddenActivitiesDialogFragment.EXTRA_SHOW_DETAILS_FOR,
          UserActivity::class.java,
        )
      if (showDetailsFor != null) {
        activityDetailsViewModel.showActivityDetails(showDetailsFor)
      }
    }
  }

  override fun onCreateView(
    inflater: LayoutInflater,
    container: ViewGroup?,
    savedInstanceState: Bundle?,
  ): View {
    val view = inflater.inflate(R.layout.launcher_fragment, container, false) as FrameLayout

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
      gestureIconView = GestureIconView(view.context)
      view.addView(gestureIconView)
    }

    view.findViewById<ComposeView>(R.id.compose_view).setContent {
      LauncherTheme {
        val filter by launcherViewModel.filter.collectAsState()
        val workProfileStatus by profileManager.profiles.collectAsState()
        val activityDetailsData by activityDetailsViewModel.activityDetails.collectAsState(null)

        Scaffold(
          bottomBar = {
            LauncherBottomBar(
              filter,
              workProfileStatus,
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
            LauncherList(paddingValues = paddingValues, recyclerAdapter = recyclerAdapter) {
              recyclerView = it
            }
          },
        )

        MoreActionsDialog(filter)

        ActivityDetailsDialog(
          activityDetailsData,
          onDismissRequest = { activityDetailsViewModel.hideActivityDetails() },
          onToggledPinned = { toggleAppPinned(it) },
          onToggleHidden = { toggleAppHidden(it) },
          onUninstall = { uninstallApp(it.userActivity) },
          onSettings = { openAppSettings(it.userActivity) },
          onShortcutClick = { view, item -> launchShortcut(view, item) },
          onShortcutLongClick = { _, item -> pinShortcut(item) },
          onShortcutCreatorClick = { _, item -> launchShortcutCreator(item) },
          onShortcutCreatorLongClick = { _, _ -> },
          onWidgetPreviewClick = { bindWidget(it) },
        )
      }
    }

    viewLifecycleOwner.lifecycleScope.launch {
      viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.CREATED) {
        launcherViewModel.viewItems.collectLatest { recyclerAdapter.submitList(it) }
      }
    }

    return view
  }

  @Composable
  fun MoreActionsDialog(filter: Filter) {
    if (filter is ProfileFilter) {
      val isShowing by showMoreActionsDialog.collectAsState()

      val hasHiddenApps by
        activityManager.data
          .map { data -> data.any { it.isHidden && it.userActivity.userHandle == filter.profile } }
          .collectAsState(initial = false)

      MoreActionsDialog(
        isShowing = isShowing,
        userHandle = filter.profile,
        hasHiddenApps = hasHiddenApps,
        fragmentManager = childFragmentManager,
        onDismissRequest = { showMoreActionsDialog.value = false },
      )
    }
  }

  override fun onDestroy() {
    super.onDestroy()

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
      requireActivity().removeOnNewIntentListener(onNewIntentListener)
    }
  }

  @RequiresApi(Build.VERSION_CODES.Q)
  private fun maybeAnimateGestureContract(gestureContract: GestureContract) {
    for ((index, item) in recyclerAdapter.currentList.withIndex()) {
      if (
        item is TileViewItem &&
          item.data is ActivityData &&
          item.data.userActivity.componentName.packageName ==
            gestureContract.componentName.packageName &&
          item.data.userActivity.userHandle == gestureContract.userHandle
      ) {
        val viewHolder = recyclerView.findViewHolderForAdapterPosition(index)
        if (viewHolder != null && viewHolder is TransparentTileViewHolder) {
          gestureIconView.animateNavigationGesture(gestureContract, viewHolder.iconView)
          return
        }
      }
    }
  }

  private fun onTileClick(view: View, data: Any) {
    when (data) {
      is ActivityData -> {
        activityManager.launchActivity(
          data.userActivity,
          view.boundsOnScreen,
          view.makeScaleUpAnimation().toBundle(),
        )
        if (launcherViewModel.filter.value is SearchFilter) {
          launcherViewModel.setFilter(ProfileFilter(Process.myUserHandle()))
        }
      }
      is UserShortcut -> {
        shortcutManager.launchShortcut(
          data,
          view.boundsOnScreen,
          view.makeScaleUpAnimation().toBundle(),
        )
      }
      else -> throw NotImplementedError()
    }
  }

  private fun onTileLongClick(tileViewData: Any) {
    when (tileViewData) {
      is ActivityData -> {
        activityDetailsViewModel.showActivityDetails(tileViewData.userActivity)
      }
      is UserShortcut -> {
        MaterialAlertDialogBuilder(requireContext())
          .setTitle(R.string.unpin_shortcut)
          .setPositiveButton(android.R.string.ok) { _, _ ->
            Toast.makeText(context, R.string.unpinned_shortcut, Toast.LENGTH_SHORT).show()
            shortcutManager.pinShortcut(tileViewData, isPinned = false)
          }
          .setNegativeButton(android.R.string.cancel, null)
          .show()
      }
      else -> throw NotImplementedError()
    }
  }

  private fun launchFirstItem() {
    val index = recyclerAdapter.currentList.indexOfFirst { it is TileViewItem }
    if (index > 0) {
      recyclerView.findViewHolderForAdapterPosition(index)?.itemView?.performClick()
    }
    launcherViewModel.setFilter(ProfileFilter(Process.myUserHandle()))
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
    activityManager.launchAppDetails(userActivity, Rect(), ActivityOptions.makeBasic().toBundle())
  }

  private fun toggleAppPinned(activityData: ActivityData) {
    activitiesViewModel.setMetadata(activityData.copy(isPinned = !activityData.isPinned))
  }

  private fun toggleAppHidden(activityData: ActivityData) {
    activitiesViewModel.setMetadata(activityData.copy(isHidden = !activityData.isHidden))
  }

  private fun launchShortcut(view: View, userShortcut: UserShortcut) {
    shortcutManager.launchShortcut(
      userShortcut,
      view.boundsOnScreen,
      view.makeScaleUpAnimation().toBundle(),
    )
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
