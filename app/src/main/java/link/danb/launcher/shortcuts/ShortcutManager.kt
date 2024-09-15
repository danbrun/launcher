package link.danb.launcher.shortcuts

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.IntentSender
import android.content.pm.LauncherActivityInfo
import android.content.pm.LauncherApps
import android.content.pm.LauncherApps.PinItemRequest
import android.content.pm.LauncherApps.ShortcutQuery
import android.content.pm.ShortcutInfo
import android.graphics.Rect
import android.os.Bundle
import android.os.UserHandle
import androidx.core.content.ContextCompat
import androidx.core.content.getSystemService
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.stateIn
import link.danb.launcher.apps.LauncherAppsCallback
import link.danb.launcher.components.UserComponent
import link.danb.launcher.components.UserShortcut
import link.danb.launcher.components.UserShortcutCreator
import link.danb.launcher.profiles.ProfileManager

@Singleton
class ShortcutManager
@Inject
constructor(
  @ApplicationContext private val context: Context,
  private val profileManager: ProfileManager,
) {

  private val launcherApps: LauncherApps by lazy { checkNotNull(context.getSystemService()) }

  val shortcuts: Flow<List<UserShortcut>> =
    callbackFlow {
        trySend(getPinnedShortcuts())

        val launcherAppsCallback = LauncherAppsCallback { _, _ -> trySend(getPinnedShortcuts()) }
        val broadcastReceiver =
          object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
              trySend(getPinnedShortcuts())
            }
          }

        launcherApps.registerCallback(launcherAppsCallback)
        ContextCompat.registerReceiver(
          context,
          broadcastReceiver,
          IntentFilter().apply {
            addAction(ACTION_PINNED_SHORTCUTS_CHANGED)

            addAction(Intent.ACTION_MANAGED_PROFILE_ADDED)
            addAction(Intent.ACTION_MANAGED_PROFILE_REMOVED)
            addAction(Intent.ACTION_MANAGED_PROFILE_UNAVAILABLE)
            addAction(Intent.ACTION_MANAGED_PROFILE_UNLOCKED)
          },
          ContextCompat.RECEIVER_NOT_EXPORTED,
        )

        awaitClose {
          launcherApps.unregisterCallback(launcherAppsCallback)
          context.unregisterReceiver(broadcastReceiver)
        }
      }
      .stateIn(MainScope(), SharingStarted.WhileSubscribed(replayExpirationMillis = 0), listOf())

  private fun getPinnedShortcuts(): List<UserShortcut> =
    launcherApps.profiles
      .flatMap {
        try {
          launcherApps.getShortcuts(it) { setQueryFlags(ShortcutQuery.FLAG_MATCH_PINNED) }
        } catch (exception: Exception) {
          listOf()
        }
      }
      .map { infoToUserShortcut(it) }

  fun getShortcuts(userComponent: UserComponent): List<UserShortcut> =
    launcherApps
      .getShortcuts(profileManager.getUserHandle(userComponent.profile)!!) {
        setQueryFlags(
          ShortcutQuery.FLAG_MATCH_DYNAMIC or
            ShortcutQuery.FLAG_MATCH_MANIFEST or
            ShortcutQuery.FLAG_MATCH_PINNED
        )
        setPackage(userComponent.packageName)
      }
      .map { infoToUserShortcut(it) }

  fun launchShortcut(userShortcut: UserShortcut, sourceBounds: Rect, startActivityOptions: Bundle) {
    launcherApps.startShortcut(
      userShortcut.packageName,
      userShortcut.shortcutId,
      sourceBounds,
      startActivityOptions,
      profileManager.getUserHandle(userShortcut.profile)!!,
    )
  }

  fun getShortcutCreators(userComponent: UserComponent): List<UserShortcutCreator> =
    launcherApps
      .getConfigurableShortcuts(
        userComponent.packageName,
        profileManager.getUserHandle(userComponent.profile)!!,
      )
      .map { UserShortcutCreator(it.componentName, profileManager.getProfile(it.user)) }

  fun getShortcutCreatorIntent(userShortcutCreator: UserShortcutCreator): IntentSender =
    checkNotNull(
      launcherApps.getShortcutConfigActivityIntent(
        launcherApps.resolveConfigurableShortcut(userShortcutCreator)
      )
    )

  fun pinShortcut(userShortcut: UserShortcut, isPinned: Boolean) {
    val currentPinnedShortcuts =
      launcherApps
        .getShortcuts(profileManager.getUserHandle(userShortcut.profile)!!) {
          setQueryFlags(ShortcutQuery.FLAG_MATCH_PINNED)
          setPackage(userShortcut.packageName)
        }
        .map { it.id }

    val newPinnedShortcuts =
      if (isPinned) {
        currentPinnedShortcuts + userShortcut.shortcutId
      } else {
        currentPinnedShortcuts - userShortcut.shortcutId
      }

    launcherApps.pinShortcuts(
      userShortcut.packageName,
      newPinnedShortcuts,
      profileManager.getUserHandle(userShortcut.profile)!!,
    )
    context.sendBroadcast(Intent(ACTION_PINNED_SHORTCUTS_CHANGED).setPackage(context.packageName))
  }

  fun acceptPinRequest(intent: Intent) {
    val request = launcherApps.getPinItemRequest(intent) ?: return
    if (request.isValid && request.requestType == PinItemRequest.REQUEST_TYPE_SHORTCUT) {
      val shortcutInfo = request.shortcutInfo ?: return
      pinShortcut(infoToUserShortcut(shortcutInfo), isPinned = true)
      request.accept()
    }
  }

  private fun infoToUserShortcut(shortcutInfo: ShortcutInfo): UserShortcut =
    UserShortcut(
      shortcutInfo.`package`,
      shortcutInfo.id,
      profileManager.getProfile(shortcutInfo.userHandle),
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

  private fun LauncherApps.getConfigurableShortcuts(
    packageName: String,
    userHandle: UserHandle,
  ): List<LauncherActivityInfo> =
    if (hasShortcutHostPermission()) {
      getShortcutConfigActivityList(packageName, userHandle)
    } else {
      listOf()
    }

  private fun LauncherApps.resolveConfigurableShortcut(
    shortcutData: UserShortcutCreator
  ): LauncherActivityInfo =
    getConfigurableShortcuts(
        shortcutData.componentName.packageName,
        profileManager.getUserHandle(shortcutData.profile)!!,
      )
      .first { it.componentName == shortcutData.componentName }

  companion object {
    const val ACTION_PINNED_SHORTCUTS_CHANGED = "link.danb.launcher.ACTION_PINNED_SHORTCUTS_CHANGED"
  }
}
