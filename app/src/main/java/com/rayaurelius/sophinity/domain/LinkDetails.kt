package com.rayaurelius.sophinity.domain

import com.rayaurelius.sophinity.data.db.ItemType
import com.rayaurelius.sophinity.data.db.LearningItem
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.doubleOrNull
import java.net.URLEncoder
import kotlin.math.roundToInt

/** What a web page says about the thing it's for. Every field is optional. */
data class LinkDetails(
    val title: String? = null,
    val creator: String? = null,
    val type: ItemType? = null,
    val durationSec: Long? = null,
    val pages: Int? = null,
) {
    val isEmpty: Boolean get() = title == null && creator == null && type == null && durationSec == null && pages == null

    /** Fields from this, with gaps filled from [other]. */
    fun or(other: LinkDetails?): LinkDetails = if (other == null) this else LinkDetails(
        title = title ?: other.title, creator = creator ?: other.creator, type = type ?: other.type,
        durationSec = durationSec ?: other.durationSec, pages = pages ?: other.pages,
    )
}

/**
 * Reads the details people would otherwise type (title, author, kind of media, run time, pages)
 * from the standard metadata pages publish for search engines and link previews: schema.org
 * JSON-LD, Open Graph and other meta tags, and oEmbed for the big video and audio sites.
 * Pure text in, data out, so it is unit-tested against saved page snippets.
 */
object LinkDetailsParser {
    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    // ---- Well-known sites: the kind of media is clear from the address alone.

    private val hostTypes: List<Pair<String, ItemType>> = listOf(
        "youtube.com" to ItemType.VIDEO, "youtu.be" to ItemType.VIDEO, "vimeo.com" to ItemType.VIDEO,
        "ted.com" to ItemType.VIDEO, "nebula.tv" to ItemType.VIDEO, "dailymotion.com" to ItemType.VIDEO,
        "coursera.org" to ItemType.COURSE, "udemy.com" to ItemType.COURSE, "edx.org" to ItemType.COURSE,
        "khanacademy.org" to ItemType.COURSE, "skillshare.com" to ItemType.COURSE, "masterclass.com" to ItemType.COURSE,
        "futurelearn.com" to ItemType.COURSE, "domestika.org" to ItemType.COURSE, "codecademy.com" to ItemType.COURSE,
        "pluralsight.com" to ItemType.COURSE, "brilliant.org" to ItemType.COURSE, "thegreatcourses.com" to ItemType.COURSE,
        "wondrium.com" to ItemType.COURSE, "ocw.mit.edu" to ItemType.COURSE,
        "audible.com" to ItemType.AUDIOBOOK, "audible.co.uk" to ItemType.AUDIOBOOK, "audible.ca" to ItemType.AUDIOBOOK,
        "audible.com.au" to ItemType.AUDIOBOOK, "audible.de" to ItemType.AUDIOBOOK, "audible.fr" to ItemType.AUDIOBOOK,
        "audible.it" to ItemType.AUDIOBOOK, "audible.es" to ItemType.AUDIOBOOK, "audible.co.jp" to ItemType.AUDIOBOOK,
        "audible.in" to ItemType.AUDIOBOOK, "libro.fm" to ItemType.AUDIOBOOK, "audiobooks.com" to ItemType.AUDIOBOOK,
        "storytel.com" to ItemType.AUDIOBOOK, "librivox.org" to ItemType.AUDIOBOOK, "chirpbooks.com" to ItemType.AUDIOBOOK,
        "podcasts.apple.com" to ItemType.PODCAST, "pocketcasts.com" to ItemType.PODCAST, "overcast.fm" to ItemType.PODCAST,
        "podbean.com" to ItemType.PODCAST, "buzzsprout.com" to ItemType.PODCAST, "castbox.fm" to ItemType.PODCAST,
        "podcasts.google.com" to ItemType.PODCAST, "podcasters.spotify.com" to ItemType.PODCAST, "simplecast.com" to ItemType.PODCAST,
        "zoom.us" to ItemType.SEMINAR, "eventbrite.com" to ItemType.SEMINAR, "eventbrite.co.uk" to ItemType.SEMINAR,
        "meetup.com" to ItemType.SEMINAR, "lu.ma" to ItemType.SEMINAR, "webex.com" to ItemType.SEMINAR,
        "gotowebinar.com" to ItemType.SEMINAR, "teams.microsoft.com" to ItemType.SEMINAR, "crowdcast.io" to ItemType.SEMINAR,
        "goodreads.com" to ItemType.BOOK, "openlibrary.org" to ItemType.BOOK, "gutenberg.org" to ItemType.BOOK,
        "bookshop.org" to ItemType.BOOK, "books.google.com" to ItemType.BOOK, "storygraph.com" to ItemType.BOOK,
        "app.thestorygraph.com" to ItemType.BOOK,
        "medium.com" to ItemType.ARTICLE, "substack.com" to ItemType.ARTICLE, "wikipedia.org" to ItemType.ARTICLE,
        "dev.to" to ItemType.ARTICLE, "arxiv.org" to ItemType.ARTICLE,
    )

