package com.rayaurelius.sophinity.domain

import com.rayaurelius.sophinity.data.LinkFetcher
import com.rayaurelius.sophinity.data.db.ItemType
import com.rayaurelius.sophinity.data.db.LearningItem
import com.rayaurelius.sophinity.domain.LinkAutofill.Field
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class LinkDetailsTest {
    private val p = LinkDetailsParser

    @Test fun youtubePageAndOEmbed() {
        val url = "https://www.youtube.com/watch?v=abc123"
        val embed = p.fromOEmbed("""{"title":"How Memory Works &amp; Why","author_name":"Brain Lab","type":"video","provider_name":"YouTube"}""", url)
        assertEquals("How Memory Works & Why", embed.title)
        assertEquals("Brain Lab", embed.creator)
        assertEquals(ItemType.VIDEO, embed.type)
        val html = """<html><head><title>(3) How Memory Works &amp; Why - YouTube</title>
            <meta property="og:site_name" content="YouTube"></head><body>
            <div itemscope itemtype="http://schema.org/VideoObject"><meta itemprop="duration" content="PT1H2M30S"></div></body></html>"""
        val page = p.fromHtml(html, url)
        assertEquals("How Memory Works & Why", page.title)
        assertEquals(3750L, page.durationSec)
        assertEquals(ItemType.VIDEO, page.type)
    }

    @Test fun courseFromJsonLd() {
        val html = """<head><meta property="og:title" content="Learning How to Learn | Coursera">
            <script type="application/ld+json">{"@context":"https://schema.org","@graph":[
              {"@type":"BreadcrumbList","name":"Crumbs"},
              {"@type":"Course","name":"Learning How to Learn","provider":{"@type":"Organization","name":"Deep Teaching Solutions"},"timeRequired":"PT15H"}]}</script></head>"""
        val d = p.fromHtml(html, "https://www.coursera.org/learn/learning-how-to-learn")
        assertEquals("Learning How to Learn", d.title)
        assertEquals("Deep Teaching Solutions", d.creator)
        assertEquals(ItemType.COURSE, d.type)
        assertEquals(15 * 3600L, d.durationSec)
    }

    @Test fun bookWithAuthorsAndPages() {
        val html = """<script type="application/ld+json">
            {"@context":"http://schema.org","@type":"Book","name":"Make It Stick","numberOfPages":336,
             "author":[{"@type":"Person","name":"Peter C. Brown"},{"@type":"Person","name":"Henry L. Roediger III"}]}</script>
            <title>Make It Stick by Peter C. Brown | Goodreads</title>"""
        val d = p.fromHtml(html, "https://www.goodreads.com/book/show/18770267")
        assertEquals("Make It Stick", d.title)
        assertEquals("Peter C. Brown, Henry L. Roediger III", d.creator)
        assertEquals(ItemType.BOOK, d.type)
        assertEquals(336, d.pages)
    }

    @Test fun audiobookPrefersAudiobookTypeAndReadsLengthText() {
        val html = """<script type="application/ld+json">[{"@type":["Book","Audiobook"],"name":"Meditations","author":"Marcus Aurelius","readBy":[{"name":"Duncan Steen"}]}]</script>
            <li class="runtimeLabel">Length: 4 hrs and 25 mins</li>"""
        val d = p.fromHtml(html, "https://example-audio.org/pd/Meditations")
        assertEquals(ItemType.AUDIOBOOK, d.type)
        assertEquals("Marcus Aurelius", d.creator)
        assertEquals(4 * 3600L + 25 * 60, d.durationSec)
    }

    @Test fun eventsAreSeminarsAndOpenGraphFallsBack() {
        val ev = p.fromHtml("""<script type='application/ld+json'>{"@type":"EducationEvent","name":"Intro to Stoicism","organizer":{"name":"City Library"}}</script>""",
            "https://example.org/events/1")
        assertEquals(ItemType.SEMINAR, ev.type)
        assertEquals("City Library", ev.creator)
        val og = p.fromHtml("""<meta content="Why Spacing Works" property="og:title"/><meta property='og:type' content='article'><meta name="author" content="A. Writer">""",
            "https://blog.example.net/spacing")
        assertEquals("Why Spacing Works", og.title)
        assertEquals(ItemType.ARTICLE, og.type)
        assertEquals("A. Writer", og.creator)
    }

    @Test fun brokenOrEmptyPagesGiveNothing() {
        assertTrue(p.fromHtml("<html><body>hi</body></html>", "https://example.org").isEmpty)
        assertTrue(p.fromHtml("""<script type="application/ld+json">{not json</script>""", "https://example.org").isEmpty)
        assertTrue(p.fromOEmbed("<html>404</html>", "https://youtube.com/watch?v=x").title == null)
    }

    @Test fun helpers() {
        assertEquals(90L, p.isoDurationSec("PT1M30S"))
        assertEquals(86400L + 7200, p.isoDurationSec("P1DT2H"))
        assertNull(p.isoDurationSec("P"))
        assertNull(p.isoDurationSec("90 minutes"))
        assertEquals("Thinking, Fast and Slow", p.cleanTitle("Thinking, Fast and Slow - Wikipedia", null, "https://en.wikipedia.org/wiki/x"))
        assertEquals("Stoics: A Guide", p.cleanTitle("Stoics: A Guide", "Coursera", "https://coursera.org/x"))
        // A short host label must not eat a real subtitle (bbc.co.uk → "co").
        assertEquals("Bread - Cooking basics", p.cleanTitle("Bread - Cooking basics", null, "https://www.bbc.co.uk/food"))
        assertEquals("Café “Plato” — 5 > 4 é", p.clean("Caf&eacute; &ldquo;Plato&rdquo; &mdash;  5 &gt; 4 &#233;").replace("&eacute;", "é"))
        assertEquals(ItemType.PODCAST, p.typeFromUrl("https://open.spotify.com/show/123"))
        assertNull(p.typeFromUrl("https://open.spotify.com/track/123"))
        assertEquals(ItemType.COURSE, p.typeFromUrl("https://www.linkedin.com/learning/thing"))
        assertEquals(ItemType.SEMINAR, p.typeFromUrl("https://us02web.zoom.us/j/123"))
        assertNull(p.typeFromUrl("https://notyoutube.com/x"))
        assertTrue(p.oEmbedEndpoint("https://youtu.be/abc")!!.startsWith("https://www.youtube.com/oembed"))
        assertNull(p.oEmbedEndpoint("https://example.org"))
    }

    @Test fun autofillOnlyFillsEmptyFieldsAndUndoes() {
        val d = LinkDetails(title = "T", creator = "C", type = ItemType.VIDEO, durationSec = 3_629, pages = 200)
        val fresh = LearningItem(type = ItemType.BOOK, title = "")
        val (filled, fields) = LinkAutofill.apply(fresh, d, allowTypeChange = true)
        assertEquals(setOf(Field.TITLE, Field.CREATOR, Field.TYPE, Field.RUNTIME), fields)
        assertEquals(ItemType.VIDEO, filled.type)
        assertEquals(60, filled.runtimeMin) // 60m29s rounds to an hour
        assertNull(filled.totalUnits) // pages only make sense for books

        val typed = LearningItem(type = ItemType.BOOK, title = "Mine", creator = "")
        val (kept, f2) = LinkAutofill.apply(typed, d, allowTypeChange = false)
        assertEquals("Mine", kept.title)
        assertEquals(ItemType.BOOK, kept.type)
        assertEquals(200, kept.totalUnits)
        assertEquals(setOf(Field.CREATOR, Field.PAGES), f2)

        // Undo restores the filled fields but keeps edits made since (here, the link).
        val edited = filled.copy(url = "https://x.org")
        val undone = LinkAutofill.undo(edited, fresh, fields)
        assertEquals(fresh.copy(url = "https://x.org"), undone)
    }

    @Test fun fetcherCombinesOEmbedWithThePage() = runBlocking {
        val calls = mutableListOf<String>()
        val f = LinkFetcher { u ->
            calls += u
            if (u.contains("/oembed")) """{"title":"Talk","author_name":"Speaker"}"""
            else """<meta itemprop="duration" content="PT20M">"""
        }
        val d = f.lookup("youtube.com/watch?v=1")!!
        assertEquals("Talk", d.title)
        assertEquals("Speaker", d.creator)
        assertEquals(1200L, d.durationSec)
        assertEquals(ItemType.VIDEO, d.type)
        assertEquals(2, calls.size)
        // A site that can't be reached still yields what its address says.
        assertEquals(ItemType.SEMINAR, LinkFetcher { null }.lookup("https://zoom.us/j/1")!!.type)
        assertNull(LinkFetcher { null }.lookup("https://example.org/x"))
        assertNull(LinkFetcher { error("must not fetch") }.lookup("not a link"))
    }
}
