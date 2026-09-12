package app.tah.shell.runtime

import app.tah.shell.data.ChatMessage
import app.tah.shell.data.ProviderSnapshot
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import okhttp3.Call
import okhttp3.Callback
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.TimeUnit

import app.tah.shell.data.ToolRisk
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.filterIsInstance

class OpenAiCompatClient(
    private val http: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(120, TimeUnit.SECONDS)
        .writeTimeout(20, TimeUnit.SECONDS)
        .build(),
) {
    sealed class StreamEvent {
        data class Token(val text: String) : StreamEvent()
        data class ToolCallReady(
            val name: String,
            val target: String,
            val argsSummary: String,
            val risk: ToolRisk,
        ) : StreamEvent()
    }

    fun streamChat(
        snapshot: ProviderSnapshot,
        apiKey: String,
        messages: List<ChatMessage>,
    ): Flow<String> = streamChatWithEvents(snapshot, apiKey, messages)
        .filterIsInstance<StreamEvent.Token>()
        .map { it.text }

    fun streamChatWithEvents(
        snapshot: ProviderSnapshot,
        apiKey: String,
        messages: List<ChatMessage>,
    ): Flow<StreamEvent> = callbackFlow {
        val url = snapshot.baseUrl.trimEnd('/') + "/chat/completions"
        val payload = JSONObject()
            .put("model", snapshot.modelId)
            .put("stream", true)
            .put("tools", createToolsJson())
            .put(
                "messages",
                JSONArray().apply {
                    messages.forEach { msg ->
                        put(JSONObject().put("role", msg.role).put("content", msg.content))
                    }
                },
            )
        val builder = Request.Builder()
            .url(url)
            .post(payload.toString().toRequestBody(JSON))
            .header("Accept", "text/event-stream")
        if (apiKey.isNotBlank()) {
            builder.header("Authorization", "Bearer $apiKey")
        }
        val call = http.newCall(builder.build())
        call.enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                close(e)
            }

            override fun onResponse(call: Call, response: Response) {
                if (!response.isSuccessful) {
                    close(IOException("HTTP ${response.code}: ${response.body?.string()?.take(240)}"))
                    return
                }
                val inflightTools = mutableMapOf<Int, InflightTool>()
                try {
                    response.body?.charStream()?.buffered()?.use { reader ->
                        while (true) {
                            val line = reader.readLine() ?: break
                            if (!line.startsWith("data:")) continue
                            val data = line.removePrefix("data:").trim()
                            if (data.isEmpty()) continue
                            if (data == "[DONE]") {
                                emitCompletedTools(inflightTools) { trySend(it) }
                                break
                            }

                            // 1. Text token
                            val token = parseDeltaContent(data)
                            if (token != null) {
                                trySend(StreamEvent.Token(token))
                            }

                            // 2. Tool call deltas
                            accumulateToolDeltas(data, inflightTools)
                        }
                    }
                    emitCompletedTools(inflightTools) { trySend(it) }
                    close()
                } catch (t: Throwable) {
                    close(t)
                }
            }
        })
        awaitClose { call.cancel() }
    }

    private data class InflightTool(
        var id: String = "",
        var name: String = "",
        val args: StringBuilder = StringBuilder(),
    )

    private fun accumulateToolDeltas(data: String, map: MutableMap<Int, InflightTool>) {
        try {
            val json = JSONObject(data)
            val choices = json.optJSONArray("choices") ?: return
            if (choices.length() == 0) return
            val delta = choices.getJSONObject(0).optJSONObject("delta") ?: return
            val toolCalls = delta.optJSONArray("tool_calls") ?: return
            for (i in 0 until toolCalls.length()) {
                val tc = toolCalls.getJSONObject(i)
                val idx = tc.optInt("index", i)
                val entry = map.getOrPut(idx) { InflightTool() }
                tc.optString("id").takeIf { it.isNotBlank() }?.let { entry.id = it }
                tc.optJSONObject("function")?.let { fn ->
                    fn.optString("name").takeIf { it.isNotBlank() }?.let { entry.name = it }
                    fn.optString("arguments").takeIf { it.isNotEmpty() }?.let { entry.args.append(it) }
                }
            }
        } catch (_: Throwable) {}
    }

    private fun emitCompletedTools(
        map: MutableMap<Int, InflightTool>,
        emitter: (StreamEvent.ToolCallReady) -> Unit,
    ) {
        if (map.isEmpty()) return
        val items = map.values.toList()
        map.clear()
        for (item in items) {
            if (item.name.isBlank()) continue
            val event = mapToolCall(item.name, item.args.toString())
            if (event != null) {
                emitter(event)
            }
        }
    }

    fun mapToolCall(fnName: String, argsRaw: String): StreamEvent.ToolCallReady? {
        val argsJson = runCatching { JSONObject(argsRaw) }.getOrDefault(JSONObject())
        return when (fnName) {
            "shell_exec" -> {
                val cmd = argsJson.optString("command", argsRaw).trim()
                StreamEvent.ToolCallReady(
                    name = "shell.exec",
                    target = cmd,
                    argsSummary = cmd,
                    risk = ToolRisk.Exec,
                )
            }
            "fs_read" -> {
                val path = argsJson.optString("path", argsRaw).trim()
                StreamEvent.ToolCallReady(
                    name = "fs.read",
                    target = path,
                    argsSummary = "read $path",
                    risk = ToolRisk.Read,
                )
            }
            "fs_write" -> {
                val path = argsJson.optString("path").trim()
                val content = argsJson.optString("content")
                StreamEvent.ToolCallReady(
                    name = "fs.write",
                    target = path,
                    argsSummary = content,
                    risk = ToolRisk.Write,
                )
            }
            "fs_list" -> {
                val path = argsJson.optString("path", "workspace").trim()
                StreamEvent.ToolCallReady(
                    name = "fs.list",
                    target = path,
                    argsSummary = "list $path",
                    risk = ToolRisk.Read,
                )
            }
            "web_fetch" -> {
                val url = argsJson.optString("url", argsRaw).trim()
                StreamEvent.ToolCallReady(
                    name = "web.fetch",
                    target = url,
                    argsSummary = "GET $url",
                    risk = ToolRisk.Network,
                )
            }
            "memory_write" -> {
                val title = argsJson.optString("title", "Note")
                val body = argsJson.optString("body", argsRaw)
                StreamEvent.ToolCallReady(
                    name = "memory.write",
                    target = title,
                    argsSummary = body,
                    risk = ToolRisk.Write,
                )
            }
            "agent_spawn" -> {
                val role = argsJson.optString("role", "Planner")
                val prompt = argsJson.optString("prompt", argsRaw)
                StreamEvent.ToolCallReady(
                    name = "agent.spawn",
                    target = role,
                    argsSummary = prompt,
                    risk = ToolRisk.Write,
                )
            }
            else -> null
        }
    }

    fun probe(snapshot: ProviderSnapshot, apiKey: String): ProbeResult {
        val url = snapshot.baseUrl.trimEnd('/') + "/models"
        val builder = Request.Builder().url(url).get()
        if (apiKey.isNotBlank()) builder.header("Authorization", "Bearer $apiKey")
        return try {
            http.newCall(builder.build()).execute().use { res ->
                val body = res.body?.string().orEmpty()
                if (!res.isSuccessful) {
                    ProbeResult(false, "HTTP ${res.code}", emptyList())
                } else {
                    val ids = parseModelIds(body)
                    ProbeResult(true, "Reached ${snapshot.baseUrl}", ids)
                }
            }
        } catch (t: Throwable) {
            ProbeResult(false, t.message ?: "probe failed", emptyList())
        }
    }

    private fun parseDeltaContent(data: String): String? {
        return try {
            val json = JSONObject(data)
            val choices = json.optJSONArray("choices") ?: return null
            if (choices.length() == 0) return null
            choices.getJSONObject(0).optJSONObject("delta")?.optString("content")
                ?.takeIf { it.isNotEmpty() }
        } catch (_: Throwable) {
            null
        }
    }

    private fun parseModelIds(body: String): List<String> {
        return try {
            val data = JSONObject(body).optJSONArray("data") ?: return emptyList()
            buildList {
                for (i in 0 until data.length()) {
                    val id = data.getJSONObject(i).optString("id")
                    if (id.isNotBlank()) add(id)
                }
            }
        } catch (_: Throwable) {
            emptyList()
        }
    }

    fun createToolsJson(): JSONArray {
        return JSONArray().apply {
            put(
                JSONObject().put("type", "function").put(
                    "function",
                    JSONObject()
                        .put("name", "shell_exec")
                        .put("description", "Execute a shell command on device (/system/bin/sh or process)")
                        .put(
                            "parameters",
                            JSONObject().put("type", "object")
                                .put("properties", JSONObject().put("command", JSONObject().put("type", "string").put("description", "Command string")))
                                .put("required", JSONArray().put("command")),
                        ),
                ),
            )
            put(
                JSONObject().put("type", "function").put(
                    "function",
                    JSONObject()
                        .put("name", "fs_read")
                        .put("description", "Read workspace or device file")
                        .put(
                            "parameters",
                            JSONObject().put("type", "object")
                                .put("properties", JSONObject().put("path", JSONObject().put("type", "string").put("description", "Path to file")))
                                .put("required", JSONArray().put("path")),
                        ),
                ),
            )
            put(
                JSONObject().put("type", "function").put(
                    "function",
                    JSONObject()
                        .put("name", "fs_write")
                        .put("description", "Write workspace or device file")
                        .put(
                            "parameters",
                            JSONObject().put("type", "object")
                                .put(
                                    "properties",
                                    JSONObject()
                                        .put("path", JSONObject().put("type", "string").put("description", "File path"))
                                        .put("content", JSONObject().put("type", "string").put("description", "File content")),
                                )
                                .put("required", JSONArray().put("path").put("content")),
                        ),
                ),
            )
            put(
                JSONObject().put("type", "function").put(
                    "function",
                    JSONObject()
                        .put("name", "fs_list")
                        .put("description", "List directory entries")
                        .put(
                            "parameters",
                            JSONObject().put("type", "object")
                                .put("properties", JSONObject().put("path", JSONObject().put("type", "string").put("description", "Directory path"))),
                        ),
                ),
            )
            put(
                JSONObject().put("type", "function").put(
                    "function",
                    JSONObject()
                        .put("name", "web_fetch")
                        .put("description", "HTTP GET fetch URL content")
                        .put(
                            "parameters",
                            JSONObject().put("type", "object")
                                .put("properties", JSONObject().put("url", JSONObject().put("type", "string").put("description", "HTTP URL")))
                                .put("required", JSONArray().put("url")),
                        ),
                ),
            )
            put(
                JSONObject().put("type", "function").put(
                    "function",
                    JSONObject()
                        .put("name", "memory_write")
                        .put("description", "Persist note to Skills & Memory")
                        .put(
                            "parameters",
                            JSONObject().put("type", "object")
                                .put(
                                    "properties",
                                    JSONObject()
                                        .put("title", JSONObject().put("type", "string").put("description", "Note title"))
                                        .put("body", JSONObject().put("type", "string").put("description", "Note body")),
                                )
                                .put("required", JSONArray().put("title").put("body")),
                        ),
                ),
            )
            put(
                JSONObject().put("type", "function").put(
                    "function",
                    JSONObject()
                        .put("name", "agent_spawn")
                        .put("description", "Spawn subagent in swarm (Planner, Researcher, Coder, Verifier)")
                        .put(
                            "parameters",
                            JSONObject().put("type", "object")
                                .put(
                                    "properties",
                                    JSONObject()
                                        .put("role", JSONObject().put("type", "string").put("description", "Subagent role"))
                                        .put("prompt", JSONObject().put("type", "string").put("description", "Task instructions")),
                                )
                                .put("required", JSONArray().put("role").put("prompt")),
                        ),
                ),
            )
        }
    }

    data class ProbeResult(val ok: Boolean, val detail: String, val models: List<String>)

    companion object {
        private val JSON = "application/json; charset=utf-8".toMediaType()
    }
}
