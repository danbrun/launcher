package link.danb.launcher.database.migrations

import android.app.Application
import android.os.Process
import android.os.UserManager
import androidx.core.content.getSystemService
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.transaction

class MigrateUserHandleToProfile(private val application: Application) : Migration(7, 8) {
  override fun migrate(db: SupportSQLiteDatabase) {
    val userManager: UserManager = checkNotNull(application.getSystemService())
    val personalUserHandle = userManager.getSerialNumberForUser(Process.myUserHandle())

    db.transaction {
      /*
        This column is part of the primary key and therefore must be unique, so here I'm doing 2
        migrations to ensure the old UserHandle serial values don't conflict with the new Profile
        enum values. There's probably a better way to do this but I don't want to learn SQLite.
      */

      // Get values not currently used in the table
      val cursor = query("SELECT userHandle FROM ActivityData")
      val userHandleColumn = cursor.getColumnIndex("ActivityData")
      val userHandles = mutableSetOf<Int>()
      cursor.use {
        if (it.moveToFirst()) {
          while (cursor.moveToNext()) {
            userHandles.add(cursor.getInt(userHandleColumn))
          }
        }
      }
      val safePersonalValue = userHandles.max() + 1
      val safeWorkValue = safePersonalValue + 1

      // Migrate to safe values
      execSQL(
        "UPDATE ActivityData SET userHandle = ? WHERE userHandle = ?",
        arrayOf(safePersonalValue, personalUserHandle),
      )
      execSQL(
        "UPDATE ActivityData SET userHandle = ? WHERE userHandle != ?",
        arrayOf(safeWorkValue, personalUserHandle),
      )

      // Migrate to final values
      execSQL(
        "UPDATE ActivityData SET userHandle = ? WHERE userHandle = ?",
        arrayOf(1, safePersonalValue),
      )
      execSQL(
        "UPDATE ActivityData SET userHandle = ? WHERE userHandle = ?",
        arrayOf(2, safeWorkValue),
      )

      // Rename the column
      execSQL("ALTER TABLE ActivityData RENAME userHandle TO profile")
    }
  }
}
