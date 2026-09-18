package app.tah.shell.runtime

import app.tah.shell.data.ChatMessage
import app.tah.shell.data.DoneChip
import app.tah.shell.data.MemoryStore
import app.tah.shell.data.ProviderKind
import app.tah.shell.data.ProviderRepository
import app.tah.shell.data.SessionColumn
import app.tah.shell.data.SessionRepository
import app.tah.shell.data.SettingsRepository
import app.tah.shell.data.SkillStore
import app.tah.shell.data.ToolCall
import app.tah.shell.data.ToolStatus
import app.tah.shell.data.WorkspaceStore
import app.tah.shell.notify.NeedsYouNotifier
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.delay

internal class AgentLoopSession(
    private val sessions: SessionRepository,
    private val providers: ProviderRepository,
    private val settings: SettingsRepository,
    private val memory: MemoryStore,
    private val skills: SkillStore,
    private val workspace: WorkspaceStore,
    private val clipboard: ClipboardAccess,
    private val notifier: NeedsYouNotifier,
    private val client: OpenAiCompatClient,
    gatesMap: MutableMap<String, CompletableDeferred<AgentLoop.GateDecision>>,
) {
    private val runtime = ToolRuntime(memory, workspace, clipboard)
    private val g = AgentLoopGates(sessions, providers, settings, notifier, client, runtime, gatesMap)

    suspend fun runSession(sessionId: String) {
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
        if (g.budgetHit(session0)) {
            g.finishBudget(sessionId)
            return
        }

        val skill = skills.byId(session0.skillId)
        val enabledSkills = skills.enabledSnapshot()
        val memoryText = memory.snapshotText()
        val messages = mutableListOf(
            ChatMessage(
                "system",
                "You are TAH, a phone-first agent harness. Prefer concise status. " +
                    "Real tools: memory.write, fs.read/write/list (app-private workspace only), " +
                    "clipboard.read/write (Ask-gated), web.fetch (HTTP GET), " +
                    "shell.exec (allowlist: date/echo/ls — never /bin/sh). " +
                    "No multi-agent swarm. Single run on the Signal Deck board.\n\n" +
                    "Active skill (${skill.title}):\n${skill.body}\n\n" +
                    "Enabled packs:\n$enabledSkills\n\nMemory:\n$memoryText\n\n" +
                    "Workspace:\n${workspace.snapshotText()}",
            ),
            ChatMessage("user", session0.prompt),
        )

        val nativeToolCalls = mutableListOf<ToolCall>()
        if (live) {
            g.streamLive(sessionId, messages) { toolCallReady ->
                nativeToolCalls += ToolCall(
                    id = SessionRepository.newId(),
                    sessionId = sessionId,
                    name = toolCallReady.name,
                    target = toolCallReady.target,
                    argsSummary = toolCallReady.argsSummary,
                    risk = toolCallReady.risk,
                    status = ToolStatus.Pending,
                )
            }
        } else {
            g.streamDemoText(
                sessionId,
                "I'll plan tools on the board, wait if Ask requires it, then keep going until budget or wrap-up.",
            )
        }

        val priorNames = mutableListOf<String>()
        var rejected = false
        var lastGuide: String? = null

        while (true) {
            val latest = sessions.session(sessionId) ?: break
            if (g.budgetHit(latest)) {
                g.finishBudget(sessionId)
                return
            }
            sessions.incrementIteration(sessionId)

            val tool = if (nativeToolCalls.isNotEmpty()) {
                nativeToolCalls.removeAt(0)
            } else {
                ToolPlanner.next(latest, priorNames, SessionRepository.newId())
            }
            if (tool == null) {
                sessions.appendSystem(sessionId, "No further tools planned. Wrapping the run.")
                break
            }
            priorNames += tool.name
            sessions.upsertTool(tool.copy(status = ToolStatus.Pending))
            delay(180)
            sessions.upsertTool(tool.copy(status = ToolStatus.Running))

            val decision = g.gate(latest, tool)
            when (decision) {
                AgentLoop.GateDecision.Approve -> {
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
                        g.streamLive(sessionId, messages)
                    } else {
                        g.streamDemoText(sessionId, outcome.excerpt)
                    }
                }
                AgentLoop.GateDecision.Reject -> {
                    rejected = true
                    sessions.upsertTool(
                        tool.copy(
                            status = ToolStatus.Rejected,
                            resultExcerpt = "You rejected ${tool.name}. Guide was not required.",
                        ),
                    )
                    sessions.appendSystem(sessionId, "You rejected ${tool.name}. That tool call ended.")
                    g.streamDemoText(sessionId, "Understood — I will not run ${tool.name}. Closing the turn.")
                    break
                }
                is AgentLoop.GateDecision.Guide -> {
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
                        g.streamLive(sessionId, messages)
                    } else {
                        g.streamDemoText(sessionId, "Guided: ${decision.instruction} — continuing if budget remains.")
                    }
                }
            }
        }

        if (!rejected) {
            val wrap = lastGuide?.let { "Guided run complete ($it)." } ?: "Board will move this session to Done."
            g.streamDemoText(sessionId, wrap)
        }
        sessions.markDone(sessionId, DoneChip.None)
    }

    suspend fun continueSeeded(sessionId: String, decision: AgentLoop.GateDecision) =
        g.continueSeeded(sessionId, decision)
}
