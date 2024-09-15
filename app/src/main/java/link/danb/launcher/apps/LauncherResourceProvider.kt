package link.danb.launcher.apps

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.LauncherActivityInfo
import android.content.pm.LauncherApps
import android.content.pm.LauncherApps.ShortcutQuery
import android.content.pm.ShortcutInfo
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.drawable.AdaptiveIconDrawable
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.graphics.drawable.InsetDrawable
import android.graphics.drawable.PaintDrawable
import android.os.UserHandle
import androidx.core.content.getSystemService
import androidx.core.graphics.drawable.toBitmap
import androidx.palette.graphics.Palette
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.async
import kotlinx.coroutines.plus
import kotlinx.coroutines.withContext
import link.danb.launcher.R
import link.danb.launcher.components.UserActivity
import link.danb.launcher.components.UserApplication
import link.danb.launcher.components.UserComponent
import link.danb.launcher.components.UserShortcut
import link.danb.launcher.components.UserShortcutCreator
import link.danb.launcher.profiles.Profile
import link.danb.launcher.profiles.ProfileManager
import link.danb.launcher.ui.LauncherIconData
import link.danb.launcher.ui.LauncherTileData

@Singleton
class LauncherResourceProvider
@Inject
constructor(
  @ApplicationContext private val context: Context,
  private val profileManager: ProfileManager,
) {

  private val launcherApps: LauncherApps by lazy { checkNotNull(context.getSystemService()) }
  private val density: Int by lazy { context.resources.displayMetrics.densityDpi }
  private val icons: MutableMap<UserComponent, Deferred<AdaptiveIconDrawable>> = mutableMapOf()
  private val badges: MutableMap<Profile, Drawable> = mutableMapOf()
  private val coroutineScope: CoroutineScope = MainScope() + Dispatchers.IO

  init {
    launcherApps.registerCallback(
      LauncherAppsCallback { packageNames: List<String>, userHandle: UserHandle ->
        icons.keys.removeIf {
          it.packageName in packageNames && it.profile == profileManager.getProfile(userHandle)
        }
      }
    )
  }

  fun getLabel(userComponent: UserComponent): String =
    when (userComponent) {
      is UserApplication -> {
        launcherApps.resolveApplication(userComponent).loadLabel(context.packageManager).toString()
      }
      is UserActivity -> {
        launcherApps.resolveActivity(userComponent).label.toString()
      }
      is UserShortcut -> {
        launcherApps.resolveShortcut(userComponent).shortLabel.toString()
      }
      is UserShortcutCreator -> {
        getLabel(UserActivity(userComponent.componentName, userComponent.profile))
      }
    }

  fun getIconWithCache(userComponent: UserComponent): Deferred<AdaptiveIconDrawable> =
    synchronized(this) {
      icons.getOrPut(userComponent) { coroutineScope.async { getIcon(userComponent) } }
    }

  suspend fun getIcon(userComponent: UserComponent): AdaptiveIconDrawable =
    withContext(Dispatchers.IO) { userComponent.getSourceIcon().toAdaptiveIconDrawable() }

  fun getBadge(profile: Profile): Drawable =
    badges.getOrPut(profile) {
      val size = context.resources.getDimensionPixelOffset(R.dimen.launcher_icon_size)
      context.packageManager.getUserBadgedIcon(
        BitmapDrawable(context.resources, Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)),
        profileManager.getUserHandle(profile)!!,
      )
    }

  suspend fun getTileData(userComponent: UserComponent): LauncherTileData =
    LauncherTileData(
      LauncherIconData(getIcon(userComponent), getBadge(userComponent.profile)),
      getLabel(userComponent),
    )

  suspend fun getTileDataWithCache(userComponent: UserComponent): Deferred<LauncherTileData> =
    coroutineScope.async {
      LauncherTileData(
        LauncherIconData(getIconWithCache(userComponent).await(), getBadge(userComponent.profile)),
        getLabel(userComponent),
      )
    }

  private fun UserComponent.getSourceIcon(): Drawable =
    when (this) {
      is UserApplication -> {
        launcherApps.resolveApplication(this).loadUnbadgedIcon(context.packageManager)
      }
      is UserActivity -> {
        launcherApps.resolveActivity(this).getIcon(density)
      }
      is UserShortcut -> {
        launcherApps.getShortcutIconDrawable(launcherApps.resolveShortcut(this), density)
          ?: UserApplication(packageName, profile).getSourceIcon()
      }
      is UserShortcutCreator -> {
        UserActivity(componentName, profile).getSourceIcon()
      }
    }

  private fun LauncherApps.resolveApplication(userApplication: UserApplication): ApplicationInfo =
    getApplicationInfo(
      userApplication.packageName,
      0,
      profileManager.getUserHandle(userApplication.profile)!!,
    )

  private fun LauncherApps.resolveActivity(
    componentName: ComponentName,
    userHandle: UserHandle,
  ): LauncherActivityInfo = resolveActivity(Intent().setComponent(componentName), userHandle)

  private fun LauncherApps.resolveActivity(userActivity: UserActivity): LauncherActivityInfo =
    resolveActivity(
      userActivity.componentName,
      profileManager.getUserHandle(userActivity.profile)!!,
    )

  private fun LauncherApps.getShortcuts(
    userHandle: UserHandle,
    queryBuilder: ShortcutQuery.() -> Unit,
  ): List<ShortcutInfo> =
    if (hasShortcutHostPermission()) {
      getShortcuts(ShortcutQuery().apply(queryBuilder), userHandle) ?: listOf()
    } else {
      listOf()
    }

  private fun LauncherApps.resolveShortcut(
    packageName: String,
    shortcutId: String,
    userHandle: UserHandle,
  ) =
    getShortcuts(userHandle) {
        setQueryFlags(
          ShortcutQuery.FLAG_MATCH_DYNAMIC or
            ShortcutQuery.FLAG_MATCH_MANIFEST or
            ShortcutQuery.FLAG_MATCH_PINNED
        )
        setPackage(packageName)
        setShortcutIds(listOf(shortcutId))
      }
      .first()

  private fun LauncherApps.resolveShortcut(userShortcut: UserShortcut): ShortcutInfo =
    resolveShortcut(
      userShortcut.packageName,
      userShortcut.shortcutId,
      profileManager.getUserHandle(userShortcut.profile)!!,
    )

  private suspend fun Drawable.toAdaptiveIconDrawable(): AdaptiveIconDrawable {
    if (this is AdaptiveIconDrawable && foreground != null && background != null) {
      return this
    }

    val palette = withContext(Dispatchers.IO) { Palette.from(toBitmap()).generate() }
    val background = PaintDrawable(palette.getMutedColor(Color.WHITE))
    val foreground = InsetDrawable(this, AdaptiveIconDrawable.getExtraInsetFraction())

    return AdaptiveIconDrawable(background, foreground)
  }
}
