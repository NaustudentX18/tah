package app.tah.shell.ui.board

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.tah.shell.data.AppContainer
import app.tah.shell.data.ProviderSnapshot
import app.tah.shell.data.SessionColumn
import app.tah.shell.data.SessionGraph
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

class BoardViewModel(container: AppContainer) : ViewModel() {
    val graph: StateFlow<SessionGraph> = container.sessions.graph
    val provider: StateFlow<ProviderSnapshot> = container.providers.snapshot
    val needsYouCount: StateFlow<Int> = combine(graph, provider) { g, _ ->
        g.sessions.count { it.column == SessionColumn.NeedsYou }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)
}