    /** The kind of media a well-known site's address implies, or null. */
    fun typeFromUrl(url: String): ItemType? {
        val host = Links.host(url)
        val path = url.substringAfter("://").substringAfter('/', "").lowercase()
        if (host == "open.spotify.com") return when {
            path.startsWith("show") || path.startsWith("episode") -> ItemType.PODCAST
            path.startsWith("audiobook") -> ItemType.AUDIOBOOK
            else -> null
        }
        if (host == "linkedin.com" && path.startsWith("learning")) return ItemType.COURSE
        if (host == "amazon.com" || host.startsWith("amazon.")) return if (path.contains("audible")) ItemType.AUDIOBOOK else null
        return hostTypes.firstOrNull { (h, _) -> host == h || host.endsWith(".$h") }?.second
    }

    // ---- oEmbed: the official way YouTube, Vimeo, Spotify and SoundCloud describe a link.

    fun oEmbedEndpoint(url: String): String? {
        val host = Links.host(url)
        val enc = URLEncoder.encode(url, "UTF-8")
        return when {
            host == "youtu.be" || host == "youtube.com" || host.endsWith(".youtube.com") -> "https://www.youtube.com/oembed?format=json&url=$enc"
            host == "vimeo.com" || host.endsWith(".vimeo.com") -> "https://vimeo.com/api/oembed.json?url=$enc"
            host == "open.spotify.com" -> "https://open.spotify.com/oembed?url=$enc"
            host == "soundcloud.com" || host.endsWith(".soundcloud.com") -> "https://soundcloud.com/oembed?format=json&url=$enc"
            else -> null
        }
    }

    fun fromOEmbed(text: String, url: String): LinkDetails {
        val o = runCatching { json.parseToJsonElement(text) as? JsonObject }.getOrNull() ?: return LinkDetails()
        val title = o.str("title")?.let { clean(it) }
        val author = o.str("author_name")?.let { clean(it) }
        val seconds = o["duration"]?.let { (it as? JsonPrimitive)?.doubleOrNull }?.toLong()?.takeIf { it > 0 }
        val type = typeFromUrl(url) ?: if (o.str("type") == "video") ItemType.VIDEO else null
        return LinkDetails(title = title, creator = author, type = type, durationSec = seconds)
    }

    // ---- The page itself.

