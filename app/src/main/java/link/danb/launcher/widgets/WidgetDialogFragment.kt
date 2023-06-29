package link.danb.launcher.widgets

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
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import link.danb.launcher.R
import link.danb.launcher.list.*
import link.danb.launcher.list.WidgetHeaderViewItem.WidgetHeaderViewItemFactory
import link.danb.launcher.widgets.AppWidgetSetupActivityResultContract.AppWidgetSetupInput
import javax.inject.Inject

@AndroidEntryPoint
class WidgetDialogFragment : BottomSheetDialogFragment() {

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

    private val launcherApps: LauncherApps by lazy {
        requireContext().getSystemService(LauncherApps::class.java)
    }

    private val packageManager: PackageManager by lazy { requireContext().packageManager }

    private val bindWidgetActivityLauncher = registerForActivityResult(
        AppWidgetSetupActivityResultContract()
    ) {
        if (it.success) {
            dismiss()
        } else {
            Toast.makeText(context, it.errorMessage, Toast.LENGTH_SHORT)
        }
        widgetViewModel.refresh()
    }

    private val widgetPreviewListener = WidgetPreviewListener { _, widgetPreviewViewItem ->
        bindWidgetActivityLauncher.launch(
            AppWidgetSetupInput(widgetPreviewViewItem.providerInfo, myUserHandle())
        )
    }

    private lateinit var widgetListAdapter: ViewBinderAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View? {
        super.onCreateView(inflater, container, savedInstanceState)

        val view = inflater.inflate(R.layout.widget_dialog_fragment, container, false)

        widgetListAdapter = ViewBinderAdapter(
            WidgetHeaderViewBinder { widgetDialogViewModel.toggleExpandedPackageName(it.packageName) },
            WidgetPreviewViewBinder(appWidgetViewProvider, widgetPreviewListener)
        )

        view.findViewById<RecyclerView>(R.id.widget_list).apply {
            layoutManager = LinearLayoutManager(context)
            adapter = widgetListAdapter
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.RESUMED) {
                widgetDialogViewModel.expandedPackageNames.collect { updateItems(it) }
            }
        }

        return view
    }

    private fun updateItems(expandedPackages: Set<String>) {
        val items = appWidgetManager.installedProviders.groupBy { it.provider.packageName }
            .mapKeys { launcherApps.getApplicationInfo(it.key, 0, myUserHandle()) }
            .toSortedMap(compareBy<ApplicationInfo> { it.loadLabel(packageManager).toString() })
            .flatMap { (appInfo, widgets) ->
                mutableListOf<ViewItem>().apply {
                    val isExpanded = expandedPackages.contains(appInfo.packageName)
                    add(widgetHeaderViewItemFactory.create(appInfo, isExpanded))
                    if (isExpanded) {
                        addAll(widgets.map { WidgetPreviewViewItem(it, myUserHandle()) })
                    }
                }
            }
        widgetListAdapter.submitList(items)
    }

    companion object {
        const val TAG = "widget_dialog_fragment"
    }
}
