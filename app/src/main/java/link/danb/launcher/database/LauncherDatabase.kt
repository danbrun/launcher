package link.danb.launcher.database

import android.app.Application
import androidx.room.*
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import org.json.JSONArray
import javax.inject.Singleton

@Database(entities = [ActivityMetadata::class, WidgetMetadata::class], version = 3)
@TypeConverters(StringSetConverter::class)
abstract class LauncherDatabase : RoomDatabase() {

    abstract fun launcherActivityMetadata(): ActivityMetadata.DataAccessObject

    abstract fun widgetMetadata(): WidgetMetadata.DataAccessObject

    @Module
    @InstallIn(SingletonComponent::class)
    class LauncherDatabaseModule {

        @Provides
        @Singleton
        fun getDatabase(application: Application): LauncherDatabase {
            return Room.databaseBuilder(
                application, LauncherDatabase::class.java, DATABASE_NAME
            ).fallbackToDestructiveMigration().build()
        }
    }

    companion object {
        private const val DATABASE_NAME = "launcher_database"
    }
}

class StringSetConverter {
    @TypeConverter
    fun toJson(stringSet: Set<String>): String {
        return JSONArray(stringSet).toString()
    }

    @TypeConverter
    fun toStringSet(json: String): Set<String> {
        return buildSet {
            val array = JSONArray(json)
            for (i in 0 until array.length()) {
                add(array.getString(i))
            }
        }
    }
}
