package io.github.kloeggaa.tickoff.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// TickOff's own "ticked-off" green. Deliberately unrelated to Obsidian's branding.
private val LightColors = lightColorScheme(
    primary = Color(0xFF2E6B4F),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFB3F0CF),
    onPrimaryContainer = Color(0xFF002114),
    secondary = Color(0xFF4D6357),
    background = Color(0xFFF6F8F5),
    onBackground = Color(0xFF171D19),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF171D19),
    surfaceVariant = Color(0xFFDCE5DD),
    onSurfaceVariant = Color(0xFF404943),
    errorContainer = Color(0xFFFFDAD6),
    onErrorContainer = Color(0xFF410002)
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFF8FD5AE),
    onPrimary = Color(0xFF003823),
    primaryContainer = Color(0xFF125237),
    onPrimaryContainer = Color(0xFFB3F0CF),
    secondary = Color(0xFFB4CCBD),
    background = Color(0xFF0F1512),
    onBackground = Color(0xFFDEE4DF),
    surface = Color(0xFF171D19),
    onSurface = Color(0xFFDEE4DF),
    surfaceVariant = Color(0xFF29322C),
    onSurfaceVariant = Color(0xFFC0C9C1),
    errorContainer = Color(0xFF93000A),
    onErrorContainer = Color(0xFFFFDAD6)
)

/** Follows the phone's light/dark setting. */
@Composable
fun TickOffTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (isSystemInDarkTheme()) DarkColors else LightColors,
        content = content
    )
}
