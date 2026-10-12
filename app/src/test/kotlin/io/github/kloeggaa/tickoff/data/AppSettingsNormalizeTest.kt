package io.github.kloeggaa.tickoff.data

import org.junit.Assert.assertEquals
import org.junit.Test

class AppSettingsNormalizeTest {

    @Test
    fun folderIsTrimmedAndCleaned() {
        assertEquals("", AppSettings.normalizeFolder(""))
        assertEquals("", AppSettings.normalizeFolder("  /  "))
        assertEquals("Tasks", AppSettings.normalizeFolder(" Tasks/ "))
        assertEquals("Notes/Inbox", AppSettings.normalizeFolder("/Notes//Inbox/"))
        assertEquals("Notes/Inbox", AppSettings.normalizeFolder("Notes\\Inbox"))
        assertEquals("Notes/Inbox", AppSettings.normalizeFolder(" Notes / Inbox "))
    }

    @Test
    fun folderCannotEscapeTheVault() {
        assertEquals("Tasks", AppSettings.normalizeFolder("../Tasks"))
        assertEquals("a/b", AppSettings.normalizeFolder("./a/../b"))
    }

    @Test
    fun blankFileNameFallsBackToDefault() {
        assertEquals(AppSettings.DEFAULT_TASK_FILE_NAME, AppSettings.normalizeFileName(""))
        assertEquals(AppSettings.DEFAULT_TASK_FILE_NAME, AppSettings.normalizeFileName("   "))
        assertEquals("TasksAI.md", AppSettings.DEFAULT_TASK_FILE_NAME)
    }

    @Test
    fun fileNameGetsMarkdownExtensionWhenMissing() {
        assertEquals("Todo.md", AppSettings.normalizeFileName("Todo"))
        assertEquals("Todo.md", AppSettings.normalizeFileName(" Todo.md "))
        assertEquals("Todo.txt", AppSettings.normalizeFileName("Todo.txt"))
    }

    @Test
    fun fileNameCannotContainFolders() {
        assertEquals("SubTodo.md", AppSettings.normalizeFileName("Sub/Todo.md"))
        assertEquals("SubTodo.md", AppSettings.normalizeFileName("Sub\\Todo.md"))
    }
}
