package app.tah.shell.runtime

import app.tah.shell.data.MemoryNote
import app.tah.shell.data.MemoryStore
import app.tah.shell.data.SessionRepository
import app.tah.shell.data.ToolCall

/**
 * Applies approved tools with an honest effect surface.
 *
 * Real effect today:
 * - memory.write → persists a MemoryNote the next agent loop will read
 *
 * Receipt-only (explicitly labeled, never claimed as device FS/shell):
 * - fs.read / fs.write / web.fetch / shell.exec
 */
class ToolRuntime(
    private val memory: MemoryStore,
) {
    data class Outcome(
        val excerpt: String,
        val appliedForReal: Boolean,
    )

    fun apply(tool: ToolCall, prompt: String): Outcome = when (tool.name) {
        "memory.write" -> {
            val title = prompt.lineSequence().firstOrNull { it.isNotBlank() }?.take(48) ?: "Run note"
            val body = buildString {
                append("From tool card ").append(tool.name).append(" on ").append(tool.target).append(". ")
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
            Outcome(
                excerpt = "Wrote an on-device memory note. Next run will see it under Skills & Memory.",
                appliedForReal = true,
            )
        }
        "fs.read" -> Outcome(
            excerpt = "Receipt only. TAH did not open a real file on this phone. Card exists so the board stays honest.",
            appliedForReal = false,
        )
        "fs.write" -> Outcome(
            excerpt = "Receipt only. No file was created on device storage. Approve ≠ silent disk write.",
            appliedForReal = false,
        )
        "web.fetch" -> Outcome(
            excerpt = "Receipt only. No live scrape ran. Point a BYOK/Ollama model at a real URL in a later milestone.",
            appliedForReal = false,
        )
        "shell.exec" -> Outcome(
            excerpt = "Receipt only. No shell ran. Exec stays gated and never silently executes.",
            appliedForReal = false,
        )
        else -> Outcome(
            excerpt = "Unknown tool ${tool.name}. Recorded as a receipt; nothing else ran.",
            appliedForReal = false,
        )
    }
}
