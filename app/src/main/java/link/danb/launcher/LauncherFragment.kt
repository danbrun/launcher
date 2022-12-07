package link.danb.launcher

import android.appwidget.AppWidgetHost
import android.appwidget.AppWidgetManager
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.children
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.GridLayoutManager.SpanSizeLookup
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import link.danb.launcher.list.*
import link.danb.launcher.model.LauncherActivityData
import link.danb.launcher.model.LauncherActivityFilter
import link.danb.launcher.model.LauncherViewModel
import link.danb.launcher.utils.getLocationOnScreen
import link.danb.launcher.utils.makeClipRevealAnimation
import link.danb.launcher.widgets.AppWidgetViewProvider
import link.danb.launcher.widgets.WidgetDialogFragment
import link.danb.launcher.widgets.WidgetViewModel
import javax.inject.Inject

@AndroidEntryPoint
class LauncherFragment : Fragment() {

    private val launcherViewModel: LauncherViewModel by activityViewModels()
    private val widgetViewModel: WidgetViewModel by activityViewModels()

    @Inject
    lateinit var appWidgetHost: AppWidgetHost

    @Inject
    lateinit var appWidgetManager: AppWidgetManager

    @Inject
    lateinit var appWidgetViewProvider: AppWidgetViewProvider

    private val activityTileListener = object : ActivityTileListener {
        override fun onClick(view: View, activityViewItem: ActivityTileViewItem) {
            launcherViewModel.launch(activityViewItem.launcherActivityData, view)
        }

        override fun onLongClick(view: View, activityViewItem: ActivityTileViewItem) {
            ActivityDetailsDialogFragment.newInstance(activityViewItem.launcherActivityData)
                .show(parentFragmentManager, ActivityDetailsDialogFragment.TAG)
        }
    }

    private val activityAdapter =
        ViewBinderAdapter(GroupHeaderViewBinder(), ActivityTileViewBinder(activityTileListener))
    private val widgetAdapter: ViewBinderAdapter by lazy {
        ViewBinderAdapter(WidgetViewBinder(appWidgetViewProvider) {
            appWidgetHost.deleteAppWidgetId(it)
            widgetViewModel.refresh(appWidgetHost)
        })
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.launcher_fragment, container, false)

        val recyclerView: RecyclerView = view.findViewById(R.id.app_list)
        val columns = requireContext().resources.getInteger(R.integer.launcher_columns)
        recyclerView.adapter = activityAdapter
        recyclerView.layoutManager = GridLayoutManager(context, columns).apply {
            spanSizeLookup = object : SpanSizeLookup() {
                override fun getSpanSize(position: Int): Int {
                    return when (activityAdapter.currentList[position]) {
                        is GroupHeaderViewItem -> columns
                        else -> 1
                    }
                }
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launcherViewModel.filteredLauncherActivities.collect {
                    activityAdapter.submitList(getAppListViewItems(it))
                }
            }
        }

        val filterChips: ChipGroup = view.findViewById(R.id.filter_list)
        listOf(
            LauncherActivityFilter.ALL, LauncherActivityFilter.PERSONAL, LauncherActivityFilter.WORK
        ).forEach { filter ->
            Chip(context).apply {
                setText(filter.nameResId)
                tag = filter
                chipStrokeWidth = 0f
                setOnClickListener { launcherViewModel.filter.value = filter }
                filterChips.addView(this)
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launcherViewModel.filter.collect {
                    filterChips.children.forEach {
                        it.isSelected = it.tag == launcherViewModel.filter.value
                    }
                }
            }
        }

        view.findViewById<MaterialButton>(R.id.widget_button).apply {
            setOnClickListener {
                WidgetDialogFragment().show(childFragmentManager, WidgetDialogFragment.TAG)
            }
            setOnLongClickListener {
                appWidgetHost.appWidgetIds.forEach { appWidgetHost.deleteAppWidgetId(it) }
                widgetViewModel.refresh(appWidgetHost)
                true
            }
        }

        view.findViewById<RecyclerView>(R.id.widgets).apply {
            adapter = widgetAdapter
            layoutManager = LinearLayoutManager(context)
        }
        widgetViewModel.widgetIds.observe(viewLifecycleOwner) {
            widgetAdapter.submitList(it.map(::WidgetViewItem))
        }

        view.findViewById<MaterialButton>(R.id.settings_button).setOnClickListener { button ->
            requireContext().startActivity(
                Intent(android.provider.Settings.ACTION_SETTINGS).also {
                    it.sourceBounds = button.getLocationOnScreen()
                }, button.makeClipRevealAnimation()
            )
        }

        return view
    }

    override fun onStart() {
        super.onStart()

        widgetViewModel.refresh(appWidgetHost)
    }

    private fun getAppListViewItems(launcherActivities: List<LauncherActivityData>): List<ViewItem> {
        return launcherActivities.groupBy {
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
