package app.tah.shell.runtime

import app.tah.shell.data.MemoryNote
import app.tah.shell.data.MemoryStore
import app.tah.shell.data.SessionRepository
import app.tah.shell.data.ToolCall
import app.tah.shell.data.WorkspaceStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

import java.io.File

/**
 * Applies approved tools.
 *
 * Real effects:
 * - memory.write — MemoryStore note
 * - fs.read / fs.write — app-private workspace or device filesystem paths
 * - fs.list — list workspace or device directories
 * - web.fetch — HTTP GET of the card target
 * - shell.exec — real process shell or in-process allowlist
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
            "fs.list" -> listFiles(tool)
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
        val target = tool.target.trim()
        val isExplicitPath = target.startsWith("/") || target.startsWith("./")
        val file = if (isExplicitPath) File(target) else null

        if (file != null) {
            return if (!file.exists()) {
                Outcome("File ${file.absolutePath} does not exist.", false)
            } else if (!file.canRead()) {
                Outcome("Permission denied reading ${file.absolutePath}.", false)
            } else {
                val text = runCatching { file.readText().take(WorkspaceStore.MAX_CHARS) }.getOrNull()
                if (text == null) {
                    Outcome("Failed to read ${file.absolutePath}.", false)
                } else {
                    Outcome("Read ${file.absolutePath} (${text.length} chars):\n${text.take(800)}", true)
                }
            }
        }

        val name = WorkspaceNames.sanitize(target)
        val body = workspace.read(name)
        return if (body.isBlank()) {
            Outcome("Workspace file $name is empty or missing.", true)
        } else {
            Outcome("Read workspace/$name (${body.length} chars):\n${body.take(800)}", true)
        }
    }

    private fun writeWorkspace(tool: ToolCall, prompt: String): Outcome {
        val target = tool.target.trim()
        val isExplicitPath = target.startsWith("/") || target.startsWith("./")
        val file = if (isExplicitPath) File(target) else null

        val content = if (tool.argsSummary.isNotBlank() && !tool.argsSummary.startsWith("write ")) {
            tool.argsSummary
        } else {
            buildString {
                append("# ").append(target).append('\n')
                append("Written by TAH Agent.\n\n")
                append(prompt.take(2_000))
            }
        }

        if (file != null) {
            return try {
                file.parentFile?.mkdirs()
                file.writeText(content.take(WorkspaceStore.MAX_CHARS))
                Outcome("Wrote ${file.absolutePath} (${file.length()} bytes).", true)
            } catch (t: Throwable) {
                Outcome("Failed writing to ${file.absolutePath}: ${t.message}", false)
            }
        }

        val name = WorkspaceNames.sanitize(target.ifBlank { WorkspaceNames.filenameFromPrompt(prompt) })
        val resultFile = workspace.write(name, content)
        return Outcome("Wrote workspace/${resultFile.name} (${resultFile.bytes} bytes).", true)
    }

    private fun listFiles(tool: ToolCall): Outcome {
        val target = tool.target.trim()
        val dir = if (target.isNotBlank() && target != "workspace") File(target) else workspace.rootDir
        if (!dir.exists()) {
            return Outcome("Directory does not exist: ${dir.absolutePath}", false)
        }
        val entries = dir.listFiles()?.take(100)?.map {
            if (it.isDirectory) "${it.name}/" else "${it.name} (${it.length()} bytes)"
        }?.sorted() ?: emptyList()

        return Outcome(
            if (entries.isEmpty()) "(empty directory: ${dir.absolutePath})"
            else "Directory ${dir.absolutePath}:\n" + entries.joinToString("\n"),
            true,
        )
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
