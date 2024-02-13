package link.danb.launcher.tiles

import android.content.pm.LauncherApps
import javax.inject.Inject
import javax.inject.Singleton
import link.danb.launcher.database.ActivityData
import link.danb.launcher.extensions.resolveActivity
import link.danb.launcher.extensions.resolveConfigurableShortcut
import link.danb.launcher.extensions.resolveShortcut
import link.danb.launcher.icons.ComponentHandle
import link.danb.launcher.icons.LauncherIconCache
import link.danb.launcher.icons.ShortcutHandle
import link.danb.launcher.data.UserShortcutCreator
import link.danb.launcher.data.UserShortcut
import link.danb.launcher.tiles.TileViewItem.Style

@Singleton
class TileViewItemFactory
@Inject
constructor(
  private val launcherApps: LauncherApps,
  private val launcherIconCache: LauncherIconCache,
) {

  suspend fun getTileViewItem(data: ActivityData, style: Style) =
    TileViewItem(
      style,
      data,
      launcherApps.resolveActivity(data.userActivity).label.toString(),
      launcherIconCache
        .getIcon(ComponentHandle(data.userActivity.componentName, data.userActivity.userHandle))
        .await(),
    ) { other ->
      this is ActivityData && other is ActivityData && userActivity == other.userActivity
    }

  suspend fun getTileViewItem(data: UserShortcut, style: Style) =
    TileViewItem(
      style,
      data,
      launcherApps.resolveShortcut(data).shortLabel.toString(),
      launcherIconCache
        .getIcon(ShortcutHandle(data.packageName, data.shortcutId, data.userHandle))
        .await(),
    )

  suspend fun getTileViewItem(data: UserShortcutCreator, style: Style) =
    TileViewItem(
      style,
      data,
      launcherApps.resolveConfigurableShortcut(data).label.toString(),
      launcherIconCache.getIcon(ComponentHandle(data.componentName, data.userHandle)).await(),
    )
}
