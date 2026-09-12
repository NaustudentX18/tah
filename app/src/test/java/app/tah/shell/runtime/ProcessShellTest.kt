package app.tah.shell.runtime

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class ProcessShellTest {

    @Test
    fun runsBasicCommandSuccessfully() {
        val isWindows = System.getProperty("os.name")?.lowercase()?.contains("windows") == true
        val command = if (isWindows) "Write-Output 'Hello ProcessShell'" else "echo 'Hello ProcessShell'"
        val result = ProcessShell.run(command)

        assertTrue(result.ok)
        assertTrue(result.output.contains("Hello ProcessShell"))
        assertFalse(result.isTimeout)
    }

    @Test
    fun handlesEmptyCommandGracefully() {
        val result = ProcessShell.run("   ")
        assertFalse(result.ok)
        assertTrue(result.output.contains("Empty command"))
    }

    @Test
    fun enforcesTimeoutOnHangingProcess() {
        val isWindows = System.getProperty("os.name")?.lowercase()?.contains("windows") == true
        // Short timeout of 500ms with a 3000ms sleep
        val command = if (isWindows) "Start-Sleep -Milliseconds 3000" else "sleep 3"
        val result = ProcessShell.run(command, timeoutMs = 500L)

        assertFalse(result.ok)
        assertTrue(result.isTimeout)
        assertTrue(result.output.contains("timed out"))
    }

    @Test
    fun respectsWorkingDirectory() {
        val tempDir = File(System.getProperty("java.io.tmpdir"), "tah_test_dir").apply { mkdirs() }
        val testFile = File(tempDir, "marker.txt").apply { writeText("tah_marker_content") }

        try {
            val isWindows = System.getProperty("os.name")?.lowercase()?.contains("windows") == true
            val command = if (isWindows) "Get-Content marker.txt" else "cat marker.txt"
            val result = ProcessShell.run(command, workingDir = tempDir)

            assertTrue(result.ok)
            assertTrue(result.output.contains("tah_marker_content"))
        } finally {
            testFile.delete()
            tempDir.delete()
        }
    }
}
