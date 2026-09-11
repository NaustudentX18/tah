package app.tah.shell.data

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

class SessionRepository(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
    private val _graph = MutableStateFlow(loadOrSeed())
    val graph: StateFlow<SessionGraph> = _graph.asStateFlow()

    fun sessions(): List<AgentSession> = _graph.value.sessions

    fun session(id: String): AgentSession? = _graph.value.sessions.firstOrNull { it.id == id }

    fun timeline(id: String): List<TimelineItem> = _graph.value.timelines[id].orEmpty()

    fun pendingFor(sessionId: String): PendingPermission? = _graph.value.pending[sessionId]

    fun needsYouCount(): Int = _graph.value.sessions.count { it.column == SessionColumn.NeedsYou }

    fun upsertSession(session: AgentSession) {
        mutate { graph ->
            val list = graph.sessions.toMutableList()
            val idx = list.indexOfFirst { it.id == session.id }
            if (idx >= 0) list[idx] = session else list.add(0, session)
            graph.copy(sessions = list.sortedByDescending { it.updatedAt })
        }
    }

    fun updateSession(id: String, transform: (AgentSession) -> AgentSession) {
        val current = session(id) ?: return
        upsertSession(transform(current).copy(updatedAt = System.currentTimeMillis()))
    }

    fun appendSystem(sessionId: String, text: String) {
        appendItem(
            TimelineItem.System(
                id = newId(),
                sessionId = sessionId,
                at = System.currentTimeMillis(),
                text = text,
            ),
        )
    }

    fun appendToken(sessionId: String, token: String) {
        mutate { graph ->
            val items = graph.timelines[sessionId].orEmpty().toMutableList()
            val last = items.lastOrNull()
            if (last is TimelineItem.Stream) {
                items[items.lastIndex] = last.copy(text = last.text + token)
            } else {
                items += TimelineItem.Stream(
                    id = newId(),
                    sessionId = sessionId,
                    at = System.currentTimeMillis(),
                    text = token,
                )
            }
            graph.copy(timelines = graph.timelines + (sessionId to items))
        }
        touch(sessionId)
    }

    fun upsertTool(tool: ToolCall) {
        mutate { graph ->
            val items = graph.timelines[tool.sessionId].orEmpty().toMutableList()
            val idx = items.indexOfFirst { it is TimelineItem.Tool && it.tool.id == tool.id }
            val item = TimelineItem.Tool(
                id = tool.id,
                sessionId = tool.sessionId,
                at = System.currentTimeMillis(),
                tool = tool,
            )
            if (idx >= 0) items[idx] = item else items += item
            graph.copy(timelines = graph.timelines + (tool.sessionId to items))
        }
        updateSession(tool.sessionId) { it.copy(lastToolName = tool.name) }
    }

    fun markNeedsYou(sessionId: String, permission: PendingPermission) {
        mutate { graph ->
            val sessions = graph.sessions.map {
                if (it.id == sessionId) {
                    it.copy(
                        column = SessionColumn.NeedsYou,
                        pendingPermissionId = permission.id,
                        updatedAt = System.currentTimeMillis(),
                    )
                } else it
            }
            graph.copy(
                sessions = sessions,
                pending = graph.pending + (sessionId to permission),
            )
        }
    }

    fun clearPending(sessionId: String, backToWorking: Boolean = true) {
        mutate { graph ->
            val sessions = graph.sessions.map {
                if (it.id != sessionId) it
                else it.copy(
                    column = if (backToWorking) SessionColumn.Working else it.column,
                    pendingPermissionId = null,
                    updatedAt = System.currentTimeMillis(),
                )
            }
            graph.copy(sessions = sessions, pending = graph.pending - sessionId)
        }
    }

    fun markDone(sessionId: String, chip: DoneChip) {
        mutate { graph ->
            val sessions = graph.sessions.map {
                if (it.id != sessionId) it
                else it.copy(
                    column = SessionColumn.Done,
                    doneChip = chip,
                    pendingPermissionId = null,
                    updatedAt = System.currentTimeMillis(),
                )
            }
            graph.copy(sessions = sessions, pending = graph.pending - sessionId)
        }
    }

    fun incrementIteration(sessionId: String) {
        updateSession(sessionId) { it.copy(iterationsUsed = it.iterationsUsed + 1) }
    }

    private fun touch(sessionId: String) {
        updateSession(sessionId) { it }
    }

    private fun appendItem(item: TimelineItem) {
        mutate { graph ->
            val items = graph.timelines[item.sessionId].orEmpty() + item
            graph.copy(timelines = graph.timelines + (item.sessionId to items))
        }
        touch(item.sessionId)
    }

    private fun mutate(block: (SessionGraph) -> SessionGraph) {
        _graph.update { block(it) }
        persist(_graph.value)
    }

    private fun persist(graph: SessionGraph) {
        prefs.edit().putString(KEY_GRAPH, encode(graph)).apply()
    }

    private fun loadOrSeed(): SessionGraph {
        val raw = prefs.getString(KEY_GRAPH, null)
        // M2: fresh install starts empty so SCR-ONBOARD → empty Board is honest.
        // Prior M1 graphs (including demo seeds) still decode from prefs.
        if (raw.isNullOrBlank()) return SessionGraph()
        return runCatching { decode(raw) }.getOrElse { SessionGraph() }
    }

    // Demo seed() removed in M2 — fresh installs start empty.

    private fun encode(graph: SessionGraph): String {
        val root = JSONObject()
        val sessions = JSONArray()
        graph.sessions.forEach { s ->
            sessions.put(
                JSONObject()
                    .put("id", s.id)
                    .put("title", s.title)
                    .put("prompt", s.prompt)
                    .put("skillId", s.skillId ?: JSONObject.NULL)
                    .put("modelId", s.modelId)
                    .put("providerKind", s.providerKind.name)
                    .put("column", s.column.name)
                    .put("doneChip", s.doneChip.name)
                    .put("iterationBudget", s.iterationBudget)
                    .put("wallClockMs", s.wallClockMs)
                    .put("iterationsUsed", s.iterationsUsed)
                    .put("startedAt", s.startedAt)
                    .put("updatedAt", s.updatedAt)
                    .put("lastToolName", s.lastToolName ?: JSONObject.NULL)
                    .put("pendingPermissionId", s.pendingPermissionId ?: JSONObject.NULL)
                    .put("isDemoSeed", s.isDemoSeed),
            )
        }
        val timelines = JSONObject()
        graph.timelines.forEach { (sid, items) ->
            val arr = JSONArray()
            items.forEach { item ->
                val o = JSONObject()
                    .put("kind", item.javaClass.simpleName)
                    .put("id", item.id)
                    .put("sessionId", item.sessionId)
                    .put("at", item.at)
                when (item) {
                    is TimelineItem.Stream -> o.put("text", item.text)
                    is TimelineItem.System -> o.put("text", item.text)
                    is TimelineItem.Tool -> {
                        val t = item.tool
                        o.put(
                            "tool",
                            JSONObject()
                                .put("id", t.id)
                                .put("sessionId", t.sessionId)
                                .put("name", t.name)
                                .put("target", t.target)
                                .put("argsSummary", t.argsSummary)
                                .put("risk", t.risk.name)
                                .put("status", t.status.name)
                                .put("resultExcerpt", t.resultExcerpt)
                                .put("durationMs", t.durationMs ?: JSONObject.NULL),
                        )
                    }
                }
                arr.put(o)
            }
            timelines.put(sid, arr)
        }
        val pending = JSONObject()
        graph.pending.forEach { (sid, p) ->
            pending.put(
                sid,
                JSONObject()
                    .put("id", p.id)
                    .put("sessionId", p.sessionId)
                    .put("toolId", p.toolId)
                    .put("name", p.toolName)
                    .put("risk", p.risk.name)
                    .put("summary", p.summary)
                    .put("target", p.target),
            )
        }
        root.put("sessions", sessions)
        root.put("timelines", timelines)
        root.put("pending", pending)
        return root.toString()
    }

    private fun decode(raw: String): SessionGraph {
        val root = JSONObject(raw)
        val sessions = mutableListOf<AgentSession>()
        val sArr = root.optJSONArray("sessions") ?: JSONArray()
        for (i in 0 until sArr.length()) {
            val o = sArr.getJSONObject(i)
            sessions += AgentSession(
                id = o.getString("id"),
                title = o.getString("title"),
                prompt = o.optString("prompt"),
                skillId = o.optString("skillId").ifBlank { null },
                modelId = o.optString("modelId"),
                providerKind = runCatching { ProviderKind.valueOf(o.getString("providerKind")) }
                    .getOrDefault(ProviderKind.Demo),
                column = SessionColumn.valueOf(o.getString("column")),
                doneChip = runCatching { DoneChip.valueOf(o.optString("doneChip", "None")) }
                    .getOrDefault(DoneChip.None),
                iterationBudget = o.optInt("iterationBudget", 8),
                wallClockMs = o.optLong("wallClockMs", 300_000),
                iterationsUsed = o.optInt("iterationsUsed", 0),
                startedAt = o.optLong("startedAt"),
                updatedAt = o.optLong("updatedAt"),
                lastToolName = o.optString("lastToolName").ifBlank { null },
                pendingPermissionId = o.optString("pendingPermissionId").ifBlank { null },
                isDemoSeed = o.optBoolean("isDemoSeed"),
            )
        }
        val timelines = mutableMapOf<String, List<TimelineItem>>()
        val tObj = root.optJSONObject("timelines") ?: JSONObject()
        tObj.keys().forEach { sid ->
            val arr = tObj.getJSONArray(sid)
            val items = mutableListOf<TimelineItem>()
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                items += when (o.getString("kind")) {
                    "Stream" -> TimelineItem.Stream(
                        o.getString("id"), o.getString("sessionId"), o.getLong("at"), o.optString("text"),
                    )
                    "System" -> TimelineItem.System(
                        o.getString("id"), o.getString("sessionId"), o.getLong("at"), o.optString("text"),
                    )
                    else -> {
                        val t = o.getJSONObject("tool")
                        val tool = ToolCall(
                            id = t.getString("id"),
                            sessionId = t.getString("sessionId"),
                            name = t.getString("name"),
                            target = t.optString("target"),
                            argsSummary = t.optString("argsSummary"),
                            risk = runCatching { ToolRisk.valueOf(t.getString("risk")) }
                                .getOrDefault(ToolRisk.Write),
                            status = runCatching { ToolStatus.valueOf(t.getString("status")) }
                                .getOrDefault(ToolStatus.Pending),
                            resultExcerpt = t.optString("resultExcerpt"),
                            durationMs = if (t.isNull("durationMs")) null else t.optLong("durationMs"),
                        )
                        TimelineItem.Tool(o.getString("id"), o.getString("sessionId"), o.getLong("at"), tool)
                    }
                }
            }
            timelines[sid] = items
        }
        val pending = mutableMapOf<String, PendingPermission>()
        val pObj = root.optJSONObject("pending") ?: JSONObject()
        pObj.keys().forEach { sid ->
            val o = pObj.getJSONObject(sid)
            pending[sid] = PendingPermission(
                id = o.getString("id"),
                sessionId = o.getString("sessionId"),
                toolId = o.getString("toolId"),
                toolName = o.getString("name"),
                risk = runCatching { ToolRisk.valueOf(o.getString("risk")) }.getOrDefault(ToolRisk.Write),
                summary = o.optString("summary"),
                target = o.optString("target"),
            )
        }
        return SessionGraph(sessions, timelines, pending)
    }

    companion object {
        private const val PREFS = "tah_sessions"
        private const val KEY_GRAPH = "graph"
        fun newId(): String = UUID.randomUUID().toString()
    }
}
