package link.danb.launcher.extensions

import android.app.ActivityOptions
import android.os.Build

fun ActivityOptions.allowPendingIntentBackgroundActivityStart() = apply {
  if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
    pendingIntentBackgroundActivityStartMode =
      ActivityOptions.MODE_BACKGROUND_ACTIVITY_START_ALLOWED
  }
}
