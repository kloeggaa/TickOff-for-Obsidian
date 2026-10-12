package io.github.kloeggaa.tickoff.widget

import org.junit.Assert.assertEquals
import org.junit.Test

class OpenModeTest {

    @Test
    fun storedKeysRoundTrip() {
        OpenMode.entries.forEach { assertEquals(it, OpenMode.fromKey(it.key)) }
    }

    @Test
    fun missingOrUnknownKeyMeansOff() {
        assertEquals(OpenMode.OFF, OpenMode.fromKey(null))
        assertEquals(OpenMode.OFF, OpenMode.fromKey("something-else"))
    }

    @Test
    fun storedKeysStayStable() {
        // These strings are persisted per widget; renaming them would reset existing widgets.
        assertEquals("off", OpenMode.OFF.key)
        assertEquals("obsidian", OpenMode.OBSIDIAN.key)
        assertEquals("advanced_uri", OpenMode.ADVANCED_URI.key)
    }
}
