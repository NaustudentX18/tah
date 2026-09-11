package app.tah.shell.ui.providers

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import app.tah.shell.TahApplication
import app.tah.shell.data.ProviderKind
import app.tah.shell.data.ProviderRepository
import app.tah.shell.ui.TahVmFactory
import app.tah.shell.ui.components.OfflineCapabilityBadge
import app.tah.shell.ui.theme.TahOutline
import app.tah.shell.ui.theme.TahPrimary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProvidersScreen() {
    val app = LocalContext.current.applicationContext as TahApplication
    val vm: ProvidersViewModel = viewModel(factory = TahVmFactory(app.container))
    val snap by vm.snapshot.collectAsStateWithLifecycle()
    val probing by vm.probing.collectAsStateWithLifecycle()
    var kind by rememberSaveable { mutableStateOf(snap.kind.takeIf { it != ProviderKind.Demo } ?: ProviderKind.Byok) }
    var baseUrl by rememberSaveable { mutableStateOf(snap.baseUrl.ifBlank { ProviderRepository.defaultUrl(kind) }) }
    var modelId by rememberSaveable { mutableStateOf(snap.modelId.ifBlank { ProviderRepository.defaultModel(kind) }) }
    var apiKey by rememberSaveable { mutableStateOf("") }

    Scaffold(topBar = { TopAppBar(title = { Text("Providers & Keys") }) }) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            OfflineCapabilityBadge(app.container.providers.capabilityCopy(offline = !snap.isLive))
            Text("Path", style = MaterialTheme.typography.titleSmall)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                PathCard("BYOK", "OpenAI-compatible", kind == ProviderKind.Byok) {
                    kind = ProviderKind.Byok
                    if (baseUrl.isBlank() || snap.kind != ProviderKind.Byok) {
                        baseUrl = ProviderRepository.defaultUrl(ProviderKind.Byok)
                    }
                    modelId = ProviderRepository.defaultModel(ProviderKind.Byok)
                }
                PathCard("Ollama LAN", "Local / LAN host", kind == ProviderKind.OllamaLan) {
                    kind = ProviderKind.OllamaLan
                    baseUrl = ProviderRepository.defaultUrl(ProviderKind.OllamaLan)
                    modelId = ProviderRepository.defaultModel(ProviderKind.OllamaLan)
                }
            }
            OutlinedTextField(
                value = baseUrl,
                onValueChange = { baseUrl = it },
                label = { Text("Base URL") },
                modifier = Modifier.fillMaxWidth(),
                supportingText = { Text("…/v1  — no TAH proxy") },
            )
            if (kind == ProviderKind.Byok) {
                OutlinedTextField(
                    value = apiKey,
                    onValueChange = { apiKey = it },
                    label = { Text("API key") },
                    modifier = Modifier.fillMaxWidth(),
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    supportingText = {
                        Text(
                            if (snap.hasKey) "Key on device ${vm.apiKeyHint()} — leave blank to keep"
                            else "Keys stay on this device (EncryptedSharedPreferences)",
                        )
                    },
                )
            }
            OutlinedTextField(
                value = modelId,
                onValueChange = { modelId = it },
                label = { Text("Model") },
                modifier = Modifier.fillMaxWidth(),
            )
            Text("Model switcher", style = MaterialTheme.typography.titleSmall)
            val models = (snap.models + ProviderRepository.stockModels(kind) + modelId).distinct()
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                models.take(6).forEach { id ->
                    FilterChip(
                        selected = modelId == id,
                        onClick = {
                            modelId = id
                            vm.setModel(id)
                        },
                        label = { Text(id) },
                    )
                }
            }
            OutlinedButton(
                onClick = {
                    vm.save(kind, baseUrl, modelId, apiKey.takeIf { it.isNotBlank() })
                    vm.probe()
                },
                enabled = !probing,
                modifier = Modifier.fillMaxWidth(),
            ) { Text(if (probing) "Probing…" else "Probe / verify") }
            snap.lastProbeOk?.let { ok ->
                Text(
                    if (ok) "Probe OK — ${snap.lastProbeDetail}" else "Probe failed — ${snap.lastProbeDetail}",
                    style = MaterialTheme.typography.bodySmall,
                )
            }
            Button(
                onClick = { vm.save(kind, baseUrl, modelId, apiKey.takeIf { it.isNotBlank() }) },
                modifier = Modifier.fillMaxWidth(),
            ) { Text("Save provider") }
        }
    }
}

@Composable
private fun PathCard(title: String, body: String, selected: Boolean, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .padding(end = 4.dp)
            .border(1.dp, if (selected) TahPrimary else TahOutline, RoundedCornerShape(10.dp))
            .clickable(onClick = onClick)
            .padding(12.dp),
    ) {
        Text(title, style = MaterialTheme.typography.titleSmall)
        Text(body, style = MaterialTheme.typography.bodySmall)
    }
}
