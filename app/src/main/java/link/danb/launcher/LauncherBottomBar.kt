package link.danb.launcher

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import link.danb.launcher.profiles.Profile
import link.danb.launcher.profiles.ProfileState
import link.danb.launcher.ui.TabButton
import link.danb.launcher.ui.TabButtonGroup

@Composable
fun LauncherBottomBar(
  profile: Profile,
  profiles: Map<Profile, ProfileState>,
  bottomBarActions: List<BottomBarAction>,
  onChangeProfile: (Profile, ProfileState) -> Unit,
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
  onChangeProfile: (Profile, ProfileState) -> Unit,
) {
  ExpandingAnimatedVisibility(visible = availableProfiles.size > 1) {
    TabButtonGroup {
      TabButton(
        painterResource(R.drawable.baseline_person_24),
        stringResource(R.string.show_personal),
        isChecked = activeProfile == Profile.PERSONAL,
      ) {
        onChangeProfile(Profile.PERSONAL, ProfileState.ENABLED)
      }

      ProfileToggleButton(
        activeProfile,
        Profile.WORK,
        availableProfiles.getValue(Profile.WORK),
        { onChangeProfile(Profile.WORK, it) },
      ) {
        when (it) {
          ProfileState.DISABLED ->
            Icon(
              painterResource(R.drawable.ic_baseline_work_off_24),
              stringResource(R.string.show_work),
            )
          ProfileState.ENABLED ->
            Icon(
              painterResource(R.drawable.ic_baseline_work_24),
              stringResource(R.string.show_work),
            )
        }
      }
    }
  }
}

@Composable
private fun ProfileToggleButton(
  activeProfile: Profile,
  targetProfile: Profile,
  currentState: ProfileState,
  onChangeState: (ProfileState) -> Unit,
  icon: @Composable (ProfileState) -> Unit,
) {
  val isActive = activeProfile == targetProfile

  val groupBackground by
    animateColorAsState(
      if (isActive) MaterialTheme.colorScheme.surfaceContainerLow else Color.Transparent,
      label = "group_background",
    )
  Row(Modifier.padding(4.dp).clip(RoundedCornerShape(20.dp)).background(groupBackground)) {
    for (profileState in listOf(ProfileState.DISABLED, ProfileState.ENABLED)) {
      val isCurrentState = currentState == profileState
      ExpandingAnimatedVisibility(isActive || isCurrentState) {
        val iconBackground by
          animateColorAsState(
            if (isActive && isCurrentState)
              IconButtonDefaults.filledIconToggleButtonColors().checkedContainerColor
            else Color.Transparent,
            label = "icon_background",
          )
        Box(
          Modifier.size(40.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(iconBackground)
            .clickable { onChangeState(profileState) },
          contentAlignment = Alignment.Center,
        ) {
          val iconForeground by
            animateColorAsState(
              if (isActive && isCurrentState)
                IconButtonDefaults.filledIconToggleButtonColors().checkedContentColor
              else IconButtonDefaults.filledIconToggleButtonColors().contentColor,
              label = "icon_foreground",
            )
          CompositionLocalProvider(LocalContentColor provides iconForeground) { icon(profileState) }
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
  TabButtonGroup {
    TabButton(
      painterResource(R.drawable.ic_baseline_search_24),
      stringResource(R.string.search),
      isChecked = false,
      onSearchClick,
    )

    ExpandingAnimatedVisibility(visible = actions.isNotEmpty()) {
      MoreActionsTabButton(visible = actions.isNotEmpty(), onMoreActionsClick)
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
      painter = painterResource(id = R.drawable.travel_explore_24),
      contentDescription = stringResource(id = R.string.search),
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
