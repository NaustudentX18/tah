package app.tah.shell.runtime

import app.tah.shell.data.DoneChip
import app.tah.shell.data.MemoryStore
import app.tah.shell.data.ProviderRepository
import app.tah.shell.data.SessionRepository
import app.tah.shell.data.SettingsRepository
import app.tah.shell.data.SkillStore
import app.tah.shell.data.WorkspaceStore
import app.tah.shell.notify.NeedsYouNotifier
import kotlinx.coroutines.CompletableDeferred

internal class AgentLoopBody(
    private val sessions: SessionRepository,
    private val providers: ProviderRepository,
    private val settings: SettingsRepository,
    private val memory: MemoryStore,
    private val skills: SkillStore,
    private val workspace: WorkspaceStore,
    private val clipboard: ClipboardAccess,
    private val notifier: NeedsYouNotifier,
    private val client: OpenAiCompatClient,
    private val gates: MutableMap<String, CompletableDeferred<AgentLoop.GateDecision>>,
) {
    private val runtime = ToolRuntime(memory, workspace, clipboard)

    suspend fun runSession(sessionId: String) {
        sessions.appendSystem(sessionId, "AgentLoopBody loading — apply next commit for full runner.")
        sessions.markDone(sessionId, DoneChip.Failed)
    }

    suspend fun continueSeeded(sessionId: String, decision: AgentLoop.GateDecision) {
        sessions.markDone(sessionId, DoneChip.None)
    }
}
