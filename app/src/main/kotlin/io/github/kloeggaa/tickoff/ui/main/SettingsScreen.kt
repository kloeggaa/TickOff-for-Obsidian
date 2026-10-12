package io.github.kloeggaa.tickoff.ui.main

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import io.github.kloeggaa.tickoff.R
import io.github.kloeggaa.tickoff.api.AnthropicClient
import io.github.kloeggaa.tickoff.api.KeyCheckOutcome
import io.github.kloeggaa.tickoff.api.KeyCheckResult
import io.github.kloeggaa.tickoff.api.ModelListResult
import io.github.kloeggaa.tickoff.api.OpenAIClient
import io.github.kloeggaa.tickoff.data.AppSettings
import io.github.kloeggaa.tickoff.storage.VaultFiles
import io.github.kloeggaa.tickoff.ui.theme.TickOffScaffold
import io.github.kloeggaa.tickoff.widget.TaskListWidgetProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun SettingsScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val settings = remember { AppSettings(context.applicationContext) }
    val scope = rememberCoroutineScope()

    var loaded by remember { mutableStateOf(false) }
    var vaultUri by remember { mutableStateOf<String?>(null) }
    var vaultName by remember { mutableStateOf<String?>(null) }
    var folder by remember { mutableStateOf("") }
    var fileName by remember { mutableStateOf("") }
    var provider by remember { mutableStateOf(AppSettings.PROVIDER_OPENAI) }
    var openAiKey by remember { mutableStateOf("") }
    var openAiModel by remember { mutableStateOf("") }
    var anthropicKey by remember { mutableStateOf("") }
    var anthropicModel by remember { mutableStateOf("") }
    var keyCheckRunning by remember { mutableStateOf(false) }
    var keyCheckResult by remember { mutableStateOf<KeyCheckResult?>(null) }
    var modelListRunning by remember { mutableStateOf(false) }
    var modelListResult by remember { mutableStateOf<ModelListResult?>(null) }
    var loadedModels by remember { mutableStateOf(emptyList<String>()) }
    var modelMenuOpen by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        vaultUri = settings.vaultTreeUriFlow.first()
        vaultName = vaultUri?.let { withContext(Dispatchers.IO) { VaultFiles.vaultName(context, it) } }
        folder = settings.taskFolderFlow.first()
        fileName = settings.taskFileNameFlow.first()
        provider = settings.aiProviderFlow.first()
        openAiKey = settings.openAiApiKeyFlow.first().orEmpty()
        openAiModel = settings.openAiModelFlow.first()
        anthropicKey = settings.anthropicApiKeyFlow.first().orEmpty()
        anthropicModel = settings.anthropicModelFlow.first()
        loaded = true
    }

    val pickVault = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri: Uri? ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            try {
                VaultFiles.takeVaultAccess(context, uri, vaultUri)
                settings.saveVaultTreeUri(uri.toString())
                vaultUri = uri.toString()
                vaultName = withContext(Dispatchers.IO) { VaultFiles.vaultName(context, uri.toString()) }
                withContext(Dispatchers.IO) { TaskListWidgetProvider.refreshAll(context) }
            } catch (_: SecurityException) {
                Toast.makeText(context, R.string.settings_vault_failed, Toast.LENGTH_LONG).show()
            }
        }
    }

    TickOffScaffold(title = stringResource(R.string.settings_title), onBack = onBack) { padding ->
        if (!loaded) return@TickOffScaffold
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(stringResource(R.string.settings_vault_section), style = MaterialTheme.typography.titleMedium)
            Text(stringResource(R.string.settings_vault_hint), style = MaterialTheme.typography.bodySmall)
            Text(
                vaultName?.let { stringResource(R.string.settings_vault_current, it) }
                    ?: stringResource(R.string.settings_vault_none),
                style = MaterialTheme.typography.bodyLarge
            )
            Button(onClick = { pickVault.launch(vaultUri?.let(Uri::parse)) }) {
                Text(stringResource(R.string.settings_pick_vault))
            }
            OutlinedTextField(
                value = folder,
                onValueChange = { folder = it },
                label = { Text(stringResource(R.string.settings_folder_label)) },
                supportingText = { Text(stringResource(R.string.settings_folder_hint)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = fileName,
                onValueChange = { fileName = it },
                label = { Text(stringResource(R.string.settings_file_label)) },
                supportingText = { Text(stringResource(R.string.settings_file_hint, AppSettings.DEFAULT_TASK_FILE_NAME)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            HorizontalDivider()
            Text(stringResource(R.string.settings_ai_section), style = MaterialTheme.typography.titleMedium)
            Text(stringResource(R.string.settings_ai_hint), style = MaterialTheme.typography.bodySmall)
            ProviderOption(
                label = stringResource(R.string.settings_provider_openai),
                selected = provider == AppSettings.PROVIDER_OPENAI,
                onSelect = {
                    provider = AppSettings.PROVIDER_OPENAI
                    keyCheckResult = null; modelListResult = null; loadedModels = emptyList()
                }
            )
            ProviderOption(
                label = stringResource(R.string.settings_provider_anthropic),
                selected = provider == AppSettings.PROVIDER_ANTHROPIC,
                onSelect = {
                    provider = AppSettings.PROVIDER_ANTHROPIC
                    keyCheckResult = null; modelListResult = null; loadedModels = emptyList()
                }
            )
            val isAnthropic = provider == AppSettings.PROVIDER_ANTHROPIC
            if (isAnthropic) {
                SecretField(
                    anthropicKey,
                    { anthropicKey = it; keyCheckResult = null; modelListResult = null; loadedModels = emptyList() },
                    stringResource(R.string.settings_anthropic_key)
                )
                ModelField(
                    anthropicModel,
                    { anthropicModel = it; keyCheckResult = null },
                    loadedModels,
                    modelMenuOpen
                ) { modelMenuOpen = it }
            } else {
                SecretField(
                    openAiKey,
                    { openAiKey = it; keyCheckResult = null; modelListResult = null; loadedModels = emptyList() },
                    stringResource(R.string.settings_openai_key)
                )
                ModelField(
                    openAiModel,
                    { openAiModel = it; keyCheckResult = null },
                    loadedModels,
                    modelMenuOpen
                ) { modelMenuOpen = it }
            }

            val keyToTest = (if (isAnthropic) anthropicKey else openAiKey).trim()
            val modelToTest = (if (isAnthropic) anthropicModel else openAiModel).trim()
                .ifBlank { if (isAnthropic) AnthropicClient.DEFAULT_MODEL else OpenAIClient.DEFAULT_MODEL }
            val busy = keyCheckRunning || modelListRunning
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(
                    enabled = keyToTest.isNotEmpty() && !busy,
                    onClick = {
                        scope.launch {
                            keyCheckRunning = true
                            keyCheckResult = null
                            modelListResult = null
                            keyCheckResult = withContext(Dispatchers.IO) {
                                if (isAnthropic) {
                                    AnthropicClient(keyToTest).checkKey(modelToTest)
                                } else {
                                    OpenAIClient(keyToTest).checkKey(modelToTest)
                                }
                            }
                            keyCheckRunning = false
                        }
                    }
                ) { Text(stringResource(R.string.settings_test_key)) }
                OutlinedButton(
                    enabled = keyToTest.isNotEmpty() && !busy,
                    onClick = {
                        scope.launch {
                            modelListRunning = true
                            keyCheckResult = null
                            modelListResult = null
                            val result = withContext(Dispatchers.IO) {
                                if (isAnthropic) AnthropicClient(keyToTest).listModels() else OpenAIClient(keyToTest).listModels()
                            }
                            modelListResult = result
                            loadedModels = result.models
                            modelMenuOpen = result.models.isNotEmpty()
                            modelListRunning = false
                        }
                    }
                ) { Text(stringResource(R.string.settings_load_models)) }
                if (busy) CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
            }
            keyCheckResult?.let { result ->
                StatusText(keyCheckMessage(result, modelToTest), ok = result.outcome == KeyCheckOutcome.OK)
            }
            modelListResult?.let { result ->
                val ok = result.check.outcome == KeyCheckOutcome.OK
                val message = when {
                    !ok -> keyCheckMessage(result.check, modelToTest)
                    result.models.isEmpty() -> stringResource(R.string.models_none)
                    else -> pluralStringResource(R.plurals.models_loaded, result.models.size, result.models.size)
                }
                StatusText(message, ok = ok && result.models.isNotEmpty())
            }
            Text(stringResource(R.string.settings_test_key_hint), style = MaterialTheme.typography.bodySmall)

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Button(onClick = {
                    scope.launch {
                        settings.saveTaskFile(folder, fileName)
                        settings.saveAi(provider, openAiKey, openAiModel, anthropicKey, anthropicModel)
                        folder = settings.taskFolderFlow.first()
                        fileName = settings.taskFileNameFlow.first()
                        withContext(Dispatchers.IO) { TaskListWidgetProvider.refreshAll(context) }
                        Toast.makeText(context, R.string.settings_saved, Toast.LENGTH_SHORT).show()
                    }
                }) { Text(stringResource(R.string.save)) }
                OutlinedButton(onClick = onBack) { Text(stringResource(R.string.cancel)) }
            }
        }
    }
}

@Composable
private fun ProviderOption(label: String, selected: Boolean, onSelect: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .selectable(selected = selected, onClick = onSelect, role = Role.RadioButton)
    ) {
        RadioButton(selected = selected, onClick = null)
        Text(label, modifier = Modifier.padding(start = 8.dp))
    }
}

