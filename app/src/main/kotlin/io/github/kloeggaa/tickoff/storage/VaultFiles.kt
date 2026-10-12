package io.github.kloeggaa.tickoff.storage

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.DocumentsContract
import android.util.Log
import androidx.documentfile.provider.DocumentFile
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** The task file: [folder] (relative, may be empty) and [fileName] inside the picked vault folder. */
data class TaskFileLocation(
    val vaultTreeUri: String,
    val folder: String,
    val fileName: String
) {
    /** Path relative to the vault root, as Obsidian's deep links expect it. */
    val relativePath: String get() = if (folder.isEmpty()) fileName else "$folder/$fileName"
}

/**
 * Reads and writes the task file through the Storage Access Framework only: the vault folder is a
 * tree URI picked with ACTION_OPEN_DOCUMENT_TREE and kept with a persisted URI permission. No file
 * paths and no storage permissions are involved.
 */
object VaultFiles {
    private const val TAG = "TickOffVault"

    fun hasVaultAccess(context: Context, vaultTreeUri: String): Boolean {
        val uri = Uri.parse(vaultTreeUri)
        return context.contentResolver.persistedUriPermissions.any {
            it.uri == uri && it.isReadPermission && it.isWritePermission
        }
    }

    /** Takes the persisted read/write grant for a newly picked vault and drops the grant of the old one. */
    fun takeVaultAccess(context: Context, newUri: Uri, oldUri: String?) {
        val flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
        context.contentResolver.takePersistableUriPermission(newUri, flags)
        if (oldUri != null && oldUri != newUri.toString()) {
            try {
                context.contentResolver.releasePersistableUriPermission(Uri.parse(oldUri), flags)
            } catch (_: SecurityException) {
                // The old grant was already gone.
            }
        }
    }

    /** The vault's folder name, which is also the vault name Obsidian uses in its links. */
    fun vaultName(context: Context, vaultTreeUri: String): String? = try {
        DocumentFile.fromTreeUri(context, Uri.parse(vaultTreeUri))?.name
    } catch (e: Exception) {
        Log.w(TAG, "Could not read vault name", e)
        null
    }

    /** Returns the file's lines, an empty list when the file doesn't exist yet, or null when the vault can't be read. */
    fun readAllLines(context: Context, location: TaskFileLocation): List<String>? {
        return try {
            val root = vaultRoot(context, location) ?: return null
            val file = findFile(root, location) ?: return emptyList()
            val text = context.contentResolver.openInputStream(file.uri)
                ?.bufferedReader()
                ?.use { it.readText() }
                ?: ""
            text.split("\n")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to read task file", e)
            null
        }
    }

    /** Adds recorded task lines to the task file, creating the folder and file if needed. */
    fun appendTasks(context: Context, location: TaskFileLocation, tasksMarkdown: String): Boolean {
        val normalized = tasksMarkdown.trim()
        if (normalized.isBlank()) return true
        return try {
            val root = vaultRoot(context, location) ?: return false
            val file = findOrCreateFile(root, location) ?: return false
            val current = context.contentResolver.openInputStream(file.uri)
                ?.bufferedReader()
                ?.use { it.readText() }
                ?: ""
            writeText(context, file.uri, TaskMarkdown.insertTaskEntry(current, "$normalized\n"))
        } catch (e: Exception) {
            Log.e(TAG, "Failed to save tasks", e)
            false
        }
    }

    /**
     * Ticks off the first open line equal to [targetLine]. The file is re-read fresh each call, so
     * it is the single source of truth for which occurrence of a duplicated line was already ticked.
     */
    fun markDone(context: Context, location: TaskFileLocation, targetLine: String): Boolean {
        return try {
            val root = vaultRoot(context, location) ?: return false
            val file = findFile(root, location) ?: return false
            val lines = context.contentResolver.openInputStream(file.uri)
                ?.bufferedReader()
                ?.use { it.readText() }
                ?.split("\n")
                ?: return false
            val today = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
            val updated = TaskMarkdown.markFirstMatchingLineDone(lines, targetLine, today) ?: return false
            writeText(context, file.uri, updated.joinToString("\n"))
        } catch (e: Exception) {
            Log.e(TAG, "Failed to tick off task", e)
            false
        }
    }

    // --- A single file picked for one Task List widget (ACTION_OPEN_DOCUMENT, own persisted grant) ---

