package link.danb.launcher

import android.app.SearchManager
import android.appwidget.AppWidgetHost
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Intent
import android.content.pm.LauncherApps
import android.content.pm.ShortcutInfo
import android.os.Bundle
import android.os.UserHandle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.graphics.toRectF
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.GridLayoutManager.SpanSizeLookup
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomappbar.BottomAppBar
import com.google.android.material.floatingactionbutton.FloatingActionButton
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import link.danb.launcher.activities.ActivityDetailsDialogFragment
import link.danb.launcher.tiles.ActivityTileData
import link.danb.launcher.activities.ActivitiesViewModel
import link.danb.launcher.gestures.GestureContractModel
import link.danb.launcher.icons.LauncherIconCache
import link.danb.launcher.activities.ActivitiesViewModel.ActivityData
import link.danb.launcher.tiles.ShortcutTileData
import link.danb.launcher.shortcuts.ShortcutsViewModel
import link.danb.launcher.tiles.TileData
import link.danb.launcher.database.WidgetMetadata
import link.danb.launcher.tiles.TileViewItem
import link.danb.launcher.tiles.TransparentTileViewBinder
import link.danb.launcher.ui.GroupHeaderViewBinder
import link.danb.launcher.ui.GroupHeaderViewItem
import link.danb.launcher.ui.InvertedCornerDrawable
import link.danb.launcher.ui.RoundedCornerOutlineProvider
import link.danb.launcher.ui.ViewBinderAdapter
import link.danb.launcher.ui.ViewItem
import link.danb.launcher.profiles.ProfilesModel
import link.danb.launcher.extensions.getBoundsOnScreen
import link.danb.launcher.extensions.makeClipRevealAnimation
import link.danb.launcher.widgets.AppWidgetViewProvider
import link.danb.launcher.widgets.WidgetEditorViewBinder
import link.danb.launcher.widgets.WidgetEditorViewItem
import link.danb.launcher.widgets.WidgetEditorViewListener
import link.danb.launcher.widgets.WidgetSizeUtil
import link.danb.launcher.widgets.WidgetViewBinder
import link.danb.launcher.widgets.WidgetViewItem
import link.danb.launcher.widgets.WidgetsViewModel
import javax.inject.Inject

@AndroidEntryPoint
class LauncherFragment : Fragment() {

    private val activitiesViewModel: ActivitiesViewModel by activityViewModels()
    private val gestureContractModel: GestureContractModel by activityViewModels()
    private val shortcutsViewModel: ShortcutsViewModel by activityViewModels()
    private val widgetsViewModel: WidgetsViewModel by activityViewModels()

    @Inject
    lateinit var appWidgetHost: AppWidgetHost

    @Inject
    lateinit var appWidgetManager: AppWidgetManager

    @Inject
    lateinit var appWidgetViewProvider: AppWidgetViewProvider

    @Inject
    lateinit var launcherApps: LauncherApps

    @Inject
    lateinit var launcherIconCache: LauncherIconCache

    @Inject
    lateinit var launcherMenuProvider: LauncherMenuProvider

    @Inject
    lateinit var profilesModel: ProfilesModel

    @Inject
    lateinit var widgetSizeUtil: WidgetSizeUtil

    private lateinit var appsList: RecyclerView

    private val activityAdapter: ViewBinderAdapter by lazy {
        ViewBinderAdapter(
            GroupHeaderViewBinder(),
            TransparentTileViewBinder(this::onTileClick) { _, it -> onTileLongClick(it) },
            WidgetViewBinder(appWidgetViewProvider) { widgetsViewModel.startEditing(it.widgetId) },
            WidgetEditorViewBinder(appWidgetViewProvider, widgetSizeUtil, widgetEditorViewListener),
        )
    }

