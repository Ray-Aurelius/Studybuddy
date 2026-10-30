package com.rayaurelius.sophinity.data

import com.rayaurelius.sophinity.domain.LinkDetails
import com.rayaurelius.sophinity.domain.LinkDetailsParser
import com.rayaurelius.sophinity.domain.Links
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.nio.charset.Charset
import java.util.Locale

/**
 * Looks up the details of a link someone pasted into a library item. Only that site (or, for
 * YouTube, Vimeo, Spotify and SoundCloud, that site's own oEmbed address) is contacted: there is no
 * Sophinity server and nothing about the person is sent. [get] is swappable for tests.
 */
open class LinkFetcher(private val get: suspend (String) -> String? = ::httpGet) {

    open suspend fun lookup(url: String): LinkDetails? {
        val u = Links.normalize(url)
        if (!Links.isOpenable(u)) return null
        val embed = LinkDetailsParser.oEmbedEndpoint(u)?.let { get(it) }?.let { LinkDetailsParser.fromOEmbed(it, u) }
        // The page itself often has what oEmbed leaves out, such as a video's length.
        val page = if (embed == null || embed.title == null || embed.creator == null || embed.durationSec == null) {
            get(u)?.let { LinkDetailsParser.fromHtml(it, u) }
        } else null
        val found = (embed ?: LinkDetails()).or(page).or(LinkDetails(type = LinkDetailsParser.typeFromUrl(u)))
        return found.takeIf { !it.isEmpty }
    }

    companion object {
        private const val MAX_BYTES = 2_000_000
        private const val MAX_REDIRECTS = 5

        /** A plain GET of a web page or JSON; null on any failure. */
        suspend fun httpGet(start: String): String? = withContext(Dispatchers.IO) {
            runCatching {
                var url = start
                repeat(MAX_REDIRECTS + 1) {
                    val c = (URL(url).openConnection() as HttpURLConnection).apply {
                        instanceFollowRedirects = false
                        connectTimeout = 6_000
                        readTimeout = 8_000
                        setRequestProperty("User-Agent", "Mozilla/5.0 (Linux; Android 15) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/131.0 Mobile Safari/537.36")
                        setRequestProperty("Accept", "text/html,application/xhtml+xml,application/json;q=0.9,*/*;q=0.5")
                        setRequestProperty("Accept-Language", Locale.getDefault().toLanguageTag() + ",en;q=0.5")
                    }
                    try {
                        val code = c.responseCode
                        if (code in 300..399) {
                            val next = c.getHeaderField("Location") ?: return@runCatching null
                            url = URL(URL(url), next).toString()
                            if (!url.startsWith("https://") && !url.startsWith("http://")) return@runCatching null
                            return@repeat
                        }
                        if (code !in 200..299) return@runCatching null
                        val type = c.contentType?.lowercase().orEmpty()
                        if (type.isNotEmpty() && !type.contains("html") && !type.contains("json") && !type.contains("xml")) return@runCatching null
                        val bytes = c.inputStream.use { input ->
                            val out = ByteArrayOutputStream()
                            val buf = ByteArray(16_384)
                            while (out.size() < MAX_BYTES) {
                                val n = input.read(buf); if (n < 0) break
                                out.write(buf, 0, n)
                            }
                            out.toByteArray()
                        }
                        return@runCatching String(bytes, charsetOf(type, bytes))
                    } finally { c.disconnect() }
                }
                null
            }.getOrNull()
        }

        /** From the Content-Type header, else a <meta charset> near the top, else UTF-8. */
        internal fun charsetOf(contentType: String, bytes: ByteArray): Charset {
            val name = Regex("charset=\"?([\\w-]+)").find(contentType)?.groupValues?.get(1)
                ?: Regex("<meta[^>]+charset=[\"']?([\\w-]+)", RegexOption.IGNORE_CASE)
                    .find(String(bytes, 0, minOf(bytes.size, 4096), Charsets.ISO_8859_1))?.groupValues?.get(1)
            return name?.let { runCatching { Charset.forName(it) }.getOrNull() } ?: Charsets.UTF_8
        }
    }
}
