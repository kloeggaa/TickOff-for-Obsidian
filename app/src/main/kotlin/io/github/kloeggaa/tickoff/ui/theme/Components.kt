package io.github.kloeggaa.tickoff.ui.theme

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import io.github.kloeggaa.tickoff.BuildConfig
import io.github.kloeggaa.tickoff.R

/** Screen scaffold with a title, an optional back arrow and an optional close button. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TickOffScaffold(
    title: String,
    onBack: (() -> Unit)?,
    onClose: (() -> Unit)? = null,
    content: @Composable (PaddingValues) -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(title) },
                navigationIcon = {
                    if (onBack != null) {
                        IconButton(onClick = onBack) {
                            Icon(
                                painterResource(R.drawable.ic_arrow_back),
                                contentDescription = stringResource(R.string.back)
                            )
                        }
                    }
                },
                actions = {
                    if (onClose != null) {
                        IconButton(onClick = onClose) {
                            Icon(
                                painterResource(R.drawable.ic_close),
                                contentDescription = stringResource(R.string.close)
                            )
                        }
                    }
                }
            )
        },
        content = content
    )
}

/**
 * Prominent "Support development" button in the coral of the app icon. Shown only in builds that
 * carry a donation link (F-Droid); the Google Play build has none (see productFlavors).
 */
@Composable
fun DonateButton(modifier: Modifier = Modifier) {
    if (BuildConfig.DONATE_URL.isEmpty()) return
    val context = LocalContext.current
    Button(
        onClick = {
            try {
                context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(BuildConfig.DONATE_URL)))
            } catch (_: ActivityNotFoundException) {
                // No browser installed.
            }
        },
        // Dark text on coral keeps the contrast readable in light and dark mode.
        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF8A65), contentColor = Color(0xFF3B1400)),
        modifier = modifier.fillMaxWidth()
    ) {
        Text(stringResource(R.string.about_donate))
    }
}
