package link.danb.launcher.model

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Upsert

@Entity
data class WidgetMetadata(@PrimaryKey val widgetId: Int, val position: Int, val height: Int) {

    @Dao
    interface DataAccessObject {
        @Query("SELECT * FROM WidgetMetadata")
        fun get(): List<WidgetMetadata>

        @Query("SELECT * FROM WidgetMetadata WHERE widgetId = :widgetId")
        fun get(widgetId: Int): WidgetMetadata?

        @Upsert
        fun put(widgetMetadata: WidgetMetadata)

        @Delete
        fun delete(widgetMetadata: WidgetMetadata)
    }
}
