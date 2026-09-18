package app.tah.shell.runtime

import app.tah.shell.data.ToolRisk
import org.json.JSONArray
import org.json.JSONObject

/** Tool JSON schemas + name mapping for OpenAI-compatible providers. */
object OpenAiToolSchemas {
    fun mapToolCall(fnName: String, argsRaw: String): OpenAiCompatClient.StreamEvent.ToolCallReady? {
        val argsJson = runCatching { JSONObject(argsRaw) }.getOrDefault(JSONObject())
        return when (fnName) {
            "shell_exec" -> {
                val cmd = argsJson.optString("command", argsRaw).trim()
                OpenAiCompatClient.StreamEvent.ToolCallReady(
                    name = "shell.exec",
                    target = cmd,
                    argsSummary = cmd,
                    risk = ToolRisk.Exec,
                )
            }
            "fs_read" -> {
                val path = argsJson.optString("path", argsRaw).trim()
                OpenAiCompatClient.StreamEvent.ToolCallReady(
                    name = "fs.read",
                    target = path,
                    argsSummary = "read $path",
                    risk = ToolRisk.Read,
                )
            }
            "fs_write" -> {
                val path = argsJson.optString("path").trim()
                val content = argsJson.optString("content")
                OpenAiCompatClient.StreamEvent.ToolCallReady(
                    name = "fs.write",
                    target = path,
                    argsSummary = content,
                    risk = ToolRisk.Write,
                )
            }
            "fs_list" -> {
                val path = argsJson.optString("path", "workspace").trim()
                OpenAiCompatClient.StreamEvent.ToolCallReady(
                    name = "fs.list",
                    target = path,
                    argsSummary = "list $path",
                    risk = ToolRisk.Read,
                )
            }
            "web_fetch" -> {
                val url = argsJson.optString("url", argsRaw).trim()
                OpenAiCompatClient.StreamEvent.ToolCallReady(
                    name = "web.fetch",
                    target = url,
                    argsSummary = "GET $url",
                    risk = ToolRisk.Network,
                )
            }
            "memory_write" -> {
                val title = argsJson.optString("title", "Note")
                val body = argsJson.optString("body", argsRaw)
                OpenAiCompatClient.StreamEvent.ToolCallReady(
                    name = "memory.write",
                    target = title,
                    argsSummary = body,
                    risk = ToolRisk.Write,
                )
            }
            "clipboard_read" -> {
                OpenAiCompatClient.StreamEvent.ToolCallReady(
                    name = "clipboard.read",
                    target = "device clipboard",
                    argsSummary = "read clipboard after Ask",
                    risk = ToolRisk.Read,
                )
            }
            "clipboard_write" -> {
                val text = argsJson.optString("text", argsRaw)
                OpenAiCompatClient.StreamEvent.ToolCallReady(
                    name = "clipboard.write",
                    target = "device clipboard",
                    argsSummary = text,
                    risk = ToolRisk.Write,
                )
            }
            else -> null
        }
    }

    fun createToolsJson(): JSONArray {
        return JSONArray().apply {
            put(
                JSONObject().put("type", "function").put(
                    "function",
                    JSONObject()
                        .put("name", "shell_exec")
                        .put("description", "In-process allowlist only: date, echo, ls of app workspace. Never /bin/sh.")
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
                        .put("description", "Read a file under the app-private workspace (filesDir/workspace)")
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
                        .put("description", "Write a file under the app-private workspace only")
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
                        .put("description", "List app-private workspace files")
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
                        .put("name", "clipboard_read")
                        .put("description", "Read device clipboard text after Ask permission card")
                        .put(
                            "parameters",
                            JSONObject().put("type", "object")
                                .put("properties", JSONObject()),
                        ),
                ),
            )
            put(
                JSONObject().put("type", "function").put(
                    "function",
                    JSONObject()
                        .put("name", "clipboard_write")
                        .put("description", "Write text to device clipboard after Ask permission card")
                        .put(
                            "parameters",
                            JSONObject().put("type", "object")
                                .put(
                                    "properties",
                                    JSONObject()
                                        .put("text", JSONObject().put("type", "string").put("description", "Text to place on clipboard")),
                                )
                                .put("required", JSONArray().put("text")),
                        ),
                ),
            )
        }
    }
}
