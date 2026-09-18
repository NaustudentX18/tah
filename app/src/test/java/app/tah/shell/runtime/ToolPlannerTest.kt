package app.tah.shell.runtime

import app.tah.shell.data.ToolRisk
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ToolPlannerTest {
    @Test
    fun rememberPromptPlansMemoryWrite() {
        val first = ToolPlanner.propose("Please remember this lock code later", emptyList())
        assertEquals("memory.write", first?.name)
        assertNull(ToolPlanner.propose("Please remember this lock code later", listOf("memory.write")))
    }

    @Test
    fun urlPromptPlansFetchThenMemory() {
        val first = ToolPlanner.propose("fetch https://example.com/docs", emptyList())
        assertEquals("web.fetch", first?.name)
        assertEquals("https://example.com/docs", first?.target)
        assertEquals(ToolRisk.Network, first?.risk)
        val second = ToolPlanner.propose("fetch https://example.com/docs", listOf("web.fetch"))
        assertEquals("memory.write", second?.name)
    }

    @Test
    fun defaultPromptPlansWorkspaceWrite() {
        val first = ToolPlanner.propose("Draft a short status for the board", emptyList())
        assertEquals("fs.write", first?.name)
        assertTrue(first?.target?.endsWith(".md") == true)
    }

    @Test
    fun clipboardPromptPlansRead() {
        val first = ToolPlanner.propose("Read my clipboard and summarize", emptyList())
        assertEquals("clipboard.read", first?.name)
        assertEquals(ToolRisk.Read, first?.risk)
    }

    @Test
    fun wrapWhenSequenceExhausted() {
        val p = "Draft a short status"
        assertNull(ToolPlanner.propose(p, listOf("fs.write", "memory.write")))
    }
}
