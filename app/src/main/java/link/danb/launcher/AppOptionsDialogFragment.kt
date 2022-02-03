package link.danb.launcher

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.activityViewModels
import com.google.android.material.bottomsheet.BottomSheetDialogFragment

class AppOptionsDialogFragment : BottomSheetDialogFragment() {

    private val appViewModel: AppViewModel by activityViewModels()

    private val appItem: AppItem? by lazy {
        appViewModel.apps.value?.firstOrNull { appItem ->
            appItem.info.componentName == arguments?.getParcelable(NAME_ARGUMENT)
                    && appItem.info.user == arguments?.getParcelable(USER_ARGUMENT)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        super.onCreateView(inflater, container, savedInstanceState)

        val view = inflater.inflate(R.layout.app_options, container, false)

        view.findViewById<TextView>(R.id.app_item).run {
            appItem?.let { app ->
                text = app.name
                background = null
                isClickable = false
                isFocusable = false
                setCompoundDrawables(app.getIcon(context), null, null, null)
            }
        }

        view.findViewById<TextView>(R.id.settings).apply {
            setOnClickListener { v ->
                appItem?.run {
                    appViewModel.openAppInfo(this, v)
                    dismiss()
                }
            }
        }

        return view
    }

    companion object {
        private const val NAME_ARGUMENT: String = "name"
        private const val USER_ARGUMENT: String = "user"

        fun newInstance(appItem: AppItem): AppOptionsDialogFragment {
            return AppOptionsDialogFragment().apply {
                arguments = Bundle().apply {
                    putParcelable(NAME_ARGUMENT, appItem.info.componentName)
                    putParcelable(USER_ARGUMENT, appItem.info.user)
                }
            }
        }
    }
}