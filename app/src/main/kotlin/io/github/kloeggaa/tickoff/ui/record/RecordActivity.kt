package io.github.kloeggaa.tickoff.ui.record

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import io.github.kloeggaa.tickoff.R
import io.github.kloeggaa.tickoff.data.AppSettings
import io.github.kloeggaa.tickoff.speech.SpeechToTextManager
import io.github.kloeggaa.tickoff.ui.main.MainActivity
import io.github.kloeggaa.tickoff.ui.theme.TickOffScaffold
import io.github.kloeggaa.tickoff.ui.theme.TickOffTheme

/** The recorder, opened by the mic widget (recording starts at once) or from the app's home screen. */
class RecordActivity : ComponentActivity() {
    companion object {
        private const val EXTRA_AUTO_START = "io.github.kloeggaa.tickoff.extra.AUTO_START"

        fun createLaunchIntent(context: Context, autoStart: Boolean): Intent =
            Intent(context, RecordActivity::class.java).apply {
                putExtra(EXTRA_AUTO_START, autoStart)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            }
    }

    private var hasMicPermission by mutableStateOf(false)
    private var autoStartRequested by mutableStateOf(false)
    // Bumped on every widget tap so a tap while the screen is open starts a new recording.
    private var launchToken by mutableIntStateOf(0)

    private val requestMicPermission = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted -> hasMicPermission = granted }

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        hasMicPermission = checkMicPermission()
        if (savedInstanceState == null) handleIntent(intent)
        if (!hasMicPermission) requestMicPermission.launch(Manifest.permission.RECORD_AUDIO)

        val appContext = applicationContext
        setContent {
            TickOffTheme {
                if (hasMicPermission) {
                    val vm: TaskRecorderViewModel = viewModel(
                        factory = object : ViewModelProvider.Factory {
                            @Suppress("UNCHECKED_CAST")
                            override fun <T : ViewModel> create(modelClass: Class<T>): T =
                                TaskRecorderViewModel(
                                    appContext,
                                    AppSettings(appContext),
                                    SpeechToTextManager(appContext)
                                ) as T
                        }
                    )
                    RecordScreen(
                        viewModel = vm,
                        onBack = { finish() },
                        onOpenSettings = {
                            startActivity(MainActivity.createSettingsIntent(this))
                        },
                        autoStartRecording = autoStartRequested && launchToken > 0,
                        onAutoStartConsumed = { autoStartRequested = false }
                    )
                } else {
                    MicPermissionScreen(
                        onBack = { finish() },
                        onGrant = { requestMicPermission.launch(Manifest.permission.RECORD_AUDIO) }
                    )
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIntent(intent)
    }

    override fun onResume() {
        super.onResume()
        // The permission may have been granted in system settings meanwhile.
        hasMicPermission = checkMicPermission()
    }

    private fun handleIntent(intent: Intent?) {
        if (intent?.getBooleanExtra(EXTRA_AUTO_START, false) == true) {
            autoStartRequested = true
            launchToken += 1
        }
    }

    private fun checkMicPermission(): Boolean =
        ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) ==
            PackageManager.PERMISSION_GRANTED
}

@androidx.compose.runtime.Composable
private fun MicPermissionScreen(onBack: () -> Unit, onGrant: () -> Unit) {
    TickOffScaffold(title = stringResource(R.string.mic_permission_title), onBack = onBack) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(stringResource(R.string.mic_permission_text), textAlign = TextAlign.Center)
            Spacer(modifier = Modifier.height(16.dp))
            Button(onClick = onGrant) {
                Text(stringResource(R.string.mic_permission_grant))
            }
        }
    }
}
