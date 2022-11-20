package link.danb.launcher

import android.app.Application
import com.google.android.material.color.DynamicColors

class LauncherApplication : Application() {
    override fun onCreate() {
        super.onCreate()

        DynamicColors.applyToActivitiesIfAvailable(this)
    }
}
