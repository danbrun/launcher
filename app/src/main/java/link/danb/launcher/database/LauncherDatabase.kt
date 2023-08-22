package link.danb.launcher.database

import android.app.Application
import android.content.ComponentName
import android.os.UserHandle
import android.os.UserManager
import androidx.room.*
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import org.json.JSONArray
import javax.inject.Inject
import javax.inject.Singleton

@Database(entities = [ActivityMetadata::class, WidgetMetadata::class], version = 4)
@TypeConverters(
    ComponentNameConverter::class, UserHandleConverter::class, StringSetConverter::class
)
abstract class LauncherDatabase : RoomDatabase() {

    abstract fun launcherActivityMetadata(): ActivityMetadata.DataAccessObject

    abstract fun widgetMetadata(): WidgetMetadata.DataAccessObject

    @Module
    @InstallIn(SingletonComponent::class)
    class LauncherDatabaseModule {

        @Provides
        @Singleton
        fun getDatabase(
            application: Application, userHandleConverter: UserHandleConverter
        ): LauncherDatabase {
            return Room.databaseBuilder(
                application, LauncherDatabase::class.java, DATABASE_NAME
            ).addTypeConverter(userHandleConverter).fallbackToDestructiveMigration().build()
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

class ComponentNameConverter {
    @TypeConverter
    fun toString(componentName: ComponentName) = componentName.flattenToString()

    @TypeConverter
    fun toComponentName(string: String) = ComponentName.unflattenFromString(string)
}

@ProvidedTypeConverter
@Singleton
class UserHandleConverter @Inject constructor(private val userManager: UserManager) {
    @TypeConverter
    fun toLong(userHandle: UserHandle): Long = userManager.getSerialNumberForUser(userHandle)

    @TypeConverter
    fun toUserHandle(long: Long): UserHandle = userManager.getUserForSerialNumber(long)
}
