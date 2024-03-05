package link.danb.launcher

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import link.danb.launcher.activities.HiddenActivitiesDialogFragment
import link.danb.launcher.profiles.ProfilesModel
import link.danb.launcher.shortcuts.PinShortcutsDialogFragment
import link.danb.launcher.ui.theme.LauncherTheme
import link.danb.launcher.widgets.PinWidgetsDialogFragment

@AndroidEntryPoint
class MoreActionsDialogFragment : BottomSheetDialogFragment() {

  @Inject lateinit var profilesModel: ProfilesModel

  override fun onCreateView(
    inflater: LayoutInflater,
    container: ViewGroup?,
    savedInstanceState: Bundle?,
  ): View {
    super.onCreateView(inflater, container, savedInstanceState)

    return ComposeView(requireContext()).apply {
      setContent {
        val actionItems =
          listOf(
            ActionItem(R.drawable.baseline_shortcut_24, R.string.pin_shortcut) {
              PinShortcutsDialogFragment.newInstance(profilesModel.activeProfile.value)
                .showNow(parentFragmentManager, PinShortcutsDialogFragment.TAG)
              dismissNow()
            },
            ActionItem(R.drawable.ic_baseline_widgets_24, R.string.pin_widget) {
              PinWidgetsDialogFragment.newInstance(profilesModel.activeProfile.value)
                .showNow(parentFragmentManager, PinShortcutsDialogFragment.TAG)
              dismissNow()
            },
            ActionItem(R.drawable.ic_baseline_visibility_24, R.string.show_hidden) {
              HiddenActivitiesDialogFragment.newInstance(profilesModel.activeProfile.value)
                .showNow(parentFragmentManager, HiddenActivitiesDialogFragment.TAG)
              dismissNow()
            },
          )

        LauncherTheme {
          Column(modifier = Modifier.padding(8.dp)) {
            for (actionItem in actionItems) {
              ActionItem(actionItem)
            }
          }
        }
      }
    }
  }

  companion object {
    const val TAG = "more_actions_dialog_fragment"
  }
}

@Immutable
data class ActionItem(
  @DrawableRes val icon: Int,
  @StringRes val name: Int,
  val onClick: () -> Unit,
)

@Composable
fun ActionItem(actionItem: ActionItem) {
  Card(shape = RoundedCornerShape(32.dp), modifier = Modifier.padding(8.dp)) {
    Row(modifier = Modifier.fillMaxWidth().clickable { actionItem.onClick() }.padding(8.dp)) {
      Icon(
        painter = painterResource(id = actionItem.icon),
        contentDescription = null,
        modifier = Modifier.padding(8.dp),
      )
      Text(
        text = stringResource(id = actionItem.name),
        modifier = Modifier.align(Alignment.CenterVertically),
      )
    }
  }
}
