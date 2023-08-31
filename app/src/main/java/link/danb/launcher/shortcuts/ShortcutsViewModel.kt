package link.danb.launcher.shortcuts

import android.app.Application
import android.content.pm.LauncherApps
import android.content.pm.LauncherApps.ShortcutQuery
import android.content.pm.ShortcutInfo
import android.os.UserHandle
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import link.danb.launcher.extensions.packageName
import link.danb.launcher.profiles.ProfilesModel
import javax.inject.Inject

@HiltViewModel
class ShortcutsViewModel
@Inject
constructor(
  application: Application,
  private val launcherApps: LauncherApps,
  private val profilesModel: ProfilesModel,
) : AndroidViewModel(application) {

  private val _pinnedShortcuts: MutableStateFlow<List<ShortcutInfo>> = MutableStateFlow(listOf())

  val pinnedShortcuts: StateFlow<List<ShortcutInfo>> = _pinnedShortcuts.asStateFlow()

  init {
    viewModelScope.launch { profilesModel.activeProfile.collect(::update) }
  }

  fun getShortcuts(packageName: String, user: UserHandle): List<ShortcutInfo> {
    val (workProfile, isWorkProfileEnabled) = profilesModel.workProfileData.value
    if (user == workProfile && !isWorkProfileEnabled) return listOf()

    return launcherApps.getShortcuts(
      ShortcutQuery()
        .setPackage(packageName)
        .setQueryFlags(ShortcutQuery.FLAG_MATCH_DYNAMIC or ShortcutQuery.FLAG_MATCH_MANIFEST),
      user
    )
      ?: listOf()
  }

  fun pinShortcut(shortcutInfo: ShortcutInfo) {
    setPinnedShortcuts(
      shortcutInfo,
      getPinnedShortcutIds(shortcutInfo.packageName) + shortcutInfo.id
    )
  }

  fun unpinShortcut(shortcutInfo: ShortcutInfo) {
    setPinnedShortcuts(
      shortcutInfo,
      getPinnedShortcutIds(shortcutInfo.packageName) - shortcutInfo.id
    )
  }

  private fun getPinnedShortcutIds(packageName: String): Set<String> =
    getPinnedShortcuts(packageName).map { it.id }.toSet()

  private fun getPinnedShortcuts(packageName: String): Set<ShortcutInfo> =
    launcherApps
      .getShortcuts(
        ShortcutQuery().setPackage(packageName).setQueryFlags(ShortcutQuery.FLAG_MATCH_PINNED),
        profilesModel.activeProfile.value
      )
      ?.toSet()
      ?: setOf()

  private fun setPinnedShortcuts(shortcutInfo: ShortcutInfo, shortcutIds: Set<String>) {
    launcherApps.pinShortcuts(shortcutInfo.`package`, shortcutIds.toList(), shortcutInfo.userHandle)
    update(profilesModel.activeProfile.value)
  }

  private fun update(user: UserHandle) {
    _pinnedShortcuts.value =
      launcherApps.getShortcuts(
        ShortcutQuery().setQueryFlags(ShortcutQuery.FLAG_MATCH_PINNED),
        user
      )!!
  }
}
