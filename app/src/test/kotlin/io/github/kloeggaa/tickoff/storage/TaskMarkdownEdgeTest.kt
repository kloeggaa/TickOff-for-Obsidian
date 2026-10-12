package io.github.kloeggaa.tickoff.storage

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class TaskMarkdownEdgeTest {

    @Test
    fun unclosedFenceCountsAsNoQueryBlock() {
        assertEquals("- [ ] New\n```tasks\n- [ ] Old", TaskMarkdown.insertTaskEntry("```tasks\n- [ ] Old", "- [ ] New\n"))
    }

    @Test
    fun insertKeepsTextAboveTheQueryBlock() {
        val content = "# Tasks\n```tasks\nnot done\n```\n- [ ] Old"
        assertEquals(
            "# Tasks\n```tasks\nnot done\n```\n- [ ] A\n- [ ] B\n- [ ] Old",
            TaskMarkdown.insertTaskEntry(content, "- [ ] A\n- [ ] B\n")
        )
    }

    @Test
    fun insertAfterQueryBlockAtEndOfFile() {
        assertEquals("```tasks\n```\n- [ ] New\n", TaskMarkdown.insertTaskEntry("```tasks\n```", "- [ ] New\n"))
    }

    @Test
    fun whitespaceOnlyFileIsTreatedAsEmpty() {
        assertEquals("- [ ] New\n", TaskMarkdown.insertTaskEntry("  \n\n", "- [ ] New\n"))
    }

    @Test
    fun markDoneTrimsTrailingSpacesBeforeDoneDate() {
        val result = TaskMarkdown.markFirstMatchingLineDone(listOf("- [ ] Call Anna   "), "- [ ] Call Anna", "2026-10-09")!!
        assertEquals("- [x] Call Anna ✅ 2026-10-09", result.single())
    }

    @Test
    fun markDoneSkipsAlreadyDoneDuplicate() {
        val lines = listOf("- [x] Same ✅ 2026-10-01", "- [ ] Same")
        val result = TaskMarkdown.markFirstMatchingLineDone(lines, "- [ ] Same", "2026-10-09")!!
        assertEquals(listOf("- [x] Same ✅ 2026-10-01", "- [x] Same ✅ 2026-10-09"), result)
    }

    @Test
    fun markDoneDoesNotChangeTheInputList() {
        val lines = listOf("- [ ] A")
        TaskMarkdown.markFirstMatchingLineDone(lines, "- [ ] A", "2026-10-09")
        assertEquals(listOf("- [ ] A"), lines)
    }

    @Test
    fun markDoneOnEditedLineFindsNothing() {
        assertNull(TaskMarkdown.markFirstMatchingLineDone(listOf("- [ ] A changed"), "- [ ] A", "2026-10-09"))
    }

    @Test
    fun normalizeHandlesBulletStylesAndDates() {
        assertEquals(
            "- [ ] Water plants\n- [ ] Pay rent 📅 2026-11-01\n- [ ] Call mum 📅 2026-10-10",
            TaskMarkdown.normalizeTaskMarkdown("* Water plants\n- Pay rent 2026-11-01\n- [ ] Call mum 📅 2026-10-10")
        )
    }

    @Test
    fun normalizeKeepsALabelWhenOnlyADateIsLeft() {
        assertEquals("- [ ] Task 📅 2026-10-10", TaskMarkdown.normalizeTaskMarkdown("- 📅 2026-10-10"))
    }

    @Test
    fun normalizeOfEmptyReplyIsEmpty() {
        assertEquals("", TaskMarkdown.normalizeTaskMarkdown("```\n\n```"))
    }
}
