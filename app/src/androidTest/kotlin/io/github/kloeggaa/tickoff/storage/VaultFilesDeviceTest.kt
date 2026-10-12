package io.github.kloeggaa.tickoff.storage

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import io.github.kloeggaa.tickoff.data.AppSettings
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Exercises the real Storage Access Framework path against the vault folder picked in the app.
 * Manual setup: install the debug app, choose a vault folder in Settings, then run
 * `./gradlew connectedDebugAndroidTest` (set ANDROID_SERIAL when several devices are connected). Skipped when no vault folder has been picked.
 * Writes into "<vault>/TickOffTest/DeviceTest.md".
 */
@RunWith(AndroidJUnit4::class)
class VaultFilesDeviceTest {

    @Test
    fun appendCreatesFolderAndFileThenTicksOff() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val vault = AppSettings(context).vaultTreeUriFlow.first()
        assumeTrue("Pick a vault folder in the app first", vault != null && VaultFiles.hasVaultAccess(context, vault))
        val location = TaskFileLocation(vault!!, "TickOffTest", "DeviceTest.md")

        assertTrue(VaultFiles.appendTasks(context, location, "- [ ] First 📅 2026-10-10"))
        assertTrue(VaultFiles.appendTasks(context, location, "- [ ] Second"))
        assertEquals(
            listOf("- [ ] Second", "- [ ] First 📅 2026-10-10", ""),
            VaultFiles.readAllLines(context, location)
        )

        assertTrue(VaultFiles.markDone(context, location, "- [ ] First 📅 2026-10-10"))
        val lines = VaultFiles.readAllLines(context, location)!!
        assertTrue(lines[1], lines[1].startsWith("- [x] First 📅 2026-10-10 ✅ "))
        assertEquals("- [ ] Second", lines[0])
    }
}
