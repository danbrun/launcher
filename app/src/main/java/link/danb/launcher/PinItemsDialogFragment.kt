package link.danb.launcher

import android.appwidget.AppWidgetHost
import android.appwidget.AppWidgetManager
import android.content.pm.ApplicationInfo
import android.content.pm.LauncherApps
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Process.myUserHandle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import link.danb.launcher.widgets.WidgetHeaderViewItem.WidgetHeaderViewItemFactory
import link.danb.launcher.tiles.ActivityTileData
import link.danb.launcher.icons.LauncherIconCache
import link.danb.launcher.activities.ActivitiesViewModel
import link.danb.launcher.shortcuts.ShortcutsViewModel
import link.danb.launcher.tiles.CardTileViewBinder
import link.danb.launcher.tiles.TileData
import link.danb.launcher.tiles.TileViewItem
import link.danb.launcher.ui.DialogHeaderViewBinder
import link.danb.launcher.ui.DialogHeaderViewItem
import link.danb.launcher.ui.GroupHeaderViewBinder
import link.danb.launcher.ui.GroupHeaderViewItem
import link.danb.launcher.ui.LoadingSpinnerViewBinder
import link.danb.launcher.ui.LoadingSpinnerViewItem
import link.danb.launcher.ui.ViewBinderAdapter
import link.danb.launcher.ui.ViewItem
import link.danb.launcher.widgets.AppWidgetSetupActivityResultContract
import link.danb.launcher.widgets.AppWidgetSetupActivityResultContract.AppWidgetSetupInput
import link.danb.launcher.widgets.AppWidgetViewProvider
import link.danb.launcher.widgets.WidgetDialogViewModel
import link.danb.launcher.widgets.WidgetHeaderViewBinder
import link.danb.launcher.widgets.WidgetPreviewListener
import link.danb.launcher.widgets.WidgetPreviewViewBinder
import link.danb.launcher.widgets.WidgetPreviewViewItem
import link.danb.launcher.widgets.WidgetsViewModel
import link.danb.launcher.work.WorkProfileViewModel
import javax.inject.Inject

@AndroidEntryPoint
class PinItemsDialogFragment : BottomSheetDialogFragment() {

    private val activitiesViewModel: ActivitiesViewModel by activityViewModels()
    private val shortcutsViewModel: ShortcutsViewModel by activityViewModels()
    private val widgetsViewModel: WidgetsViewModel by activityViewModels()
    private val widgetDialogViewModel: WidgetDialogViewModel by viewModels()
    private val workProfileViewModel: WorkProfileViewModel by activityViewModels()

    @Inject
    lateinit var appWidgetHost: AppWidgetHost

    @Inject
    lateinit var appWidgetManager: AppWidgetManager

    @Inject
    lateinit var appWidgetViewProvider: AppWidgetViewProvider

    @Inject
    lateinit var widgetHeaderViewItemFactory: WidgetHeaderViewItemFactory

    @Inject
    lateinit var launcherIconCache: LauncherIconCache

    @Inject
    lateinit var launcherApps: LauncherApps

    private val packageManager: PackageManager by lazy { requireContext().packageManager }

