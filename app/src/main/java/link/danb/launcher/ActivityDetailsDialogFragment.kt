package link.danb.launcher

import android.appwidget.AppWidgetHost
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Intent
import android.content.pm.LauncherApps
import android.content.pm.LauncherApps.ShortcutQuery
import android.net.Uri
import android.os.Bundle
import android.os.UserHandle
import android.os.UserManager
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
import link.danb.launcher.list.*
import link.danb.launcher.model.LauncherActivityData
import link.danb.launcher.model.LauncherShortcutData
import link.danb.launcher.model.LauncherViewModel
import link.danb.launcher.model.ShortcutViewModel
import link.danb.launcher.model.TileViewData
import link.danb.launcher.utils.getBoundsOnScreen
import link.danb.launcher.utils.getParcelableCompat
import link.danb.launcher.utils.makeClipRevealAnimation
import link.danb.launcher.widgets.AppWidgetSetupActivityResultContract
import link.danb.launcher.widgets.AppWidgetSetupActivityResultContract.AppWidgetSetupInput
import link.danb.launcher.widgets.AppWidgetViewProvider
import link.danb.launcher.widgets.WidgetViewModel
import javax.inject.Inject

@AndroidEntryPoint
class ActivityDetailsDialogFragment : BottomSheetDialogFragment() {

    private val launcherViewModel: LauncherViewModel by activityViewModels()
    private val widgetViewModel: WidgetViewModel by activityViewModels()
    private val shortcutViewModel: ShortcutViewModel by activityViewModels()

    @Inject
    lateinit var appWidgetHost: AppWidgetHost

    @Inject
    lateinit var appWidgetManager: AppWidgetManager

    @Inject
    lateinit var appWidgetViewProvider: AppWidgetViewProvider

    private val launcherActivity by lazy {
        val component: ComponentName = arguments?.getParcelableCompat(COMPONENT_ARGUMENT)!!
        val user: UserHandle = arguments?.getParcelableCompat(USER_ARGUMENT)!!

        launcherViewModel.launcherActivities.value.first {
            it.component == component && it.user == user
        }
    }

    private val launcherApps: LauncherApps by lazy {
        requireContext().getSystemService(LauncherApps::class.java)
    }

    private val userManager: UserManager by lazy {
        requireContext().getSystemService(UserManager::class.java)
    }

    private val bindWidgetActivityLauncher =
        registerForActivityResult(AppWidgetSetupActivityResultContract()) {
            if (it.success) {
                Toast.makeText(context, R.string.pinned_widget, Toast.LENGTH_SHORT).show()
                dismiss()
            } else {
                Toast.makeText(context, it.errorMessage, Toast.LENGTH_SHORT).show()
            }
            widgetViewModel.refresh()
        }

    private val activityHeaderListener = object : ActivityHeaderListener {
        override fun onUninstallButtonClick(view: View, viewItem: ActivityHeaderViewItem) {
            val packageName = viewItem.launcherActivityData.component.packageName
            view.context.startActivity(
                Intent(Intent.ACTION_DELETE).setData(Uri.parse("package:$packageName"))
                    .putExtra(Intent.EXTRA_USER, viewItem.launcherActivityData.user)
            )
            dismiss()
        }

        override fun onSettingsButtonClick(view: View, viewItem: ActivityHeaderViewItem) {
            launcherApps.startAppDetailsActivity(
                viewItem.launcherActivityData.component,
                viewItem.launcherActivityData.user,
                view.getBoundsOnScreen(),
                view.makeClipRevealAnimation()
            )
            dismiss()
        }

        override fun onVisibilityButtonClick(view: View, viewItem: ActivityHeaderViewItem) {
            launcherViewModel.setVisibility(
                viewItem.launcherActivityData,
                !launcherViewModel.isVisible(viewItem.launcherActivityData)
            )
            dismiss()
        }
    }

    private val widgetPreviewListener = WidgetPreviewListener { _, widgetPreviewViewItem ->
        bindWidgetActivityLauncher.launch(
            AppWidgetSetupInput(widgetPreviewViewItem.providerInfo, launcherActivity.user)
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
            ActivityHeaderViewBinder(this, activityHeaderListener),
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

        val items = mutableListOf<ViewItem>(ActivityHeaderViewItem(launcherActivity))

        if (!userManager.isQuietModeEnabled(launcherActivity.user)) {
            val shortcuts = launcherApps.getShortcuts(
                ShortcutQuery().setQueryFlags(
                    ShortcutQuery.FLAG_MATCH_DYNAMIC or ShortcutQuery.FLAG_MATCH_MANIFEST
                ).setPackage(launcherActivity.component.packageName), launcherActivity.user
            )

            shortcuts?.forEach { shortcut ->
                items.add(CardTileViewItem(LauncherShortcutData(launcherApps, shortcut)))
            }
        }

        appWidgetManager.getInstalledProvidersForPackage(
            launcherActivity.component.packageName, launcherActivity.user
        ).forEach { items.add(WidgetPreviewViewItem(it, launcherActivity.user)) }

        adapter.submitList(items as List<ViewItem>?)

        return recyclerView
    }

    private fun onTileClick(view: View, tileViewData: TileViewData) = when (tileViewData) {
        is LauncherActivityData -> Unit
        is LauncherShortcutData -> {
            launcherApps.startShortcut(
                tileViewData.shortcutInfo, view.getBoundsOnScreen(), view.makeClipRevealAnimation()
            )
            dismiss()
        }
    }

    private fun onTileLongClick(tileViewData: TileViewData) = when (tileViewData) {
        is LauncherActivityData -> Unit
        is LauncherShortcutData -> {
            shortcutViewModel.pinShortcut(tileViewData.shortcutInfo)
            Toast.makeText(context, R.string.pinned_shortcut, Toast.LENGTH_SHORT).show()
        }
    }

    companion object {
        const val TAG = "app_options_dialog_fragment"

        private const val COMPONENT_ARGUMENT: String = "name"
        private const val USER_ARGUMENT: String = "user"

        fun newInstance(launcherActivityData: LauncherActivityData): ActivityDetailsDialogFragment {
            return ActivityDetailsDialogFragment().apply {
                arguments = Bundle().apply {
                    putParcelable(COMPONENT_ARGUMENT, launcherActivityData.component)
                    putParcelable(USER_ARGUMENT, launcherActivityData.user)
                }
            }
        }
    }
}
