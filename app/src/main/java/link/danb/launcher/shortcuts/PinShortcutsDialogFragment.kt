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
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.flow.transform
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import link.danb.launcher.R
import link.danb.launcher.activities.ActivitiesViewModel
import link.danb.launcher.database.ActivityData
import link.danb.launcher.extensions.getConfigurableShortcuts
import link.danb.launcher.extensions.setSpanSizeProvider
import link.danb.launcher.extensions.toConfigurableShortcutData
import link.danb.launcher.extensions.toShortcutData
import link.danb.launcher.profiles.ProfilesModel
import link.danb.launcher.tiles.CardTileViewBinder
import link.danb.launcher.tiles.TileViewItem
import link.danb.launcher.tiles.TileViewItemFactory
import link.danb.launcher.ui.DialogHeaderViewBinder
import link.danb.launcher.ui.DialogHeaderViewItem
import link.danb.launcher.ui.GroupHeaderViewBinder
import link.danb.launcher.ui.LoadingSpinnerViewBinder
import link.danb.launcher.ui.LoadingSpinnerViewItem
import link.danb.launcher.ui.ViewBinderAdapter
import link.danb.launcher.ui.ViewItem

@AndroidEntryPoint
class PinShortcutsDialogFragment : BottomSheetDialogFragment() {

  private val activitiesViewModel: ActivitiesViewModel by activityViewModels()
  private val shortcutsViewModel: ShortcutsViewModel by activityViewModels()

  @Inject lateinit var appWidgetHost: AppWidgetHost
  @Inject lateinit var appWidgetManager: AppWidgetManager
  @Inject lateinit var launcherApps: LauncherApps
  @Inject lateinit var profilesModel: ProfilesModel
  @Inject lateinit var tileViewItemFactory: TileViewItemFactory

  private val shortcutActivityLauncher =
    registerForActivityResult(
      ActivityResultContracts.StartIntentSenderForResult(),
      ::onPinShortcutActivityResult
    )

  private val header by lazy {
    DialogHeaderViewItem(
      requireContext().getString(R.string.shortcuts),
      R.drawable.baseline_push_pin_24
    )
  }

  override fun onCreateView(
    inflater: LayoutInflater,
    container: ViewGroup?,
    savedInstanceState: Bundle?
  ): View {
    super.onCreateView(inflater, container, savedInstanceState)

    val recyclerView =
      inflater.inflate(R.layout.recycler_view_dialog_fragment, container, false) as RecyclerView

    val adapter =
      ViewBinderAdapter(
        DialogHeaderViewBinder(),
        GroupHeaderViewBinder(),
        LoadingSpinnerViewBinder(),
        CardTileViewBinder({ _, it -> onTileClick(it) }),
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
                is TileViewItem -> 1
                else -> spanCount
              }
            }
          }
    }

    adapter.submitList(listOf(header, LoadingSpinnerViewItem))

    viewLifecycleOwner.lifecycleScope.launch {
      viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.RESUMED) {
        combine(activitiesViewModel.activities, profilesModel.activeProfile, ::getViewItems)
          .collect { adapter.submitList(listOf(header) + it) }
      }
    }

    return recyclerView
  }

  private suspend fun getViewItems(
    activityData: List<ActivityData>,
    activeProfile: UserHandle
  ): List<ViewItem> =
    withContext(Dispatchers.IO) {
      activityData
        .asFlow()
        .filter { it.userHandle == activeProfile }
        .transform {
          emitAll(
            launcherApps
              .getConfigurableShortcuts(it.componentName.packageName, it.userHandle)
              .asFlow()
          )
        }
        .map {
          tileViewItemFactory.getTileViewItem(
            it.toConfigurableShortcutData(),
            TileViewItem.Style.CARD
          )
        }
        .toList()
        .sortedBy { it.name.toString().lowercase() }
    }

  private fun onTileClick(data: Any) {
    when (data) {
      is ConfigurableShortcutData ->
        shortcutActivityLauncher.launch(
          IntentSenderRequest.Builder(shortcutsViewModel.getConfigurableShortcutIntent(data))
            .build()
        )
      else -> throw NotImplementedError()
    }
  }

  private fun onPinShortcutActivityResult(activityResult: ActivityResult) {
    if (activityResult.data == null) return

    val pinItemRequest = launcherApps.getPinItemRequest(activityResult.data) ?: return
    if (!pinItemRequest.isValid) return
    if (pinItemRequest.requestType != LauncherApps.PinItemRequest.REQUEST_TYPE_SHORTCUT) return

    val info = pinItemRequest.shortcutInfo ?: return

    pinItemRequest.accept()
    shortcutsViewModel.pinShortcut(info.toShortcutData())
    Toast.makeText(context, R.string.pinned_shortcut, Toast.LENGTH_SHORT).show()
    dismiss()
  }

  companion object {
    const val TAG = "widget_dialog_fragment"
  }
}
