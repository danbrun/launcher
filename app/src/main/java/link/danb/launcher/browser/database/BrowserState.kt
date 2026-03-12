package link.danb.launcher.browser.database

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.ForeignKey.Companion.CASCADE
import androidx.room.PrimaryKey
import androidx.room.TypeConverters
import org.mozilla.geckoview.GeckoSession

@Entity(
    foreignKeys =
        [ForeignKey(BrowserState::class, arrayOf("tabId"), arrayOf("tabId"), onDelete = CASCADE)]
)
@TypeConverters(SessionStateTypeConverter::class)
class BrowserState(
    @PrimaryKey val tabId: Int,
    val state: GeckoSession.SessionState,
)
