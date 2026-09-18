package app.tah.shell.runtime

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ClipboardAccessTest {
    @Test
    fun fakeClipboardRoundTrip() {
        val clip = FakeClipboard("seed")
        assertEquals("seed", clip.read())
        assertTrue(clip.write("board-text"))
        assertEquals("board-text", clip.read())
    }
}
