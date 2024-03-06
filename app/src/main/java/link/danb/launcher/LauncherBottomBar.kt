package link.danb.launcher

import android.app.SearchManager
import android.content.Intent
import android.os.Bundle
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsBottomHeight
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.FilledIconToggleButton
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.fragment.app.FragmentManager
import link.danb.launcher.extensions.isPersonalProfile
import link.danb.launcher.profiles.ProfilesModel
import link.danb.launcher.profiles.WorkProfileInstalled
import link.danb.launcher.profiles.WorkProfileManager
import link.danb.launcher.profiles.WorkProfileNotInstalled

@Composable
fun ProfileTabs(
  profilesModel: ProfilesModel,
  workProfileManager: WorkProfileManager,
  fragmentManager: FragmentManager,
) {
  val hasWorkProfile by workProfileManager.status.collectAsState(initial = WorkProfileNotInstalled)

  TabButtonGroup {
    ShowPersonalTabButton(profilesModel)

    if (hasWorkProfile is WorkProfileInstalled) {
      ShowWorkTabButton(profilesModel)
    }

    MoreActionsTabButton(fragmentManager)
  }
}

@Composable
fun ShowPersonalTabButton(profilesModel: ProfilesModel) {
  val activeProfile by profilesModel.activeProfile.collectAsState()

  TabButton(
    icon = painterResource(id = R.drawable.baseline_person_24),
    name = stringResource(id = R.string.show_personal),
    isChecked = activeProfile.isPersonalProfile,
  ) {
    profilesModel.toggleActiveProfile(showWorkProfile = false)
  }
}

@Composable
fun ShowWorkTabButton(profilesModel: ProfilesModel) {
  val activeProfile by profilesModel.activeProfile.collectAsState()

  TabButton(
    icon = painterResource(id = R.drawable.ic_baseline_work_24),
    name = stringResource(id = R.string.show_work),
    isChecked = !activeProfile.isPersonalProfile,
  ) {
    profilesModel.toggleActiveProfile(showWorkProfile = true)
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
      painter = painterResource(id = R.drawable.ic_baseline_search_24),
      contentDescription = stringResource(id = R.string.search),
    )
  }
}

@Composable
fun BottomBar(
  tabButtonGroups: @Composable () -> Unit,
  floatingActionButton: @Composable () -> Unit,
) {
  Column {
    Row(modifier = Modifier.align(Alignment.CenterHorizontally)) {
      tabButtonGroups()

      Spacer(modifier = Modifier.width(8.dp))

      floatingActionButton()
    }

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
