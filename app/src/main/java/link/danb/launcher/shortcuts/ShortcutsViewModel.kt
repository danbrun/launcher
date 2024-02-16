package link.danb.launcher.shortcuts

import android.app.Application
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.LauncherApps
import android.content.pm.LauncherApps.ShortcutQuery
import android.os.Build
import android.os.Process
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import link.danb.launcher.data.UserShortcut
import link.danb.launcher.extensions.getShortcuts
import link.danb.launcher.profiles.ProfilesModel

@HiltViewModel
class ShortcutsViewModel
@Inject
constructor(
  application: Application,
  private val launcherApps: LauncherApps,
  private val profilesModel: ProfilesModel,
) : AndroidViewModel(application) {

  private val _shortcuts: MutableStateFlow<List<UserShortcut>> = MutableStateFlow(listOf())

  val shortcuts: StateFlow<List<UserShortcut>> = _shortcuts.asStateFlow()

  private val receiver =
    object : BroadcastReceiver() {
      override fun onReceive(context: Context?, intent: Intent?) {
        update(profilesModel.workProfileData.value)
      }
    }

  init {
    viewModelScope.launch {
      profilesModel.workProfileData.collect { workProfileData -> update(workProfileData) }
    }

    application.registerReceiver(
      receiver,
      IntentFilter().apply { addAction(ShortcutManager.ACTION_PINNED_SHORTCUTS_CHANGED) },
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        Context.RECEIVER_NOT_EXPORTED
      } else {
        0
      },
    )
  }

  override fun onCleared() {
    super.onCleared()

    getApplication<Application>().unregisterReceiver(receiver)
  }

  private fun update(workProfileData: ProfilesModel.WorkProfileData) {
    if (!launcherApps.hasShortcutHostPermission()) return

    val personalShortcuts =
      launcherApps.getShortcuts(Process.myUserHandle()) {
        setQueryFlags(ShortcutQuery.FLAG_MATCH_PINNED)
      }

    val workShortcuts =
      if (workProfileData.user != null && workProfileData.isEnabled) {
        launcherApps.getShortcuts(workProfileData.user) {
          setQueryFlags(
            ShortcutQuery.FLAG_MATCH_DYNAMIC or
              ShortcutQuery.FLAG_MATCH_MANIFEST or
              ShortcutQuery.FLAG_MATCH_PINNED
          )
        }
      } else {
        listOf()
      }

    _shortcuts.value = (personalShortcuts + workShortcuts).map { UserShortcut(it) }
  }
}
