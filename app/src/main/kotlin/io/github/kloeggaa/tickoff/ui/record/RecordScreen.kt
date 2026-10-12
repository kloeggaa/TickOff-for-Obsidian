package io.github.kloeggaa.tickoff.ui.record

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import io.github.kloeggaa.tickoff.R
import io.github.kloeggaa.tickoff.ui.theme.TickOffScaffold

@Composable
fun RecordScreen(
    viewModel: TaskRecorderViewModel,
    onBack: () -> Unit,
    onOpenSettings: () -> Unit,
    autoStartRecording: Boolean,
    onAutoStartConsumed: () -> Unit
) {
    val isListening by viewModel.isListening.collectAsState()
    val recordingActive by viewModel.recordingActive.collectAsState()
    val isExtractingTasks by viewModel.isExtractingTasks.collectAsState()
    val transcript by viewModel.finalTranscript.collectAsState()
    val taskMarkdown by viewModel.taskMarkdown.collectAsState()
    val lastError by viewModel.lastError.collectAsState()
    val statusText by viewModel.statusText.collectAsState()
    val needsSettings by viewModel.needsSettings.collectAsState()

    DisposableEffect(viewModel) {
        onDispose { viewModel.onScreenClosed() }
    }
    LaunchedEffect(autoStartRecording, recordingActive, isExtractingTasks) {
        if (autoStartRecording && !recordingActive && !isExtractingTasks) {
            viewModel.startCapture()
            onAutoStartConsumed()
        }
    }

    TickOffScaffold(title = stringResource(R.string.record_title), onBack = onBack) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Short recognizer hiccups restart silently; only show errors that need attention.
            val showSttIssue = recordingActive && lastError.isNotBlank() && lastError !in setOf(
                "Client side recognition error",
                "No speech match",
                "Speech timeout",
                "Recognizer busy"
            )
            if (showSttIssue) {
                Text(
                    text = stringResource(R.string.record_stt_issue, lastError),
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            val statusLabel = when {
                isExtractingTasks -> stringResource(R.string.record_status_extracting)
                recordingActive || isListening -> stringResource(R.string.record_status_listening)
                statusText.isNotBlank() -> statusText
                else -> stringResource(R.string.record_status_ready)
            }
            Surface(
                shape = MaterialTheme.shapes.large,
                color = MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        painter = painterResource(
                            if (recordingActive || isListening) R.drawable.ic_mic else R.drawable.ic_play
                        ),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = statusLabel,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 4,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            if (needsSettings && !recordingActive) {
                OutlinedButton(onClick = onOpenSettings, modifier = Modifier.fillMaxWidth()) {
                    Text(stringResource(R.string.open_settings))
                }
            }

            Button(
                onClick = { if (recordingActive) viewModel.stopCapture() else viewModel.startCapture() },
                enabled = !isExtractingTasks,
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (recordingActive) {
                        MaterialTheme.colorScheme.errorContainer
                    } else {
                        MaterialTheme.colorScheme.primary
                    },
                    contentColor = if (recordingActive) {
                        MaterialTheme.colorScheme.onErrorContainer
                    } else {
                        MaterialTheme.colorScheme.onPrimary
                    }
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 52.dp)
            ) {
                Text(stringResource(if (recordingActive) R.string.record_stop else R.string.record_start))
            }

            OutputCard(
                label = stringResource(R.string.record_transcript),
                text = transcript,
                placeholder = stringResource(R.string.record_transcript_hint),
                minHeight = 120
            )
            OutputCard(
                label = stringResource(R.string.record_tasks),
                text = taskMarkdown,
                placeholder = stringResource(R.string.record_tasks_hint),
                minHeight = 140
            )
        }
    }
}

@Composable
private fun OutputCard(label: String, text: String, placeholder: String, minHeight: Int) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(label, style = MaterialTheme.typography.labelLarge)
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = minHeight.dp)
                    .padding(top = 8.dp),
                shape = MaterialTheme.shapes.medium,
                color = MaterialTheme.colorScheme.surface
            ) {
                Box(modifier = Modifier.padding(12.dp)) {
                    Text(
                        text = text.ifBlank { placeholder },
                        style = MaterialTheme.typography.bodyLarge,
                        color = if (text.isBlank()) {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        } else {
                            MaterialTheme.colorScheme.onSurface
                        }
                    )
                }
            }
        }
    }
}
