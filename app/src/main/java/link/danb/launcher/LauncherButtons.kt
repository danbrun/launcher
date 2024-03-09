package link.danb.launcher

import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource

@Composable
fun ShowPersonalTabButton(isChecked: Boolean, onClick: () -> Unit) {
  TabButton(
    icon = painterResource(id = R.drawable.baseline_person_24),
    name = stringResource(id = R.string.show_personal),
    isChecked = isChecked,
    onClick = onClick,
  )
}

@Composable
fun ShowWorkTabButton(isChecked: Boolean, onClick: () -> Unit) {
  TabButton(
    icon = painterResource(id = R.drawable.ic_baseline_work_24),
    name = stringResource(id = R.string.show_work),
    isChecked = isChecked,
    onClick = onClick,
  )
}

@Composable
fun ShowAllAppsButton(isChecked: Boolean, onClick: () -> Unit) {
  TabButton(
    icon = painterResource(id = R.drawable.baseline_apps_24),
    name = stringResource(id = R.string.show_home),
    isChecked = isChecked,
    onClick = onClick,
  )
}

@Composable
fun ShowSearchTabButton(isChecked: Boolean, onClick: () -> Unit) {
  TabButton(
    icon = painterResource(id = R.drawable.ic_baseline_search_24),
    name = stringResource(id = R.string.search),
    isChecked = isChecked,
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
