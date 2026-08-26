package com.ray.iptv.data.repo

import android.content.Context
import android.net.Uri
import com.ray.iptv.data.catalog.SeriesNameGrouping
import com.ray.iptv.data.epg.GlobalEpgService
import com.ray.iptv.data.local.CategoryEntity
import com.ray.iptv.data.local.ChannelEntity
import com.ray.iptv.data.local.DownloadEntity
import com.ray.iptv.data.local.EpisodeEntity
import com.ray.iptv.data.local.EpgEntity
import com.ray.iptv.data.local.EpgMatchEntity
import com.ray.iptv.data.local.EpgSourceEntity
import com.ray.iptv.data.local.FavoriteEntity
import com.ray.iptv.data.local.GroupEntity
import com.ray.iptv.data.local.GroupMemberEntity
import com.ray.iptv.data.local.ProgressEntity
import com.ray.iptv.data.local.RayDatabase
import com.ray.iptv.data.local.SourceEntity
import com.ray.iptv.data.local.VodEntity
import com.ray.iptv.data.parser.EpgMatcher
import com.ray.iptv.data.parser.M3uParser
import com.ray.iptv.data.parser.M3uEntry
import com.ray.iptv.data.parser.M3uXtreamSniffer
import com.ray.iptv.data.parser.XmltvParser
import com.ray.iptv.data.parser.XmltvDocument
import com.ray.iptv.data.remote.StalkerClient
import com.ray.iptv.data.remote.XtreamAccountSnapshot
import com.ray.iptv.data.remote.XtreamClient
import com.ray.iptv.data.remote.XtreamSession
import com.ray.iptv.data.remote.int
import com.ray.iptv.data.remote.str
import com.ray.iptv.download.VodDownloadWorker
import com.ray.iptv.net.PlaylistHttp
import com.ray.iptv.player.AndroidPlaybackSocHints
import dagger.hilt.android.qualifiers.ApplicationContext
import androidx.room.withTransaction
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.coroutines.yield
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.OutOfQuotaPolicy
import androidx.work.WorkManager
import androidx.work.workDataOf
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader
import java.nio.charset.CodingErrorAction
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

data class SyncState(
    val running: Boolean = false,
    val message: String = "",
    val error: String = "",
    val catalog: Boolean = false,
    val liveCount: Int = 0,
    val movieCount: Int = 0,
    val seriesCount: Int = 0,
    val liveReady: Boolean = false,
    val moviesReady: Boolean = false,
    val seriesReady: Boolean = false,
    val done: Boolean = false
)
data class XtreamVodDetail(
    val plot: String = "",
    val rating: String = "",
    val genre: String = "",
    val year: String = "",
    val cast: String = "",
    val poster: String = "",
    val trailer: String = ""
)

data class EpgSuggestion(
    val channel: ChannelEntity,
    val epgId: String,
    val score: Int
)
data class EpgStats(
    val channels: Int = 0,
    val programmes: Int = 0,
    val backupChannels: Int = 0,
    val backupProgrammes: Int = 0
)