    private val metaTag = Regex("<meta\\b[^>]*>", RegexOption.IGNORE_CASE)
    private val attr = Regex("([a-zA-Z_:][-a-zA-Z0-9_:.]*)\\s*=\\s*(?:\"([^\"]*)\"|'([^']*)'|([^\\s\"'>]+))")
    private val titleTag = Regex("<title\\b[^>]*>(.*?)</title>", setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL))
    private val ldScript = Regex(
        "<script\\b[^>]*type\\s*=\\s*[\"']?application/ld\\+json[\"']?[^>]*>(.*?)</script>",
        setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL),
    )
    /** Audible and others print "Length: 7 hrs and 30 mins" rather than marking it up. */
    private val lengthText = Regex(
        "Length:\\s*(?:(\\d+)\\s*hrs?)?\\s*(?:and)?\\s*(?:(\\d+)\\s*mins?)?", RegexOption.IGNORE_CASE,
    )

    /** Meta tags by their property / name / itemprop (lower-case); the first one of each wins. */
    fun metaTags(html: String): Map<String, String> {
        val out = LinkedHashMap<String, String>()
        for (m in metaTag.findAll(html)) {
            val attrs = attr.findAll(m.value).associate { a ->
                a.groupValues[1].lowercase() to (a.groups[2]?.value ?: a.groups[3]?.value ?: a.groups[4]?.value ?: "")
            }
            val content = attrs["content"] ?: continue
            for (k in listOf("property", "name", "itemprop")) {
                val key = attrs[k]?.lowercase() ?: continue
                out.putIfAbsent(key, content)
            }
        }
        return out
    }

    /** Every JSON-LD object on the page, with @graph and arrays flattened. */
    fun ldObjects(html: String): List<JsonObject> {
        val out = mutableListOf<JsonObject>()
        fun walk(e: JsonElement?) {
            when (e) {
                is JsonArray -> e.forEach(::walk)
                is JsonObject -> { out += e; walk(e["@graph"]) }
                else -> Unit
            }
        }
        for (m in ldScript.findAll(html)) {
            val body = m.groupValues[1].trim().removePrefix("<!--").removeSuffix("-->").trim()
            walk(runCatching { json.parseToJsonElement(body) }.getOrNull())
        }
        return out
    }

    private val ldTypes: Map<String, ItemType> = mapOf(
        "audiobook" to ItemType.AUDIOBOOK,
        "book" to ItemType.BOOK,
        "podcastseries" to ItemType.PODCAST, "podcastepisode" to ItemType.PODCAST, "podcastseason" to ItemType.PODCAST,
        "radioseries" to ItemType.PODCAST,
        "course" to ItemType.COURSE, "courseinstance" to ItemType.COURSE,
        "videoobject" to ItemType.VIDEO, "movie" to ItemType.VIDEO, "tvepisode" to ItemType.VIDEO, "tvseries" to ItemType.VIDEO,
        "event" to ItemType.SEMINAR, "educationevent" to ItemType.SEMINAR, "businessevent" to ItemType.SEMINAR,
        "socialevent" to ItemType.SEMINAR, "hackathon" to ItemType.SEMINAR,
        "article" to ItemType.ARTICLE, "newsarticle" to ItemType.ARTICLE, "blogposting" to ItemType.ARTICLE,
        "techarticle" to ItemType.ARTICLE, "scholarlyarticle" to ItemType.ARTICLE, "report" to ItemType.ARTICLE,
    )

    private fun JsonObject.types(): List<String> = when (val t = this["@type"]) {
        is JsonPrimitive -> listOfNotNull(t.contentOrNull)
        is JsonArray -> t.mapNotNull { (it as? JsonPrimitive)?.contentOrNull }
        else -> emptyList()
    }.map { it.substringAfterLast('/').lowercase() }

    /** Audiobook outranks Book (an audiobook is often typed as both), and so on down the list. */
    private fun JsonObject.mediaType(): ItemType? {
        val t = types()
        return ldTypes.entries.firstOrNull { (k, _) -> k in t }?.value
    }

    private fun JsonObject.str(key: String): String? =
        (this[key] as? JsonPrimitive)?.contentOrNull?.takeIf { it.isNotBlank() }

    /** A person or organisation, a plain name, or a list of them. */
    private fun names(e: JsonElement?): List<String> = when (e) {
        is JsonPrimitive -> listOfNotNull(e.contentOrNull?.takeIf { it.isNotBlank() && !it.startsWith("http") })
        is JsonObject -> listOfNotNull(e.str("name"))
        is JsonArray -> e.flatMap(::names)
        else -> emptyList()
    }

    fun fromHtml(html: String, url: String): LinkDetails {
        val meta = metaTags(html)
        val ld = ldObjects(html)
        // The page's main subject: the first object of a media kind we know, else the first with a name.
        val main = ld.firstOrNull { it.mediaType() != null } ?: ld.firstOrNull { it.str("name") != null }
        val siteName = meta["og:site_name"]

        val rawTitle = main?.str("name") ?: main?.str("headline") ?: meta["og:title"] ?: meta["twitter:title"]
            ?: titleTag.find(html)?.groupValues?.get(1)
        val title = rawTitle?.let { cleanTitle(clean(it), siteName, url) }?.takeIf { it.isNotBlank() }

        val creatorKeys = listOf("author", "byArtist", "creator", "readBy", "director", "organizer", "provider", "performer")
        val creator = main?.let { m -> creatorKeys.firstNotNullOfOrNull { k -> names(m[k]).takeIf { it.isNotEmpty() } } }
            ?.distinct()?.take(3)?.joinToString(", ")
            ?: meta["author"]?.takeIf { !it.startsWith("http") }
            ?: meta["book:author"]?.takeIf { !it.startsWith("http") }
            ?: meta["article:author"]?.takeIf { !it.startsWith("http") }

        val ogType = meta["og:type"]?.lowercase()
        val type = typeFromUrl(url) ?: main?.mediaType() ?: when {
            ogType == null -> null
            ogType.startsWith("video") -> ItemType.VIDEO
            ogType == "book" || ogType == "books.book" -> ItemType.BOOK
            ogType == "article" -> ItemType.ARTICLE
            else -> null
        }

        val duration = (main?.let { it.str("duration") ?: it.str("timeRequired") })?.let(::isoDurationSec)
            ?: meta["duration"]?.let(::isoDurationSec)
            ?: listOf("og:video:duration", "video:duration", "music:duration").firstNotNullOfOrNull { k -> meta[k]?.toDoubleOrNull()?.toLong() }
            ?: lengthText.find(html)?.let { m ->
                val h = m.groupValues[1].toLongOrNull() ?: 0
                val min = m.groupValues[2].toLongOrNull() ?: 0
                (h * 3600 + min * 60).takeIf { it > 0 }
            }

        val pages = main?.get("numberOfPages")?.let { e ->
            (e as? JsonPrimitive)?.contentOrNull?.filter { it.isDigit() }?.toIntOrNull()
        } ?: meta["books:page_count"]?.toIntOrNull()

        return LinkDetails(
            title = title, creator = creator?.let { clean(it) }?.takeIf { it.isNotBlank() }, type = type,
            durationSec = duration?.takeIf { it in 1..(400L * 3600) }, pages = pages?.takeIf { it in 1..20_000 },
        )
    }

    // ---- Text tidying

    /** "PT1H2M30S" or "P1DT2H" → seconds. */
    fun isoDurationSec(s: String): Long? {
        val m = Regex("^P(?:(\\d+(?:\\.\\d+)?)D)?(?:T(?:(\\d+(?:\\.\\d+)?)H)?(?:(\\d+(?:\\.\\d+)?)M)?(?:(\\d+(?:\\.\\d+)?)S)?)?$", RegexOption.IGNORE_CASE)
            .find(s.trim()) ?: return null
        val (d, h, min, sec) = m.destructured
        if (d.isEmpty() && h.isEmpty() && min.isEmpty() && sec.isEmpty()) return null
        fun n(x: String) = x.toDoubleOrNull() ?: 0.0
        return (n(d) * 86400 + n(h) * 3600 + n(min) * 60 + n(sec)).roundToInt().toLong().takeIf { it > 0 }
    }

    /** Drops a trailing " - YouTube" / " | Coursera" style site name from a page title. */
    fun cleanTitle(title: String, siteName: String?, url: String): String {
        val brand = Links.host(url).split('.').let { parts -> if (parts.size >= 2) parts[parts.size - 2] else parts.first() }
        val names = listOfNotNull(siteName, brand).map { it.lowercase().replace(" ", "") }
        var t = title.trim()
        val sep = Regex("\\s+[-|–—·:]\\s+")
        val last = sep.findAll(t).lastOrNull()
        if (last != null) {
            val tail = t.substring(last.range.last + 1).lowercase().replace(" ", "")
            if (names.any { tail == it || (it.length >= 4 && tail.startsWith(it)) || (tail.length >= 4 && it.startsWith(tail)) }) t = t.substring(0, last.range.first).trim()
        }
        // YouTube's <title> starts with the unread-notification count, e.g. "(3) ".
        return t.replace(Regex("^\\(\\d+\\)\\s+"), "")
    }

    /** Decodes HTML entities and squeezes whitespace. */
    fun clean(s: String): String {
        val named = mapOf("amp" to "&", "lt" to "<", "gt" to ">", "quot" to "\"", "apos" to "'", "nbsp" to " ",
            "ndash" to "–", "mdash" to "—", "lsquo" to "‘", "rsquo" to "’", "ldquo" to "“", "rdquo" to "”", "hellip" to "…")
        val decoded = Regex("&(#x[0-9a-fA-F]+|#\\d+|[a-zA-Z]+);").replace(s) { m ->
            val code = m.groupValues[1]
            when {
                code.startsWith("#x") || code.startsWith("#X") -> code.drop(2).toIntOrNull(16)?.let { String(Character.toChars(it)) } ?: m.value
                code.startsWith("#") -> code.drop(1).toIntOrNull()?.let { String(Character.toChars(it)) } ?: m.value
                else -> named[code.lowercase()] ?: m.value
            }
        }
        return decoded.replace(Regex("\\s+"), " ").trim()
    }
}

