package io.github.kloeggaa.tickoff.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// TickOff's warm plum, taken from the app icon (#5B237B / #75369F, lavender #F2EDF8,
// checklist lines #8F79B2, coral accent #FF8A65). Deliberately a warm, reddish plum on
// light or aubergine surfaces, not Obsidian's cool violet on black.
private val LightColors = lightColorScheme(
    primary = Color(0xFF5B237B),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFEBDCF6),
    onPrimaryContainer = Color(0xFF2A0B3D),
    secondary = Color(0xFF6E5B8C),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFE6DDF2),
    onSecondaryContainer = Color(0xFF2A1F3B),
    tertiary = Color(0xFFB4532F),
    onTertiary = Color.White,
    background = Color(0xFFFBF8FD),
    onBackground = Color(0xFF1E1A22),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF1E1A22),
    surfaceVariant = Color(0xFFEDE5F4),
    onSurfaceVariant = Color(0xFF4A4453),
    outline = Color(0xFF8F79B2),
    errorContainer = Color(0xFFFFDAD6),
    onErrorContainer = Color(0xFF410002)
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFFD7B8F0),
    onPrimary = Color(0xFF3E1257),
    primaryContainer = Color(0xFF5B237B),
    onPrimaryContainer = Color(0xFFF2E1FF),
    secondary = Color(0xFFCBBDE6),
    onSecondary = Color(0xFF33284A),
    secondaryContainer = Color(0xFF4A3D63),
    onSecondaryContainer = Color(0xFFE9DDFF),
    tertiary = Color(0xFFFFB59C),
    onTertiary = Color(0xFF561F0B),
    background = Color(0xFF16111A),
    onBackground = Color(0xFFEAE0EE),
    surface = Color(0xFF1F1824),
    onSurface = Color(0xFFEAE0EE),
    surfaceVariant = Color(0xFF2F2637),
    onSurfaceVariant = Color(0xFFCDC2D6),
    outline = Color(0xFF978BA2),
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
