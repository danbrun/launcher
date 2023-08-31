package link.danb.launcher.extensions

import android.os.Build
import android.os.Bundle
import android.os.Parcelable

/**
 * Convenience method to get a [Parcelable] from a [Bundle] using the appropriate API.
 *
 * The underlying method was deprecated in SDK 33 but we will continue using it until SDK 34 due to
 * [implementation bugs in SDK 33](https://issuetracker.google.com/issues/240585930#comment6).
 */
inline fun <reified T> Bundle.getParcelableCompat(key: String): T? =
  when {
    Build.VERSION.SDK_INT > Build.VERSION_CODES.TIRAMISU -> getParcelable(key, T::class.java)
    else -> @Suppress("DEPRECATION") getParcelable(key) as T?
  }
