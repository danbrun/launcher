package link.danb.launcher

import android.appwidget.AppWidgetHostView
import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.Intent
import android.content.res.Resources
import android.graphics.Rect
import android.os.Build
import android.os.Bundle
import android.util.SizeF
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.ListView
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
    private val widgetViewModel: WidgetViewModel by activityViewModels()

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

        view.findViewById<MaterialButton>(R.id.widget_button).apply {
            setOnClickListener {
                WidgetDialogFragment().show(childFragmentManager, WidgetDialogFragment.TAG)
            }
            setOnLongClickListener {
                widgetViewModel.unbind()
                true
            }
        }

        var widgetViews: List<AppWidgetHostView> = listOf()
        val widgetContainer = view.findViewById<LinearLayout>(R.id.widget)
        widgetContainer.addOnLayoutChangeListener { _, _, _, _, _, _, _, _, _ ->
            widgetViews.forEach {
                it.updateAppWidgetSize(
                    widgetContainer.measuredWidth,
                    widgetContainer.context.pixelsToDips(
                        resources.getDimensionPixelSize(R.dimen.widget_max_height)
                    )
                )
            }
        }
        widgetViewModel.widgetHandles.observe(viewLifecycleOwner) {
            widgetContainer.apply {
                removeAllViews()
                widgetViews = it.map { widgetViewModel.getView(it) }
                widgetViews.forEach { addView(it) }
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

    companion object {
        fun Context.pixelsToDips(pixels: Int): Int {
            return (pixels / resources.displayMetrics.density).toInt()
        }

        fun AppWidgetHostView.updateAppWidgetSize(maxWidthPixels: Int, maxHeightPixels: Int) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                updateAppWidgetSize(
                    Bundle(),
                    listOf(SizeF(maxWidthPixels.toFloat(), maxHeightPixels.toFloat()))
                )
            } else {
                @Suppress("DEPRECATION")
                updateAppWidgetSize(Bundle(), 0, 0, maxWidthPixels, maxHeightPixels)
            }
        }
    }
}
