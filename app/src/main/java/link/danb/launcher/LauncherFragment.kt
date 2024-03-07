package link.danb.launcher

import android.app.SearchManager
import android.appwidget.AppWidgetHost
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.core.util.Consumer
import androidx.core.view.children
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import link.danb.launcher.activities.ActivityDetailsDialogFragment
import link.danb.launcher.activities.ActivityManager
import link.danb.launcher.components.UserShortcut
import link.danb.launcher.database.ActivityData
import link.danb.launcher.database.WidgetData
import link.danb.launcher.extensions.boundsOnScreen
import link.danb.launcher.extensions.makeScaleUpAnimation
import link.danb.launcher.gestures.GestureContract
import link.danb.launcher.gestures.GestureIconView
import link.danb.launcher.profiles.ProfilesModel
import link.danb.launcher.profiles.WorkProfileInstalled
import link.danb.launcher.profiles.WorkProfileManager
import link.danb.launcher.profiles.WorkProfileNotInstalled
import link.danb.launcher.shortcuts.ShortcutManager
import link.danb.launcher.tiles.TileViewItem
import link.danb.launcher.tiles.TransparentTileViewBinder
import link.danb.launcher.tiles.TransparentTileViewHolder
import link.danb.launcher.ui.GroupHeaderViewBinder
import link.danb.launcher.ui.ViewBinderAdapter
import link.danb.launcher.ui.theme.LauncherTheme
import link.danb.launcher.widgets.AppWidgetViewProvider
import link.danb.launcher.widgets.WidgetEditorViewBinder
import link.danb.launcher.widgets.WidgetManager
import link.danb.launcher.widgets.WidgetSizeUtil
import link.danb.launcher.widgets.WidgetViewBinder
import link.danb.launcher.widgets.WidgetsViewModel

@AndroidEntryPoint
class LauncherFragment : Fragment() {

  private val launcherViewModel: LauncherViewModel by activityViewModels()
  private val widgetsViewModel: WidgetsViewModel by activityViewModels()

  @Inject lateinit var activityManager: ActivityManager
  @Inject lateinit var appWidgetHost: AppWidgetHost
  @Inject lateinit var appWidgetViewProvider: AppWidgetViewProvider
  @Inject lateinit var profilesModel: ProfilesModel
  @Inject lateinit var shortcutManager: ShortcutManager
  @Inject lateinit var widgetManager: WidgetManager
  @Inject lateinit var widgetSizeUtil: WidgetSizeUtil
  @Inject lateinit var workProfileManager: WorkProfileManager

  private lateinit var recyclerView: RecyclerView
  private lateinit var gestureIconView: GestureIconView

  private val recyclerAdapter: ViewBinderAdapter by lazy {
    ViewBinderAdapter(
      GroupHeaderViewBinder(),
      TransparentTileViewBinder(this::onTileClick) { _, it -> onTileLongClick(it) },
      WidgetViewBinder(appWidgetViewProvider) { launcherViewModel.isInEditMode.value = true },
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
        { launcherViewModel.isInEditMode.value = false },
      ),
    )
  }

  @RequiresApi(Build.VERSION_CODES.Q)
  private val onNewIntentListener: Consumer<Intent> = Consumer { intent ->
    GestureContract.fromIntent(intent)?.let { maybeAnimateGestureContract(it) }
  }

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

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
      gestureIconView = GestureIconView(view.context)
      view.addView(gestureIconView)
    }

    view.findViewById<ComposeView>(R.id.compose_view).setContent {
      LauncherTheme {
        LauncherLayout(
          launcherList = { windowInsets ->
            LauncherList(windowInsets = windowInsets, recyclerAdapter = recyclerAdapter) {
              recyclerView = it
            }
          },
          bottomBar = {
            BottomBar(
              searchBar = { SearchBar() },
              tabButtonGroups = { ProfileTabs() },
              floatingActionButton = { SearchFab { onFabClick() } },
            )
          },
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
  fun SearchBar() {
    val searchQuery by launcherViewModel.searchQuery.collectAsState()

    if (searchQuery != null) {
      val focusRequester = FocusRequester()

      Row(modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)) {
        TextField(
          value = searchQuery ?: "",
          onValueChange = { launcherViewModel.searchQuery.value = it },
          keyboardOptions = KeyboardOptions(imeAction = ImeAction.Go),
          keyboardActions =
            KeyboardActions(
              onGo = {
                recyclerView.children.firstOrNull()?.performClick()
                launcherViewModel.searchQuery.value = null
              }
            ),
          modifier = Modifier.fillMaxWidth().focusRequester(focusRequester),
        )
      }

      LaunchedEffect(Unit) { focusRequester.requestFocus() }
    }
  }

  @Composable
  fun ProfileTabs() {
    val hasWorkProfile by
      workProfileManager.status.collectAsState(initial = WorkProfileNotInstalled)

    TabButtonGroup {
      val activeProfile by profilesModel.activeProfile.collectAsState()
      val searchQuery by launcherViewModel.searchQuery.collectAsState()

      ShowPersonalTabButton(activeProfile, searchQuery) {
        profilesModel.toggleActiveProfile(showWorkProfile = false)
        launcherViewModel.searchQuery.value = null
      }

      if (hasWorkProfile is WorkProfileInstalled) {
        ShowWorkTabButton(activeProfile, searchQuery) {
          profilesModel.toggleActiveProfile(showWorkProfile = true)
          launcherViewModel.searchQuery.value = null
        }
      }

      ShowSearchTabButton(searchQuery) { launcherViewModel.searchQuery.value = "" }

      val hasHiddenApps by
        activityManager.data
          .map { data -> data.any { it.isHidden && it.userActivity.userHandle == activeProfile } }
          .collectAsState(initial = false)

      MoreActionsTabButton {
        MoreActionsDialogFragment.newInstance(activeProfile, hasHiddenApps)
          .showNow(childFragmentManager, MoreActionsDialogFragment.TAG)
      }
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
        ActivityDetailsDialogFragment.newInstance(tileViewData)
          .show(parentFragmentManager, ActivityDetailsDialogFragment.TAG)
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
}
