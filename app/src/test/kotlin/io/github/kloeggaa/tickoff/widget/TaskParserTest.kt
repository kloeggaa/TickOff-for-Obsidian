package io.github.kloeggaa.tickoff.widget

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class TaskParserTest {

    @Test
    fun parsesOpenTaskWithDueDate() {
        val task = TaskParser.parseOpenTasks(listOf("- [ ] Call Anna 📅 2026-09-30")).single()
        assertEquals("Call Anna", task.text)
        assertEquals("2026-09-30", task.dueDate)
        assertEquals(0, task.originalLineIndex)
    }

    @Test
    fun parsesOpenTaskWithoutDueDate() {
        val task = TaskParser.parseOpenTasks(listOf("- [ ] Buy milk")).single()
        assertEquals("Buy milk", task.text)
        assertNull(task.dueDate)
    }

    @Test
    fun onlyCalendarEmojiDateIsDueDate() {
        val task = TaskParser.parseOpenTasks(listOf("- [ ] Pay rent ⏳ 2026-09-28 📅 2026-10-01")).single()
        assertEquals("2026-10-01", task.dueDate)
        assertEquals("Pay rent ⏳ 2026-09-28", task.text)
    }

    @Test
    fun plainDateWithoutEmojiIsNotDueDate() {
        val task = TaskParser.parseOpenTasks(listOf("- [ ] Read notes from 2026-09-01")).single()
        assertNull(task.dueDate)
        assertEquals("Read notes from 2026-09-01", task.text)
    }

    @Test
    fun skipsDoneTasksAndOtherLines() {
        val lines = listOf("# Tasks", "- [x] Done", "- [X] Also done", "", "some note", "- [ ] Open")
        val tasks = TaskParser.parseOpenTasks(lines)
        assertEquals(listOf("Open"), tasks.map { it.text })
        assertEquals(5, tasks.single().originalLineIndex)
    }

    @Test
    fun keepsDuplicateLinesAsSeparateTasks() {
        val tasks = TaskParser.parseOpenTasks(listOf("- [ ] Same", "- [ ] Same"))
        assertEquals(listOf(0, 1), tasks.map { it.originalLineIndex })
    }

    @Test
    fun keepsRawLineIncludingIndentation() {
        val task = TaskParser.parseOpenTasks(listOf("  - [ ] Indented")).single()
        assertEquals("  - [ ] Indented", task.rawLine)
        assertEquals("Indented", task.text)
    }
}
