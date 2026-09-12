package app.tah.shell.runtime

import java.io.File
import java.util.concurrent.TimeUnit

/**
 * Real process execution engine supporting Android (/system/bin/sh) and host JVM environments.
 * Captures exit codes, stdout, stderr, and enforces execution timeouts.
 */
object ProcessShell {

    data class Result(
        val ok: Boolean,
        val exitCode: Int,
        val output: String,
        val isTimeout: Boolean = false,
    )

    fun run(
        command: String,
        workingDir: File? = null,
        timeoutMs: Long = 15_000L,
        maxOutputBytes: Int = 16_384,
    ): Result {
        val trimmed = command.trim()
        if (trimmed.isBlank()) {
            return Result(ok = false, exitCode = -1, output = "Empty command. Nothing executed.")
        }

        val isWindows = System.getProperty("os.name")?.lowercase()?.contains("windows") == true
        val androidShell = File("/system/bin/sh")
        val commandList = when {
            androidShell.exists() -> listOf("/system/bin/sh", "-c", trimmed)
            isWindows -> listOf("powershell.exe", "-NoProfile", "-NonInteractive", "-Command", trimmed)
            else -> listOf("/bin/sh", "-c", trimmed)
        }

        return try {
            val process = ProcessBuilder(commandList)
                .apply {
                    if (workingDir != null && workingDir.exists()) {
                        directory(workingDir)
                    }
                    redirectErrorStream(true)
                }
                .start()

            val completed = process.waitFor(timeoutMs, TimeUnit.MILLISECONDS)
            if (!completed) {
                process.destroyForcibly()
                return Result(
                    ok = false,
                    exitCode = -1,
                    output = "Command timed out after ${timeoutMs / 1000}s. Process terminated.",
                    isTimeout = true,
                )
            }

            val rawOutput = process.inputStream.bufferedReader().use { it.readText() }
            val truncated = if (rawOutput.length > maxOutputBytes) {
                rawOutput.take(maxOutputBytes) + "\n... [truncated, ${rawOutput.length} total chars]"
            } else {
                rawOutput
            }

            val exitCode = process.exitValue()
            val text = if (truncated.isBlank()) {
                if (exitCode == 0) "(command completed with no output)" else "Exit code: $exitCode"
            } else {
                truncated.trimEnd()
            }

            Result(
                ok = (exitCode == 0),
                exitCode = exitCode,
                output = text,
            )
        } catch (t: Throwable) {
            Result(
                ok = false,
                exitCode = -1,
                output = "Execution failed: ${t.message ?: t.javaClass.simpleName}",
            )
        }
    }
}