    fun hasFileAccess(context: Context, documentUri: String): Boolean {
        val uri = Uri.parse(documentUri)
        return context.contentResolver.persistedUriPermissions.any {
            it.uri == uri && it.isReadPermission && it.isWritePermission
        }
    }

    /** Keeps read/write access to a picked file. Returns false when the provider doesn't allow that. */
    fun takeFileAccess(context: Context, documentUri: Uri): Boolean = try {
        context.contentResolver.takePersistableUriPermission(
            documentUri,
            Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
        )
        true
    } catch (_: SecurityException) {
        false
    }

    fun releaseFileAccess(context: Context, documentUri: String) {
        try {
            context.contentResolver.releasePersistableUriPermission(
                Uri.parse(documentUri),
                Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            )
        } catch (_: SecurityException) {
            // Permission was already gone or never persisted.
        }
    }

    /** The picked file's display name (e.g. "Groceries.md"), or null if unavailable. */
    fun displayName(context: Context, documentUri: String): String? = try {
        DocumentFile.fromSingleUri(context, Uri.parse(documentUri))?.name
    } catch (_: Exception) {
        null
    }

    /** Lines of a picked file, or null when it can't be read (deleted, access lost). */
    fun readDocumentLines(context: Context, documentUri: String): List<String>? = try {
        context.contentResolver.openInputStream(Uri.parse(documentUri))
            ?.bufferedReader()
            ?.use { it.readText() }
            ?.split("\n")
    } catch (e: Exception) {
        Log.e(TAG, "Failed to read picked task file", e)
        null
    }

    fun markDoneInDocument(context: Context, documentUri: String, targetLine: String): Boolean {
        val lines = readDocumentLines(context, documentUri) ?: return false
        val today = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
        val updated = TaskMarkdown.markFirstMatchingLineDone(lines, targetLine, today) ?: return false
        return try {
            writeText(context, Uri.parse(documentUri), updated.joinToString("\n"))
        } catch (e: Exception) {
            Log.e(TAG, "Failed to tick off task in picked file", e)
            false
        }
    }

    /**
     * Path of a picked file relative to the vault root, for Obsidian's deep links; null when the
     * file isn't inside the vault folder or comes from a different storage provider.
     */
    fun relativePathInVault(vaultTreeUri: String, documentUri: String): String? = try {
        val tree = Uri.parse(vaultTreeUri)
        val doc = Uri.parse(documentUri)
        if (tree.authority != doc.authority) {
            null
        } else {
            DocumentPaths.relativePath(
                DocumentsContract.getTreeDocumentId(tree),
                DocumentsContract.getDocumentId(doc)
            )
        }
    } catch (_: Exception) {
        null
    }

    /** A document URI for the vault folder itself, to open the file picker there. */
    fun vaultFolderDocumentUri(vaultTreeUri: String): Uri? = try {
        val tree = Uri.parse(vaultTreeUri)
        DocumentsContract.buildDocumentUriUsingTree(tree, DocumentsContract.getTreeDocumentId(tree))
    } catch (_: Exception) {
        null
    }

    private fun writeText(context: Context, uri: Uri, content: String): Boolean {
        val out = context.contentResolver.openOutputStream(uri, "wt") ?: return false
        out.bufferedWriter().use { it.write(content) }
        return true
    }

    private fun vaultRoot(context: Context, location: TaskFileLocation): DocumentFile? {
        val root = DocumentFile.fromTreeUri(context, Uri.parse(location.vaultTreeUri)) ?: return null
        return if (root.exists() && root.isDirectory) root else null
    }

    private fun findFile(root: DocumentFile, location: TaskFileLocation): DocumentFile? {
        var dir = root
        for (segment in location.folder.split('/').filter { it.isNotEmpty() }) {
            dir = dir.findFile(segment)?.takeIf { it.isDirectory } ?: return null
        }
        return dir.findFile(location.fileName)?.takeIf { it.isFile }
    }

    private fun findOrCreateFile(root: DocumentFile, location: TaskFileLocation): DocumentFile? {
        var dir = root
        for (segment in location.folder.split('/').filter { it.isNotEmpty() }) {
            dir = dir.findFile(segment)?.takeIf { it.isDirectory }
                ?: dir.createDirectory(segment)
                ?: return null
        }
        dir.findFile(location.fileName)?.takeIf { it.isFile }?.let { return it }
        // "text/markdown" makes some providers append ".md" themselves, so create with the exact
        // name and an opaque type, which providers keep as-is.
        return dir.createFile("application/octet-stream", location.fileName)
    }
}
