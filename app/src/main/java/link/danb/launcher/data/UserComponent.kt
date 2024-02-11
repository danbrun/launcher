package link.danb.launcher.data

import android.content.ComponentName
import android.os.Parcelable
import android.os.UserHandle
import kotlinx.parcelize.Parcelize

@Parcelize
data class UserComponent(val componentName: ComponentName, val userHandle: UserHandle) : Parcelable
