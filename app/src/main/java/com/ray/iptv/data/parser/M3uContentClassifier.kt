package com.ray.iptv.data.parser

enum class M3uContentKind { LIVE, MOVIE, SERIES }

/**
 * Mina `M3uContentClassifier`: düz M3U girişini canlı / film (VOD) / dizi olarak ayırır.
 *
 * Öncelik sırası:
 * 1) Xtream yol biçimi (/series/, /movie/, /live/) en yüksek önceliklidir.
 * 2) Grup + ad kalıpları (S01E02, 1x02, 12. Bölüm, Season 2, vb. -> Dizi).
 * 3) Film ipuçları (IMDb id /tt1234567, 4 haneli yıl grupları 2024, sinema/film anahtarları).
 * 4) Medya uzantıları (.mp4, .mkv, .avi, .mov, .wmv, .webm -> Film/Dizi).
 * 5) Varsayılan: Canlı (LIVE).
 */
object M3uContentClassifier {
    // S01E02, S1 E2, S01x02
    private val seasonEpisode = Regex("""s\d{1,3}\s?[ex]\s?\d{1,3}""", RegexOption.IGNORE_CASE)

    // 1x02 biçimi (2-4 haneli bölüm, 4x4 veya 24x7 kanalları dışlar)
    private val nxnnEpisode = Regex("""\b\d{1,2}\s?x\s?\d{2,4}\b""", RegexOption.IGNORE_CASE)

    // Ad içinde bölüm/sezon kelimeleri (TR + EN)
    private val episodeWords = Regex("""(\bbölüm\b|\bbolum\b|\bsezon\b|\bepisode\b|\bseason\b)""", RegexOption.IGNORE_CASE)

    // IMDb ID: /tt + en az 6 rakam
    private val imdbIdInUrl = Regex("""/tt\d{6,}""")

    // group-title yalnızca 4 haneli yıl ("2024") -> genelde film arşivi
    private val yearOnlyGroup = Regex("""^(19|20)\d{2}$""")

    fun classify(name: String, url: String, group: String): M3uContentKind {
        val nameLc = name.lowercase().trim()
        val urlLc = url.lowercase().trim()
        val groupLc = group.lowercase().trim()

        // 1) Xtream yol biçimi en kesin sinyaldir ve en yüksek önceliklidir
        if (urlLc.contains("/series/")) return M3uContentKind.SERIES
        if (urlLc.contains("/movie/")) return M3uContentKind.MOVIE
        if (urlLc.contains("/live/")) return M3uContentKind.LIVE

        // 2) Grup + ad ipuçları (düz listeler)
        if (isSeries(nameLc, groupLc)) return M3uContentKind.SERIES
        if (isMovie(urlLc, groupLc)) return M3uContentKind.MOVIE

        // 3) Bilinen VOD dosya uzantıları
        val urlClean = urlLc.substringBefore('?')
        if (urlClean.endsWith(".mp4") || urlClean.endsWith(".mkv") || urlClean.endsWith(".avi") ||
            urlClean.endsWith(".mov") || urlClean.endsWith(".wmv") || urlClean.endsWith(".webm") ||
            urlClean.endsWith(".m4v") || urlClean.endsWith(".flv")
        ) {
            return M3uContentKind.MOVIE
        }

        return M3uContentKind.LIVE
    }

    private fun isSeries(name: String, group: String): Boolean {
        if (group.contains("series") ||
            group.contains("tv show") ||
            group.contains("episode") ||
            group.contains("dizi") ||
            group.contains("sezon") ||
            group.contains("season") ||
            group.contains("sitcom") ||
            group.contains("anime")
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
            group.contains("belgesel") ||
            group.contains("documentary") ||
            group.contains("pelicula") ||
            group.contains("filme")
        ) return true

        if (url.contains("/vod/") || url.contains("/movies/")) return true
        if (url.contains("/title/tt") || imdbIdInUrl.containsMatchIn(url)) return true
        if (yearOnlyGroup.matches(group.trim())) return true
        return false
    }
}

