package link.danb.launcher.list

import android.annotation.SuppressLint
import android.appwidget.AppWidgetProviderInfo
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import com.google.android.material.button.MaterialButton
import link.danb.launcher.R
import link.danb.launcher.model.WidgetMetadata
import link.danb.launcher.utils.inflate

class WidgetEditorViewBinder(private val widgetEditorViewListener: WidgetEditorViewListener) :
    ViewBinder {

    override val viewType: Int = R.id.widget_editor_view_type_id

    override fun createViewHolder(parent: ViewGroup): ViewHolder {
        return WidgetEditorViewHolder(parent.inflate(R.layout.widget_editor_view))
    }

    @SuppressLint("ClickableViewAccessibility", "Recycle")
    override fun bindViewHolder(holder: ViewHolder, viewItem: ViewItem) {
        holder as WidgetEditorViewHolder
        viewItem as WidgetEditorViewItem

        holder.moveUpButton.setOnClickListener {
            widgetEditorViewListener.onMoveUp(viewItem.widgetMetadata)
        }

        holder.moveDownButton.setOnClickListener {
            widgetEditorViewListener.onMoveDown(viewItem.widgetMetadata)
        }

        var downEvent: MotionEvent? = null
        holder.heightResizeView.setOnTouchListener { view, motionEvent ->
            when (motionEvent.action) {
                MotionEvent.ACTION_DOWN -> {
                    view.parent.requestDisallowInterceptTouchEvent(true)
                    downEvent = MotionEvent.obtain(motionEvent)
                    true
                }

                MotionEvent.ACTION_MOVE -> {
                    downEvent != null
                }

                MotionEvent.ACTION_UP -> {
                    view.parent.requestDisallowInterceptTouchEvent(false)
                    widgetEditorViewListener.onChangeHeight(
                        viewItem.widgetMetadata, motionEvent.rawY.toInt() - downEvent!!.rawY.toInt()
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
            widgetEditorViewListener.onConfigureWidget(viewItem.widgetMetadata)
        }

        holder.removeButton.setOnClickListener {
            widgetEditorViewListener.onRemoveWidget(viewItem.widgetMetadata)
        }

        holder.doneButton.setOnClickListener {
            widgetEditorViewListener.onFinishEditing(viewItem.widgetMetadata)
        }
    }

    private class WidgetEditorViewHolder(view: View) : ViewHolder(view) {
        val moveUpButton: MaterialButton = view.findViewById(R.id.move_up)
        val moveDownButton: MaterialButton = view.findViewById(R.id.move_down)
        val heightResizeView: TextView = view.findViewById(R.id.height_resize)
        val configureButton: TextView = view.findViewById(R.id.configure_widget)
        val removeButton: TextView = view.findViewById(R.id.remove_widget)
        val doneButton: TextView = view.findViewById(R.id.done_editing)
    }
}

class WidgetEditorViewItem(
    val widgetMetadata: WidgetMetadata, val widgetProviderInfo: AppWidgetProviderInfo
) : ViewItem {
    override val viewType: Int = R.id.widget_editor_view_type_id

    override fun areItemsTheSame(other: ViewItem): Boolean {
        return other is WidgetEditorViewItem && widgetMetadata.widgetId == other.widgetMetadata.widgetId
    }

    override fun areContentsTheSame(other: ViewItem): Boolean {
        return true
    }
}

interface WidgetEditorViewListener {
    fun onFinishEditing(widgetMetadata: WidgetMetadata)
    fun onRemoveWidget(widgetMetadata: WidgetMetadata)
    fun onConfigureWidget(widgetMetadata: WidgetMetadata)
    fun onChangeHeight(widgetMetadata: WidgetMetadata, heightChange: Int)
    fun onMoveUp(widgetMetadata: WidgetMetadata)
    fun onMoveDown(widgetMetadata: WidgetMetadata)
}