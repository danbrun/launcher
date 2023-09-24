package link.danb.launcher.widgets

import android.appwidget.AppWidgetHost
import android.appwidget.AppWidgetManager
import android.content.pm.ApplicationInfo
import android.content.pm.LauncherApps
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import link.danb.launcher.R
import link.danb.launcher.icons.LauncherIconCache
import link.danb.launcher.profiles.ProfilesModel
import link.danb.launcher.ui.DialogHeaderViewBinder
import link.danb.launcher.ui.DialogHeaderViewItem
import link.danb.launcher.ui.LoadingSpinnerViewBinder
import link.danb.launcher.ui.LoadingSpinnerViewItem
import link.danb.launcher.ui.ViewBinderAdapter
import link.danb.launcher.ui.ViewItem
import link.danb.launcher.widgets.AppWidgetSetupActivityResultContract.AppWidgetSetupInput
import link.danb.launcher.widgets.WidgetHeaderViewItem.WidgetHeaderViewItemFactory
import javax.inject.Inject

@AndroidEntryPoint
class PinWidgetsDialogFragment : BottomSheetDialogFragment() {

  private val widgetsViewModel: WidgetsViewModel by activityViewModels()
  private val widgetDialogViewModel: WidgetDialogViewModel by viewModels()

  @Inject lateinit var appWidgetHost: AppWidgetHost
  @Inject lateinit var appWidgetManager: AppWidgetManager
  @Inject lateinit var appWidgetViewProvider: AppWidgetViewProvider
  @Inject lateinit var widgetHeaderViewItemFactory: WidgetHeaderViewItemFactory
  @Inject lateinit var launcherIconCache: LauncherIconCache
  @Inject lateinit var launcherApps: LauncherApps
  @Inject lateinit var profilesModel: ProfilesModel

  private val packageManager: PackageManager by lazy { requireContext().packageManager }

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

  private val header by lazy {
    DialogHeaderViewItem(
      requireContext().getString(R.string.widgets),
      R.drawable.ic_baseline_widgets_24
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
        LoadingSpinnerViewBinder(),
        WidgetHeaderViewBinder { widgetDialogViewModel.toggleExpandedPackageName(it.packageName) },
        WidgetPreviewViewBinder(appWidgetViewProvider) { _, it -> onWidgetPreviewClick(it) },
      )

    recyclerView.apply {
      this.adapter = adapter
      layoutManager = LinearLayoutManager(context)
    }

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
      val user = profilesModel.activeProfile.value
      appWidgetManager
        .getInstalledProvidersForProfile(user)
        .groupBy { it.provider.packageName }
        .mapKeys { launcherApps.getApplicationInfo(it.key, 0, user) }
        .toSortedMap(
          compareBy<ApplicationInfo> { it.loadLabel(packageManager).toString().lowercase() }
        )
        .flatMap { (appInfo, widgets) ->
          buildList {
            val isExpanded = expandedPackages.contains(appInfo.packageName)
            add(widgetHeaderViewItemFactory.create(appInfo, user, isExpanded))
            if (isExpanded) {
              addAll(widgets.map { WidgetPreviewViewItem(it, user) })
            }
          }
        }
    }

  private fun onWidgetPreviewClick(widgetPreviewViewItem: WidgetPreviewViewItem) {
    bindWidgetActivityLauncher.launch(
      AppWidgetSetupInput(widgetPreviewViewItem.providerInfo, profilesModel.activeProfile.value)
    )
  }

  companion object {
    const val TAG = "widget_dialog_fragment"
  }
}
