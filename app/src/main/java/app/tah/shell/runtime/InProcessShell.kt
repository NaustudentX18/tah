package app.tah.shell.runtime

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * In-process shell allowlist only.
 * Never spawns /bin/sh or /system/bin/sh — that stays out of product.
 */
object InProcessShell {
    fun run(
        command: String,
        workspaceNames: List<String> = emptyList(),
    ): Pair<Boolean, String> {
        val trimmed = command.trim()
        if (trimmed.isBlank()) {
            return false to "Empty command. Nothing ran."
        }
        val head = trimmed.substringBefore(' ').lowercase()
        return when (head) {
            "date" -> true to SimpleDateFormat("yyyy-MM-dd HH:mm:ss Z", Locale.US).format(Date())
            "echo" -> true to trimmed.removePrefix("echo").trim()
            "ls", "workspace.ls" -> {
                true to if (workspaceNames.isEmpty()) "(workspace empty)" else workspaceNames.joinToString("\n")
            }
            else -> false to "Refused. Allowlist only (date, echo, ls). Not /bin/sh. Command was: ${trimmed.take(80)}"
        }
    }
}
