package link.danb.launcher.widgets

import android.appwidget.AppWidgetProviderInfo
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.ActivityResultLauncher
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import link.danb.launcher.R

class WidgetDialogFragment : BottomSheetDialogFragment() {

    private val widgetViewModel: WidgetViewModel by activityViewModels()

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

        val widgetListAdapter = WidgetItemAdapter(this::onClick)
        widgetList.adapter = widgetListAdapter
        widgetListAdapter.submitList(
            widgetViewModel.getProviders()
                .sortedBy { it.loadLabel(requireContext().packageManager).lowercase() })

        return view
    }

    private fun onClick(info: AppWidgetProviderInfo) {
        val widgetData = widgetViewModel.newHandle(info)

        if (widgetViewModel.bind(widgetData)) {
            dismiss()
        } else {
            widgetPermissionsLauncher?.launch(widgetData)
        }

        return
    }

    companion object {
        const val TAG = "widget_dialog_fragment"
    }
}
