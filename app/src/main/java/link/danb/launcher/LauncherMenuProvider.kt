package link.danb.launcher

import android.content.Intent
import android.os.Process.myUserHandle
import android.provider.Settings
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import androidx.core.view.MenuProvider
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.*
import kotlinx.coroutines.launch
import link.danb.launcher.activities.HiddenActivitiesDialogFragment
import link.danb.launcher.activities.ActivitiesViewModel
import link.danb.launcher.work.WorkProfileViewModel
import javax.inject.Inject

class LauncherMenuProvider @Inject constructor(private val fragment: Fragment) :
    DefaultLifecycleObserver, MenuProvider {

    private val activitiesViewModel: ActivitiesViewModel by fragment.activityViewModels()
    private val workProfileViewModel: WorkProfileViewModel by fragment.activityViewModels()

    private lateinit var profileToggle: MenuItem
    private lateinit var visibilityToggle: MenuItem
    private lateinit var pinItemsButton: MenuItem
    private lateinit var settingsShortcut: MenuItem

    init {
        fragment.lifecycle.addObserver(this)
    }

    override fun onStart(owner: LifecycleOwner) {
        fragment.viewLifecycleOwner.lifecycleScope.launch {
            fragment.viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    activitiesViewModel.launcherActivities.collect { activities ->
                        profileToggle.isVisible = activities.any { it.info.user != myUserHandle() }
                        visibilityToggle.isVisible = activities.any { it.metadata.isHidden }
                    }
                }

                launch {
                    workProfileViewModel.showWorkActivities.collect {
                        profileToggle.setTitle(if (it) R.string.show_personal else R.string.show_work)
                        profileToggle.setIcon(if (it) R.drawable.ic_baseline_work_off_24 else R.drawable.ic_baseline_work_24)
                    }
                }
            }
        }
    }

    override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
        menuInflater.inflate(R.menu.launcher_menu, menu)

        profileToggle = menu.findItem(R.id.profile_toggle)
        visibilityToggle = menu.findItem(R.id.visibility_toggle)
        pinItemsButton = menu.findItem(R.id.pin_items_button)
        settingsShortcut = menu.findItem(R.id.settings_shortcut)
    }

    override fun onMenuItemSelected(menuItem: MenuItem): Boolean = when (menuItem.itemId) {
        R.id.profile_toggle -> {
            workProfileViewModel.toggleWorkActivities()
            true
        }

        R.id.visibility_toggle -> {
            HiddenActivitiesDialogFragment().show(fragment.childFragmentManager, HiddenActivitiesDialogFragment.TAG)
            true
        }

        R.id.pin_items_button -> {
            PinItemsDialogFragment().showNow(
                fragment.childFragmentManager, PinItemsDialogFragment.TAG
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
