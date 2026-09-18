package app.tah.shell.runtime

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context

/**
 * Clipboard bridge for sandboxed on-device tools.
 * Always gated by Ask-default permission cards in the agent loop.
 */
interface ClipboardAccess {
    fun read(): String
    fun write(text: String): Boolean
}

class SystemClipboard(context: Context) : ClipboardAccess {
    private val appContext = context.applicationContext

    override fun read(): String {
        val cm = appContext.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
            ?: return ""
        val clip = cm.primaryClip ?: return ""
        if (clip.itemCount <= 0) return ""
        return clip.getItemAt(0).coerceToText(appContext).toString().take(MAX_CHARS)
    }

    override fun write(text: String): Boolean {
        val cm = appContext.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
            ?: return false
        return try {
            cm.setPrimaryClip(ClipData.newPlainText("TAH", text.take(MAX_CHARS)))
            true
        } catch (_: Throwable) {
            false
        }
    }

    companion object {
        const val MAX_CHARS = 16_384
    }
}

/** JVM / unit-test double. */
class FakeClipboard(
    initial: String = "",
) : ClipboardAccess {
    var value: String = initial
    override fun read(): String = value.take(SystemClipboard.MAX_CHARS)
    override fun write(text: String): Boolean {
        value = text.take(SystemClipboard.MAX_CHARS)
        return true
    }
}
