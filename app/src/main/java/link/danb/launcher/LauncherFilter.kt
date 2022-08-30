package link.danb.launcher

import android.os.Process.myUserHandle
import androidx.annotation.StringRes

/** A filter that can restrict the launcher icons to be shown. */
data class LauncherFilter(
    @StringRes val nameResId: Int,
    val function: (LauncherActivity) -> Boolean
) {
    companion object {
        val ALL = LauncherFilter(R.string.all) { true }
        val PERSONAL = LauncherFilter(R.string.personal) { it.user == myUserHandle() }
        val WORK = LauncherFilter(R.string.work) { it.user != myUserHandle() }
        val NONE = LauncherFilter(R.string.none) { false }
    }
}
