package link.danb.launcher

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp

@Composable
fun LauncherBottomBar(
  bottomBarState: BottomBarState,
  onChangeFilter: (Filter) -> Unit,
  onSearchChange: (String) -> Unit,
  onSearchGo: () -> Unit,
  onWorkProfileToggled: (Boolean) -> Unit,
  onMoreActionsClick: () -> Unit,
  onSearchFabClick: () -> Unit,
) {
  Row(
    modifier =
      Modifier.fillMaxWidth()
        .consumeWindowInsets(WindowInsets.safeDrawing.only(WindowInsetsSides.Top))
        .safeDrawingPadding()
        .padding(8.dp),
    horizontalArrangement = Arrangement.Center,
  ) {
    TabButtonGroup {
      for (filter in bottomBarState.filters) {
        TabButton(
          painterResource(filter.icon),
          stringResource(filter.name),
          isChecked = filter.isChecked,
        ) {
          onChangeFilter(filter.filter)
        }
      }
    }

    SearchBar(bottomBarState.searchQuery, onSearchChange, onSearchGo)

    Spacer(modifier = Modifier.width(8.dp))

    BottomBarAnimatedVisibility(
      visible = bottomBarState.workProfileToggle != null || bottomBarState.actions.isNotEmpty()
    ) {
      TabButtonGroup {
        WorkProfileToggle(bottomBarState.workProfileToggle, onWorkProfileToggled)

        BottomBarAnimatedVisibility(visible = bottomBarState.actions.isNotEmpty()) {
          MoreActionsTabButton(onMoreActionsClick)
        }
      }
    }

    Spacer(modifier = Modifier.width(8.dp))

    SearchFab(onSearchFabClick)
  }
}

@Composable
private fun SearchBar(searchQuery: String?, onValueChange: (String) -> Unit, onGo: () -> Unit) {
  BottomBarAnimatedVisibility(visible = searchQuery != null) {
    val focusRequester = FocusRequester()

    TextField(
      value = searchQuery ?: "",
      onValueChange = onValueChange,
      keyboardOptions = KeyboardOptions(imeAction = ImeAction.Go),
      keyboardActions = KeyboardActions(onGo = { onGo() }),
      modifier = Modifier.fillMaxWidth().padding(start = 8.dp).focusRequester(focusRequester),
    )

    LaunchedEffect(Unit) { focusRequester.requestFocus() }
  }
}

@Composable
private fun WorkProfileToggle(
  workProfileToggle: Boolean?,
  onWorkProfileToggled: (Boolean) -> Unit,
) {
  BottomBarAnimatedVisibility(visible = workProfileToggle != null) {
    Switch(
      checked = workProfileToggle == true,
      onCheckedChange = onWorkProfileToggled,
      thumbContent = {
        Icon(
          painter =
            painterResource(
              if (workProfileToggle == true) {
                R.drawable.ic_baseline_work_24
              } else {
                R.drawable.ic_baseline_work_off_24
              }
            ),
          contentDescription = null,
          modifier = Modifier.size(SwitchDefaults.IconSize),
        )
      },
      modifier = Modifier.padding(horizontal = 8.dp),
    )
  }
}

@Composable
private fun MoreActionsTabButton(onClick: () -> Unit) {
  TabButton(
    icon = painterResource(id = R.drawable.baseline_more_horiz_24),
    name = stringResource(id = R.string.more_actions),
    isChecked = false,
    onClick = onClick,
  )
}

@Composable
private fun SearchFab(onClick: () -> Unit) {
  FloatingActionButton(onClick = onClick, containerColor = MaterialTheme.colorScheme.primary) {
    Icon(
      painter = painterResource(id = R.drawable.travel_explore_24),
      contentDescription = stringResource(id = R.string.search),
    )
  }
}

@Composable
private fun BottomBarAnimatedVisibility(visible: Boolean, content: @Composable () -> Unit) {
  AnimatedVisibility(
    visible = visible,
    enter = fadeIn() + expandHorizontally(),
    exit = fadeOut() + shrinkHorizontally(),
  ) {
    content()
  }
}
