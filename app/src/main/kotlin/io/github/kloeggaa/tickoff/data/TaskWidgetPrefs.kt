package io.github.kloeggaa.tickoff.data

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import io.github.kloeggaa.tickoff.widget.OpenMode
import io.github.kloeggaa.tickoff.widget.TaskListWidgetProvider
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

/**
 * Per-widget-instance settings of the Task List widget, keyed by appWidgetId: which file it shows
 * (a file picked at setup, or none for the default task file from Settings), how many days ahead
 * it shows, and whether tapping a task opens the file in Obsidian (and how).
 */
object TaskWidgetPrefs {

    /** Default due-date window when a widget instance hasn't set one explicitly. */
    const val DEFAULT_HORIZON_DAYS = 7

    private fun fileUriKey(widgetId: Int) = stringPreferencesKey("task_widget_uri_$widgetId")
    private fun horizonDaysKey(widgetId: Int) = intPreferencesKey("task_widget_horizon_days_$widgetId")
    private fun openModeKey(widgetId: Int) = stringPreferencesKey("task_widget_open_mode_$widgetId")

    /** The file picked for this widget, or null when it shows the default task file. */
    suspend fun getWidgetFileUri(context: Context, widgetId: Int): String? =
        context.dataStore.data.map { it[fileUriKey(widgetId)] }.first()

    /** The "due date window" this widget instance was configured with, in days. */
    suspend fun getWidgetHorizonDays(context: Context, widgetId: Int): Int =
        context.dataStore.data.map { it[horizonDaysKey(widgetId)] ?: DEFAULT_HORIZON_DAYS }.first()

    suspend fun getWidgetOpenMode(context: Context, widgetId: Int): OpenMode =
        context.dataStore.data.map { OpenMode.fromKey(it[openModeKey(widgetId)]) }.first()

    suspend fun setWidgetConfig(context: Context, widgetId: Int, fileUri: String?, days: Int, openMode: OpenMode) {
        context.dataStore.edit {
            if (fileUri == null) it.remove(fileUriKey(widgetId)) else it[fileUriKey(widgetId)] = fileUri
            it[horizonDaysKey(widgetId)] = days
            it[openModeKey(widgetId)] = openMode.key
        }
    }

    suspend fun removeWidget(context: Context, widgetId: Int) {
        context.dataStore.edit {
            it.remove(fileUriKey(widgetId))
            it.remove(horizonDaysKey(widgetId))
            it.remove(openModeKey(widgetId))
        }
    }

    /**
     * Whether a Task List widget other than [excludeWidgetId] that is still placed on a home
     * screen shows [fileUri]. Used to decide whether a picked file's persisted access can be released.
     */
    suspend fun isFileUsedByOtherWidget(context: Context, excludeWidgetId: Int, fileUri: String): Boolean {
        val liveIds = AppWidgetManager.getInstance(context)
            .getAppWidgetIds(ComponentName(context, TaskListWidgetProvider::class.java))
        return liveIds
            .filter { it != excludeWidgetId }
            .any { getWidgetFileUri(context, it) == fileUri }
    }
}
