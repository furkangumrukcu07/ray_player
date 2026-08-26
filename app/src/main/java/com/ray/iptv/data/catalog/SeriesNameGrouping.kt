package com.ray.iptv.data.catalog

import com.ray.iptv.data.local.EpisodeEntity
import com.ray.iptv.data.local.VodEntity

/**
 * Mina `SeriesNameGrouping`: M3U / Xtream dizi satırlarını bölüm-sezon
 * soneklerinden arındırarak gruplar. "Kurtlar Vadisi 01/02/03" → tek dizi.
 */
object SeriesNameGrouping {
    private val spaces = Regex("""\s+""")
    private val episodeMarkerPatterns = listOf(
        Regex("""\s+S\d{1,2}\s*[-–]\s*E\d{1,4}\s*$""", RegexOption.IGNORE_CASE),
        Regex("""\s+S\d{1,2}\s*[-–]?\s*E\d{1,4}\s*$""", RegexOption.IGNORE_CASE),
        Regex("""[.\s_-]+S\d{1,2}\s*[-–]?\s*E\d{1,4}\s*$""", RegexOption.IGNORE_CASE),
        Regex("""\s+\d{1,2}x\d{1,4}\s*$""", RegexOption.IGNORE_CASE),
        Regex("""[.\s_-]+\d{1,2}x\d{1,4}\s*$""", RegexOption.IGNORE_CASE),
        Regex("""\s+S\d{1,2}\s*$""", RegexOption.IGNORE_CASE),
        Regex("""[.\s_-]+S\d{1,2}\s*$""", RegexOption.IGNORE_CASE),
        Regex("""\s+\d{1,2}\.\s*(?:Sezon|Season)\s*$""", RegexOption.IGNORE_CASE),
        Regex("""\s+(?:Sezon|Season)\s*\d{1,2}\s*$""", RegexOption.IGNORE_CASE),
        Regex("""\s+\d{1,4}\.\s*(?:Bölüm|Bolum|Episode|Part)\s*$""", RegexOption.IGNORE_CASE),
        Regex("""\s+(?:Bölüm|Bolum|Episode|Part)\s*\d{1,4}\s*$""", RegexOption.IGNORE_CASE)
    )
    private val seasonEpisodeRe = Regex("""S(\d{1,2})\s*[-–]?\s*E(\d{1,4})\b""", RegexOption.IGNORE_CASE)
    private val seasonXEpisodeRe = Regex("""(\d{1,2})x(\d{1,4})\s*$""", RegexOption.IGNORE_CASE)
    private val seasonEpisodeWordRe = Regex(
        """(?:Season|Sezon)\s*(\d{1,2})\s*(?:Episode|Part|Bolum|Bölüm)\s*(\d{1,4})""",
        RegexOption.IGNORE_CASE
    )
    private val episodeOnlyRe = Regex("""(?:Episode|Part|Bolum|Bölüm)\s*(\d{1,4})""", RegexOption.IGNORE_CASE)
    private val numberedEpisodeRe = Regex("""(?:^|\s)(\d{1,3})\s*[-–.:]""")
    private val trailingNumberRe = Regex("""^(.+\S)\s+(\d{1,3})\s*$""")
    private val trailingSeasonCodeRe = Regex("""S(\d{1,2})\s*$""", RegexOption.IGNORE_CASE)
    private val trailingSeasonWordRe = Regex("""(?:Sezon|Season)\s*(\d{1,2})\s*$""", RegexOption.IGNORE_CASE)
    private val trailingSeasonDotRe = Regex("""(\d{1,2})\.\s*(?:Sezon|Season)\s*$""", RegexOption.IGNORE_CASE)

    fun collapseSpaces(s: String): String = s.replace(spaces, " ").trim()

