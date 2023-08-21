package link.danb.launcher.shortcuts

import android.app.Application
import android.content.pm.LauncherApps
import android.content.pm.LauncherApps.ShortcutQuery
import android.content.pm.ShortcutInfo
import androidx.lifecycle.AndroidViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

@HiltViewModel
class ShortcutsViewModel @Inject constructor(
    application: Application, private val launcherApps: LauncherApps
) : AndroidViewModel(application) {

    private val _shortcuts: MutableStateFlow<List<ShortcutInfo>> = MutableStateFlow(listOf())

    val shortcuts = _shortcuts.asStateFlow()

    init {
        refresh()
    }

    fun pinShortcut(shortcutInfo: ShortcutInfo) {
        launcherApps.pinShortcuts(
            shortcutInfo.`package`,
            getPinnedIdsForPackage(shortcutInfo.`package`) + shortcutInfo.id,
            shortcutInfo.userHandle
        )
        refresh()
    }

    fun unpinShortcut(shortcutInfo: ShortcutInfo) {
        launcherApps.pinShortcuts(
            shortcutInfo.`package`,
            getPinnedIdsForPackage(shortcutInfo.`package`) - shortcutInfo.id,
            shortcutInfo.userHandle
        )
        refresh()
    }

    private fun getPinnedIdsForPackage(pkg: String): List<String> =
        _shortcuts.value.filter { it.`package` == pkg }.map { it.id }

    private fun refresh() {
        _shortcuts.value = launcherApps.profiles.flatMap {
            launcherApps.getShortcuts(
                ShortcutQuery().setQueryFlags(ShortcutQuery.FLAG_MATCH_PINNED), it
            )!!
        }
    }
}
