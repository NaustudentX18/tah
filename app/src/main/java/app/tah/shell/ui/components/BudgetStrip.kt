package app.tah.shell.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import app.tah.shell.data.AgentSession
import app.tah.shell.ui.theme.TahNeedsYou
import app.tah.shell.ui.theme.TahPrimary

@Composable
fun BudgetStrip(session: AgentSession, modifier: Modifier = Modifier) {
    val iterFrac = (session.iterationsUsed.toFloat() / session.iterationBudget.coerceAtLeast(1))
        .coerceIn(0f, 1f)
    val elapsed = (System.currentTimeMillis() - session.startedAt).coerceAtLeast(0)
    val timeFrac = (elapsed.toFloat() / session.wallClockMs.coerceAtLeast(1)).coerceIn(0f, 1f)
    val warn = iterFrac > 0.9f || timeFrac > 0.9f
    val remainMin = ((session.wallClockMs - elapsed).coerceAtLeast(0) / 60_000L)
    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                "Iter ${session.iterationsUsed}/${session.iterationBudget}",
                style = MaterialTheme.typography.labelSmall,
            )
            Text(
                "Wall ~${remainMin}m left",
                style = MaterialTheme.typography.labelSmall,
                color = if (warn) TahNeedsYou else MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        LinearProgressIndicator(
            progress = { maxOf(iterFrac, timeFrac) },
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 6.dp),
            color = if (warn) TahNeedsYou else TahPrimary,
        )
    }
}
