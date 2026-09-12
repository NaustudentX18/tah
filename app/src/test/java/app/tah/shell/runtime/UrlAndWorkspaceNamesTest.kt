package app.tah.shell.runtime

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class UrlAndWorkspaceNamesTest {
    @Test
    fun extractsFirstHttpUrl() {
        val url = UrlSupport.firstHttpUrl("see https://example.com/a and http://other.test")
        assertEquals("https://example.com/a", url)
        assertTrue(UrlSupport.isAllowed("https://example.com"))
        assertFalse(UrlSupport.isAllowed("file:///etc/passwd"))
        assertFalse(UrlSupport.isAllowed("javascript:alert(1)"))
    }

    @Test
    fun sanitizesWorkspaceNames() {
        assertEquals("notes.md", WorkspaceNames.sanitize("../etc/passwd"))
        assertEquals("ok-file.txt", WorkspaceNames.sanitize("ok-file.txt"))
        assertEquals("plan.md", WorkspaceNames.filenameFromPrompt("write plan.md please"))
    }
}
