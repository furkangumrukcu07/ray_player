package com.ray.iptv.data.parser

import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory
import java.io.BufferedInputStream
import java.io.InputStream
import java.io.InputStreamReader
import java.io.Reader
import java.io.StringReader
import java.nio.charset.StandardCharsets
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import java.util.zip.GZIPInputStream


data class XmltvProgramme(
    val epgId: String,
    val title: String,
    val plot: String,
    val startMs: Long,
    val endMs: Long
)

data class XmltvChannel(
    val id: String,
    val name: String,
    val logo: String = ""
)

data class XmltvDocument(
    val channels: List<XmltvChannel>,
    val programmes: List<XmltvProgramme>
)

object XmltvParser {
    private val xmltvOffset: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyyMMddHHmmss Z", Locale.US)
    private val xmltvLocal: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyyMMddHHmmss", Locale.US)
    private val tzInsert = Regex("(?<=\\d)(?=[+-])")

    fun parse(xml: String, limit: Int = 500_000): List<XmltvProgramme> =
        parseDocument(xml, limit).programmes

    fun parseDocument(xml: String, limit: Int = 500_000): XmltvDocument =
        parseReader(StringReader(xml), limit)

    fun parseStream(stream: InputStream, gzip: Boolean = false, limit: Int = 500_000): XmltvDocument {
        val buffered = if (stream is BufferedInputStream) stream else BufferedInputStream(stream, 64 * 1024)
        buffered.mark(4)
        val b0 = buffered.read()
        val b1 = buffered.read()
        buffered.reset()
        val isGz = gzip || (b0 == 0x1f && b1 == 0x8b)
        val decoded: InputStream = if (isGz) GZIPInputStream(buffered) else buffered
        return parseReader(InputStreamReader(decoded, StandardCharsets.UTF_8), limit)
    }

    fun parseReader(reader: Reader, limit: Int = 500_000): XmltvDocument {
        val factory = XmlPullParserFactory.newInstance()
        val parser = factory.newPullParser()
        parser.setInput(reader)
        val programmes = ArrayList<XmltvProgramme>(4096)
        val channels = ArrayList<XmltvChannel>(256)
        var event = parser.eventType
        var channel = ""
        var start = 0L
        var stop = 0L
        var title = ""
        var plot = ""
        var inTitle = false
        var inDesc = false
        var inDisplayName = false
        var chId = ""
        var chName = ""
        var chLogo = ""
        while (event != XmlPullParser.END_DOCUMENT && programmes.size < limit) {
            when (event) {
                XmlPullParser.START_TAG -> when (parser.name) {
                    "channel" -> {
                        chId = parser.getAttributeValue(null, "id").orEmpty()
                        chName = ""
                        chLogo = ""
                    }
                    "display-name" -> inDisplayName = true
                    "icon" -> {
                        val src = parser.getAttributeValue(null, "src").orEmpty()
                        if (src.isNotBlank() && chLogo.isBlank()) chLogo = src
                    }
                    "programme" -> {
                        channel = parser.getAttributeValue(null, "channel").orEmpty()
                        start = parseTime(parser.getAttributeValue(null, "start"))
                        stop = parseTime(parser.getAttributeValue(null, "stop"))
                        title = ""
                        plot = ""
                    }
                    "title" -> inTitle = true
                    "desc" -> inDesc = true
                }
                XmlPullParser.TEXT -> {
                    val text = parser.text.orEmpty()
                    if (inTitle) title += text
                    if (inDesc) plot += text
                    if (inDisplayName) chName += text
                }
                XmlPullParser.END_TAG -> when (parser.name) {
                    "title" -> inTitle = false
                    "desc" -> inDesc = false
                    "display-name" -> inDisplayName = false
                    "channel" -> if (chId.isNotBlank()) {
                        channels += XmltvChannel(chId, chName.trim().ifBlank { chId }, chLogo)
                    }
                    "programme" -> if (channel.isNotBlank() && start > 0 && stop > start) {
                        programmes += XmltvProgramme(channel, title.trim(), plot.trim(), start, stop)
                    }
                }
            }
            event = parser.next()
        }
        return XmltvDocument(channels, programmes)
    }

    fun parseTime(raw: String?): Long {
        if (raw.isNullOrBlank()) return 0
        val cleaned = raw.trim().replace(tzInsert, " ")
        runCatching { OffsetDateTime.parse(cleaned, xmltvOffset).toInstant().toEpochMilli() }
            .getOrNull()
            ?.let { return it }
        val localPart = cleaned.substringBefore(' ').trim()
        if (localPart.length >= 14) {
            runCatching {
                LocalDateTime.parse(localPart.take(14), xmltvLocal)
                    .atZone(ZoneId.systemDefault())
                    .toInstant()
                    .toEpochMilli()
            }.getOrNull()?.let { return it }
        }
        return 0
    }
}

/** Mina `M3uXmltvNameMatcher` — yedek EPG ad eşlemesi. */
object EpgMatcher {
    const val MIN_ACCEPT = 0.35

    private val reCountryPrefix = Regex("""^[a-z]{2,3}\s?[:\-|]\s?""")
    private val reTechWords = Regex(
        """\b(hd|fhd|uhd|4k|sd|hevc|h265|h264|raw|vip|premium|back|alternate|multi|mono|stereo|aac|ac3|dts|8k|plus|backup)\b"""
    )
    private val reBracket = Regex("""\[[^\]]*\]""")
    private val reParen = Regex("""\([^)]*\)""")
    private val reChannelWords = Regex("""\b(tv|iptv|channel|stream|official|live)\b""")
    private val reSpecial = Regex("""[^a-zA-Z0-9ğüşöçıİâîû\s]""")
    private val reMultiSpace = Regex("""\s+""")

    fun normalize(name: String): String {
        var s = name.lowercase(Locale.US).trim()
        s = s.replaceFirst(reCountryPrefix, " ")
        s = s.replace(reTechWords, " ")
        s = s.replace(reBracket, " ")
        s = s.replace(reParen, " ")
        s = s.replace(reChannelWords, " ")
        s = s.replace(reSpecial, " ")
        s = s.replace(reMultiSpace, " ")
        return foldTr(s.trim())
    }

    fun compact(name: String): String = normalize(name).replace(" ", "")

    private fun foldTr(s: String): String = s
        .replace('ğ', 'g')
        .replace('ü', 'u')
        .replace('ş', 's')
        .replace('ı', 'i')
        .replace('ö', 'o')
        .replace('ç', 'c')
        .replace('â', 'a')
        .replace('î', 'i')
        .replace('û', 'u')
}
