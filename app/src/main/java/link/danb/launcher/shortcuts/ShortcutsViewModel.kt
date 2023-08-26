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
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import link.danb.launcher.profiles.ProfilesModel
import link.danb.launcher.profiles.ProfilesModel.WorkProfileData
import javax.inject.Inject

@HiltViewModel
class ShortcutsViewModel @Inject constructor(
    application: Application,
    private val launcherApps: LauncherApps,
    private val profilesModel: ProfilesModel,
) : AndroidViewModel(application) {

    private val _shortcuts: MutableStateFlow<List<ShortcutInfo>> = MutableStateFlow(listOf())

    val shortcuts = _shortcuts.asStateFlow()

    init {
        viewModelScope.launch {
            profilesModel.workProfileData.collect { workProfileData ->
                _shortcuts.value = getShortcuts(workProfileData)
            }
        }
    }

    fun pinShortcut(shortcutInfo: ShortcutInfo) {
        setPinnedShortcuts(
            shortcutInfo, getPinnedShortcuts(shortcutInfo.`package`) + shortcutInfo.id
        )
    }

    fun unpinShortcut(shortcutInfo: ShortcutInfo) {
        setPinnedShortcuts(
            shortcutInfo, getPinnedShortcuts(shortcutInfo.`package`) - shortcutInfo.id
        )
    }

    private fun getPinnedShortcuts(packageName: String): Set<String> =
        _shortcuts.value.filter { it.`package` == packageName }.map { it.id }.toSet()

    private fun setPinnedShortcuts(shortcutInfo: ShortcutInfo, shortcutIds: Set<String>) {
        launcherApps.pinShortcuts(
            shortcutInfo.`package`, shortcutIds.toList(), shortcutInfo.userHandle
        )
        _shortcuts.value = getShortcuts(profilesModel.workProfileData.value)
    }

    private fun getShortcuts(workProfileData: WorkProfileData): List<ShortcutInfo> = buildList {
        addAll(getShortcuts(ProfilesModel.personalProfile))
        workProfileData.user?.takeIf { workProfileData.isEnabled }
            ?.let { addAll(getShortcuts(it)) }
    }

    private fun getShortcuts(user: UserHandle): List<ShortcutInfo> = launcherApps.getShortcuts(
        ShortcutQuery().setQueryFlags(ShortcutQuery.FLAG_MATCH_PINNED), user
    )!!
}
