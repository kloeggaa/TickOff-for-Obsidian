package io.github.kloeggaa.tickoff.ui.main

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import io.github.kloeggaa.tickoff.BuildConfig
import io.github.kloeggaa.tickoff.R
import io.github.kloeggaa.tickoff.ui.theme.TickOffScaffold

@Composable
fun AboutScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val openUrl: (String) -> Unit = { url ->
        try {
            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
        } catch (_: ActivityNotFoundException) {
            // No browser installed; the URL is shown as text anyway.
        }
    }
    TickOffScaffold(title = stringResource(R.string.about_title), onBack = onBack) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                stringResource(R.string.app_name) + " " + BuildConfig.VERSION_NAME,
                style = MaterialTheme.typography.titleLarge
            )
            Text(stringResource(R.string.about_disclaimer), fontWeight = FontWeight.Bold)
            Text(stringResource(R.string.about_text))

            Text(stringResource(R.string.privacy_title), style = MaterialTheme.typography.titleMedium)
            Text(stringResource(R.string.privacy_text))

            Text(stringResource(R.string.about_license), style = MaterialTheme.typography.bodyMedium)
            TextButton(onClick = { openUrl(context.getString(R.string.source_url)) }) {
                Text(stringResource(R.string.about_source))
            }
            TextButton(onClick = { openUrl(context.getString(R.string.privacy_url)) }) {
                Text(stringResource(R.string.about_privacy_policy))
            }
            // Empty in the Google Play build (see productFlavors in app/build.gradle.kts).
            if (BuildConfig.DONATE_URL.isNotEmpty()) {
                TextButton(onClick = { openUrl(BuildConfig.DONATE_URL) }) {
                    Text(stringResource(R.string.about_donate))
                }
            }
            Text(stringResource(R.string.about_contact), style = MaterialTheme.typography.bodySmall)
        }
    }
}
