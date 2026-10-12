package io.github.kloeggaa.tickoff.widget

import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.DocumentsContract
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContract
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.lifecycleScope
import io.github.kloeggaa.tickoff.R
import io.github.kloeggaa.tickoff.data.AppSettings
import io.github.kloeggaa.tickoff.data.TaskWidgetPrefs
import io.github.kloeggaa.tickoff.storage.VaultFiles
import io.github.kloeggaa.tickoff.ui.main.MainActivity
import io.github.kloeggaa.tickoff.ui.theme.TickOffScaffold
import io.github.kloeggaa.tickoff.ui.theme.TickOffTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * The `android:configure` target for the Task List widget, shown when the widget is placed (and,
 * on Android 12+, when the user long-presses it and picks "reconfigure"). Asks which file the
 * widget shows (the default task file or any file picked here), the due-date window, and whether
 * tapping a task opens the file in Obsidian.
 */
class TaskListWidgetConfigActivity : ComponentActivity() {

    private var appWidgetId = AppWidgetManager.INVALID_APPWIDGET_ID

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        // If the user backs out, the widget host must cancel the placement rather than
        // leave an unconfigured widget on the home screen.
        setResult(RESULT_CANCELED)

        appWidgetId = intent?.extras
            ?.getInt(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID)
            ?: AppWidgetManager.INVALID_APPWIDGET_ID
        if (appWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
            finish()
            return
        }

        lifecycleScope.launch {
            val context = applicationContext
            val settings = AppSettings(context)
            val initial = ConfigState(
                vaultTreeUri = settings.vaultTreeUriFlow.first(),
                defaultPath = settings.taskFileLocation()?.relativePath,
                savedFileUri = TaskWidgetPrefs.getWidgetFileUri(context, appWidgetId),
                days = TaskWidgetPrefs.getWidgetHorizonDays(context, appWidgetId),
                mode = TaskWidgetPrefs.getWidgetOpenMode(context, appWidgetId),
                obsidianInstalled = ObsidianLauncher.isObsidianInstalled(context)
            )
            setContent {
                TickOffTheme {
                    ConfigScreen(
                        initial = initial,
                        onOpenSettings = {
                            startActivity(MainActivity.createSettingsIntent(this@TaskListWidgetConfigActivity))
                        },
                        onCancel = { pickedFileUri -> cancelSetup(initial.savedFileUri, pickedFileUri) },
                        onSave = { fileUri, days, mode -> finishSetup(initial.savedFileUri, fileUri, days, mode) }
                    )
                }
            }
        }
    }

    private fun finishSetup(savedFileUri: String?, fileUri: String?, horizonDays: Int, openMode: OpenMode) {
        lifecycleScope.launch {
            TaskWidgetPrefs.setWidgetConfig(applicationContext, appWidgetId, fileUri, horizonDays, openMode)
            if (savedFileUri != null && savedFileUri != fileUri) {
                releaseIfUnused(savedFileUri)
            }
            val appWidgetManager = AppWidgetManager.getInstance(applicationContext)
            TaskListWidgetProvider.updateWidget(applicationContext, appWidgetManager, appWidgetId)
            @Suppress("DEPRECATION")
            appWidgetManager.notifyAppWidgetViewDataChanged(appWidgetId, R.id.task_list_view)

            setResult(RESULT_OK, Intent().putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId))
            finish()
        }
    }

    /** Drops the access taken for a file picked in this session but then not saved. */
    private fun cancelSetup(savedFileUri: String?, pickedFileUri: String?) {
        lifecycleScope.launch {
            if (pickedFileUri != null && pickedFileUri != savedFileUri) {
                releaseIfUnused(pickedFileUri)
            }
            finish()
        }
    }

    private suspend fun releaseIfUnused(fileUri: String) {
        if (!TaskWidgetPrefs.isFileUsedByOtherWidget(applicationContext, appWidgetId, fileUri)) {
            VaultFiles.releaseFileAccess(applicationContext, fileUri)
        }
    }
}

