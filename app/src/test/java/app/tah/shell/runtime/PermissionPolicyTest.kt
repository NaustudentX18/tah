package app.tah.shell.runtime

import app.tah.shell.data.PermissionMode
import app.tah.shell.data.ToolRisk
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PermissionPolicyTest {
    @Test
    fun execAlwaysAsks() {
        assertTrue(PermissionPolicy.requiresAsk(ToolRisk.Exec, PermissionMode.AllowEdits))
        assertTrue(PermissionPolicy.requiresAsk(ToolRisk.Exec, PermissionMode.AllowReads))
        assertTrue(PermissionPolicy.requiresAsk(ToolRisk.Network, PermissionMode.AllowEdits))
    }

    @Test
    fun allowEditsSkipsWriteAsk() {
        assertFalse(PermissionPolicy.requiresAsk(ToolRisk.Write, PermissionMode.AllowEdits))
        assertTrue(PermissionPolicy.requiresAsk(ToolRisk.Write, PermissionMode.Ask))
    }

    @Test
    fun allowReadsSkipsReadAsk() {
        assertFalse(PermissionPolicy.requiresAsk(ToolRisk.Read, PermissionMode.AllowReads))
        assertTrue(PermissionPolicy.requiresAsk(ToolRisk.Read, PermissionMode.Ask))
    }
}
