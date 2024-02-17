package link.danb.launcher.widgets

import android.appwidget.AppWidgetManager
import android.os.Bundle
import android.os.UserHandle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.os.bundleOf
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import link.danb.launcher.R
import link.danb.launcher.apps.LauncherResourceProvider
import link.danb.launcher.components.UserApplication
import link.danb.launcher.extensions.getParcelableCompat
import link.danb.launcher.ui.DialogHeaderViewBinder
import link.danb.launcher.ui.DialogHeaderViewItem
import link.danb.launcher.ui.LoadingSpinnerViewBinder
import link.danb.launcher.ui.LoadingSpinnerViewItem
import link.danb.launcher.ui.ViewBinderAdapter
import link.danb.launcher.ui.ViewItem
import link.danb.launcher.widgets.AppWidgetSetupActivityResultContract.AppWidgetSetupInput
import link.danb.launcher.widgets.WidgetHeaderViewItem.WidgetHeaderViewItemFactory

@AndroidEntryPoint
class PinWidgetsDialogFragment : BottomSheetDialogFragment() {

  private val widgetsViewModel: WidgetsViewModel by activityViewModels()
  private val widgetDialogViewModel: WidgetDialogViewModel by viewModels()

  @Inject lateinit var appWidgetManager: AppWidgetManager
  @Inject lateinit var appWidgetViewProvider: AppWidgetViewProvider
  @Inject lateinit var launcherResourceProvider: LauncherResourceProvider
  @Inject lateinit var widgetHeaderViewItemFactory: WidgetHeaderViewItemFactory

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

  private val userHandle: UserHandle by lazy {
    checkNotNull(requireArguments().getParcelableCompat(EXTRA_USER_HANDLE))
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
        WidgetHeaderViewBinder { widgetDialogViewModel.toggleExpandedPackageName(it.packageName) },
        WidgetPreviewViewBinder(appWidgetViewProvider) { _, it -> onWidgetPreviewClick(it) },
      )

    recyclerView.apply {
      this.adapter = adapter
      layoutManager = LinearLayoutManager(context)
    }

    val header =
      DialogHeaderViewItem(
        requireContext().getString(R.string.widgets),
        R.drawable.ic_baseline_widgets_24,
      )

    adapter.submitList(listOf(header, LoadingSpinnerViewItem))

    viewLifecycleOwner.lifecycleScope.launch {
      viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.RESUMED) {
        widgetDialogViewModel.expandedPackageNames.collect {
          adapter.submitList(listOf(header) + getViewItems(it))
        }
      }
    }

    return recyclerView
  }

  private suspend fun getViewItems(expandedPackages: Set<String>): List<ViewItem> =
    withContext(Dispatchers.IO) {
      appWidgetManager
        .getInstalledProvidersForProfile(userHandle)
        .groupBy { UserApplication(it.provider.packageName, userHandle) }
        .toSortedMap(compareBy { launcherResourceProvider.getLabel(it).lowercase() })
        .flatMap { (appInfo, widgets) ->
          buildList {
            val isExpanded = expandedPackages.contains(appInfo.packageName)
            add(widgetHeaderViewItemFactory.create(appInfo, isExpanded))
            if (isExpanded) {
              addAll(widgets.map { WidgetPreviewViewItem(it, userHandle) })
            }
          }
        }
    }

  private fun onWidgetPreviewClick(widgetPreviewViewItem: WidgetPreviewViewItem) {
    bindWidgetActivityLauncher.launch(
      AppWidgetSetupInput(widgetPreviewViewItem.providerInfo, userHandle)
    )
  }

  companion object {
    const val TAG = "widget_dialog_fragment"

    private const val EXTRA_USER_HANDLE = "extra_user_handle"

    fun newInstance(userHandle: UserHandle): PinWidgetsDialogFragment =
      PinWidgetsDialogFragment().apply { arguments = bundleOf(EXTRA_USER_HANDLE to userHandle) }
  }
}
