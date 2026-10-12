package io.github.kloeggaa.tickoff.ui.main

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import io.github.kloeggaa.tickoff.ui.record.RecordActivity
import io.github.kloeggaa.tickoff.ui.theme.TickOffTheme

/** Host app: home with widget preview, settings (vault folder, task file, AI key) and about/privacy. */
class MainActivity : ComponentActivity() {
    companion object {
        private const val EXTRA_SCREEN = "io.github.kloeggaa.tickoff.extra.SCREEN"
        const val SCREEN_HOME = "home"
        const val SCREEN_SETTINGS = "settings"
        const val SCREEN_ABOUT = "about"

        fun createSettingsIntent(context: Context): Intent =
            Intent(context, MainActivity::class.java).putExtra(EXTRA_SCREEN, SCREEN_SETTINGS)
    }

    private var screen by mutableStateOf(SCREEN_HOME)

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        screen = savedInstanceState?.getString(EXTRA_SCREEN)
            ?: intent?.getStringExtra(EXTRA_SCREEN)
            ?: SCREEN_HOME

        setContent {
            TickOffTheme {
                BackHandler(enabled = screen != SCREEN_HOME) { screen = SCREEN_HOME }
                when (screen) {
                    SCREEN_SETTINGS -> SettingsScreen(onBack = { screen = SCREEN_HOME })
                    SCREEN_ABOUT -> AboutScreen(onBack = { screen = SCREEN_HOME })
                    else -> HomeScreen(
                        onRecord = { startActivity(RecordActivity.createLaunchIntent(this, autoStart = false)) },
                        onOpenSettings = { screen = SCREEN_SETTINGS },
                        onOpenAbout = { screen = SCREEN_ABOUT },
                        onClose = { finish() }
                    )
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        intent.getStringExtra(EXTRA_SCREEN)?.let { screen = it }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putString(EXTRA_SCREEN, screen)
    }
}
