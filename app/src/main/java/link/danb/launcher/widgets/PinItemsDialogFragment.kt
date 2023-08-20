package link.danb.launcher.widgets

import android.appwidget.AppWidgetHost
import android.appwidget.AppWidgetManager
import android.content.pm.ApplicationInfo
import android.content.pm.LauncherActivityInfo
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
import link.danb.launcher.R
import link.danb.launcher.list.*
import link.danb.launcher.list.WidgetHeaderViewItem.WidgetHeaderViewItemFactory
import link.danb.launcher.model.ActivityTileData
import link.danb.launcher.model.LauncherIconProvider
import link.danb.launcher.model.LauncherViewModel
import link.danb.launcher.model.ShortcutViewModel
import link.danb.launcher.model.TileData
import link.danb.launcher.widgets.AppWidgetSetupActivityResultContract.AppWidgetSetupInput
import javax.inject.Inject

@AndroidEntryPoint
class PinItemsDialogFragment : BottomSheetDialogFragment() {

    private val launcherViewModel: LauncherViewModel by activityViewModels()
    private val shortcutViewModel: ShortcutViewModel by activityViewModels()
    private val widgetViewModel: WidgetViewModel by activityViewModels()
    private val widgetDialogViewModel: WidgetDialogViewModel by viewModels()

    @Inject
    lateinit var appWidgetHost: AppWidgetHost

    @Inject
    lateinit var appWidgetManager: AppWidgetManager

    @Inject
    lateinit var appWidgetViewProvider: AppWidgetViewProvider

    @Inject
    lateinit var widgetHeaderViewItemFactory: WidgetHeaderViewItemFactory

    @Inject
    lateinit var launcherIconProvider: LauncherIconProvider

    private val launcherApps: LauncherApps by lazy {
        requireContext().getSystemService(LauncherApps::class.java)
    }

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
            shortcutViewModel.pinShortcut(info)
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
        widgetViewModel.refresh()
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
                    launcherViewModel.shortcutActivities,
                    launcherViewModel.showWorkActivities,
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
        shortcutActivities: List<LauncherActivityInfo>,
        showWorkActivities: Boolean,
        expandedPackages: Set<String>
    ): List<ViewItem> {
        val items = mutableListOf<ViewItem>()

        items.addAll(headerItems)
        items.add(GroupHeaderViewItem(requireContext().getString(R.string.shortcuts)))

        items.addAll(shortcutActivities.filter { showWorkActivities != (it.user == myUserHandle()) }
            .sortedBy { it.label.toString().lowercase() }.map {
                TileViewItem.cardTileViewItem(
                    ActivityTileData(it), it.label, launcherIconProvider.get(it)
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
