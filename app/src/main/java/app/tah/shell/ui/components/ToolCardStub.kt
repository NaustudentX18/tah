package app.tah.shell.ui.components

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import app.tah.shell.ui.theme.TahOutline
import app.tah.shell.ui.theme.TahPrimary

/** Ship-gate chrome stub for tool invocation cards. */
@Composable
fun ToolCardStub(
    toolName: String,
    summary: String,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .border(1.dp, TahOutline, RoundedCornerShape(10.dp))
            .padding(12.dp),
    ) {
        Text(
            toolName,
            style = MaterialTheme.typography.labelLarge,
            color = TahPrimary,
        )
        Text(
            summary,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(vertical = 8.dp),
        )
        Row(verticalAlignment = Alignment.CenterVertically) {
            OutlinedButton(onClick = { /* stub */ }) { Text("Allow once") }
            Spacer(Modifier.width(8.dp))
            OutlinedButton(onClick = { /* stub */ }) { Text("Deny") }
        }
    }
}
