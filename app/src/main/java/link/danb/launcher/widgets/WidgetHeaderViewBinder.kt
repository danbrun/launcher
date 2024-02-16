package link.danb.launcher.widgets

import android.graphics.drawable.Drawable
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import javax.inject.Inject
import link.danb.launcher.R
import link.danb.launcher.apps.LauncherResourceProvider
import link.danb.launcher.data.UserApplication
import link.danb.launcher.extensions.inflate
import link.danb.launcher.extensions.setSize
import link.danb.launcher.ui.ViewBinder
import link.danb.launcher.ui.ViewItem

class WidgetHeaderViewBinder(private val onClick: (UserApplication) -> Unit) :
  ViewBinder<WidgetHeaderViewHolder, WidgetHeaderViewItem> {

  override val viewType: Int = R.id.widget_header_view_type_id

  override fun createViewHolder(parent: ViewGroup): WidgetHeaderViewHolder =
    WidgetHeaderViewHolder(parent.inflate(R.layout.widget_header_view))

  override fun bindViewHolder(holder: WidgetHeaderViewHolder, viewItem: WidgetHeaderViewItem) {
    holder.textView.apply {
      text = viewItem.name
      updateDrawables(viewItem)

      setOnClickListener {
        onClick.invoke(viewItem.userApplication)
        viewItem.isExpanded = !viewItem.isExpanded
        updateDrawables(viewItem)
      }
    }
  }

  private fun TextView.updateDrawables(viewItem: WidgetHeaderViewItem) {
    val appIconSize = context.resources.getDimensionPixelSize(R.dimen.launcher_icon_size)
    val dropdownIconSize = (resources.displayMetrics.density * 24).toInt()
    val dropdownDrawableRes =
      if (viewItem.isExpanded) R.drawable.baseline_expand_less_24
      else R.drawable.baseline_expand_more_24

    setCompoundDrawablesRelative(
      viewItem.icon.apply { setSize(appIconSize) },
      null,
      ContextCompat.getDrawable(context, dropdownDrawableRes)?.apply { setSize(dropdownIconSize) },
      null,
    )
  }
}

class WidgetHeaderViewHolder(view: View) : ViewHolder(view) {
  val textView = view as TextView
}

class WidgetHeaderViewItem
private constructor(
  val userApplication: UserApplication,
  val name: CharSequence,
  val icon: Drawable,
  var isExpanded: Boolean,
) : ViewItem {

  override val viewType: Int = R.id.widget_header_view_type_id

  override fun areItemsTheSame(other: ViewItem): Boolean =
    other is WidgetHeaderViewItem &&
      userApplication.packageName == other.userApplication.packageName

  override fun areContentsTheSame(other: ViewItem): Boolean =
    other is WidgetHeaderViewItem && name == other.name && icon == other.icon

  class WidgetHeaderViewItemFactory
  @Inject
  constructor(private val launcherResourceProvider: LauncherResourceProvider) {

    suspend fun create(userApplication: UserApplication, isExpanded: Boolean) =
      WidgetHeaderViewItem(
        userApplication,
        launcherResourceProvider.getLabel(userApplication),
        launcherResourceProvider.getIcon(userApplication).await(),
        isExpanded,
      )
  }
}
