package link.danb.launcher.widgets

import android.annotation.SuppressLint
import android.appwidget.AppWidgetProviderInfo
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import com.google.android.material.button.MaterialButton
import link.danb.launcher.R
import link.danb.launcher.database.WidgetData
import link.danb.launcher.extensions.inflate
import link.danb.launcher.extensions.setLayoutSize
import link.danb.launcher.ui.ViewBinder
import link.danb.launcher.ui.ViewItem

class WidgetEditorViewBinder(
  private val appWidgetViewProvider: AppWidgetViewProvider,
  private val widgetSizeUtil: WidgetSizeUtil,
  private val onConfigure: (widgetData: WidgetData, view: View) -> Unit,
  private val onDelete: (widgetData: WidgetData) -> Unit,
  private val onMoveUp: (widgetData: WidgetData) -> Unit,
  private val onResize: (widgetData: WidgetData, height: Int) -> Unit,
  private val onMoveDown: (widgetData: WidgetData) -> Unit,
  private val onDone: (widgetData: WidgetData) -> Unit,
) : ViewBinder<WidgetEditorViewHolder, WidgetEditorViewItem> {

  override val viewType: Int = R.id.widget_editor_view_type_id

  override fun createViewHolder(parent: ViewGroup): WidgetEditorViewHolder =
    WidgetEditorViewHolder(parent.inflate(R.layout.widget_editor_view))

  @SuppressLint("ClickableViewAccessibility", "Recycle")
  override fun bindViewHolder(holder: WidgetEditorViewHolder, viewItem: WidgetEditorViewItem) {
    holder.moveUpButton.setOnClickListener { onMoveUp(viewItem.widgetData) }

    holder.moveDownButton.setOnClickListener { onMoveDown(viewItem.widgetData) }

    var downEvent: MotionEvent? = null
    holder.resizeButton.setOnTouchListener { view, motionEvent ->
      when (motionEvent.action) {
        MotionEvent.ACTION_DOWN -> {
          view.parent.requestDisallowInterceptTouchEvent(true)
          downEvent = MotionEvent.obtain(motionEvent)
          true
        }
        MotionEvent.ACTION_MOVE -> {
          appWidgetViewProvider
            .getView(viewItem.widgetData.widgetId)
            .setLayoutSize(
              height =
                widgetSizeUtil.getWidgetHeight(
                  viewItem.widgetData.height + motionEvent.rawY.toInt() - downEvent!!.rawY.toInt()
                )
            )
          downEvent != null
        }
        MotionEvent.ACTION_UP -> {
          view.parent.requestDisallowInterceptTouchEvent(false)
          onResize(
            viewItem.widgetData,
            viewItem.widgetData.height + motionEvent.rawY.toInt() - downEvent!!.rawY.toInt()
          )
          downEvent!!.recycle()
          downEvent = null
          true
        }
        else -> false
      }
    }

    holder.configureButton.apply {
      visibility =
        if (viewItem.widgetProviderInfo.configure != null) {
          View.VISIBLE
        } else {
          View.GONE
        }
      setOnClickListener { onConfigure(viewItem.widgetData, this) }
    }

    holder.deleteButton.setOnClickListener { onDelete(viewItem.widgetData) }

    holder.doneButton.setOnClickListener { onDone(viewItem.widgetData) }
  }
}

class WidgetEditorViewHolder(view: View) : ViewHolder(view) {
  val configureButton: TextView = view.findViewById(R.id.widget_configure_button)
  val deleteButton: TextView = view.findViewById(R.id.widget_delete_button)
  val moveUpButton: MaterialButton = view.findViewById(R.id.widget_move_up_button)
  val resizeButton: TextView = view.findViewById(R.id.widget_resize_button)
  val moveDownButton: MaterialButton = view.findViewById(R.id.widget_move_down_button)
  val doneButton: TextView = view.findViewById(R.id.widget_done_button)
}

class WidgetEditorViewItem(
  val widgetData: WidgetData,
  val widgetProviderInfo: AppWidgetProviderInfo,
) : ViewItem {

  override val viewType: Int = R.id.widget_editor_view_type_id

  override fun areItemsTheSame(other: ViewItem): Boolean =
    other is WidgetEditorViewItem && widgetData.widgetId == other.widgetData.widgetId

  override fun areContentsTheSame(other: ViewItem): Boolean =
    other is WidgetEditorViewItem && widgetData == other.widgetData
}
