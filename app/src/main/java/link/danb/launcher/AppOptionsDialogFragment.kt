package link.danb.launcher

import android.app.Dialog
import android.content.Intent
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

    private val launcherViewModel: LauncherViewModel by activityViewModels()

    private val launcherIcon: LauncherIcon by lazy {
        launcherViewModel.iconList.value.first { info ->
            info.componentName == arguments?.getParcelable(NAME_ARGUMENT)
                    && info.user == arguments?.getParcelable(USER_ARGUMENT)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        super.onCreateView(inflater, container, savedInstanceState)

        val view = inflater.inflate(R.layout.app_options, container, false)

        view.findViewById<TextView>(R.id.launcher_icon).run {
            text = launcherIcon.label
            background = null
            isClickable = false
            isFocusable = false
            setCompoundDrawables(launcherIcon.icon, null, null, null)
        }

        view.findViewById<TextView>(R.id.settings).setOnClickListener {
            launcherViewModel.openAppInfo(launcherIcon.componentName, launcherIcon.user, it)
            dismiss()
        }

        view.findViewById<TextView>(R.id.uninstall).setOnClickListener {
            requireContext().startActivity(
                Intent(
                    Intent.ACTION_DELETE,
                    Uri.parse("package:" + launcherIcon.componentName.packageName)
                )
            )
            dismiss()
        }

        return view
    }

    companion object {
        const val TAG = "app_options_dialog_fragment"

        private const val NAME_ARGUMENT: String = "name"
        private const val USER_ARGUMENT: String = "user"

        fun newInstance(launcherIcon: LauncherIcon): AppOptionsDialogFragment {
            return AppOptionsDialogFragment().apply {
                arguments = Bundle().apply {
                    putParcelable(NAME_ARGUMENT, launcherIcon.componentName)
                    putParcelable(USER_ARGUMENT, launcherIcon.user)
                }
            }
        }
    }
}
