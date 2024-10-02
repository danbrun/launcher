package link.danb.launcher.components

import android.net.Uri
import android.os.Bundle
import androidx.core.os.BundleCompat
import androidx.navigation.NavType
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

object UserActivityNavType : NavType<UserActivity>(false) {
  override fun get(bundle: Bundle, key: String): UserActivity? =
    BundleCompat.getParcelable(bundle, key, UserActivity::class.java)

  override fun put(bundle: Bundle, key: String, value: UserActivity) =
    bundle.putParcelable(key, value)

  override fun parseValue(value: String): UserActivity = Json.decodeFromString(value)

  override fun serializeAsValue(value: UserActivity): String =
    Uri.encode(Json.encodeToString(value))
}
