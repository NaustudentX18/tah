package app.tah.shell.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import app.tah.shell.data.DoneChip
import app.tah.shell.data.SessionColumn
import app.tah.shell.data.ToolRisk
import app.tah.shell.data.ToolStatus
import app.tah.shell.ui.theme.TahNeedsYou
import app.tah.shell.ui.theme.TahOnPrimary
import app.tah.shell.ui.theme.TahPrimary
import app.tah.shell.ui.theme.TahReject
import app.tah.shell.ui.theme.TahSuccess

@Composable
fun StatusChip(
    column: SessionColumn,
    doneChip: DoneChip = DoneChip.None,
    modifier: Modifier = Modifier,
) {
    val (label, bg) = when {
        column == SessionColumn.Working -> "Working" to TahPrimary
        column == SessionColumn.NeedsYou -> "Needs you" to TahNeedsYou
        doneChip == DoneChip.Failed -> "Failed" to TahReject
        doneChip == DoneChip.BudgetHit -> "Budget hit" to TahNeedsYou
        else -> "Done" to TahSuccess
    }
    Chip(label, bg, modifier)
}

@Composable
fun ToolStatusChip(status: ToolStatus, modifier: Modifier = Modifier) {
    val (label, bg) = when (status) {
        ToolStatus.Pending -> "Pending" to TahNeedsYou
        ToolStatus.Running -> "Running" to TahPrimary
        ToolStatus.AwaitingPermission -> "Needs you" to TahNeedsYou
        ToolStatus.Succeeded -> "Succeeded" to TahSuccess
        ToolStatus.Failed -> "Failed" to TahReject
        ToolStatus.Rejected -> "Rejected" to TahReject
    }
    Chip(label, bg, modifier)
}

@Composable
fun RiskChip(risk: ToolRisk, modifier: Modifier = Modifier) {
    val (label, bg) = when (risk) {
        ToolRisk.Read -> "Read" to TahSuccess
        ToolRisk.Write -> "Write" to TahNeedsYou
        ToolRisk.Exec -> "Exec" to TahReject
        ToolRisk.Network -> "Network" to TahPrimary
    }
    Chip(label, bg, modifier)
}

@Composable
private fun Chip(label: String, bg: Color, modifier: Modifier = Modifier) {
    Text(
        text = label,
        color = TahOnPrimary,
        modifier = modifier
            .clip(RoundedCornerShape(999.dp))
            .background(bg)
            .padding(horizontal = 8.dp, vertical = 3.dp),
    )
}
