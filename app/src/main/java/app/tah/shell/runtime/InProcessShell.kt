package app.tah.shell.runtime

import app.tah.shell.data.WorkspaceStore
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Tiny allowlist. This is not /bin/sh and never becomes /bin/sh.
 */
object InProcessShell {
    fun run(command: String, workspace: WorkspaceStore): Pair<Boolean, String> {
        val trimmed = command.trim()
        if (trimmed.isBlank()) {
            return false to "Empty command. Nothing ran."
        }
        val head = trimmed.substringBefore(' ').lowercase()
        return when (head) {
            "date" -> true to SimpleDateFormat("yyyy-MM-dd HH:mm:ss Z", Locale.US).format(Date())
            "echo" -> true to trimmed.removePrefix("echo").trim()
            "ls", "workspace.ls" -> {
                val names = workspace.listNames()
                true to if (names.isEmpty()) "(workspace empty)" else names.joinToString("\n")
            }
            else -> false to
                "Refused. TAH shell is an in-process allowlist (date, echo, ls). " +
                    "No /bin/sh. Command was: ${trimmed.take(80)}"
        }
    }
}
