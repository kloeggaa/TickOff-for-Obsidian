package io.github.kloeggaa.tickoff.ui.record

import org.junit.Assert.assertEquals
import org.junit.Test

class TranscriptMergeTest {

    @Test
    fun firstUtteranceIsTakenAsIs() {
        assertEquals("buy milk", TranscriptMerge.merge("", "  buy milk "))
    }

    @Test
    fun blankIncomingKeepsExisting() {
        assertEquals("buy milk", TranscriptMerge.merge("buy milk", "  "))
    }

    @Test
    fun separateUtterancesAreJoined() {
        assertEquals("buy milk call Anna tomorrow", TranscriptMerge.merge("buy milk", "call Anna tomorrow"))
    }

    @Test
    fun repeatedUtteranceIsNotDuplicated() {
        assertEquals("Buy milk", TranscriptMerge.merge("Buy milk", "buy milk"))
    }

    @Test
    fun extendedUtteranceReplacesItsPrefix() {
        assertEquals("buy milk and bread", TranscriptMerge.merge("buy milk", "buy milk and bread"))
    }

    @Test
    fun shorterRepeatOfThePrefixIsIgnored() {
        assertEquals("buy milk and bread", TranscriptMerge.merge("buy milk and bread", "buy milk"))
    }
}
