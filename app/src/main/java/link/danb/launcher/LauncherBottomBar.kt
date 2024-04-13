package link.danb.launcher

import android.os.Process
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
import androidx.compose.material3.Icon
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import link.danb.launcher.extensions.isPersonalProfile
import link.danb.launcher.profiles.WorkProfileInstalled
import link.danb.launcher.profiles.WorkProfileManager
import link.danb.launcher.profiles.WorkProfileStatus

@Composable
fun LauncherBottomBar(
  filter: Filter,
  workProfileStatus: WorkProfileStatus,
  workProfileManager: WorkProfileManager,
  onChangeFilter: (Filter) -> Unit,
  onSearchChange: (String) -> Unit,
  onSearchGo: () -> Unit,
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
    FilterSelectionTabGroup(filter, workProfileStatus, onChangeFilter)

    SearchBar(filter, onSearchChange, onSearchGo)

    Spacer(modifier = Modifier.width(8.dp))

    TabButtonGroup {
      WorkProfileToggle(filter, workProfileStatus, workProfileManager)

      MoreActionsTabButton(onMoreActionsClick)
    }

    Spacer(modifier = Modifier.width(8.dp))

    SearchFab(onSearchFabClick)
  }
}

@Composable
fun FilterSelectionTabGroup(
  filter: Filter,
  workProfileStatus: WorkProfileStatus,
  onChangeFilter: (Filter) -> Unit,
) {
  TabButtonGroup {
    if (workProfileStatus is WorkProfileInstalled) {
      ShowPersonalTabButton(
        isChecked = filter.let { it is ProfileFilter && it.profile.isPersonalProfile }
      ) {
        onChangeFilter(ProfileFilter(Process.myUserHandle()))
      }

      ShowWorkTabButton(
        isChecked = filter.let { it is ProfileFilter && !it.profile.isPersonalProfile }
      ) {
        onChangeFilter(ProfileFilter(workProfileStatus.userHandle))
      }
    } else {
      ShowAllAppsButton(isChecked = filter is ProfileFilter) {
        onChangeFilter(ProfileFilter(Process.myUserHandle()))
      }
    }

    ShowSearchTabButton(isChecked = filter is SearchFilter) { onChangeFilter(SearchFilter("")) }
  }
}

@Composable
fun SearchBar(filter: Filter, onValueChange: (String) -> Unit, onGo: () -> Unit) {
  AnimatedVisibility(
    visible = filter is SearchFilter,
    enter = fadeIn() + expandHorizontally(),
    exit = fadeOut() + shrinkHorizontally(),
  ) {
    val focusRequester = FocusRequester()

    TextField(
      value =
        filter.let {
          if (it is SearchFilter) {
            it.query
          } else {
            ""
          }
        },
      onValueChange = onValueChange,
      keyboardOptions = KeyboardOptions(imeAction = ImeAction.Go),
      keyboardActions = KeyboardActions(onGo = { onGo() }),
      modifier = Modifier.fillMaxWidth().padding(start = 8.dp).focusRequester(focusRequester),
    )

    LaunchedEffect(Unit) { focusRequester.requestFocus() }
  }
}

@Composable
fun WorkProfileToggle(
  filter: Filter,
  workProfileStatus: WorkProfileStatus,
  workProfileManager: WorkProfileManager,
) {
  AnimatedVisibility(
    visible = filter.let { it is ProfileFilter && !it.profile.isPersonalProfile },
    enter = fadeIn() + expandHorizontally(),
    exit = fadeOut() + shrinkHorizontally(),
  ) {
    val isChecked = workProfileStatus.let { it is WorkProfileInstalled && it.isEnabled }
    Switch(
      checked = isChecked,
      onCheckedChange = { workProfileManager.setWorkProfileEnabled(it) },
      thumbContent = {
        Icon(
          painter =
            painterResource(
              if (isChecked) {
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
