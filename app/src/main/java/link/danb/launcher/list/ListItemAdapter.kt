package link.danb.launcher.list

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import link.danb.launcher.R

class ListItemAdapter(
    private val onClick: ListItemClickHandler?,
    private val onLongClick: ListItemClickHandler?
) : ListAdapter<ListItem, ViewHolder>(diffUtilItemCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(
            LayoutInflater.from(parent.context).inflate(R.layout.list_item, parent, false)
        )
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val listItem = getItem(position)
        val size =
            holder.textView.context.resources.getDimensionPixelSize(R.dimen.launcher_icon_size)
        listItem.icon.apply { setBounds(0, 0, size, size) }

        holder.textView.text = listItem.name
        holder.textView.setCompoundDrawablesRelative(listItem.icon, null, null, null)
        holder.textView.setOnClickListener { onClick?.invoke(it, listItem) }
        holder.textView.setOnLongClickListener { onLongClick?.invoke(it, listItem); true }
    }

    companion object {
        val diffUtilItemCallback = object : DiffUtil.ItemCallback<ListItem>() {
            override fun areItemsTheSame(oldItem: ListItem, newItem: ListItem): Boolean {
                return oldItem.areItemsTheSame(newItem)
            }

            @SuppressLint("DiffUtilEquals")
            override fun areContentsTheSame(oldItem: ListItem, newItem: ListItem): Boolean {
                return oldItem.areContentsTheSame(newItem)
            }
        }
    }
}

class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
    val textView = view as TextView
}

