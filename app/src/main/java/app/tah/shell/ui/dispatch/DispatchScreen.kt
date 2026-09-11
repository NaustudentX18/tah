package app.tah.shell.ui.dispatch

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
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
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DispatchScreen() {
    var prompt by remember { mutableStateOf("") }
    var skill by remember { mutableStateOf("general") }
    var model by remember { mutableStateOf("default") }
    var budget by remember { mutableStateOf("standard") }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Dispatch") })
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                "Spin up a session. Wiring to the agent runtime is M1.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            OutlinedTextField(
                value = prompt,
                onValueChange = { prompt = it },
                label = { Text("Prompt") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 3,
            )
            OutlinedTextField(
                value = skill,
                onValueChange = { skill = it },
                label = { Text("Skill (stub)") },
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = model,
                onValueChange = { model = it },
                label = { Text("Model (stub)") },
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = budget,
                onValueChange = { budget = it },
                label = { Text("Budget (stub)") },
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(8.dp))
            Button(
                onClick = { /* M1: enqueue dispatch */ },
                modifier = Modifier.fillMaxWidth(),
                enabled = prompt.isNotBlank(),
            ) {
                Text("Dispatch (stub)")
            }
        }
    }
}
