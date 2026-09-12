package app.tah.shell.data

enum class SessionColumn { Working, NeedsYou, Done }

enum class DoneChip { None, Failed, BudgetHit, Cancelled }

enum class PermissionMode { Ask, AllowReads, AllowEdits }

enum class InputMode { TouchSteer, Keys }

enum class ProviderKind { Demo, Byok, OllamaLan }

enum class ToolStatus {
    Pending,
    Running,
    AwaitingPermission,
    Succeeded,
    Failed,
    Rejected,
}

enum class ToolRisk { Read, Write, Exec, Network }

data class AgentSession(
    val id: String,
    val title: String,
    val prompt: String,
    val skillId: String?,
    val modelId: String,
    val providerKind: ProviderKind,
    val column: SessionColumn,
    val doneChip: DoneChip = DoneChip.None,
    val iterationBudget: Int = 8,
    val wallClockMs: Long = 5 * 60_000L,
    val iterationsUsed: Int = 0,
    val startedAt: Long,
    val updatedAt: Long,
    val lastToolName: String? = null,
    val pendingPermissionId: String? = null,
    val isDemoSeed: Boolean = false,
)

data class ToolCall(
    val id: String,
    val sessionId: String,
    val name: String,
    val target: String,
    val argsSummary: String,
    val risk: ToolRisk,
    val status: ToolStatus,
    val resultExcerpt: String = "",
    val durationMs: Long? = null,
)

sealed class TimelineItem {
    abstract val id: String
    abstract val sessionId: String
    abstract val at: Long

    data class Stream(
        override val id: String,
        override val sessionId: String,
        override val at: Long,
        val text: String,
    ) : TimelineItem()

    data class Tool(
        override val id: String,
        override val sessionId: String,
        override val at: Long,
        val tool: ToolCall,
    ) : TimelineItem()

    data class System(
        override val id: String,
        override val sessionId: String,
        override val at: Long,
        val text: String,
    ) : TimelineItem()
}

data class PendingPermission(
    val id: String,
    val sessionId: String,
    val toolId: String,
    val toolName: String,
    val risk: ToolRisk,
    val summary: String,
    val target: String,
)

data class ProviderSnapshot(
    val kind: ProviderKind = ProviderKind.Demo,
    val baseUrl: String = "",
    val modelId: String = "demo-offline",
    val hasKey: Boolean = false,
    val lastProbeOk: Boolean? = null,
    val lastProbeDetail: String = "",
    val models: List<String> = emptyList(),
) {
    val isLive: Boolean
        get() = when (kind) {
            ProviderKind.Demo -> false
            ProviderKind.Byok -> baseUrl.isNotBlank() && hasKey && modelId.isNotBlank()
            ProviderKind.OllamaLan -> baseUrl.isNotBlank() && modelId.isNotBlank()
        }
}

data class UserSettings(
    val permissionMode: PermissionMode = PermissionMode.Ask,
    val inputMode: InputMode = InputMode.TouchSteer,
    val notificationsEnabled: Boolean = true,
    val onboardingComplete: Boolean = false,
)

data class SkillPack(
    val id: String,
    val title: String,
    val body: String,
    val enabled: Boolean = true,
    val bundled: Boolean = true,
)

data class MemoryNote(
    val id: String,
    val title: String,
    val body: String,
    val updatedAt: Long,
)

data class SessionGraph(
    val sessions: List<AgentSession> = emptyList(),
    val timelines: Map<String, List<TimelineItem>> = emptyMap(),
    val pending: Map<String, PendingPermission> = emptyMap(),
)

data class ChatMessage(
    val role: String,
    val content: String,
)
