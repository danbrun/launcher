package link.danb.launcher.utils

import android.content.Context
import android.content.pm.LauncherApps

/** Convenience method for getting LauncherApps from a context. */
fun Context.getLauncherApps(): LauncherApps {
    return getSystemService(Context.LAUNCHER_APPS_SERVICE) as LauncherApps
}
