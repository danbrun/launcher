package link.danb.launcher.activities

import android.content.pm.LauncherActivityInfo
import android.graphics.drawable.Drawable
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import com.google.android.material.button.MaterialButton
import link.danb.launcher.R
import link.danb.launcher.ui.ViewBinder
import link.danb.launcher.ui.ViewItem
import link.danb.launcher.utils.inflate
import link.danb.launcher.utils.applySize

class ActivityHeaderViewBinder(
    fragment: Fragment, private val activityHeaderListener: ActivityHeaderListener? = null
) : ViewBinder<ActivityHeaderViewHolder, ActivityHeaderViewItem> {

    private val activitiesViewModel: ActivitiesViewModel by fragment.activityViewModels()

    override val viewType: Int = R.id.activity_header_view_type_id

    override fun createViewHolder(parent: ViewGroup): ActivityHeaderViewHolder {
        return ActivityHeaderViewHolder(
            parent.inflate(R.layout.activity_details_header_view)
        )
    }

    override fun bindViewHolder(
        holder: ActivityHeaderViewHolder, viewItem: ActivityHeaderViewItem
    ) {
        holder.activityItem.apply {
            text = viewItem.name
            viewItem.icon.applySize(
                context.resources.getDimensionPixelSize(R.dimen.launcher_icon_size)
            )
            setCompoundDrawables(viewItem.icon, null, null, null)
        }

        holder.visibilityButton.apply {
            setIconResource(
                if (activitiesViewModel.isVisible(viewItem.info)) {
                    R.drawable.ic_baseline_visibility_off_24
                } else {
                    R.drawable.ic_baseline_visibility_24
                }
            )
            setOnClickListener {
                activityHeaderListener?.onVisibilityButtonClick(it, viewItem)
            }
        }

        holder.uninstallButton.setOnClickListener {
            activityHeaderListener?.onUninstallButtonClick(it, viewItem)
        }

        holder.settingsButton.setOnClickListener {
            activityHeaderListener?.onSettingsButtonClick(it, viewItem)
        }
    }
}

class ActivityHeaderViewHolder(view: View) : ViewHolder(view) {
    val activityItem: TextView = view.findViewById(R.id.activity_item)
    val visibilityButton: MaterialButton = view.findViewById(R.id.visibility_button)
    val uninstallButton: MaterialButton = view.findViewById(R.id.uninstall_button)
    val settingsButton: MaterialButton = view.findViewById(R.id.settings_button)
}

class ActivityHeaderViewItem(val info: LauncherActivityInfo, lazyIcon: Lazy<Drawable>) : ViewItem {

    override val viewType: Int = R.id.activity_header_view_type_id

    val name: CharSequence
        get() = info.label

    val icon: Drawable by lazyIcon

    override fun areItemsTheSame(other: ViewItem): Boolean =
        other is ActivityHeaderViewItem && info.componentName == other.info.componentName && info.user == other.info.user

    override fun areContentsTheSame(other: ViewItem): Boolean =
        other is ActivityHeaderViewItem && name == other.name && icon == other.icon
}

interface ActivityHeaderListener {
    fun onVisibilityButtonClick(view: View, viewItem: ActivityHeaderViewItem)
    fun onUninstallButtonClick(view: View, viewItem: ActivityHeaderViewItem)
    fun onSettingsButtonClick(view: View, viewItem: ActivityHeaderViewItem)
}
