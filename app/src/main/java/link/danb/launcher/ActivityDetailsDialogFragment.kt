package link.danb.launcher

import android.content.ComponentName
import android.content.pm.LauncherApps
import android.os.Bundle
import android.os.UserHandle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.GridLayoutManager.SpanSizeLookup
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import link.danb.launcher.list.*
import link.danb.launcher.model.LauncherActivityData
import link.danb.launcher.model.LauncherViewModel
import link.danb.launcher.utils.getLauncherApps
import link.danb.launcher.utils.getLocationOnScreen
import link.danb.launcher.utils.getParcelableCompat
import link.danb.launcher.utils.makeClipRevealAnimation
import link.danb.launcher.widgets.WidgetBinder
import link.danb.launcher.widgets.WidgetViewModel

class ActivityDetailsDialogFragment : BottomSheetDialogFragment() {

    private val launcherApps: LauncherApps by lazy { requireContext().getLauncherApps() }
    private val launcherViewModel: LauncherViewModel by activityViewModels()

    private val widgetViewModel: WidgetViewModel by activityViewModels()
    private val widgetBinder = WidgetBinder(this) {
        widgetViewModel.refresh()
        dismiss()
    }

    private val launcherActivity by lazy {
        val component: ComponentName = arguments?.getParcelableCompat(COMPONENT_ARGUMENT)!!
        val user: UserHandle = arguments?.getParcelableCompat(USER_ARGUMENT)!!

        launcherViewModel.launcherActivities.value.first {
            it.component == component && it.user == user
        }
    }

    private val activityHeaderListener = object : ActivityHeaderListener {
        override fun onUninstallButtonClick(activityHeaderViewItem: ActivityHeaderViewItem) {
            dismiss()
        }

        override fun onSettingsButtonClick(activityHeaderViewItem: ActivityHeaderViewItem) {
            dismiss()
        }
    }

    private val shortcutTileListener =
        ShortcutTileListener { view, shortcutTileViewItem ->
            launcherApps.startShortcut(
                shortcutTileViewItem.info,
                view.getLocationOnScreen(),
                view.makeClipRevealAnimation()
            )
            dismiss()
        }

    private val widgetPreviewListener =
        WidgetPreviewListener { _, widgetPreviewViewItem ->
            widgetBinder.bindWidget(widgetPreviewViewItem.appWidgetProviderInfo)
        }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        super.onCreateView(inflater, container, savedInstanceState)

        val recyclerView = inflater.inflate(
            R.layout.activity_details_dialog_fragment,
            container,
            false
        ) as RecyclerView

        val size = requireContext().resources.getDimensionPixelSize(R.dimen.launcher_icon_size)

        val adapter =
            ViewBinderAdapter(
                ActivityHeaderViewBinder(activityHeaderListener),
                ShortcutTileViewBinder(shortcutTileListener),
                WidgetPreviewViewBinder(widgetPreviewListener)
            )

        val columns = requireContext().resources.getInteger(R.integer.launcher_columns)

        recyclerView.adapter = adapter
        recyclerView.isNestedScrollingEnabled = true
        recyclerView.layoutManager = GridLayoutManager(
            context,
            columns
        ).apply {
            spanSizeLookup = object : SpanSizeLookup() {
                override fun getSpanSize(position: Int): Int {
                    return when (adapter.currentList[position]) {
                        is ActivityHeaderViewItem, is WidgetPreviewViewItem -> columns
                        else -> 1
                    }
                }
            }
        }

        val items = mutableListOf<ViewItem>(ActivityHeaderViewItem(launcherActivity))

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

            items.add(ShortcutTileViewItem(shortcut, shortcut.shortLabel!!, icon!!))
        }

        widgetViewModel.providers
            .filter { it.provider.packageName == launcherActivity.component.packageName }
            .forEach { items.add(WidgetPreviewViewItem(it)) }

        adapter.submitList(items as List<ViewItem>?)

        return recyclerView
    }

    companion object {
        const val TAG = "app_options_dialog_fragment"

        private const val COMPONENT_ARGUMENT: String = "name"
        private const val USER_ARGUMENT: String = "user"

        fun newInstance(launcherActivityData: LauncherActivityData): ActivityDetailsDialogFragment {
            return ActivityDetailsDialogFragment().apply {
                arguments = Bundle().apply {
                    putParcelable(COMPONENT_ARGUMENT, launcherActivityData.component)
                    putParcelable(USER_ARGUMENT, launcherActivityData.user)
                }
            }
        }
    }
}
