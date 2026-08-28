package com.ray.iptv.data.parser

data class M3uEntry(
    val name: String,
    val url: String,
    val logo: String,
    val group: String,
    val epgId: String,
    val catchup: String,
    val catchupDays: Int,
    val userAgent: String,
    val referer: String,
    val kindHint: String,
    val plot: String = ""
)

object M3uParser {
    const val BATCH = 2500

    private val attrCache = mutableMapOf<String, List<Regex>>()

    fun parse(text: String): List<M3uEntry> {
        val out = ArrayList<M3uEntry>()
        val sink = LineSink { out += it }
        text.lineSequence().forEach { sink.accept(it) }
        return out
    }

    /**
     * Mina [M3uStreamParser]: dosyayı tek String olarak tutmaz; satır satır okur.
     * Her [BATCH] girişte [onBatch] çağrılır ki UI/DB nefes alsın.
     */
    suspend fun parseReader(
        reader: java.io.BufferedReader,
        onBatch: suspend (List<M3uEntry>) -> Unit
    ) {
        val batch = ArrayList<M3uEntry>(BATCH)
        val sink = LineSink { entry ->
            batch += entry
        }
        while (true) {
            val raw = reader.readLine() ?: break
            sink.accept(raw)
            if (batch.size >= BATCH) {
                onBatch(ArrayList(batch))
                batch.clear()
                kotlinx.coroutines.yield()
            }
        }
        if (batch.isNotEmpty()) onBatch(batch)
    }

    private class LineSink(private val emit: (M3uEntry) -> Unit) {
        var pendingExtinf: String? = null
        var pendingGroup: String? = null
        var userAgent = ""
        var referer = ""

        fun accept(raw: String) {
            val line = raw.trim()
            if (line.startsWith("#EXTINF", ignoreCase = true)) {
                pendingExtinf = line
                pendingGroup = null
                return
            }
            if (pendingExtinf == null) {
                captureHttpHint(line)?.let { (ua, ref) ->
                    if (ua.isNotEmpty()) userAgent = ua
                    if (ref.isNotEmpty()) referer = ref
                }
                return
            }
            if (line.isEmpty() || line.startsWith("#")) {
                if (line.startsWith("#EXTGRP:", ignoreCase = true)) {
                    val g = line.substringAfter(':').trim()
                    if (g.isNotEmpty()) pendingGroup = g
                }
                captureHttpHint(line)?.let { (ua, ref) ->
                    if (ua.isNotEmpty()) userAgent = ua
                    if (ref.isNotEmpty()) referer = ref
                }
                return
            }
            val info = parseExtinfFast(pendingExtinf!!)
            pendingExtinf = null
            var group = info.group.ifBlank { "Uncategorised" }
            if (group == "Uncategorised" && !pendingGroup.isNullOrBlank()) {
                group = pendingGroup!!
            }
            pendingGroup = null
            val name = info.name.ifBlank { "Channel" }

            val rawUrl = line.trim().trim('"', '\'')
            var finalUrl = rawUrl
            var finalUa = userAgent
            var finalRef = referer
            if ('|' in rawUrl) {
                val parts = rawUrl.split('|', limit = 2)
                finalUrl = parts[0].trim()
                val headers = parts.getOrNull(1).orEmpty()
                for (param in headers.split('&')) {
                    val kv = param.split('=', limit = 2)
                    val k = kv[0].trim().lowercase()
                    val v = kv.getOrNull(1)?.trim().orEmpty()
                    if (k == "user-agent" || k == "http-user-agent") {
                        if (v.isNotEmpty()) finalUa = v
                    } else if (k == "referer" || k == "http-referrer" || k == "referrer") {
                        if (v.isNotEmpty()) finalRef = v
                    }
                }
            }
            if (' ' in finalUrl) {
                finalUrl = finalUrl.replace(" ", "%20")
            }

            val kind = M3uContentClassifier.classify(
                name = name.lowercase(),
                url = finalUrl.lowercase(),
                group = group.lowercase()
            )
            emit(
                M3uEntry(
                    name = name,
                    url = finalUrl,
                    logo = info.logo,
                    group = group.ifBlank { "All" },
                    epgId = info.tvgId,
                    catchup = info.catchup,
                    catchupDays = info.catchupDays.toIntOrNull() ?: 0,
                    userAgent = finalUa,
                    referer = finalRef,
                    kindHint = kind.name,
                    plot = info.plot
                )
            )
            userAgent = ""
            referer = ""
        }
    }

