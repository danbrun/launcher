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
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView

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

    class Adapter : androidx.recyclerview.widget.ListAdapter<AppItem, Adapter.ViewHolder>(DIFF) {

        class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            private val textView: TextView = view.findViewById(R.id.app_item)

            fun bindTo(appItem: AppItem) {
                textView.apply {
                    text = appItem.name.value
                    setCompoundDrawables(appItem.getScaledIcon(context), null, null, null)
                    setOnClickListener { appItem.launch(context) }
                    setOnLongClickListener { appItem.uninstall(context); true }
                }
            }
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            return ViewHolder(
                LayoutInflater.from(parent.context).inflate(R.layout.app_item, parent, false))
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            holder.bindTo(getItem(position))
        }

        companion object {
            val DIFF = object : DiffUtil.ItemCallback<AppItem>() {
                override fun areItemsTheSame(oldItem: AppItem, newItem: AppItem): Boolean =
                    oldItem === newItem

                override fun areContentsTheSame(oldItem: AppItem, newItem: AppItem): Boolean =
                    oldItem == newItem
            }
        }
    }
}