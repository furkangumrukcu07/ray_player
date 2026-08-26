package com.ray.iptv.data.epg

import com.ray.iptv.data.local.ChannelEntity
import com.ray.iptv.data.local.EpgEntity
import com.ray.iptv.data.local.GlobalEpgChannelEntity
import com.ray.iptv.data.local.GlobalEpgProgrammeEntity
import com.ray.iptv.data.local.RayDatabase
import com.ray.iptv.data.parser.EpgMatcher
import com.ray.iptv.data.parser.XmltvParser
import com.ray.iptv.data.repo.SettingsRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.Locale
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Mina `GlobalEpgService` — EPGShare01 yedek rehber.
 * Birincil Xtream/XMLTV haritalarına yazılmaz; kanal adı ile okuma anında doldurulur.
 */
@Singleton
class GlobalEpgService @Inject constructor(
    private val db: RayDatabase,
    private val http: OkHttpClient,
    private val settings: SettingsRepository
) {
    private val mutex = Mutex()
    private val nameLowerToXmlId = HashMap<String, String>()
    private var lastFingerprint = 0
    private var lastLoadAtMs = 0L

    private val downloadClient by lazy {
        http.newBuilder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(600, TimeUnit.SECONDS)
            .writeTimeout(45, TimeUnit.SECONDS)
            .build()
    }

    suspend fun stats(): Pair<Int, Int> = db.globalEpgChannels().count() to db.globalEpgProgrammes().count()

    fun xmlIdForName(name: String): String? {
        if (name.isBlank()) return null
        nameLowerToXmlId[name.lowercase(Locale.US)]?.let { return it }
        nameLowerToXmlId[EpgMatcher.normalize(name)]?.let { return it }
        nameLowerToXmlId[EpgMatcher.compact(name)]?.let { return it }
        return null
    }

    fun findEpgIdForName(name: String): String? {
        val exact = xmlIdForName(name)
        if (exact != null) return exact
        val comp = EpgMatcher.compact(name)
        if (comp.length < 3) return null
        for ((key, xmlId) in nameLowerToXmlId) {
            if (key.length >= 3 && (key.contains(comp) || comp.contains(key))) {
                return xmlId
            }
        }
        return null
    }

    private suspend fun ensureMatched() {
        if (nameLowerToXmlId.isNotEmpty()) return
        rematchFromGlobalNames()
    }

    private suspend fun rematchFromGlobalNames() = withContext(Dispatchers.Default) {
        val dbChannels = db.globalEpgChannels().all()
        val map = HashMap<String, String>(dbChannels.size * 2)
        for (row in dbChannels) {
            val xml = row.xmlChannelId
            if (xml.isBlank()) continue
            val display = row.displayName
            if (display.isNotBlank()) map.putIfAbsent(display.lowercase(Locale.US), xml)
            val n = EpgMatcher.normalize(display)
            if (n.isNotEmpty()) map.putIfAbsent(n, xml)
            val compact = EpgMatcher.compact(display)
            if (compact.isNotEmpty()) map.putIfAbsent(compact, xml)
        }
        nameLowerToXmlId.clear()
        nameLowerToXmlId.putAll(map)
    }

    suspend fun now(channel: ChannelEntity, now: Long): EpgEntity? {
        ensureMatched()
        val xmlId = xmlIdForName(channel.name) ?: return null
        val row = db.globalEpgProgrammes().now(xmlId, now) ?: return null
        return row.toEntity(channel)
    }

    suspend fun next(channel: ChannelEntity, now: Long): EpgEntity? {
        ensureMatched()
        val xmlId = xmlIdForName(channel.name) ?: return null
        return db.globalEpgProgrammes().window(xmlId, now, now + 6L * 3_600_000)
            .firstOrNull { it.startMs > now }
            ?.toEntity(channel)
    }

    suspend fun window(channel: ChannelEntity, from: Long, to: Long): List<EpgEntity> {
        ensureMatched()
        val xmlId = xmlIdForName(channel.name) ?: return emptyList()
        return db.globalEpgProgrammes().window(xmlId, from, to).map { it.toEntity(channel) }
    }

    suspend fun nowMany(channels: List<ChannelEntity>, now: Long): List<EpgEntity> {
        if (channels.isEmpty()) return emptyList()
        ensureMatched()
        val byXml = LinkedHashMap<String, ChannelEntity>()
        for (ch in channels) {
            val xmlId = xmlIdForName(ch.name) ?: continue
            byXml.putIfAbsent(xmlId, ch)
        }
        if (byXml.isEmpty()) return emptyList()
        val xmlIds = byXml.keys.toList()
        val rows = ArrayList<EpgEntity>()
        for (chunk in xmlIds.chunked(400)) {
            db.globalEpgProgrammes().nowMany(chunk, now).forEach { p ->
                val ch = byXml[p.xmlChannelId] ?: return@forEach
                rows += p.toEntity(ch)
            }
        }
        return rows
    }

    suspend fun loadForChannels(
        channels: List<ChannelEntity>,
        force: Boolean = false,
        extraCountries: Set<String> = emptySet(),
        langFallback: String = "TR"
    ) = mutex.withLock {
        val fp = fingerprint(channels)
        val now = System.currentTimeMillis()
        if (!force && fp == lastFingerprint && now - lastLoadAtMs < THROTTLE_MS) return@withLock
        runLoad(channels, force, extraCountries, langFallback)
        lastFingerprint = fp
        lastLoadAtMs = System.currentTimeMillis()
    }

    private suspend fun runLoad(
        channels: List<ChannelEntity>,
        force: Boolean,
        extraCountries: Set<String>,
        langFallback: String
    ) {
        val detected = detectCountries(channels) + extraCountries.map { it.uppercase(Locale.US) }
        val countries = if (detected.isEmpty()) {
            setOf(langFallback.uppercase(Locale.US).ifBlank { "TR" })
        } else {
            detected
        }
        val snap = settings.settings.first()
        val hasDb = db.globalEpgChannels().count() > 0
        val ttlMs = if (snap.epgRefreshDays <= 0) 10L * 365 * 86_400_000L else snap.epgRefreshDays * 86_400_000L
        val last = snap.lastGlobalEpgFetchMs
        val due = force || !hasDb || (last > 0L && System.currentTimeMillis() - last > ttlMs)
        if (!due) {
            rematchFromGlobalNames()
            return
        }
        downloadAndMerge(countries.toList())
        settings.setLastGlobalEpgFetch(System.currentTimeMillis())
    }

    private suspend fun downloadAndMerge(countryCodes: List<String>) {
        val semaphore = Semaphore(4)
        val results = coroutineScope {
            countryCodes.flatMap { code ->
                GlobalEpg.getCountryGuideUrls(code).map { url ->
                    async(Dispatchers.IO) {
                        semaphore.withPermit { downloadOne(code, url) }
                    }
                }
            }.awaitAll().filterNotNull()
        }
        if (results.isEmpty()) {
            rematchFromGlobalNames()
            return
        }
        val now = System.currentTimeMillis()
        val from = now - 7L * 24 * 60 * 60 * 1000
        val to = now + 2L * 24 * 60 * 60 * 1000
        for (code in results.map { it.country }.toSet()) {
            db.globalEpgProgrammes().deleteCountry(code)
            db.globalEpgChannels().deleteCountry(code)
        }
        for (pack in results) {
            if (pack.channels.isNotEmpty()) db.globalEpgChannels().upsertAll(pack.channels)
            val windowed = pack.programmes.filter { it.startMs in from..to }
            for (chunk in windowed.chunked(400)) {
                db.globalEpgProgrammes().upsertAll(chunk)
            }
        }
        rematchFromGlobalNames()
    }

    private fun downloadOne(country: String, url: String): CountryPack? {
        return runCatching {
            downloadClient.newCall(Request.Builder().url(url).build()).execute().use { resp ->
                if (!resp.isSuccessful) return@use null
                val stream = resp.body?.byteStream() ?: return@use null
                val doc = XmltvParser.parseStream(stream, gzip = url.endsWith(".gz", true), limit = 500_000)
                val chRows = doc.channels.map {
                    GlobalEpgChannelEntity(country, it.id, it.name, it.logo, url)
                }
                val pRows = doc.programmes.map {
                    GlobalEpgProgrammeEntity(country, it.epgId, it.startMs, it.endMs, it.title, it.plot, url)
                }
                CountryPack(country, chRows, pRows)
            }
        }.getOrNull()
    }

    private data class CountryPack(
        val country: String,
        val channels: List<GlobalEpgChannelEntity>,
        val programmes: List<GlobalEpgProgrammeEntity>
    )

    companion object {
        private const val THROTTLE_MS = 15L * 60_000

        private val countryPatterns = listOf(
            "TR" to Regex("""\b(TR|TURK|TÜRKİYE|TURKEY)\b""", RegexOption.IGNORE_CASE),
            "DE" to Regex("""\b(DE|GERMANY|ALMANYA|DEUTSCH)\b""", RegexOption.IGNORE_CASE),
            "FR" to Regex("""\b(FR|FRANCE|FRENCH)\b""", RegexOption.IGNORE_CASE),
            "US" to Regex("""\b(US|USA|AMERICA|AMERIKA)\b""", RegexOption.IGNORE_CASE),
            "UK" to Regex("""\b(UK|UNITED|KINGDOM|BRITISH)\b""", RegexOption.IGNORE_CASE),
            "IT" to Regex("""\b(IT|ITALY|ITALIA|İTALYA)\b""", RegexOption.IGNORE_CASE),
            "ES" to Regex("""\b(ES|SPAIN|ESPAÑA|İSPANYA)\b""", RegexOption.IGNORE_CASE),
            "NL" to Regex("""\b(NL|NETHERLANDS|HOLLAND)\b""", RegexOption.IGNORE_CASE),
            "BE" to Regex("""\b(BE|BELGIUM|BELGIKA)\b""", RegexOption.IGNORE_CASE),
            "AT" to Regex("""\b(AT|AUSTRIA|AVUSTURYA)\b""", RegexOption.IGNORE_CASE),
            "CH" to Regex("""\b(CH|SWITZERLAND|İSVİÇRE)\b""", RegexOption.IGNORE_CASE),
            "PL" to Regex("""\b(PL|POLAND|POLONYA)\b""", RegexOption.IGNORE_CASE),
            "CZ" to Regex("""\b(CZ|CZECH|ÇEK)\b""", RegexOption.IGNORE_CASE),
            "SK" to Regex("""\b(SK|SLOVAK|SLOVAKYA)\b""", RegexOption.IGNORE_CASE),
            "HU" to Regex("""\b(HU|HUNGARY|MACARISTAN)\b""", RegexOption.IGNORE_CASE),
            "RO" to Regex("""\b(RO|ROMANIA|ROMANYA)\b""", RegexOption.IGNORE_CASE),
            "BG" to Regex("""\b(BG|BULGARIA|BULGARISTAN)\b""", RegexOption.IGNORE_CASE),
            "HR" to Regex("""\b(HR|CROATIA|HIRVATISTAN)\b""", RegexOption.IGNORE_CASE),
            "RS" to Regex("""\b(RS|SERBIA|SIRBISTAN)\b""", RegexOption.IGNORE_CASE),
            "GR" to Regex("""\b(GR|GREECE|YUNANISTAN)\b""", RegexOption.IGNORE_CASE),
            "PT" to Regex("""\b(PT|PORTUGAL|PORTEKİZ)\b""", RegexOption.IGNORE_CASE),
            "SE" to Regex("""\b(SE|SWEDEN|İSVEÇ)\b""", RegexOption.IGNORE_CASE),
            "NO" to Regex("""\b(NO|NORWAY|NORVEÇ)\b""", RegexOption.IGNORE_CASE),
            "DK" to Regex("""\b(DK|DENMARK|DANIMARKA)\b""", RegexOption.IGNORE_CASE),
            "FI" to Regex("""\b(FI|FINLAND|FINLANDİYA)\b""", RegexOption.IGNORE_CASE)
        )

        fun detectCountries(channels: List<ChannelEntity>): Set<String> {
            val out = LinkedHashSet<String>()
            for (ch in channels) {
                val text = ch.name
                for ((code, re) in countryPatterns) {
                    if (re.containsMatchIn(text)) out += code
                }
            }
            return out
        }

        private fun fingerprint(channels: List<ChannelEntity>): Int {
            var hash = channels.size
            val step = (channels.size / 64).coerceAtLeast(1)
            var i = 0
            while (i < channels.size) {
                hash = hash * 31 + channels[i].name.hashCode()
                i += step
            }
            return hash
        }
    }

    private fun GlobalEpgProgrammeEntity.toEntity(ch: ChannelEntity) = EpgEntity(
        id = "g:${xmlChannelId}:$startMs",
        channelId = ch.id,
        epgId = xmlChannelId,
        title = title,
        plot = description,
        startMs = startMs,
        endMs = endMs,
        hasCatchup = ch.hasArchive && endMs < System.currentTimeMillis()
    )
}
