package link.danb.launcher.profiles

import android.content.Context
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import javax.inject.Inject
import link.danb.launcher.R

class ToggleWorkProfileDialogBuilder @Inject constructor(private val profilesModel: ProfilesModel) {

  fun getToggleWorkProfileDialogBuilder(context: Context): MaterialAlertDialogBuilder =
    MaterialAlertDialogBuilder(context)
      .setTitle(
        if (profilesModel.workProfileData.value.isEnabled) {
          R.string.turn_off_work_profile_title
        } else {
          R.string.turn_on_work_profile_title
        }
      )
      .setPositiveButton(
        if (profilesModel.workProfileData.value.isEnabled) {
          R.string.turn_off
        } else {
          R.string.turn_on
        }
      ) { _, _ ->
        profilesModel.setWorkProfileEnabled(!profilesModel.workProfileData.value.isEnabled)
      }
      .setNegativeButton(android.R.string.cancel, null)
}
