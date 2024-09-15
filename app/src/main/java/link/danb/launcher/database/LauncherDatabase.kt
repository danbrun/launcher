package link.danb.launcher.database

import android.app.Application
import androidx.room.*
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton
import link.danb.launcher.database.migrations.DeleteActivityDataTagsColumn
import link.danb.launcher.database.migrations.MigrateUserHandleToProfile

@Database(
  entities = [ActivityData::class, WidgetData::class],
  version = 8,
  autoMigrations =
    [
      AutoMigration(from = 1, to = 6),
      AutoMigration(from = 6, to = 7, spec = DeleteActivityDataTagsColumn::class),
    ],
)
@TypeConverters(ComponentNameConverter::class, StringSetConverter::class)
abstract class LauncherDatabase : RoomDatabase() {

  abstract fun activityData(): ActivityData.DataAccessObject

  abstract fun widgetData(): WidgetData.DataAccessObject

  @Module
  @InstallIn(SingletonComponent::class)
  class LauncherDatabaseModule {

    @Provides
    @Singleton
    fun getDatabase(application: Application): LauncherDatabase =
      Room.databaseBuilder(application, LauncherDatabase::class.java, DATABASE_NAME)
        .addMigrations(MigrateUserHandleToProfile(application))
        .build()
  }

  companion object {
    private const val DATABASE_NAME = "launcher_database"
  }
}
