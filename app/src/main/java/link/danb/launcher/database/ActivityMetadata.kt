package link.danb.launcher.database

import android.content.ComponentName
import android.os.UserHandle
import androidx.room.*

@Entity(primaryKeys = ["componentName", "userHandle"])
data class ActivityMetadata(
    val componentName: ComponentName,
    val userHandle: UserHandle,
    val isHidden: Boolean,
    val tags: Set<String>,
) {
    @Dao
    interface DataAccessObject {
        @Query("SELECT * FROM ActivityMetadata")
        fun get(): List<ActivityMetadata>

        @Query("SELECT * FROM ActivityMetadata WHERE componentName = :componentName AND userHandle = :userHandle")
        fun get(componentName: ComponentName, userHandle: UserHandle): ActivityMetadata?

        @Upsert
        fun put(vararg activityMetadata: ActivityMetadata)

        @Delete
        fun delete(activityMetadata: ActivityMetadata)
    }
}
