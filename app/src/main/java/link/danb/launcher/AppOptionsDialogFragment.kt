package link.danb.launcher

import android.content.Context
import android.content.Intent
import android.content.pm.LauncherActivityInfo
import android.content.pm.LauncherApps
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import link.danb.launcher.list.*
import link.danb.launcher.utils.getLocationOnScreen
import link.danb.launcher.utils.makeClipRevealAnimation

class AppOptionsDialogFragment : BottomSheetDialogFragment() {

    private val launcherViewModel: LauncherViewModel by activityViewModels()

    private val launcherApps: LauncherApps by lazy {
        requireContext().getSystemService(Context.LAUNCHER_APPS_SERVICE) as LauncherApps
    }

    private val appItem: AppItem by lazy {
        launcherViewModel.iconList.value.first { item ->
            item.info.componentName == arguments?.getParcelable(NAME_ARGUMENT)
                    && item.info.user == arguments?.getParcelable(USER_ARGUMENT)
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

        val items = mutableListOf<ListItem>(appItem)

        items.add(
            CustomItem(
                requireContext(),
                R.string.settings,
                R.drawable.ic_baseline_settings_24,
                { view, _ ->
                    launcherViewModel.openAppInfo(
                        appItem.info.componentName,
                        appItem.info.user,
                        view
                    )
                },
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
                            Uri.parse("package:" + appItem.info.componentName.packageName)
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
                .setPackage(appItem.info.componentName.packageName),
            appItem.info.user
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
            is AppItem -> launcherViewModel.openApp(
                appItem.info.componentName,
                appItem.info.user,
                view
            )
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

        private const val NAME_ARGUMENT: String = "name"
        private const val USER_ARGUMENT: String = "user"

        fun newInstance(info: LauncherActivityInfo): AppOptionsDialogFragment {
            return AppOptionsDialogFragment().apply {
                arguments = Bundle().apply {
                    putParcelable(NAME_ARGUMENT, info.componentName)
                    putParcelable(USER_ARGUMENT, info.user)
                }
            }
        }
    }
}
