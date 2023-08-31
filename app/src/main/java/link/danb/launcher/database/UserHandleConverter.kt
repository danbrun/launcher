package link.danb.launcher.database

import android.os.UserHandle
import android.os.UserManager
import androidx.room.ProvidedTypeConverter
import androidx.room.TypeConverter
import javax.inject.Inject
import javax.inject.Singleton

@ProvidedTypeConverter
@Singleton
class UserHandleConverter @Inject constructor(private val userManager: UserManager) {
  @TypeConverter
  fun toLong(userHandle: UserHandle): Long = userManager.getSerialNumberForUser(userHandle)

  @TypeConverter fun toUserHandle(long: Long): UserHandle = userManager.getUserForSerialNumber(long)
}
