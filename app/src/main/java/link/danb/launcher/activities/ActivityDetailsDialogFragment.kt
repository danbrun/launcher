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
import kotlinx.coroutines.launch
import link.danb.launcher.R
import link.danb.launcher.tiles.ActivityTileData
import link.danb.launcher.icons.LauncherIconCache
import link.danb.launcher.tiles.ShortcutTileData
import link.danb.launcher.shortcuts.ShortcutsViewModel
import link.danb.launcher.tiles.CardTileViewBinder
import link.danb.launcher.tiles.TileData
import link.danb.launcher.tiles.TileViewItem
import link.danb.launcher.ui.ViewBinderAdapter
import link.danb.launcher.ui.ViewItem
import link.danb.launcher.profiles.ProfilesModel
import link.danb.launcher.extensions.getBoundsOnScreen
import link.danb.launcher.extensions.getParcelableCompat
import link.danb.launcher.extensions.makeClipRevealAnimation
import link.danb.launcher.extensions.setSpanSizeProvider
import link.danb.launcher.widgets.AppWidgetSetupActivityResultContract
import link.danb.launcher.widgets.AppWidgetSetupActivityResultContract.AppWidgetSetupInput
import link.danb.launcher.widgets.AppWidgetViewProvider
import link.danb.launcher.widgets.WidgetPreviewViewBinder
import link.danb.launcher.widgets.WidgetPreviewViewItem
import link.danb.launcher.widgets.WidgetsViewModel
import javax.inject.Inject

@AndroidEntryPoint
class ActivityDetailsDialogFragment : BottomSheetDialogFragment() {

    private val activitiesViewModel: ActivitiesViewModel by activityViewModels()
    private val widgetsViewModel: WidgetsViewModel by activityViewModels()
    private val shortcutsViewModel: ShortcutsViewModel by activityViewModels()

    @Inject
    lateinit var appWidgetHost: AppWidgetHost

    @Inject
    lateinit var appWidgetManager: AppWidgetManager

    @Inject
    lateinit var appWidgetViewProvider: AppWidgetViewProvider

    @Inject
    lateinit var launcherApps: LauncherApps

    @Inject
    lateinit var launcherIconCache: LauncherIconCache

    @Inject
    lateinit var profilesModel: ProfilesModel

    private val launcherActivity by lazy {
        val component: ComponentName = arguments?.getParcelableCompat(COMPONENT_ARGUMENT)!!
        val user: UserHandle = arguments?.getParcelableCompat(USER_ARGUMENT)!!

        activitiesViewModel.launcherActivities.value.first {
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
            widgetsViewModel.refresh()
        }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        super.onCreateView(inflater, container, savedInstanceState)

        val recyclerView = inflater.inflate(
            R.layout.recycler_view_dialog_fragment, container, false
        ) as RecyclerView

        val adapter = ViewBinderAdapter(
            ActivityHeaderViewBinder(
                ::onVisibilityButtonClick, ::onUninstallButtonClick, ::onSettingsButtonClick
            ),
            CardTileViewBinder(::onTileClick, ::onTileLongClick),
            WidgetPreviewViewBinder(appWidgetViewProvider, ::onWidgetPreviewClick),
        )

        val gridLayoutManager = GridLayoutManager(
            context, requireContext().resources.getInteger(R.integer.launcher_columns)
        ).setSpanSizeProvider { position, spanCount ->
            when (adapter.currentList[position]) {
                is ActivityHeaderViewItem, is WidgetPreviewViewItem -> spanCount
                else -> 1
            }
        }

        recyclerView.apply {
            this.adapter = adapter
            layoutManager = gridLayoutManager
        }

        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                profilesModel.activeProfile.collect { adapter.submitList(getViewItems(it)) }
            }
        }

        return recyclerView
    }

    private fun getViewItems(activeProfile: UserHandle): List<ViewItem> = buildList {
        add(ActivityHeaderViewItem(launcherActivity, launcherIconCache.get(launcherActivity.info)))

        shortcutsViewModel.pinnedShortcuts.value.filter { it.userHandle == profilesModel.activeProfile.value }

        val shortcuts = shortcutsViewModel.getShortcuts(
            launcherActivity.info.componentName.packageName, activeProfile
        ).map {
            TileViewItem.cardTileViewItem(
                ShortcutTileData(it), it.shortLabel!!, launcherIconCache.get(it)
            )
        }

        addAll(shortcuts)

        val widgets = appWidgetManager.getInstalledProvidersForPackage(
            launcherActivity.info.componentName.packageName, launcherActivity.info.user
        ).map { WidgetPreviewViewItem(it, launcherActivity.info.user) }

        addAll(widgets)
    }

    private fun onUninstallButtonClick(view: View, viewItem: ActivityHeaderViewItem) {
        val packageName = viewItem.data.info.componentName.packageName
        view.context.startActivity(
            Intent(Intent.ACTION_DELETE).setData(Uri.parse("package:$packageName"))
                .putExtra(Intent.EXTRA_USER, viewItem.data.info.user)
        )
        dismiss()
    }

    private fun onSettingsButtonClick(view: View, viewItem: ActivityHeaderViewItem) {
        launcherApps.startAppDetailsActivity(
            viewItem.data.info.componentName,
            viewItem.data.info.user,
            view.getBoundsOnScreen(),
            view.makeClipRevealAnimation()
        )
        dismiss()
    }

    private fun onVisibilityButtonClick(view: View, viewItem: ActivityHeaderViewItem) {
        activitiesViewModel.setIsHidden(viewItem.data.info, !viewItem.data.metadata.isHidden)
        dismiss()
    }

    private fun onTileClick(view: View, tileData: TileData) = when (tileData) {
        is ActivityTileData -> throw NotImplementedError()
        is ShortcutTileData -> {
            launcherApps.startShortcut(
                tileData.info, view.getBoundsOnScreen(), view.makeClipRevealAnimation()
            )
            dismiss()
        }
    }

    private fun onTileLongClick(view: View, tileData: TileData) = when (tileData) {
        is ActivityTileData -> throw NotImplementedError()
        is ShortcutTileData -> {
            shortcutsViewModel.pinShortcut(tileData.info)
            Toast.makeText(context, R.string.pinned_shortcut, Toast.LENGTH_SHORT).show()
        }
    }

    private fun onWidgetPreviewClick(view: View, widgetPreviewViewItem: WidgetPreviewViewItem) {
        bindWidgetActivityLauncher.launch(
            AppWidgetSetupInput(widgetPreviewViewItem.providerInfo, launcherActivity.info.user)
        )
    }

    companion object {
        const val TAG = "app_options_dialog_fragment"

        private const val COMPONENT_ARGUMENT: String = "name"
        private const val USER_ARGUMENT: String = "user"

        fun newInstance(info: LauncherActivityInfo): ActivityDetailsDialogFragment {
            return ActivityDetailsDialogFragment().apply {
                arguments = Bundle().apply {
                    putParcelable(COMPONENT_ARGUMENT, info.componentName)
                    putParcelable(USER_ARGUMENT, info.user)
                }
            }
        }
    }
}
