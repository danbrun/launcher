package link.danb.launcher.gestures

import android.app.Application
import android.content.Intent
import androidx.lifecycle.AndroidViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import link.danb.launcher.extensions.gestureContract

@HiltViewModel
class GestureContractModel @Inject constructor(application: Application) :
  AndroidViewModel(application) {

  private lateinit var onNewGestureContract: (GestureContract) -> Unit

  val gestureContract: Flow<GestureContract> = callbackFlow {
    onNewGestureContract = { trySend(it) }
    awaitClose()
  }

  fun onNewIntent(intent: Intent) {
    intent.gestureContract?.let { onNewGestureContract(it) }
  }
}