@Composable
private fun SecretField(value: String, onValueChange: (String) -> Unit, label: String) {
    var visible by remember { mutableStateOf(false) }
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        singleLine = true,
        visualTransformation = if (visible) VisualTransformation.None else PasswordVisualTransformation(),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, autoCorrectEnabled = false),
        trailingIcon = {
            IconButton(onClick = { visible = !visible }) {
                Icon(
                    painterResource(if (visible) R.drawable.ic_visibility_off else R.drawable.ic_visibility),
                    contentDescription = stringResource(if (visible) R.string.settings_hide_key else R.string.settings_show_key)
                )
            }
        },
        modifier = Modifier.fillMaxWidth()
    )
}

@Composable
private fun keyCheckMessage(result: KeyCheckResult, model: String): String = when (result.outcome) {
    KeyCheckOutcome.OK -> stringResource(R.string.key_check_ok, model)
    KeyCheckOutcome.INVALID_KEY -> stringResource(R.string.key_check_invalid)
    KeyCheckOutcome.NO_ACCESS -> stringResource(R.string.key_check_no_access, model)
    KeyCheckOutcome.MODEL_NOT_FOUND -> stringResource(R.string.key_check_model_not_found, model)
    KeyCheckOutcome.RATE_LIMITED -> stringResource(R.string.key_check_rate_limited)
    KeyCheckOutcome.SERVER_ERROR -> stringResource(R.string.key_check_server_error, result.httpCode ?: 0)
    KeyCheckOutcome.NETWORK_ERROR -> stringResource(R.string.key_check_network)
    KeyCheckOutcome.OTHER -> stringResource(R.string.key_check_other, result.httpCode?.toString() ?: "?")
}

@Composable
private fun StatusText(text: String, ok: Boolean) {
    Text(
        text = text,
        color = if (ok) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
        style = MaterialTheme.typography.bodyMedium
    )
}

/** Model name field; once models are loaded, the arrow opens a list to pick from. Typing stays possible. */
@Composable
private fun ModelField(
    value: String,
    onValueChange: (String) -> Unit,
    models: List<String>,
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit
) {
    Box {
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            label = { Text(stringResource(R.string.settings_model)) },
            singleLine = true,
            trailingIcon = if (models.isEmpty()) {
                null
            } else {
                {
                    IconButton(onClick = { onExpandedChange(!expanded) }) {
                        Icon(
                            painterResource(R.drawable.ic_arrow_drop_down),
                            contentDescription = stringResource(R.string.settings_pick_model)
                        )
                    }
                }
            },
            modifier = Modifier.fillMaxWidth()
        )
        DropdownMenu(expanded = expanded && models.isNotEmpty(), onDismissRequest = { onExpandedChange(false) }) {
            models.forEach { id ->
                DropdownMenuItem(
                    text = { Text(id) },
                    onClick = {
                        onValueChange(id)
                        onExpandedChange(false)
                    }
                )
            }
        }
    }
}
