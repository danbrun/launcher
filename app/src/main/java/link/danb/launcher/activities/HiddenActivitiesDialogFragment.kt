package link.danb.launcher.activities

import android.content.pm.LauncherApps
import android.os.Bundle
import android.os.UserHandle
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
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import link.danb.launcher.R
import link.danb.launcher.tiles.CardTileViewBinder
import link.danb.launcher.ui.DialogHeaderViewBinder
import link.danb.launcher.ui.DialogHeaderViewItem
import link.danb.launcher.ui.LoadingSpinnerViewBinder
import link.danb.launcher.ui.LoadingSpinnerViewItem
import link.danb.launcher.tiles.TileViewItem
import link.danb.launcher.ui.ViewBinderAdapter
import link.danb.launcher.tiles.ActivityTileData
import link.danb.launcher.icons.LauncherIconCache
import link.danb.launcher.tiles.ShortcutTileData
import link.danb.launcher.tiles.TileData
import link.danb.launcher.profiles.ProfilesModel
import link.danb.launcher.ui.ViewItem
import link.danb.launcher.extensions.getBoundsOnScreen
import link.danb.launcher.extensions.makeClipRevealAnimation
import javax.inject.Inject

@AndroidEntryPoint
class HiddenActivitiesDialogFragment : BottomSheetDialogFragment() {

    private val activitiesViewModel: ActivitiesViewModel by activityViewModels()

    @Inject
    lateinit var launcherApps: LauncherApps

    @Inject
    lateinit var launcherIconCache: LauncherIconCache

    @Inject
    lateinit var profilesModel: ProfilesModel

    private val header by lazy {
        DialogHeaderViewItem(
            requireContext().getString(R.string.hidden_apps), R.drawable.ic_baseline_visibility_24
        )
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        super.onCreateView(inflater, container, savedInstanceState)

        val recyclerView = inflater.inflate(
            R.layout.recycler_view_dialog_fragment, container, false
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

        adapter.submitList(listOf(header, LoadingSpinnerViewItem))

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    combine(
                        activitiesViewModel.launcherActivities, profilesModel.activeProfile
                    ) { activities, currentUser ->
                        async(Dispatchers.IO) { getViewItems(activities, currentUser) }.await()
                    }.collect(adapter::submitList)
                }
            }
        }

        return recyclerView
    }

    private suspend fun getViewItems(
        activities: List<ActivityInfoWithData>, activeProfile: UserHandle
    ): List<ViewItem> = buildList {
        add(header)

        val activityItems =
            activities.filter { it.data.isHidden && it.data.userHandle == activeProfile }
                .sortedBy { it.info.label.toString().lowercase() }.map {
                    TileViewItem.cardTileViewItem(
                        ActivityTileData(it.info), it.info.label, launcherIconCache.get(it.info)
                    )
                }

        addAll(activityItems)
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
