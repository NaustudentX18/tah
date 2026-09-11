package app.tah.shell.ui.board

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import app.tah.shell.ui.theme.TahNeedsYou
import app.tah.shell.ui.theme.TahOutline
import app.tah.shell.ui.theme.TahPrimary
import app.tah.shell.ui.theme.TahSuccess
import app.tah.shell.ui.theme.TahSurfaceBright
import app.tah.shell.ui.theme.TahSurfaceContainer

private data class BoardColumn(
    val title: String,
    val accent: Color,
    val emptyWit: String,
    val badgeCount: Int = 0,
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SessionBoardScreen(
    onOpenSession: (String) -> Unit,
) {
    val columns = listOf(
        BoardColumn(
            title = "Working",
            accent = TahPrimary,
            emptyWit = "Agents are on coffee break. Dispatch something spicy.",
        ),
        BoardColumn(
            title = "Needs you",
            accent = TahNeedsYou,
            emptyWit = "Nothing needs you. Enjoy the rare silence.",
            badgeCount = 0,
        ),
        BoardColumn(
            title = "Done",
            accent = TahSuccess,
            emptyWit = "No victories yet. The board remembers.",
        ),
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Session Board") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                ),
                actions = {
                    // Badge stub for future Needs-you count
                    BadgedBox(
                        badge = {
                            Badge(containerColor = TahNeedsYou) {
                                Text("0")
                            }
                        },
                        modifier = Modifier.padding(end = 16.dp),
                    ) {
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clip(CircleShape)
                                .background(TahSurfaceBright),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text("!", style = MaterialTheme.typography.labelSmall)
                        }
                    }
                },
            )
        },
    ) { padding ->
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            columns.forEach { col ->
                Column(
                    modifier = Modifier
                        .width(260.dp)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(12.dp))
                        .background(TahSurfaceContainer)
                        .border(1.dp, TahOutline, RoundedCornerShape(12.dp))
                        .padding(12.dp),
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(col.accent),
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            col.title,
                            style = MaterialTheme.typography.titleMedium,
                            color = col.accent,
                        )
                    }
                    Spacer(Modifier.height(16.dp))
                    // Empty state — M0 has no live sessions yet
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .clickable { onOpenSession("demo-session") }
                            .padding(8.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            col.emptyWit,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                        )
                    }
                }
            }
        }
    }
}
