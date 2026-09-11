package app.tah.shell.data

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import org.json.JSONObject

class MemoryStore(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences("tah_memory", Context.MODE_PRIVATE)
    private val _notes = MutableStateFlow(load())
    val notes: StateFlow<List<MemoryNote>> = _notes.asStateFlow()

    fun snapshotText(): String = _notes.value.joinToString("\n") { "- ${it.title}: ${it.body}" }

    fun upsert(note: MemoryNote) {
        val next = _notes.value.filterNot { it.id == note.id } + note
        _notes.value = next.sortedByDescending { it.updatedAt }
        persist()
    }

    private fun load(): List<MemoryNote> {
        val raw = prefs.getString("notes", null) ?: return listOf(
            MemoryNote(
                id = "mem-locks",
                title = "CoS locks",
                body = "Failed is a chip on Done. Reject ends the tool. Guide is independent. Allow edits ≠ exec.",
                updatedAt = System.currentTimeMillis(),
            ),
        )
        return runCatching {
            val arr = JSONArray(raw)
            buildList {
                for (i in 0 until arr.length()) {
                    val o = arr.getJSONObject(i)
                    add(
                        MemoryNote(
                            id = o.getString("id"),
                            title = o.getString("title"),
                            body = o.getString("body"),
                            updatedAt = o.optLong("updatedAt"),
                        ),
                    )
                }
            }
        }.getOrDefault(emptyList())
    }

    private fun persist() {
        val arr = JSONArray()
        _notes.value.forEach { n ->
            arr.put(
                JSONObject()
                    .put("id", n.id)
                    .put("title", n.title)
                    .put("body", n.body)
                    .put("updatedAt", n.updatedAt),
            )
        }
        prefs.edit().putString("notes", arr.toString()).apply()
    }
}
