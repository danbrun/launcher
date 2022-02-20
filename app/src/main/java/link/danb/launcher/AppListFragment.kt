package link.danb.launcher

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import link.danb.launcher.ActivityFilterViewModel.ActivityFilter

class AppListFragment : Fragment() {

    private val activityInfoViewModel: ActivityInfoViewModel by activityViewModels()
    private val activityFilterViewModel: ActivityFilterViewModel by activityViewModels()
    private val activityIconViewModel: ActivityIconViewModel by activityViewModels()

    private var adapter: AppItem.Adapter = AppItem.Adapter().apply {
        setOnClickListener { appItem, view ->
            activityInfoViewModel.openApp(
                appItem.componentName,
                appItem.userHandle,
                view
            )
        }
        setOnLongClickListener { appItem, _ ->
            AppOptionsDialogFragment.newInstance(appItem).show(parentFragmentManager, "test")
        }
    }

    private val filters: List<ActivityFilter> =
        listOf(ActivityFilter.ALL, ActivityFilter.PERSONAL, ActivityFilter.WORK)
    private val filterChips: HashMap<Int, Chip> = HashMap()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        val view = inflater.inflate(R.layout.fragment_app_list, container, false)

        val recyclerView = view.findViewById<RecyclerView>(R.id.app_list)
        recyclerView.adapter = adapter
        recyclerView.layoutManager = GridLayoutManager(
            context,
            requireContext().resources.getInteger(R.integer.launcher_columns)
        )

        view.findViewById<ChipGroup>(R.id.filter_list)?.run {
            filters.forEach { filter ->
                val chip = Chip(context)
                chip.setText(filter.nameResId)
                chip.chipStrokeWidth = 0f
                chip.setOnClickListener { activityFilterViewModel.setFilter(filter) }
                addView(chip)
                filterChips[filter.nameResId] = chip
            }
        }

        view.findViewById<MaterialButton>(R.id.settings_button)?.run {
            setOnClickListener {
                requireContext().startActivity(
                    Intent(android.provider.Settings.ACTION_SETTINGS).also {
                        it.sourceBounds = getLocationOnScreen()
                    },
                    makeClipRevealAnimation()
                )
            }
        }

        activityInfoViewModel.activities.observe(viewLifecycleOwner) { updateAppList() }
        activityFilterViewModel.filter.observe(viewLifecycleOwner) { updateFilterList() }
        activityIconViewModel.observableModel.observe(viewLifecycleOwner) { updateAppList() }

        return view
    }

    private fun updateAppList() {
        adapter.submitList(
            activityInfoViewModel.activities.value!!
                .filter(activityFilterViewModel.filter.value!!.filterFunction)
                .map {
                    AppItem(
                        it.componentName,
                        it.user,
                        it.label as String,
                        activityIconViewModel.getIconTimestamp(it)
                    ) { activityIconViewModel.getIcon(it) }
                }.sortedBy { it.label.lowercase() })
    }

    private fun updateFilterList() {
        filterChips.values.forEach {
            it.isSelected = false
        }
        val filterChip = filterChips[activityFilterViewModel.filter.value!!.nameResId]
        if (filterChip != null) {
            filterChip.isSelected = true
        }
        updateAppList()
    }
}
