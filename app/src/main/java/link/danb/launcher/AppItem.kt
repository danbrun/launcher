package link.danb.launcher

import android.content.pm.LauncherActivityInfo
import android.graphics.Rect
import android.graphics.drawable.Drawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView

typealias AppItemClickListener = (appItem: AppItem, bounds: Rect) -> Unit

data class AppItem(val info: LauncherActivityInfo) {

    val name: String = info.label as String
    val icon: Drawable = info.getBadgedIcon(0)

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

            private fun getBounds(): Rect {
                val position = IntArray(2).apply { textView.getLocationOnScreen(this) }
                return Rect(
                    position[0], position[1], textView.measuredWidth, textView.measuredHeight
                )
            }

            fun bindTo(appItem: AppItem) {
                textView.apply {
                    text = appItem.name
                    setCompoundDrawables(
                        appItem.icon.apply {
                            val size = (48f * context.resources.displayMetrics.density).toInt()
                            setBounds(0, 0, size, size)
                        },
                        null,
                        null,
                        null
                    )
                    setOnClickListener { _onClickListener?.invoke(appItem, getBounds()) }
                    setOnLongClickListener {
                        _onLongClickListener?.invoke(appItem, getBounds())
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
                    oldItem.info.componentName == newItem.info.componentName
                            && oldItem.info.user == newItem.info.user

                override fun areContentsTheSame(oldItem: AppItem, newItem: AppItem): Boolean = false
            }
        }
    }
}