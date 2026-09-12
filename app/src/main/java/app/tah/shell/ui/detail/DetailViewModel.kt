package app.tah.shell.ui.detail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.tah.shell.data.AgentSession
import app.tah.shell.data.AppContainer
import app.tah.shell.data.PendingPermission
import app.tah.shell.data.SessionGraph
import app.tah.shell.data.TimelineItem
import app.tah.shell.runtime.AgentLoop
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

class DetailViewModel(
    val sessionId: String,
    private val container: AppContainer,
) : ViewModel() {
    private val loop = container.loop
    val peeking = MutableStateFlow(false)
    val guiding = MutableStateFlow(false)

    val graph: StateFlow<SessionGraph> = container.sessions.graph
    val session: StateFlow<AgentSession?> = graph
        .map { it.sessions.firstOrNull { s -> s.id == sessionId } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), container.sessions.session(sessionId))
    val timeline: StateFlow<List<TimelineItem>> = graph
        .map { it.timelines[sessionId].orEmpty() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), container.sessions.timeline(sessionId))
    val pending: StateFlow<PendingPermission?> = graph
        .map { it.pending[sessionId] }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), container.sessions.pendingFor(sessionId))

    fun approve() = loop.decide(sessionId, AgentLoop.GateDecision.Approve)
    fun reject() = loop.decide(sessionId, AgentLoop.GateDecision.Reject)
    fun guide(instruction: String) {
        guiding.value = false
        loop.decide(sessionId, AgentLoop.GateDecision.Guide(instruction))
    }

    fun togglePeek() {
        peeking.value = !peeking.value
    }

    fun openGuide() {
        guiding.value = true
    }

    fun cancelGuide() {
        guiding.value = false
    }

    fun stopRun() {
        loop.cancel(sessionId)
        guiding.value = false
        peeking.value = false
    }

    fun isLive(): Boolean = loop.isRunning(sessionId)
}
