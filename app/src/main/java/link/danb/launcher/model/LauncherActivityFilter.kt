package link.danb.launcher.model

import android.os.Process.myUserHandle
import androidx.annotation.StringRes
import link.danb.launcher.R

/** A filter that can restrict the launcher icons to be shown. */
data class LauncherActivityFilter(
    @StringRes val nameResId: Int,
    val function: (LauncherViewModel, LauncherActivityData) -> Boolean
) {
    companion object {
        val ALL = LauncherActivityFilter(R.string.all) { _, _ -> true }

        val PERSONAL = LauncherActivityFilter(R.string.personal) { model, data ->
            data.user == myUserHandle() && model.isVisible(data)
        }

        val WORK = LauncherActivityFilter(R.string.work) { model, data ->
            data.user != myUserHandle() && model.isVisible(data)
        }

        val HIDDEN =
            LauncherActivityFilter(R.string.hidden) { model, data -> !model.isVisible(data) }

        val FILTERS = listOf(ALL, PERSONAL, WORK, HIDDEN)
    }
}
