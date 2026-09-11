package app.tah.shell.ui.onboard

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import app.tah.shell.ui.components.HarnessGlyph
import app.tah.shell.ui.components.OfflineCapabilityBadge

/**
 * SCR-ONBOARD — light first-run hero → Providers wizard → Board.
 * Touch Steer is default; no input-mode prompt here.
 */
@Composable
fun OnboardScreen(
    onWireProvider: () -> Unit,
    onSkipToBoard: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        HarnessGlyph(size = 72.dp)
        Spacer(Modifier.height(20.dp))
        Text(
            "TAH",
            style = MaterialTheme.typography.displaySmall,
            color = MaterialTheme.colorScheme.primary,
        )
        Text(
            "Steer agents. Phone-first.",
            style = MaterialTheme.typography.titleMedium,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(12.dp))
        Text(
            "Session board · tool cards · Ask-default permissions. Not a chat app. Not Termux with vibes.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(20.dp))
        OfflineCapabilityBadge(
            text = "Wire a provider, then steer. Skip keeps the offline demo stream honest.",
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(28.dp))
        Button(
            onClick = onWireProvider,
            modifier = Modifier.fillMaxWidth(),
        ) { Text("Wire a provider") }
        TextButton(
            onClick = onSkipToBoard,
            modifier = Modifier.fillMaxWidth(),
        ) { Text("I'll set this up later") }
    }
}
