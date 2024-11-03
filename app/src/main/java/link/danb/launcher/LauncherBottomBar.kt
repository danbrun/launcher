package link.danb.launcher

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.FilledIconToggleButton
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import link.danb.launcher.profiles.Profile
import link.danb.launcher.profiles.ProfileState
import link.danb.launcher.ui.FilledIconSelector
import link.danb.launcher.ui.IconButtonGroup

@Composable
fun LauncherBottomBar(
  profile: Profile,
  profiles: Map<Profile, ProfileState>,
  bottomBarActions: List<BottomBarAction>,
  onChangeProfile: (Profile, Boolean) -> Unit,
  searchQuery: String?,
  onSearchChange: (String) -> Unit,
  onSearchGo: () -> Unit,
  onSearchCancel: () -> Unit,
  onMoreActionsClick: () -> Unit,
  onSearchFabClick: () -> Unit,
) {
  Column(
    Modifier.consumeWindowInsets(WindowInsets.safeDrawing.only(WindowInsetsSides.Top))
      .safeDrawingPadding()
      .padding(8.dp)
  ) {
    AnimatedVisibility(visible = searchQuery == null) {
      Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
        ProfilesTabGroup(profile, profiles, onChangeProfile)

        MoreActionsTabGroup({ onSearchChange("") }, bottomBarActions, onMoreActionsClick)

        SearchFab(onSearchFabClick)
      }
    }

    AnimatedVisibility(visible = searchQuery != null) {
      SearchBar(onSearchChange, onSearchGo, onSearchCancel)
    }
  }
}

@Composable
private fun ProfilesTabGroup(
  activeProfile: Profile,
  availableProfiles: Map<Profile, ProfileState>,
  onChangeProfile: (Profile, Boolean) -> Unit,
) {
  ExpandingAnimatedVisibility(visible = availableProfiles.size > 1) {
    IconButtonGroup {
      FilledIconToggleButton(
        activeProfile == Profile.PERSONAL,
        { onChangeProfile(Profile.PERSONAL, true) },
      ) {
        Icon(painterResource(R.drawable.baseline_person_24), stringResource(R.string.show_personal))
      }

      val workProfileStatus = availableProfiles[Profile.WORK]
      if (workProfileStatus != null) {
        FilledIconSelector(
          items =
            if (workProfileStatus.canToggle) {
              listOf(false, true)
            } else {
              listOf(workProfileStatus.isEnabled)
            },
          selected = workProfileStatus.isEnabled,
          isChecked = activeProfile == Profile.WORK,
          onClick = { onChangeProfile(Profile.WORK, it) },
        ) {
          when (it) {
            false ->
              Icon(
                painterResource(R.drawable.ic_baseline_work_off_24),
                stringResource(R.string.show_work),
              )
            true ->
              Icon(
                painterResource(R.drawable.ic_baseline_work_24),
                stringResource(R.string.show_work),
              )
          }
        }
      }
    }
  }
}

@Composable
private fun SearchBar(onValueChange: (String) -> Unit, onGo: () -> Unit, onCancel: () -> Unit) {
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
    trailingIcon = {
      IconButton(onCancel) {
        Icon(painterResource(R.drawable.baseline_close_24), stringResource(R.string.cancel))
      }
    },
  )

  LaunchedEffect(Unit) { focusRequester.requestFocus() }
}

@Composable
private fun MoreActionsTabGroup(
  onSearchClick: () -> Unit,
  actions: List<BottomBarAction>,
  onMoreActionsClick: () -> Unit,
) {
  IconButtonGroup {
    IconButton(onSearchClick) {
      Icon(painterResource(R.drawable.ic_baseline_search_24), stringResource(R.string.search))
    }

    ExpandingAnimatedVisibility(visible = actions.isNotEmpty()) {
      ExpandingAnimatedVisibility(actions.isNotEmpty()) {
        IconButton(onMoreActionsClick) {
          Icon(
            painterResource(R.drawable.baseline_more_horiz_24),
            stringResource(R.string.more_actions),
          )
        }
      }
    }
  }
}

@Composable
private fun SearchFab(onClick: () -> Unit) {
  FloatingActionButton(
    onClick = onClick,
    modifier = Modifier.padding(horizontal = 4.dp),
    containerColor = MaterialTheme.colorScheme.primary,
  ) {
    Icon(
      painter = painterResource(R.drawable.travel_explore_24),
      contentDescription = stringResource(R.string.search),
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
