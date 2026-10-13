package io.github.kloeggaa.tickoff.widget

import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.Intent
import android.view.View
import android.widget.RemoteViews
import android.widget.RemoteViewsService.RemoteViewsFactory
import io.github.kloeggaa.tickoff.R
import io.github.kloeggaa.tickoff.data.TaskWidgetPrefs
import kotlinx.coroutines.runBlocking

/**
 * Supplies the rows for one Task List widget instance's ListView. All methods here
 * run on a binder thread of this app's process, so the blocking file I/O and DataStore
 * reads below are safe and expected (unlike inside [TaskListWidgetProvider.onReceive],
 * which must not block).
 */
class TaskListRemoteViewsFactory(
    private val context: Context,
    private val appWidgetId: Int
) : RemoteViewsFactory {

    private var tasks: List<ParsedTask> = emptyList()
    private var horizonDays: Int = TaskWidgetPrefs.DEFAULT_HORIZON_DAYS
    private var openMode: OpenMode = OpenMode.OFF

    override fun onCreate() {
        tasks = emptyList()
    }

    override fun onDataSetChanged() {
        if (appWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
            tasks = emptyList()
            return
        }
        horizonDays = runBlocking { TaskWidgetPrefs.getWidgetHorizonDays(context, appWidgetId) }
        openMode = runBlocking { TaskWidgetPrefs.getWidgetOpenMode(context, appWidgetId) }
        val lines = runBlocking { WidgetTaskFile.readLines(context, appWidgetId) }
        tasks = TaskListSorter.visibleSortedTasks(TaskParser.parseOpenTasks(lines), horizonDays = horizonDays)
    }

    override fun onDestroy() {
        tasks = emptyList()
    }

    override fun getCount(): Int = tasks.size

    override fun getViewAt(position: Int): RemoteViews {
        val task = tasks[position]
        val views = RemoteViews(context.packageName, R.layout.widget_task_list_item)
        views.setTextViewText(R.id.task_item_text, task.text)

        if (task.dueDate != null) {
            views.setViewVisibility(R.id.task_item_due, View.VISIBLE)
            val prefix = when (TaskListSorter.bucketFor(task.dueDate, horizonDays = horizonDays)) {
                TaskListSorter.BUCKET_OVERDUE -> "🔥 "
                TaskListSorter.BUCKET_TODAY -> "🌟 "
                else -> ""
            }
            views.setTextViewText(R.id.task_item_due, "$prefix${task.dueDate}")
        } else {
            views.setViewVisibility(R.id.task_item_due, View.GONE)
        }

        val toggleFill = Intent().apply {
            putExtra(TaskRowActionReceiver.EXTRA_ROW_ACTION, TaskRowActionReceiver.ROW_ACTION_TOGGLE)
            putExtra(TaskRowActionReceiver.EXTRA_LINE_CONTENT, task.rawLine)
        }
        views.setOnClickFillInIntent(R.id.task_item_checkbox, toggleFill)

        if (openMode != OpenMode.OFF) {
            val openFill = Intent().apply {
                putExtra(TaskRowActionReceiver.EXTRA_ROW_ACTION, TaskRowActionReceiver.ROW_ACTION_OPEN)
            }
            views.setOnClickFillInIntent(R.id.task_item_root, openFill)
        }

        return views
    }

    override fun getLoadingView(): RemoteViews? = null

    override fun getViewTypeCount(): Int = 1

    override fun getItemId(position: Int): Long = position.toLong()

    override fun hasStableIds(): Boolean = false
}
