package io.github.kloeggaa.tickoff.storage

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class DocumentPathsTest {

    @Test
    fun fileInsideVault() {
        assertEquals(
            "Tasks/Todo.md",
            DocumentPaths.relativePath("primary:Documents/MyVault", "primary:Documents/MyVault/Tasks/Todo.md")
        )
    }

    @Test
    fun vaultAtVolumeRoot() {
        assertEquals("Todo.md", DocumentPaths.relativePath("primary:", "primary:Todo.md"))
    }

    @Test
    fun siblingFolderWithSamePrefixIsOutside() {
        assertNull(DocumentPaths.relativePath("primary:Documents/MyVault", "primary:Documents/MyVault2/Todo.md"))
    }

    @Test
    fun otherVolumeOrProviderIdIsOutside() {
        assertNull(DocumentPaths.relativePath("primary:Documents/MyVault", "1A2B-3C4D:Documents/MyVault/Todo.md"))
        assertNull(DocumentPaths.relativePath("primary:Documents/MyVault", "msf:1234"))
    }
}
