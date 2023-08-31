package link.danb.launcher.database

import android.app.Application
import androidx.room.*
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Database(entities = [ActivityData::class, WidgetData::class], version = 5)
@TypeConverters(
  ComponentNameConverter::class,
  UserHandleConverter::class,
  StringSetConverter::class,
)
abstract class LauncherDatabase : RoomDatabase() {

  abstract fun activityData(): ActivityData.DataAccessObject
  abstract fun widgetData(): WidgetData.DataAccessObject

  @Module
  @InstallIn(SingletonComponent::class)
  class LauncherDatabaseModule {

    @Provides
    @Singleton
    fun getDatabase(
      application: Application,
      userHandleConverter: UserHandleConverter
    ): LauncherDatabase =
      Room.databaseBuilder(application, LauncherDatabase::class.java, DATABASE_NAME)
        .addTypeConverter(userHandleConverter)
        .fallbackToDestructiveMigration()
        .build()
  }

  companion object {
    private const val DATABASE_NAME = "launcher_database"
  }
}
