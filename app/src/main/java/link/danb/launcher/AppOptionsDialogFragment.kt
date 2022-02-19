package link.danb.launcher

import android.app.Dialog
import android.content.Intent
import android.content.pm.LauncherActivityInfo
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.activityViewModels
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment

class AppOptionsDialogFragment : BottomSheetDialogFragment() {

    private val activityInfoViewModel: ActivityInfoViewModel by activityViewModels()
    private val iconViewModel: ActivityIconViewModel by activityViewModels()

    private val activityInfo: LauncherActivityInfo? by lazy {
        activityInfoViewModel.activities.value?.firstOrNull { info ->
            info.componentName == arguments?.getParcelable(NAME_ARGUMENT)
                    && info.user == arguments?.getParcelable(USER_ARGUMENT)
        }
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = BottomSheetDialog(requireContext())
        dialog.behavior.state = BottomSheetBehavior.STATE_EXPANDED
        return dialog
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        super.onCreateView(inflater, container, savedInstanceState)

        val view = inflater.inflate(R.layout.app_options, container, false)

        view.findViewById<TextView>(R.id.app_item).run {
            activityInfo?.let { app ->
                text = app.label
                background = null
                isClickable = false
                isFocusable = false
                setCompoundDrawables(iconViewModel.getIcon(app), null, null, null)
            }
        }

        view.findViewById<TextView>(R.id.settings).apply {
            setOnClickListener { v ->
                activityInfo?.run {
                    activityInfoViewModel.openAppInfo(componentName, user, v)
                    dismiss()
                }
            }
        }

        view.findViewById<TextView>(R.id.uninstall).apply {
            setOnClickListener {
                activityInfo?.run {
                    context.startActivity(
                        Intent(
                            Intent.ACTION_DELETE,
                            Uri.parse("package:" + applicationInfo.packageName)
                        )
                    )
                }
                dismiss()
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
                    putParcelable(NAME_ARGUMENT, appItem.componentName)
                    putParcelable(USER_ARGUMENT, appItem.userHandle)
                }
            }
        }
    }
}
