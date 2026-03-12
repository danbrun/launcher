package link.danb.launcher.browser.database

import android.database.Cursor
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Query
import androidx.room.Update
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface BrowserTabDao {

  @Query("SELECT * FROM BrowserTab") fun getAll(): Flow<List<BrowserTab>>

  @Query("SELECT * FROM BrowserTab") fun getContentProviderCursor(): Cursor

  @Query("SELECT * FROM BrowserTab WHERE tabId = :tabId") suspend fun get(tabId: Int): BrowserTab?

  @Query("SELECT NOT EXISTS (SELECT * FROM BrowserTab)") suspend fun isEmpty(): Boolean

  @Query("DELETE FROM BrowserTab WHERE tabId = :tabId") suspend fun delete(tabId: Int)

  @Upsert suspend fun upsert(tab: BrowserTab): Long

  @Delete suspend fun delete(tab: BrowserTab)

  @Update(BrowserTab::class) suspend fun update(title: BrowserTitle)

  @Update(BrowserTab::class) suspend fun update(url: BrowserUrl)

  @Update(BrowserTab::class) suspend fun update(progress: BrowserProgress)

  @Update(BrowserTab::class) suspend fun update(progress: BrowserBitmap)
}
