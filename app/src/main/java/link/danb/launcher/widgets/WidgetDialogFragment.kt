package link.danb.launcher.widgets

import android.content.pm.ApplicationInfo
import android.content.pm.LauncherApps
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Process.myUserHandle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import link.danb.launcher.R
import link.danb.launcher.list.*
import link.danb.launcher.utils.getLauncherApps

class WidgetDialogFragment : BottomSheetDialogFragment() {

    private val launcherApps: LauncherApps by lazy { requireContext().getLauncherApps() }
    private val packageManager: PackageManager by lazy { requireContext().packageManager }

    private val widgetViewModel: WidgetViewModel by activityViewModels()
    private val widgetBinder = WidgetBinder(this) {
        widgetViewModel.refresh()
        dismiss()
    }

    private val widgetPreviewListener = WidgetPreviewListener { _, widgetPreviewViewItem ->
        widgetBinder.bindWidget(widgetPreviewViewItem.appWidgetProviderInfo)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        super.onCreateView(inflater, container, savedInstanceState)

        val view = inflater.inflate(R.layout.widget_dialog_fragment, container, false)

        val widgetList: RecyclerView = view.findViewById(R.id.widget_list)
        widgetList.isNestedScrollingEnabled = true
        widgetList.layoutManager = LinearLayoutManager(context)

        val widgetListAdapter =
            ViewBinderAdapter(
                ApplicationHeaderViewBinder(),
                WidgetPreviewViewBinder(widgetPreviewListener)
            )
        widgetList.adapter = widgetListAdapter

        widgetListAdapter.submitList(
            widgetViewModel.providers
                .groupBy { it.provider.packageName }
                .mapKeys { launcherApps.getApplicationInfo(it.key, 0, myUserHandle()) }
                .toSortedMap(compareBy<ApplicationInfo> { it.loadLabel(packageManager).toString() })
                .flatMap { (appInfo, widgets) ->
                    listOf(
                        ApplicationHeaderViewItem(requireActivity().application, appInfo),
                        *widgets.map { WidgetPreviewViewItem(it) }.toTypedArray()
                    )
                })

        return view
    }

    companion object {
        const val TAG = "widget_dialog_fragment"
    }
}
