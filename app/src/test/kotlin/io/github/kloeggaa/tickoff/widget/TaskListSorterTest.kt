package io.github.kloeggaa.tickoff.widget

import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.Calendar
import java.util.Date

class TaskListSorterTest {

    // Fixed "today" of 2026-09-25, midday to stay clear of any day boundary.
    private val now: Date = Calendar.getInstance().apply {
        clear()
        set(2026, Calendar.SEPTEMBER, 25, 12, 0)
    }.time

    private fun task(text: String, due: String?, index: Int) =
        ParsedTask(rawLine = "- [ ] $text", text = text, dueDate = due, originalLineIndex = index)

    @Test
    fun bucketFor_classifiesRelativeToToday() {
        assertEquals(TaskListSorter.BUCKET_OVERDUE, TaskListSorter.bucketFor("2026-09-24", now, 7))
        assertEquals(TaskListSorter.BUCKET_TODAY, TaskListSorter.bucketFor("2026-09-25", now, 7))
        assertEquals(TaskListSorter.BUCKET_SOON, TaskListSorter.bucketFor("2026-09-26", now, 7))
        assertEquals(TaskListSorter.BUCKET_NO_DATE, TaskListSorter.bucketFor(null, now, 7))
    }

    @Test
    fun bucketFor_windowEndIsInclusive() {
        assertEquals(TaskListSorter.BUCKET_SOON, TaskListSorter.bucketFor("2026-10-02", now, 7))
        assertEquals(-1, TaskListSorter.bucketFor("2026-10-03", now, 7))
    }

    @Test
    fun bucketFor_windowCrossesMonthAndYear() {
        val newYearsEve = Calendar.getInstance().apply {
            clear()
            set(2026, Calendar.DECEMBER, 30, 12, 0)
        }.time
        assertEquals(TaskListSorter.BUCKET_SOON, TaskListSorter.bucketFor("2027-01-02", newYearsEve, 3))
        assertEquals(-1, TaskListSorter.bucketFor("2027-01-03", newYearsEve, 3))
    }

    @Test
    fun visibleSortedTasks_ordersBucketsAndDropsFarFutureTasks() {
        val tasks = listOf(
            task("no date B", null, 0),
            task("far future", "2026-12-01", 1),
            task("soon", "2026-09-28", 2),
            task("today", "2026-09-25", 3),
            task("overdue newer", "2026-09-20", 4),
            task("no date A", null, 5),
            task("overdue older", "2026-09-01", 6)
        )
        val result = TaskListSorter.visibleSortedTasks(tasks, now, 7).map { it.text }
        assertEquals(
            listOf("overdue older", "overdue newer", "today", "soon", "no date B", "no date A"),
            result
        )
    }

    @Test
    fun visibleSortedTasks_noDateTasksKeepFileOrderPastNineLines() {
        val tasks = (0..11).map { task("t$it", null, it) }.reversed()
        val result = TaskListSorter.visibleSortedTasks(tasks, now, 7).map { it.originalLineIndex }
        assertEquals((0..11).toList(), result)
    }
}
