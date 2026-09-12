package app.tah.shell.runtime

import app.tah.shell.data.MemoryNote
import app.tah.shell.data.MemoryStore
import app.tah.shell.data.SessionRepository
import app.tah.shell.data.ToolCall
import app.tah.shell.data.WorkspaceStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Applies approved tools.
 *
 * Real effects:
 * - memory.write — MemoryStore note
 * - fs.read / fs.write — app-private workspace (not shared storage)
 * - web.fetch — HTTP GET of the card target
 * - shell.exec — in-process allowlist only
 */
class ToolRuntime(
    private val memory: MemoryStore,
    private val workspace: WorkspaceStore,
    private val http: HttpFetcher = HttpFetcher(),
) {
    data class Outcome(
        val excerpt: String,
        val appliedForReal: Boolean,
    )

    suspend fun apply(tool: ToolCall, prompt: String): Outcome = withContext(Dispatchers.IO) {
        when (tool.name) {
            "memory.write" -> writeMemory(tool, prompt)
            "fs.read" -> readWorkspace(tool)
            "fs.write" -> writeWorkspace(tool, prompt)
            "web.fetch" -> fetch(tool, prompt)
            "shell.exec" -> exec(tool)
            else -> Outcome(
                excerpt = "Unknown tool ${tool.name}. Recorded; nothing else ran.",
                appliedForReal = false,
            )
        }
    }

    private fun writeMemory(tool: ToolCall, prompt: String): Outcome {
        val title = prompt.lineSequence().firstOrNull { it.isNotBlank() }?.take(48) ?: "Run note"
        val body = buildString {
            append("From ").append(tool.name).append(" on ").append(tool.target).append(". ")
            append(prompt.take(280).ifBlank { tool.argsSummary })
        }
        memory.upsert(
            MemoryNote(
                id = SessionRepository.newId(),
                title = title,
                body = body,
                updatedAt = System.currentTimeMillis(),
            ),
        )
        return Outcome("Wrote an on-device memory note.", true)
    }

    private fun readWorkspace(tool: ToolCall): Outcome {
        val name = WorkspaceNames.sanitize(tool.target)
        val body = workspace.read(name)
        return if (body.isBlank()) {
            Outcome("Workspace file $name is empty or missing. App-private dir only — not phone storage.", true)
        } else {
            Outcome("Read workspace/$name (${body.length} chars):\n${body.take(800)}", true)
        }
    }

    private fun writeWorkspace(tool: ToolCall, prompt: String): Outcome {
        val name = WorkspaceNames.sanitize(tool.target.ifBlank { WorkspaceNames.filenameFromPrompt(prompt) })
        val body = buildString {
            append("# ").append(name).append('\n')
            append("Written by TAH workspace (app-private).\n\n")
            append(prompt.take(2_000).ifBlank { tool.argsSummary })
        }
        val file = workspace.write(name, body)
        return Outcome("Wrote workspace/${file.name} (${file.bytes} bytes). Not shared storage.", true)
    }

    private fun fetch(tool: ToolCall, prompt: String): Outcome {
        val url = UrlSupport.firstHttpUrl(tool.target)
            ?: UrlSupport.firstHttpUrl(prompt)
            ?: tool.target.takeIf { UrlSupport.isAllowed(it) }
        if (url == null) {
            return Outcome("No http(s) URL on the card. Fetch skipped.", false)
        }
        val result = http.get(url)
        return Outcome(result.excerpt, result.ok)
    }

    private fun exec(tool: ToolCall): Outcome {
        val command = tool.target.ifBlank { tool.argsSummary }
        val (ok, text) = InProcessShell.run(command, workspace)
        return Outcome(text, ok)
    }
}