private data class ConfigState(
    val vaultTreeUri: String?,
    /** The default task file's path inside the vault, or null while no vault folder is chosen. */
    val defaultPath: String?,
    val savedFileUri: String?,
    val days: Int,
    val mode: OpenMode,
    val obsidianInstalled: Boolean
)

/** Shows the picked file's path inside the vault, or just its name when it lies elsewhere. */
private fun describeFile(context: Context, vaultTreeUri: String?, fileUri: String): String =
    vaultTreeUri?.let { VaultFiles.relativePathInVault(it, fileUri) }
        ?: VaultFiles.displayName(context, fileUri)
        ?: fileUri

@Composable
private fun ConfigScreen(
    initial: ConfigState,
    onOpenSettings: () -> Unit,
    onCancel: (pickedFileUri: String?) -> Unit,
    onSave: (fileUri: String?, days: Int, mode: OpenMode) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var fileUri by remember { mutableStateOf(initial.savedFileUri) }
    var useOwnFile by remember { mutableStateOf(initial.savedFileUri != null) }
    var fileLabel by remember {
        mutableStateOf(initial.savedFileUri?.let { describeFile(context, initial.vaultTreeUri, it) })
    }
    var pickError by remember { mutableStateOf(false) }
    var daysText by remember { mutableStateOf(initial.days.toString()) }
    var mode by remember { mutableStateOf(initial.mode) }

    val pickFile = rememberLauncherForActivityResult(
        OpenDocumentWithInitialUri(initial.vaultTreeUri?.let(VaultFiles::vaultFolderDocumentUri))
    ) { uri: Uri? ->
        if (uri == null) {
            if (fileUri == null) useOwnFile = false
            return@rememberLauncherForActivityResult
        }
        scope.launch {
            val kept = withContext(Dispatchers.IO) { VaultFiles.takeFileAccess(context, uri) }
            if (!kept) {
                pickError = true
                if (fileUri == null) useOwnFile = false
                return@launch
            }
            pickError = false
            // A file picked earlier in this session but replaced now is no longer needed.
            fileUri?.takeIf { it != initial.savedFileUri && it != uri.toString() }?.let {
                VaultFiles.releaseFileAccess(context, it)
            }
            fileUri = uri.toString()
            useOwnFile = true
            fileLabel = withContext(Dispatchers.IO) { describeFile(context, initial.vaultTreeUri, uri.toString()) }
        }
    }

    // Opening in Obsidian needs the file's path inside the vault.
    val chosenFile = if (useOwnFile) fileUri else null
    val obsidianPossible = if (chosenFile != null) {
        initial.vaultTreeUri?.let { VaultFiles.relativePathInVault(it, chosenFile) } != null
    } else {
        initial.defaultPath != null
    }

    BackHandler { onCancel(fileUri) }
    TickOffScaffold(title = stringResource(R.string.config_title), onBack = { onCancel(fileUri) }) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(stringResource(R.string.config_file_title), style = MaterialTheme.typography.titleMedium)
            Option(
                label = stringResource(R.string.config_file_default),
                hint = initial.defaultPath ?: stringResource(R.string.config_file_default_unset),
                selected = !useOwnFile,
                enabled = true
            ) { useOwnFile = false }
            Option(
                label = stringResource(R.string.config_file_own),
                hint = fileLabel ?: stringResource(R.string.config_file_own_hint),
                selected = useOwnFile,
                enabled = true
            ) {
                useOwnFile = true
                if (fileUri == null) pickFile.launch(arrayOf("text/markdown", "text/plain", "*/*"))
            }
            if (useOwnFile && fileUri != null) {
                OutlinedButton(onClick = { pickFile.launch(arrayOf("text/markdown", "text/plain", "*/*")) }) {
                    Text(stringResource(R.string.config_file_change))
                }
            }
            if (pickError) {
                Text(stringResource(R.string.config_file_no_access), color = MaterialTheme.colorScheme.error)
            }
            if (!useOwnFile && initial.defaultPath == null) {
                OutlinedButton(onClick = onOpenSettings) { Text(stringResource(R.string.open_settings)) }
            }

            HorizontalDivider()
            Text(stringResource(R.string.task_list_due_window_title), style = MaterialTheme.typography.titleMedium)
            Text(stringResource(R.string.task_list_due_window_hint), style = MaterialTheme.typography.bodySmall)
            OutlinedTextField(
                value = daysText,
                onValueChange = { input -> daysText = input.filter(Char::isDigit).take(4) },
                label = { Text(stringResource(R.string.task_list_due_window_label)) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            HorizontalDivider()
            Text(stringResource(R.string.config_open_title), style = MaterialTheme.typography.titleMedium)
            Text(stringResource(R.string.config_open_hint), style = MaterialTheme.typography.bodySmall)
            if (!obsidianPossible) {
                Text(stringResource(R.string.config_open_outside_vault), color = MaterialTheme.colorScheme.error)
            } else if (!initial.obsidianInstalled) {
                Text(stringResource(R.string.config_obsidian_missing), color = MaterialTheme.colorScheme.error)
            }
            val effectiveMode = if (obsidianPossible) mode else OpenMode.OFF
            Option(stringResource(R.string.config_open_off), null, effectiveMode == OpenMode.OFF, true) {
                mode = OpenMode.OFF
            }
            Option(
                stringResource(R.string.config_open_obsidian),
                stringResource(R.string.config_open_obsidian_hint),
                effectiveMode == OpenMode.OBSIDIAN,
                obsidianPossible
            ) { mode = OpenMode.OBSIDIAN }
            Option(
                stringResource(R.string.config_open_adv),
                stringResource(R.string.config_open_adv_hint),
                effectiveMode == OpenMode.ADVANCED_URI,
                obsidianPossible
            ) { mode = OpenMode.ADVANCED_URI }

            Button(
                onClick = {
                    val days = daysText.toIntOrNull()?.coerceAtLeast(0) ?: TaskWidgetPrefs.DEFAULT_HORIZON_DAYS
                    if (useOwnFile && fileUri == null) {
                        pickFile.launch(arrayOf("text/markdown", "text/plain", "*/*"))
                    } else {
                        // An own file picked earlier but switched back to the default is released on save.
                        val unused = fileUri?.takeIf { !useOwnFile && it != initial.savedFileUri }
                        if (unused != null) VaultFiles.releaseFileAccess(context, unused)
                        onSave(chosenFile, days, effectiveMode)
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) { Text(stringResource(R.string.config_add)) }
        }
    }
}

@Composable
private fun Option(label: String, hint: String?, selected: Boolean, enabled: Boolean, onSelect: () -> Unit) {
    Row(
        verticalAlignment = Alignment.Top,
        modifier = Modifier
            .fillMaxWidth()
            .selectable(selected = selected, enabled = enabled, onClick = onSelect, role = Role.RadioButton)
            .padding(vertical = 4.dp)
    ) {
        RadioButton(selected = selected, onClick = null, enabled = enabled)
        Column(modifier = Modifier.padding(start = 8.dp)) {
            Text(
                label,
                color = if (enabled) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (hint != null) Text(hint, style = MaterialTheme.typography.bodySmall)
        }
    }
}

/** ACTION_OPEN_DOCUMENT that starts in the vault folder when one is known. */
private class OpenDocumentWithInitialUri(
    private val initialUri: Uri?
) : ActivityResultContract<Array<String>, Uri?>() {

    private val delegate = ActivityResultContracts.OpenDocument()

    override fun createIntent(context: Context, input: Array<String>): Intent {
        return delegate.createIntent(context, input).apply {
            if (initialUri != null) {
                putExtra(DocumentsContract.EXTRA_INITIAL_URI, initialUri)
            }
        }
    }

    override fun parseResult(resultCode: Int, intent: Intent?): Uri? {
        return delegate.parseResult(resultCode, intent)
    }
}