    private val shortcutActivityLauncher =
        registerForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) {
            if (it.data == null) return@registerForActivityResult
            val pinItemRequest =
                launcherApps.getPinItemRequest(it.data) ?: return@registerForActivityResult
            if (!pinItemRequest.isValid) return@registerForActivityResult
            if (pinItemRequest.requestType != LauncherApps.PinItemRequest.REQUEST_TYPE_SHORTCUT) return@registerForActivityResult
            val info = pinItemRequest.shortcutInfo ?: return@registerForActivityResult

            pinItemRequest.accept()
            shortcutsViewModel.pinShortcut(info)
            Toast.makeText(context, R.string.pinned_shortcut, Toast.LENGTH_SHORT).show()
            dismiss()
        }

    private val bindWidgetActivityLauncher = registerForActivityResult(
        AppWidgetSetupActivityResultContract()
    ) {
        if (it.success) {
            Toast.makeText(context, R.string.pinned_widget, Toast.LENGTH_SHORT).show()
            dismiss()
        } else {
            Toast.makeText(context, it.errorMessage, Toast.LENGTH_SHORT).show()
        }
        widgetsViewModel.refresh()
    }

    private val widgetPreviewListener = WidgetPreviewListener { _, widgetPreviewViewItem ->
        bindWidgetActivityLauncher.launch(
            AppWidgetSetupInput(widgetPreviewViewItem.providerInfo, myUserHandle())
        )
    }

    private val headerItems by lazy {
        listOf(DialogHeaderViewItem(requireContext().getString(R.string.pin_items)))
    }

    private lateinit var widgetListAdapter: ViewBinderAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View? {
        super.onCreateView(inflater, container, savedInstanceState)

        val view = inflater.inflate(R.layout.pin_items_dialog_fragment, container, false)

        widgetListAdapter = ViewBinderAdapter(
            DialogHeaderViewBinder(),
            GroupHeaderViewBinder(),
            LoadingSpinnerViewBinder(),
            CardTileViewBinder({ _, it -> onTileClick(it) }),
            WidgetHeaderViewBinder { widgetDialogViewModel.toggleExpandedPackageName(it.packageName) },
            WidgetPreviewViewBinder(appWidgetViewProvider, widgetPreviewListener)
        )

        val gridLayoutManager = GridLayoutManager(
            context, requireContext().resources.getInteger(R.integer.launcher_columns)
        )
        gridLayoutManager.spanSizeLookup = object : GridLayoutManager.SpanSizeLookup() {
            override fun getSpanSize(position: Int): Int =
                when (widgetListAdapter.currentList[position]) {
                    is TileViewItem -> 1
                    else -> gridLayoutManager.spanCount
                }
        }

        view.findViewById<RecyclerView>(R.id.widget_list).apply {
            layoutManager = gridLayoutManager
            adapter = widgetListAdapter
        }

        widgetListAdapter.submitList(headerItems + listOf(LoadingSpinnerViewItem()))

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.RESUMED) {
                combine(
                    activitiesViewModel.launcherActivities,
                    workProfileViewModel.showWorkActivities,
                    widgetDialogViewModel.expandedPackageNames,
                    this@PinItemsDialogFragment::getViewItems
                ).collect { widgetListAdapter.submitList(it) }
            }
        }

        return view
    }

    private fun onTileClick(tileData: TileData) {
        if (tileData !is ActivityTileData) return

        shortcutActivityLauncher.launch(
            IntentSenderRequest.Builder(
                launcherApps.getShortcutConfigActivityIntent(tileData.info)!!
            ).build()
        )
    }

    private fun getViewItems(
        shortcutActivities: List<ActivitiesViewModel.ActivityData>,
        showWorkActivities: Boolean,
        expandedPackages: Set<String>
    ): List<ViewItem> {
        val items = mutableListOf<ViewItem>()

        items.addAll(headerItems)
        items.add(GroupHeaderViewItem(requireContext().getString(R.string.shortcuts)))

        items.addAll(shortcutActivities.filter { showWorkActivities != (it.info.user == myUserHandle()) }
            .flatMap {
                launcherApps.getShortcutConfigActivityList(
                    it.info.componentName.packageName, it.info.user
                )
            }.sortedBy { it.label.toString().lowercase() }.map {
                TileViewItem.cardTileViewItem(
                    ActivityTileData(it), it.label, launcherIconCache.get(it)
                )
            })

        items.add(GroupHeaderViewItem(requireContext().getString(R.string.widgets)))

        val widgetItems = appWidgetManager.installedProviders.groupBy { it.provider.packageName }
            .mapKeys { launcherApps.getApplicationInfo(it.key, 0, myUserHandle()) }
            .toSortedMap(compareBy<ApplicationInfo> {
                it.loadLabel(packageManager).toString().lowercase()
            }).flatMap { (appInfo, widgets) ->
                mutableListOf<ViewItem>().apply {
                    val isExpanded = expandedPackages.contains(appInfo.packageName)
                    add(widgetHeaderViewItemFactory.create(appInfo, isExpanded))
                    if (isExpanded) {
                        addAll(widgets.map { WidgetPreviewViewItem(it, myUserHandle()) })
                    }
                }
            }

        items.addAll(widgetItems)

        return items.toList()
    }

    companion object {
        const val TAG = "widget_dialog_fragment"
    }
}
