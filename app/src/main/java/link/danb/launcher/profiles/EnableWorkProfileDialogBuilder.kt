package link.danb.launcher.profiles

import android.content.Context
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import link.danb.launcher.R
import javax.inject.Inject

class EnableWorkProfileDialogBuilder @Inject constructor(private val profilesModel: ProfilesModel) {

    fun getEnableWorkProfileDialogBuilder(context: Context): MaterialAlertDialogBuilder =
        MaterialAlertDialogBuilder(context).setTitle(R.string.turn_on_work_profile)
            .setPositiveButton(R.string.turn_on) { _, _ ->
                profilesModel.setWorkProfileEnabled(true)
            }.setNegativeButton(android.R.string.cancel, null)
}
