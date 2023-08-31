package link.danb.launcher.database

import android.content.ComponentName
import androidx.room.TypeConverter

class ComponentNameConverter {
  @TypeConverter fun toString(componentName: ComponentName) = componentName.flattenToString()
  @TypeConverter fun toComponentName(string: String) = ComponentName.unflattenFromString(string)
}
