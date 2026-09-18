package app.tah.shell.runtime

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class InProcessShellTest {
    @Test
    fun dateAndEchoAllowed() {
        val (okDate, outDate) = InProcessShell.run("date")
        assertTrue(okDate)
        assertTrue(outDate.isNotBlank())

        val (okEcho, outEcho) = InProcessShell.run("echo hello-tah")
        assertTrue(okEcho)
        assertTrue(outEcho.contains("hello-tah"))
    }

    @Test
    fun lsListsWorkspaceNames() {
        val (ok, out) = InProcessShell.run("ls", listOf("a.md", "b.txt"))
        assertTrue(ok)
        assertTrue(out.contains("a.md"))
        assertTrue(out.contains("b.txt"))
    }

    @Test
    fun refusesArbitraryCommands() {
        val (ok, out) = InProcessShell.run("rm -rf /")
        assertFalse(ok)
        assertTrue(out.contains("Allowlist only"))
        assertTrue(out.contains("Not /bin/sh"))
    }

    @Test
    fun emptyCommandFails() {
        val (ok, out) = InProcessShell.run("   ")
        assertFalse(ok)
        assertTrue(out.contains("Empty"))
    }
}
