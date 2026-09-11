package app.tah.shell.ui.board

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import app.tah.shell.TahApplication
import app.tah.shell.data.AgentSession
import app.tah.shell.data.DoneChip
import app.tah.shell.data.SessionColumn
import app.tah.shell.ui.TahVmFactory
import app.tah.shell.ui.components.OfflineCapabilityBadge
import app.tah.shell.ui.components.StatusChip
import app.tah.shell.ui.theme.TahNeedsYou
import app.tah.shell.ui.theme.TahOutline
import app.tah.shell.ui.theme.TahSurfaceContainer

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SessionBoardScreen(
    onOpenSession: (String) -> Unit,
    onNewRun: () -> Unit,
) {
    val app = LocalContext.current.applicationContext as TahApplication
    val vm: BoardViewModel = viewModel(factory = TahVmFactory(app.container))
    val graph by vm.graph.collectAsStateWithLifecycle()
    val provider by vm.provider.collectAsStateWithLifecycle()
    val needsYou by vm.needsYouCount.collectAsStateWithLifecycle()
    val columns = listOf(SessionColumn.Working, SessionColumn.NeedsYou, SessionColumn.Done)
    var selected by rememberSaveable { mutableIntStateOf(0) }
    val column = columns[selected]
    val rows = graph.sessions.filter { it.column == column }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Session Board") },
                actions = {
                    BadgedBox(
                        badge = {
                            if (needsYou > 0) {
                                Badge(containerColor = TahNeedsYou) { Text(needsYou.toString()) }
                            }
                        },
                        modifier = Modifier.padding(end = 20.dp),
                    ) {
                        Text("!", style = MaterialTheme.typography.titleMedium)
                    }
                },
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onNewRun) {
                Icon(Icons.Filled.Add, contentDescription = "New run")
            }
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            OfflineCapabilityBadge(
                text = app.container.providers.capabilityCopy(offline = !provider.isLive),
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            )
            TabRow(selectedTabIndex = selected) {
                columns.forEachIndexed { index, col ->
                    val label = when (col) {
                        SessionColumn.Working -> "Working"
                        SessionColumn.NeedsYou -> "Needs you"
                        SessionColumn.Done -> "Done"
                    }
                    Tab(
                        selected = selected == index,
                        onClick = { selected = index },
                        text = {
                            if (col == SessionColumn.NeedsYou && needsYou > 0) {
                                BadgedBox(badge = { Badge(containerColor = TahNeedsYou) { Text("$needsYou") } }) {
                                    Text(label)
                                }
                            } else {
                                Text(label)
                            }
                        },
                    )
                }
            }
            if (rows.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        emptyCopy(column),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    items(rows, key = { it.id }) { session ->
                        SessionBoardCard(session = session, onClick = { onOpenSession(session.id) })
                    }
                }
            }
        }
    }
}

@Composable
private fun SessionBoardCard(session: AgentSession, onClick: () -> Unit) {
    val rail = session.column == SessionColumn.NeedsYou
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(TahSurfaceContainer)
            .border(1.dp, if (rail) TahNeedsYou else TahOutline, RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(12.dp),
    ) {
        if (rail) {
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .height(56.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(TahNeedsYou),
            )
            Spacer(Modifier.size(10.dp))
        }
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
                session.title,
                style = MaterialTheme.typography.titleMedium,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                StatusChip(session.column, session.doneChip)
                if (session.column == SessionColumn.Done && session.doneChip != DoneChip.None) {
                    // chip already encodes Failed / Budget hit — no fourth column
                }
            }
            Text(
                listOfNotNull(session.modelId, session.lastToolName, "iter ${session.iterationsUsed}/${session.iterationBudget}")
                    .joinToString(" · "),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

private fun emptyCopy(column: SessionColumn): String = when (column) {
    SessionColumn.Working -> "No agents grinding. Dispatch something worth watching."
    SessionColumn.NeedsYou -> "Inbox zero for emergencies. Enjoy the silence."
    SessionColumn.Done -> "No finished runs yet. Glory is earned."
}
