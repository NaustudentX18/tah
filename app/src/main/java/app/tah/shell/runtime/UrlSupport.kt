package app.tah.shell.runtime

import java.net.URI

object UrlSupport {
    private val urlRegex = Regex("""https?://[^\s)>\]]+""", RegexOption.IGNORE_CASE)

    fun firstHttpUrl(text: String): String? {
        val raw = urlRegex.find(text)?.value?.trimEnd('.', ',', ';') ?: return null
        return if (isAllowed(raw)) raw else null
    }

    fun isAllowed(url: String): Boolean {
        return try {
            val uri = URI(url)
            val scheme = uri.scheme?.lowercase()
            scheme == "http" || scheme == "https"
        } catch (_: Throwable) {
            false
        }
    }
}
