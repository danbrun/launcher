package link.danb.launcher.widgets

import android.appwidget.AppWidgetManager
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.LauncherApps
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Process.myUserHandle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.ActivityResultLauncher
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import link.danb.launcher.R
import link.danb.launcher.list.*
import link.danb.launcher.utils.getLauncherApps

class WidgetDialogFragment : BottomSheetDialogFragment() {

    private val widgetViewModel: WidgetViewModel by activityViewModels()
    private val launcherApps: LauncherApps by lazy { requireContext().getLauncherApps() }
    private val packageManager: PackageManager by lazy { requireContext().packageManager }

    private val widgetPreviewListener = WidgetPreviewListener { _, widgetPreviewViewItem ->
        val widgetData = widgetViewModel.newHandle(widgetPreviewViewItem.appWidgetProviderInfo)

        if (widgetViewModel.bind(widgetData)) {
            if (widgetPreviewViewItem.appWidgetProviderInfo.configure != null) {
                startActivity(Intent().apply {
                    action = AppWidgetManager.ACTION_APPWIDGET_CONFIGURE
                    component = widgetPreviewViewItem.appWidgetProviderInfo.configure
                    putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetData.id)
                })
            }

            dismiss()
        } else {
            widgetPermissionsLauncher?.launch(widgetData)
        }
    }

    private var widgetPermissionsLauncher: ActivityResultLauncher<WidgetHandle>? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        widgetPermissionsLauncher =
            registerForActivityResult(widgetViewModel.WidgetPermissionResultHandler()) {
                if (it) {
                    dismiss()
                }
            }
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
            widgetViewModel.getProviders()
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
