package link.danb.launcher.data

import android.content.ComponentName
import android.os.Parcelable
import android.os.UserHandle
import kotlinx.parcelize.Parcelize

@Parcelize
data class UserActivity(val componentName: ComponentName, override val userHandle: UserHandle) :
  UserComponent, Parcelable
