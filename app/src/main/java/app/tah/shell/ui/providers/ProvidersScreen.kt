package app.tah.shell.ui.providers

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import app.tah.shell.ui.theme.TahOutline
import app.tah.shell.ui.theme.TahSurfaceContainer

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProvidersScreen() {
    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Providers") })
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            ProviderStubCard(
                title = "BYOK",
                body = "Bring your own key — OpenAI / Anthropic / others. Config UI in M1.",
            )
            ProviderStubCard(
                title = "Ollama",
                body = "Local models via Ollama. Connection + model pick lands in M1.",
            )
        }
    }
}

@Composable
private fun ProviderStubCard(title: String, body: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(TahSurfaceContainer)
            .border(1.dp, TahOutline, RoundedCornerShape(12.dp))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(title, style = MaterialTheme.typography.titleMedium)
        Text(
            body,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
