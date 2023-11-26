package link.danb.launcher.database

import android.content.ComponentName
import android.os.UserHandle
import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Entity(primaryKeys = ["componentName", "userHandle"])
data class ActivityData(
  val componentName: ComponentName,
  val userHandle: UserHandle,
  @ColumnInfo(defaultValue = "0") val isPinned: Boolean,
  val isHidden: Boolean,
  val tags: Set<String>,
) {

  @Dao
  interface DataAccessObject {
    @Query("SELECT * FROM ActivityData") fun get(): Flow<List<ActivityData>>

    @Upsert suspend fun put(vararg activityMetadata: ActivityData)

    @Delete suspend fun delete(vararg activityData: ActivityData)
  }
}
