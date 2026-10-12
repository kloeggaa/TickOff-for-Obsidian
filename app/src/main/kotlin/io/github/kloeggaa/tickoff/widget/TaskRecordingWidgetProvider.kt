package io.github.kloeggaa.tickoff.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.widget.RemoteViews
import io.github.kloeggaa.tickoff.R
import io.github.kloeggaa.tickoff.ui.record.RecordActivity

/** 1×1 mic button: opens the recorder and starts recording right away. */
class TaskRecordingWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        appWidgetIds.forEach { appWidgetId ->
            val views = RemoteViews(context.packageName, R.layout.widget_task_recording)
            val pendingIntent = PendingIntent.getActivity(
                context,
                appWidgetId,
                RecordActivity.createLaunchIntent(context, autoStart = true),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.widget_task_root, pendingIntent)
            appWidgetManager.updateAppWidget(appWidgetId, views)
        }
    }
}
