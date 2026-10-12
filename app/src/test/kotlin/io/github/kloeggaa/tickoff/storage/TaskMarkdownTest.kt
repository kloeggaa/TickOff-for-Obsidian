package io.github.kloeggaa.tickoff.storage

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class TaskMarkdownTest {

    @Test
    fun insertIntoEmptyFile() {
        assertEquals("- [ ] A\n", TaskMarkdown.insertTaskEntry("", "- [ ] A\n"))
    }

    @Test
    fun insertWithoutFencePrepends() {
        assertEquals("- [ ] New\n- [ ] Old", TaskMarkdown.insertTaskEntry("- [ ] Old", "- [ ] New\n"))
    }

    @Test
    fun insertGoesAfterLastQueryBlock() {
        val content = "```tasks\nnot done\n```\n```tasks\ndone\n```\n- [ ] Old"
        val expected = "```tasks\nnot done\n```\n```tasks\ndone\n```\n- [ ] New\n- [ ] Old"
        assertEquals(expected, TaskMarkdown.insertTaskEntry(content, "- [ ] New\n"))
    }

    @Test
    fun markDoneAddsCheckAndDoneDate() {
        val lines = listOf("# Tasks", "- [ ] Call Anna 📅 2026-09-30", "- [ ] Call Anna 📅 2026-09-30")
        val result = TaskMarkdown.markFirstMatchingLineDone(lines, "- [ ] Call Anna 📅 2026-09-30", "2026-10-09")!!
        assertEquals("- [x] Call Anna 📅 2026-09-30 ✅ 2026-10-09", result[1])
        assertEquals("- [ ] Call Anna 📅 2026-09-30", result[2])
    }

    @Test
    fun markDoneKeepsIndentation() {
        val result = TaskMarkdown.markFirstMatchingLineDone(listOf("  - [ ] Sub"), "  - [ ] Sub", "2026-10-09")!!
        assertEquals("  - [x] Sub ✅ 2026-10-09", result[0])
    }

    @Test
    fun markDoneReturnsNullWhenLineIsGone() {
        assertNull(TaskMarkdown.markFirstMatchingLineDone(listOf("- [x] A"), "- [ ] A", "2026-10-09"))
    }

    @Test
    fun normalizeCleansModelOutput() {
        val raw = "```markdown\n- [ ] Buy milk\n* Call Anna 2026-09-30\n\n- [ ] Buy milk\n```"
        assertEquals(
            "- [ ] Buy milk\n- [ ] Call Anna 📅 2026-09-30",
            TaskMarkdown.normalizeTaskMarkdown(raw)
        )
    }
}
