package link.danb.launcher

import android.os.Process

data class AppFilter(val name: String, val filterFunction: (AppItem) -> Boolean) {
    companion object {
        val ALL = AppFilter("All") { (_) -> true }
        val PERSONAL = AppFilter("Personal") { (appItem) -> appItem.user == Process.myUserHandle() }
        val WORK = AppFilter("Work") { (appItem) -> appItem.user != Process.myUserHandle() }
    }
}
