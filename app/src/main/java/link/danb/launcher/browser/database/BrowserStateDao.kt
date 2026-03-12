package link.danb.launcher.browser.database

import androidx.room.Dao
import androidx.room.Query
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import androidx.room.Upsert
import org.mozilla.geckoview.GeckoSession

@Dao
@TypeConverters(SessionStateTypeConverter::class)
interface BrowserStateDao {

  @Query("SELECT * FROM BrowserState WHERE tabId = :tabId") suspend fun get(tabId: Int): BrowserState?

  @Upsert suspend fun upsert(state: BrowserState)
}

class SessionStateTypeConverter {

  @TypeConverter fun toString(value: GeckoSession.SessionState?): String? = value?.toString()

  @TypeConverter
  fun fromString(value: String): GeckoSession.SessionState? =
      GeckoSession.SessionState.fromString(value)
}
