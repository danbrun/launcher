package link.danb.launcher.ui

import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.only
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch

@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun BottomSheet(
  isShowing: Boolean,
  onHidden: () -> Unit,
  content: @Composable ColumnScope.(hide: () -> Unit) -> Unit,
) {
  val sheetState = rememberModalBottomSheetState()
  val coroutineScope = rememberCoroutineScope()
  val hide = remember {
    { coroutineScope.launch { sheetState.hide() }.invokeOnCompletion { onHidden() } }
  }

  if (isShowing) {
    ModalBottomSheet(
      onDismissRequest = onHidden,
      sheetState = sheetState,
      windowInsets = BottomSheetDefaults.windowInsets.only(WindowInsetsSides.Top),
    ) {
      content(hide)
    }
  }
}
