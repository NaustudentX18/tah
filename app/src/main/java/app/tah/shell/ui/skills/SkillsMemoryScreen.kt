package app.tah.shell.ui.skills

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import app.tah.shell.TahApplication
import app.tah.shell.ui.TahVmFactory

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SkillsMemoryScreen() {
    val app = LocalContext.current.applicationContext as TahApplication
    val vm: SkillsViewModel = viewModel(factory = TahVmFactory(app.container))
    val notes by vm.notes.collectAsStateWithLifecycle()
    var title by rememberSaveable { mutableStateOf("") }
    var body by rememberSaveable { mutableStateOf("") }

    Scaffold(topBar = { TopAppBar(title = { Text("Skills & Memory") }) }) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text("Bundled skill packs (M1 light)", style = MaterialTheme.typography.titleMedium)
            vm.packs.forEach { pack ->
                Text("• ${pack.title}", style = MaterialTheme.typography.bodyMedium)
            }
            Text(
                "Import / enable polish is M2. Dispatch can already apply a pack to a run.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text("Memory notes", style = MaterialTheme.typography.titleMedium)
            notes.forEach { note ->
                Text("• ${note.title}: ${note.body}", style = MaterialTheme.typography.bodySmall)
            }
            OutlinedTextField(title, { title = it }, label = { Text("Title") }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(body, { body = it }, label = { Text("Note") }, modifier = Modifier.fillMaxWidth())
            Button(
                onClick = {
                    vm.addNote(title, body)
                    title = ""
                    body = ""
                },
                enabled = body.isNotBlank(),
                modifier = Modifier.fillMaxWidth(),
            ) { Text("Save note") }
        }
    }
}
