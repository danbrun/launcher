package link.danb.launcher

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
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

class AppListFragment : Fragment() {

    private val launcherViewModel: LauncherViewModel by activityViewModels()

    private var adapter: LauncherIcon.Adapter = LauncherIcon.Adapter(
        { view, launcherIcon ->
            launcherViewModel.openApp(
                launcherIcon.componentName,
                launcherIcon.user,
                view
            )
        },
        { _, launcherIcon ->
            AppOptionsDialogFragment.newInstance(launcherIcon)
                .show(parentFragmentManager, AppOptionsDialogFragment.TAG)
        }
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (savedInstanceState == null) {
            launcherViewModel.setFilter(LauncherFilter.PERSONAL)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_app_list, container, false)

        val recyclerView = view.findViewById<RecyclerView>(R.id.app_list)
        recyclerView.adapter = adapter
        recyclerView.layoutManager = GridLayoutManager(
            context,
            requireContext().resources.getInteger(R.integer.launcher_columns)
        )

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launcherViewModel.iconList.collect {
                    adapter.submitList(launcherViewModel.iconList.value)
                }
            }
        }

        val filterChips: ChipGroup = view.findViewById(R.id.filter_list)
        LauncherViewModel.FILTERS.forEach { filter ->
            Chip(context).apply {
                setText(filter.nameResId)
                tag = filter
                chipStrokeWidth = 0f
                setOnClickListener { launcherViewModel.setFilter(filter) }
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

        view.findViewById<MaterialButton>(R.id.settings_button).setOnClickListener { button ->
            requireContext().startActivity(
                Intent(android.provider.Settings.ACTION_SETTINGS).also {
                    it.sourceBounds = button.getLocationOnScreen()
                },
                button.makeClipRevealAnimation()
            )
        }

        return view
    }
}
