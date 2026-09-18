package app.tah.shell.runtime

import app.tah.shell.data.MemoryStore
import app.tah.shell.data.ProviderRepository
import app.tah.shell.data.SessionRepository
import app.tah.shell.data.SettingsRepository
import app.tah.shell.data.SkillStore
import app.tah.shell.data.WorkspaceStore
import app.tah.shell.notify.NeedsYouNotifier
import kotlinx.coroutines.CompletableDeferred

/** Thin facade over [AgentLoopSession]. */
internal class AgentLoopBody(
    sessions: SessionRepository,
    providers: ProviderRepository,
    settings: SettingsRepository,
    memory: MemoryStore,
    skills: SkillStore,
    workspace: WorkspaceStore,
    clipboard: ClipboardAccess,
    notifier: NeedsYouNotifier,
    client: OpenAiCompatClient,
    gates: MutableMap<String, CompletableDeferred<AgentLoop.GateDecision>>,
) {
    private val session = AgentLoopSession(
        sessions, providers, settings, memory, skills, workspace, clipboard, notifier, client, gates,
    )

    suspend fun runSession(sessionId: String) = session.runSession(sessionId)

    suspend fun continueSeeded(sessionId: String, decision: AgentLoop.GateDecision) =
        session.continueSeeded(sessionId, decision)
}
