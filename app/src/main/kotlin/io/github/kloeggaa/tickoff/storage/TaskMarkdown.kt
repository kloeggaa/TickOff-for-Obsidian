package io.github.kloeggaa.tickoff.storage

/** Pure text transformations on the task markdown file, kept free of Android types for unit tests. */
object TaskMarkdown {
    private val DATE_REGEX = Regex("\\d{4}-\\d{2}-\\d{2}")

    /**
     * Places a newly recorded task [entry] into [currentContent].
     *
     * The task file can start with one or more Obsidian Tasks-plugin query blocks,
     * each fenced as ` ```tasks ` ... ` ``` `. A recorded task must never land inside
     * or before one of those blocks. So: find the *last* closing ` ``` ` fence in the
     * file and insert the new entry immediately after it, ahead of whatever task lines
     * already follow. If the file has no fenced block at all, prepend the entry at the
     * very top of the file.
     *
     * Fences are matched by pairing consecutive fence-marker lines (open, close,
     * open, close, ...) rather than specifically requiring "```tasks", so a task
     * whose own text happens to contain "```" can't be mistaken for a query block
     * boundary as long as it isn't itself sitting on its own line as a bare fence.
     */
    fun insertTaskEntry(currentContent: String, entry: String): String {
        if (currentContent.isBlank()) {
            return entry
        }

        val lines = currentContent.split("\n")
        var inFence = false
        var lastFenceCloseIndex = -1
        for (i in lines.indices) {
            if (lines[i].trim().startsWith("```")) {
                inFence = if (inFence) {
                    lastFenceCloseIndex = i
                    false
                } else {
                    true
                }
            }
        }

        if (lastFenceCloseIndex == -1) {
            val separator = if (!entry.endsWith("\n")) "\n" else ""
            return entry + separator + currentContent
        }

        val before = lines.subList(0, lastFenceCloseIndex + 1).joinToString("\n")
        val after = lines.subList(lastFenceCloseIndex + 1, lines.size).joinToString("\n")
        return before + "\n" + entry.removeSuffix("\n") + "\n" + after
    }

    /**
     * Rewrites the first not-yet-done line equal to [targetLine] (after trimming) from
     * "- [ ]" to "- [x]" and appends the Obsidian Tasks done date "✅ [today]". Returns
     * null when no such line exists (e.g. it was already ticked or edited elsewhere).
     */
    fun markFirstMatchingLineDone(lines: List<String>, targetLine: String, today: String): List<String>? {
        val target = targetLine.trim()
        val idx = lines.indexOfFirst { it.trim() == target && it.contains("- [ ]") }
        if (idx == -1) return null
        val done = lines[idx].replaceFirst("- [ ]", "- [x]").trimEnd() + " ✅ $today"
        return lines.toMutableList().also { it[idx] = done }
    }

    /**
     * Cleans the model's reply into one "- [ ] task" line per task, with a due date
     * written as "📅 yyyy-MM-dd" when the reply contains one.
     */
    fun normalizeTaskMarkdown(raw: String): String {
        val lines = raw
            .replace("```markdown", "")
            .replace("```", "")
            .lines()
            .map { it.trim() }
            .filter { it.isNotBlank() }

        val normalized = lines.map { line ->
            val content = line
                .removePrefix("- [ ]")
                .removePrefix("-")
                .removePrefix("*")
                .trim()

            val date = DATE_REGEX.find(content)?.value
            val withoutDateParts = content
                .replace("📅", "")
                .replace(DATE_REGEX, "")
                .trim()
                .ifBlank { "Task" }

            if (date != null) {
                "- [ ] $withoutDateParts 📅 $date"
            } else {
                "- [ ] $withoutDateParts"
            }
        }

        return normalized.distinct().joinToString("\n")
    }
}
