package link.danb.launcher.model

import android.content.ComponentName
import android.content.pm.LauncherActivityInfo
import android.os.Process.myUserHandle
import android.os.UserHandle
import androidx.room.*

@Entity(primaryKeys = ["packageName", "className", "isMainUser"])
data class LauncherActivityMetadata(
    val packageName: String, val className: String, val isMainUser: Boolean, val tags: Set<String>
) {
    constructor(
        component: ComponentName, user: UserHandle, tags: Set<String>
    ) : this(component.packageName, component.className, user == myUserHandle(), tags)

    @Dao
    interface DataAccessObject {
        @Query("SELECT * FROM LauncherActivityMetadata")
        fun get(): List<LauncherActivityMetadata>

        @Query(
            "SELECT * FROM LauncherActivityMetadata WHERE packageName = :packageName AND className = :className AND isMainUser = :isMainUser"
        )
        fun get(
            packageName: String, className: String, isMainUser: Boolean
        ): LauncherActivityMetadata?

        @Upsert
        fun put(vararg launcherActivityMetadata: LauncherActivityMetadata)

        @Delete
        fun delete(launcherActivityMetadata: LauncherActivityMetadata)

        fun get(info: LauncherActivityInfo): LauncherActivityMetadata {
            return get(
                info.componentName.packageName,
                info.componentName.className,
                info.user == myUserHandle()
            ) ?: LauncherActivityMetadata(
                info.componentName.packageName,
                info.componentName.className,
                info.user == myUserHandle(),
                setOf()
            )
        }
    }
}
