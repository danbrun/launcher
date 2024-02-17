package link.danb.launcher.activities

import android.appwidget.AppWidgetManager
import android.content.Intent
import android.net.Uri
import android.os.Bundle
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
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import link.danb.launcher.R
import link.danb.launcher.apps.LauncherResourceProvider
import link.danb.launcher.components.UserActivity
import link.danb.launcher.components.UserShortcut
import link.danb.launcher.components.UserShortcutCreator
import link.danb.launcher.database.ActivityData
import link.danb.launcher.extensions.boundsOnScreen
import link.danb.launcher.extensions.getParcelableCompat
import link.danb.launcher.extensions.makeScaleUpAnimation
import link.danb.launcher.extensions.setSpanSizeProvider
import link.danb.launcher.shortcuts.ShortcutManager
import link.danb.launcher.tiles.CardTileViewBinder
import link.danb.launcher.tiles.CardTileViewHolder
import link.danb.launcher.tiles.TileViewItem
import link.danb.launcher.tiles.TileViewItemFactory
import link.danb.launcher.ui.DialogSubtitleViewBinder
import link.danb.launcher.ui.DialogSubtitleViewItem
import link.danb.launcher.ui.DynamicGridLayoutManager
import link.danb.launcher.ui.ViewBinderAdapter
import link.danb.launcher.ui.ViewItem
import link.danb.launcher.widgets.AppWidgetSetupActivityResultContract
import link.danb.launcher.widgets.AppWidgetSetupActivityResultContract.AppWidgetSetupInput
import link.danb.launcher.widgets.AppWidgetViewProvider
import link.danb.launcher.widgets.WidgetPreviewViewBinder
import link.danb.launcher.widgets.WidgetPreviewViewItem
import link.danb.launcher.widgets.WidgetsViewModel

@AndroidEntryPoint
class ActivityDetailsDialogFragment : BottomSheetDialogFragment() {

  private val activitiesViewModel: ActivitiesViewModel by activityViewModels()
  private val widgetsViewModel: WidgetsViewModel by activityViewModels()

  @Inject lateinit var activityManager: ActivityManager
  @Inject lateinit var appWidgetManager: AppWidgetManager
  @Inject lateinit var appWidgetViewProvider: AppWidgetViewProvider
  @Inject lateinit var launcherResourceProvider: LauncherResourceProvider
  @Inject lateinit var shortcutManager: ShortcutManager
  @Inject lateinit var tileViewItemFactory: TileViewItemFactory

  private val userActivity: UserActivity by lazy {
    arguments?.getParcelableCompat(EXTRA_USER_COMPONENT)!!
  }

  private val activityData: Flow<ActivityData> by lazy {
    activityManager.data.map { activities -> activities.first { it.userActivity == userActivity } }
  }

  private val shortcutActivityLauncher =
    registerForActivityResult(
      ActivityResultContracts.StartIntentSenderForResult(),
      ::onPinShortcutActivityResult,
    )

  private val bindWidgetActivityLauncher =
    registerForActivityResult(AppWidgetSetupActivityResultContract()) {
      if (it.success) {
        Toast.makeText(context, R.string.pinned_widget, Toast.LENGTH_SHORT).show()
        dismiss()
      } else {
        Toast.makeText(context, it.errorMessage, Toast.LENGTH_SHORT).show()
      }
      widgetsViewModel.checkForNewWidgets()
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
        ActivityHeaderViewBinder(
          { _, it -> onPinButtonClick(it) },
          { _, it -> onVisibilityButtonClick(it) },
          ::onUninstallButtonClick,
          ::onSettingsButtonClick,
        ),
        DialogSubtitleViewBinder(),
        CardTileViewBinder(::onTileClick) { _, it -> onTileLongClick(it) },
        WidgetPreviewViewBinder(appWidgetViewProvider) { _, it -> onWidgetPreviewClick(it) },
      )

    recyclerView.apply {
      this.adapter = adapter
      layoutManager =
        DynamicGridLayoutManager(context, R.dimen.min_column_width).apply {
          setSpanSizeProvider { position, spanCount ->
            when (adapter.currentList[position]) {
              is ActivityHeaderViewItem,
              is DialogSubtitleViewItem,
              is WidgetPreviewViewItem -> spanCount
              else -> 1
            }
          }
        }
    }

