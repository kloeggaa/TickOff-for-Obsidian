package io.github.kloeggaa.tickoff.ui.main

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
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
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import io.github.kloeggaa.tickoff.R
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
                onSelect = { provider = AppSettings.PROVIDER_OPENAI }
            )
            ProviderOption(
                label = stringResource(R.string.settings_provider_anthropic),
                selected = provider == AppSettings.PROVIDER_ANTHROPIC,
                onSelect = { provider = AppSettings.PROVIDER_ANTHROPIC }
            )
            if (provider == AppSettings.PROVIDER_OPENAI) {
                SecretField(openAiKey, { openAiKey = it }, stringResource(R.string.settings_openai_key))
                OutlinedTextField(
                    value = openAiModel,
                    onValueChange = { openAiModel = it },
                    label = { Text(stringResource(R.string.settings_model)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            } else {
                SecretField(anthropicKey, { anthropicKey = it }, stringResource(R.string.settings_anthropic_key))
                OutlinedTextField(
                    value = anthropicModel,
                    onValueChange = { anthropicModel = it },
                    label = { Text(stringResource(R.string.settings_model)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }

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
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        singleLine = true,
        visualTransformation = PasswordVisualTransformation(),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, autoCorrectEnabled = false),
        modifier = Modifier.fillMaxWidth()
    )
}