/** Fills the empty parts of a library item from a link's details, and can take that back. */
object LinkAutofill {
    enum class Field { TITLE, CREATOR, TYPE, RUNTIME, PAGES }

    /**
     * Only fields still empty are filled; nothing typed is overwritten. The kind of media changes
     * only when [allowTypeChange] (a new item whose type the person hasn't picked themselves).
     */
    fun apply(item: LearningItem, d: LinkDetails, allowTypeChange: Boolean): Pair<LearningItem, Set<Field>> {
        var out = item
        val filled = mutableSetOf<Field>()
        if (out.title.isBlank() && d.title != null) { out = out.copy(title = d.title.take(300)); filled += Field.TITLE }
        if (out.creator.isBlank() && d.creator != null) { out = out.copy(creator = d.creator.take(200)); filled += Field.CREATOR }
        if (allowTypeChange && d.type != null && d.type != out.type && out.type != ItemType.CUSTOM) {
            out = out.copy(type = d.type, categoryId = null); filled += Field.TYPE
        }
        val sec = d.durationSec
        if (sec != null && sec >= 60 && out.runtimeMin == null && MediaTime.supportsRuntime(out.type)) {
            out = out.copy(runtimeMin = ((sec + 30) / 60).toInt()); filled += Field.RUNTIME
        }
        if (d.pages != null && out.totalUnits == null && out.type == ItemType.BOOK) {
            out = out.copy(totalUnits = d.pages); filled += Field.PAGES
        }
        return out to filled
    }

    /** Puts back what [fields] were in [before], keeping anything else changed since. */
    fun undo(current: LearningItem, before: LearningItem, fields: Set<Field>): LearningItem {
        var out = current
        for (f in fields) out = when (f) {
            Field.TITLE -> out.copy(title = before.title)
            Field.CREATOR -> out.copy(creator = before.creator)
            Field.TYPE -> out.copy(type = before.type, categoryId = before.categoryId)
            Field.RUNTIME -> out.copy(runtimeMin = before.runtimeMin)
            Field.PAGES -> out.copy(totalUnits = before.totalUnits)
        }
        return out
    }
}
