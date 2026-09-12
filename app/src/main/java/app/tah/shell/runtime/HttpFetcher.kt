package app.tah.shell.runtime

import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit

class HttpFetcher(
    private val http: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(12, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .followRedirects(true)
        .followSslRedirects(true)
        .build(),
) {
    data class FetchResult(
        val ok: Boolean,
        val excerpt: String,
        val status: Int? = null,
    )

    fun get(url: String): FetchResult {
        if (!UrlSupport.isAllowed(url)) {
            return FetchResult(false, "Blocked URL. Only http(s) targets are fetched.")
        }
        val request = Request.Builder()
            .url(url)
            .header("User-Agent", "TAH/0.5 (Signal Deck; honest fetch)")
            .header("Accept", "text/*,application/json,application/xml")
            .get()
            .build()
        return try {
            http.newCall(request).execute().use { response ->
                val bytes = response.body?.bytes() ?: ByteArray(0)
                val clipped = bytes.copyOf(bytes.size.coerceAtMost(MAX_BYTES))
                val text = clipped.toString(Charsets.UTF_8).take(MAX_CHARS)
                val prefix = "HTTP ${response.code} ${url.take(80)} (${clipped.size} bytes)"
                if (!response.isSuccessful) {
                    FetchResult(false, "$prefix\n${text.take(400)}", response.code)
                } else {
                    FetchResult(true, "$prefix\n${text.take(1_200)}", response.code)
                }
            }
        } catch (t: Throwable) {
            FetchResult(false, "Fetch failed: ${t.message ?: t.javaClass.simpleName}")
        }
    }

    companion object {
        const val MAX_BYTES = 32_768
        const val MAX_CHARS = 4_000
    }
}
