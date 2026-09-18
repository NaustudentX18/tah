package app.tah.shell.ui.skills

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier.modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import app.tah.shell.ui.components.TahEmptyState
import app.tah.shell.ui.theme.TahOutline

@Composable
internal fun WorkspaceTab(
    files: List<app.tah.shell.data.WorkspaceFile>,
    vm: SkillsViewModel,
) {
    var name by rememberSaveable { mutableStateOf("notes.md") }
    var body by rememberSaveable { mutableStateOf("") }
    var preview by rememberSaveable { mutableStateOf<String?>(null) }
    var exportName by rememberSaveable { mutableStateOf("notes.md") }
    val context = LocalContext.current
    val createDoc = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/markdown"),
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        val text = vm.workspacePreview(exportName)
        val ok = runCatching {
            context.contentResolver.openOutputStream(uri)?.bufferedWriter()?.use { it.write(text) }
            true
        }.getOrDefault(false)
        if (ok) vm.markExported(exportName) else vm.markExportFailed()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            "App-private files under the TAH workspace. fs.read / fs.write use this folder. Not shared phone storage.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        if (files.isEmpty()) {
            TahEmptyState(
                title = "Workspace empty",
                body = "Dispatch a write, or create a file here. Agents see this list at run start.",
            )
        } else {
            files.forEach { file ->
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, TahOutline, RoundedCornerShape(12.dp))
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Text(file.name, style = MaterialTheme.typography.titleSmall)
                    Text(
                        "${file.bytes} bytes",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Row {
                        TextButton(onClick = { preview = vm.workspacePreview(file.name) }) { Text("Preview") }
                        TextButton(
                            onClick = {
                                exportName = file.name
                                createDoc.launch(file.name)
                            },
                        ) { Text("Export") }
                        TextButton(onClick = { vm.deleteWorkspaceFile(file.name) }) { Text("Delete") }
                    }
                }
            }
        }
        preview?.let {
            Text(it, style = MaterialTheme.typography.bodySmall)
        }
        HorizontalDivider()
        OutlinedTextField(
            value = name,
            onValueChange = { name = it },
            label = { Text("Filename") },
            modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
            value = body,
            onValueChange = { body = it },
            label = { Text("Contents") },
            modifier = Modifier.fillMaxWidth(),
            minLines = 4,
        )
        Button(
            onClick = {
                vm.addWorkspaceFile(name, body)
                body = ""
            },
            enabled = body.isNotBlank(),
            modifier = Modifier.fillMaxWidth(),
        ) { Text("Write workspace file") }
    }
}
