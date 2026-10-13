package io.github.kloeggaa.tickoff.widget

import android.appwidget.AppWidgetManager
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import io.github.kloeggaa.tickoff.R
import io.github.kloeggaa.tickoff.data.TaskWidgetPrefs
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Handles the Task List widget's own clicks (row checkbox, row tap, refresh button). Not exported:
 * only this app's PendingIntents reach it, so other apps can't tick off tasks. The widget provider
 * itself has to stay exported for the system's APPWIDGET_* broadcasts.
 */
class TaskRowActionReceiver : BroadcastReceiver() {

    companion object {
        const val ACTION_TASK_LIST_ITEM_CLICK = "io.github.kloeggaa.tickoff.ACTION_TASK_ROW_CLICK"
        const val ACTION_REFRESH_TASK_LIST = "io.github.kloeggaa.tickoff.ACTION_REFRESH_TASK_LIST"
        const val EXTRA_ROW_ACTION = "io.github.kloeggaa.tickoff.extra.ROW_ACTION"
        const val ROW_ACTION_TOGGLE = "toggle"
        const val ROW_ACTION_OPEN = "open"
        const val EXTRA_LINE_CONTENT = "io.github.kloeggaa.tickoff.extra.LINE_CONTENT"
    }

    override fun onReceive(context: Context, intent: Intent) {
        val widgetId = intent.getIntExtra(
            AppWidgetManager.EXTRA_APPWIDGET_ID,
            AppWidgetManager.INVALID_APPWIDGET_ID
        )
        // Only placed Task List widgets: an unknown ID would otherwise fall back to the default task file.
        val manager = AppWidgetManager.getInstance(context)
        val liveIds = manager.getAppWidgetIds(ComponentName(context, TaskListWidgetProvider::class.java))
        if (widgetId !in liveIds) return

        when (intent.action) {
            ACTION_REFRESH_TASK_LIST -> runAsync {
                // Also re-renders the header, so a vault picked meanwhile shows up.
                TaskListWidgetProvider.updateWidget(context, manager, widgetId)
                @Suppress("DEPRECATION")
                manager.notifyAppWidgetViewDataChanged(widgetId, R.id.task_list_view)
            }
            ACTION_TASK_LIST_ITEM_CLICK -> when (intent.getStringExtra(EXTRA_ROW_ACTION)) {
                ROW_ACTION_TOGGLE -> {
                    val lineContent = intent.getStringExtra(EXTRA_LINE_CONTENT) ?: return
                    runAsync {
                        if (WidgetTaskFile.markDone(context, widgetId, lineContent)) {
                            // Other widgets may show the same file, so all of them need the change.
                            TaskListWidgetProvider.refreshAll(context)
                        }
                    }
                }
                ROW_ACTION_OPEN -> runAsync {
                    ObsidianLauncher.openTaskFile(context, widgetId, TaskWidgetPrefs.getWidgetOpenMode(context, widgetId))
                }
            }
        }
    }

    private fun runAsync(block: suspend () -> Unit) {
        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                block()
            } finally {
                pendingResult.finish()
            }
        }
    }
}
