package app.tah.shell.runtime

import app.tah.shell.data.AgentSession
import app.tah.shell.data.ChatMessage
import app.tah.shell.data.DoneChip
import app.tah.shell.data.MemoryStore
import app.tah.shell.data.PendingPermission
import app.tah.shell.data.ProviderKind
import app.tah.shell.data.ProviderRepository
import app.tah.shell.data.SessionRepository
import app.tah.shell.data.SettingsRepository
import app.tah.shell.data.SkillCatalog
import app.tah.shell.data.ToolCall
import app.tah.shell.data.ToolRisk
import app.tah.shell.data.ToolStatus
import app.tah.shell.notify.NeedsYouNotifier
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import java.util.concurrent.ConcurrentHashMap

class AgentLoop(
    private val sessions: SessionRepository,
    private val providers: ProviderRepository,
    private val settings: SettingsRepository,
    private val memory: MemoryStore,
    private val notifier: NeedsYouNotifier,
    private val client: OpenAiCompatClient,
    private val scope: CoroutineScope,
) {
    private val gates = ConcurrentHashMap<String, CompletableDeferred<GateDecision>>()
    private val jobs = ConcurrentHashMap<String, Job>()

    sealed class GateDecision {
        data object Approve : GateDecision()
        data object Reject : GateDecision()
        data class Guide(val instruction: String) : GateDecision()
    }

    fun start(sessionId: String) {
        jobs[sessionId]?.cancel()
        jobs[sessionId] = scope.launch {
            try {
                runSession(sessionId)
            } catch (_: CancellationException) {
                sessions.appendSystem(sessionId, "Run cancelled.")
            } catch (t: Throwable) {
                sessions.appendSystem(sessionId, "Agent process died. Session kept; Needs-you preserved if any.")
                sessions.markDone(sessionId, DoneChip.Failed)
            } finally {
                jobs.remove(sessionId)
            }
        }
    }

    fun decide(sessionId: String, decision: GateDecision) {
        val deferred = gates.remove(sessionId)
        sessions.clearPending(sessionId, backToWorking = true)
        notifier.cancel(sessionId)
        if (deferred != null) {
            deferred.complete(decision)
        } else {
            scope.launch { continueSeeded(sessionId, decision) }
        }
    }

    fun isRunning(sessionId: String): Boolean = jobs[sessionId]?.isActive == true

    private suspend fun runSession(sessionId: String) {
        val session = sessions.session(sessionId) ?: return
        sessions.updateSession(sessionId) { it.copy(column = app.tah.shell.data.SessionColumn.Working) }
        val provider = providers.current
        val live = provider.isLive && session.providerKind != ProviderKind.Demo
        val label = if (live) {
            "${provider.kind.name.lowercase()} · ${provider.modelId}"
        } else {
            "demo stream (offline)"
        }
        sessions.appendSystem(sessionId, "Run started · $label")
        if (budgetHit(session.copy(iterationsUsed = session.iterationsUsed))) {
            finishBudget(sessionId)
            return
        }

        val skill = SkillCatalog.byId(session.skillId)
        val memoryText = memory.snapshotText()
        val messages = mutableListOf(
            ChatMessage(
                "system",
                "You are TAH, a phone-first agent harness. Prefer short status. " +
                    "Do not pretend tools ran without a card.\n\nSkill:\n${skill.body}\n\nMemory:\n$memoryText",
            ),
            ChatMessage("user", session.prompt),
        )

        if (live) {
            streamLive(sessionId, messages)
        } else {
            streamDemoText(
                sessionId,
                "I'll inspect the workspace, then propose a tool. Tokens are streaming locally — no TAH proxy.",
            )
        }
        sessions.incrementIteration(sessionId)

        val tool = proposeTool(session)
        sessions.upsertTool(tool.copy(status = ToolStatus.Pending))
        delay(250)
        sessions.upsertTool(tool.copy(status = ToolStatus.Running))

        val latest = sessions.session(sessionId) ?: session
        if (budgetHit(latest)) {
            finishBudget(sessionId)
            return
        }

        val decision = gate(latest, tool)
        when (decision) {
            GateDecision.Approve -> {
                sessions.upsertTool(
                    tool.copy(
                        status = ToolStatus.Succeeded,
                        resultExcerpt = "Approved. Receipt recorded on the timeline.",
                        durationMs = 420,
                    ),
                )
                sessions.appendSystem(sessionId, "Tool ${tool.name} approved.")
                if (live) {
                    messages += ChatMessage("assistant", "Requested ${tool.name} on ${tool.target}.")
                    messages += ChatMessage("user", "Tool ${tool.name} succeeded. Wrap up in 3 sentences.")
                    streamLive(sessionId, messages)
                } else {
                    streamDemoText(sessionId, "Write landed. Board will move this session to Done.")
                }
                sessions.markDone(sessionId, DoneChip.None)
            }
            GateDecision.Reject -> {
                sessions.upsertTool(
                    tool.copy(
                        status = ToolStatus.Rejected,
                        resultExcerpt = "You rejected ${tool.name}. Guide was not required.",
                    ),
                )
                sessions.appendSystem(sessionId, "You rejected ${tool.name}. That tool call ended.")
                streamDemoText(sessionId, "Understood — I will not run ${tool.name}. Closing the turn.")
                sessions.markDone(sessionId, DoneChip.None)
            }
            is GateDecision.Guide -> {
                sessions.upsertTool(
                    tool.copy(
                        status = ToolStatus.Succeeded,
                        resultExcerpt = "Guided: ${decision.instruction}",
                        durationMs = 360,
                    ),
                )
                sessions.appendSystem(sessionId, "Guide injected: ${decision.instruction}")
                if (live) {
                    messages += ChatMessage(
                        "user",
                        "Guide from the human (do not ignore): ${decision.instruction}. Continue briefly.",
                    )
                    streamLive(sessionId, messages)
                } else {
                    streamDemoText(sessionId, "Guided: ${decision.instruction} — continuing the turn, then Done.")
                }
                sessions.markDone(sessionId, DoneChip.None)
            }
        }
    }

    private suspend fun continueSeeded(sessionId: String, decision: GateDecision) {
        val pendingTool = sessions.timeline(sessionId)
            .filterIsInstance<app.tah.shell.data.TimelineItem.Tool>()
            .lastOrNull()
            ?.tool
            ?: return
        when (decision) {
            GateDecision.Approve -> {
                sessions.upsertTool(
                    pendingTool.copy(
                        status = ToolStatus.Succeeded,
                        resultExcerpt = "Approved from seeded Needs-you card.",
                        durationMs = 200,
                    ),
                )
                streamDemoText(sessionId, "Seeded write approved. Session moves to Done.")
                sessions.markDone(sessionId, DoneChip.None)
            }
            GateDecision.Reject -> {
                sessions.upsertTool(
                    pendingTool.copy(
                        status = ToolStatus.Rejected,
                        resultExcerpt = "You rejected ${pendingTool.name}.",
                    ),
                )
                sessions.appendSystem(sessionId, "You rejected ${pendingTool.name}. That tool call ended.")
                sessions.markDone(sessionId, DoneChip.None)
            }
            is GateDecision.Guide -> {
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

    private suspend fun streamLive(sessionId: String, messages: List<ChatMessage>) {
        val snap = providers.current
        client.streamChat(snap, providers.apiKey(), messages)
            .catch { e ->
                sessions.appendSystem(sessionId, "Provider stream failed: ${e.message}. Falling back to demo tokens.")
                streamDemoText(sessionId, "Offline fallback after provider error.")
            }
            .collect { token -> sessions.appendToken(sessionId, token) }
    }

    private suspend fun streamDemoText(sessionId: String, text: String) {
        val words = text.split(" ")
        val builder = StringBuilder()
        for (word in words) {
            if (builder.isNotEmpty()) builder.append(' ')
            builder.append(word)
            sessions.appendToken(sessionId, if (builder.length == word.length) word else " $word")
            delay(28)
        }
    }

    private suspend fun gate(session: AgentSession, tool: ToolCall): GateDecision {
        val mode = settings.current.permissionMode
        if (!PermissionPolicy.requiresAsk(tool.risk, mode)) {
            sessions.appendSystem(session.id, "Auto-allowed ${tool.name} under ${mode.name} (exec still Ask).")
            return GateDecision.Approve
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
            notifier.notifyNeedsYou(session.copy(column = app.tah.shell.data.SessionColumn.NeedsYou), permission)
        }
        val deferred = CompletableDeferred<GateDecision>()
        gates[session.id] = deferred
        return deferred.await()
    }

    private fun proposeTool(session: AgentSession): ToolCall {
        val prompt = session.prompt.lowercase()
        val (name, target, summary, risk) = when {
            listOf("shell", "exec", "run command", "bash").any { it in prompt } ->
                ToolProposal("shell.exec", "sh -c …", "exec stays Ask even if Allow edits is on", ToolRisk.Exec)
            listOf("http", "search", "fetch", "web").any { it in prompt } ->
                ToolProposal("web.fetch", "https://example.invalid/docs", "GET summary", ToolRisk.Network)
            listOf("read", "open", "inspect").any { it in prompt } && !prompt.contains("write") ->
                ToolProposal("fs.read", "notes.md", "read excerpt", ToolRisk.Read)
            else ->
                ToolProposal("fs.write", "notes.md", "write a short patch from the prompt", ToolRisk.Write)
        }
        return ToolCall(
            id = SessionRepository.newId(),
            sessionId = session.id,
            name = name,
            target = target,
            argsSummary = summary,
            risk = risk,
            status = ToolStatus.Pending,
        )
    }

    private fun budgetHit(session: AgentSession): Boolean {
        if (session.iterationsUsed >= session.iterationBudget) return true
        val elapsed = System.currentTimeMillis() - session.startedAt
        return elapsed >= session.wallClockMs
    }

    private fun finishBudget(sessionId: String) {
        sessions.appendSystem(sessionId, "Budget hit — stopped clean, not hung.")
        sessions.markDone(sessionId, DoneChip.BudgetHit)
    }

    private data class ToolProposal(
        val name: String,
        val target: String,
        val summary: String,
        val risk: ToolRisk,
    )
}
