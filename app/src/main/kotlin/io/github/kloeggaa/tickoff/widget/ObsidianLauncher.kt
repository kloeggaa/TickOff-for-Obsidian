package io.github.kloeggaa.tickoff.widget

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.util.Log
import android.widget.Toast
import io.github.kloeggaa.tickoff.R
import io.github.kloeggaa.tickoff.data.AppSettings
import io.github.kloeggaa.tickoff.storage.VaultFiles
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext

/** What tapping a task row in the Task List widget does. */
enum class OpenMode(val key: String) {
    /** Nothing; only the checkbox reacts. */
    OFF("off"),

    /** Obsidian's built-in `obsidian://open` link. No plugin needed, but repeated taps can open new tabs. */
    OBSIDIAN("obsidian"),

    /** `obsidian://adv-uri` from the Advanced URI community plugin; reopens the note in place. */
    ADVANCED_URI("advanced_uri");

    companion object {
        fun fromKey(key: String?): OpenMode = entries.firstOrNull { it.key == key } ?: OFF
    }
}

/**
 * Opens the task file in the Obsidian app through a deep link. The vault name is the picked vault
 * folder's name and the file path is relative to it, both known from Settings, so no filesystem
 * path has to be guessed.
 */
object ObsidianLauncher {
    private const val TAG = "TickOffObsidian"
    const val OBSIDIAN_PACKAGE = "md.obsidian"

    fun isObsidianInstalled(context: Context): Boolean = try {
        context.packageManager.getPackageInfo(OBSIDIAN_PACKAGE, 0)
        true
    } catch (_: PackageManager.NameNotFoundException) {
        false
    }

    fun buildUri(mode: OpenMode, vaultName: String, relativePath: String): Uri? = when (mode) {
        OpenMode.OFF -> null
        OpenMode.OBSIDIAN -> Uri.Builder()
            .scheme("obsidian")
            .authority("open")
            .appendQueryParameter("vault", vaultName)
            .appendQueryParameter("file", relativePath)
            .build()
        // "adv-uri", not "advanced-uri": Obsidian rejects the latter as an unrecognized action.
        OpenMode.ADVANCED_URI -> Uri.Builder()
            .scheme("obsidian")
            .authority("adv-uri")
            .appendQueryParameter("vault", vaultName)
            .appendQueryParameter("filepath", relativePath)
            .build()
    }

    suspend fun openTaskFile(context: Context, widgetId: Int, mode: OpenMode) {
        if (mode == OpenMode.OFF) return
        val vaultUri = AppSettings(context).vaultTreeUriFlow.first()
        if (vaultUri == null) {
            showToast(context, R.string.obsidian_no_vault)
            return
        }
        if (!isObsidianInstalled(context)) {
            showToast(context, R.string.obsidian_not_installed)
            return
        }
        val relativePath = WidgetTaskFile.relativePathInVault(context, widgetId)
        if (relativePath == null) {
            showToast(context, R.string.obsidian_outside_vault)
            return
        }
        val vaultName = VaultFiles.vaultName(context, vaultUri)
        if (vaultName.isNullOrBlank()) {
            showToast(context, R.string.obsidian_no_vault)
            return
        }
        val uri = buildUri(mode, vaultName, relativePath) ?: return
        // The URI holds the vault name and file path, so only the mode is logged.
        Log.d(TAG, "opening task file in Obsidian, mode=${mode.key}")
        withContext(Dispatchers.Main) {
            try {
                context.startActivity(Intent(Intent.ACTION_VIEW, uri).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            } catch (_: ActivityNotFoundException) {
                Toast.makeText(context, R.string.obsidian_not_installed, Toast.LENGTH_LONG).show()
            }
        }
    }

    private suspend fun showToast(context: Context, messageRes: Int) {
        withContext(Dispatchers.Main) {
            Toast.makeText(context, messageRes, Toast.LENGTH_LONG).show()
        }
    }
}
