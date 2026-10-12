package io.github.kloeggaa.tickoff.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.RemoteViews
import io.github.kloeggaa.tickoff.R
import io.github.kloeggaa.tickoff.data.TaskWidgetPrefs
import io.github.kloeggaa.tickoff.storage.VaultFiles
import io.github.kloeggaa.tickoff.ui.main.MainActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class TaskListWidgetProvider : AppWidgetProvider() {

    companion object {
        const val ACTION_TASK_LIST_ITEM_CLICK = "io.github.kloeggaa.tickoff.ACTION_TASK_ROW_CLICK"
        const val ACTION_REFRESH_TASK_LIST = "io.github.kloeggaa.tickoff.ACTION_REFRESH_TASK_LIST"
        const val EXTRA_ROW_ACTION = "io.github.kloeggaa.tickoff.extra.ROW_ACTION"
        const val ROW_ACTION_TOGGLE = "toggle"
        const val ROW_ACTION_OPEN = "open"
        const val EXTRA_LINE_CONTENT = "io.github.kloeggaa.tickoff.extra.LINE_CONTENT"

        suspend fun updateWidget(
            context: Context,
            appWidgetManager: AppWidgetManager,
            appWidgetId: Int
        ) {
            val views = RemoteViews(context.packageName, R.layout.widget_task_list)

            val info = WidgetTaskFile.info(context, appWidgetId)
            views.setTextViewText(
                R.id.task_list_file_name,
                info.title ?: context.getString(R.string.app_name)
            )
            views.setTextViewText(
                R.id.task_list_empty,
                context.getString(
                    when (info.status) {
                        WidgetTaskFile.Status.NO_VAULT -> R.string.task_list_no_vault
                        WidgetTaskFile.Status.NO_ACCESS -> R.string.task_list_no_access
                        WidgetTaskFile.Status.OK -> R.string.task_list_empty
                    }
                )
            )

            // A per-widget-unique `data` Uri is required: Intent.filterEquals() ignores
            // extras, so without this the widget host can reuse one RemoteViewsFactory
            // across multiple widget instances.
            val adapterIntent = Intent(context, TaskListWidgetService::class.java).apply {
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
                data = Uri.parse("content://${context.packageName}.widget.tasklist/$appWidgetId")
            }
            @Suppress("DEPRECATION")
            views.setRemoteAdapter(R.id.task_list_view, adapterIntent)
            views.setEmptyView(R.id.task_list_view, R.id.task_list_empty)

            val itemClickIntent = Intent(context, TaskListWidgetProvider::class.java).apply {
                action = ACTION_TASK_LIST_ITEM_CLICK
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
            }
            // Mutable: each row fills in its own action and line through setOnClickFillInIntent.
            val itemClickPendingIntent = PendingIntent.getBroadcast(
                context,
                appWidgetId,
                itemClickIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
            )
            views.setPendingIntentTemplate(R.id.task_list_view, itemClickPendingIntent)

            val refreshIntent = Intent(context, TaskListWidgetProvider::class.java).apply {
                action = ACTION_REFRESH_TASK_LIST
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
            }
            val refreshPendingIntent = PendingIntent.getBroadcast(
                context,
                appWidgetId,
                refreshIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.task_list_refresh_button, refreshPendingIntent)

            val openAppPendingIntent = PendingIntent.getActivity(
                context,
                0,
                Intent(context, MainActivity::class.java),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.task_list_file_name, openAppPendingIntent)
            views.setOnClickPendingIntent(R.id.task_list_empty, openAppPendingIntent)

            appWidgetManager.updateAppWidget(appWidgetId, views)
        }

        /** Re-renders every Task List widget, e.g. after the task file or its location changed. */
        suspend fun refreshAll(context: Context) {
            val manager = AppWidgetManager.getInstance(context)
            val ids = manager.getAppWidgetIds(ComponentName(context, TaskListWidgetProvider::class.java))
            ids.forEach { updateWidget(context, manager, it) }
            @Suppress("DEPRECATION")
            manager.notifyAppWidgetViewDataChanged(ids, R.id.task_list_view)
        }
    }

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                appWidgetIds.forEach { appWidgetId ->
                    updateWidget(context, appWidgetManager, appWidgetId)
                }
            } finally {
                pendingResult.finish()
            }
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        val widgetId = intent.getIntExtra(
            AppWidgetManager.EXTRA_APPWIDGET_ID,
            AppWidgetManager.INVALID_APPWIDGET_ID
        )
        when (intent.action) {
            ACTION_REFRESH_TASK_LIST -> {
                if (widgetId == AppWidgetManager.INVALID_APPWIDGET_ID) return
                // Also re-renders the header, so a vault picked meanwhile shows up.
                val pendingResult = goAsync()
                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        val manager = AppWidgetManager.getInstance(context)
                        updateWidget(context, manager, widgetId)
                        @Suppress("DEPRECATION")
                        manager.notifyAppWidgetViewDataChanged(widgetId, R.id.task_list_view)
                    } finally {
                        pendingResult.finish()
                    }
                }
            }
            ACTION_TASK_LIST_ITEM_CLICK -> {
                if (widgetId == AppWidgetManager.INVALID_APPWIDGET_ID) return
                when (intent.getStringExtra(EXTRA_ROW_ACTION)) {
                    ROW_ACTION_TOGGLE -> handleToggle(context, intent, widgetId)
                    ROW_ACTION_OPEN -> handleOpenInObsidian(context, widgetId)
                }
            }
            // Must fall through to super for onUpdate/onDeleted/onAppWidgetOptionsChanged
            // dispatch to keep working.
            else -> super.onReceive(context, intent)
        }
    }

    private fun handleToggle(context: Context, intent: Intent, widgetId: Int) {
        val lineContent = intent.getStringExtra(EXTRA_LINE_CONTENT) ?: return
        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                if (WidgetTaskFile.markDone(context, widgetId, lineContent)) {
                    // Other widgets may show the same file, so all of them need the change.
                    refreshAll(context)
                }
            } finally {
                pendingResult.finish()
            }
        }
    }

    private fun handleOpenInObsidian(context: Context, widgetId: Int) {
        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                ObsidianLauncher.openTaskFile(context, widgetId, TaskWidgetPrefs.getWidgetOpenMode(context, widgetId))
            } finally {
                pendingResult.finish()
            }
        }
    }

    override fun onDeleted(context: Context, appWidgetIds: IntArray) {
        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                appWidgetIds.forEach { widgetId ->
                    val fileUri = TaskWidgetPrefs.getWidgetFileUri(context, widgetId)
                    TaskWidgetPrefs.removeWidget(context, widgetId)
                    // Keep the picked file's access while another widget still shows it.
                    if (fileUri != null && !TaskWidgetPrefs.isFileUsedByOtherWidget(context, widgetId, fileUri)) {
                        VaultFiles.releaseFileAccess(context, fileUri)
                    }
                }
            } finally {
                pendingResult.finish()
            }
        }
    }
}
