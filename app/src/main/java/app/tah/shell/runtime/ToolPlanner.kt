package app.tah.shell.runtime

import app.tah.shell.data.AgentSession
import app.tah.shell.data.ToolCall
import app.tah.shell.data.ToolRisk
import app.tah.shell.data.ToolStatus

/**
 * Plans the next on-device tool for a run.
 *
 * Honest contract:
 * - Plans are deterministic from the prompt + prior tool names, not model function-calling JSON.
 * - Returns null when the loop should wrap up (no more tools this run).
 * - Does not invent FS/shell execution.
 */
object ToolPlanner {
    data class Proposal(
        val name: String,
        val target: String,
        val summary: String,
        val risk: ToolRisk,
    )

    fun next(session: AgentSession, priorNames: List<String>, toolId: String): ToolCall? {
        val proposal = propose(session.prompt, priorNames) ?: return null
        return ToolCall(
            id = toolId,
            sessionId = session.id,
            name = proposal.name,
            target = proposal.target,
            argsSummary = proposal.summary,
            risk = proposal.risk,
            status = ToolStatus.Pending,
        )
    }

    fun propose(prompt: String, priorNames: List<String>): Proposal? {
        val used = priorNames.map { it.lowercase() }.toSet()
        val p = prompt.lowercase()

        val sequence = buildList {
            when {
                listOf("shell", "exec", "run command", "bash", "terminal").any { it in p } -> {
                    add(Proposal("shell.exec", "sh -c …", "exec stays Ask even if Allow edits is on", ToolRisk.Exec))
                    add(Proposal("memory.write", "Skills & Memory", "record what you refused or approved", ToolRisk.Write))
                }
                listOf("http", "search", "fetch", "web", "url").any { it in p } -> {
                    add(Proposal("web.fetch", "https://example.invalid/docs", "GET summary — no vendor scrape", ToolRisk.Network))
                    add(Proposal("memory.write", "Skills & Memory", "save the fetch receipt as a note", ToolRisk.Write))
                }
                listOf("read", "open", "inspect").any { it in p } && !p.contains("write") -> {
                    add(Proposal("fs.read", "notes.md", "read excerpt — receipt only, not device FS", ToolRisk.Read))
                    add(Proposal("memory.write", "Skills & Memory", "keep the excerpt as a memory note", ToolRisk.Write))
                }
                listOf("remember", "memory", "note this", "save note").any { it in p } -> {
                    add(Proposal("memory.write", "Skills & Memory", "write a note the next run can read", ToolRisk.Write))
                }
                else -> {
                    add(Proposal("fs.write", "notes.md", "draft a short patch — receipt only, not device FS", ToolRisk.Write))
                    add(Proposal("memory.write", "Skills & Memory", "store the draft summary on-device", ToolRisk.Write))
                }
            }
        }

        return sequence.firstOrNull { it.name.lowercase() !in used }
    }
}
