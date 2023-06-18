package link.danb.launcher.widgets

import android.appwidget.AppWidgetHost
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.activityViewModels
import com.google.android.material.button.MaterialButton
import dagger.hilt.android.AndroidEntryPoint
import link.danb.launcher.R
import link.danb.launcher.model.WidgetMetadata
import javax.inject.Inject

@AndroidEntryPoint
class WidgetOptionsDialogFragment : DialogFragment() {

    private val widgetViewModel: WidgetViewModel by activityViewModels()

    @Inject
    lateinit var appWidgetHost: AppWidgetHost

    @Inject
    lateinit var appWidgetViewProvider: AppWidgetViewProvider

    private val widgetId by lazy { requireArguments().getInt(EXTRA_WIDGET_ID) }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View? {
        super.onCreateView(inflater, container, savedInstanceState)

        val view = inflater.inflate(R.layout.widget_options_dialog_fragment, container, false)

        view.findViewById<TextView>(R.id.remove_widget).apply {
            setOnClickListener {
                widgetViewModel.delete(widgetId)
                dismiss()
            }
        }

        view.findViewById<MaterialButton>(R.id.move_down).setOnClickListener {
            widgetViewModel.moveUp(widgetId)
        }

        view.findViewById<MaterialButton>(R.id.move_up).setOnClickListener {
            widgetViewModel.moveDown(widgetId)
        }

        view.findViewById<MaterialButton>(R.id.decrease_height).setOnClickListener {
            widgetViewModel.decreaseHeight(widgetId)
        }

        view.findViewById<MaterialButton>(R.id.increase_height).setOnClickListener {
            widgetViewModel.increaseHeight(widgetId)
        }

        return view
    }

    companion object {
        const val TAG = "widget_options_dialog"

        private const val EXTRA_WIDGET_ID = "widget_id"

        fun newInstance(widgetMetadata: WidgetMetadata) = WidgetOptionsDialogFragment().apply {
            arguments = Bundle().apply {
                putInt(EXTRA_WIDGET_ID, widgetMetadata.widgetId)
            }
        }
    }
}
