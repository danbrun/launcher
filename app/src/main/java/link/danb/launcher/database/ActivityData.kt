package link.danb.launcher.database

import android.content.ComponentName
import android.os.UserHandle
import androidx.room.*

@Entity(primaryKeys = ["componentName", "userHandle"])
data class ActivityData(
    val componentName: ComponentName,
    val userHandle: UserHandle,
    val isHidden: Boolean,
    val tags: Set<String>,
) {
    @Dao
    interface DataAccessObject {
        @Query("SELECT * FROM ActivityData")
        fun get(): List<ActivityData>

        @Query("SELECT * FROM ActivityData WHERE componentName = :componentName AND userHandle = :userHandle")
        fun get(componentName: ComponentName, userHandle: UserHandle): ActivityData?

        @Upsert
        fun put(vararg activityMetadata: ActivityData)

        @Delete
        fun delete(vararg activityData: ActivityData)
    }
}
