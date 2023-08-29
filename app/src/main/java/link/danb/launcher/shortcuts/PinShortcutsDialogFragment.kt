package link.danb.launcher.shortcuts

import android.appwidget.AppWidgetHost
import android.appwidget.AppWidgetManager
import android.content.pm.LauncherApps
import android.os.Bundle
import android.os.UserHandle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.ActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import link.danb.launcher.R
import link.danb.launcher.tiles.ActivityTileData
import link.danb.launcher.icons.LauncherIconCache
import link.danb.launcher.activities.ActivitiesViewModel
import link.danb.launcher.activities.ActivityInfoWithData
import link.danb.launcher.extensions.setSpanSizeProvider
import link.danb.launcher.tiles.CardTileViewBinder
import link.danb.launcher.tiles.TileData
import link.danb.launcher.tiles.TileViewItem
import link.danb.launcher.ui.DialogHeaderViewBinder
import link.danb.launcher.ui.DialogHeaderViewItem
import link.danb.launcher.ui.GroupHeaderViewBinder
import link.danb.launcher.ui.LoadingSpinnerViewBinder
import link.danb.launcher.ui.LoadingSpinnerViewItem
import link.danb.launcher.ui.ViewBinderAdapter
import link.danb.launcher.ui.ViewItem
import link.danb.launcher.profiles.ProfilesModel
import javax.inject.Inject

@AndroidEntryPoint
class PinShortcutsDialogFragment : BottomSheetDialogFragment() {

    private val activitiesViewModel: ActivitiesViewModel by activityViewModels()
    private val shortcutsViewModel: ShortcutsViewModel by activityViewModels()

    @Inject
    lateinit var appWidgetHost: AppWidgetHost

    @Inject
    lateinit var appWidgetManager: AppWidgetManager

    @Inject
    lateinit var launcherIconCache: LauncherIconCache

    @Inject
    lateinit var launcherApps: LauncherApps

    @Inject
    lateinit var profilesModel: ProfilesModel

    private val shortcutActivityLauncher = registerForActivityResult(
        ActivityResultContracts.StartIntentSenderForResult(), ::onPinShortcutActivityResult
    )

    private val headerItems by lazy {
        listOf(
            DialogHeaderViewItem(
                requireContext().getString(R.string.shortcuts), R.drawable.baseline_push_pin_24
            )
        )
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        super.onCreateView(inflater, container, savedInstanceState)

        val recyclerView = inflater.inflate(
            R.layout.recycler_view_dialog_fragment, container, false
        ) as RecyclerView

        val adapter = ViewBinderAdapter(
            DialogHeaderViewBinder(),
            GroupHeaderViewBinder(),
            LoadingSpinnerViewBinder(),
            CardTileViewBinder(::onTileClick),
        )

        val gridLayoutManager = GridLayoutManager(
            context, requireContext().resources.getInteger(R.integer.launcher_columns)
        ).setSpanSizeProvider { position, spanCount ->
            when (adapter.currentList[position]) {
                is TileViewItem -> 1
                else -> spanCount
            }
        }

        recyclerView.apply {
            this.adapter = adapter
            layoutManager = gridLayoutManager
        }

        adapter.submitList(headerItems + listOf(LoadingSpinnerViewItem))

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.RESUMED) {
                combine(
                    activitiesViewModel.launcherActivities,
                    profilesModel.activeProfile,
                    ::getViewItems
                ).collect { adapter.submitList(it) }
            }
        }

        return recyclerView
    }

    private suspend fun getViewItems(
        shortcutActivities: List<ActivityInfoWithData>, activeProfile: UserHandle
    ): List<ViewItem> = buildList {
        addAll(headerItems)

        addAll(shortcutActivities.filter { it.info.user == activeProfile }.flatMap {
            launcherApps.getShortcutConfigActivityList(
                it.info.componentName.packageName, it.info.user
            )
        }.sortedBy { it.label.toString().lowercase() }.map {
            TileViewItem.cardTileViewItem(
                ActivityTileData(it), it.label, launcherIconCache.get(it)
            )
        })
    }

    private fun onTileClick(view: View, tileData: TileData) {
        if (tileData !is ActivityTileData) return

        shortcutActivityLauncher.launch(
            IntentSenderRequest.Builder(
                launcherApps.getShortcutConfigActivityIntent(tileData.info)!!
            ).build()
        )
    }

    private fun onPinShortcutActivityResult(activityResult: ActivityResult) {
        if (activityResult.data == null) return

        val pinItemRequest = launcherApps.getPinItemRequest(activityResult.data) ?: return
        if (!pinItemRequest.isValid) return
        if (pinItemRequest.requestType != LauncherApps.PinItemRequest.REQUEST_TYPE_SHORTCUT) return

        val info = pinItemRequest.shortcutInfo ?: return

        pinItemRequest.accept()
        shortcutsViewModel.pinShortcut(info)
        Toast.makeText(context, R.string.pinned_shortcut, Toast.LENGTH_SHORT).show()
        dismiss()
    }

    companion object {
        const val TAG = "widget_dialog_fragment"
    }
}
