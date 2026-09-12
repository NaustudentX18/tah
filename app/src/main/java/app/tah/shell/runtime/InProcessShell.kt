package app.tah.shell.runtime

import app.tah.shell.data.WorkspaceStore
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Shell execution engine.
 * Supports quick in-process built-ins (date, echo, ls) and falls back to
 * real process execution via ProcessShell (/system/bin/sh or host shell).
 */
object InProcessShell {
    fun run(
        command: String,
        workspace: WorkspaceStore,
        allowRealProcess: Boolean = true,
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
                val names = workspace.listNames()
                true to if (names.isEmpty()) "(workspace empty)" else names.joinToString("\n")
            }
            else -> {
                if (allowRealProcess) {
                    val res = ProcessShell.run(
                        command = trimmed,
                        workingDir = workspace.rootDir,
                    )
                    res.ok to res.output
                } else {
                    false to "Refused. Safe allowlist mode only (date, echo, ls). Command was: ${trimmed.take(80)}"
                }
            }
        }
    }
}
