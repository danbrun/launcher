package link.danb.launcher

import android.content.Intent
import android.provider.Settings
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import androidx.core.view.MenuProvider
import androidx.fragment.app.Fragment
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import javax.inject.Inject
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import link.danb.launcher.activities.ActivityManager
import link.danb.launcher.activities.HiddenActivitiesDialogFragment
import link.danb.launcher.extensions.isPersonalProfile
import link.danb.launcher.extensions.makeScaleUpAnimation
import link.danb.launcher.profiles.ProfilesModel
import link.danb.launcher.profiles.WorkProfileInstalled
import link.danb.launcher.profiles.WorkProfileManager
import link.danb.launcher.shortcuts.PinShortcutsDialogFragment
import link.danb.launcher.widgets.PinWidgetsDialogFragment

class LauncherMenuProvider
@Inject
constructor(
  private val fragment: Fragment,
  private val activityManager: ActivityManager,
  private val profilesModel: ProfilesModel,
  private val workProfileManager: WorkProfileManager,
) : DefaultLifecycleObserver, MenuProvider {

  private lateinit var profileToggle: MenuItem
  private lateinit var visibilityToggle: MenuItem

  init {
    fragment.lifecycle.addObserver(this)
  }

  override fun onStart(owner: LifecycleOwner) {
    fragment.viewLifecycleOwner.lifecycleScope.launch {
      fragment.viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
        combine(
            activityManager.data,
            workProfileManager.status,
            profilesModel.activeProfile,
            ::Triple,
          )
          .collect { (activities, workProfileStatus, activeProfile) ->
            profileToggle.isVisible = workProfileStatus is WorkProfileInstalled
            profileToggle.setTitle(
              if (activeProfile.isPersonalProfile) {
                R.string.show_work
              } else {
                R.string.show_personal
              }
            )
            profileToggle.setIcon(
              if (activeProfile.isPersonalProfile) {
                R.drawable.ic_baseline_work_24
              } else {
                R.drawable.baseline_person_24
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
    visibilityToggle = menu.findItem(R.id.visibility_toggle)
  }

  override fun onMenuItemSelected(menuItem: MenuItem): Boolean =
    when (menuItem.itemId) {
      R.id.profile_toggle -> {
        profilesModel.toggleActiveProfile()
        true
      }
      R.id.visibility_toggle -> {
        HiddenActivitiesDialogFragment.newInstance(profilesModel.activeProfile.value)
          .showNow(fragment.childFragmentManager, HiddenActivitiesDialogFragment.TAG)
        true
      }
      R.id.pin_shortcut_button -> {
        PinShortcutsDialogFragment.newInstance(profilesModel.activeProfile.value)
          .showNow(fragment.childFragmentManager, PinShortcutsDialogFragment.TAG)
        true
      }
      R.id.pin_widget_button -> {
        PinWidgetsDialogFragment.newInstance(profilesModel.activeProfile.value)
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
