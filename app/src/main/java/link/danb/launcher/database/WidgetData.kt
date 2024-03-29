package link.danb.launcher.database

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Entity
data class WidgetData(@PrimaryKey val widgetId: Int, val position: Int, val height: Int) {

  @Dao
  interface DataAccessObject {
    @Query("SELECT * FROM WidgetData") fun getFlow(): Flow<List<WidgetData>>

    @Query("SELECT * FROM WidgetData") fun get(): List<WidgetData>

    @Query("SELECT * FROM WidgetData WHERE widgetId = :widgetId")
    suspend fun get(widgetId: Int): WidgetData?

    @Upsert suspend fun put(vararg widgetData: WidgetData)

    @Delete suspend fun delete(vararg widgetData: WidgetData)
  }
}
