package io.github.kloeggaa.tickoff.ui.record

import java.util.Locale

/**
 * Joins speech-recognizer utterances into one transcript. The recognizer sometimes repeats or
 * extends what it already delivered, so overlapping text is merged instead of duplicated.
 */
object TranscriptMerge {
    fun merge(existing: String, incoming: String): String {
        val base = existing.trim()
        val next = incoming.trim()
        if (base.isBlank()) return next
        if (next.isBlank()) return base

        val baseLower = base.lowercase(Locale.getDefault())
        val nextLower = next.lowercase(Locale.getDefault())
        return when {
            baseLower == nextLower -> base
            nextLower.startsWith(baseLower) -> next
            baseLower.startsWith(nextLower) -> base
            else -> "$base $next"
        }
    }
}
