package com.beno.example.urllauncher

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.beno.subfeatures.InitEvent
import com.beno.subfeatures.use

// ─── Screen (entry point) ────────────────────────────────────────────────────

@Composable
fun UrlLauncherScreen(
    vm: UrlLauncherViewModel = viewModel(),
) {
    val context = LocalContext.current
    var toastMessage by remember { mutableStateOf<String?>(null) }

    val state = vm.use(initEvent = InitEvent.Once(UrlLauncherContract.Action.ViewReady)) { effect ->
        when (effect) {
            is UrlLauncherContract.SideEffect.OpenUrl -> {
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(effect.url))
                context.startActivity(intent)
            }
            is UrlLauncherContract.SideEffect.ShowMessage -> {
                toastMessage = effect.text
            }
        }
    }

    UrlLauncherContent(
        state = state,
        onOpenClick = { vm.onAction(UrlLauncherContract.Action.OpenUrlClicked) },
    )

    // ── Toast / Snackbar ──────────────────────────────────────────────────────
    toastMessage?.let { message ->
        AlertDialog(
            onDismissRequest = { toastMessage = null },
            confirmButton = {
                TextButton(onClick = { toastMessage = null }) { Text("OK") }
            },
            text = { Text(message) },
        )
    }
}

// ─── Stateless content ───────────────────────────────────────────────────────

@Composable
private fun UrlLauncherContent(
    state: UrlLauncherContract.MyUiState,
    onOpenClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        topBar = {
            TopAppBar(title = { Text("URL Launcher") })
        }
    ) { innerPadding ->
        Box(
            modifier = modifier
                .padding(innerPadding)
                .fillMaxSize()
                .padding(24.dp),
            contentAlignment = Alignment.Center,
        ) {
            if (state.isLoading) {
                CircularProgressIndicator()
            } else {
                Column(
                    verticalArrangement = Arrangement.spacedBy(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(
                        text = state.url,
                        style = MaterialTheme.typography.bodyMedium,
                    )

                    Button(onClick = onOpenClick) {
                        Text("Open URL")
                    }

                    Text(
                        text = "Opened ${state.openCount} time(s)",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

// ─── Preview ─────────────────────────────────────────────────────────────────

@Preview(showBackground = true)
@Composable
private fun UrlLauncherPreview() {
    MaterialTheme {
        UrlLauncherContent(
            state = UrlLauncherContract.MyUiState(url = "https://example.com", openCount = 3),
            onOpenClick = {},
        )
    }
}
