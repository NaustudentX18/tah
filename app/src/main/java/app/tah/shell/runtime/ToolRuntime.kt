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
 * Real effects (sandboxed):
 * - memory.write — MemoryStore note
 * - fs.read / fs.write / fs.list — app-private workspace only (filesDir/workspace)
 * - clipboard.read / clipboard.write — device clipboard after Ask card
 * - web.fetch — HTTP GET of the card target
 * - shell.exec — in-process allowlist (date, echo, ls). Never /bin/sh.
 */
class ToolRuntime(
    private val memory: MemoryStore,
    private val workspace: WorkspaceStore,
    private val clipboard: ClipboardAccess = FakeClipboard(),
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
            "fs.list" -> listWorkspace(tool)
            "clipboard.read" -> readClipboard()
            "clipboard.write" -> writeClipboard(tool, prompt)
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
            Outcome("Workspace file $name is empty or missing.", true)
        } else {
            Outcome("Read workspace/$name (${body.length} chars):\n${body.take(800)}", true)
        }
    }

    private fun writeWorkspace(tool: ToolCall, prompt: String): Outcome {
        val name = WorkspaceNames.sanitize(
            tool.target.ifBlank { WorkspaceNames.filenameFromPrompt(prompt) },
        )
        val content = if (tool.argsSummary.isNotBlank() && !tool.argsSummary.startsWith("write ")) {
            tool.argsSummary
        } else {
            buildString {
                append("# ").append(name).append('\n')
                append("Written by TAH.\n\n")
                append(prompt.take(2_000))
            }
        }
        val resultFile = workspace.write(name, content)
        return Outcome("Wrote workspace/${resultFile.name} (${resultFile.bytes} bytes).", true)
    }

    private fun listWorkspace(tool: ToolCall): Outcome {
        val names = workspace.listNames()
        return Outcome(
            if (names.isEmpty()) "(workspace empty — app-private filesDir/workspace only)"
            else "Workspace (${names.size}):\n" + names.joinToString("\n"),
            true,
        )
    }

    private fun readClipboard(): Outcome {
        val text = clipboard.read()
        return if (text.isBlank()) {
            Outcome("Clipboard is empty.", true)
        } else {
            Outcome("Clipboard (${text.length} chars):\n${text.take(800)}", true)
        }
    }

    private fun writeClipboard(tool: ToolCall, prompt: String): Outcome {
        val text = tool.argsSummary.ifBlank { tool.target }.ifBlank { prompt.take(500) }
        if (text.isBlank()) {
            return Outcome("Nothing to write to clipboard.", false)
        }
        val ok = clipboard.write(text)
        return if (ok) {
            Outcome("Wrote ${text.length} chars to clipboard.", true)
        } else {
            Outcome("Clipboard write failed.", false)
        }
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
        val (ok, text) = InProcessShell.run(command, workspace.listNames())
        return Outcome(text, ok)
    }
}
