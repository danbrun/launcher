package link.danb.launcher.extensions

import android.content.Intent
import link.danb.launcher.gestures.GestureContract

val Intent.gestureContract: GestureContract?
  get() = GestureContract.fromIntent(this)
