package app.tah.shell.data

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import org.json.JSONObject

/**
 * Markdown skill packs the agent loop can read.
 * Bundled packs ship with the APK; users can paste-import more markdown.
 */
class SkillStore(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
    private val _packs = MutableStateFlow(load())
    val packs: StateFlow<List<SkillPack>> = _packs.asStateFlow()

    fun byId(id: String?): SkillPack =
        _packs.value.firstOrNull { it.id == id } ?: _packs.value.firstOrNull { it.enabled }
            ?: bundled().first()

    fun enabledSnapshot(): String =
        _packs.value.filter { it.enabled }.joinToString("\n\n") { "## ${it.title}\n${it.body}" }

    fun setEnabled(id: String, enabled: Boolean) {
        _packs.value = _packs.value.map { if (it.id == id) it.copy(enabled = enabled) else it }
        persist()
    }

    /**
     * Import a markdown skill pack from pasted text.
     * First `# heading` becomes the title; body is the full markdown.
     * Returns error message or null on success.
     */
    fun importMarkdown(raw: String): String? {
        val text = raw.trim()
        if (text.isBlank()) return "Paste some markdown first."
        if (text.length > 48_000) return "Pack too large (48 KB max for MVP)."
        val title = text.lineSequence()
            .map { it.trim() }
            .firstOrNull { it.startsWith("#") }
            ?.trimStart('#')
            ?.trim()
            ?.takeIf { it.isNotBlank() }
            ?: "Imported pack"
        val id = "import-${System.currentTimeMillis()}"
        val pack = SkillPack(id = id, title = title, body = text, enabled = true, bundled = false)
        _packs.value = listOf(pack) + _packs.value
        persist()
        return null
    }

    fun remove(id: String): String? {
        val pack = _packs.value.firstOrNull { it.id == id } ?: return "Pack not found."
        if (pack.bundled) return "Bundled packs stay on the deck — disable instead of delete."
        _packs.value = _packs.value.filterNot { it.id == id }
        persist()
        return null
    }

    private fun load(): List<SkillPack> {
        val raw = prefs.getString(KEY, null) ?: return bundled()
        return runCatching {
            val arr = JSONArray(raw)
            buildList {
                for (i in 0 until arr.length()) {
                    val o = arr.getJSONObject(i)
                    add(
                        SkillPack(
                            id = o.getString("id"),
                            title = o.getString("title"),
                            body = o.getString("body"),
                            enabled = o.optBoolean("enabled", true),
                            bundled = o.optBoolean("bundled", false),
                        ),
                    )
                }
            }.ifEmpty { bundled() }
        }.getOrElse { bundled() }
    }

    private fun persist() {
        val arr = JSONArray()
        _packs.value.forEach { p ->
            arr.put(
                JSONObject()
                    .put("id", p.id)
                    .put("title", p.title)
                    .put("body", p.body)
                    .put("enabled", p.enabled)
                    .put("bundled", p.bundled),
            )
        }
        prefs.edit().putString(KEY, arr.toString()).apply()
    }

    companion object {
        private const val PREFS = "tah_skills"
        private const val KEY = "packs"

        fun bundled(): List<SkillPack> = listOf(
            SkillPack(
                id = "general",
                title = "General steer",
                body = """
                    # General
                    Be concise. Surface tool intent before acting. Prefer cards over chatter.
                    Never claim a tool ran unless a tool card exists.
                """.trimIndent(),
                enabled = true,
                bundled = true,
            ),
            SkillPack(
                id = "research",
                title = "Research sweep",
                body = """
                    # Research
                    Cite sources in plain language. Stop cleanly when the budget is hit.
                    Prefer read/network tools over exec.
                """.trimIndent(),
                enabled = true,
                bundled = true,
            ),
            SkillPack(
                id = "coding",
                title = "Coding patch",
                body = """
                    # Coding
                    Propose the smallest write. Exec stays gated. Show paths on tool cards.
                """.trimIndent(),
                enabled = true,
                bundled = true,
            ),
        )
    }
}

/** Back-compat alias used by Dispatch skill picker. */
object SkillCatalog {
    fun packs(): List<SkillPack> = SkillStore.bundled()
    fun byId(id: String?): SkillPack = SkillStore.bundled().firstOrNull { it.id == id }
        ?: SkillStore.bundled().first()
}
