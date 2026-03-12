package link.danb.launcher.browser

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import link.danb.launcher.R

@Composable
fun BrowserScreen(viewModel: BrowserViewModel = viewModel()) {
  val viewState = viewModel.viewState.collectAsStateWithLifecycle().value

  Scaffold(
    modifier = Modifier.fillMaxSize(),
    bottomBar = {
      Column(Modifier.navigationBarsPadding()) {
        AnimatedVisibility(viewState.currentTab != null && viewState.currentTab.progress < 1f) {
          LinearProgressIndicator(
            progress = { viewState.currentTab?.progress ?: 0f },
            Modifier.fillMaxWidth().padding(top = 8.dp),
          )
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
          var url by
            remember(viewState.currentTab?.url) {
              mutableStateOf(TextFieldValue(viewState.currentTab?.url ?: ""))
            }
          var selectAll by remember { mutableStateOf(false) }
          TextField(
            url,
            onValueChange = {
              url =
                if (selectAll) {
                  selectAll = false
                  it.copy(selection = TextRange(0, it.text.length))
                } else it
            },
            Modifier.weight(1f).padding(top = 8.dp).onFocusChanged { focusState ->
              if (focusState.isFocused) {
                selectAll = true
              }
            },
            singleLine = true,
            keyboardOptions =
              KeyboardOptions(
                capitalization = KeyboardCapitalization.None,
                autoCorrectEnabled = false,
                keyboardType = KeyboardType.Uri,
                imeAction = ImeAction.Go,
              ),
            keyboardActions =
              KeyboardActions {
                viewModel.openUrl(url.text)
                defaultKeyboardAction(ImeAction.Next)
              },
            shape = MaterialTheme.shapes.extraLarge,
            colors =
              TextFieldDefaults.colors(
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent,
              ),
          )

          IconButton(onClick = { viewModel.newTab() }) {
            Icon(painterResource(R.drawable.outline_add_24), contentDescription = null)
          }

          Box {
            var showTabsList by remember { mutableStateOf(false) }
            IconButton(onClick = { showTabsList = true }) {
              Icon(painterResource(R.drawable.outline_more_vert_24), contentDescription = null)

              DropdownMenu(showTabsList, onDismissRequest = { showTabsList = false }) {
                for (tab in viewState.allTabs) {
                  DropdownMenuItem(
                    text = { Text(tab.title) },
                    onClick = {
                      viewModel.changeTab(tab.tabId)
                      showTabsList = false
                    },
                    leadingIcon = {
                      if (tab.bitmap != null) {
                        Image(
                          tab.bitmap.asImageBitmap(),
                          contentDescription = null,
                          Modifier.size(24.dp),
                        )
                      }
                    },
                    trailingIcon = {
                      IconButton(onClick = { viewModel.closeTab(tab) }) {
                        Icon(
                          painterResource(R.drawable.baseline_close_24),
                          contentDescription = null,
                        )
                      }
                    },
                  )
                }
              }
            }
          }
        }
      }
    },
  ) { innerPadding ->
    AndroidView(
      factory = { context -> BrowserView(context) },
      Modifier.padding(innerPadding).fillMaxSize().clip(MaterialTheme.shapes.extraLarge),
      onReset = { it.clearTab() },
      onRelease = { it.clearTab() },
      update = {
        if (viewState.currentTab != null) {
          it.setTab(viewState.currentTab)
        }
      },
    )
  }
}
