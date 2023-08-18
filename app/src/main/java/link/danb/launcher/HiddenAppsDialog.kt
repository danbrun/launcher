package link.danb.launcher

import android.content.pm.LauncherApps
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import link.danb.launcher.list.ActivityHeaderViewItem
import link.danb.launcher.list.CardTileViewBinder
import link.danb.launcher.list.CardTileViewItem
import link.danb.launcher.list.ViewBinderAdapter
import link.danb.launcher.list.WidgetPreviewViewItem
import link.danb.launcher.model.LauncherActivityData
import link.danb.launcher.model.LauncherShortcutData
import link.danb.launcher.model.LauncherViewModel
import link.danb.launcher.model.TileViewData
import link.danb.launcher.utils.getBoundsOnScreen
import link.danb.launcher.utils.makeClipRevealAnimation
import javax.inject.Inject

@AndroidEntryPoint
class HiddenAppsDialog : BottomSheetDialogFragment() {

    private val launcherViewModel: LauncherViewModel by activityViewModels()

    @Inject
    lateinit var launcherApps: LauncherApps

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        super.onCreateView(inflater, container, savedInstanceState)

        val recyclerView = inflater.inflate(
            R.layout.hidden_apps_dialog_fragment, container, false
        ) as RecyclerView

        val adapter =
            ViewBinderAdapter(CardTileViewBinder(this::onTileClick) { _, it -> onTileLongClick(it) })

        val columns = requireContext().resources.getInteger(R.integer.launcher_columns)

        recyclerView.adapter = adapter
        recyclerView.isNestedScrollingEnabled = true
        recyclerView.layoutManager = GridLayoutManager(
            context, columns
        ).apply {
            spanSizeLookup = object : GridLayoutManager.SpanSizeLookup() {
                override fun getSpanSize(position: Int): Int {
                    return when (adapter.currentList[position]) {
                        is ActivityHeaderViewItem, is WidgetPreviewViewItem -> columns
                        else -> 1
                    }
                }
            }
        }


        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    launcherViewModel.launcherActivities.collect { activities ->
                        adapter.submitList(activities.filter { !launcherViewModel.isVisible(it) }
                            .sortedBy { it.name.toString().lowercase() }
                            .map { CardTileViewItem(it) })
                    }
                }
            }
        }

        return recyclerView
    }

    private fun onTileClick(view: View, tileViewData: TileViewData) {
        when (tileViewData) {
            is LauncherActivityData -> {
                launcherApps.startMainActivity(
                    tileViewData.component,
                    tileViewData.user,
                    view.getBoundsOnScreen(),
                    view.makeClipRevealAnimation()
                )
                dismiss()
            }

            is LauncherShortcutData -> Unit
        }
    }

    private fun onTileLongClick(tileViewData: TileViewData) {
        when (tileViewData) {
            is LauncherActivityData -> {
                ActivityDetailsDialogFragment.newInstance(tileViewData)
                    .show(parentFragmentManager, ActivityDetailsDialogFragment.TAG)
            }

            is LauncherShortcutData -> Unit
        }
    }

    companion object {
        const val TAG = "hidden_apps_dialog"
    }
}
