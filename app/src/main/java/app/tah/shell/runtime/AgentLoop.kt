package app.tah.shell.runtime

import android.app.Application
import app.tah.shell.data.AgentSession
import app.tah.shell.data.ChatMessage
import app.tah.shell.data.DoneChip
import app.tah.shell.data.MemoryStore
import app.tah.shell.data.PendingPermission
import app.tah.shell.data.ProviderKind
import app.tah.shell.data.ProviderRepository
import app.tah.shell.data.SessionColumn
import app.tah.shell.data.SessionRepository
import app.tah.shell.data.SettingsRepository
import app.tah.shell.data.SkillStore
import app.tah.shell.data.ToolCall
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
    private val app: Application,
    private val sessions: SessionRepository,
    private val providers: ProviderRepository,
    private val settings: SettingsRepository,
    private val memory: MemoryStore,
    private val skills: SkillStore,
    private val notifier: NeedsYouNotifier,
    private val client: OpenAiCompatClient,
    private val scope: CoroutineScope,
) {
    private val gates = ConcurrentHashMap<String, CompletableDeferred<GateDecision>>()
    private val jobs = ConcurrentHashMap<String, Job>()
    private val runtime = ToolRuntime(memory)

    sealed class GateDecision {
        data object Approve : GateDecision()
        data object Reject : GateDecision()
        data class Guide(val instruction: String) : GateDecision()
    }

    fun start(sessionId: String) {
        jobs[sessionId]?.cancel()
        jobs[sessionId] = scope.launch {
            val session = sessions.session(sessionId)
            AgentRunForegroundService.start(
                app,
                title = "TAH · ${session?.title?.take(40) ?: "run"}",
            )
            try {
                runSession(sessionId)
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
                if (jobs.isEmpty()) {
                    AgentRunForegroundService.stop(app)
                }
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

    fun anyRunning(): Boolean = jobs.values.any { it.isActive }

    private suspend fun runSession(sessionId: String) {
        val session0 = sessions.session(sessionId) ?: return
        sessions.updateSession(sessionId) { it.copy(column = SessionColumn.Working) }
        val provider = providers.current
        val live = provider.isLive && session0.providerKind != ProviderKind.Demo
        val label = if (live) {
            "${provider.kind.name.lowercase()} · ${provider.modelId}"
        } else {
            "demo stream (offline)"
        }
        sessions.appendSystem(sessionId, "Run started · $label · multi-tool loop · FG up (not immortal)")
        if (budgetHit(session0)) {
            finishBudget(sessionId)
            return
        }

        val skill = skills.byId(session0.skillId)
        val enabledSkills = skills.enabledSnapshot()
        val memoryText = memory.snapshotText()
        val messages = mutableListOf(
            ChatMessage(
                "system",
                "You are TAH, a phone-first agent harness. Prefer short status. " +
                    "Do not pretend tools ran without a card. " +
                    "memory.write is the only tool that persists on-device today.\n\n" +
                    "Active skill (${skill.title}):\n${skill.body}\n\n" +
                    "Enabled packs:\n$enabledSkills\n\nMemory:\n$memoryText",
            ),
            ChatMessage("user", session0.prompt),
        )

        if (live) {
            streamLive(sessionId, messages)
        } else {
            streamDemoText(
                sessionId,
                "I'll plan tools on the board, wait if Ask requires it, then keep going until budget or wrap-up.",
            )
        }

        val priorNames = mutableListOf<String>()
        var rejected = false
        var lastGuide: String? = null

        while (true) {
            val latest = sessions.session(sessionId) ?: break
            if (budgetHit(latest)) {
                finishBudget(sessionId)
                return
            }
            sessions.incrementIteration(sessionId)

            val tool = ToolPlanner.next(latest, priorNames, SessionRepository.newId())
            if (tool == null) {
                sessions.appendSystem(sessionId, "No further tools planned. Wrapping the run.")
                break
            }
            priorNames += tool.name
            sessions.upsertTool(tool.copy(status = ToolStatus.Pending))
            delay(180)
            sessions.upsertTool(tool.copy(status = ToolStatus.Running))

            val decision = gate(latest, tool)
            when (decision) {
                GateDecision.Approve -> {
                    val outcome = runtime.apply(tool, latest.prompt)
                    sessions.upsertTool(
                        tool.copy(
                            status = ToolStatus.Succeeded,
                            resultExcerpt = outcome.excerpt,
                            durationMs = if (outcome.appliedForReal) 90 else 40,
                        ),
                    )
                    sessions.appendSystem(
                        sessionId,
                        if (outcome.appliedForReal) {
                            "Tool ${tool.name} approved and applied on-device."
                        } else {
                            "Tool ${tool.name} approved. Receipt only — no silent ${tool.name} side effect."
                        },
                    )
                    if (live) {
                        messages += ChatMessage("assistant", "Requested ${tool.name} on ${tool.target}.")
                        messages += ChatMessage("user", "Tool ${tool.name}: ${outcome.excerpt}. Continue if useful.")
                        streamLive(sessionId, messages)
                    } else {
                        streamDemoText(sessionId, outcome.excerpt)
                    }
                }
                GateDecision.Reject -> {
                    rejected = true
                    sessions.upsertTool(
                        tool.copy(
                            status = ToolStatus.Rejected,
                            resultExcerpt = "You rejected ${tool.name}. Guide was not required.",
                        ),
                    )
                    sessions.appendSystem(sessionId, "You rejected ${tool.name}. That tool call ended.")
                    streamDemoText(sessionId, "Understood — I will not run ${tool.name}. Closing the turn.")
                    break
                }
                is GateDecision.Guide -> {
                    lastGuide = decision.instruction
                    val outcome = runtime.apply(tool, "${latest.prompt}\nGuide: ${decision.instruction}")
                    sessions.upsertTool(
                        tool.copy(
                            status = ToolStatus.Succeeded,
                            resultExcerpt = "Guided: ${decision.instruction}. ${outcome.excerpt}",
                            durationMs = 120,
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
                        streamDemoText(sessionId, "Guided: ${decision.instruction} — continuing if budget remains.")
                    }
                }
            }
        }

        if (!rejected) {
            val wrap = lastGuide?.let { "Guided run complete ($it)." } ?: "Board will move this session to Done."
            streamDemoText(sessionId, wrap)
        }
        sessions.markDone(sessionId, DoneChip.None)
    }

    private suspend fun continueSeeded(sessionId: String, decision: GateDecision) {
        val pendingTool = sessions.timeline(sessionId)
            .filterIsInstance<app.tah.shell.data.TimelineItem.Tool>()
            .lastOrNull()
            ?.tool
            ?: return
        when (decision) {
            GateDecision.Approve -> {
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
            delay(22)
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
            notifier.notifyNeedsYou(session.copy(column = SessionColumn.NeedsYou), permission)
        }
        val deferred = CompletableDeferred<GateDecision>()
        gates[session.id] = deferred
        return deferred.await()
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
}
