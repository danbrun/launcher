package link.danb.launcher.shortcuts

import android.app.Application
import android.content.IntentSender
import android.content.pm.LauncherApps
import android.content.pm.LauncherApps.ShortcutQuery
import android.content.pm.ShortcutInfo
import android.graphics.Rect
import android.os.Bundle
import android.os.Process
import android.os.UserHandle
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import link.danb.launcher.data.UserShortcut
import link.danb.launcher.data.UserShortcutCreator
import link.danb.launcher.extensions.getShortcuts
import link.danb.launcher.extensions.resolveConfigurableShortcut
import link.danb.launcher.profiles.ProfilesModel

@HiltViewModel
class ShortcutsViewModel
@Inject
constructor(
  application: Application,
  private val launcherApps: LauncherApps,
  private val profilesModel: ProfilesModel,
) : AndroidViewModel(application) {

  private val _pinnedShortcuts: MutableStateFlow<List<UserShortcut>> = MutableStateFlow(listOf())

  val pinnedShortcuts: StateFlow<List<UserShortcut>> = _pinnedShortcuts.asStateFlow()

  init {
    viewModelScope.launch {
      profilesModel.workProfileData.collect { workProfileData -> update(workProfileData) }
    }
  }

  fun getShortcuts(packageName: String, user: UserHandle): List<ShortcutInfo> {
    val (workProfile, isWorkProfileEnabled) = profilesModel.workProfileData.value
    if (user == workProfile && !isWorkProfileEnabled) return listOf()

    return launcherApps.getShortcuts(user) {
      setPackage(packageName)
      setQueryFlags(
        ShortcutQuery.FLAG_MATCH_DYNAMIC or
          ShortcutQuery.FLAG_MATCH_MANIFEST or
          ShortcutQuery.FLAG_MATCH_PINNED
      )
    }
  }

  fun pinShortcut(userShortcut: UserShortcut) {
    setPinnedShortcuts(
      userShortcut,
      getPinnedShortcutIds(userShortcut.packageName) + userShortcut.shortcutId,
    )
  }

  fun unpinShortcut(userShortcut: UserShortcut) {
    setPinnedShortcuts(
      userShortcut,
      getPinnedShortcutIds(userShortcut.packageName) - userShortcut.shortcutId,
    )
  }

  fun launchShortcut(userShortcut: UserShortcut, sourceBounds: Rect, startActivityOptions: Bundle) {
    launcherApps.startShortcut(
      userShortcut.packageName,
      userShortcut.shortcutId,
      sourceBounds,
      startActivityOptions,
      userShortcut.userHandle,
    )
  }

  fun getConfigurableShortcutIntent(userShortcutCreator: UserShortcutCreator): IntentSender =
    launcherApps.getShortcutConfigActivityIntent(
      launcherApps.resolveConfigurableShortcut(userShortcutCreator)
    )!!

  private fun getPinnedShortcutIds(packageName: String): Set<String> =
    getPinnedShortcuts(packageName).map { it.id }.toSet()

  private fun getPinnedShortcuts(packageName: String): Set<ShortcutInfo> =
    launcherApps
      .getShortcuts(profilesModel.activeProfile.value) {
        setPackage(packageName)
        setQueryFlags(ShortcutQuery.FLAG_MATCH_PINNED)
      }
      .toSet()

  private fun setPinnedShortcuts(userShortcut: UserShortcut, shortcutIds: Set<String>) {
    launcherApps.pinShortcuts(
      userShortcut.packageName,
      shortcutIds.toList(),
      userShortcut.userHandle,
    )
    update(profilesModel.workProfileData.value)
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
          setQueryFlags(ShortcutQuery.FLAG_MATCH_PINNED)
        }
      } else {
        listOf()
      }

    _pinnedShortcuts.value = (personalShortcuts + workShortcuts).map { UserShortcut(it) }
  }
}
