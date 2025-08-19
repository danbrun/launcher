package link.danb.launcher.apps

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.LauncherActivityInfo
import android.content.pm.LauncherApps
import android.content.pm.LauncherApps.ShortcutQuery
import android.content.pm.ShortcutInfo
import android.graphics.Color
import android.graphics.drawable.AdaptiveIconDrawable
import android.graphics.drawable.Drawable
import android.graphics.drawable.InsetDrawable
import android.graphics.drawable.PaintDrawable
import android.os.UserHandle
import androidx.annotation.DrawableRes
import androidx.core.content.getSystemService
import androidx.core.graphics.drawable.toBitmap
import androidx.core.graphics.drawable.toDrawable
import androidx.palette.graphics.Palette
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Deferred
import link.danb.launcher.R
import link.danb.launcher.components.UserActivity
import link.danb.launcher.components.UserApplication
import link.danb.launcher.components.UserComponent
import link.danb.launcher.components.UserShortcut
import link.danb.launcher.components.UserShortcutCreator
import link.danb.launcher.profiles.Profile
import link.danb.launcher.profiles.ProfileManager

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

  fun getIcon(userComponent: UserComponent): AdaptiveIconDrawable =
    userComponent.getSourceIcon().toAdaptiveIconDrawable()

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

  private fun Drawable.toAdaptiveIconDrawable(): AdaptiveIconDrawable {
    if (this is AdaptiveIconDrawable && foreground != null && background != null) {
      return this
    }

    try {
      val palette = Palette.from(toBitmap()).generate()
      val background = PaintDrawable(palette.getMutedColor(Color.WHITE))
      val foreground = InsetDrawable(this, AdaptiveIconDrawable.getExtraInsetFraction())
      return AdaptiveIconDrawable(background, foreground)
    } catch (_: IllegalArgumentException) {
      return AdaptiveIconDrawable(Color.WHITE.toDrawable(), this)
    }
  }

  companion object {
    @DrawableRes
    fun getBadge(profile: Profile): Int? =
      when (profile) {
        Profile.PERSONAL -> null
        Profile.WORK -> R.drawable.ic_baseline_work_24
        Profile.PRIVATE -> R.drawable.baseline_lock_24
      }
  }

  @EntryPoint
  @InstallIn(SingletonComponent::class)
  interface LauncherResourceProviderEntryPoint {
    @Singleton fun launcherResourceProvider(): LauncherResourceProvider
  }
}