@Singleton
class CatalogRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val db: RayDatabase,
    private val xtream: XtreamClient,
    private val stalker: StalkerClient,
    private val http: OkHttpClient,
    private val globalEpg: GlobalEpgService
) {
    private val _sync = MutableStateFlow(SyncState())
    val sync: StateFlow<SyncState> = _sync
    private val epgMutex = Mutex()
    @Volatile var lastXtreamAccount: Pair<String, String>? = null
        private set

    fun sources() = db.sources().observe()
    fun categories(sourceId: String, kind: String) = db.categories().observe(sourceId, kind)
    fun allLiveCategories() = db.categories().observeKind("LIVE")
    fun allMovieCategories() = db.categories().observeKind("MOVIE")
    fun allSeriesCategories() = db.categories().observeKind("SERIES")
    fun channels(sourceId: String, categoryId: String) = db.channels().observe(sourceId, categoryId)
    fun channelsPage(sourceId: String, categoryId: String, combine: Boolean, limit: Int) =
        if (combine) db.channels().observePageAll(categoryId, limit)
        else db.channels().observePage(sourceId, categoryId, limit)
    fun liveVisibleCount(sourceId: String, combine: Boolean) =
        if (combine) db.channels().observeVisibleCountAll()
        else db.channels().observeVisibleCount(sourceId)
    fun liveCounts(sourceId: String, combine: Boolean) =
        if (combine) db.channels().observeCountsAll()
        else db.channels().observeCounts(sourceId)
    suspend fun listChannels(sourceId: String, categoryId: String) =
        db.channels().list(sourceId, categoryId)
    suspend fun channelsByIds(ids: List<String>) =
        if (ids.isEmpty()) emptyList() else db.channels().byIds(ids)
    fun vod(sourceId: String, kind: String, categoryId: String) = db.vod().observe(sourceId, kind, categoryId)
    fun vodPage(sourceId: String, kind: String, categoryId: String, combine: Boolean, limit: Int) =
        if (combine) db.vod().observeKindPage(kind, categoryId, limit)
        else db.vod().observePage(sourceId, kind, categoryId, limit)
    fun vodKindCount(sourceId: String, kind: String, combine: Boolean) =
        if (combine) db.vod().observeKindCountAll(kind)
        else db.vod().observeKindCount(sourceId, kind)
    fun vodCounts(sourceId: String, kind: String, combine: Boolean) =
        if (combine) db.vod().observeKindCounts(kind)
        else db.vod().observeCounts(sourceId, kind)
    fun episodes(seriesId: String) = db.episodes().observe(seriesId)
    suspend fun listEpisodes(seriesId: String) = db.episodes().list(seriesId)
    suspend fun upsertEpisodes(items: List<EpisodeEntity>) {
        if (items.isNotEmpty()) db.episodes().upsertAll(items)
    }
    fun favorites(profileId: String) = db.favorites().observe(profileId)
    fun continueWatching(profileId: String) = db.progress().continueWatching(profileId)
    fun recentLive(profileId: String) = db.progress().recentLive(profileId)
    suspend fun progressList(profileId: String) = db.progress().listByProfile(profileId)
    fun downloads(profileId: String) = db.downloads().observe(profileId)
    fun groups(profileId: String) = db.groups().observe(profileId)
    fun epgSources() = db.epgSources().observe()
    fun epgUpcoming(channelId: String) = db.epg().upcoming(channelId, System.currentTimeMillis())

    fun groupChannels(groupId: String) = flow {
        val ids = db.groups().members(groupId)
        emit(if (ids.isEmpty()) emptyList() else db.channels().byIds(ids))
    }

    suspend fun source(id: String) = db.sources().byId(id)
    suspend fun channel(id: String) = db.channels().byId(id)
    suspend fun vodItem(id: String) = db.vod().byId(id)
    suspend fun vodByIds(ids: List<String>) =
        if (ids.isEmpty()) emptyList() else db.vod().byIds(ids.take(400))
    suspend fun vodInCategory(sourceId: String, kind: String, categoryId: String, combine: Boolean) =
        if (combine) db.vod().listByCategoryAll(kind, categoryId)
        else db.vod().listByCategory(sourceId, kind, categoryId)

    suspend fun vodLastAdded(sourceId: String, kind: String, combine: Boolean) =
        if (combine) db.vod().lastAddedAll(kind) else db.vod().lastAdded(sourceId, kind)
    suspend fun vodTopRated(sourceId: String, kind: String, combine: Boolean) =
        if (combine) db.vod().topRatedAll(kind) else db.vod().topRated(sourceId, kind)
    suspend fun vodTrend(sourceId: String, kind: String, combine: Boolean): List<VodEntity> {
        val rated = if (combine) db.vod().trendRatedAll(kind) else db.vod().trendRated(sourceId, kind)
        if (rated.size >= 50) return rated
        val recent = vodLastAdded(sourceId, kind, combine)
        if (rated.isEmpty()) return recent
        val seen = rated.map { it.id }.toHashSet()
        return (rated + recent.filter { it.id !in seen }).take(50)
    }
    suspend fun vodMixed(sourceId: String, kind: String, combine: Boolean) =
        if (combine) db.vod().mixedAll(kind) else db.vod().mixed(sourceId, kind)
    suspend fun liveMixed(sourceId: String, combine: Boolean) =
        if (combine) db.channels().mixedAll() else db.channels().mixed(sourceId)
    suspend fun epgUpcomingRange(now: Long, until: Long, limit: Int = 2000) =
        db.epg().upcomingInRange(now, until, limit)
    suspend fun epgRecentlyEnded(from: Long, now: Long) = db.epg().recentlyEnded(from, now)
    suspend fun deleteProgress(profileId: String, mediaId: String) =
        db.progress().delete(profileId, mediaId)
    suspend fun clearWatchHistory(profileId: String) = db.progress().clear(profileId)
    suspend fun clearAllPlaylists() = withContext(Dispatchers.IO) {
        db.sources().all().forEach { removeSource(it.id) }
    }
    suspend fun deleteDownload(id: String) = db.downloads().delete(id)
    suspend fun episode(id: String) = db.episodes().byId(id)
    suspend fun download(id: String) = db.downloads().byId(id)

    suspend fun addXtream(name: String, base: String, user: String, pass: String, existingId: String? = null): String {
        val trimmed = base.trim()
        val resolved = withContext(Dispatchers.IO) {
            if (PlaylistHttp.isShortUrl(trimmed)) {
                runCatching { PlaylistHttp.resolve(http, trimmed) }.getOrDefault(trimmed)
            } else trimmed
        }
        val session = xtream.authenticateResolved(resolved, user, pass)
        return persistSource(
            existingId,
            "XTREAM",
            name.ifBlank { "Xtream" },
            session.base,
            user,
            pass,
            ""
        )
    }

    /**
     * Mina: M3U URL'sinde username/password varsa Xtream API dener;
     * boş katalog veya hata olursa ham M3U'ya düşer.
     * Kısa URL (TinyURL vb.) önce gerçek hedefe çözülür, sonra sniff edilir.
     */
    suspend fun addM3u(name: String, url: String, existingId: String? = null): String {
        val trimmed = url.trim()
        val resolved = withContext(Dispatchers.IO) {
            if (PlaylistHttp.isShortUrl(trimmed)) {
                runCatching { PlaylistHttp.resolve(http, trimmed) }.getOrDefault(trimmed)
            } else trimmed
        }
        val persistUrl = if (PlaylistHttp.isShortUrl(resolved)) trimmed else resolved
        for (candidate in listOf(resolved, trimmed, persistUrl).distinct()) {
            val sniffed = M3uXtreamSniffer.toXtreamSource(candidate) ?: continue
            val session = runCatching {
                xtream.authenticateResolved(sniffed.baseUrl, sniffed.username, sniffed.password)
            }.getOrNull() ?: continue
            val id = persistSource(
                existingId,
                "XTREAM",
                name.ifBlank { "Xtream" },
                session.base,
                sniffed.username,
                sniffed.password,
                ""
            )
            if (!_sync.value.catalog) beginCatalogSync()
            val loaded = runCatching {
                syncXtream(db.sources().byId(id)!!)
                db.channels().count(id) > 0 || db.vod().count(id) > 0
            }.getOrDefault(false)
            if (loaded) {
                finishCatalogSync()
                return id
            }
            if (existingId == null) removeSource(id)
        }
        return persistSource(existingId, "M3U", name.ifBlank { "M3U" }, persistUrl, "", "", "")
    }

    suspend fun addLocalM3u(name: String, uri: String, existingId: String? = null): String {
        return persistSource(existingId, "M3U_FILE", name.ifBlank { "Yerel M3U" }, uri, "", "", "")
    }

    /** Mina kurulum sihirbazındaki demo liste — sunucu bilgisi gerektirmez. */
    suspend fun addDemoPlaylist(): String {
        val file = withContext(Dispatchers.IO) {
            File(context.filesDir, "ray_demo.m3u").apply { writeText(DEMO_M3U) }
        }
        return persistSource(null, "M3U_FILE", "Demo", Uri.fromFile(file).toString(), "", "", "")
    }

    suspend fun addStalker(name: String, portal: String, mac: String, existingId: String? = null): String {
        val token = stalker.handshake(portal, mac)
        return persistSource(existingId, "STALKER", name.ifBlank { "Stalker" }, portal.trim(), mac, "", token)
    }

    suspend fun renameSource(id: String, name: String) {
        val s = db.sources().byId(id) ?: return
        db.sources().upsert(s.copy(name = name.ifBlank { s.name }))
    }

    suspend fun removeSource(id: String) = withContext(Dispatchers.IO) {
        db.categories().clearSource(id)
        db.channels().clearSource(id)
        db.vod().clearSource(id)
        db.episodes().clearSource(id)
        db.sources().delete(id)
        compactSortOrders()
    }

    suspend fun allSources(): List<SourceEntity> = db.sources().all()

    suspend fun setSourceEnabled(id: String, enabled: Boolean) {
        val all = db.sources().all()
        if (!enabled && all.count { it.enabled } <= 1) {
            error("LAST_ENABLED")
        }
        db.sources().setEnabled(id, enabled)
    }

    suspend fun moveSource(id: String, delta: Int) {
        val all = db.sources().all()
        val i = all.indexOfFirst { it.id == id }
        val j = i + delta
        if (i < 0 || j !in all.indices) return
        val reordered = all.toMutableList()
        val moved = reordered.removeAt(i)
        reordered.add(j, moved)
        reordered.forEachIndexed { index, src ->
            if (src.sortOrder != index) db.sources().setSortOrder(src.id, index)
        }
    }

    suspend fun ensureSourceOrder() {
        val all = db.sources().all()
        if (all.isEmpty()) return
        if (all.map { it.sortOrder }.distinct().size == all.size && all.all { it.sortOrder >= 0 }) return
        compactSortOrders()
    }

    private suspend fun compactSortOrders() {
        db.sources().all().forEachIndexed { index, src ->
            if (src.sortOrder != index) db.sources().setSortOrder(src.id, index)
        }
    }

    private fun m3uReader(raw: String): BufferedReader {
        val uri = Uri.parse(raw)
        val stream = if (uri.scheme == "file" || uri.scheme.isNullOrBlank()) {
            val path = uri.path ?: raw.removePrefix("file://")
            File(path).inputStream()
        } else {
            context.contentResolver.openInputStream(uri)
                ?: error("M3U dosyası açılamadı")
        }
        return utf8Reader(stream)
    }

    private fun utf8Reader(stream: java.io.InputStream): BufferedReader {
        val decoder = StandardCharsets.UTF_8.newDecoder()
            .onMalformedInput(CodingErrorAction.REPLACE)
            .onUnmappableCharacter(CodingErrorAction.REPLACE)
        return BufferedReader(InputStreamReader(stream, decoder), 64 * 1024)
    }

    private suspend fun persistSource(
        existingId: String?,
        kind: String,
        name: String,
        baseUrl: String,
        username: String,
        password: String,
        extra: String
    ): String {
        val old = existingId?.let { db.sources().byId(it) }
        if (old != null) {
            db.categories().clearSource(old.id)
            db.channels().clearSource(old.id)
            db.vod().clearSource(old.id)
            db.episodes().clearSource(old.id)
            db.sources().upsert(
                old.copy(
                    kind = kind,
                    name = name,
                    baseUrl = baseUrl,
                    username = username,
                    password = password,
                    extra = extra
                )
            )
            return old.id
        }
        val all = db.sources().all()
        if (all.size >= MAX_PLAYLIST_SLOTS) error("MAX_PLAYLISTS")
        val id = UUID.randomUUID().toString()
        val order = (all.maxOfOrNull { it.sortOrder } ?: -1) + 1
        db.sources().upsert(
            SourceEntity(id, kind, name, baseUrl, username, password, extra, System.currentTimeMillis(), true, order)
        )
        return id
    }

    companion object {
        const val MAX_PLAYLIST_SLOTS = 32
        private const val DB_CHUNK = 1000
        const val LIVE_WINDOW = 800
        const val VOD_WINDOW = 400
        const val LIVE_WINDOW_TV = 200
        const val VOD_WINDOW_TV = 120
        private const val DEMO_M3U = """#EXTM3U
#EXTINF:-1 tvg-id="1" tvg-name="Demo Live 1" tvg-logo="https://raw.githubusercontent.com/iptv-org/iptv/master/logos/BigBuckBunny.png" group-title="Live TV",Big Buck Bunny (HLS Live)
https://test-streams.mux.dev/x36xhzz/x36xhzz.m3u8
#EXTINF:-1 tvg-id="2" tvg-name="Demo Live 2" tvg-logo="https://raw.githubusercontent.com/iptv-org/iptv/master/logos/TearsOfSteel.png" group-title="Live TV",Tears of Steel 4K (HLS Live)
https://test-streams.mux.dev/tos_crossorigin/tos.m3u8
#EXTINF:-1 tvg-id="3" tvg-name="Sintel Movie" tvg-logo="https://image.tmdb.org/t/p/w500/q6y0Go1tsGEmtSt2MwfnrzHaozV.jpg" group-title="Movies",Sintel (Demo Movie)
https://test-streams.mux.dev/test_001/stream.m3u8
#EXTINF:-1 tvg-id="4" tvg-name="Ocean Life" tvg-logo="https://image.tmdb.org/t/p/w500/8c4a8kE7PjhL0yEV827z6R50xY1.jpg" group-title="Movies",Ocean Life 4K (Demo Movie)
https://test-streams.mux.dev/pts_shift/master.m3u8
#EXTINF:-1 tvg-id="5" tvg-name="Cosmos Series S01E01" tvg-logo="https://image.tmdb.org/t/p/w500/u3bZgnGQ9T01sWNhyve4z0wH0Hl.jpg" group-title="Series",Cosmos Discovery S01 E01
https://test-streams.mux.dev/dai-discontinuity-deltatre/manifest.m3u8
#EXTINF:-1 tvg-id="6" tvg-name="Cosmos Series S01E02" tvg-logo="https://image.tmdb.org/t/p/w500/u3bZgnGQ9T01sWNhyve4z0wH0Hl.jpg" group-title="Series",Cosmos Discovery S01 E02
https://test-streams.mux.dev/dai-discontinuity-deltatre/manifest.m3u8
"""
    }

    fun beginCatalogSync() {
        _sync.value = SyncState(running = true, catalog = true, message = "Bağlanıyor…")
    }

    fun failCatalogSync(message: String) {
        _sync.value = SyncState(
            running = false,
            catalog = true,
            error = message.ifBlank { "Eşitleme başarısız" }
        )
    }

    fun acknowledgeSync() {
        if (!_sync.value.running) _sync.value = SyncState()
    }

    private fun catalogCopy(
        running: Boolean = true,
        message: String? = null,
        liveCount: Int? = null,
        movieCount: Int? = null,
        seriesCount: Int? = null,
        liveReady: Boolean? = null,
        moviesReady: Boolean? = null,
        seriesReady: Boolean? = null
    ) {
        val cur = if (_sync.value.catalog) _sync.value else SyncState(catalog = true)
        _sync.value = cur.copy(
            running = running,
            catalog = true,
            message = message ?: cur.message,
            error = "",
            done = false,
            liveCount = liveCount ?: cur.liveCount,
            movieCount = movieCount ?: cur.movieCount,
            seriesCount = seriesCount ?: cur.seriesCount,
            liveReady = liveReady ?: cur.liveReady,
            moviesReady = moviesReady ?: cur.moviesReady,
            seriesReady = seriesReady ?: cur.seriesReady
        )
    }

    private fun finishCatalogSync() {
        val cur = _sync.value
        _sync.value = cur.copy(
            running = false,
            catalog = true,
            message = "Hazır",
            error = "",
            done = true,
            liveReady = true,
            moviesReady = true,
            seriesReady = true
        )
    }

    suspend fun syncSource(sourceId: String) = withContext(Dispatchers.IO) {
        val source = db.sources().byId(sourceId) ?: return@withContext
        if (!_sync.value.catalog || !_sync.value.running) beginCatalogSync()
        runCatching {
            when (source.kind) {
                "XTREAM" -> syncXtream(source)
                "STALKER" -> syncStalker(source)
                else -> syncM3u(source)
            }
            finishCatalogSync()
        }.onFailure {
            failCatalogSync(it.message ?: "Eşitleme başarısız")
        }
    }

    private fun dbChunk(): Int {
        val hints = AndroidPlaybackSocHints.get(context)
        return if (hints.oneGiBRamClass || hints.playbackChallengedTv) 1000 else 2500
    }

    fun livePageSize(): Int {
        val hints = AndroidPlaybackSocHints.get(context)
        return when {
            hints.oneGiBRamClass -> 120
            hints.androidTv || hints.playbackChallengedTv -> LIVE_WINDOW_TV
            else -> LIVE_WINDOW
        }
    }

    fun vodPageSize(): Int {
        val hints = AndroidPlaybackSocHints.get(context)
        return when {
            hints.oneGiBRamClass -> 80
            hints.androidTv || hints.playbackChallengedTv -> VOD_WINDOW_TV
            else -> VOD_WINDOW
        }
    }

    fun liveBrowseCap(): Int {
        val hints = AndroidPlaybackSocHints.get(context)
        return when {
            hints.oneGiBRamClass -> 800
            hints.androidTv || hints.playbackChallengedTv -> 1600
            else -> 4000
        }
    }

    fun vodBrowseCap(): Int {
        val hints = AndroidPlaybackSocHints.get(context)
        return when {
            hints.oneGiBRamClass -> 400
            hints.androidTv || hints.playbackChallengedTv -> 800
            else -> 2000
        }
    }

    private suspend fun syncXtream(source: SourceEntity) = withContext(Dispatchers.IO) {
        val session = xtream.authenticate(source.baseUrl, source.username, source.password)
        lastXtreamAccount = session.status to session.expires
        val chunk = dbChunk()
        catalogCopy(message = "Canlı kategoriler")
        val liveCats = xtream.categories(session.base, session.username, session.password, "get_live_categories")
        val vodCats = xtream.categories(session.base, session.username, session.password, "get_vod_categories")
        val serCats = xtream.categories(session.base, session.username, session.password, "get_series_categories")
        val catEntities = buildList {
            liveCats.forEachIndexed { i, o -> add(cat(source.id, o.str("category_id"), "LIVE", o.str("category_name"), i)) }
            vodCats.forEachIndexed { i, o -> add(cat(source.id, o.str("category_id"), "MOVIE", o.str("category_name"), i)) }
            serCats.forEachIndexed { i, o -> add(cat(source.id, o.str("category_id"), "SERIES", o.str("category_name"), i)) }
        }
        val mergedCats = mergeCategoryLayout(source.id, catEntities)
        val catName = mergedCats.associate { it.remoteId + "/" + it.kind to it.name }
        val matches = db.epgMatch().all().associate { it.channelId to it.epgId }
        val oldLayout = db.channels().layoutBySource(source.id).associateBy { it.id }

        db.categories().clearSource(source.id)
        db.channels().clearSource(source.id)
        db.vod().clearSource(source.id)
        db.categories().upsertAll(mergedCats)

        catalogCopy(message = "Canlı kanallar")
        val liveN = ingestXtreamLive(source, session, catName, matches, oldLayout, chunk)
        catalogCopy(message = "Filmler", liveCount = liveN, liveReady = true)
        val movieN = ingestXtreamMovies(source, session, catName, chunk)
        catalogCopy(message = "Diziler", movieCount = movieN, moviesReady = true)
        val seriesN = ingestXtreamSeries(source, session, catName, chunk)
        catalogCopy(
            message = "Kaydedildi",
            liveCount = liveN,
            movieCount = movieN,
            seriesCount = seriesN,
            liveReady = true,
            moviesReady = true,
            seriesReady = true
        )
    }

    private suspend fun ingestXtreamLive(
        source: SourceEntity,
        session: XtreamSession,
        catName: Map<String, String>,
        matches: Map<String, String>,
        oldLayout: Map<String, com.ray.iptv.data.local.ChannelLayoutSnap>,
        chunk: Int
    ): Int {
        var n = 0
        val buf = ArrayList<ChannelEntity>(chunk)
        suspend fun flush() {
            if (buf.isEmpty()) return
            db.channels().upsertAll(buf)
            buf.clear()
            catalogCopy(message = "Canlı kanallar", liveCount = n)
            yield()
        }
        xtream.forEachStream(session.base, session.username, session.password, "get_live_streams") { o ->
            val sid = o.str("stream_id")
            if (sid.isBlank()) return@forEachStream
            val ext = o.str("container_extension").ifBlank { "m3u8" }
            val catId = o.str("category_id")
            val id = "${source.id}:live:$sid"
            val old = oldLayout[id]
            buf += ChannelEntity(
                id = id,
                sourceId = source.id,
                remoteId = sid,
                name = o.str("name"),
                number = o.int("num").takeIf { it > 0 } ?: (n + 1),
                logo = o.str("stream_icon"),
                categoryId = "${source.id}:LIVE:$catId",
                categoryName = catName["$catId/LIVE"].orEmpty(),
                epgId = matches[id] ?: o.str("epg_channel_id").ifBlank { o.str("epg_id") }.ifBlank { o.str("custom_sid") },
                hasArchive = o.int("tv_archive") == 1,
                archiveDays = o.int("tv_archive_duration"),
                streamUrl = session.liveUrl(sid, ext),
                userAgent = "",
                referer = "",
                hidden = old?.hidden ?: false,
                layoutSort = old?.layoutSort ?: -1
            )
            n++
            if (buf.size >= chunk) flush()
        }
        flush()
        return n
    }

    private suspend fun ingestXtreamMovies(
        source: SourceEntity,
        session: XtreamSession,
        catName: Map<String, String>,
        chunk: Int
    ): Int {
        var n = 0
        val buf = ArrayList<VodEntity>(chunk)
        suspend fun flush() {
            if (buf.isEmpty()) return
            db.vod().upsertAll(buf)
            buf.clear()
            catalogCopy(message = "Filmler", movieCount = n)
            yield()
        }
        xtream.forEachStream(session.base, session.username, session.password, "get_vod_streams") { o ->
            val sid = o.str("stream_id")
            if (sid.isBlank()) return@forEachStream
            val ext = o.str("container_extension").ifBlank { "mp4" }
            val catId = o.str("category_id")
            buf += VodEntity(
                id = "${source.id}:movie:$sid",
                sourceId = source.id,
                remoteId = sid,
                kind = "MOVIE",
                name = o.str("name"),
                poster = o.str("stream_icon"),
                plot = o.str("plot"),
                year = o.str("year"),
                rating = o.str("rating").ifBlank { o.str("rating_imdb") },
                genre = o.str("genre"),
                categoryId = "${source.id}:MOVIE:$catId",
                categoryName = catName["$catId/MOVIE"].orEmpty(),
                streamUrl = session.vodUrl(sid, ext),
                extension = ext,
                addedUnix = vodAddedEpoch(o)
            )
            n++
            if (buf.size >= chunk) flush()
        }
        flush()
        return n
    }

    private suspend fun ingestXtreamSeries(
        source: SourceEntity,
        session: XtreamSession,
        catName: Map<String, String>,
        chunk: Int
    ): Int {
        var n = 0
        val buf = ArrayList<VodEntity>(chunk)
        suspend fun flush() {
            if (buf.isEmpty()) return
            db.vod().upsertAll(buf)
            buf.clear()
            catalogCopy(message = "Diziler", seriesCount = n)
            yield()
        }
        xtream.forEachStream(session.base, session.username, session.password, "get_series") { o ->
            val sid = o.str("series_id").ifBlank { o.str("stream_id") }
            if (sid.isBlank()) return@forEachStream
            val catId = o.str("category_id")
            buf += VodEntity(
                id = "${source.id}:series:$sid",
                sourceId = source.id,
                remoteId = sid,
                kind = "SERIES",
                name = o.str("name"),
                poster = o.str("cover").ifBlank { o.str("stream_icon") },
                plot = o.str("plot"),
                year = o.str("year"),
                rating = o.str("rating").ifBlank { o.str("rating_imdb") },
                genre = o.str("genre"),
                categoryId = "${source.id}:SERIES:$catId",
                categoryName = catName["$catId/SERIES"].orEmpty(),
                streamUrl = "",
                extension = "",
                addedUnix = vodAddedEpoch(o)
            )
            n++
            if (buf.size >= chunk) flush()
        }
        flush()
        return n
    }

    private suspend fun syncStalker(source: SourceEntity) = withContext(Dispatchers.IO) {
        val mac = source.username
        val token = runCatching { stalker.handshake(source.baseUrl, mac) }.getOrDefault(source.extra)
        db.sources().upsert(source.copy(extra = token))
        val genres = stalker.genres(source.baseUrl, mac, token)
        val cats = mergeCategoryLayout(source.id, genres.mapIndexed { i, g -> cat(source.id, g.id, "LIVE", g.name, i) })
        val names = cats.associate { it.remoteId to it.name }
        val oldLayout = db.channels().layoutBySource(source.id).associateBy { it.id }
        val list = stalker.channels(source.baseUrl, mac, token)
        val chunk = dbChunk()
        db.categories().clearSource(source.id)
        db.channels().clearSource(source.id)
        db.vod().clearSource(source.id)
        db.categories().upsertAll(cats)
        var liveN = 0
        val buf = ArrayList<ChannelEntity>(chunk)
        suspend fun flush() {
            if (buf.isEmpty()) return
            db.channels().upsertAll(buf)
            buf.clear()
            catalogCopy(message = "Kaydediliyor", liveCount = liveN, liveReady = true)
            yield()
        }
        list.forEachIndexed { index, ch ->
            val id = "${source.id}:stk:${ch.id}"
            val old = oldLayout[id]
            buf += ChannelEntity(
                id = id,
                sourceId = source.id,
                remoteId = ch.id,
                name = ch.name,
                number = ch.number.takeIf { it > 0 } ?: (index + 1),
                logo = ch.logo,
                categoryId = "${source.id}:LIVE:${ch.genreId}",
                categoryName = names[ch.genreId].orEmpty(),
                epgId = "",
                hasArchive = ch.archive,
                archiveDays = if (ch.archive) 7 else 0,
                streamUrl = "stalker:${ch.cmd}",
                userAgent = "",
                referer = "",
                hidden = old?.hidden ?: false,
                layoutSort = old?.layoutSort ?: -1
            )
            liveN++
            if (buf.size >= chunk) flush()
        }
        flush()

        var movieN = 0
        runCatching {
            val vGenres = stalker.vodGenres(source.baseUrl, mac, token)
            val vCats = mergeCategoryLayout(source.id, vGenres.mapIndexed { i, g -> cat(source.id, g.id, "MOVIE", g.name, i) })
            val vNames = vCats.associate { it.remoteId to it.name }
            val vList = stalker.vods(source.baseUrl, mac, token)
            db.categories().upsertAll(vCats)
            val vodBuf = ArrayList<VodEntity>(chunk)
            vList.forEach { v ->
                vodBuf += VodEntity(
                    id = "${source.id}:stkv:${v.id}",
                    sourceId = source.id,
                    remoteId = v.id,
                    kind = "MOVIE",
                    name = v.name,
                    poster = v.logo,
                    plot = "",
                    year = "",
                    rating = "",
                    genre = vNames[v.genreId].orEmpty(),
                    categoryId = "${source.id}:MOVIE:${v.genreId}",
                    categoryName = vNames[v.genreId].orEmpty(),
                    streamUrl = "stalker:${v.cmd}",
                    extension = "mp4",
                    addedUnix = System.currentTimeMillis()
                )
                movieN++
                if (vodBuf.size >= chunk) {
                    db.vod().upsertAll(vodBuf)
                    vodBuf.clear()
                    catalogCopy(message = "Filmler Kaydediliyor", liveCount = liveN, movieCount = movieN, liveReady = true)
                    yield()
                }
            }
            if (vodBuf.isNotEmpty()) {
                db.vod().upsertAll(vodBuf)
                vodBuf.clear()
            }
        }

        catalogCopy(
            message = "Kaydedildi",
            liveCount = liveN,
            movieCount = movieN,
            seriesCount = 0,
            liveReady = true,
            moviesReady = true,
            seriesReady = true
        )
    }

    private suspend fun syncM3u(source: SourceEntity) = withContext(Dispatchers.IO) {
        catalogCopy(message = "Liste indiriliyor")
        val oldCats = db.categories().listBySource(source.id).associateBy { it.id }
        val oldLayout = db.channels().layoutBySource(source.id).associateBy { it.id }
        val matches = db.epgMatch().all().associate { it.channelId to it.epgId }

        db.categories().clearSource(source.id)
        db.channels().clearSource(source.id)
        db.vod().clearSource(source.id)

        val liveGroups = LinkedHashMap<String, Int>()
        val movieGroups = LinkedHashMap<String, Int>()
        val seriesGroups = LinkedHashMap<String, Int>()
        val chunk = dbChunk()
        val channelBuf = ArrayList<ChannelEntity>(chunk)
        val vodBuf = ArrayList<VodEntity>(chunk)
        var liveN = 0
        var movieN = 0
        var seriesN = 0

        suspend fun flush() {
            if (channelBuf.isNotEmpty() || vodBuf.isNotEmpty()) {
                db.withTransaction {
                    if (channelBuf.isNotEmpty()) {
                        db.channels().upsertAll(channelBuf)
                        channelBuf.clear()
                    }
                    if (vodBuf.isNotEmpty()) {
                        db.vod().upsertAll(vodBuf)
                        vodBuf.clear()
                    }
                }
            }
            catalogCopy(
                message = "İşleniyor… $liveN canlı · $movieN film · $seriesN dizi",
                liveCount = liveN,
                movieCount = movieN,
                seriesCount = seriesN
            )
            yield()
        }

        fun rememberGroup(map: LinkedHashMap<String, Int>, group: String) {
            if (group !in map) map[group] = map.size
        }

        fun toChannel(e: M3uEntry): ChannelEntity {
            rememberGroup(liveGroups, e.group)
            val stable = m3uStableId(e.url)
            val id = "${source.id}:m3u:$stable"
            val old = oldLayout[id]
            return ChannelEntity(
                id = id,
                sourceId = source.id,
                remoteId = stable,
                name = e.name,
                number = liveN + 1,
                logo = e.logo,
                categoryId = "${source.id}:LIVE:${e.group}",
                categoryName = e.group,
                epgId = matches[id] ?: e.epgId,
                hasArchive = e.catchupDays > 0 || e.catchup.isNotBlank(),
                archiveDays = e.catchupDays,
                streamUrl = e.url,
                userAgent = e.userAgent,
                referer = e.referer,
                hidden = old?.hidden ?: false,
                layoutSort = old?.layoutSort ?: -1
            )
        }

        fun toVod(e: M3uEntry): VodEntity {
            val series = e.kindHint == "SERIES"
            if (series) rememberGroup(seriesGroups, e.group) else rememberGroup(movieGroups, e.group)
            val stable = m3uStableId(e.url)
            val prefix = if (series) "series" else "movie"
            return VodEntity(
                id = "${source.id}:$prefix:$stable",
                sourceId = source.id,
                remoteId = stable,
                kind = e.kindHint,
                name = e.name,
                poster = e.logo,
                plot = e.plot,
                year = "",
                rating = "",
                genre = e.group,
                categoryId = "${source.id}:${e.kindHint}:${e.group}",
                categoryName = e.group,
                streamUrl = e.url,
                extension = e.url.substringAfterLast('.', "mp4").substringBefore('?')
            )
        }

        withContext(Dispatchers.IO) {
            openM3u(source).use { reader ->
                M3uParser.parseReader(reader) { batch ->
                    for (e in batch) {
                        when (e.kindHint) {
                            "MOVIE", "SERIES" -> {
                                if (e.kindHint == "SERIES") seriesN++ else movieN++
                                vodBuf += toVod(e)
                                if (vodBuf.size >= chunk) flush()
                            }
                            else -> {
                                liveN++
                                channelBuf += toChannel(e)
                                if (channelBuf.size >= chunk) flush()
                            }
                        }
                    }
                }
            }
        }
        flush()

        val cats =
            liveGroups.keys.mapIndexed { i, g -> mergeCat(oldCats, cat(source.id, g, "LIVE", g, i)) } +
                movieGroups.keys.mapIndexed { i, g -> mergeCat(oldCats, cat(source.id, g, "MOVIE", g, i)) } +
                seriesGroups.keys.mapIndexed { i, g -> mergeCat(oldCats, cat(source.id, g, "SERIES", g, i)) }
        if (cats.isNotEmpty()) db.categories().upsertAll(cats)

        catalogCopy(
            message = "Kaydedildi",
            liveCount = liveN,
            movieCount = movieN,
            seriesCount = seriesN,
            liveReady = true,
            moviesReady = true,
            seriesReady = true
        )
    }

    private fun mergeCat(old: Map<String, CategoryEntity>, cat: CategoryEntity): CategoryEntity {
        val prev = old[cat.id] ?: return cat
        return cat.copy(
            hidden = prev.hidden,
            pinned = prev.pinned,
            locked = prev.locked,
            sortOrder = prev.sortOrder
        )
    }

    private fun openM3u(source: SourceEntity): BufferedReader {
        return if (source.kind == "M3U_FILE") {
            m3uReader(source.baseUrl)
        } else {
            utf8Reader(PlaylistHttp.openStream(http, source.baseUrl))
        }
    }

    suspend fun resolvePlayUrl(ch: ChannelEntity): String = withContext(Dispatchers.IO) {
        if (!ch.streamUrl.startsWith("stalker:")) return@withContext ch.streamUrl
        val source = db.sources().byId(ch.sourceId) ?: return@withContext ""
        val cmd = ch.streamUrl.removePrefix("stalker:")
        val token = runCatching { stalker.handshake(source.baseUrl, source.username) }.getOrDefault(source.extra)
        stalker.createLink(source.baseUrl, source.username, token, cmd)
    }

    suspend fun loadSeries(sourceId: String, seriesId: String) {
        val vod = db.vod().byId("$sourceId:series:$seriesId")
            ?: db.vod().listKind(sourceId, "SERIES").firstOrNull { it.remoteId == seriesId }
            ?: return
        loadSeriesEpisodes(vod)
    }

    suspend fun loadSeriesEpisodes(series: VodEntity) = withContext(Dispatchers.IO) {
        val source = db.sources().byId(series.sourceId) ?: return@withContext
        val hints = AndroidPlaybackSocHints.get(context)
        val xtream = source.kind.equals("XTREAM", true)
        val weak = hints.oneGiBRamClass || hints.playbackChallengedTv || hints.androidTv
        val pool = if (xtream || weak) {
            listOf(series)
        } else {
            db.vod().listKind(source.id, "SERIES").ifEmpty {
                db.vod().listAllKind("SERIES")
            }
        }
        val title = SeriesNameGrouping.displayTitleFromName(series.name)
        val cluster = SeriesNameGrouping.expandCluster(listOf(series), title, pool)
        val collected = mutableListOf<EpisodeEntity>()

        val withUrl = cluster.filter { it.streamUrl.isNotBlank() }
        if (withUrl.isNotEmpty()) {
            collected += SeriesNameGrouping.m3uEpisodes(series.id, withUrl)
        }

        if (source.kind == "XTREAM") {
            val apiMembers = cluster.filter { it.streamUrl.isBlank() }
                .ifEmpty { if (withUrl.isEmpty()) listOf(series) else emptyList() }
                .distinctBy { it.remoteId }
            for (member in apiMembers) {
                val fetched = fetchXtreamEpisodes(source, member)
                val hint = SeriesNameGrouping.seasonHintFromName(member.name)
                val remapped = if (hint != null && hint > 0 && fetched.isNotEmpty() &&
                    fetched.all { it.season == fetched.first().season }
                ) {
                    fetched.map { it.copy(seriesId = series.id, season = hint) }
                } else {
                    fetched.map { it.copy(seriesId = series.id) }
                }
                collected += remapped
            }
        }

        val episodes = collected
            .groupBy { ep ->
                if (ep.episode > 0) "${ep.season}|${ep.episode}"
                else "${ep.season}|0|${ep.remoteId.ifBlank { ep.id }}"
            }
            .map { (_, opts) ->
                opts.maxWith(
                    compareBy<EpisodeEntity> { if (it.streamUrl.isNotBlank()) 1 else 0 }
                        .thenBy { if (it.plot.isNotBlank()) 1 else 0 }
                        .thenBy { it.name.length }
                )
            }
            .sortedWith(compareBy({ it.season }, { it.episode }, { it.name }))

        db.episodes().clearSeries(series.id)
        if (episodes.isNotEmpty()) db.episodes().upsertAll(episodes)
    }

    private suspend fun fetchXtreamEpisodes(
        source: SourceEntity,
        series: VodEntity
    ): List<EpisodeEntity> {
        val seriesId = series.remoteId
        val info = runCatching {
            xtream.seriesInfo(source.baseUrl, source.username, source.password, seriesId)
        }.getOrNull() ?: return emptyList()
        val episodes = mutableListOf<EpisodeEntity>()
        val seasons = info["episodes"]
        val seasonEntries: List<Pair<String, kotlinx.serialization.json.JsonArray>> = when (seasons) {
            is kotlinx.serialization.json.JsonObject -> seasons.map { (k, v) ->
                k to when (v) {
                    is kotlinx.serialization.json.JsonArray -> v
                    is kotlinx.serialization.json.JsonObject -> kotlinx.serialization.json.JsonArray(v.values.toList())
                    else -> kotlinx.serialization.json.JsonArray(emptyList())
                }
            }
            is kotlinx.serialization.json.JsonArray -> listOf("1" to seasons)
            else -> emptyList()
        }
        seasonEntries.forEach { (seasonKey, items) ->
            items.forEach { el ->
                val o = runCatching { el.jsonObject }.getOrNull() ?: return@forEach
                val eid = o.str("id")
                val ext = o.str("container_extension").ifBlank { "mp4" }
                episodes += EpisodeEntity(
                    id = "${source.id}:ep:$eid",
                    seriesId = series.id,
                    sourceId = source.id,
                    remoteId = eid,
                    season = o.int("season").takeIf { it > 0 } ?: seasonKey.toIntOrNull() ?: 1,
                    episode = o.int("episode_num"),
                    name = o.str("title").ifBlank { "Episode ${o.int("episode_num")}" },
                    plot = o.str("plot"),
                    still = o.str("movie_image"),
                    streamUrl = "${XtreamClient.normalize(source.baseUrl)}/series/${source.username}/${source.password}/$eid.$ext",
                    extension = ext
                )
            }
        }
        return episodes
    }

    /** Mina `getXtreamAccountSnapshot` — ayarlar > hesap bilgileri. */
    suspend fun fetchXtreamAccount(source: SourceEntity): XtreamAccountSnapshot? {
        if (!source.kind.equals("XTREAM", true)) return null
        if (source.baseUrl.isBlank() || source.username.isBlank() || source.password.isBlank()) return null
        return xtream.fetchAccountSnapshot(source.baseUrl, source.username, source.password)
    }

    /** Mina `get_vod_info` / `get_series_info` — liste satırındaki kısa plot yerine tam özet. */
    suspend fun fetchXtreamVodDetail(item: VodEntity): XtreamVodDetail = withContext(Dispatchers.IO) {
        val source = db.sources().byId(item.sourceId) ?: return@withContext XtreamVodDetail()
        if (!source.kind.equals("XTREAM", true) || item.remoteId.isBlank()) return@withContext XtreamVodDetail()
        val root = runCatching {
            if (item.kind.equals("SERIES", true)) {
                xtream.seriesInfo(source.baseUrl, source.username, source.password, item.remoteId)
            } else {
                xtream.vodInfo(source.baseUrl, source.username, source.password, item.remoteId)
            }
        }.getOrNull() ?: return@withContext XtreamVodDetail()
        parseXtreamVodDetail(root)
    }

    suspend fun importXmltv(sourceId: String) {
        val source = db.sources().byId(sourceId) ?: return
        if (source.kind != "XTREAM") return
        _sync.value = SyncState(true, "TV rehberi")
        runCatching { applyXtreamSourceEpg(source) }
            .onFailure { _sync.value = SyncState(false, "Rehber hatası", error = it.message.orEmpty()) }
            .onSuccess { _sync.value = SyncState(false, "Rehber hazır") }
    }

    suspend fun epgStats(): EpgStats {
        val backup = globalEpg.stats()
        return EpgStats(
            channels = db.epg().distinctChannelCount(),
            programmes = db.epg().programmeCount(),
            backupChannels = backup.first,
            backupProgrammes = backup.second
        )
    }

    suspend fun addEpgSource(name: String, url: String) {
        db.epgSources().upsert(EpgSourceEntity(UUID.randomUUID().toString(), name.ifBlank { "XMLTV" }, url, true))
    }

    suspend fun removeEpgSource(id: String) = db.epgSources().delete(id)

    suspend fun importAllXmltv(
        includeFiles: Boolean = true,
        includeXtream: Boolean = true,
        includeGlobal: Boolean = true,
        forceGlobal: Boolean = false,
        langFallback: String = "TR"
    ) = epgMutex.withLock {
        try {
            _sync.value = SyncState(true, "XMLTV")
            val channels = db.channels().all()
            if (includeFiles) {
                db.epgSources().enabled().forEach { src ->
                    runCatching {
                        val doc = withContext(Dispatchers.IO) { downloadXmlDoc(src.url) }
                        applyXmltvDoc(doc, channels, prune = false)
                    }
                }
            }
            if (includeXtream) {
                db.sources().all().filter { it.kind.equals("XTREAM", true) }.forEach { s ->
                    runCatching {
                        _sync.value = SyncState(true, "Xtream EPG")
                        applyXtreamSourceEpg(s)
                    }
                }
            }
            if (includeGlobal) {
                _sync.value = SyncState(true, "EPGShare01 yedek")
                runCatching {
                    val probe = if (channels.size > 500) channels.take(500) else channels
                    globalEpg.loadForChannels(probe, force = forceGlobal, langFallback = langFallback)
                }
            }
            _sync.value = SyncState(false, "Rehber hazır")
        } catch (t: Throwable) {
            _sync.value = SyncState(false, "Rehber hatası", error = t.message.orEmpty())
            throw t
        }
    }

    suspend fun refreshBackupEpg(force: Boolean = false, langFallback: String = "TR", extraCountries: Set<String> = emptySet()) {
        globalEpg.loadForChannels(
            db.channels().all(),
            force = force,
            extraCountries = extraCountries,
            langFallback = langFallback
        )
    }

    private suspend fun applyXtreamSourceEpg(source: SourceEntity) {
        val live = db.channels().list(source.id, "")
        val epgCap = if (AndroidPlaybackSocHints.get(context).oneGiBRamClass) 8L * 1024 * 1024 else 64L * 1024 * 1024
        runCatching {
            applyXtreamApiEpg(source.id, xtream.getAllLiveEpg(source.baseUrl, source.username, source.password, maxBytes = epgCap), live)
        }
        runCatching {
            val doc = xtream.readXmltv(source.baseUrl, source.username, source.password) { stream ->
                XmltvParser.parseStream(stream, limit = 500_000)
            }
            applyXmltvDoc(doc, live, prune = false)
        }
    }

    private suspend fun applyXtreamApiEpg(
        sourceId: String,
        map: Map<String, List<com.ray.iptv.data.epg.XtreamEpgProgramme>>,
        channels: List<ChannelEntity> = emptyList()
    ) {
        if (map.isEmpty()) return
        val list = channels.ifEmpty { db.channels().list(sourceId, "") }
        val matches = db.epgMatch().all().associate { it.channelId to it.epgId }
        val byEpg = HashMap<String, ChannelEntity>(list.size * 2)
        val byEpgLower = HashMap<String, ChannelEntity>(list.size * 2)
        val byRemote = HashMap<String, ChannelEntity>(list.size * 2)
        val byCompact = HashMap<String, ChannelEntity>(list.size * 2)
        val byNorm = HashMap<String, ChannelEntity>(list.size * 2)
        for (ch in list) {
            val epg = (matches[ch.id] ?: ch.epgId).trim()
            if (epg.isNotEmpty()) {
                byEpg.putIfAbsent(epg, ch)
                byEpgLower.putIfAbsent(epg.lowercase(), ch)
            }
            val remote = ch.remoteId.trim()
            if (remote.isNotEmpty()) byRemote.putIfAbsent(remote, ch)
            remote.toLongOrNull()?.toString()?.let { byRemote.putIfAbsent(it, ch) }
            val compact = EpgMatcher.compact(ch.name)
            val norm = EpgMatcher.normalize(ch.name)
            if (compact.isNotEmpty()) byCompact.putIfAbsent(compact, ch)
            if (norm.isNotEmpty()) byNorm.putIfAbsent(norm, ch)
        }
        val now = System.currentTimeMillis()
        val from = now - 7L * 24 * 60 * 60 * 1000
        val to = now + 2L * 24 * 60 * 60 * 1000
        val rows = ArrayList<EpgEntity>()
        for ((sid, programmes) in map) {
            val ch = matchXmltvChannel(sid, null, byEpg, byEpgLower, byRemote, byCompact, byNorm) ?: continue
            for (p in programmes) {
                if (!epgOverlaps(p.startMs, p.endMs, from, to)) continue
                rows += EpgEntity(
                    id = "${ch.id}:${p.startMs}",
                    channelId = ch.id,
                    epgId = sid,
                    title = p.title,
                    plot = p.plot,
                    startMs = p.startMs,
                    endMs = p.endMs,
                    hasCatchup = ch.hasArchive && p.endMs < now
                )
            }
        }
        upsertEpg(rows)
    }

    private suspend fun applyXmltvDoc(doc: XmltvDocument, channels: List<ChannelEntity>, prune: Boolean = true) {
        if (doc.programmes.isEmpty() && doc.channels.isEmpty()) return
        val matches = db.epgMatch().all().associate { it.channelId to it.epgId }
        val byEpg = HashMap<String, ChannelEntity>()
        val byEpgLower = HashMap<String, ChannelEntity>()
        val byRemote = HashMap<String, ChannelEntity>()
        val byCompact = HashMap<String, ChannelEntity>()
        val byNorm = HashMap<String, ChannelEntity>()
        for (ch in channels) {
            val epg = (matches[ch.id] ?: ch.epgId).trim()
            if (epg.isNotEmpty()) {
                byEpg.putIfAbsent(epg, ch)
                byEpgLower.putIfAbsent(epg.lowercase(), ch)
            }
            val remote = ch.remoteId.trim()
            if (remote.isNotEmpty()) byRemote.putIfAbsent(remote, ch)
            remote.toLongOrNull()?.toString()?.let { byRemote.putIfAbsent(it, ch) }
            val compact = EpgMatcher.compact(ch.name)
            val norm = EpgMatcher.normalize(ch.name)
            if (compact.isNotEmpty()) byCompact.putIfAbsent(compact, ch)
            if (norm.isNotEmpty()) byNorm.putIfAbsent(norm, ch)
        }
        val xmltvNames = HashMap<String, String>(doc.channels.size)
        for (c in doc.channels) {
            if (c.id.isNotBlank()) xmltvNames[c.id] = c.name
        }
        val now = System.currentTimeMillis()
        val from = now - 7L * 24 * 60 * 60 * 1000
        val to = now + 2L * 24 * 60 * 60 * 1000
        val rows = ArrayList<EpgEntity>(minOf(doc.programmes.size, 16_384))
        for (p in doc.programmes) {
            if (!epgOverlaps(p.startMs, p.endMs, from, to)) continue
            val ch = matchXmltvChannel(p.epgId, xmltvNames[p.epgId], byEpg, byEpgLower, byRemote, byCompact, byNorm)
                ?: continue
            rows += EpgEntity(
                id = "${ch.id}:${p.startMs}",
                channelId = ch.id,
                epgId = p.epgId,
                title = p.title,
                plot = p.plot,
                startMs = p.startMs,
                endMs = p.endMs,
                hasCatchup = ch.hasArchive && p.endMs < now
            )
        }
        if (prune) db.epg().prune(from)
        upsertEpg(rows)
    }

    private fun matchXmltvChannel(
        epgId: String,
        displayName: String?,
        byEpg: Map<String, ChannelEntity>,
        byEpgLower: Map<String, ChannelEntity>,
        byRemote: Map<String, ChannelEntity>,
        byCompact: Map<String, ChannelEntity>,
        byNorm: Map<String, ChannelEntity>
    ): ChannelEntity? {
        if (epgId.isBlank()) return null
        byEpg[epgId]?.let { return it }
        byEpgLower[epgId.lowercase()]?.let { return it }
        val remote = epgId.trim().toLongOrNull()?.toString() ?: epgId.trim()
        byRemote[remote]?.let { return it }
        val names = listOfNotNull(displayName?.takeIf { it.isNotBlank() }, epgId)
        for (n in names) {
            val compact = EpgMatcher.compact(n)
            if (compact.isNotEmpty()) byCompact[compact]?.let { return it }
            val norm = EpgMatcher.normalize(n)
            if (norm.isNotEmpty()) byNorm[norm]?.let { return it }
        }
        // Fallback partial prefix matching (bounded byCompact size to prevent CPU overload)
        if (byCompact.size <= 300) {
            for (n in names) {
                val compact = EpgMatcher.compact(n)
                if (compact.length >= 4) {
                    for ((cKey, ch) in byCompact) {
                        if (cKey.length >= 4 && (cKey.startsWith(compact) || compact.startsWith(cKey))) {
                            return ch
                        }
                    }
                }
            }
        }
        return null
    }

    private fun epgOverlaps(start: Long, end: Long, from: Long, to: Long): Boolean =
        start > 0L && end > start && end > from && start < to

    private suspend fun upsertEpg(rows: List<EpgEntity>) {
        if (rows.isEmpty()) return
        for (chunk in rows.chunked(400)) db.epg().upsertAll(chunk)
    }

    suspend fun suggestEpgMatches(): List<EpgSuggestion> {
        val channels = db.channels().all()
        val dbEpgIds = db.epg().allEpgIds().filter { it.isNotBlank() }
        val dbChanIds = db.epg().allChannelIds().filter { it.isNotBlank() }
        val candidateMap = HashMap<String, String>()
        for (epgId in dbEpgIds) {
            val c = EpgMatcher.compact(epgId)
            if (c.isNotEmpty()) candidateMap.putIfAbsent(c, epgId)
        }
        for (chId in dbChanIds) {
            val c = EpgMatcher.compact(chId)
            if (c.isNotEmpty()) candidateMap.putIfAbsent(c, chId)
        }
        for (ch in channels) {
            if (ch.epgId.isNotBlank()) {
                val c = EpgMatcher.compact(ch.epgId)
                if (c.isNotEmpty()) candidateMap.putIfAbsent(c, ch.epgId)
            }
        }

        val suggestions = ArrayList<EpgSuggestion>()
        for (ch in channels) {
            val comp = EpgMatcher.compact(ch.name)
            if (comp.isEmpty()) continue

            var hitId = candidateMap[comp]
            if (hitId == null) {
                for ((candComp, candId) in candidateMap) {
                    if (candComp.length >= 3 && comp.length >= 3) {
                        if (candComp.contains(comp) || comp.contains(candComp)) {
                            hitId = candId
                            break
                        }
                    }
                }
            }
            if (hitId == null) {
                hitId = globalEpg.findEpgIdForName(ch.name)
            }
            if (hitId != null) {
                suggestions += EpgSuggestion(ch, hitId, 100)
            }
        }
        return suggestions
    }

    suspend fun applyEpgMatch(channelId: String, epgId: String) {
        db.epgMatch().upsert(EpgMatchEntity(channelId, epgId))
        db.channels().setEpgId(channelId, epgId)
    }

    suspend fun nowEpg(channelId: String, now: Long = System.currentTimeMillis()): EpgEntity? {
        db.epg().now(channelId, now)?.let { return it }
        val ch = db.channels().byId(channelId) ?: return null
        return globalEpg.now(ch, now)
    }
    suspend fun nowEpgMany(ids: List<String>, now: Long): List<EpgEntity> {
        if (ids.isEmpty()) return emptyList()
        val primary = db.epg().nowMany(ids, now)
        if (primary.size >= ids.size) return primary
        val have = primary.map { it.channelId }.toHashSet()
        val missing = ids.filter { it !in have }
        if (missing.isEmpty()) return primary
        val extras = globalEpg.nowMany(db.channels().byIds(missing), now)
        return if (extras.isEmpty()) primary else primary + extras
    }
    suspend fun nextEpg(channelId: String, now: Long = System.currentTimeMillis()): EpgEntity? {
        db.epg().window(channelId, now, now + 6L * 3600_000).firstOrNull { it.startMs > now }?.let { return it }
        val ch = db.channels().byId(channelId) ?: return null
        return globalEpg.next(ch, now)
    }
    suspend fun epgWindow(channelId: String, from: Long, to: Long): List<EpgEntity> {
        val primary = db.epg().window(channelId, from, to)
        if (primary.isNotEmpty()) return primary
        val ch = db.channels().byId(channelId) ?: return emptyList()
        return globalEpg.window(ch, from, to)
    }

    suspend fun search(sourceId: String, q: String): Triple<List<ChannelEntity>, List<VodEntity>, List<VodEntity>> =
        withContext(Dispatchers.IO) {
        if (q.isBlank()) return@withContext Triple(emptyList(), emptyList(), emptyList())
        val live = if (sourceId.isBlank()) db.channels().searchAll(q)
        else db.channels().search(sourceId, q)
        val allVod = if (sourceId.isBlank()) db.vod().searchAll(q)
        else db.vod().search(sourceId, q)
        Triple(
            live,
            allVod.filter { it.kind == "MOVIE" },
            SeriesNameGrouping.collapseForBrowse(allVod.filter { it.kind == "SERIES" })
        )
    }

    suspend fun setFavorite(profileId: String, mediaId: String, kind: String, on: Boolean) {
        if (on) db.favorites().upsert(FavoriteEntity(profileId, mediaId, kind, System.currentTimeMillis()))
        else db.favorites().delete(profileId, mediaId)
    }

    fun isFavorite(profileId: String, mediaId: String) = db.favorites().isFavorite(profileId, mediaId)

    suspend fun saveProgress(
        profileId: String,
        mediaId: String,
        kind: String,
        title: String,
        poster: String,
        position: Long,
        duration: Long
    ) {
        db.progress().upsert(
            ProgressEntity(profileId, mediaId, kind, title, poster, position, duration, System.currentTimeMillis())
        )
    }

    suspend fun pinCategory(id: String, pinned: Boolean) = db.categories().setPinned(id, pinned)
    suspend fun hideCategory(id: String, hidden: Boolean) = db.categories().setHidden(id, hidden)
    suspend fun lockCategory(id: String, locked: Boolean) = db.categories().setLocked(id, locked)

    suspend fun saveCategoryLayout(items: List<CategoryEntity>) {
        items.groupBy { it.kind }.forEach { (_, group) ->
            group.forEachIndexed { index, cat ->
                db.categories().setHidden(cat.id, cat.hidden)
                db.categories().setSortOrder(cat.id, index)
            }
        }
    }

    suspend fun saveLiveChannelLayout(hiddenIds: Set<String>, orderByCategory: Map<String, List<String>>) =
        withContext(Dispatchers.IO) {
        hiddenIds.forEach { db.channels().setHidden(it, true) }
        orderByCategory.forEach { (_, ids) ->
            ids.forEachIndexed { index, id ->
                db.channels().setHidden(id, false)
                db.channels().setLayoutSort(id, index)
            }
        }
    }

    private suspend fun replaceCatalogKeepingLayout(
        sourceId: String,
        categories: List<CategoryEntity>,
        channels: List<ChannelEntity>,
        vod: List<VodEntity>
    ) {
        val mergedCats = mergeCategoryLayout(sourceId, categories)
        val oldLayout = db.channels().layoutBySource(sourceId).associateBy { it.id }
        val mergedChannels = channels.map { ch ->
            val old = oldLayout[ch.id] ?: return@map ch
            ch.copy(hidden = old.hidden, layoutSort = old.layoutSort)
        }
        db.replaceCatalog(sourceId, mergedCats, mergedChannels, vod)
    }

    private suspend fun mergeCategoryLayout(
        sourceId: String,
        categories: List<CategoryEntity>
    ): List<CategoryEntity> {
        val oldCats = db.categories().listBySource(sourceId).associateBy { it.id }
        return categories.map { cat ->
            val old = oldCats[cat.id] ?: return@map cat
            cat.copy(
                hidden = old.hidden,
                pinned = old.pinned,
                locked = old.locked,
                sortOrder = old.sortOrder
            )
        }
    }

    suspend fun channelByNumber(sourceId: String, number: Int): ChannelEntity? {
        if (sourceId.isBlank()) return db.channels().byNumberAny(number)
        return db.channels().byNumber(sourceId, number)
    }

    fun timeshift(source: SourceEntity, streamId: String, start: String, minutes: Int): String =
        xtream.timeshiftUrl(source.baseUrl, source.username, source.password, streamId, start, minutes)

    suspend fun createGroup(profileId: String, name: String): String {
        val id = UUID.randomUUID().toString()
        db.groups().upsert(GroupEntity(id, profileId, name.ifBlank { "Grup" }))
        return id
    }

    suspend fun deleteGroup(id: String) = db.groups().delete(id)

    suspend fun addToGroup(groupId: String, channelId: String) {
        db.groups().addMember(GroupMemberEntity(groupId, channelId))
    }

    suspend fun isFavoriteNow(profileId: String, mediaId: String) = db.favorites().exists(profileId, mediaId)
    suspend fun progressOf(profileId: String, mediaId: String) = db.progress().byId(profileId, mediaId)

    suspend fun findContentByActor(actorName: String, titles: List<String>): List<VodEntity> = withContext(Dispatchers.IO) {
        val list = ArrayList<VodEntity>()
        if (actorName.isNotBlank()) {
            list.addAll(db.vod().searchAll("%$actorName%"))
        }
        titles.take(15).forEach { title ->
            if (title.length >= 3) {
                val matches = db.vod().searchAll("%$title%")
                matches.forEach { v ->
                    if (list.none { it.id == v.id }) list.add(v)
                }
            }
        }
        list.take(20)
    }

    suspend fun queueDownload(profileId: String, item: VodEntity) {
        val id = UUID.randomUUID().toString()
        db.downloads().upsert(
            DownloadEntity(id, profileId, item.id, item.name, item.poster, item.streamUrl, "", 0, 0, "QUEUED", System.currentTimeMillis())
        )
        val req = OneTimeWorkRequestBuilder<VodDownloadWorker>()
            .setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
            .setInputData(
                workDataOf(
                    VodDownloadWorker.KEY_ID to id,
                    VodDownloadWorker.KEY_URL to item.streamUrl,
                    VodDownloadWorker.KEY_TITLE to item.name,
                    VodDownloadWorker.KEY_POSTER to item.poster,
                    VodDownloadWorker.KEY_PROFILE to profileId,
                    VodDownloadWorker.KEY_MEDIA to item.id
                )
            )
            .build()
        WorkManager.getInstance(context).enqueueUniqueWork("dl-$id", ExistingWorkPolicy.KEEP, req)
    }

    suspend fun queueDownload(profileId: String, ep: EpisodeEntity, poster: String) {
        val id = UUID.randomUUID().toString()
        val title = "S${ep.season}E${ep.episode}  ${ep.name}"
        db.downloads().upsert(
            DownloadEntity(id, profileId, ep.id, title, poster, ep.streamUrl, "", 0, 0, "QUEUED", System.currentTimeMillis())
        )
        val req = OneTimeWorkRequestBuilder<VodDownloadWorker>()
            .setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
            .setInputData(
                workDataOf(
                    VodDownloadWorker.KEY_ID to id,
                    VodDownloadWorker.KEY_URL to ep.streamUrl,
                    VodDownloadWorker.KEY_TITLE to title,
                    VodDownloadWorker.KEY_POSTER to poster,
                    VodDownloadWorker.KEY_PROFILE to profileId,
                    VodDownloadWorker.KEY_MEDIA to ep.id
                )
            )
            .build()
        WorkManager.getInstance(context).enqueueUniqueWork("dl-$id", ExistingWorkPolicy.KEEP, req)
    }

    private fun downloadXmlDoc(url: String): XmltvDocument {
        http.newCall(Request.Builder().url(url).build()).execute().use { resp ->
            if (!resp.isSuccessful) return XmltvDocument(emptyList(), emptyList())
            val stream = resp.body?.byteStream() ?: return XmltvDocument(emptyList(), emptyList())
            return XmltvParser.parseStream(stream, gzip = url.endsWith(".gz", true), limit = 500_000)
        }
    }

    suspend fun nextEpisode(current: EpisodeEntity): EpisodeEntity? {
        val list = db.episodes().list(current.seriesId)
        val idx = list.indexOfFirst { it.id == current.id }
        return list.getOrNull(idx + 1)
    }

    private fun parseXtreamVodDetail(root: JsonObject): XtreamVodDetail {
        fun scanPlot(o: JsonObject): String {
            for (k in listOf("plot", "description", "review", "desc", "overview", "synopsis", "storyline", "story")) {
                val v = runCatching { o.str(k).trim() }.getOrDefault("")
                if (v.isNotBlank() && !v.equals("N/A", true)) return v
            }
            for (nest in listOf("info", "movie_data", "movie", "data", "series")) {
                val child = o[nest]
                if (child is JsonObject) {
                    val p = scanPlot(child)
                    if (p.isNotBlank()) return p
                }
            }
            return ""
        }
        fun scanField(o: JsonObject, keys: List<String>): String {
            for (k in keys) {
                val v = runCatching { o.str(k).trim() }.getOrDefault("")
                if (v.isNotBlank() && !v.equals("N/A", true) && v != "0" && v != "0.0") {
                    return v.substringBefore('/').trim()
                }
            }
            for (nest in listOf("info", "movie_data", "movie", "data")) {
                val child = o[nest]
                if (child is JsonObject) {
                    val v = scanField(child, keys)
                    if (v.isNotBlank()) return v
                }
            }
            return ""
        }
        val trailerRaw = scanField(root, listOf("youtube_trailer", "trailer_url", "trailer"))
        val trailer = when {
            trailerRaw.startsWith("http", true) -> trailerRaw
            trailerRaw.matches(Regex("""^[a-zA-Z0-9_-]{8,}$""")) -> "https://www.youtube.com/watch?v=$trailerRaw"
            else -> ""
        }
        return XtreamVodDetail(
            plot = scanPlot(root),
            rating = scanField(root, listOf("rating_imdb", "imdb_rating", "rating", "imdb")),
            genre = scanField(root, listOf("genre", "genres")),
            year = scanField(root, listOf("releasedate", "releaseDate", "year")).take(4),
            cast = scanField(root, listOf("cast", "actors", "stars")),
            poster = scanField(root, listOf("movie_image", "cover_big", "cover", "stream_icon")),
            trailer = trailer
        )
    }

    private fun vodAddedEpoch(o: kotlinx.serialization.json.JsonObject): Long {
        for (key in listOf("added", "last_modified", "date_added")) {
            val v = o.str(key).trim().toLongOrNull() ?: continue
            if (v <= 0L) continue
            return if (v > 200_000_000_000L) v / 1000L else v
        }
        return 0L
    }

    private val md5ThreadLocal = ThreadLocal.withInitial { MessageDigest.getInstance("MD5") }
    private val hexArray = "0123456789abcdef".toCharArray()

    private fun m3uStableId(url: String): String {
        val md = md5ThreadLocal.get() ?: MessageDigest.getInstance("MD5")
        md.reset()
        val bytes = md.digest(url.toByteArray(StandardCharsets.UTF_8))
        val hexChars = CharArray(16)
        for (i in 0 until 8) {
            val v = bytes[i].toInt() and 0xFF
            hexChars[i * 2] = hexArray[v ushr 4]
            hexChars[i * 2 + 1] = hexArray[v and 0x0F]
        }
        return String(hexChars)
    }

    private fun cat(sourceId: String, remoteId: String, kind: String, name: String, order: Int) =
        CategoryEntity(
            id = "$sourceId:$kind:$remoteId",
            sourceId = sourceId,
            remoteId = remoteId,
            kind = kind,
            name = name.ifBlank { kind },
            sortOrder = order,
            locked = Parental.isAdult(name)
        )
}

