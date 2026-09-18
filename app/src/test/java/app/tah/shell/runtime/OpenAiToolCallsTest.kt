package app.tah.shell.runtime

import app.tah.shell.data.ToolRisk
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class OpenAiToolCallsTest {

    private val client = OpenAiCompatClient()

    @Test
    fun generatesCompleteToolsSchema() {
        val tools = client.createToolsJson()
        assertEquals(8, tools.length())

        val toolNames = mutableListOf<String>()
        for (i in 0 until tools.length()) {
            val fn = tools.getJSONObject(i).getJSONObject("function")
            toolNames += fn.getString("name")
            assertTrue(fn.has("description"))
            assertTrue(fn.has("parameters"))
        }

        assertTrue(toolNames.contains("shell_exec"))
        assertTrue(toolNames.contains("fs_read"))
        assertTrue(toolNames.contains("fs_write"))
        assertTrue(toolNames.contains("fs_list"))
        assertTrue(toolNames.contains("web_fetch"))
        assertTrue(toolNames.contains("memory_write"))
        assertTrue(toolNames.contains("clipboard_read"))
        assertTrue(toolNames.contains("clipboard_write"))
        assertTrue(!toolNames.contains("agent_spawn"))
    }

    @Test
    fun mapsShellExecCorrectly() {
        val event = client.mapToolCall("shell_exec", """{"command":"ls -la"}""")
        assertNotNull(event)
        assertEquals("shell.exec", event?.name)
        assertEquals("ls -la", event?.target)
        assertEquals(ToolRisk.Exec, event?.risk)
    }

    @Test
    fun mapsFsReadAndWriteCorrectly() {
        val readEvent = client.mapToolCall("fs_read", """{"path":"test.json"}""")
        assertNotNull(readEvent)
        assertEquals("fs.read", readEvent?.name)
        assertEquals("test.json", readEvent?.target)
        assertEquals(ToolRisk.Read, readEvent?.risk)

        val writeEvent = client.mapToolCall("fs_write", """{"path":"test.json","content":"{\"ok\":true}"}""")
        assertNotNull(writeEvent)
        assertEquals("fs.write", writeEvent?.name)
        assertEquals("test.json", writeEvent?.target)
        assertEquals("""{"ok":true}""", writeEvent?.argsSummary)
        assertEquals(ToolRisk.Write, writeEvent?.risk)
    }

    @Test
    fun mapsClipboardTools() {
        val read = client.mapToolCall("clipboard_read", "{}")
        assertNotNull(read)
        assertEquals("clipboard.read", read?.name)
        assertEquals(ToolRisk.Read, read?.risk)

        val write = client.mapToolCall("clipboard_write", """{"text":"hello board"}""")
        assertNotNull(write)
        assertEquals("clipboard.write", write?.name)
        assertEquals("hello board", write?.argsSummary)
        assertEquals(ToolRisk.Write, write?.risk)
    }

    @Test
    fun ignoresUnknownTools() {
        val event = client.mapToolCall("unknown_teleport", "{}")
        assertNull(event)
        assertNull(client.mapToolCall("agent_spawn", """{"role":"Planner","prompt":"x"}"""))
    }
}
