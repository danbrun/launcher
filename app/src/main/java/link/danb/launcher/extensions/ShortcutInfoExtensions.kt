package link.danb.launcher.extensions

import android.content.pm.ShortcutInfo

val ShortcutInfo.packageName: String
    get() = this.`package`
