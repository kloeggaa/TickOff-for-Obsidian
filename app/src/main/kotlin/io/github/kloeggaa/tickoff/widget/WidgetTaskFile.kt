package io.github.kloeggaa.tickoff.widget

import android.content.Context
import io.github.kloeggaa.tickoff.data.AppSettings
import io.github.kloeggaa.tickoff.data.TaskWidgetPrefs
import io.github.kloeggaa.tickoff.storage.VaultFiles
import kotlinx.coroutines.flow.first

/**
 * The file one Task List widget shows: the file picked at widget setup, or else the default task
 * file from Settings. Hides that difference from the widget's rendering and click handling.
 */
object WidgetTaskFile {

    enum class Status { OK, NO_VAULT, NO_ACCESS }

    data class Info(
        /** Header text: path inside the vault, or the file name when the file lies elsewhere. */
        val title: String?,
        val status: Status
    )

    suspend fun info(context: Context, widgetId: Int): Info {
        val settings = AppSettings(context)
        val picked = TaskWidgetPrefs.getWidgetFileUri(context, widgetId)
        if (picked != null) {
            val vault = settings.vaultTreeUriFlow.first()
            val title = vault?.let { VaultFiles.relativePathInVault(it, picked) }
                ?: VaultFiles.displayName(context, picked)
            val status = if (VaultFiles.hasFileAccess(context, picked)) Status.OK else Status.NO_ACCESS
            return Info(title, status)
        }
        val location = settings.taskFileLocation() ?: return Info(null, Status.NO_VAULT)
        val status = if (VaultFiles.hasVaultAccess(context, location.vaultTreeUri)) Status.OK else Status.NO_ACCESS
        return Info(location.relativePath, status)
    }

    suspend fun readLines(context: Context, widgetId: Int): List<String> {
        val picked = TaskWidgetPrefs.getWidgetFileUri(context, widgetId)
        if (picked != null) return VaultFiles.readDocumentLines(context, picked).orEmpty()
        val location = AppSettings(context).taskFileLocation() ?: return emptyList()
        return VaultFiles.readAllLines(context, location).orEmpty()
    }

    suspend fun markDone(context: Context, widgetId: Int, line: String): Boolean {
        val picked = TaskWidgetPrefs.getWidgetFileUri(context, widgetId)
        if (picked != null) return VaultFiles.markDoneInDocument(context, picked, line)
        val location = AppSettings(context).taskFileLocation() ?: return false
        return VaultFiles.markDone(context, location, line)
    }

    /** Path of the widget's file relative to the vault root, or null when it isn't inside the vault. */
    suspend fun relativePathInVault(context: Context, widgetId: Int): String? {
        val settings = AppSettings(context)
        val picked = TaskWidgetPrefs.getWidgetFileUri(context, widgetId)
            ?: return settings.taskFileLocation()?.relativePath
        val vault = settings.vaultTreeUriFlow.first() ?: return null
        return VaultFiles.relativePathInVault(vault, picked)
    }
}
