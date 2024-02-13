package link.danb.launcher

import android.content.Intent
import android.provider.Settings
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import androidx.core.view.MenuProvider
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import javax.inject.Inject
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import link.danb.launcher.activities.ActivitiesViewModel
import link.danb.launcher.activities.HiddenActivitiesDialogFragment
import link.danb.launcher.extensions.isPersonalProfile
import link.danb.launcher.extensions.makeScaleUpAnimation
import link.danb.launcher.profiles.ProfilesModel
import link.danb.launcher.profiles.ToggleWorkProfileDialogBuilder
import link.danb.launcher.shortcuts.PinShortcutsDialogFragment
import link.danb.launcher.widgets.PinWidgetsDialogFragment

class LauncherMenuProvider
@Inject
constructor(
  private val fragment: Fragment,
  private val profilesModel: ProfilesModel,
  private val toggleWorkProfileDialogBuilder: ToggleWorkProfileDialogBuilder,
) : DefaultLifecycleObserver, MenuProvider {

  private val activitiesViewModel: ActivitiesViewModel by fragment.activityViewModels()

  private lateinit var profileToggle: MenuItem
  private lateinit var workToggle: MenuItem
  private lateinit var visibilityToggle: MenuItem

  init {
    fragment.lifecycle.addObserver(this)
  }

  override fun onStart(owner: LifecycleOwner) {
    fragment.viewLifecycleOwner.lifecycleScope.launch {
      fragment.viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
        combine(
            activitiesViewModel.activities,
            profilesModel.workProfileData,
            profilesModel.activeProfile,
            ::Triple,
          )
          .collect { (activities, workProfileData, activeProfile) ->
            profileToggle.isVisible = workProfileData.user != null
            profileToggle.setTitle(
              if (activeProfile.isPersonalProfile) {
                R.string.show_work
              } else {
                R.string.show_personal
              }
            )
            profileToggle.setIcon(
              if (activeProfile.isPersonalProfile) {
                R.drawable.baseline_person_24
              } else {
                R.drawable.ic_baseline_work_account_24
              }
            )

            workToggle.isVisible = workProfileData.user != null
            workToggle.setTitle(
              if (workProfileData.isEnabled) {
                R.string.turn_off_work_profile
              } else {
                R.string.turn_on_work_profile
              }
            )
            workToggle.setIcon(
              if (workProfileData.isEnabled) {
                R.drawable.ic_baseline_work_24
              } else {
                R.drawable.ic_baseline_work_off_24
              }
            )

            visibilityToggle.isVisible =
              activities.any { it.isHidden && it.userActivity.userHandle == activeProfile }
          }
      }
    }
  }

  override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
    menuInflater.inflate(R.menu.launcher_menu, menu)

    profileToggle = menu.findItem(R.id.profile_toggle)
    workToggle = menu.findItem(R.id.work_toggle)
    visibilityToggle = menu.findItem(R.id.visibility_toggle)
  }

  override fun onMenuItemSelected(menuItem: MenuItem): Boolean =
    when (menuItem.itemId) {
      R.id.profile_toggle -> {
        profilesModel.toggleActiveProfile()
        true
      }
      R.id.work_toggle -> {
        toggleWorkProfileDialogBuilder
          .getToggleWorkProfileDialogBuilder(fragment.requireContext())
          .show()
        true
      }
      R.id.visibility_toggle -> {
        HiddenActivitiesDialogFragment()
          .show(fragment.childFragmentManager, HiddenActivitiesDialogFragment.TAG)
        true
      }
      R.id.pin_shortcut_button -> {
        PinShortcutsDialogFragment()
          .showNow(fragment.childFragmentManager, PinShortcutsDialogFragment.TAG)
        true
      }
      R.id.pin_widget_button -> {
        PinWidgetsDialogFragment()
          .showNow(fragment.childFragmentManager, PinWidgetsDialogFragment.TAG)
        true
      }
      R.id.settings_button -> {
        fragment.startActivity(
          Intent(Settings.ACTION_SETTINGS).setFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
          fragment.view
            ?.findViewById<View>(R.id.settings_button)
            ?.makeScaleUpAnimation()
            ?.toBundle(),
        )
        true
      }
      else -> false
    }
}
