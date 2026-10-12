package io.github.kloeggaa.tickoff.widget

/**
 * One open (not-done) task line parsed out of a task markdown file, e.g.
 *   "- [ ] Task text 📅 yyyy-MM-dd"
 *   "- [ ] Task text"
 */
data class ParsedTask(
    val rawLine: String,
    val text: String,
    val dueDate: String?,
    val originalLineIndex: Int
)

object TaskParser {
    private val TASK_LINE_REGEX = Regex("""^-\s*\[([ xX])\]\s*(.+)$""")

    /**
     * Only a date marked with 📅 is a due date (Obsidian Tasks convention); other dates on the
     * line, such as ⏳ scheduled or 🛫 start dates, stay part of the task text.
     */
    private val DUE_DATE_REGEX = Regex("""📅\s*(\d{4}-\d{2}-\d{2})""")

    /**
     * Parses every not-done task line out of [rawLines]. Done ("- [x]"/"- [X]") lines
     * and any non-matching line are skipped. Duplicate lines are intentionally kept as
     * separate entries so each can be toggled off independently.
     */
    fun parseOpenTasks(rawLines: List<String>): List<ParsedTask> =
        rawLines.mapIndexedNotNull { index, rawLine ->
            val trimmed = rawLine.trim()
            val match = TASK_LINE_REGEX.find(trimmed) ?: return@mapIndexedNotNull null
            if (match.groupValues[1].equals("x", ignoreCase = true)) {
                return@mapIndexedNotNull null
            }
            val rest = match.groupValues[2]
            val dueMatch = DUE_DATE_REGEX.find(rest)
            val text = if (dueMatch != null) rest.removeRange(dueMatch.range) else rest
            ParsedTask(
                rawLine = rawLine,
                text = text.replace(Regex("""\s{2,}"""), " ").trim(),
                dueDate = dueMatch?.groupValues?.get(1),
                originalLineIndex = index
            )
        }
}