    private fun captureHttpHint(line: String): Pair<String, String>? {
        if (!line.startsWith("#EXTVLCOPT:", ignoreCase = true) &&
            !line.startsWith("#EXTHTTP:", ignoreCase = true) &&
            !line.startsWith("#KODIPROP:", ignoreCase = true)
        ) return null
        val body = line.substringAfter(':')
        var ua = ""
        var ref = ""
        when {
            body.contains("http-user-agent=", ignoreCase = true) ||
            body.contains("inputstream.adaptive.manifest_headers=user-agent=", ignoreCase = true) ->
                ua = body.substringAfter('=').trim().trim('"', '\'')
            body.contains("http-referrer=", ignoreCase = true) ||
            body.contains("referer=", ignoreCase = true) ->
                ref = body.substringAfter('=').trim().trim('"', '\'')
        }
        if (ua.isEmpty() && ref.isEmpty()) return null
        return ua to ref
    }

    private data class ExtinfAttributes(
        val name: String,
        val tvgId: String,
        val logo: String,
        val group: String,
        val catchup: String,
        val catchupDays: String,
        val plot: String
    )

    private fun parseExtinfFast(line: String): ExtinfAttributes {
        val commaIdx = line.lastIndexOf(',')
        val name = if (commaIdx >= 0) line.substring(commaIdx + 1).trim() else ""
        val attrSection = if (commaIdx >= 0) line.substring(0, commaIdx) else line

        var tvgId = ""
        var logo = ""
        var group = ""
        var catchup = ""
        var catchupDays = ""
        var plot = ""

        var idx = 0
        val len = attrSection.length
        while (idx < len) {
            while (idx < len && (attrSection[idx].isWhitespace() || attrSection[idx] == ':')) idx++
            if (idx >= len) break
            val keyStart = idx
            while (idx < len && attrSection[idx] != '=' && !attrSection[idx].isWhitespace()) idx++
            val key = attrSection.substring(keyStart, idx).lowercase()

            while (idx < len && attrSection[idx].isWhitespace()) idx++
            if (idx < len && attrSection[idx] == '=') {
                idx++
                while (idx < len && attrSection[idx].isWhitespace()) idx++
                val value: String
                if (idx < len && (attrSection[idx] == '"' || attrSection[idx] == '\'')) {
                    val quote = attrSection[idx]
                    idx++
                    val valStart = idx
                    while (idx < len && attrSection[idx] != quote) idx++
                    value = attrSection.substring(valStart, idx).trim()
                    if (idx < len) idx++
                } else {
                    val valStart = idx
                    while (idx < len && !attrSection[idx].isWhitespace() && attrSection[idx] != ',') idx++
                    value = attrSection.substring(valStart, idx).trim()
                }

                when (key) {
                    "tvg-id" -> if (tvgId.isEmpty()) tvgId = value
                    "tvg-name" -> if (tvgId.isEmpty()) tvgId = value
                    "tvg-logo", "logo" -> if (logo.isEmpty()) logo = value
                    "group-title", "group" -> if (group.isEmpty()) group = value
                    "catchup", "catchup-source" -> catchup = value
                    "catchup-days" -> catchupDays = value
                    "plot", "description", "summary", "info" -> if (plot.isEmpty()) plot = value
                }
            }
        }

        return ExtinfAttributes(name, tvgId, logo, group, catchup, catchupDays, plot)
    }
}
