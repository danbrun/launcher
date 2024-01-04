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
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import link.danb.launcher.R
import link.danb.launcher.database.ActivityData
import link.danb.launcher.extensions.boundsOnScreen
import link.danb.launcher.extensions.makeScaleUpAnimation
import link.danb.launcher.extensions.setSpanSizeProvider
import link.danb.launcher.profiles.ProfilesModel
import link.danb.launcher.tiles.CardTileViewBinder
import link.danb.launcher.tiles.TileViewItem
import link.danb.launcher.tiles.TileViewItemFactory
import link.danb.launcher.ui.DialogHeaderViewBinder
import link.danb.launcher.ui.DialogHeaderViewItem
import link.danb.launcher.ui.LoadingSpinnerViewBinder
import link.danb.launcher.ui.LoadingSpinnerViewItem
import link.danb.launcher.ui.ViewBinderAdapter
import link.danb.launcher.ui.ViewItem

@AndroidEntryPoint
class HiddenActivitiesDialogFragment : BottomSheetDialogFragment() {

  private val activitiesViewModel: ActivitiesViewModel by activityViewModels()

  @Inject lateinit var launcherApps: LauncherApps
  @Inject lateinit var profilesModel: ProfilesModel
  @Inject lateinit var tileViewItemFactory: TileViewItemFactory

  private val header by lazy {
    DialogHeaderViewItem(
      requireContext().getString(R.string.hidden_apps),
      R.drawable.ic_baseline_visibility_24
    )
  }

  override fun onCreateView(
    inflater: LayoutInflater,
    container: ViewGroup?,
    savedInstanceState: Bundle?,
  ): View {
    super.onCreateView(inflater, container, savedInstanceState)

    val recyclerView =
      inflater.inflate(R.layout.recycler_view_dialog_fragment, container, false) as RecyclerView

    val adapter =
      ViewBinderAdapter(
        DialogHeaderViewBinder(),
        LoadingSpinnerViewBinder(),
        CardTileViewBinder(this::onTileClick) { _, it -> onTileLongClick(it) }
      )

    recyclerView.apply {
      this.adapter = adapter
      layoutManager =
        GridLayoutManager(
            context,
            requireContext().resources.getInteger(R.integer.launcher_columns)
          )
          .apply {
            setSpanSizeProvider { position, spanCount ->
              when (adapter.currentList[position]) {
                is DialogHeaderViewItem,
                is LoadingSpinnerViewItem -> spanCount
                else -> 1
              }
            }
          }
    }

    adapter.submitList(listOf(header, LoadingSpinnerViewItem))

    viewLifecycleOwner.lifecycleScope.launch {
      viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
        launch {
          combine(activitiesViewModel.activities, profilesModel.activeProfile) {
              activities,
              currentUser ->
              listOf(header) + getViewItems(activities, currentUser)
            }
            .collect(adapter::submitList)
        }
      }
    }

    return recyclerView
  }

  private suspend fun getViewItems(
    activities: List<ActivityData>,
    activeProfile: UserHandle,
  ): List<ViewItem> =
    withContext(Dispatchers.IO) {
      activities
        .asFlow()
        .filter { it.isHidden && it.userHandle == activeProfile }
        .map { tileViewItemFactory.getTileViewItem(it, TileViewItem.Style.CARD) }
        .toList()
        .sortedBy { it.name.toString().lowercase() }
    }

  private fun onTileClick(view: View, data: Any) {
    when (data) {
      is ActivityData -> {
        activitiesViewModel.launchActivity(
          data,
          view.boundsOnScreen,
          view.makeScaleUpAnimation().toBundle()
        )
        dismiss()
      }
      else -> throw NotImplementedError()
    }
  }

  private fun onTileLongClick(data: Any) {
    when (data) {
      is ActivityData -> {
        ActivityDetailsDialogFragment.newInstance(data)
          .show(parentFragmentManager, ActivityDetailsDialogFragment.TAG)
      }
      else -> throw NotImplementedError()
    }
  }

  companion object {
    const val TAG = "hidden_apps_dialog"
  }
}
