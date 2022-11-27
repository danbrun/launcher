package link.danb.launcher.list

import android.graphics.drawable.Drawable
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import com.google.android.material.button.MaterialButton
import link.danb.launcher.R
import link.danb.launcher.model.LauncherActivityData
import link.danb.launcher.model.LauncherViewModel
import link.danb.launcher.utils.inflate
import link.danb.launcher.utils.setSize

class ActivityHeaderViewBinder(
    fragment: Fragment,
    private val activityHeaderListener: ActivityHeaderListener? = null
) :
    ViewBinder {
    private val launcherViewModel: LauncherViewModel by fragment.activityViewModels()

    override val viewType: Int = R.id.activity_header_view_type_id

    override fun createViewHolder(parent: ViewGroup): ViewHolder {
        return ActivityHeaderViewHolder(
            parent.inflate(R.layout.activity_details_header_view)
        )
    }

    override fun bindViewHolder(holder: ViewHolder, viewItem: ViewItem) {
        holder as ActivityHeaderViewHolder
        viewItem as ActivityHeaderViewItem

        holder.activityItem.apply {
            text = viewItem.name
            viewItem.icon.setSize(
                context.resources.getDimensionPixelSize(R.dimen.launcher_icon_size)
            )
            setCompoundDrawables(viewItem.icon, null, null, null)
        }

        holder.uninstallButton.setOnClickListener {
            launcherViewModel.uninstall(viewItem.launcherActivityData, it)
            activityHeaderListener?.onUninstallButtonClick(viewItem)
        }

        holder.settingsButton.setOnClickListener {
            launcherViewModel.manage(viewItem.launcherActivityData, it)
            activityHeaderListener?.onSettingsButtonClick(viewItem)
        }
    }

    private class ActivityHeaderViewHolder(view: View) : ViewHolder(view) {
        val activityItem: TextView = view.findViewById(R.id.activity_item)
        val uninstallButton: MaterialButton = view.findViewById(R.id.uninstall_button)
        val settingsButton: MaterialButton = view.findViewById(R.id.settings_button)
    }
}

class ActivityHeaderViewItem(val launcherActivityData: LauncherActivityData) : ViewItem {
    override val viewType: Int = R.id.activity_header_view_type_id

    val name: CharSequence
        get() = launcherActivityData.name

    val icon: Drawable
        get() = launcherActivityData.icon

    override fun areItemsTheSame(other: ViewItem): Boolean {
        return other is ActivityHeaderViewItem
                && launcherActivityData.component == other.launcherActivityData.component
                && launcherActivityData.user == other.launcherActivityData.user
    }

    override fun areContentsTheSame(other: ViewItem): Boolean {
        return other is ActivityHeaderViewItem
                && launcherActivityData.timestamp == other.launcherActivityData.timestamp
    }
}

interface ActivityHeaderListener {
    fun onUninstallButtonClick(activityHeaderViewItem: ActivityHeaderViewItem)
    fun onSettingsButtonClick(activityHeaderViewItem: ActivityHeaderViewItem)
}
