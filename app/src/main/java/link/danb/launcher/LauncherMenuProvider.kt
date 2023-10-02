package link.danb.launcher

import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
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
import link.danb.launcher.profiles.EnableWorkProfileDialogBuilder
import link.danb.launcher.profiles.ProfilesModel
import link.danb.launcher.shortcuts.PinShortcutsDialogFragment
import link.danb.launcher.widgets.PinWidgetsDialogFragment

class LauncherMenuProvider
@Inject
constructor(
  private val fragment: Fragment,
  private val profilesModel: ProfilesModel,
  private val enableWorkProfileDialogBuilder: EnableWorkProfileDialogBuilder,
) : DefaultLifecycleObserver, MenuProvider {

  private val activitiesViewModel: ActivitiesViewModel by fragment.activityViewModels()

  private lateinit var profileToggle: MenuItem
  private lateinit var visibilityToggle: MenuItem

  init {
    fragment.lifecycle.addObserver(this)
  }

  override fun onStart(owner: LifecycleOwner) {
    fragment.viewLifecycleOwner.lifecycleScope.launch {
      fragment.viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
        launch {
          combine(activitiesViewModel.activities, profilesModel.activeProfile) {
              activities,
              activeProfile ->
              Pair(
                activities.any { !it.userHandle.isPersonalProfile },
                activities.any { it.isHidden && it.userHandle == activeProfile }
              )
            }
            .collect { (hasWorkProfileApps, hasHiddenApps) ->
              profileToggle.isVisible = hasWorkProfileApps
              visibilityToggle.isVisible = hasHiddenApps
            }
        }

        launch {
          combine(
              profilesModel.workProfileData,
              profilesModel.activeProfile,
            ) { workProfileData, activeProfile ->
              WorkProfileToggleData(
                workProfileData.user != null,
                if (activeProfile.isPersonalProfile) {
                  if (workProfileData.isEnabled) {
                    R.drawable.ic_baseline_work_24
                  } else {
                    R.drawable.ic_baseline_work_off_24
                  }
                } else {
                  R.drawable.baseline_person_24
                },
                if (activeProfile.isPersonalProfile) {
                  R.string.show_work
                } else {
                  R.string.show_personal
                },
              )
            }
            .collect { data ->
              profileToggle.isVisible = data.show
              profileToggle.setTitle(data.title)
              profileToggle.setIcon(data.icon)
            }
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
        if (
          !profilesModel.activeProfile.value.isPersonalProfile ||
            profilesModel.workProfileData.value.isEnabled
        ) {
          profilesModel.toggleActiveProfile()
        } else {
          enableWorkProfileDialogBuilder
            .getEnableWorkProfileDialogBuilder(fragment.requireContext())
            .show()
        }
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
      else -> false
    }

  data class WorkProfileToggleData(
    val show: Boolean,
    @DrawableRes val icon: Int,
    @StringRes val title: Int,
  )
}