    fun stripTrailingEpisodeMarkers(input: String): String {
        var s = collapseSpaces(input)
        if (s.isEmpty()) return s
        if (!hasSeasonEpisodeHint(s)) return stripTrailingIndex(s)
        var changed = true
        while (changed && s.isNotEmpty()) {
            changed = false
            for (re in episodeMarkerPatterns) {
                val ns = s.replaceFirst(re, "").trim()
                if (ns != s && ns.isNotEmpty()) {
                    s = ns
                    changed = true
                    break
                }
            }
        }
        return stripTrailingIndex(s)
    }

    private fun hasSeasonEpisodeHint(s: String): Boolean {
        for (i in s.indices) {
            val c = s[i]
            if (c in '0'..'9') return true
        }
        val lower = s.lowercase()
        return lower.contains("sezon") || lower.contains("season") || lower.contains("bölüm") ||
            lower.contains("bolum") || lower.contains("episode") || lower.contains("part")
    }

    private fun stripTrailingIndex(input: String): String {
        val m = trailingNumberRe.matchEntire(input) ?: return input
        val left = m.groupValues[1].trim()
        val words = left.split(spaces).filter { it.isNotEmpty() }
        if (words.size < 2 || left.length < 4) return input
        return left
    }

    fun canonicalKey(rawName: String): String {
        var t = collapseSpaces(rawName)
        if (t.isEmpty()) return ""
        t = stripTrailingEpisodeMarkers(t)
        if (t.isEmpty()) return ""
        return foldTr(t)
    }

    fun displayTitleFromName(rawName: String): String {
        val stripped = stripTrailingEpisodeMarkers(rawName.trim())
        return stripped.ifBlank { rawName.trim() }
    }

    fun group(items: List<VodEntity>): List<List<VodEntity>> {
        val map = linkedMapOf<String, MutableList<VodEntity>>()
        for (s in items) {
            var key = canonicalKey(s.name)
            if (key.isEmpty()) key = "__id_${s.id}"
            map.getOrPut(key) { mutableListOf() }.add(s)
        }
        return map.values
            .map { g -> g.sortedBy { it.name.lowercase() } }
            .sortedBy { displayTitleForGroup(it).lowercase() }
    }

    fun representative(group: List<VodEntity>): VodEntity =
        group.minBy { it.name.trim().length }

    fun displayTitleForGroup(group: List<VodEntity>): String {
        if (group.isEmpty()) return ""
        return displayTitleFromName(representative(group).name)
    }

    fun collapseForBrowse(items: List<VodEntity>): List<VodEntity> {
        if (items.isEmpty()) return items
        return group(items).map { g ->
            val rep = representative(g)
            val title = displayTitleForGroup(g)
            val poster = g.firstOrNull { it.poster.isNotBlank() }?.poster ?: rep.poster
            val plot = g.firstOrNull { it.plot.isNotBlank() }?.plot ?: rep.plot
            rep.copy(name = title.ifBlank { rep.name }, poster = poster, plot = plot)
        }
    }

    fun expandCluster(
        seed: List<VodEntity>,
        displayTitle: String,
        pool: List<VodEntity>
    ): List<VodEntity> {
        if (seed.isEmpty()) return seed
        val byId = seed.associateBy { it.id }.toMutableMap()
        var pk = canonicalKey(displayTitle)
        if (pk.isEmpty()) pk = canonicalKey(seed.first().name)
        if (pk.isEmpty()) return byId.values.sortedBy { it.name.lowercase() }
        for (o in pool) {
            var ok = canonicalKey(o.name)
            if (ok.isEmpty()) ok = "__id_${o.id}"
            if (ok == pk) byId[o.id] = o
        }
        return byId.values.sortedBy { it.name.lowercase() }
    }

