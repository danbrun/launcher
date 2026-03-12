package link.danb.launcher.browser.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Database(entities = [BrowserState::class, BrowserTab::class], version = 1)
abstract class BrowserDatabase : RoomDatabase() {
  abstract fun browserSessionDao(): BrowserStateDao

  abstract fun browserTabDao(): BrowserTabDao
}

@Module
@InstallIn(SingletonComponent::class)
object BrowserDatabaseModule {

  @Provides
  @Singleton
  fun getBrowserDatabase(@ApplicationContext context: Context): BrowserDatabase =
      Room.databaseBuilder(context, BrowserDatabase::class.java, "browser-database").build()
}
