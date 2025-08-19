package link.danb.launcher

import android.app.SearchManager
import android.content.Intent
import android.os.Bundle
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledIconToggleButton
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalFloatingToolbar
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.collections.immutable.ImmutableList
import link.danb.launcher.profiles.Profile
import link.danb.launcher.profiles.ProfileState

@Composable
fun LauncherBottomBar(
  launcherViewModel: LauncherViewModel = hiltViewModel(),
  onChangeProfile: (Profile, Boolean) -> Unit,
  onSearchGo: () -> Unit,
  showMoreActionsMenu: Boolean,
  onMoreActionsClick: () -> Unit,
  moreActionsMenu: @Composable () -> Unit,
) {
  Column(
    Modifier.consumeWindowInsets(WindowInsets.safeDrawing.only(WindowInsetsSides.Top))
      .safeDrawingPadding()
      .padding(8.dp)
  ) {
    val searchQuery by launcherViewModel.searchQuery.collectAsStateWithLifecycle()
    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
      @OptIn(ExperimentalMaterial3ExpressiveApi::class)
      HorizontalFloatingToolbar(
        expanded = true,
        floatingActionButton = {
          val context = LocalContext.current
          SearchFab {
            context.startActivity(
              Intent().apply {
                action = Intent.ACTION_WEB_SEARCH
                putExtra(SearchManager.EXTRA_NEW_SEARCH, true)
                // This extra is for Firefox to open a new tab.
                putExtra("open_to_search", "static_shortcut_new_tab")
              },
              Bundle(),
            )
          }
        },
      ) {
        val profile by launcherViewModel.profile.collectAsStateWithLifecycle()
        val profiles by launcherViewModel.profiles.collectAsStateWithLifecycle()
        Profiles(profile.takeIf { searchQuery == null }, profiles) { profile, enabled ->
          launcherViewModel.setSearchQuery(null)
          onChangeProfile(profile, enabled)
        }

        MoreActions(
          searchQuery,
          onSearchClick = { launcherViewModel.setSearchQuery("") },
          showMoreActionsMenu,
          onMoreActionsClick,
          moreActionsMenu,
        )
      }
    }

    AnimatedVisibility(visible = searchQuery != null) {
      SearchBar(
        onValueChange = { launcherViewModel.setSearchQuery(it) },
        onGo = onSearchGo,
        onCancel = { launcherViewModel.setSearchQuery(null) },
      )
    }
  }
}

@Composable
private fun Profiles(
  activeProfile: Profile?,
  availableProfiles: ImmutableList<ProfileState>,
  onChangeProfile: (Profile, Boolean) -> Unit,
) {
  FilledIconToggleButton(
    activeProfile == Profile.PERSONAL,
    { onChangeProfile(Profile.PERSONAL, true) },
  ) {
    Icon(painterResource(R.drawable.baseline_person_24), stringResource(R.string.show_personal))
  }

  for (profile in listOf(Profile.WORK, Profile.PRIVATE)) {
    val profileStatus = availableProfiles.singleOrNull { it.profile == profile }
    if (profileStatus != null) {
      FilledIconToggleButton(
        checked = activeProfile == profile,
        onCheckedChange = {
          onChangeProfile(profile, (activeProfile == profile) xor profileStatus.isEnabled)
        },
      ) {
        Icon(
          painterResource(
            when (profile) {
              Profile.PERSONAL -> throw IllegalStateException()
              Profile.WORK ->
                if (profileStatus.isEnabled) {
                  R.drawable.ic_baseline_work_24
                } else {
                  R.drawable.ic_baseline_work_off_24
                }
              Profile.PRIVATE ->
                if (profileStatus.isEnabled) {
                  R.drawable.baseline_lock_open_24
                } else {
                  R.drawable.baseline_lock_24
                }
            }
          ),
          stringResource(R.string.show_work),
        )
      }
    }
  }
}

@Composable
private fun SearchBar(onValueChange: (String) -> Unit, onGo: () -> Unit, onCancel: () -> Unit) {
  val focusRequester = remember { FocusRequester() }
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
private fun MoreActions(
  searchQuery: String?,
  onSearchClick: () -> Unit,
  showMoreActionsMenu: Boolean,
  onMoreActionsClick: () -> Unit,
  moreActionsMenu: @Composable () -> Unit,
) {
  FilledIconToggleButton(searchQuery != null, { onSearchClick() }) {
    Icon(painterResource(R.drawable.ic_baseline_search_24), stringResource(R.string.search))
  }

  Box {
    FilledIconToggleButton(
      showMoreActionsMenu,
      onCheckedChange = {
        if (it) {
          onMoreActionsClick()
        }
      },
    ) {
      Icon(
        painterResource(R.drawable.baseline_more_horiz_24),
        stringResource(R.string.more_actions),
      )
    }

    moreActionsMenu()
  }
}

@Composable
private fun SearchFab(onClick: () -> Unit) {
  FloatingActionButton(onClick = onClick, containerColor = MaterialTheme.colorScheme.primary) {
    Icon(
      painter = painterResource(R.drawable.travel_explore_24),
      contentDescription = stringResource(R.string.search),
    )
  }
}