    private val widgetEditorViewListener = object : WidgetEditorViewListener {
        override fun onConfigure(widgetMetadata: WidgetMetadata) {
            appWidgetHost.startAppWidgetConfigureActivityForResult(
                this@LauncherFragment.requireActivity(), widgetMetadata.widgetId,/* intentFlags = */
                0, R.id.app_widget_configure_request_id,/* options = */
                null
            )
        }

        override fun onDelete(widgetMetadata: WidgetMetadata) {
            widgetsViewModel.delete(widgetMetadata.widgetId)
        }

        override fun onMoveUp(widgetMetadata: WidgetMetadata) {
            widgetsViewModel.moveUp(widgetMetadata.widgetId)
        }

        override fun onResize(widgetMetadata: WidgetMetadata, height: Int) {
            widgetsViewModel.setHeight(widgetMetadata.widgetId, height)
        }

        override fun onMoveDown(widgetMetadata: WidgetMetadata) {
            widgetsViewModel.moveDown(widgetMetadata.widgetId)
        }

        override fun onDone(widgetMetadata: WidgetMetadata) {
            widgetsViewModel.finishEditing()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.launcher_fragment, container, false)

        val gridLayoutManager = GridLayoutManager(
            context, requireContext().resources.getInteger(R.integer.launcher_columns)
        )
        gridLayoutManager.spanSizeLookup = object : SpanSizeLookup() {
            override fun getSpanSize(position: Int): Int =
                when (activityAdapter.currentList[position]) {
                    is WidgetViewItem, is WidgetEditorViewItem, is GroupHeaderViewItem -> gridLayoutManager.spanCount
                    else -> 1
                }
        }

        appsList = view.findViewById(R.id.app_list)
        appsList.adapter = activityAdapter
        appsList.layoutManager = gridLayoutManager

        val radius = resources.getDimensionPixelSize(R.dimen.apps_list_corner_radius)
        appsList.clipToOutline = true
        appsList.outlineProvider = RoundedCornerOutlineProvider(radius)
        view.findViewById<View>(R.id.app_list_background).background =
            InvertedCornerDrawable(radius)

        activityAdapter.onBindViewHolderListener = {
            maybeAnimationNewIntent()
        }

        view.findViewById<BottomAppBar>(R.id.bottom_app_bar).addMenuProvider(launcherMenuProvider)

        view.findViewById<FloatingActionButton>(R.id.floating_action).setOnClickListener { button ->
            startActivity(Intent().apply {
                action = Intent.ACTION_WEB_SEARCH
                putExtra(SearchManager.EXTRA_NEW_SEARCH, true)
                // This extra is for Firefox to open a new tab.
                putExtra("open_to_search", "static_shortcut_new_tab")
            }, button.makeClipRevealAnimation())
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    val widgetsFlow = combine(
                        widgetsViewModel.widgets,
                        widgetsViewModel.widgetToEdit,
                        profilesModel.activeProfile,
                        ::getWidgetListViewItems
                    )

                    val shortcutsFlow = combine(
                        shortcutsViewModel.pinnedShortcuts,
                        profilesModel.activeProfile,
                        ::getShortcutListViewItems
                    )

                    val appsFlow = combine(
                        activitiesViewModel.launcherActivities,
                        profilesModel.activeProfile,
                        ::getAppListViewItems
                    )

                    combine(widgetsFlow, shortcutsFlow, appsFlow) { widgets, shortcuts, apps ->
                        widgets + shortcuts + apps
                    }.collect { activityAdapter.submitList(it) }
                }

                launch {
                    gestureContractModel.gestureContract.collect {
                        maybeAnimationNewIntent()
                    }
                }
            }
        }

        return view
    }

    override fun onStart() {
        super.onStart()

        widgetsViewModel.refresh()
    }

    private fun getWidgetListViewItems(
        widgets: List<WidgetMetadata>, widgetToEdit: Int?, activeProfile: UserHandle
    ): List<ViewItem> = widgets.flatMap {
        val info = appWidgetManager.getAppWidgetInfo(it.widgetId)
        if (info.profile != activeProfile) return@flatMap listOf()

        if (it.widgetId == widgetToEdit) {
            listOf(
                WidgetViewItem(it),
                WidgetEditorViewItem(it, appWidgetManager.getAppWidgetInfo(it.widgetId))
            )
        } else {
            listOf(WidgetViewItem(it))
        }
    }

    private fun getShortcutListViewItems(
        shortcuts: List<ShortcutInfo>, activeProfile: UserHandle
    ): List<ViewItem> = shortcuts.filter { it.userHandle == activeProfile }.groupBy { true }
        .flatMap { (_, shortcuts) ->
            buildList {
                add(GroupHeaderViewItem(requireContext().getString(R.string.shortcuts)))
                addAll(shortcuts.map {
                    TileViewItem.transparentTileViewItem(
                        ShortcutTileData(it), it.shortLabel!!, launcherIconCache.get(it)
                    )
                }.sortedBy { it.name.toString().lowercase() })
            }
        }

    private fun getAppListViewItems(
        launcherActivities: List<ActivityData>, activeProfile: UserHandle
    ): List<ViewItem> = launcherActivities.filter {
        !it.metadata.isHidden && it.info.user == activeProfile
    }.groupBy {
        val initial = it.info.label.first().uppercaseChar()
        when {
            initial.isLetter() -> initial.toString()
            else -> "..."
        }
    }.toSortedMap().flatMap { (groupName, launcherActivities) ->
        buildList {
            add(GroupHeaderViewItem(groupName))
            addAll(launcherActivities.sortedBy { it.info.label.toString().lowercase() }.map {
                TileViewItem.transparentTileViewItem(
                    ActivityTileData(it.info), it.info.label, launcherIconCache.get(it.info)
                )
            })
        }
    }

    private fun maybeAnimationNewIntent() {
        val gestureContract = gestureContractModel.gestureContract.value ?: return

        val firstMatchingIndex =
            getFirstMatchingIndex(gestureContract.componentName, gestureContract.userHandle)

        if (firstMatchingIndex < 0) return

        val view = appsList.findViewHolderForAdapterPosition(firstMatchingIndex)?.itemView

        if (view != null) {
            gestureContractModel.setBounds(view.getBoundsOnScreen().toRectF())
        } else {
            appsList.scrollToPosition(firstMatchingIndex)
        }
    }

    private fun getFirstMatchingIndex(component: ComponentName, user: UserHandle): Int {
        // If there is an exact component match, returns that icon view. Otherwise returns the icon
        // view of the first package match.

        var firstMatchIndex = -1

        for (index in activityAdapter.currentList.indices) {
            val item = activityAdapter.currentList[index]

            if (item !is TileViewItem || item.data !is ActivityTileData || item.data.info.user != user) continue

            if (item.data.info.componentName == component) {
                return index
            }

            if (item.data.info.componentName.packageName == component.packageName) {
                firstMatchIndex = index
            }
        }

        return firstMatchIndex
    }

    private fun onTileClick(view: View, tileViewData: TileData) {
        when (tileViewData) {
            is ActivityTileData -> {
                launcherApps.startMainActivity(
                    tileViewData.info.componentName,
                    tileViewData.info.user,
                    view.getBoundsOnScreen(),
                    view.makeClipRevealAnimation()
                )
            }

            is ShortcutTileData -> {
                launcherApps.startShortcut(
                    tileViewData.info, view.getBoundsOnScreen(), view.makeClipRevealAnimation()
                )
            }
        }
    }

    private fun onTileLongClick(tileViewData: TileData) {
        when (tileViewData) {
            is ActivityTileData -> {
                ActivityDetailsDialogFragment.newInstance(tileViewData.info)
                    .show(parentFragmentManager, ActivityDetailsDialogFragment.TAG)
            }

            is ShortcutTileData -> {
                Toast.makeText(context, R.string.unpinned_shortcut, Toast.LENGTH_SHORT).show()
                shortcutsViewModel.unpinShortcut(tileViewData.info)
            }
        }
    }
}
