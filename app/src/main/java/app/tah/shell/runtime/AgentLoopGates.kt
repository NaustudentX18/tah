package app.tah.shell.runtime

import app.tah.shell.data.AgentSession
import app.tah.shell.data.ChatMessage
import app.tah.shell.data.DoneChip
import app.tah.shell.data.PendingPermission
import app.tah.shell.data.ProviderRepository
import app.tah.shell.data.SessionColumn
import app.tah.shell.data.SessionRepository
import app.tah.shell.data.SettingsRepository
import app.tah.shell.data.ToolCall
import app.tah.shell.data.ToolStatus
import app.tah.shell.notify.NeedsYouNotifier
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.catch

internal class AgentLoopGates(
    private val sessions: SessionRepository,
    private val providers: ProviderRepository,
    private val settings: SettingsRepository,
    private val notifier: NeedsYouNotifier,
    private val client: OpenAiCompatClient,
    private val runtime: ToolRuntime,
    private val gates: MutableMap<String, CompletableDeferred<AgentLoop.GateDecision>>,
) {
    suspend fun streamLive(
        sessionId: String,
        messages: List<ChatMessage>,
        onToolReady: ((OpenAiCompatClient.StreamEvent.ToolCallReady) -> Unit)? = null,
    ) {
        val snap = providers.current
        client.streamChatWithEvents(snap, providers.apiKey(), messages)
            .catch { e ->
                sessions.appendSystem(sessionId, "Provider stream failed: ${e.message}. Falling back to demo tokens.")
                streamDemoText(sessionId, "Offline fallback after provider error.")
            }
            .collect { event ->
                when (event) {
                    is OpenAiCompatClient.StreamEvent.Token -> {
                        sessions.appendToken(sessionId, event.text)
                    }
                    is OpenAiCompatClient.StreamEvent.ToolCallReady -> {
                        onToolReady?.invoke(event)
                    }
                }
            }
    }

    suspend fun streamDemoText(sessionId: String, text: String) {
        val words = text.split(" ")
        val builder = StringBuilder()
        for (word in words) {
            if (builder.isNotEmpty()) builder.append(' ')
            builder.append(word)
            sessions.appendToken(sessionId, if (builder.length == word.length) word else " $word")
            delay(22)
        }
    }

    suspend fun gate(session: AgentSession, tool: ToolCall): AgentLoop.GateDecision {
        val mode = settings.current.permissionMode
        if (!PermissionPolicy.requiresAsk(tool.risk, mode)) {
            sessions.appendSystem(session.id, "Auto-allowed ${tool.name} under ${mode.name} (exec still Ask).")
            return AgentLoop.GateDecision.Approve
        }
        val permission = PendingPermission(
            id = SessionRepository.newId(),
            sessionId = session.id,
            toolId = tool.id,
            toolName = tool.name,
            risk = tool.risk,
            summary = tool.argsSummary,
            target = tool.target,
        )
        sessions.upsertTool(tool.copy(status = ToolStatus.AwaitingPermission))
        sessions.markNeedsYou(session.id, permission)
        if (settings.current.notificationsEnabled) {
            notifier.notifyNeedsYou(session.copy(column = SessionColumn.NeedsYou), permission)
        }
        val deferred = CompletableDeferred<AgentLoop.GateDecision>()
        gates[session.id] = deferred
        return deferred.await()
    }

    fun budgetHit(session: AgentSession): Boolean {
        if (session.iterationsUsed >= session.iterationBudget) return true
        val elapsed = System.currentTimeMillis() - session.startedAt
        return elapsed >= session.wallClockMs
    }

    fun finishBudget(sessionId: String) {
        sessions.appendSystem(sessionId, "Budget hit — stopped clean, not hung.")
        sessions.markDone(sessionId, DoneChip.BudgetHit)
    }

    suspend fun continueSeeded(sessionId: String, decision: AgentLoop.GateDecision) {
        val pendingTool = sessions.timeline(sessionId)
            .filterIsInstance<app.tah.shell.data.TimelineItem.Tool>()
            .lastOrNull()
            ?.tool
            ?: return
        when (decision) {
            AgentLoop.GateDecision.Approve -> {
                val outcome = runtime.apply(pendingTool, sessions.session(sessionId)?.prompt.orEmpty())
                sessions.upsertTool(
                    pendingTool.copy(
                        status = ToolStatus.Succeeded,
                        resultExcerpt = "Approved from seeded Needs-you card. ${outcome.excerpt}",
                        durationMs = 200,
                    ),
                )
                streamDemoText(sessionId, outcome.excerpt)
                sessions.markDone(sessionId, DoneChip.None)
            }
            AgentLoop.GateDecision.Reject -> {
                sessions.upsertTool(
                    pendingTool.copy(
                        status = ToolStatus.Rejected,
                        resultExcerpt = "You rejected ${pendingTool.name}.",
                    ),
                )
                sessions.appendSystem(sessionId, "You rejected ${pendingTool.name}. That tool call ended.")
                sessions.markDone(sessionId, DoneChip.None)
            }
            is AgentLoop.GateDecision.Guide -> {
                sessions.upsertTool(
                    pendingTool.copy(
                        status = ToolStatus.Succeeded,
                        resultExcerpt = "Guided: ${decision.instruction}",
                    ),
                )
                streamDemoText(sessionId, "Guide received: ${decision.instruction}")
                sessions.markDone(sessionId, DoneChip.None)
            }
        }
    }
}
