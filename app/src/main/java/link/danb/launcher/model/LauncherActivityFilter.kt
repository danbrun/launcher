package link.danb.launcher.model

import android.os.Process.myUserHandle
import androidx.annotation.StringRes
import link.danb.launcher.R

/** A filter that can restrict the launcher icons to be shown. */
data class LauncherActivityFilter(
    @StringRes val nameResId: Int,
    val function: (LauncherActivityData) -> Boolean
) {
    companion object {
        val ALL = LauncherActivityFilter(R.string.all) { true }
        val PERSONAL = LauncherActivityFilter(R.string.personal) { it.user == myUserHandle() }
        val WORK = LauncherActivityFilter(R.string.work) { it.user != myUserHandle() }
        val NONE = LauncherActivityFilter(R.string.none) { false }
    }
}
