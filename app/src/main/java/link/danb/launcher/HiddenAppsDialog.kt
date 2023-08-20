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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import link.danb.launcher.list.CardTileViewBinder
import link.danb.launcher.list.DialogHeaderViewBinder
import link.danb.launcher.list.DialogHeaderViewItem
import link.danb.launcher.list.LoadingSpinnerViewBinder
import link.danb.launcher.list.LoadingSpinnerViewItem
import link.danb.launcher.list.TileViewItem
import link.danb.launcher.list.ViewBinderAdapter
import link.danb.launcher.model.ActivityTileData
import link.danb.launcher.model.LauncherIconProvider
import link.danb.launcher.model.LauncherViewModel
import link.danb.launcher.model.ShortcutTileData
import link.danb.launcher.model.TileData
import link.danb.launcher.utils.getBoundsOnScreen
import link.danb.launcher.utils.makeClipRevealAnimation
import javax.inject.Inject

@AndroidEntryPoint
class HiddenAppsDialog : BottomSheetDialogFragment() {

    private val launcherViewModel: LauncherViewModel by activityViewModels()

    @Inject
    lateinit var launcherApps: LauncherApps

    @Inject
    lateinit var launcherIconProvider: LauncherIconProvider

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        super.onCreateView(inflater, container, savedInstanceState)

        val recyclerView = inflater.inflate(
            R.layout.hidden_apps_dialog_fragment, container, false
        ) as RecyclerView

        val adapter = ViewBinderAdapter(DialogHeaderViewBinder(),
            LoadingSpinnerViewBinder(),
            CardTileViewBinder(this::onTileClick) { _, it -> onTileLongClick(it) })

        val columns = requireContext().resources.getInteger(R.integer.launcher_columns)

        recyclerView.adapter = adapter
        recyclerView.isNestedScrollingEnabled = true
        recyclerView.layoutManager = GridLayoutManager(
            context, columns
        ).apply {
            spanSizeLookup = object : GridLayoutManager.SpanSizeLookup() {
                override fun getSpanSize(position: Int): Int {
                    return when (adapter.currentList[position]) {
                        is DialogHeaderViewItem, is LoadingSpinnerViewItem -> columns
                        else -> 1
                    }
                }
            }
        }

        val headerItems =
            listOf(DialogHeaderViewItem(requireContext().getString(R.string.hidden_apps)))

        adapter.submitList(headerItems + listOf(LoadingSpinnerViewItem()))

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    launcherViewModel.launcherActivities.collect { activities ->
                        adapter.submitList(
                            async(Dispatchers.IO) {
                                headerItems + activities.filter { !launcherViewModel.isVisible(it.info) }
                                    .sortedBy { it.info.label.toString().lowercase() }.map {
                                        TileViewItem.cardTileViewItem(
                                            ActivityTileData(it.info),
                                            it.info.label,
                                            launcherIconProvider.get(it.info)
                                        )
                                    }
                            }.await()
                        )
                    }
                }
            }
        }

        return recyclerView
    }

    private fun onTileClick(view: View, tileData: TileData) {
        when (tileData) {
            is ActivityTileData -> {
                launcherApps.startMainActivity(
                    tileData.info.componentName,
                    tileData.info.user,
                    view.getBoundsOnScreen(),
                    view.makeClipRevealAnimation()
                )
                dismiss()
            }

            is ShortcutTileData -> throw NotImplementedError()
        }
    }

    private fun onTileLongClick(tileData: TileData) {
        when (tileData) {
            is ActivityTileData -> {
                ActivityDetailsDialogFragment.newInstance(tileData.info)
                    .show(parentFragmentManager, ActivityDetailsDialogFragment.TAG)
            }

            is ShortcutTileData -> throw NotImplementedError()
        }
    }

    companion object {
        const val TAG = "hidden_apps_dialog"
    }
}
