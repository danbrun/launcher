package link.danb.launcher.database.migrations

import androidx.room.DeleteColumn
import androidx.room.migration.AutoMigrationSpec

@DeleteColumn(tableName = "ActivityData", columnName = "tags")
class DeleteActivityDataTagsColumn : AutoMigrationSpec
