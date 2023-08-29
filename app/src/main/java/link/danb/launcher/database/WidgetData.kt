package link.danb.launcher.database

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Upsert

@Entity
data class WidgetData(@PrimaryKey val widgetId: Int, val position: Int, val height: Int) {

    @Dao
    interface DataAccessObject {
        @Query("SELECT * FROM WidgetData")
        fun get(): List<WidgetData>

        @Query("SELECT * FROM WidgetData WHERE widgetId = :widgetId")
        fun get(widgetId: Int): WidgetData?

        @Upsert
        fun put(vararg widgetData: WidgetData)

        @Delete
        fun delete(vararg widgetData: WidgetData)
    }
}
