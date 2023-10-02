package link.danb.launcher.activities

import android.graphics.drawable.Drawable
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import com.google.android.material.button.MaterialButton
import link.danb.launcher.R
import link.danb.launcher.database.ActivityData
import link.danb.launcher.extensions.inflate
import link.danb.launcher.extensions.setSize
import link.danb.launcher.ui.ViewBinder
import link.danb.launcher.ui.ViewItem

class ActivityHeaderViewBinder(
  private val onVisibilityButtonClick: ((view: View, viewItem: ActivityHeaderViewItem) -> Unit)?,
  private val onUninstallButtonClick: ((view: View, viewItem: ActivityHeaderViewItem) -> Unit)?,
  private val onSettingsButtonClick: ((view: View, viewItem: ActivityHeaderViewItem) -> Unit)?,
) : ViewBinder<ActivityHeaderViewHolder, ActivityHeaderViewItem> {

  override val viewType: Int = R.id.activity_header_view_type_id

  override fun createViewHolder(parent: ViewGroup): ActivityHeaderViewHolder =
    ActivityHeaderViewHolder(parent.inflate(R.layout.activity_details_header_view))

  override fun bindViewHolder(holder: ActivityHeaderViewHolder, viewItem: ActivityHeaderViewItem) {
    holder.activityItem.apply {
      text = viewItem.name
      viewItem.icon.setSize(context.resources.getDimensionPixelSize(R.dimen.launcher_icon_size))
      setCompoundDrawables(viewItem.icon, null, null, null)
    }

    holder.visibilityButton.apply {
      setIconResource(
        if (!viewItem.data.isHidden) {
          R.drawable.ic_baseline_visibility_off_24
        } else {
          R.drawable.ic_baseline_visibility_24
        }
      )
      setOnClickListener { onVisibilityButtonClick?.invoke(it, viewItem) }
    }

    holder.uninstallButton.setOnClickListener { onUninstallButtonClick?.invoke(it, viewItem) }

    holder.settingsButton.setOnClickListener { onSettingsButtonClick?.invoke(it, viewItem) }
  }
}

class ActivityHeaderViewHolder(view: View) : ViewHolder(view) {
  val activityItem: TextView = view.findViewById(R.id.activity_item)
  val visibilityButton: MaterialButton = view.findViewById(R.id.visibility_button)
  val uninstallButton: MaterialButton = view.findViewById(R.id.uninstall_button)
  val settingsButton: MaterialButton = view.findViewById(R.id.settings_button)
}

class ActivityHeaderViewItem(val data: ActivityData, val icon: Drawable, val name: CharSequence) :
  ViewItem {

  override val viewType: Int = R.id.activity_header_view_type_id

  override fun areItemsTheSame(other: ViewItem): Boolean =
    other is ActivityHeaderViewItem &&
      data.componentName == other.data.componentName &&
      data.userHandle == other.data.userHandle

  override fun areContentsTheSame(other: ViewItem): Boolean =
    other is ActivityHeaderViewItem && name == other.name && icon == other.icon
}
