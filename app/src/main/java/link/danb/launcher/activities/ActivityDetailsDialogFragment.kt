package link.danb.launcher.activities

import android.appwidget.AppWidgetHost
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Intent
import android.content.pm.LauncherActivityInfo
import android.content.pm.LauncherApps
import android.content.pm.LauncherApps.ShortcutQuery
import android.net.Uri
import android.os.Bundle
import android.os.UserHandle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.GridLayoutManager.SpanSizeLookup
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import dagger.hilt.android.AndroidEntryPoint
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
import link.danb.launcher.utils.getBoundsOnScreen
import link.danb.launcher.utils.getParcelableCompat
import link.danb.launcher.utils.isPersonalProfile
import link.danb.launcher.utils.makeClipRevealAnimation
import link.danb.launcher.widgets.AppWidgetSetupActivityResultContract
import link.danb.launcher.widgets.AppWidgetSetupActivityResultContract.AppWidgetSetupInput
import link.danb.launcher.widgets.AppWidgetViewProvider
import link.danb.launcher.widgets.WidgetPreviewListener
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

    private val activityHeaderListener = object : ActivityHeaderListener {
        override fun onUninstallButtonClick(view: View, viewItem: ActivityHeaderViewItem) {
            val packageName = viewItem.data.info.componentName.packageName
            view.context.startActivity(
                Intent(Intent.ACTION_DELETE).setData(Uri.parse("package:$packageName"))
                    .putExtra(Intent.EXTRA_USER, viewItem.data.info.user)
            )
            dismiss()
        }

        override fun onSettingsButtonClick(view: View, viewItem: ActivityHeaderViewItem) {
            launcherApps.startAppDetailsActivity(
                viewItem.data.info.componentName,
                viewItem.data.info.user,
                view.getBoundsOnScreen(),
                view.makeClipRevealAnimation()
            )
            dismiss()
        }

        override fun onVisibilityButtonClick(view: View, viewItem: ActivityHeaderViewItem) {
            activitiesViewModel.setIsHidden(viewItem.data.info, !viewItem.data.metadata.isHidden)
            dismiss()
        }
    }

    private val widgetPreviewListener = WidgetPreviewListener { _, widgetPreviewViewItem ->
        bindWidgetActivityLauncher.launch(
            AppWidgetSetupInput(widgetPreviewViewItem.providerInfo, launcherActivity.info.user)
        )
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        super.onCreateView(inflater, container, savedInstanceState)

        val recyclerView = inflater.inflate(
            R.layout.activity_details_dialog_fragment, container, false
        ) as RecyclerView

        val adapter = ViewBinderAdapter(
            ActivityHeaderViewBinder(activityHeaderListener),
            CardTileViewBinder(this::onTileClick) { _, it -> onTileLongClick(it) },
            WidgetPreviewViewBinder(appWidgetViewProvider, widgetPreviewListener)
        )

        val columns = requireContext().resources.getInteger(R.integer.launcher_columns)

        recyclerView.adapter = adapter
        recyclerView.isNestedScrollingEnabled = true
        recyclerView.layoutManager = GridLayoutManager(
            context, columns
        ).apply {
            spanSizeLookup = object : SpanSizeLookup() {
                override fun getSpanSize(position: Int): Int {
                    return when (adapter.currentList[position]) {
                        is ActivityHeaderViewItem, is WidgetPreviewViewItem -> columns
                        else -> 1
                    }
                }
            }
        }

        val items = mutableListOf<ViewItem>(
            ActivityHeaderViewItem(launcherActivity, launcherIconCache.get(launcherActivity.info))
        )

        if (launcherActivity.info.user.isPersonalProfile() || profilesModel.workProfileData.value.isEnabled) {
            val shortcuts = launcherApps.getShortcuts(
                ShortcutQuery().setQueryFlags(
                    ShortcutQuery.FLAG_MATCH_DYNAMIC or ShortcutQuery.FLAG_MATCH_MANIFEST
                ).setPackage(launcherActivity.info.componentName.packageName),
                launcherActivity.info.user
            )

            shortcuts?.forEach {
                items.add(
                    TileViewItem.cardTileViewItem(
                        ShortcutTileData(it), it.shortLabel!!, launcherIconCache.get(it)
                    )
                )
            }
        }

        appWidgetManager.getInstalledProvidersForPackage(
            launcherActivity.info.componentName.packageName, launcherActivity.info.user
        ).forEach { items.add(WidgetPreviewViewItem(it, launcherActivity.info.user)) }

        adapter.submitList(items as List<ViewItem>?)

        return recyclerView
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

    private fun onTileLongClick(tileData: TileData) = when (tileData) {
        is ActivityTileData -> throw NotImplementedError()
        is ShortcutTileData -> {
            shortcutsViewModel.pinShortcut(tileData.info)
            Toast.makeText(context, R.string.pinned_shortcut, Toast.LENGTH_SHORT).show()
        }
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
