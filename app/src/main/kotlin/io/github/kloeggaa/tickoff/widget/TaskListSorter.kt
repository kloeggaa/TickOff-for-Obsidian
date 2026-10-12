package io.github.kloeggaa.tickoff.widget

import io.github.kloeggaa.tickoff.data.TaskWidgetPrefs
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/**
 * Filters and orders parsed tasks for display in the Task List widget:
 *   1. Overdue (due date before today) — oldest due date first.
 *   2. Due today.
 *   3. Due within the "due date window" (tomorrow..today+[horizonDays], inclusive) —
 *      each widget instance configures its own window; see [TaskWidgetPrefs].
 *   4. No due date at all — bottom, in file order.
 * Tasks due further out than the window are dropped entirely.
 *
 * Dates are plain "yyyy-MM-dd" strings. That format is fixed-width and zero-padded,
 * so lexicographic string comparison is already chronological comparison — no
 * date-diff arithmetic (or java.time, unavailable pre-API 26 without desugaring)
 * is needed.
 */
object TaskListSorter {

    const val BUCKET_OVERDUE = 0
    const val BUCKET_TODAY = 1
    const val BUCKET_SOON = 2
    const val BUCKET_NO_DATE = 3
    private const val BUCKET_EXCLUDED = -1

    /**
     * Classifies a "yyyy-MM-dd" due date (or null) relative to [now]. Shared by the
     * filter/sort below and by the widget row rendering (overdue/today badges), so
     * the two never drift apart. [horizonDays] is the widget instance's configured
     * due date window (see [TaskWidgetPrefs.getWidgetHorizonDays]).
     */
    fun bucketFor(
        dueDate: String?,
        now: Date = Date(),
        horizonDays: Int = TaskWidgetPrefs.DEFAULT_HORIZON_DAYS
    ): Int {
        if (dueDate == null) return BUCKET_NO_DATE
        val fmt = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        val today = fmt.format(now)
        val windowEnd = fmt.format(
            Calendar.getInstance().apply {
                time = now
                add(Calendar.DAY_OF_YEAR, horizonDays)
            }.time
        )
        return when {
            dueDate < today -> BUCKET_OVERDUE
            dueDate == today -> BUCKET_TODAY
            dueDate <= windowEnd -> BUCKET_SOON
            else -> BUCKET_EXCLUDED
        }
    }

    fun visibleSortedTasks(
        tasks: List<ParsedTask>,
        now: Date = Date(),
        horizonDays: Int = TaskWidgetPrefs.DEFAULT_HORIZON_DAYS
    ): List<ParsedTask> {
        return tasks
            .map { it to bucketFor(it.dueDate, now, horizonDays) }
            .filter { (_, bucket) -> bucket != BUCKET_EXCLUDED }
            .sortedWith(
                compareBy(
                    { (_, bucket) -> bucket },
                    { (task, bucket) ->
                        if (bucket == BUCKET_NO_DATE) {
                            "%09d".format(Locale.US, task.originalLineIndex)
                        } else {
                            task.dueDate
                        }
                    }
                )
            )
            .map { (task, _) -> task }
    }
}
