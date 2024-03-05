package link.danb.launcher

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.height
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
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp

@Immutable
data class BottomBarData(val tabBar: TabBarData, val actionButtonData: ActionButtonData)

@Composable
fun BottomBar(bottomBar: BottomBarData) {
  Column {
    Spacer(modifier = Modifier.height(8.dp))

    Row(modifier = Modifier.align(Alignment.CenterHorizontally)) {
      TabBar(tabBar = bottomBar.tabBar, modifier = Modifier.align(Alignment.CenterVertically))

      Spacer(modifier = Modifier.width(8.dp))

      ActionButton(
        bottomBar.actionButtonData,
        modifier = Modifier.align(Alignment.CenterVertically),
      )
    }

    Spacer(modifier = Modifier.windowInsetsBottomHeight(WindowInsets.systemBars))
  }
}

@Immutable
data class TabBarData(val buttons: List<TabButtonData>)

@Composable
fun TabBar(tabBar: TabBarData, modifier: Modifier = Modifier) {
  Card(modifier = modifier, shape = RoundedCornerShape(28.dp)) {
    Row(modifier = Modifier.padding(4.dp)) {
      for (button in tabBar.buttons) {
        TabButton(button)
      }
    }
  }
}

@Immutable
data class TabButtonData(
  @DrawableRes val icon: Int,
  @StringRes val description: Int,
  val isHighlighted: Boolean,
  val onClick: () -> Unit,
)

@Composable
fun TabButton(tabButton: TabButtonData) {
  FilledIconToggleButton(
    checked = tabButton.isHighlighted,
    onCheckedChange = { _ -> tabButton.onClick() },
  ) {
    Icon(
      painter = painterResource(id = tabButton.icon),
      contentDescription = stringResource(id = tabButton.description),
    )
  }
}

@Immutable
data class ActionButtonData(
  @DrawableRes val icon: Int,
  @StringRes val description: Int,
  val onClick: () -> Unit,
)

@Composable
fun ActionButton(actionButton: ActionButtonData, modifier: Modifier = Modifier) {
  FloatingActionButton(onClick = actionButton.onClick, modifier = modifier) {
    Icon(
      painter = painterResource(id = actionButton.icon),
      contentDescription = stringResource(id = actionButton.description),
    )
  }
}
