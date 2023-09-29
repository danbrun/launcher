package link.danb.launcher.activities

import android.appwidget.AppWidgetHost
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Intent
import android.content.pm.LauncherActivityInfo
import android.content.pm.LauncherApps
import android.net.Uri
import android.os.Bundle
import android.os.UserHandle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.launch
import link.danb.launcher.R
import link.danb.launcher.extensions.boundsOnScreen
import link.danb.launcher.extensions.getParcelableCompat
import link.danb.launcher.extensions.makeScaleUpAnimation
import link.danb.launcher.extensions.setSpanSizeProvider
import link.danb.launcher.icons.LauncherIconCache
import link.danb.launcher.profiles.ProfilesModel
import link.danb.launcher.shortcuts.ShortcutsViewModel
import link.danb.launcher.tiles.ActivityTileData
import link.danb.launcher.tiles.CardTileViewBinder
import link.danb.launcher.tiles.ShortcutTileData
import link.danb.launcher.tiles.TileData
import link.danb.launcher.tiles.TileViewItem
import link.danb.launcher.ui.ViewBinderAdapter
import link.danb.launcher.ui.ViewItem
import link.danb.launcher.widgets.AppWidgetSetupActivityResultContract
import link.danb.launcher.widgets.AppWidgetSetupActivityResultContract.AppWidgetSetupInput
import link.danb.launcher.widgets.AppWidgetViewProvider
import link.danb.launcher.widgets.WidgetPreviewViewBinder
import link.danb.launcher.widgets.WidgetPreviewViewItem
import link.danb.launcher.widgets.WidgetsViewModel

@AndroidEntryPoint
class ActivityDetailsDialogFragment : BottomSheetDialogFragment() {

  private val activitiesViewModel: ActivitiesViewModel by activityViewModels()
  private val widgetsViewModel: WidgetsViewModel by activityViewModels()
  private val shortcutsViewModel: ShortcutsViewModel by activityViewModels()

  @Inject lateinit var appWidgetHost: AppWidgetHost
  @Inject lateinit var appWidgetManager: AppWidgetManager
  @Inject lateinit var appWidgetViewProvider: AppWidgetViewProvider
  @Inject lateinit var launcherApps: LauncherApps
  @Inject lateinit var launcherIconCache: LauncherIconCache
  @Inject lateinit var profilesModel: ProfilesModel

  private val launcherActivity by lazy {
    val component: ComponentName = arguments?.getParcelableCompat(COMPONENT_ARGUMENT)!!
    val user: UserHandle = arguments?.getParcelableCompat(USER_ARGUMENT)!!

    activitiesViewModel.activities.value.first {
      it.info.componentName == component && it.info.user == user
    }
  }

  private val bindWidgetActivityLauncher =
    registerForActivityResult(AppWidgetSetupActivityResultContract()) {
      if (it.success) {
        Toast.makeText(context, R.string.pinned_widget, Toast.LENGTH_SHORT).show()
        dismiss()
      } else {
        Toast.makeText(context, it.errorMessage, Toast.LENGTH_SHORT).show()
      }
      widgetsViewModel.checkForNewWidgets()
    }

  override fun onCreateView(
    inflater: LayoutInflater,
    container: ViewGroup?,
    savedInstanceState: Bundle?,
  ): View {
    super.onCreateView(inflater, container, savedInstanceState)

    val recyclerView =
      inflater.inflate(R.layout.recycler_view_dialog_fragment, container, false) as RecyclerView

    val adapter =
      ViewBinderAdapter(
        ActivityHeaderViewBinder(
          { _, it -> onVisibilityButtonClick(it) },
          ::onUninstallButtonClick,
          ::onSettingsButtonClick
        ),
        CardTileViewBinder(::onTileClick) { _, it -> onTileLongClick(it) },
        WidgetPreviewViewBinder(appWidgetViewProvider) { _, it -> onWidgetPreviewClick(it) },
      )

    recyclerView.apply {
      this.adapter = adapter
      layoutManager =
        GridLayoutManager(
            context,
            requireContext().resources.getInteger(R.integer.launcher_columns)
          )
          .apply {
            setSpanSizeProvider { position, spanCount ->
              when (adapter.currentList[position]) {
                is ActivityHeaderViewItem,
                is WidgetPreviewViewItem -> spanCount
                else -> 1
              }
            }
          }
    }

    viewLifecycleOwner.lifecycleScope.launch {
      repeatOnLifecycle(Lifecycle.State.STARTED) {
        profilesModel.activeProfile.collect { adapter.submitList(getViewItems(it)) }
      }
    }

    return recyclerView
  }

