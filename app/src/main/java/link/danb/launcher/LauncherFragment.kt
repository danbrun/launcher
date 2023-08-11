package link.danb.launcher

import android.app.SearchManager
import android.appwidget.AppWidgetHost
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Intent
import android.content.pm.LauncherApps
import android.os.Bundle
import android.os.Process.myUserHandle
import android.os.UserHandle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
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
import link.danb.launcher.list.*
import link.danb.launcher.model.LauncherActivityData
import link.danb.launcher.model.LauncherViewModel
import link.danb.launcher.model.GestureContractModel
import link.danb.launcher.model.WidgetMetadata
import link.danb.launcher.ui.InvertedCornerDrawable
import link.danb.launcher.ui.RoundedCornerOutlineProvider
import link.danb.launcher.utils.getBoundsOnScreen
import link.danb.launcher.utils.makeClipRevealAnimation
import link.danb.launcher.widgets.AppWidgetViewProvider
import link.danb.launcher.widgets.WidgetSizeUtil
import link.danb.launcher.widgets.WidgetViewModel
import javax.inject.Inject

@AndroidEntryPoint
class LauncherFragment : Fragment() {

    private val launcherViewModel: LauncherViewModel by activityViewModels()
    private val widgetViewModel: WidgetViewModel by activityViewModels()
    private val gestureContractModel: GestureContractModel by activityViewModels()

    @Inject
    lateinit var launcherApps: LauncherApps

    @Inject
    lateinit var appWidgetHost: AppWidgetHost

    @Inject
    lateinit var appWidgetManager: AppWidgetManager

    @Inject
    lateinit var appWidgetViewProvider: AppWidgetViewProvider

    @Inject
    lateinit var widgetSizeUtil: WidgetSizeUtil

    @Inject
    lateinit var launcherMenuProvider: LauncherMenuProvider

    private lateinit var appsList: RecyclerView

    private val activityAdapter: ViewBinderAdapter by lazy {
        ViewBinderAdapter(
            GroupHeaderViewBinder(),
            ActivityTileViewBinder(activityTileListener),
            WidgetViewBinder(appWidgetViewProvider, widgetViewListener),
            WidgetEditorViewBinder(appWidgetViewProvider, widgetSizeUtil, widgetEditorViewListener),
        )
    }

    private val activityTileListener = object : ActivityTileListener {
        override fun onClick(view: View, activityViewItem: ActivityTileViewItem) {
            launcherApps.startMainActivity(
                activityViewItem.launcherActivityData.component,
                activityViewItem.launcherActivityData.user,
                view.getBoundsOnScreen(),
                view.makeClipRevealAnimation()
            )
        }

        override fun onLongClick(view: View, activityViewItem: ActivityTileViewItem) {
            ActivityDetailsDialogFragment.newInstance(activityViewItem.launcherActivityData)
                .show(parentFragmentManager, ActivityDetailsDialogFragment.TAG)
        }
    }

    private val widgetViewListener = WidgetViewListener {
        widgetViewModel.startEditing(it.widgetId)
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
            widgetViewModel.delete(widgetMetadata.widgetId)
        }

        override fun onMoveUp(widgetMetadata: WidgetMetadata) {
            widgetViewModel.moveUp(widgetMetadata.widgetId)
        }

        override fun onResize(widgetMetadata: WidgetMetadata, height: Int) {
            widgetViewModel.setHeight(widgetMetadata.widgetId, height)
        }

        override fun onMoveDown(widgetMetadata: WidgetMetadata) {
            widgetViewModel.moveDown(widgetMetadata.widgetId)
        }

        override fun onDone(widgetMetadata: WidgetMetadata) {
            widgetViewModel.finishEditing()
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
                        widgetViewModel.widgets,
                        widgetViewModel.widgetToEdit,
                        ::getWidgetListViewItems
                    )

                    val appsFlow = combine(
                        launcherViewModel.launcherActivities,
                        launcherViewModel.activitiesFilter,
                        ::getAppListViewItems
                    )

                    combine(widgetsFlow, appsFlow) { widgets, apps ->
                        widgets + apps
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

        widgetViewModel.refresh()
    }

    private fun getWidgetListViewItems(
        widgets: List<WidgetMetadata>, widgetToEdit: Int?
    ): List<ViewItem> {
        return widgets.flatMap {
            if (it.widgetId == widgetToEdit) {
                listOf(
                    WidgetViewItem(it),
                    WidgetEditorViewItem(it, appWidgetManager.getAppWidgetInfo(it.widgetId))
                )
            } else {
                listOf(WidgetViewItem(it))
            }
        }
    }

    private fun getAppListViewItems(
        launcherActivities: List<LauncherActivityData>,
        launcherFilters: LauncherViewModel.ActivitiesFilter,
    ): List<ViewItem> {
        return launcherActivities.filter {
            val isWorkActivity = it.user != myUserHandle()
            val isHiddenActivity = !launcherViewModel.isVisible(it)
            launcherFilters.showWorkActivities == isWorkActivity && (launcherFilters.showHiddenActivities || !isHiddenActivity)
        }.groupBy {
            val initial = it.name.first().uppercaseChar()
            when {
                initial.isLetter() -> initial.toString()
                else -> "..."
            }
        }.toSortedMap().flatMap { (groupName, launcherActivities) ->
            buildList {
                add(GroupHeaderViewItem(groupName))
                addAll(launcherActivities.sortedBy { it.name.toString().lowercase() }
                    .map { ActivityTileViewItem(it) })
            }
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

            if (item !is ActivityTileViewItem || item.launcherActivityData.user != user) continue

            if (item.launcherActivityData.component == component) {
                return index
            }

            if (item.launcherActivityData.component.packageName == component.packageName) {
                firstMatchIndex = index
            }
        }

        return firstMatchIndex
    }
}
