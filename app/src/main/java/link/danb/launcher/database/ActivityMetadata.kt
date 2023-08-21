package link.danb.launcher.database

import android.content.pm.LauncherActivityInfo
import android.os.Process.myUserHandle
import androidx.room.*

@Entity(primaryKeys = ["packageName", "className", "isMainUser"])
data class ActivityMetadata(
    val packageName: String,
    val className: String,
    val isMainUser: Boolean,
    val isHidden: Boolean,
    val tags: Set<String>
) {
    @Dao
    interface DataAccessObject {
        @Query("SELECT * FROM ActivityMetadata")
        fun get(): List<ActivityMetadata>

        @Query(
            "SELECT * FROM ActivityMetadata WHERE packageName = :packageName AND className = :className AND isMainUser = :isMainUser"
        )
        fun get(
            packageName: String, className: String, isMainUser: Boolean
        ): ActivityMetadata?

        @Upsert
        fun put(vararg activityMetadata: ActivityMetadata)

        @Delete
        fun delete(activityMetadata: ActivityMetadata)

        fun get(info: LauncherActivityInfo): ActivityMetadata = get(
            info.componentName.packageName,
            info.componentName.className,
            info.user == myUserHandle()
        ) ?: ActivityMetadata(
            info.componentName.packageName,
            info.componentName.className,
            info.user == myUserHandle(),
            isHidden = false,
            setOf()
        )
    }
}
