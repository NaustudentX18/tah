package app.tah.shell.runtime

import app.tah.shell.data.AgentSession
import app.tah.shell.data.ToolCall
import app.tah.shell.data.ToolRisk
import app.tah.shell.data.ToolStatus

/**
 * Plans the next on-device tool for a run.
 *
 * Honest contract:
 * - Deterministic from the prompt + prior tool names (not model tool-JSON).
 * - Returns null when the loop should wrap up.
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
        val url = UrlSupport.firstHttpUrl(prompt)
        val file = WorkspaceNames.filenameFromPrompt(prompt)

        val sequence = buildList {
            when {
                listOf("shell", "exec", "run command", "bash", "terminal", "date", "echo ").any { it in p } -> {
                    val cmd = when {
                        p.contains("ls") -> "ls"
                        p.contains("date") -> "date"
                        p.contains("echo") -> "echo ${prompt.take(80)}"
                        else -> "ls"
                    }
                    add(Proposal("shell.exec", cmd, "in-process allowlist — not /bin/sh", ToolRisk.Exec))
                    add(Proposal("memory.write", "Skills & Memory", "record the command result", ToolRisk.Write))
                }
                url != null || listOf("http", "fetch", "web", "url").any { it in p } -> {
                    add(
                        Proposal(
                            "web.fetch",
                            url ?: "https://example.com",
                            "GET the card URL after Ask (32 KiB cap)",
                            ToolRisk.Network,
                        ),
                    )
                    add(Proposal("memory.write", "Skills & Memory", "save the fetch excerpt", ToolRisk.Write))
                }
                listOf("read", "open", "inspect", "show file").any { it in p } && !p.contains("write") -> {
                    add(Proposal("fs.read", file, "read app-private workspace file", ToolRisk.Read))
                    add(Proposal("memory.write", "Skills & Memory", "keep an excerpt as a note", ToolRisk.Write))
                }
                listOf("remember", "memory", "note this", "save note").any { it in p } &&
                    !listOf("write file", "save file", "create file").any { it in p } -> {
                    add(Proposal("memory.write", "Skills & Memory", "write a note the next run can read", ToolRisk.Write))
                }
                else -> {
                    add(Proposal("fs.write", file, "write app-private workspace file", ToolRisk.Write))
                    add(Proposal("memory.write", "Skills & Memory", "store a pointer to the file", ToolRisk.Write))
                }
            }
        }

        return sequence.firstOrNull { it.name.lowercase() !in used }
    }
}
