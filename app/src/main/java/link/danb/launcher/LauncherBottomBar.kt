package link.danb.launcher

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Card
import androidx.compose.material3.FilledIconToggleButton
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.painter.Painter
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
    FiltersTabGroup(bottomBarState.filters, onChangeFilter)

    SearchBar(visible = bottomBarState.isSearching, onSearchChange, onSearchGo)

    MoreActionsTabGroup(
      bottomBarState.workProfileToggle,
      bottomBarState.actions,
      onWorkProfileToggled,
      onMoreActionsClick,
    )

    SearchFab(visible = !bottomBarState.isSearching, onSearchFabClick)
  }
}

@Composable
private fun FiltersTabGroup(filters: List<BottomBarFilter>, onChangeFilter: (Filter) -> Unit) {
  ExpandingAnimatedVisibility(visible = filters.isNotEmpty()) {
    TabButtonGroup {
      for (filter in filters) {
        TabButton(
          painterResource(filter.icon),
          stringResource(filter.name),
          isChecked = filter.isChecked,
        ) {
          onChangeFilter(filter.filter)
        }
      }
    }
  }
}

@Composable
private fun SearchBar(visible: Boolean, onValueChange: (String) -> Unit, onGo: () -> Unit) {
  ExpandingAnimatedVisibility(visible) {
    val focusRequester = FocusRequester()
    var query: String by remember { mutableStateOf("") }

    TextField(
      value = query,
      onValueChange = {
        query = it
        onValueChange(query)
      },
      keyboardOptions = KeyboardOptions(imeAction = ImeAction.Go),
      keyboardActions = KeyboardActions(onGo = { onGo() }),
      modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp).focusRequester(focusRequester),
    )

    LaunchedEffect(Unit) { focusRequester.requestFocus() }
  }
}

@Composable
private fun MoreActionsTabGroup(
  workProfileToggle: Boolean?,
  actions: List<BottomBarAction>,
  onWorkProfileToggled: (Boolean) -> Unit,
  onMoreActionsClick: () -> Unit,
) {
  ExpandingAnimatedVisibility(visible = workProfileToggle != null || actions.isNotEmpty()) {
    TabButtonGroup {
      WorkProfileToggle(workProfileToggle, onWorkProfileToggled)

      MoreActionsTabButton(visible = actions.isNotEmpty(), onMoreActionsClick)
    }
  }
}

@Composable
private fun SearchFab(visible: Boolean, onClick: () -> Unit) {
  ExpandingAnimatedVisibility(visible) {
    FloatingActionButton(
      onClick = onClick,
      modifier = Modifier.padding(horizontal = 4.dp),
      containerColor = MaterialTheme.colorScheme.primary,
    ) {
      Icon(
        painter = painterResource(id = R.drawable.travel_explore_24),
        contentDescription = stringResource(id = R.string.search),
      )
    }
  }
}

@Composable
private fun WorkProfileToggle(
  workProfileToggle: Boolean?,
  onWorkProfileToggled: (Boolean) -> Unit,
) {
  ExpandingAnimatedVisibility(visible = workProfileToggle != null) {
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
private fun MoreActionsTabButton(visible: Boolean, onClick: () -> Unit) {
  ExpandingAnimatedVisibility(visible) {
    TabButton(
      icon = painterResource(id = R.drawable.baseline_more_horiz_24),
      name = stringResource(id = R.string.more_actions),
      isChecked = false,
      onClick = onClick,
    )
  }
}

@Composable
private fun ExpandingAnimatedVisibility(visible: Boolean, content: @Composable () -> Unit) {
  AnimatedVisibility(
    visible = visible,
    enter = fadeIn() + expandHorizontally(),
    exit = fadeOut() + shrinkHorizontally(),
  ) {
    content()
  }
}

@Composable
fun TabButtonGroup(iconButtons: @Composable () -> Unit) {
  Card(Modifier.padding(horizontal = 4.dp), RoundedCornerShape(28.dp)) {
    Row(modifier = Modifier.padding(4.dp)) { iconButtons() }
  }
}

@Composable
fun TabButton(icon: Painter, name: String, isChecked: Boolean, onClick: () -> Unit) {
  FilledIconToggleButton(checked = isChecked, onCheckedChange = { _ -> onClick() }) {
    Icon(painter = icon, contentDescription = name)
  }
}
