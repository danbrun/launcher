package link.danb.launcher.database

import android.content.ComponentName
import android.os.UserHandle
import androidx.room.*

@Entity(primaryKeys = ["componentName", "userHandle"])
data class ActivityData(
  val componentName: ComponentName,
  val userHandle: UserHandle,
  @ColumnInfo(defaultValue = "0")
  val isPinned: Boolean,
  val isHidden: Boolean,
  val tags: Set<String>,
) {

  @Dao
  interface DataAccessObject {
    @Query("SELECT * FROM ActivityData") fun get(): List<ActivityData>

    @Query(
      "SELECT * FROM ActivityData WHERE componentName = :componentName AND userHandle = :userHandle"
    )
    suspend fun get(componentName: ComponentName, userHandle: UserHandle): ActivityData?

    @Upsert suspend fun put(vararg activityMetadata: ActivityData)

    @Delete suspend fun delete(vararg activityData: ActivityData)
  }
}
