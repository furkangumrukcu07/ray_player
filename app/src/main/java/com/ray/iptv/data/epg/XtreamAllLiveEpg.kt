package com.ray.iptv.data.epg

import android.util.Base64
import com.ray.iptv.data.parser.XmltvParser
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.longOrNull
import java.nio.charset.StandardCharsets
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

data class XtreamEpgProgramme(
    val streamId: String,
    val title: String,
    val plot: String,
    val startMs: Long,
    val endMs: Long
)

/** Mina `parseXtreamGetAllLiveEpgJsonString`. */
object XtreamAllLiveEpg {
    private val base64Title = Regex("""^[A-Za-z0-9+/]+={0,2}$""")
    private val skipRoot = setOf("user_info", "server_info", "epg_listings", "categories")
    private val localDateTime: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
    private val localDateTimeT: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss")

    fun parse(rootEl: JsonElement): Map<String, List<XtreamEpgProgramme>> {
        val decoded = unwrap(rootEl) ?: return emptyMap()
        val byStream = extractLists(decoded)
        val out = LinkedHashMap<String, List<XtreamEpgProgramme>>()
        for ((sid, rows) in byStream) {
            val programmes = rows.mapNotNull { row ->
                if (row !is JsonObject) return@mapNotNull null
                programme(sid, row)
            }.sortedBy { it.startMs }
            if (programmes.isNotEmpty()) out[sid] = programmes
        }
        return out
    }

    private fun unwrap(el: JsonElement): JsonObject? {
        val obj = el as? JsonObject ?: return null
        val data = obj["data"]
        return if (data is JsonObject) data else obj
    }

    private fun extractLists(root: JsonObject): Map<String, List<JsonElement>> {
        val out = LinkedHashMap<String, MutableList<JsonElement>>()
        fun add(sid: String, items: List<JsonElement>) {
            if (sid.isBlank()) return
            out.getOrPut(sid) { ArrayList() }.addAll(items)
        }
        val listings = root["epg_listings"]
        if (listings is JsonObject) {
            for ((k, v) in listings) {
                val sid = normalizeStreamId(k) ?: continue
                if (v is JsonArray) add(sid, v)
            }
            if (out.isNotEmpty()) return out
        }
        if (listings is JsonArray) {
            for (item in listings) {
                val m = item as? JsonObject ?: continue
                val sid = firstStreamId(m) ?: continue
                val nested = m["epg_listings"] ?: m["programmes"]
                if (nested is JsonArray) add(sid, nested) else add(sid, listOf(item))
            }
            if (out.isNotEmpty()) return out
        }
        for ((k, v) in root) {
            if (k in skipRoot) continue
            val sid = normalizeStreamId(k) ?: continue
            if (v is JsonArray) add(sid, v)
        }
        return out
    }

    private fun programme(streamId: String, m: JsonObject): XtreamEpgProgramme? {
        val title = xtreamTitle(m)
        if (title.isBlank()) return null
        val start = xtreamTime(m["start"] ?: m["start_timestamp"] ?: m["time"] ?: m["begin"])
        val end = xtreamTime(m["end"] ?: m["stop"] ?: m["stop_timestamp"] ?: m["end_timestamp"])
        if (start <= 0L || end <= start) return null
        val plot = jsonText(m["description"] ?: m["desc"] ?: m["plot"])
        return XtreamEpgProgramme(streamId, title, plot, start, end)
    }

    private fun xtreamTitle(m: JsonObject): String {
        val t = jsonText(m["title"]).ifBlank { jsonText(m["name"]) }
        if (t.length < 8 || t.length % 4 != 0 || !base64Title.matches(t)) return t
        return runCatching {
            val decoded = String(Base64.decode(t, Base64.DEFAULT), StandardCharsets.UTF_8).trim()
            if (decoded.isNotEmpty() && !decoded.contains('\u0000')) decoded else t
        }.getOrDefault(t)
    }

    internal fun xtreamTime(el: JsonElement?): Long {
        if (el == null) return 0L
        if (el is JsonObject) return xtreamTime(el["1"] ?: el.values.firstOrNull())
        if (el is JsonArray) return xtreamTime(el.firstOrNull())
        val p = el as? JsonPrimitive ?: return 0L
        p.longOrNull?.let { return unixOrXmltvMillis(it) }
        p.intOrNull?.let { return unixOrXmltvMillis(it.toLong()) }
        p.doubleOrNull?.let { return unixOrXmltvMillis(it.toLong()) }
        return parseTimeString(p.contentOrNull?.trim().orEmpty())
    }

    internal fun parseTimeString(s: String): Long {
        if (s.isEmpty()) return 0L
        val digits = s.take(14)
        if (digits.length == 14 && digits.all { it.isDigit() } && (s.length == 14 || s.any { it == '+' || it == '-' || it.isWhitespace() })) {
            val xmltv = XmltvParser.parseTime(s)
            if (xmltv > 0L) return xmltv
        }
        s.toLongOrNull()?.let { return unixOrXmltvMillis(it) }
        runCatching {
            LocalDateTime.parse(s.replace('T', ' ').take(19), localDateTime)
                .atZone(ZoneId.systemDefault())
                .toInstant()
                .toEpochMilli()
        }.getOrNull()?.let { return it }
        runCatching {
            LocalDateTime.parse(s.take(19), localDateTimeT)
                .atZone(ZoneId.systemDefault())
                .toInstant()
                .toEpochMilli()
        }.getOrNull()?.let { return it }
        runCatching { java.time.Instant.parse(s).toEpochMilli() }.getOrNull()?.let { return it }
        return XmltvParser.parseTime(s)
    }

    private fun unixOrXmltvMillis(v: Long): Long {
        if (v == 0L) return 0L
        val a = kotlin.math.abs(v)
        // yyyyMMddHHmmss around 1990–2100 is 14 digits (~1.99e13–2.10e13)
        if (a in 19_900_000_000_000L..21_001_231_235_959L) {
            val parsed = XmltvParser.parseTime(v.toString())
            if (parsed > 0L) return parsed
        }
        return if (a >= 100_000_000_000L) v else v * 1000L
    }

    private fun jsonText(el: JsonElement?): String {
        return when (el) {
            null -> ""
            is JsonPrimitive -> el.contentOrNull?.trim().orEmpty()
            is JsonObject -> jsonText(el["1"] ?: el["en"] ?: el["tr"] ?: el.values.firstOrNull())
            is JsonArray -> jsonText(el.firstOrNull())
            else -> ""
        }
    }

    private fun normalizeStreamId(raw: String): String? {
        val n = raw.trim().toLongOrNull() ?: return null
        if (n <= 0L) return null
        return n.toString()
    }

    private fun firstStreamId(m: JsonObject): String? {
        for (k in listOf("stream_id", "streamId", "channel_id")) {
            normalizeStreamId(jsonText(m[k]))?.let { return it }
        }
        return null
    }
}
