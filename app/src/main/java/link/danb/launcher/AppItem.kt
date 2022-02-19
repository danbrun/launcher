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
import androidx.recyclerview.widget.RecyclerView

typealias AppItemClickListener = (appItem: AppItem, view: View) -> Unit

data class AppItem(
    val componentName: ComponentName,
    val userHandle: UserHandle,
    val label: String,
    val getIcon: () -> Drawable?
) {
    class Adapter : androidx.recyclerview.widget.ListAdapter<AppItem, Adapter.ViewHolder>(DIFF) {

        private var _onClickListener: AppItemClickListener? = null
        private var _onLongClickListener: AppItemClickListener? = null

        fun setOnClickListener(onClickListener: AppItemClickListener) {
            _onClickListener = onClickListener
        }

        fun setOnLongClickListener(onLongClickListener: AppItemClickListener) {
            _onLongClickListener = onLongClickListener
        }

        inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {

            private val textView: TextView = view.findViewById(R.id.app_item)

            fun bindTo(appItem: AppItem) {
                textView.apply {
                    val size = context.resources.getDimension(R.dimen.launcher_icon_size).toInt()
                    val icon = appItem.getIcon()?.apply {
                        setBounds(0, 0, size, size)
                    }

                    text = appItem.label
                    setCompoundDrawables(icon, null, null, null)
                    setOnClickListener { _onClickListener?.invoke(appItem, textView) }
                    setOnLongClickListener {
                        _onLongClickListener?.invoke(appItem, textView)
                        true
                    }
                }
            }
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            return ViewHolder(
                LayoutInflater.from(parent.context).inflate(R.layout.app_item, parent, false)
            )
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            holder.bindTo(getItem(position))
        }

        companion object {
            val DIFF = object : DiffUtil.ItemCallback<AppItem>() {
                override fun areItemsTheSame(oldItem: AppItem, newItem: AppItem): Boolean =
                    oldItem.componentName == newItem.componentName
                            && oldItem.userHandle == newItem.userHandle

                // Icons are compared by reference since the ActivityIconViewModel caches rendered
                // bitmaps and should return the same value each time.
                @SuppressLint("DiffUtilEquals")
                override fun areContentsTheSame(oldItem: AppItem, newItem: AppItem): Boolean =
                    oldItem.label == newItem.label && oldItem.getIcon === newItem.getIcon
            }
        }
    }
}
