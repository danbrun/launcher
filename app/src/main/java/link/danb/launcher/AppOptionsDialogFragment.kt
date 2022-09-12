package link.danb.launcher

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.LauncherApps
import android.net.Uri
import android.os.Bundle
import android.os.UserHandle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import link.danb.launcher.list.*
import link.danb.launcher.model.LauncherActivityData
import link.danb.launcher.model.LauncherViewModel
import link.danb.launcher.utils.getLocationOnScreen
import link.danb.launcher.utils.getParcelableCompat
import link.danb.launcher.utils.makeClipRevealAnimation

class AppOptionsDialogFragment : BottomSheetDialogFragment() {

    private val launcherViewModel: LauncherViewModel by activityViewModels()

    private val launcherApps: LauncherApps by lazy {
        requireContext().getSystemService(Context.LAUNCHER_APPS_SERVICE) as LauncherApps
    }

    private val launcherActivity by lazy {
        val component =
            arguments?.getParcelableCompat(COMPONENT_ARGUMENT, ComponentName::class.java)
        val user = arguments?.getParcelableCompat(USER_ARGUMENT, UserHandle::class.java)

        launcherViewModel.launcherActivities.value.first {
            it.component == component && it.user == user
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        super.onCreateView(inflater, container, savedInstanceState)

        val recyclerView = inflater.inflate(R.layout.app_options, container, false) as RecyclerView

        val size = requireContext().resources.getDimensionPixelSize(R.dimen.launcher_icon_size)

        val adapter = ListItemAdapter(this::onListItemClick, null)

        recyclerView.adapter = adapter
        recyclerView.isNestedScrollingEnabled = true
        recyclerView.layoutManager = GridLayoutManager(
            context,
            requireContext().resources.getInteger(R.integer.launcher_columns)
        )

        val items = mutableListOf<ListItem>(ActivityItem(launcherActivity))

        items.add(
            CustomItem(
                requireContext(),
                R.string.settings,
                R.drawable.ic_baseline_settings_24,
                { view, _ -> launcherViewModel.openDetailsActivity(launcherActivity, view) },
                null
            )
        )
        items.add(
            CustomItem(
                requireContext(),
                R.string.uninstall,
                R.drawable.ic_baseline_delete_forever_24,
                { _, _ ->
                    requireContext().startActivity(
                        Intent(
                            Intent.ACTION_DELETE,
                            Uri.parse("package:" + launcherActivity.component.packageName)
                        )
                    )
                },
                null
            )
        )

        val shortcuts = launcherApps.getShortcuts(
            LauncherApps.ShortcutQuery()
                .setQueryFlags(
                    LauncherApps.ShortcutQuery.FLAG_MATCH_DYNAMIC or
                            LauncherApps.ShortcutQuery.FLAG_MATCH_MANIFEST
                )
                .setPackage(launcherActivity.component.packageName),
            launcherActivity.user
        )

        shortcuts?.forEach { shortcut ->
            val icon = launcherApps.getShortcutIconDrawable(shortcut, 0)
                ?.let { icon -> LauncherIconDrawable(icon) }
            icon?.setBounds(0, 0, size, size)

            items.add(ShortcutItem(shortcut, shortcut.shortLabel!!, icon!!))
        }

        adapter.submitList(items)

        return recyclerView
    }

    private fun onListItemClick(view: View, item: ListItem) {
        when (item) {
            is ActivityItem -> launcherViewModel.openActivity(launcherActivity, view)
            is ShortcutItem -> launcherApps.startShortcut(
                item.info,
                view.getLocationOnScreen(),
                view.makeClipRevealAnimation()
            )
            is CustomItem -> item.onClick?.invoke(view, item)
        }
        dismiss()
    }

    companion object {
        const val TAG = "app_options_dialog_fragment"

        private const val COMPONENT_ARGUMENT: String = "name"
        private const val USER_ARGUMENT: String = "user"

        fun newInstance(launcherActivityData: LauncherActivityData): AppOptionsDialogFragment {
            return AppOptionsDialogFragment().apply {
                arguments = Bundle().apply {
                    putParcelable(COMPONENT_ARGUMENT, launcherActivityData.component)
                    putParcelable(USER_ARGUMENT, launcherActivityData.user)
                }
            }
        }
    }
}
