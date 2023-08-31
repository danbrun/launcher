package link.danb.launcher

import android.app.Application
import com.google.android.material.color.DynamicColors
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class LauncherApplication : Application() {
  override fun onCreate() {
    super.onCreate()

    DynamicColors.applyToActivitiesIfAvailable(this)
  }
}
