package link.danb.launcher

import android.os.UserHandle
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import link.danb.launcher.extensions.isPersonalProfile

@Composable
fun ShowPersonalTabButton(activeProfile: UserHandle, searchQuery: String?, onClick: () -> Unit) {
  TabButton(
    icon = painterResource(id = R.drawable.baseline_person_24),
    name = stringResource(id = R.string.show_personal),
    isChecked = activeProfile.isPersonalProfile && searchQuery == null,
    onClick = onClick,
  )
}

@Composable
fun ShowWorkTabButton(activeProfile: UserHandle, searchQuery: String?, onClick: () -> Unit) {
  TabButton(
    icon = painterResource(id = R.drawable.ic_baseline_work_24),
    name = stringResource(id = R.string.show_work),
    isChecked = !activeProfile.isPersonalProfile && searchQuery == null,
    onClick = onClick,
  )
}

@Composable
fun ShowSearchTabButton(searchQuery: String?, onClick: () -> Unit) {
  TabButton(
    icon = painterResource(id = R.drawable.ic_baseline_search_24),
    name = stringResource(id = R.string.search),
    isChecked = searchQuery != null,
    onClick = onClick,
  )
}

@Composable
fun MoreActionsTabButton(onClick: () -> Unit) {
  TabButton(
    icon = painterResource(id = R.drawable.baseline_more_horiz_24),
    name = stringResource(id = R.string.more_actions),
    isChecked = false,
    onClick = onClick,
  )
}

@Composable
fun SearchFab(onClick: () -> Unit) {
  FloatingActionButton(onClick = onClick) {
    Icon(
      painter = painterResource(id = R.drawable.travel_explore_24),
      contentDescription = stringResource(id = R.string.search),
    )
  }
}
