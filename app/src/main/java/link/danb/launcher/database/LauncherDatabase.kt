package link.danb.launcher.database

import android.app.Application
import androidx.room.*
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
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
