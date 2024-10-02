package link.danb.launcher.ui

import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.graphics.Color
import kotlinx.coroutines.launch

@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun BottomSheet(
  isShowing: Boolean,
  onDismissRequest: () -> Unit,
  content: @Composable ColumnScope.(dismiss: () -> Unit) -> Unit,
) {
  val sheetState = rememberModalBottomSheetState()
  val coroutineScope = rememberCoroutineScope()
  val dismiss: () -> Unit = remember {
    { coroutineScope.launch { sheetState.hide() }.invokeOnCompletion { onDismissRequest() } }
  }

  if (isShowing) {
    ModalBottomSheet(
      onDismissRequest = onDismissRequest,
      sheetState = sheetState,
      scrimColor = Color.Transparent,
    ) {
      content(dismiss)
    }
  }
}