  private suspend fun getViewItems(activeProfile: UserHandle): List<ViewItem> = buildList {
    add(ActivityHeaderViewItem(launcherActivity, launcherIconCache.get(launcherActivity.info)))

    shortcutsViewModel.pinnedShortcuts.value.filter {
      it.userHandle == profilesModel.activeProfile.value
    }

    val shortcuts =
      shortcutsViewModel
        .getShortcuts(launcherActivity.info.componentName.packageName, activeProfile)
        .map {
          TileViewItem.cardTileViewItem(
            ShortcutTileData(it),
            it.shortLabel!!,
            launcherIconCache.get(it)
          )
        }

    addAll(shortcuts)

    val widgets =
      appWidgetManager
        .getInstalledProvidersForPackage(
          launcherActivity.info.componentName.packageName,
          launcherActivity.info.user
        )
        .map { WidgetPreviewViewItem(it, launcherActivity.info.user) }

    addAll(widgets)
  }

  private fun onUninstallButtonClick(view: View, viewItem: ActivityHeaderViewItem) {
    val packageName = viewItem.data.info.componentName.packageName
    view.context.startActivity(
      Intent(Intent.ACTION_DELETE)
        .setData(Uri.parse("package:$packageName"))
        .putExtra(Intent.EXTRA_USER, viewItem.data.info.user)
    )
    dismiss()
  }

  private fun onSettingsButtonClick(view: View, viewItem: ActivityHeaderViewItem) {
    launcherApps.startAppDetailsActivity(
      viewItem.data.info.componentName,
      viewItem.data.info.user,
      view.boundsOnScreen,
      view.makeScaleUpAnimation().toBundle()
    )
    dismiss()
  }

  private fun onVisibilityButtonClick(viewItem: ActivityHeaderViewItem) {
    activitiesViewModel.putMetadataInBackground(
      viewItem.data.data.copy(isHidden = !viewItem.data.data.isHidden)
    )
    dismiss()
  }

  private fun onTileClick(view: View, tileData: TileData) =
    when (tileData) {
      is ActivityTileData -> throw NotImplementedError()
      is ShortcutTileData -> {
        launcherApps.startShortcut(
          tileData.info,
          view.boundsOnScreen,
          view.makeScaleUpAnimation().toBundle()
        )
        dismiss()
      }
    }

  private fun onTileLongClick(tileData: TileData) =
    when (tileData) {
      is ActivityTileData -> throw NotImplementedError()
      is ShortcutTileData -> {
        shortcutsViewModel.pinShortcut(tileData.info)
        Toast.makeText(context, R.string.pinned_shortcut, Toast.LENGTH_SHORT).show()
      }
    }

  private fun onWidgetPreviewClick(widgetPreviewViewItem: WidgetPreviewViewItem) {
    bindWidgetActivityLauncher.launch(
      AppWidgetSetupInput(widgetPreviewViewItem.providerInfo, launcherActivity.info.user)
    )
  }

  companion object {
    const val TAG = "app_options_dialog_fragment"

    private const val COMPONENT_ARGUMENT: String = "name"
    private const val USER_ARGUMENT: String = "user"

    fun newInstance(info: LauncherActivityInfo): ActivityDetailsDialogFragment =
      ActivityDetailsDialogFragment().apply {
        arguments =
          Bundle().apply {
            putParcelable(COMPONENT_ARGUMENT, info.componentName)
            putParcelable(USER_ARGUMENT, info.user)
          }
      }
  }
}
