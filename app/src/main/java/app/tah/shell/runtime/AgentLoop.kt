package app.tah.shell.runtime

import android.app.Application
import app.tah.shell.data.DoneChip
import app.tah.shell.data.MemoryStore
import app.tah.shell.data.ProviderRepository
import app.tah.shell.data.SessionRepository
import app.tah.shell.data.SettingsRepository
import app.tah.shell.data.SkillStore
import app.tah.shell.data.WorkspaceStore
import app.tah.shell.notify.NeedsYouNotifier
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import java.util.concurrent.ConcurrentHashMap

class AgentLoop(
    private val app: Application,
    private val sessions: SessionRepository,
    private val providers: ProviderRepository,
    private val settings: SettingsRepository,
    private val memory: MemoryStore,
    private val skills: SkillStore,
    private val workspace: WorkspaceStore,
    private val clipboard: ClipboardAccess,
    private val notifier: NeedsYouNotifier,
    private val client: OpenAiCompatClient,
    private val scope: CoroutineScope,
) {
    private val gates = ConcurrentHashMap<String, CompletableDeferred<GateDecision>>()
    private val jobs = ConcurrentHashMap<String, Job>()
    private val body = AgentLoopBody(
        sessions, providers, settings, memory, skills, workspace, clipboard, notifier, client, gates,
    )

    sealed class GateDecision {
        data object Approve : GateDecision()
        data object Reject : GateDecision()
        data class Guide(val instruction: String) : GateDecision()
    }

    fun start(sessionId: String) {
        jobs[sessionId]?.cancel()
        jobs[sessionId] = scope.launch {
            val session = sessions.session(sessionId)
            AgentRunForegroundService.start(app, title = "TAH · ${session?.title?.take(40) ?: "run"}")
            try {
                body.runSession(sessionId)
            } catch (_: CancellationException) {
                sessions.appendSystem(sessionId, "Run cancelled.")
            } catch (t: Throwable) {
                sessions.appendSystem(
                    sessionId,
                    "Agent process died. Session kept; Needs-you preserved if any. (${t.message ?: "error"})",
                )
                sessions.markDone(sessionId, DoneChip.Failed)
            } finally {
                jobs.remove(sessionId)
                if (jobs.isEmpty()) AgentRunForegroundService.stop(app)
            }
        }
    }

    fun decide(sessionId: String, decision: GateDecision) {
        val deferred = gates.remove(sessionId)
        sessions.clearPending(sessionId, backToWorking = true)
        notifier.cancel(sessionId)
        if (deferred != null) deferred.complete(decision)
        else scope.launch { body.continueSeeded(sessionId, decision) }
    }

    fun isRunning(sessionId: String): Boolean = jobs[sessionId]?.isActive == true
    fun anyRunning(): Boolean = jobs.values.any { it.isActive }

    fun cancel(sessionId: String) {
        gates.remove(sessionId)?.cancel()
        jobs[sessionId]?.cancel()
        sessions.clearPending(sessionId, backToWorking = false)
        notifier.cancel(sessionId)
        sessions.appendSystem(sessionId, "You stopped the run.")
        sessions.markDone(sessionId, DoneChip.Cancelled)
    }
}
