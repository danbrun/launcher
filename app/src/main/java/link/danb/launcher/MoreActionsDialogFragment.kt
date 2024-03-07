package link.danb.launcher

import android.os.Bundle
import android.os.UserHandle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.os.bundleOf
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import dagger.hilt.android.AndroidEntryPoint
import link.danb.launcher.activities.HiddenActivitiesDialogFragment
import link.danb.launcher.extensions.getParcelableCompat
import link.danb.launcher.shortcuts.PinShortcutsDialogFragment
import link.danb.launcher.ui.theme.LauncherTheme
import link.danb.launcher.widgets.PinWidgetsDialogFragment

@AndroidEntryPoint
class MoreActionsDialogFragment : BottomSheetDialogFragment() {

  private val userHandle: UserHandle by lazy {
    checkNotNull(requireArguments().getParcelableCompat(EXTRA_USER_HANDLE))
  }

  private val hasHiddenApps: Boolean by lazy {
    requireArguments().getBoolean(EXTRA_HAS_HIDDEN_APPS)
  }

  override fun onCreateView(
    inflater: LayoutInflater,
    container: ViewGroup?,
    savedInstanceState: Bundle?,
  ): View {
    super.onCreateView(inflater, container, savedInstanceState)

    return ComposeView(requireContext()).apply {
      setContent {
        LauncherTheme {
          Column(modifier = Modifier.padding(8.dp)) {
            ActionItem(R.drawable.baseline_shortcut_24, R.string.pin_shortcut) {
              PinShortcutsDialogFragment.newInstance(userHandle)
                .showNow(parentFragmentManager, PinShortcutsDialogFragment.TAG)
              dismissNow()
            }

            ActionItem(R.drawable.ic_baseline_widgets_24, R.string.pin_widget) {
              PinWidgetsDialogFragment.newInstance(userHandle)
                .showNow(parentFragmentManager, PinShortcutsDialogFragment.TAG)
              dismissNow()
            }

            if (hasHiddenApps) {
              ActionItem(R.drawable.ic_baseline_visibility_24, R.string.show_hidden) {
                HiddenActivitiesDialogFragment.newInstance(userHandle)
                  .showNow(parentFragmentManager, HiddenActivitiesDialogFragment.TAG)
                dismissNow()
              }
            }
          }
        }
      }
    }
  }

  companion object {
    const val TAG = "more_actions_dialog_fragment"

    private const val EXTRA_USER_HANDLE = "extra_user_handle"
    private const val EXTRA_HAS_HIDDEN_APPS = "extra_has_hidden_apps"

    fun newInstance(userHandle: UserHandle, hasHiddenApps: Boolean): MoreActionsDialogFragment =
      MoreActionsDialogFragment().apply {
        arguments =
          bundleOf(EXTRA_USER_HANDLE to userHandle, EXTRA_HAS_HIDDEN_APPS to hasHiddenApps)
      }
  }
}

@Composable
fun ActionItem(icon: Int, name: Int, onClick: () -> Unit) {
  Card(shape = RoundedCornerShape(32.dp), modifier = Modifier.padding(8.dp)) {
    Row(modifier = Modifier.fillMaxWidth().clickable { onClick() }.padding(8.dp)) {
      Icon(
        painter = painterResource(id = icon),
        contentDescription = null,
        modifier = Modifier.padding(8.dp),
      )
      Text(text = stringResource(id = name), modifier = Modifier.align(Alignment.CenterVertically))
    }
  }
}
