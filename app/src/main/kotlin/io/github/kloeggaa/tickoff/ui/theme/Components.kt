package io.github.kloeggaa.tickoff.ui.theme

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
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
