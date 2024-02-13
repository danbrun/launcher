package link.danb.launcher.database

import androidx.room.ColumnInfo
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow
import link.danb.launcher.data.UserActivity

@Entity
data class ActivityData(
  @PrimaryKey @Embedded val userActivity: UserActivity,
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
