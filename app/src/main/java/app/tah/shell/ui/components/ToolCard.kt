package app.tah.shell.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import app.tah.shell.data.ToolCall
import app.tah.shell.data.ToolStatus
import app.tah.shell.ui.theme.TahNeedsYou
import app.tah.shell.ui.theme.TahOutline
import app.tah.shell.ui.theme.TahPrimary
import app.tah.shell.ui.theme.TahReject
import app.tah.shell.ui.theme.TahSuccess

/** CMP-TOOL-CARD — compact expandable tool receipt. Tap does not approve. */
@Composable
fun ToolCard(
    tool: ToolCall,
    modifier: Modifier = Modifier,
) {
    var expanded by rememberSaveable(tool.id) { mutableStateOf(tool.status == ToolStatus.Failed) }
    val border = when (tool.status) {
        ToolStatus.Running -> TahPrimary
        ToolStatus.AwaitingPermission -> TahNeedsYou
        ToolStatus.Succeeded -> TahSuccess.copy(alpha = 0.7f)
        ToolStatus.Failed, ToolStatus.Rejected -> TahReject
        ToolStatus.Pending -> TahOutline
    }
    Column(
        modifier = modifier
            .fillMaxWidth()
            .border(1.dp, border, RoundedCornerShape(10.dp))
            .clickable { expanded = !expanded }
            .animateContentSize()
            .padding(12.dp),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            if (tool.status == ToolStatus.Running || tool.status == ToolStatus.Pending) {
                CircularProgressIndicator(
                    modifier = Modifier.size(14.dp),
                    strokeWidth = 2.dp,
                    color = TahPrimary,
                )
            }
            Text(
                tool.name,
                style = MaterialTheme.typography.labelLarge,
                color = TahPrimary,
                modifier = Modifier.weight(1f),
            )
            ToolStatusChip(tool.status)
            Icon(
                if (expanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                contentDescription = if (expanded) "Collapse" else "Expand",
            )
        }
        Text(
            tool.target,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 6.dp),
        )
        AnimatedVisibility(visible = expanded) {
            Column(Modifier.padding(top = 8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(tool.argsSummary, style = MaterialTheme.typography.bodySmall)
                if (tool.resultExcerpt.isNotBlank()) {
                    Text(tool.resultExcerpt, style = MaterialTheme.typography.bodySmall)
                }
                tool.durationMs?.let {
                    Text("${it}ms", style = MaterialTheme.typography.labelSmall)
                }
            }
        }
    }
}
