package link.danb.launcher.extensions

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerEventTimeoutCancellationException
import androidx.compose.ui.input.pointer.PointerInputScope
import androidx.compose.ui.util.fastAny
import androidx.compose.ui.util.fastForEach

/** Detects and consumes long press events. */
suspend fun PointerInputScope.detectLongPress(pass: PointerEventPass, onLongPress: () -> Unit) {
  awaitEachGesture {
    awaitFirstDown(pass = pass)
    try {
      withTimeout(viewConfiguration.longPressTimeoutMillis) { waitForUpOrCancellation(pass) }
    } catch (_: PointerEventTimeoutCancellationException) {
      onLongPress()

      do {
        val event = awaitPointerEvent(pass)
        event.changes.fastForEach { it.consume() }
      } while (event.changes.fastAny { it.pressed })
    }
  }
}
