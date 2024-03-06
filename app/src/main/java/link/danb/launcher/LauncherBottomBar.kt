package link.danb.launcher

import android.app.SearchManager
import android.content.Intent
import android.os.Bundle
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsBottomHeight
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Card
import androidx.compose.material3.FilledIconToggleButton
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.fragment.app.FragmentManager
import link.danb.launcher.extensions.isPersonalProfile
import link.danb.launcher.profiles.ProfilesModel
import link.danb.launcher.profiles.WorkProfileInstalled
import link.danb.launcher.profiles.WorkProfileManager
import link.danb.launcher.profiles.WorkProfileNotInstalled

@Composable
fun SearchBar(launcherViewModel: LauncherViewModel, onEnter: () -> Unit) {
  val searchQuery by launcherViewModel.searchQuery.collectAsState()

  if (searchQuery != null) {
    val focusRequester = FocusRequester()

    Row(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
      TextField(
        value = searchQuery ?: "",
        onValueChange = { launcherViewModel.searchQuery.value = it },
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Go),
        keyboardActions = KeyboardActions(onGo = { onEnter() }),
        modifier = Modifier.fillMaxWidth().focusRequester(focusRequester),
      )
    }

    LaunchedEffect(Unit) { focusRequester.requestFocus() }
  }
}

@Composable
fun ProfileTabs(
  profilesModel: ProfilesModel,
  workProfileManager: WorkProfileManager,
  launcherViewModel: LauncherViewModel,
  fragmentManager: FragmentManager,
) {
  val hasWorkProfile by workProfileManager.status.collectAsState(initial = WorkProfileNotInstalled)

  TabButtonGroup {
    ShowPersonalTabButton(profilesModel, launcherViewModel)

    if (hasWorkProfile is WorkProfileInstalled) {
      ShowWorkTabButton(profilesModel, launcherViewModel)
    }

    ShowSearchTabButton(launcherViewModel = launcherViewModel)

    MoreActionsTabButton(fragmentManager)
  }
}

@Composable
fun ShowPersonalTabButton(profilesModel: ProfilesModel, launcherViewModel: LauncherViewModel) {
  val activeProfile by profilesModel.activeProfile.collectAsState()
  val searchQuery by launcherViewModel.searchQuery.collectAsState()

  TabButton(
    icon = painterResource(id = R.drawable.baseline_person_24),
    name = stringResource(id = R.string.show_personal),
    isChecked = activeProfile.isPersonalProfile && searchQuery == null,
  ) {
    profilesModel.toggleActiveProfile(showWorkProfile = false)
    launcherViewModel.searchQuery.value = null
  }
}

@Composable
fun ShowWorkTabButton(profilesModel: ProfilesModel, launcherViewModel: LauncherViewModel) {
  val activeProfile by profilesModel.activeProfile.collectAsState()
  val searchQuery by launcherViewModel.searchQuery.collectAsState()

  TabButton(
    icon = painterResource(id = R.drawable.ic_baseline_work_24),
    name = stringResource(id = R.string.show_work),
    isChecked = !activeProfile.isPersonalProfile && searchQuery == null,
  ) {
    profilesModel.toggleActiveProfile(showWorkProfile = true)
    launcherViewModel.searchQuery.value = null
  }
}

@Composable
fun ShowSearchTabButton(launcherViewModel: LauncherViewModel) {
  val searchQuery by launcherViewModel.searchQuery.collectAsState()

  TabButton(
    icon = painterResource(id = R.drawable.ic_baseline_search_24),
    name = stringResource(id = R.string.search),
    isChecked = searchQuery != null,
  ) {
    launcherViewModel.searchQuery.value = ""
  }
}

@Composable
fun MoreActionsTabButton(fragmentManager: FragmentManager) {
  TabButton(
    icon = painterResource(id = R.drawable.baseline_more_horiz_24),
    name = stringResource(id = R.string.add_item),
    isChecked = false,
  ) {
    MoreActionsDialogFragment().showNow(fragmentManager, MoreActionsDialogFragment.TAG)
  }
}

@Composable
fun SearchFab() {
  val context = LocalContext.current

  FloatingActionButton(
    onClick = {
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
  ) {
    Icon(
      painter = painterResource(id = R.drawable.travel_explore_24),
      contentDescription = stringResource(id = R.string.search),
    )
  }
}

@Composable
fun BottomBar(
  searchBar: @Composable () -> Unit,
  tabButtonGroups: @Composable () -> Unit,
  floatingActionButton: @Composable () -> Unit,
) {
  Column {
    searchBar()

    Row(modifier = Modifier.align(Alignment.CenterHorizontally)) {
      tabButtonGroups()

      Spacer(modifier = Modifier.width(8.dp))

      floatingActionButton()
    }

    Spacer(modifier = Modifier.windowInsetsBottomHeight(WindowInsets.ime))
    Spacer(modifier = Modifier.windowInsetsBottomHeight(WindowInsets.systemBars))
  }
}

@Composable
fun TabButtonGroup(iconButtons: @Composable () -> Unit) {
  Card(shape = RoundedCornerShape(28.dp)) {
    Row(modifier = Modifier.padding(4.dp)) { iconButtons() }
  }
}

@Composable
fun TabButton(icon: Painter, name: String, isChecked: Boolean, onClick: () -> Unit) {
  FilledIconToggleButton(checked = isChecked, onCheckedChange = { _ -> onClick() }) {
    Icon(painter = icon, contentDescription = name)
  }
}
