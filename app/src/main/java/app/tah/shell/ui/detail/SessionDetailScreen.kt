package app.tah.shell.ui.detail

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import app.tah.shell.TahApplication
import app.tah.shell.data.SessionColumn
import app.tah.shell.data.TimelineItem
import app.tah.shell.ui.TahVmFactory
import app.tah.shell.ui.components.BudgetStrip
import app.tah.shell.ui.components.GuideComposer
import app.tah.shell.ui.components.PermissionCard
import app.tah.shell.ui.components.StatusChip
import app.tah.shell.ui.components.ToolCard
import app.tah.shell.ui.theme.TahNeedsYou
import app.tah.shell.ui.theme.TahSurfaceBright

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SessionDetailScreen(
    sessionId: String,
    focusPermission: Boolean,
    onBack: () -> Unit,
) {
    val app = LocalContext.current.applicationContext as TahApplication
    val vm: DetailViewModel = viewModel(
        key = sessionId,
        factory = TahVmFactory(app.container, sessionId),
    )
    val session by vm.session.collectAsStateWithLifecycle()
    val timeline by vm.timeline.collectAsStateWithLifecycle()
    val pending by vm.pending.collectAsStateWithLifecycle()
    val peeking by vm.peeking.collectAsStateWithLifecycle()
    val guiding by vm.guiding.collectAsStateWithLifecycle()
    val listState = rememberLazyListState()

    LaunchedEffect(timeline.size, session?.column) {
        if (session?.column == SessionColumn.Working && timeline.isNotEmpty()) {
            listState.animateScrollToItem(timeline.lastIndex)
        }
    }
    LaunchedEffect(focusPermission, pending) {
        if (focusPermission && pending != null) {
            vm.peeking.value = false
            vm.guiding.value = false
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(session?.title ?: sessionId) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    session?.let { StatusChip(it.column, it.doneChip, Modifier.padding(end = 8.dp)) }
                    IconButton(onClick = vm::togglePeek) {
                        Icon(Icons.Outlined.Visibility, contentDescription = "Peek")
                    }
                },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            session?.let {
                BudgetStrip(it, Modifier.padding(horizontal = 16.dp, vertical = 8.dp))
            }
            if (peeking && pending != null) {
                Text(
                    "Inspecting — run still waiting on you",
                    color = TahNeedsYou,
                    style = MaterialTheme.typography.labelMedium,
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(TahSurfaceBright)
                        .padding(12.dp),
                )
            }
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                items(timeline, key = { it.id }) { item ->
                    when (item) {
                        is TimelineItem.Stream -> Text(
                            item.text,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        is TimelineItem.System -> Text(
                            item.text,
                            style = MaterialTheme.typography.labelMedium,
                            color = TahNeedsYou,
                        )
                        is TimelineItem.Tool -> ToolCard(item.tool)
                    }
                }
            }
            if (pending != null && !peeking) {
                if (guiding) {
                    GuideComposer(
                        onSend = vm::guide,
                        onCancel = vm::cancelGuide,
                        modifier = Modifier.padding(16.dp),
                    )
                } else {
                    PermissionCard(
                        permission = pending!!,
                        onReject = vm::reject,
                        onGuide = vm::openGuide,
                        onApprove = vm::approve,
                        onPeek = vm::togglePeek,
                        modifier = Modifier.padding(16.dp),
                    )
                }
            }
        }
    }
}
