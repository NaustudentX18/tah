package app.tah.shell.ui.dispatch

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import app.tah.shell.TahApplication
import app.tah.shell.ui.TahVmFactory
import app.tah.shell.ui.components.OfflineCapabilityBadge

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DispatchScreen(
    onStarted: (String) -> Unit,
    onConfigureProvider: () -> Unit,
) {
    val app = LocalContext.current.applicationContext as TahApplication
    val vm: DispatchViewModel = viewModel(factory = TahVmFactory(app.container))
    val provider by vm.provider.collectAsStateWithLifecycle()
    var prompt by rememberSaveable { mutableStateOf("") }
    var skillId by rememberSaveable { mutableStateOf("general") }
    var iterations by rememberSaveable { mutableIntStateOf(8) }
    var wallMinutes by rememberSaveable { mutableIntStateOf(5) }

    Scaffold(topBar = { TopAppBar(title = { Text("New run") }) }) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            OfflineCapabilityBadge(app.container.providers.capabilityCopy(offline = !provider.isLive))
            if (!provider.isLive) {
                Text(
                    "No live provider — Start still works via the offline demo stream.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Button(onClick = onConfigureProvider, modifier = Modifier.fillMaxWidth()) {
                    Text("Configure Providers")
                }
            }
            OutlinedTextField(
                value = prompt,
                onValueChange = { prompt = it },
                label = { Text("Prompt") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 4,
            )
            Text("Skill preset", style = MaterialTheme.typography.titleSmall)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                vm.skills.forEach { pack ->
                    FilterChip(
                        selected = skillId == pack.id,
                        onClick = { skillId = pack.id },
                        label = { Text(pack.title) },
                    )
                }
            }
            Text(
                "Model · ${provider.modelId} · ${provider.kind}",
                style = MaterialTheme.typography.bodyMedium,
            )
            Text("Budget", style = MaterialTheme.typography.titleSmall)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(4, 8, 16).forEach { n ->
                    FilterChip(
                        selected = iterations == n,
                        onClick = { iterations = n },
                        label = { Text("$n iter") },
                    )
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(2, 5, 15).forEach { n ->
                    FilterChip(
                        selected = wallMinutes == n,
                        onClick = { wallMinutes = n },
                        label = { Text("${n}m") },
                    )
                }
            }
            Button(
                onClick = {
                    val id = vm.startRun(prompt, skillId, iterations, wallMinutes)
                    onStarted(id)
                },
                enabled = prompt.isNotBlank(),
                modifier = Modifier.fillMaxWidth(),
            ) { Text("Start run") }
        }
    }
}
