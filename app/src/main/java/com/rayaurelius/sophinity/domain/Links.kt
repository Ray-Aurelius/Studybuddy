package com.rayaurelius.sophinity.domain

/** Web links on library items: what people paste is tidied up, and shown as a short site name. */
object Links {
    /**
     * Trims the text and adds https:// when no scheme was typed ("youtube.com/watch?v=…").
     * Blank stays blank. Text with spaces inside isn't a link, so it's left alone for the person to fix.
     */
    fun normalize(input: String): String {
        val t = input.trim()
        if (t.isEmpty()) return ""
        if (t.any { it.isWhitespace() }) return t
        return if (Regex("^[a-zA-Z][a-zA-Z0-9+.-]*://").containsMatchIn(t)) t else "https://$t"
    }

    /** True for something a browser can open. */
    fun isOpenable(url: String): Boolean =
        url.isNotBlank() && url.none { it.isWhitespace() } &&
            (url.startsWith("https://", ignoreCase = true) || url.startsWith("http://", ignoreCase = true)) &&
            host(url).contains('.')

    /** "https://www.example.com/talks/42" → "example.com". */
    fun host(url: String): String =
        url.substringAfter("://").substringBefore('/').substringBefore('?').substringBefore('#')
            .substringAfterLast('@').substringBefore(':').removePrefix("www.").lowercase()

    /**
     * The start time a share link carries, in seconds: YouTube "?t=916" / "&t=15m16s",
     * Vimeo "#t=15m16s", Spotify "?t=916", or "start=" / "time_continue=". Null if there's none.
     */
    fun timestampSec(url: String): Long? {
        val params = (url.substringAfter('?', "").substringBefore('#').split('&') + url.substringAfter('#', "").split('&'))
            .mapNotNull { p -> p.split('=', limit = 2).takeIf { it.size == 2 }?.let { it[0].lowercase() to it[1] } }
        val raw = params.firstOrNull { it.first in TIME_KEYS }?.second ?: return null
        return parseTime(java.net.URLDecoder.decode(raw, "UTF-8"))?.takeIf { it > 0 }
    }

    /** "916", "916s", "15m16s", "1h2m3s", "15:16" or "1:02:03" → seconds. */
    fun parseTime(s: String): Long? {
        val t = s.trim().lowercase()
        t.toLongOrNull()?.let { return it }
        Regex("^(\\d+)s$").find(t)?.let { return it.groupValues[1].toLong() }
        Regex("^(?:(\\d+)h)?(?:(\\d+)m)?(?:(\\d+)s)?$").find(t)?.takeIf { t.isNotEmpty() }?.let { m ->
            val (h, mi, se) = m.destructured
            return (h.toLongOrNull() ?: 0) * 3600 + (mi.toLongOrNull() ?: 0) * 60 + (se.toLongOrNull() ?: 0)
        }
        val parts = t.split(':')
        if (parts.size in 2..3 && parts.all { it.isNotEmpty() && it.all(Char::isDigit) }) {
            return parts.fold(0L) { acc, x -> acc * 60 + x.toLong() }
        }
        return null
    }

    /** The link without its start time, so opening it later doesn't jump back to an old spot. */
    fun withoutTimestamp(url: String): String {
        val base = url.substringBefore('#')
        val fragment = url.substringAfter('#', "")
        val path = base.substringBefore('?')
        val query = base.substringAfter('?', "").split('&').filter { it.isNotEmpty() && it.substringBefore('=').lowercase() !in TIME_KEYS }
        val frag = fragment.split('&').filter { it.isNotEmpty() && it.substringBefore('=').lowercase() !in TIME_KEYS }
        return path + (if (query.isNotEmpty()) "?" + query.joinToString("&") else "") + (if (frag.isNotEmpty()) "#" + frag.joinToString("&") else "")
    }

    /** The first web address in shared text ("Check this out https://youtu.be/x?t=5" → the link). */
    fun firstUrl(text: String): String? =
        Regex("https?://[^\\s<>\"]+", RegexOption.IGNORE_CASE).find(text)?.value?.trimEnd('.', ',', ')', '!', '?', ';')

    /**
     * Two links point at the same video, episode or page when their keys match: YouTube by video id
     * (watch?v=, youtu.be, shorts, live), anything else by site and path, ignoring the query, the
     * start time, "www.", "m." and a trailing slash.
     */
    fun mediaKey(url: String): String {
        val u = normalize(url)
        var host = host(u).removePrefix("m.")
        val path = u.substringAfter("://").substringAfter('/', "").substringBefore('?').substringBefore('#').trimEnd('/')
        if (host == "youtu.be" || host == "youtube.com" || host.endsWith(".youtube.com")) {
            host = "youtube.com"
            val id = when {
                host(u) == "youtu.be" -> path.substringBefore('/')
                path.startsWith("shorts/") || path.startsWith("live/") || path.startsWith("embed/") -> path.substringAfter('/').substringBefore('/')
                else -> u.substringAfter('?', "").substringBefore('#').split('&').firstOrNull { it.startsWith("v=") }?.substringAfter("v=")
            }
            if (!id.isNullOrEmpty()) return "youtube:$id"
        }
        // Spotify links carry a per-share "si" tag; Apple Podcasts episodes live in "?i=".
        val episode = u.substringAfter('?', "").split('&').firstOrNull { it.startsWith("i=") }
        return "$host/$path" + (if (host == "podcasts.apple.com" && episode != null) "?$episode" else "")
    }

    private val TIME_KEYS = setOf("t", "start", "time_continue")
}
