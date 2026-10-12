package io.github.kloeggaa.tickoff.storage

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class DocumentPathsEdgeTest {

    @Test
    fun treeIdWithTrailingSlash() {
        assertEquals("Todo.md", DocumentPaths.relativePath("primary:Vault/", "primary:Vault/Todo.md"))
    }

    @Test
    fun theVaultFolderItselfIsNotAFile() {
        assertNull(DocumentPaths.relativePath("primary:Documents/MyVault", "primary:Documents/MyVault"))
        assertNull(DocumentPaths.relativePath("primary:Documents/MyVault", "primary:Documents/MyVault/"))
    }

    @Test
    fun fileAboveTheVaultIsOutside() {
        assertNull(DocumentPaths.relativePath("primary:Documents/MyVault", "primary:Documents/Todo.md"))
        assertNull(DocumentPaths.relativePath("primary:Documents/MyVault", "primary:Download/Shopping.md"))
    }

    @Test
    fun namesWithSpacesAndUnicodeAreKept() {
        assertEquals(
            "Projekte/Größe Liste.md",
            DocumentPaths.relativePath("primary:Documents/Mein Vault", "primary:Documents/Mein Vault/Projekte/Größe Liste.md")
        )
    }
}
