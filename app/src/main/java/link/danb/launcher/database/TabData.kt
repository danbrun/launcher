package link.danb.launcher.database

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow
import kotlinx.serialization.Serializable

@Entity
@Serializable
data class TabData(
  @PrimaryKey val id: Int,
  val url: String,
  val title: String = url,
  val capture: String? = null,
) {

  @Dao
  interface DataAccessObject {
    @Query("SELECT * FROM TabData") fun get(): Flow<List<TabData>>

    @Upsert suspend fun put(vararg tabData: TabData)

    @Query("DELETE FROM TabData WHERE id=:id") suspend fun delete(id: Int)
  }
}
