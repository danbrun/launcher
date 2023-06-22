package link.danb.launcher

import android.content.Intent
import android.provider.Settings
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import androidx.core.view.MenuProvider
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.*
import kotlinx.coroutines.launch
import link.danb.launcher.model.LauncherViewModel
import link.danb.launcher.widgets.WidgetDialogFragment
import javax.inject.Inject

class LauncherMenuProvider @Inject constructor(private val fragment: Fragment) :
    DefaultLifecycleObserver, MenuProvider {

    private val launcherViewModel: LauncherViewModel by fragment.activityViewModels()

    private lateinit var profileToggle: MenuItem
    private lateinit var visibilityToggle: MenuItem
    private lateinit var addWidgetButton: MenuItem
    private lateinit var settingsShortcut: MenuItem

    init {
        fragment.lifecycle.addObserver(this)
    }

    override fun onStart(owner: LifecycleOwner) {
        fragment.viewLifecycleOwner.lifecycleScope.launch {
            fragment.viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    launcherViewModel.activitiesMetadata.collect {
                        profileToggle.isVisible = it.hasWorkActivities
                        visibilityToggle.isVisible = it.hasHiddenActivities
                    }
                }

                launch {
                    launcherViewModel.activitiesFilter.collect {
                        profileToggle.setTitle(
                            if (it.showWorkActivities) R.string.show_personal
                            else R.string.show_work
                        )
                        profileToggle.setIcon(
                            if (it.showWorkActivities) R.drawable.ic_baseline_work_off_24
                            else R.drawable.ic_baseline_work_24
                        )

                        visibilityToggle.setTitle(
                            if (it.showHiddenActivities) R.string.hide_hidden
                            else R.string.show_hidden
                        )
                        visibilityToggle.setIcon(
                            if (it.showHiddenActivities) R.drawable.ic_baseline_visibility_off_24
                            else R.drawable.ic_baseline_visibility_24
                        )
                    }
                }
            }
        }
    }

    override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
        menuInflater.inflate(R.menu.launcher_menu, menu)

        profileToggle = menu.findItem(R.id.profile_toggle)
        visibilityToggle = menu.findItem(R.id.visibility_toggle)
        addWidgetButton = menu.findItem(R.id.add_widget_button)
        settingsShortcut = menu.findItem(R.id.settings_shortcut)
    }

    override fun onMenuItemSelected(menuItem: MenuItem): Boolean = when (menuItem.itemId) {
        R.id.profile_toggle -> {
            launcherViewModel.activitiesFilter.apply {
                value = value.copy(showWorkActivities = !value.showWorkActivities)
            }
            true
        }
        R.id.visibility_toggle -> {
            launcherViewModel.activitiesFilter.apply {
                value = value.copy(showHiddenActivities = !value.showHiddenActivities)
            }
            true
        }
        R.id.add_widget_button -> {
            WidgetDialogFragment().showNow(
                fragment.childFragmentManager, WidgetDialogFragment.TAG
            )
            true
        }
        R.id.settings_shortcut -> {
            fragment.startActivity(Intent(Settings.ACTION_SETTINGS))
            true
        }
        else -> false
    }
}