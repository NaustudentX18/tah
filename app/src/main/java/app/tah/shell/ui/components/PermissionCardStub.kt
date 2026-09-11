package app.tah.shell.ui.components

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import app.tah.shell.ui.theme.TahNeedsYou
import app.tah.shell.ui.theme.TahOutline

/** Ship-gate chrome stub for permission / needs-you cards. */
@Composable
fun PermissionCardStub(
    title: String,
    detail: String,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .border(1.dp, TahNeedsYou.copy(alpha = 0.5f), RoundedCornerShape(10.dp))
            .padding(12.dp),
    ) {
        Text(
            title,
            style = MaterialTheme.typography.labelLarge,
            color = TahNeedsYou,
        )
        Text(
            detail,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(vertical = 8.dp),
        )
        Row(verticalAlignment = Alignment.CenterVertically) {
            Button(onClick = { /* stub */ }) { Text("Approve") }
            Spacer(Modifier.width(8.dp))
            OutlinedButton(onClick = { /* stub */ }) { Text("Reject") }
        }
    }
}
