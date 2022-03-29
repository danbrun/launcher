package link.danb.launcher

import android.annotation.SuppressLint
import android.content.ComponentName
import android.graphics.drawable.Drawable
import android.os.UserHandle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView

/** An activity that can be shown in the launcher with its name and icon. */
data class LauncherIcon(
    val componentName: ComponentName,
    val user: UserHandle,
    val label: String,
    val icon: Drawable
) {
    /** Adapter for launcher icons in recycler views. */
    class Adapter(
        private val onClick: (View, LauncherIcon) -> Unit,
        private val onLongClick: (View, LauncherIcon) -> Unit
    ) : ListAdapter<LauncherIcon, ViewHolder>(DiffUtilItemCallback()) {

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder =
            ViewHolder(
                LayoutInflater.from(parent.context).inflate(R.layout.launcher_icon, parent, false)
            )

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            holder.textView.apply {
                val launcherIcon = getItem(position)
                val size = context.resources.getDimensionPixelSize(R.dimen.launcher_icon_size)
                val icon = launcherIcon.icon.apply { setBounds(0, 0, size, size) }

                text = launcherIcon.label
                setCompoundDrawables(icon, null, null, null)

                setOnClickListener { onClick(it, launcherIcon) }
                setOnLongClickListener { onLongClick(it, launcherIcon); true }
            }
        }
    }

    /** View holder for launch icons in recycler views. */
    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val textView: TextView = view.findViewById(R.id.launcher_icon)
    }

    private class DiffUtilItemCallback : DiffUtil.ItemCallback<LauncherIcon>() {

        override fun areItemsTheSame(oldItem: LauncherIcon, newItem: LauncherIcon): Boolean =
            oldItem.componentName == newItem.componentName && oldItem.user == newItem.user

        @SuppressLint("DiffUtilEquals")
        override fun areContentsTheSame(oldItem: LauncherIcon, newItem: LauncherIcon): Boolean =
            oldItem.label == newItem.label && oldItem.icon == newItem.icon
    }
}
