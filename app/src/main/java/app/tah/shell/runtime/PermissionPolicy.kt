package app.tah.shell.runtime

import app.tah.shell.data.PermissionMode
import app.tah.shell.data.ToolRisk

/**
 * Locked CoS rules:
 * - Ask is default for write/exec-class tools
 * - Allow edits ≠ exec — filesystem writes may auto-allow; shell exec stays Ask
 * - No silent full-bypass path
 */
object PermissionPolicy {
    fun requiresAsk(risk: ToolRisk, mode: PermissionMode): Boolean = when (risk) {
        ToolRisk.Read -> mode == PermissionMode.Ask
        ToolRisk.Write -> mode != PermissionMode.AllowEdits
        ToolRisk.Network -> true
        ToolRisk.Exec -> true
    }
}