object Parental {
    private val ageMark = Regex(
        """(?:\+|plus)?\s*1[89]\s*\+|\+\s*1[89]|21\s*\+|r\s*-?\s*18""",
        RegexOption.IGNORE_CASE
    )
    private val tokens = listOf(
        "🔞", "xxx", "+18", "18+", "(18+)", "(+18)", "+21", "21+",
        "r18", "r-18", "x-rated", "x rated", "xrated", "nsfw", "uncensored",
        "adult", "adults", "adulto", "adultos", "adult only", "adults only",
        "for adults", "yetişkin", "yetiskin", "erotik", "erotic", "erotica",
        "erotique", "porno", "porn", "pornhub", "seks", "seksi", "cinsel",
        "müstehcen", "mustehcen", "hentai", "softcore", "hardcore", "fetish",
        "playboy", "playmate", "brazzers", "hustler", "penthouse", "dorcel",
        "bangbros", "vivid tv", "spice tv", "red light", "redlight", "pinkx",
        "naughty america", "babestation", "onlyfans", "chaturbate", "bongacams",
        "stripchat", "livejasmin", "fake taxi", "reality kings", "evil angel",
        "digital playground", "teamskeet", "metart", "sexart"
    )

    fun isAdult(name: String?): Boolean {
        if (name.isNullOrBlank()) return false
        val s = name.lowercase()
        if (ageMark.containsMatchIn(s)) return true
        return tokens.any { s.contains(it) }
    }

    fun isAnyAdult(vararg texts: String?): Boolean = texts.any { isAdult(it) }
}
