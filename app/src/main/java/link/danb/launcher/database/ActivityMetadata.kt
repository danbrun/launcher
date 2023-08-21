package link.danb.launcher.database

import android.content.ComponentName
import android.content.pm.LauncherActivityInfo
import android.os.Process.myUserHandle
import android.os.UserHandle
import androidx.room.*

@Entity(primaryKeys = ["packageName", "className", "isMainUser"])
data class ActivityMetadata(
    val packageName: String, val className: String, val isMainUser: Boolean, val tags: Set<String>
) {
    constructor(
        component: ComponentName, user: UserHandle, tags: Set<String>
    ) : this(component.packageName, component.className, user == myUserHandle(), tags)

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

        fun get(info: LauncherActivityInfo): ActivityMetadata {
            return get(
                info.componentName.packageName,
                info.componentName.className,
                info.user == myUserHandle()
            ) ?: ActivityMetadata(
                info.componentName.packageName,
                info.componentName.className,
                info.user == myUserHandle(),
                setOf()
            )
        }
    }
}
