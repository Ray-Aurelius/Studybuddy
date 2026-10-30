package com.rayaurelius.sophinity.domain

import com.rayaurelius.sophinity.data.db.ItemType
import com.rayaurelius.sophinity.data.db.LearningItem
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Test

class MediaPositionTest {
    @Test fun timestampsInShareLinks() {
        assertEquals(916L, Links.timestampSec("https://youtu.be/abc?t=916"))
        assertEquals(916L, Links.timestampSec("https://www.youtube.com/watch?v=abc&t=916s"))
        assertEquals(916L, Links.timestampSec("https://www.youtube.com/watch?v=abc&t=15m16s"))
        assertEquals(3723L, Links.timestampSec("https://vimeo.com/123#t=1h2m3s"))
        assertEquals(916L, Links.timestampSec("https://open.spotify.com/episode/x?si=y&t=916"))
        assertEquals(916L, Links.timestampSec("https://example.org/v?start=15:16"))
        assertNull(Links.timestampSec("https://www.youtube.com/watch?v=abc"))
        assertNull(Links.timestampSec("https://example.org/?tab=2"))
        assertNull(Links.timestampSec("https://youtu.be/abc?t=0"))
        assertEquals(3723L, Links.parseTime("1:02:03"))
        assertNull(Links.parseTime("soon"))
    }

    @Test fun timestampsAreDroppedFromStoredLinks() {
        assertEquals("https://www.youtube.com/watch?v=abc", Links.withoutTimestamp("https://www.youtube.com/watch?v=abc&t=916s"))
        assertEquals("https://youtu.be/abc", Links.withoutTimestamp("https://youtu.be/abc?t=916"))
        assertEquals("https://vimeo.com/123", Links.withoutTimestamp("https://vimeo.com/123#t=15m16s"))
        assertEquals("https://example.org/a?x=1#top", Links.withoutTimestamp("https://example.org/a?x=1&start=5#top"))
    }

    @Test fun sharedTextAndMatching() {
        assertEquals("https://youtu.be/abc?t=5", Links.firstUrl("Watch this! https://youtu.be/abc?t=5."))
        assertNull(Links.firstUrl("no link here"))
        val key = Links.mediaKey("https://www.youtube.com/watch?v=abc123&list=x")
        assertEquals(key, Links.mediaKey("https://youtu.be/abc123?t=916"))
        assertEquals(key, Links.mediaKey("https://m.youtube.com/watch?feature=share&v=abc123"))
        assertEquals(key, Links.mediaKey("youtube.com/shorts/abc123"))
        assertNotEquals(key, Links.mediaKey("https://youtu.be/other"))
        assertEquals(Links.mediaKey("https://open.spotify.com/episode/42?si=aa"), Links.mediaKey("https://open.spotify.com/episode/42/?si=bb&t=60"))
        assertNotEquals(Links.mediaKey("https://podcasts.apple.com/us/podcast/x/id1?i=100"), Links.mediaKey("https://podcasts.apple.com/us/podcast/x/id1?i=200"))
    }

    @Test fun positionAdjustsOnlyTheEarlierPart() {
        val book = LearningItem(type = ItemType.AUDIOBOOK, title = "A", runtimeMin = 600, playbackSpeed = 1.5f)
        // 40 minutes logged at 1.5× is an hour of audio; to be at 2:15:16 the earlier part is 1:15:16.
        assertEquals(4516L, MediaTime.priorForPosition(book, 8116, loggedSec = 2400))
        assertEquals(8116L, MediaTime.progress(book.copy(priorSec = 4516), 2400)!!.completedSec)
        // Going back before what's logged can't make the earlier part negative.
        assertEquals(0L, MediaTime.priorForPosition(book, 600, loggedSec = 2400))
        // Time on the running session counts too.
        assertEquals(4516L - 900, MediaTime.priorForPosition(book, 8116, loggedSec = 2400, liveSec = 600))
    }
}
