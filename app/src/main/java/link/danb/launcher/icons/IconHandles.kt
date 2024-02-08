package link.danb.launcher.icons

import android.content.ComponentName
import android.os.UserHandle

sealed interface IconHandle {
  val packageName: String
  val userHandle: UserHandle
}

data class ApplicationHandle(
  override val packageName: String,
  override val userHandle: UserHandle,
) : IconHandle

data class ComponentHandle(val componentName: ComponentName, override val userHandle: UserHandle) :
  IconHandle {
  override val packageName: String
    get() = componentName.packageName
}

data class ShortcutHandle(
  override val packageName: String,
  val shortcutId: String,
  override val userHandle: UserHandle,
) : IconHandle
