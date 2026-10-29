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
}
