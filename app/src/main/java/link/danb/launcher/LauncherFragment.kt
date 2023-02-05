package link.danb.launcher

import android.app.SearchManager
import android.appwidget.AppWidgetHost
import android.appwidget.AppWidgetManager
import android.content.Intent
import android.content.pm.LauncherApps
import android.os.Bundle
import android.os.Process.myUserHandle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
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
import link.danb.launcher.ui.InvertedCornerDrawable
import link.danb.launcher.ui.RoundedCornerOutlineProvider
import link.danb.launcher.utils.getLocationOnScreen
import link.danb.launcher.utils.makeClipRevealAnimation
import link.danb.launcher.widgets.AppWidgetViewProvider
import link.danb.launcher.widgets.WidgetViewModel
import javax.inject.Inject

@AndroidEntryPoint
class LauncherFragment : Fragment() {

    private val launcherViewModel: LauncherViewModel by activityViewModels()
    private val widgetViewModel: WidgetViewModel by activityViewModels()

    @Inject
    lateinit var launcherApps: LauncherApps

    @Inject
    lateinit var appWidgetHost: AppWidgetHost

    @Inject
    lateinit var appWidgetManager: AppWidgetManager

    @Inject
    lateinit var appWidgetViewProvider: AppWidgetViewProvider

    @Inject
    lateinit var launcherMenuProvider: LauncherMenuProvider

    private val activityAdapter: ViewBinderAdapter by lazy {
        ViewBinderAdapter(
            GroupHeaderViewBinder(),
            ActivityTileViewBinder(activityTileListener),
            WidgetViewBinder(appWidgetViewProvider, widgetViewListener),
        )
    }

    private val activityTileListener = object : ActivityTileListener {
        override fun onClick(view: View, activityViewItem: ActivityTileViewItem) {
            launcherApps.startMainActivity(
                activityViewItem.launcherActivityData.component,
                activityViewItem.launcherActivityData.user,
                view.getLocationOnScreen(),
                view.makeClipRevealAnimation()
            )
        }

        override fun onLongClick(view: View, activityViewItem: ActivityTileViewItem) {
            ActivityDetailsDialogFragment.newInstance(activityViewItem.launcherActivityData)
                .show(parentFragmentManager, ActivityDetailsDialogFragment.TAG)
        }
    }

    private val widgetViewListener = WidgetViewListener {
        appWidgetHost.deleteAppWidgetId(it)
        widgetViewModel.refresh(appWidgetHost)
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
                    is WidgetViewItem, is GroupHeaderViewItem -> gridLayoutManager.spanCount
                    else -> 1
                }
        }

        val appsList: RecyclerView = view.findViewById(R.id.app_list)
        appsList.adapter = activityAdapter
        appsList.layoutManager = gridLayoutManager

        val radius = resources.getDimensionPixelSize(R.dimen.apps_list_corner_radius)
        appsList.clipToOutline = true
        appsList.outlineProvider = RoundedCornerOutlineProvider(radius)
        view.findViewById<View>(R.id.app_list_background).background =
            InvertedCornerDrawable(radius)

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
                    combine(
                        launcherViewModel.launcherActivities,
                        launcherViewModel.activitiesFilter,
                        widgetViewModel.widgetIds,
                        ::LauncherListUpdateData
                    ).collect(::updateLauncherList)
                }
            }
        }

        return view
    }

    override fun onStart() {
        super.onStart()

        widgetViewModel.refresh(appWidgetHost)
    }

    private data class LauncherListUpdateData(
        val activities: List<LauncherActivityData>,
        val activitiesFilter: LauncherViewModel.ActivitiesFilter,
        val widgetIds: List<Int>,
    )

    private fun updateLauncherList(data: LauncherListUpdateData) {
        activityAdapter.submitList(
            data.widgetIds.map(::WidgetViewItem) + getAppListViewItems(
                data.activities,
                data.activitiesFilter.showWorkActivities,
                data.activitiesFilter.showHiddenActivities
            )
        )
    }

    private fun getAppListViewItems(
        launcherActivities: List<LauncherActivityData>,
        showWorkProfileActivities: Boolean,
        showHiddenActivities: Boolean,
    ): List<ViewItem> {
        return launcherActivities.filter {
            val isWorkActivity = it.user != myUserHandle()
            val isHiddenActivity = !launcherViewModel.isVisible(it)
            showWorkProfileActivities == isWorkActivity && (showHiddenActivities || !isHiddenActivity)
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
}
