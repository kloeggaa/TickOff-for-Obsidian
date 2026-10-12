package io.github.kloeggaa.tickoff.storage

import org.junit.Assert.assertEquals
import org.junit.Test

class TaskFileLocationTest {

    @Test
    fun fileAtVaultRoot() {
        assertEquals("TasksAI.md", TaskFileLocation("content://vault", "", "TasksAI.md").relativePath)
    }

    @Test
    fun fileInSubfolder() {
        assertEquals("Notes/Inbox/Todo.md", TaskFileLocation("content://vault", "Notes/Inbox", "Todo.md").relativePath)
    }
}
