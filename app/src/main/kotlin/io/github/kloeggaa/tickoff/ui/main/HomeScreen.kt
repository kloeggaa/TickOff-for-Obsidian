package io.github.kloeggaa.tickoff.ui.main

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.os.Build
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.repeatOnLifecycle
import io.github.kloeggaa.tickoff.R
import io.github.kloeggaa.tickoff.data.AppSettings
import io.github.kloeggaa.tickoff.data.TaskWidgetPrefs
import io.github.kloeggaa.tickoff.storage.VaultFiles
import io.github.kloeggaa.tickoff.widget.ParsedTask
import io.github.kloeggaa.tickoff.widget.TaskListSorter
import io.github.kloeggaa.tickoff.widget.TaskListWidgetProvider
import io.github.kloeggaa.tickoff.widget.TaskParser
import io.github.kloeggaa.tickoff.widget.TaskRecordingWidgetProvider
import io.github.kloeggaa.tickoff.ui.theme.TickOffScaffold
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private sealed interface PreviewState {
    data object Loading : PreviewState
    data object NoVault : PreviewState
    data object NoAccess : PreviewState
    data class Loaded(val path: String, val tasks: List<ParsedTask>) : PreviewState
}

@Composable
fun HomeScreen(onRecord: () -> Unit, onOpenSettings: () -> Unit, onOpenAbout: () -> Unit) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var preview by remember { mutableStateOf<PreviewState>(PreviewState.Loading) }

    // Reload whenever the screen comes back, e.g. after Settings or a tick in the widget.
    LaunchedEffect(lifecycleOwner) {
        lifecycleOwner.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            preview = withContext(Dispatchers.IO) {
                val location = AppSettings(context).taskFileLocation()
                when {
                    location == null -> PreviewState.NoVault
                    !VaultFiles.hasVaultAccess(context, location.vaultTreeUri) -> PreviewState.NoAccess
                    else -> {
                        val lines = VaultFiles.readAllLines(context, location)
                        if (lines == null) {
                            PreviewState.NoAccess
                        } else {
                            PreviewState.Loaded(
                                location.relativePath,
                                TaskListSorter.visibleSortedTasks(
                                    TaskParser.parseOpenTasks(lines),
                                    horizonDays = TaskWidgetPrefs.DEFAULT_HORIZON_DAYS
                                )
                            )
                        }
                    }
                }
            }
        }
    }

    TickOffScaffold(title = stringResource(R.string.app_name), onBack = null) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(stringResource(R.string.home_intro), style = MaterialTheme.typography.bodyMedium)

            WidgetPreviewCard(preview, onOpenSettings)

            Button(onClick = onRecord, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.home_record))
            }

            val pinSupported = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O &&
                AppWidgetManager.getInstance(context).isRequestPinAppWidgetSupported
            Text(stringResource(R.string.home_widgets_title), style = MaterialTheme.typography.titleMedium)
            if (pinSupported) {
                OutlinedButton(
                    onClick = { requestPin(context, TaskListWidgetProvider::class.java) },
                    modifier = Modifier.fillMaxWidth()
                ) { Text(stringResource(R.string.home_add_list_widget)) }
                OutlinedButton(
                    onClick = { requestPin(context, TaskRecordingWidgetProvider::class.java) },
                    modifier = Modifier.fillMaxWidth()
                ) { Text(stringResource(R.string.home_add_mic_widget)) }
            } else {
                Text(stringResource(R.string.home_add_widget_manual), style = MaterialTheme.typography.bodyMedium)
            }

            HorizontalDivider()
            OutlinedButton(onClick = onOpenSettings, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.settings_title))
            }
            OutlinedButton(onClick = onOpenAbout, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.about_title))
            }
        }
    }
}

private fun requestPin(context: android.content.Context, provider: Class<*>) {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
    val ok = AppWidgetManager.getInstance(context)
        .requestPinAppWidget(ComponentName(context, provider), null, null)
    if (!ok) Toast.makeText(context, R.string.home_add_widget_manual, Toast.LENGTH_LONG).show()
}

/** Shows the open tasks the way the Task List widget will (default 7-day window). */
@Composable
private fun WidgetPreviewCard(state: PreviewState, onOpenSettings: () -> Unit) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(stringResource(R.string.home_preview_title), style = MaterialTheme.typography.labelLarge)
            when (state) {
                PreviewState.Loading -> Text("…")
                PreviewState.NoVault, PreviewState.NoAccess -> {
                    Text(
                        stringResource(
                            if (state == PreviewState.NoVault) R.string.task_list_no_vault else R.string.task_list_no_access
                        )
                    )
                    Button(onClick = onOpenSettings) { Text(stringResource(R.string.settings_pick_vault)) }
                }
                is PreviewState.Loaded -> {
                    Text(state.path, style = MaterialTheme.typography.bodySmall)
                    if (state.tasks.isEmpty()) {
                        Text(stringResource(R.string.task_list_empty))
                    }
                    state.tasks.take(6).forEach { task ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Image(
                                painterResource(R.drawable.ic_task_checkbox_unchecked),
                                contentDescription = null,
                                modifier = Modifier
                                    .padding(end = 10.dp)
                                    .size(22.dp)
                            )
                            Column {
                                Text(task.text, style = MaterialTheme.typography.bodyMedium)
                                task.dueDate?.let { Text(it, style = MaterialTheme.typography.bodySmall) }
                            }
                        }
                    }
                    if (state.tasks.size > 6) {
                        Text(pluralStringResource(R.plurals.home_preview_more, state.tasks.size - 6, state.tasks.size - 6))
                    }
                }
            }
        }
    }
}
