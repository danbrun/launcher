package link.danb.launcher.extensions

import android.content.Intent
import android.os.Build
import androidx.annotation.RequiresApi
import link.danb.launcher.gestures.GestureContract

@get:RequiresApi(Build.VERSION_CODES.Q)
val Intent.gestureContract: GestureContract?
  get() = GestureContract.fromIntent(this)