    fun parseSeasonEpisode(rawName: String): Pair<Int, Int> {
        val s = rawName.trim()
        if (s.isEmpty()) return 1 to 0
        val lastSe = seasonEpisodeRe.findAll(s).lastOrNull()
        if (lastSe != null) {
            val sn = lastSe.groupValues[1].toIntOrNull() ?: 1
            val en = lastSe.groupValues[2].toIntOrNull() ?: 0
            return sn to en
        }
        seasonXEpisodeRe.find(s)?.let {
            return (it.groupValues[1].toIntOrNull() ?: 1) to (it.groupValues[2].toIntOrNull() ?: 0)
        }
        seasonEpisodeWordRe.find(s)?.let {
            return (it.groupValues[1].toIntOrNull() ?: 1) to (it.groupValues[2].toIntOrNull() ?: 0)
        }
        episodeOnlyRe.find(s)?.let {
            return 1 to (it.groupValues[1].toIntOrNull() ?: 0)
        }
        numberedEpisodeRe.find(s)?.let {
            return 1 to (it.groupValues[1].toIntOrNull() ?: 0)
        }
        trailingNumberRe.matchEntire(collapseSpaces(s))?.let { m ->
            val left = m.groupValues[1].trim()
            val n = m.groupValues[2].toIntOrNull() ?: 0
            if (left.split(spaces).size >= 2 && n > 0) return 1 to n
        }
        return 1 to 0
    }

    /** Xtream'de her sezon ayrı dizi kaydıysa ("Kurtlar Vadisi 2") sezon numarasını çıkarır. */
    fun seasonHintFromName(rawName: String): Int? {
        val s = collapseSpaces(rawName)
        trailingSeasonCodeRe.find(s)?.let { return it.groupValues[1].toIntOrNull() }
        trailingSeasonWordRe.find(s)?.let { return it.groupValues[1].toIntOrNull() }
        trailingSeasonDotRe.find(s)?.let { return it.groupValues[1].toIntOrNull() }
        val m = trailingNumberRe.matchEntire(s) ?: return null
        val left = m.groupValues[1].trim()
        val n = m.groupValues[2].toIntOrNull() ?: return null
        if (left.split(spaces).size < 2 || n <= 0) return null
        return n
    }

    fun m3uEpisodes(seriesId: String, items: List<VodEntity>): List<EpisodeEntity> {
        val byParsed = linkedMapOf<String, MutableList<EpisodeEntity>>()
        val unparsed = mutableListOf<EpisodeEntity>()
        for (s in items) {
            if (s.streamUrl.isBlank()) continue
            val (season, episode) = parseSeasonEpisode(s.name)
            val ep = EpisodeEntity(
                id = "$seriesId:m3u:${s.id}",
                seriesId = seriesId,
                sourceId = s.sourceId,
                remoteId = s.remoteId,
                season = season.coerceAtLeast(1),
                episode = episode,
                name = s.name,
                plot = s.plot,
                still = s.poster,
                streamUrl = s.streamUrl,
                extension = s.extension
            )
            if (episode > 0) byParsed.getOrPut("$season|$episode") { mutableListOf() }.add(ep)
            else unparsed += ep
        }
        val result = mutableListOf<EpisodeEntity>()
        for (opts in byParsed.values) {
            result += opts.maxBy { it.name.length }
        }
        unparsed.sortedBy { it.name.lowercase() }.forEachIndexed { i, ep ->
            result += if (ep.episode <= 0) ep.copy(episode = i + 1) else ep
        }
        return result.sortedWith(compareBy({ it.season }, { it.episode }, { it.name }))
    }

    fun bestPlotFromCluster(cluster: List<VodEntity>): String? =
        cluster.firstOrNull { it.plot.isNotBlank() }?.plot

    private fun foldTr(t: String): String {
        var k = t.replace('İ', 'I').replace('ı', 'i').lowercase()
        k = k.replace('ğ', 'g').replace('ü', 'u').replace('ş', 's')
            .replace('ö', 'o').replace('ç', 'c').replace('ı', 'i')
            .replace('â', 'a').replace('î', 'i').replace('û', 'u')
        return k
    }
}
