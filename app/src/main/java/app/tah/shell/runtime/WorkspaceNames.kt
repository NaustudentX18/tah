package app.tah.shell.runtime

/**
 * Safe names for the app-private workspace. No path traversal, no hidden roots.
 */
object WorkspaceNames {
    private val allowed = Regex("^[A-Za-z0-9._-]{1,64}$")

    fun sanitize(raw: String, fallback: String = "notes.md"): String {
        val base = raw.substringAfterLast('/').substringAfterLast('\\').trim()
        val candidate = if (allowed.matches(base)) base else fallback
        return if (candidate == "." || candidate == "..") fallback else candidate
    }

    fun filenameFromPrompt(prompt: String, fallback: String = "notes.md"): String {
        val match = Regex("""([\w.\-]+\.(md|txt|json|csv|log))""", RegexOption.IGNORE_CASE)
            .find(prompt)
        return sanitize(match?.groupValues?.get(1) ?: fallback, fallback)
    }
}
