package link.danb.launcher

import android.content.Context
import android.content.Intent
import android.content.pm.LauncherActivityInfo
import android.content.pm.LauncherApps
import android.graphics.Rect
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Bundle
import android.os.UserHandle
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.view.children
import androidx.recyclerview.widget.RecyclerView
import kotlin.math.ceil

data class AppItem(val user: UserHandle, val info: LauncherActivityInfo) {

    val name: Lazy<String> = lazy { info.label as String }
    private val icon: Lazy<Drawable> = lazy { info.getBadgedIcon(0) }

    fun getScaledIcon(context: Context): Drawable {
        return icon.value.apply {
            val size =
                TypedValue
                    .applyDimension(
                        TypedValue.COMPLEX_UNIT_DIP,
                        48f,
                        context.resources.displayMetrics
                    )
                    .toInt()
            setBounds(0, 0, size, size)
        }
    }

    fun launch(context: Context) {
        (context.getSystemService(Context.LAUNCHER_APPS_SERVICE) as LauncherApps)
            .startMainActivity(
                info.componentName,
                info.user,
                Rect(0, 0, 0, 0),
                Bundle()
            )
    }

    fun uninstall(context: Context) {
        context.startActivity(
            Intent(
                Intent.ACTION_DELETE,
                Uri.fromParts("package", info.applicationInfo.packageName, null)
            )
        )
    }

    class Adapter(
        private val appList: List<AppItem>,
        private val appsPerRow: Int
    ) : RecyclerView.Adapter<Adapter.ViewHolder>() {

        class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val children = (view as LinearLayout).children.map { it as TextView }
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view =
                LayoutInflater
                    .from(parent.context)
                    .inflate(R.layout.app_row, parent, false) as LinearLayout

            (0 until appsPerRow).map {
                LayoutInflater
                    .from(parent.context)
                    .inflate(R.layout.app_item, view, false)
                    .apply { view.addView(this) }
                    .findViewById(R.id.app_item) as TextView
            }

            return ViewHolder(view)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            holder.children.forEachIndexed { index, view ->
                val appIndex = position * appsPerRow + index

                if (appIndex < appList.size) {
                    setAppItem(view, appList[appIndex])
                } else {
                    unsetAppItem(view)
                }
            }
        }

        override fun getItemCount() = ceil(appList.size.toDouble() / appsPerRow).toInt()

        private fun setAppItem(view: TextView, data: AppItem) {
            view.apply {
                isEnabled = true
                text = data.name.value
                setCompoundDrawables(data.getScaledIcon(context), null, null, null)
                setOnClickListener { data.launch(context) }
                setOnLongClickListener { data.uninstall(context); true }
            }
        }

        private fun unsetAppItem(view: TextView) {
            view.apply {
                isEnabled = false
                text = null
                setCompoundDrawables(null, null, null, null)
                setOnClickListener(null)
                setOnLongClickListener(null)
            }
        }
    }
}