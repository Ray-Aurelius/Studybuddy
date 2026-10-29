package com.rayaurelius.sophinity.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LinksTest {
    @Test fun addsHttpsWhenMissing() {
        assertEquals("https://youtube.com/watch?v=abc", Links.normalize("  youtube.com/watch?v=abc "))
        assertEquals("http://example.com", Links.normalize("http://example.com"))
        assertEquals("", Links.normalize("   "))
    }

    @Test fun shortSiteName() {
        assertEquals("example.com", Links.host("https://www.example.com/talks/42?x=1"))
        assertEquals("audible.com", Links.host("https://audible.com"))
        assertEquals("zoom.us", Links.host("https://user@zoom.us:443/j/123"))
    }

    @Test fun onlyWebLinksOpen() {
        assertTrue(Links.isOpenable("https://example.com/x"))
        assertFalse(Links.isOpenable("https://not a link"))
        assertFalse(Links.isOpenable("https://localhost"))
        assertFalse(Links.isOpenable("javascript:alert(1)"))
        assertFalse(Links.isOpenable(""))
    }
}
