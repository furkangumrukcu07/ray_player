package com.ray.iptv.data.parser

enum class M3uContentKind { LIVE, MOVIE, SERIES }

/**
 * Mina `M3uContentClassifier`: düz M3U girişini canlı / film / dizi olarak ayırır.
 * [name] / [url] / [group] küçük harfe çevrilmiş beklenir.
 */
object M3uContentClassifier {
    private val seasonEpisode = Regex("""s\d{1,3}\s?[ex]\s?\d{1,3}""", RegexOption.IGNORE_CASE)
    private val nxnnEpisode = Regex("""\b\d{1,2}\s?x\s?\d{2,4}\b""", RegexOption.IGNORE_CASE)
    private val episodeWords = Regex("""(\bbölüm\b|\bsezon\b|\bepisode\b|\bseason\b)""", RegexOption.IGNORE_CASE)
    private val imdbIdInUrl = Regex("""/tt\d{6,}""")
    private val yearOnlyGroup = Regex("""^(19|20)\d{2}$""")

    fun classify(name: String, url: String, group: String): M3uContentKind {
        if (url.contains("/series/")) return M3uContentKind.SERIES
        if (url.contains("/movie/")) return M3uContentKind.MOVIE
        if (url.contains("/live/")) return M3uContentKind.LIVE
        if (isSeries(name, group)) return M3uContentKind.SERIES
        if (isMovie(url, group)) return M3uContentKind.MOVIE
        return M3uContentKind.LIVE
    }

    private fun isSeries(name: String, group: String): Boolean {
        if (group.contains("series") ||
            group.contains("tv show") ||
            group.contains("episode") ||
            group.contains("dizi") ||
            group.contains("sezon") ||
            group.contains("season") ||
            group.contains("show")
        ) return true
        if (name.contains("bölüm") ||
            name.contains("bolum") ||
            name.contains("sezon") ||
            name.contains("season") ||
            name.contains("episode")
        ) return true
        if (episodeWords.containsMatchIn(name)) return true
        if (seasonEpisode.containsMatchIn(name)) return true
        if (nxnnEpisode.containsMatchIn(name)) return true
        return false
    }

    private fun isMovie(url: String, group: String): Boolean {
        if (group.contains("movie") ||
            group.contains("vod") ||
            group.contains("film") ||
            group.contains("kino") ||
            group.contains("sinema") ||
            group.contains("cinema") ||
            group.contains("belgesel")
        ) return true
        if (url.contains("/vod/") || url.contains("/movies/")) return true
        if (imdbIdInUrl.containsMatchIn(url)) return true
        val trimmed = group.trim()
        if (trimmed.length == 4 && (trimmed.startsWith("19") || trimmed.startsWith("20")) && trimmed.all { it.isDigit() }) return true
        return false
    }
}
