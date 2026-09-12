package app.tah.shell.ui.skills

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import app.tah.shell.TahApplication
import app.tah.shell.data.MemoryNote
import app.tah.shell.data.SkillPack
import app.tah.shell.ui.TahVmFactory
import app.tah.shell.ui.components.TahEmptyState
import app.tah.shell.ui.theme.TahOutline
import app.tah.shell.ui.theme.TahReject

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SkillsMemoryScreen() {
    val app = LocalContext.current.applicationContext as TahApplication
    val vm: SkillsViewModel = viewModel(factory = TahVmFactory(app.container))
    val packs by vm.packs.collectAsStateWithLifecycle()
    val notes by vm.notes.collectAsStateWithLifecycle()
    val message by vm.message.collectAsStateWithLifecycle()
    var tab by rememberSaveable { mutableIntStateOf(0) }

    LaunchedEffect(message) {
        if (message != null) {
            kotlinx.coroutines.delay(3500)
            vm.clearMessage()
        }
    }

    Scaffold(topBar = { TopAppBar(title = { Text("Skills & Memory") }) }) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            TabRow(selectedTabIndex = tab) {
                Tab(selected = tab == 0, onClick = { tab = 0 }, text = { Text("Skills") })
                Tab(selected = tab == 1, onClick = { tab = 1 }, text = { Text("Memory") })
            }
            message?.let {
                Text(
                    it,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    style = MaterialTheme.typography.bodySmall,
                    color = if (it.contains("need", ignoreCase = true) || it.contains("too large", ignoreCase = true) || it.contains("Bundled", ignoreCase = true)) {
                        TahReject
                    } else {
                        MaterialTheme.colorScheme.primary
                    },
                )
            }
            when (tab) {
                0 -> SkillsTab(packs = packs, vm = vm)
                else -> MemoryTab(notes = notes, vm = vm)
            }
        }
    }
}

@Composable
private fun SkillsTab(packs: List<SkillPack>, vm: SkillsViewModel) {
    var importText by rememberSaveable { mutableStateOf("") }
    var showImport by rememberSaveable { mutableStateOf(false) }
    val context = LocalContext.current
    val openMarkdown = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        val text = runCatching {
            context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
        }.getOrNull()
        if (text.isNullOrBlank()) {
            vm.importMarkdown("")
        } else {
            vm.importMarkdown(text)
        }
    }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            "Markdown skill packs the agent loop reads at run start. Enable what Dispatch may apply. Import from a .md file or paste.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        if (packs.isEmpty()) {
            TahEmptyState(
                title = "No skill packs on deck",
                body = "Paste markdown to load a pack. Bundled packs usually ride along — if you wiped them, reinstall or import.",
            )
        } else {
            packs.forEach { pack ->
                SkillPackRow(
                    pack = pack,
                    onToggle = { vm.setEnabled(pack.id, it) },
                    onRemove = { vm.removePack(pack.id) },
                )
            }
        }
        HorizontalDivider()
        OutlinedButton(
            onClick = { openMarkdown.launch(arrayOf("text/markdown", "text/plain", "*/*")) },
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("Import from file")
        }
        OutlinedButton(
            onClick = { showImport = !showImport },
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(if (showImport) "Hide paste import" else "Paste markdown pack")
        }
        if (showImport) {
            OutlinedTextField(
                value = importText,
                onValueChange = { importText = it },
                label = { Text("Paste markdown (# Title …)") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 6,
            )
            Button(
                onClick = {
                    vm.importMarkdown(importText)
                    if (importText.isNotBlank()) importText = ""
                },
                enabled = importText.isNotBlank(),
                modifier = Modifier.fillMaxWidth(),
            ) { Text("Load pack") }
        }
    }
}

@Composable
private fun SkillPackRow(
    pack: SkillPack,
    onToggle: (Boolean) -> Unit,
    onRemove: () -> Unit,
) {
    var expanded by rememberSaveable(pack.id) { mutableStateOf(false) }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, TahOutline, RoundedCornerShape(12.dp))
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(pack.title, style = MaterialTheme.typography.titleMedium)
                Text(
                    if (pack.bundled) "Bundled" else "Imported",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Switch(checked = pack.enabled, onCheckedChange = onToggle)
        }
        TextButton(onClick = { expanded = !expanded }) {
            Text(if (expanded) "Hide markdown" else "Preview markdown")
        }
        if (expanded) {
            Text(
                pack.body,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        if (!pack.bundled) {
            TextButton(onClick = onRemove) { Text("Remove import") }
        }
    }
}

@Composable
private fun MemoryTab(notes: List<MemoryNote>, vm: SkillsViewModel) {
    var title by rememberSaveable { mutableStateOf("") }
    var body by rememberSaveable { mutableStateOf("") }
    var editingId by rememberSaveable { mutableStateOf<String?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            "Simple notes the agent loop injects into the system prompt. Soft keyboard only while editing.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        if (notes.isEmpty()) {
            TahEmptyState(
                title = "Memory is blank",
                body = "Write a note the agent should remember — locks, preferences, project crumbs.",
            )
        } else {
            notes.forEach { note ->
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, TahOutline, RoundedCornerShape(12.dp))
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Text(note.title, style = MaterialTheme.typography.titleSmall)
                    Text(note.body, style = MaterialTheme.typography.bodySmall)
                    Row {
                        TextButton(
                            onClick = {
                                editingId = note.id
                                title = note.title
                                body = note.body
                            },
                        ) { Text("Edit") }
                        TextButton(onClick = { vm.deleteNote(note.id) }) { Text("Delete") }
                    }
                }
            }
        }
        HorizontalDivider()
        Text(
            if (editingId == null) "New note" else "Edit note",
            style = MaterialTheme.typography.titleMedium,
        )
        OutlinedTextField(
            value = title,
            onValueChange = { title = it },
            label = { Text("Title") },
            modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
            value = body,
            onValueChange = { body = it },
            label = { Text("Note") },
            modifier = Modifier.fillMaxWidth(),
            minLines = 3,
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(
                onClick = {
                    val id = editingId
                    if (id == null) vm.addNote(title, body) else vm.updateNote(id, title, body)
                    title = ""
                    body = ""
                    editingId = null
                },
                enabled = body.isNotBlank(),
                modifier = Modifier.weight(1f),
            ) { Text(if (editingId == null) "Save note" else "Update note") }
            if (editingId != null) {
                OutlinedButton(
                    onClick = {
                        editingId = null
                        title = ""
                        body = ""
                    },
                    modifier = Modifier.weight(1f),
                ) { Text("Cancel") }
            }
        }
    }
}
