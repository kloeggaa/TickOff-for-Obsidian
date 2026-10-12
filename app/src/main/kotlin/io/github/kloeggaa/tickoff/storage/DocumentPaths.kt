package io.github.kloeggaa.tickoff.storage

/** Pure helpers on SAF document IDs, kept free of Android types for unit tests. */
object DocumentPaths {
    /**
     * Relative path of [documentId] below [treeDocumentId], e.g. tree "primary:Documents/MyVault"
     * and document "primary:Documents/MyVault/Tasks/Todo.md" give "Tasks/Todo.md". Null when the
     * document isn't below the tree. Path-like IDs are what the device storage provider uses, which
     * is where Obsidian vaults live on Android.
     */
    fun relativePath(treeDocumentId: String, documentId: String): String? {
        val prefix = if (treeDocumentId.endsWith("/") || treeDocumentId.endsWith(":")) {
            treeDocumentId
        } else {
            "$treeDocumentId/"
        }
        if (!documentId.startsWith(prefix)) return null
        return documentId.removePrefix(prefix).takeIf { it.isNotEmpty() }
    }
}
