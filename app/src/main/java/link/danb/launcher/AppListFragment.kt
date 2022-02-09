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

class AppListFragment : Fragment() {

    private val appViewModel: AppViewModel by activityViewModels()

    private var adapter: AppItem.Adapter = AppItem.Adapter().apply {
        setOnClickListener { appItem, view -> appViewModel.openApp(appItem, view) }
        setOnLongClickListener { appItem, _ ->
            AppOptionsDialogFragment.newInstance(appItem).show(parentFragmentManager, "test")
        }
    }

    private val filters: List<AppFilter> = listOf(AppFilter.ALL, AppFilter.PERSONAL, AppFilter.WORK)
    private val filterChips: HashMap<String, Chip> = HashMap()

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
                chip.text = filter.name
                chip.chipStrokeWidth = 0f
                chip.setOnClickListener { appViewModel.setFilter(filter) }
                addView(chip)
                filterChips[filter.name] = chip
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

        appViewModel.apps.observe(viewLifecycleOwner) { updateAppList() }
        appViewModel.filter.observe(viewLifecycleOwner) { updateFilterList() }

        return view
    }

    private fun updateAppList() {
        adapter.submitList(
            appViewModel.apps.value!!
                .filter(appViewModel.filter.value!!.filterFunction)
                .sortedBy { it.name.lowercase() })
    }

    private fun updateFilterList() {
        filterChips.values.forEach {
            it.isSelected = false
        }
        val filterChip = filterChips[appViewModel.filter.value!!.name]
        if (filterChip != null) {
            filterChip.isSelected = true
        }
        updateAppList()
    }
}
