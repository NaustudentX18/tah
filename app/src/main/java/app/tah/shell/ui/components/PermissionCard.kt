package app.tah.shell.ui.components

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.dp
import app.tah.shell.data.PendingPermission
import app.tah.shell.ui.theme.TahNeedsYou

/** CMP-PERMISSION-CARD — Reject · Guide · Approve. Reject ends the tool; Guide is independent. */
@Composable
fun PermissionCard(
    permission: PendingPermission,
    onReject: () -> Unit,
    onGuide: () -> Unit,
    onApprove: () -> Unit,
    onPeek: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val haptic = LocalHapticFeedback.current
    Column(
        modifier = modifier
            .fillMaxWidth()
            .border(1.dp, TahNeedsYou, RoundedCornerShape(12.dp))
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                permission.toolName,
                style = MaterialTheme.typography.titleMedium,
                color = TahNeedsYou,
                modifier = Modifier.weight(1f),
            )
            RiskChip(permission.risk)
            IconButton(onClick = onPeek) {
                Icon(Icons.Outlined.Visibility, contentDescription = "Peek")
            }
        }
        Text(
            "${permission.target}\n${permission.summary}",
            style = MaterialTheme.typography.bodyMedium,
        )
        Text(
            "Ask default. Dismissing a notification does not approve.",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            TextButton(
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onReject()
                },
            ) { Text("Reject") }
            OutlinedButton(onClick = onGuide) { Text("Guide") }
            Button(
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    onApprove()
                },
                modifier = Modifier.weight(1f),
            ) { Text("Approve") }
        }
    }
}