    viewLifecycleOwner.lifecycleScope.launch {
      viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.CREATED) {
        activityData.collect { adapter.submitList(getViewItems(it)) }
      }
    }

    return recyclerView
  }

  private suspend fun getViewItems(activityData: ActivityData): List<ViewItem> = buildList {
    add(
      ActivityHeaderViewItem(
        activityData,
        launcherResourceProvider.getIcon(activityData.userActivity).await(),
        launcherResourceProvider.getLabel(activityData.userActivity),
      )
    )

    val shortcuts =
      shortcutManager
        .getShortcuts(activityData.userActivity)
        .map { tileViewItemFactory.getTileViewItem(it, TileViewItem.Style.CARD) }
        .sortedBy { it.name.toString() }

    if (shortcuts.isNotEmpty()) {
      add(DialogSubtitleViewItem(requireContext().getString(R.string.shortcuts)))
      addAll(shortcuts)
    }

    val configurableShortcuts =
      shortcutManager
        .getShortcutCreators(activityData.userActivity)
        .map { tileViewItemFactory.getTileViewItem(it, TileViewItem.Style.CARD) }
        .sortedBy { it.name.toString() }

    if (configurableShortcuts.isNotEmpty()) {
      add(DialogSubtitleViewItem(requireContext().getString(R.string.configurable_shortcuts)))
      addAll(configurableShortcuts)
    }

    val widgets =
      appWidgetManager
        .getInstalledProvidersForPackage(
          activityData.userActivity.componentName.packageName,
          activityData.userActivity.userHandle,
        )
        .map { WidgetPreviewViewItem(it, activityData.userActivity.userHandle) }

    if (widgets.isNotEmpty()) {
      add(DialogSubtitleViewItem(requireContext().getString(R.string.widgets)))
      addAll(widgets)
    }
  }

  private fun onUninstallButtonClick(view: View, viewItem: ActivityHeaderViewItem) {
    val packageName = viewItem.data.userActivity.componentName.packageName
    view.context.startActivity(
      Intent(Intent.ACTION_DELETE)
        .setData(Uri.parse("package:$packageName"))
        .putExtra(Intent.EXTRA_USER, viewItem.data.userActivity.userHandle)
    )
    dismiss()
  }

  private fun onSettingsButtonClick(view: View, viewItem: ActivityHeaderViewItem) {
    activityManager.launchAppDetails(
      viewItem.data.userActivity,
      view.boundsOnScreen,
      view.makeScaleUpAnimation().toBundle(),
    )
    dismiss()
  }

  private fun onPinButtonClick(viewItem: ActivityHeaderViewItem) {
    activitiesViewModel.setMetadata(viewItem.data.copy(isPinned = !viewItem.data.isPinned))
    dismiss()
  }

  private fun onVisibilityButtonClick(viewItem: ActivityHeaderViewItem) {
    activitiesViewModel.setMetadata(viewItem.data.copy(isHidden = !viewItem.data.isHidden))
    dismiss()
  }

  private fun onTileClick(holder: CardTileViewHolder, data: Any) =
    when (data) {
      is UserShortcut -> {
        shortcutManager.launchShortcut(
          data,
          holder.iconView.boundsOnScreen,
          holder.iconView.makeScaleUpAnimation().toBundle(),
        )
        dismiss()
      }
      is UserShortcutCreator -> {
        shortcutActivityLauncher.launch(
          IntentSenderRequest.Builder(shortcutManager.getShortcutCreatorIntent(data)).build()
        )
      }
      else -> throw NotImplementedError()
    }

  private fun onTileLongClick(data: Any) =
    when (data) {
      is UserShortcut -> {
        shortcutManager.pinShortcut(data, isPinned = true)
        Toast.makeText(context, R.string.pinned_shortcut, Toast.LENGTH_SHORT).show()
      }
      is UserShortcutCreator -> Unit
      else -> throw NotImplementedError()
    }

  private fun onWidgetPreviewClick(widgetPreviewViewItem: WidgetPreviewViewItem) {
    bindWidgetActivityLauncher.launch(
      AppWidgetSetupInput(widgetPreviewViewItem.providerInfo, userActivity.userHandle)
    )
  }

  private fun onPinShortcutActivityResult(activityResult: ActivityResult) {
    val data = activityResult.data ?: return
    shortcutManager.acceptPinRequest(data)
    Toast.makeText(context, R.string.pinned_shortcut, Toast.LENGTH_SHORT).show()
    dismiss()
  }

  companion object {
    const val TAG = "app_options_dialog_fragment"

    private const val EXTRA_USER_COMPONENT: String = "extra_user_component"

    fun newInstance(activityData: ActivityData): ActivityDetailsDialogFragment =
      ActivityDetailsDialogFragment().apply {
        arguments =
          Bundle().apply { putParcelable(EXTRA_USER_COMPONENT, activityData.userActivity) }
      }
  }
}
