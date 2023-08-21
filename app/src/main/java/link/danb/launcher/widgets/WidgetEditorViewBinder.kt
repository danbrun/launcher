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
import link.danb.launcher.database.WidgetMetadata
import link.danb.launcher.ui.ViewBinder
import link.danb.launcher.ui.ViewItem
import link.danb.launcher.utils.inflate
import link.danb.launcher.utils.setLayoutSize

class WidgetEditorViewBinder(
    private val appWidgetViewProvider: AppWidgetViewProvider,
    private val widgetSizeUtil: WidgetSizeUtil,
    private val widgetEditorViewListener: WidgetEditorViewListener,
) : ViewBinder<WidgetEditorViewHolder, WidgetEditorViewItem> {

    override val viewType: Int = R.id.widget_editor_view_type_id

    override fun createViewHolder(parent: ViewGroup): WidgetEditorViewHolder {
        return WidgetEditorViewHolder(parent.inflate(R.layout.widget_editor_view))
    }

    @SuppressLint("ClickableViewAccessibility", "Recycle")
    override fun bindViewHolder(holder: WidgetEditorViewHolder, viewItem: WidgetEditorViewItem) {
        holder.moveUpButton.setOnClickListener {
            widgetEditorViewListener.onMoveUp(viewItem.widgetMetadata)
        }

        holder.moveDownButton.setOnClickListener {
            widgetEditorViewListener.onMoveDown(viewItem.widgetMetadata)
        }

        var downEvent: MotionEvent? = null
        holder.resizeButton.setOnTouchListener { view, motionEvent ->
            when (motionEvent.action) {
                MotionEvent.ACTION_DOWN -> {
                    view.parent.requestDisallowInterceptTouchEvent(true)
                    downEvent = MotionEvent.obtain(motionEvent)
                    true
                }

                MotionEvent.ACTION_MOVE -> {
                    appWidgetViewProvider.getView(viewItem.widgetMetadata.widgetId)
                        .setLayoutSize(height = widgetSizeUtil.getWidgetHeight(viewItem.widgetMetadata.height + motionEvent.rawY.toInt() - downEvent!!.rawY.toInt()))
                    downEvent != null
                }

                MotionEvent.ACTION_UP -> {
                    view.parent.requestDisallowInterceptTouchEvent(false)
                    widgetEditorViewListener.onResize(
                        viewItem.widgetMetadata,
                        viewItem.widgetMetadata.height + motionEvent.rawY.toInt() - downEvent!!.rawY.toInt()
                    )
                    downEvent!!.recycle()
                    downEvent = null
                    true
                }

                else -> false
            }
        }

        holder.configureButton.visibility = if (viewItem.widgetProviderInfo.configure != null) {
            View.VISIBLE
        } else {
            View.GONE
        }
        holder.configureButton.setOnClickListener {
            widgetEditorViewListener.onConfigure(viewItem.widgetMetadata)
        }

        holder.deleteButton.setOnClickListener {
            widgetEditorViewListener.onDelete(viewItem.widgetMetadata)
        }

        holder.doneButton.setOnClickListener {
            widgetEditorViewListener.onDone(viewItem.widgetMetadata)
        }
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
    val widgetMetadata: WidgetMetadata, val widgetProviderInfo: AppWidgetProviderInfo
) : ViewItem {

    override val viewType: Int = R.id.widget_editor_view_type_id

    override fun areItemsTheSame(other: ViewItem): Boolean =
        other is WidgetEditorViewItem && widgetMetadata.widgetId == other.widgetMetadata.widgetId

    override fun areContentsTheSame(other: ViewItem): Boolean =
        other is WidgetEditorViewItem && widgetMetadata == other.widgetMetadata
}

interface WidgetEditorViewListener {
    fun onConfigure(widgetMetadata: WidgetMetadata)
    fun onDelete(widgetMetadata: WidgetMetadata)
    fun onMoveUp(widgetMetadata: WidgetMetadata)
    fun onResize(widgetMetadata: WidgetMetadata, height: Int)
    fun onMoveDown(widgetMetadata: WidgetMetadata)
    fun onDone(widgetMetadata: WidgetMetadata)
}
