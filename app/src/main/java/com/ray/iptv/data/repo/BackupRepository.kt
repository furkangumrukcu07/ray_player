package com.ray.iptv.data.repo

import android.content.Context
import android.net.Uri
import com.ray.iptv.data.local.EpgSourceEntity
import com.ray.iptv.data.local.FavoriteEntity
import com.ray.iptv.data.local.ProfileEntity
import com.ray.iptv.data.local.ProgressEntity
import com.ray.iptv.data.local.RayDatabase
import com.ray.iptv.data.local.SourceEntity
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.first
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

@Serializable
data class BackupFile(
    val version: Int = 2,
    val settings: BackupSettings = BackupSettings(),
    val profiles: List<BackupProfile> = emptyList(),
    val sources: List<BackupSource> = emptyList(),
    val epgSources: List<BackupEpg> = emptyList(),
    val favorites: List<BackupFavorite> = emptyList(),
    val progress: List<BackupProgress> = emptyList()
)

@Serializable
data class BackupSettings(

    val startup: String = "LIVE",
    val glass: String = "DARK",
    val hideAdult: Boolean = true,
    val hideLocked: Boolean = true,
    val previewLive: Boolean = true,
    val autoplayNext: Boolean = true,
    val catchupTz: Boolean = true,
    val lang: String = "TR",
    val aspect: String = "STRETCH",
    val speed: String = "1.0",
    val combineM3u: Boolean = false,
    val layoutMode: String = "TV",
    val appFontKey: String = "sony",
    val liveEngine: String = "BETTER",
    val vodPlaybackEngine: String = "MEDIA_KIT",
    val lowEndMode: Boolean = false,
    val adaptiveHaptics: Boolean = true,
    val epgEnabled: Boolean = true,
    val epgSourceMode: String = "AUTO",
    val launchOnBoot: Boolean = false,
    val stripChannelPrefix: Boolean = false,
    val autoBackupInterval: String = "DAILY",
    val osdHideSeconds: Int = 7,
    val osdOpacity: Int = 70,
    val liveBufferSeconds: Int = 0,
    val streamFormat: String = "AUTO",
    val userAgentPreset: String = "DEFAULT",
    val customUserAgent: String = "",
    val ignoreSsl: Boolean = true,
    val softwareDecoder: Boolean = false,
    val smartPlayerSelection: Boolean = false,
    val mediaKitLowPowerHwdec: Boolean = true,
    val smartPlayerMemory: String = "{}",
    val externalPlayerEnabled: Boolean = false,
    val externalPlayerPackage: String = "",
    val externalPlayerLabel: String = "",
    val subtitleFont: String = "sans",
    val subtitleSize: Int = 22,
    val subtitleOutline: Boolean = true,
    val subtitleAuto: Boolean = false,
    val subtitleColor: String = "white",
    val preferredSubtitleToken: String = "",
    val backgroundPlayback: Boolean = false,
    val pipMode: Boolean = false,
    val silentSync: Boolean = true,
    val autoRefreshHours: Int = 24,
    val epg24h: Boolean = true,
    val epgOffsetHours: Int = 0,
    val xtreamEpgOnly: Boolean = false,
    val epgRefreshDays: Int = 1,
    val epgOffsetMinutes: Int = 0,
    val vodInfoEngine: String = "AUTO",
    val translateMeta: Boolean = true,
    val catchupPreset: String = "XTREAM_PATH",
    val catchupTemplate: String = "",
    val osdSizeTier: Int = 1,
    val zapInvert: Boolean = false,
    val homeContinue: Boolean = true,
    val homeAiRecommendations: Boolean = true,
    val homeUpcomingEpg: Boolean = true,
    val homeTrendFilms: Boolean = true,
    val homeTrendSeries: Boolean = true,
    val homeFavoriteFilms: Boolean = true,
    val homeFavoriteSeries: Boolean = true,
    val homeMixedFilms: Boolean = true,
    val homeMixedSeries: Boolean = true,
    val homeMixedLive: Boolean = true,
    val homeUpcomingMatches: Boolean = true,
    val homeLastWatchedButton: Boolean = true,
    val pageTransitionEffect: String = "IOS",
    val dockbarStyle: String = "ORIGINAL",
    val homeRecentLive: Boolean = true,
    val homeLive: Boolean = true,
    val homeMovies: Boolean = true,
    val homeSeries: Boolean = true,
    val homeFavorites: Boolean = true,
    val homeDownloads: Boolean = true,
    val railLive: Boolean = true,
    val railMovies: Boolean = true,
    val railSeries: Boolean = true,
    val railContinue: Boolean = true,
    val railPlaylists: Boolean = true,
    val railRepeat: Boolean = true,
    val railCinemaHub: Boolean = false,
    val keyMapJson: String = "{}"
)

@Serializable
data class BackupProfile(
    val id: String,
    val name: String,
    val pinHash: String? = null,
    val isKids: Boolean = false,
    val avatarHue: Float = 200f
)

@Serializable
data class BackupSource(
    val id: String,
    val kind: String,
    val name: String,
    val baseUrl: String,
    val username: String,
    val password: String,
    val extra: String = ""
)

@Serializable
data class BackupEpg(
    val id: String,
    val name: String,
    val url: String,
    val enabled: Boolean = true
)

@Serializable
data class BackupFavorite(
    val profileId: String,
    val mediaId: String,
    val kind: String,
    val createdAt: Long
)

@Serializable
data class BackupProgress(
    val profileId: String,
    val mediaId: String,
    val kind: String,
    val title: String,
    val poster: String,
    val positionMs: Long,
    val durationMs: Long,
    val updatedAt: Long
)

@Singleton
class BackupRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val db: RayDatabase,
    private val settings: SettingsRepository,
    private val json: Json
) {
    suspend fun exportTo(uri: Uri) {
        val jsonStr = exportJson()
        context.contentResolver.openOutputStream(uri)?.use { out ->
            out.write(jsonStr.toByteArray())
        } ?: error("Cannot write backup")
    }

    suspend fun importFrom(uri: Uri) {
        val text = context.contentResolver.openInputStream(uri)?.use { it.readBytes().decodeToString() }
            ?: error("Cannot read backup")
        importJson(text)
    }

    suspend fun exportJson(): String {
        val s = settings.settings.first()
        val file = BackupFile(
            version = 2,
            settings = BackupSettings(
                startup = s.startup.name,
                glass = s.glass.name,
                hideAdult = s.hideAdult,
                hideLocked = s.hideLocked,
                previewLive = s.previewLive,
                autoplayNext = s.autoplayNext,
                catchupTz = s.catchupTimezoneDevice,
                lang = s.lang.name,
                aspect = s.aspect.name,
                speed = s.speed.toString(),
                combineM3u = s.combineM3u,
                layoutMode = s.layoutMode.name,
                appFontKey = s.appFontKey,
                liveEngine = s.liveEngine.name,
                vodPlaybackEngine = s.vodPlaybackEngine.name,
                lowEndMode = s.lowEndMode,
                adaptiveHaptics = s.adaptiveHaptics,
                epgEnabled = s.epgEnabled,
                epgSourceMode = s.epgSourceMode.name,
                launchOnBoot = s.launchOnBoot,
                stripChannelPrefix = s.stripChannelPrefix,
                autoBackupInterval = s.autoBackupInterval.name,
                osdHideSeconds = s.osdHideSeconds,
                osdOpacity = s.osdOpacity,
                liveBufferSeconds = s.liveBufferSeconds,
                streamFormat = s.streamFormat.name,
                userAgentPreset = s.userAgentPreset.name,
                customUserAgent = s.customUserAgent,
                ignoreSsl = s.ignoreSsl,
                softwareDecoder = s.softwareDecoder,
                smartPlayerSelection = s.smartPlayerSelection,
                mediaKitLowPowerHwdec = s.mediaKitLowPowerHwdec,
                smartPlayerMemory = s.smartPlayerMemory,
                externalPlayerEnabled = s.externalPlayerEnabled,
                externalPlayerPackage = s.externalPlayerPackage,
                externalPlayerLabel = s.externalPlayerLabel,
                subtitleFont = s.subtitleFont,
                subtitleSize = s.subtitleSize,
                subtitleOutline = s.subtitleOutline,
                subtitleAuto = s.subtitleAuto,
                subtitleColor = s.subtitleColor,
                preferredSubtitleToken = s.preferredSubtitleToken,
                backgroundPlayback = s.backgroundPlayback,
                pipMode = s.pipMode,
                silentSync = s.silentSync,
                autoRefreshHours = s.autoRefreshHours,
                epg24h = s.epg24h,
                epgOffsetHours = s.epgOffsetHours,
                xtreamEpgOnly = s.xtreamEpgOnly,
                epgRefreshDays = s.epgRefreshDays,
                epgOffsetMinutes = s.epgOffsetMinutes,
                vodInfoEngine = s.vodInfoEngine.name,
                translateMeta = s.translateMeta,
                catchupPreset = s.catchupPreset.name,
                catchupTemplate = s.catchupTemplate,
                osdSizeTier = s.osdSizeTier,
                zapInvert = s.zapInvert,
                homeContinue = s.homeContinue,
                homeAiRecommendations = s.homeAiRecommendations,
                homeUpcomingEpg = s.homeUpcomingEpg,
                homeTrendFilms = s.homeTrendFilms,
                homeTrendSeries = s.homeTrendSeries,
                homeFavoriteFilms = s.homeFavoriteFilms,
                homeFavoriteSeries = s.homeFavoriteSeries,
                homeMixedFilms = s.homeMixedFilms,
                homeMixedSeries = s.homeMixedSeries,
                homeMixedLive = s.homeMixedLive,
                homeUpcomingMatches = s.homeUpcomingMatches,
                homeLastWatchedButton = s.homeLastWatchedButton,
                pageTransitionEffect = s.pageTransitionEffect.name,
                dockbarStyle = s.dockbarStyle.name,
                homeRecentLive = s.homeRecentLive,
                homeLive = s.homeLive,
                homeMovies = s.homeMovies,
                homeSeries = s.homeSeries,
                homeFavorites = s.homeFavorites,
                homeDownloads = s.homeDownloads,
                railLive = s.railLive,
                railMovies = s.railMovies,
                railSeries = s.railSeries,
                railContinue = s.railContinue,
                railPlaylists = s.railPlaylists,
                railRepeat = s.railRepeat,
                railCinemaHub = s.railCinemaHub,
                keyMapJson = s.keyMapJson
            ),


            profiles = db.profiles().all().map {
                BackupProfile(it.id, it.name, it.pinHash, it.isKids, it.avatarHue)
            },
            sources = db.sources().all().map {
                BackupSource(it.id, it.kind, it.name, it.baseUrl, it.username, it.password, it.extra)
            },
            epgSources = db.epgSources().all().map {
                BackupEpg(it.id, it.name, it.url, it.enabled)
            },
            favorites = db.favorites().all().map {
                BackupFavorite(it.profileId, it.mediaId, it.kind, it.createdAt)
            },
            progress = db.progress().all().map {
                BackupProgress(it.profileId, it.mediaId, it.kind, it.title, it.poster, it.positionMs, it.durationMs, it.updatedAt)
            }
        )
        return json.encodeToString(BackupFile.serializer(), file)
    }

    fun parseBackup(text: String): BackupFile {
        return json.decodeFromString(BackupFile.serializer(), text)
    }

    suspend fun importJson(text: String) {
        importJsonWithMode(text, overwrite = false)
    }

    suspend fun importJsonWithMode(text: String, overwrite: Boolean) {
        val file = json.decodeFromString(BackupFile.serializer(), text)
        if (overwrite) {
            db.favorites().deleteAll()
            db.progress().deleteAll()
        }
        file.profiles.forEach {
            db.profiles().upsert(
                ProfileEntity(it.id, it.name, it.pinHash, it.isKids, it.avatarHue, System.currentTimeMillis())
            )
        }
        file.sources.forEach {
            db.sources().upsert(
                SourceEntity(it.id, it.kind, it.name, it.baseUrl, it.username, it.password, it.extra, System.currentTimeMillis())
            )
        }
        file.epgSources.forEach {
            db.epgSources().upsert(EpgSourceEntity(it.id, it.name, it.url, it.enabled))
        }
        file.favorites.forEach {
            db.favorites().upsert(FavoriteEntity(it.profileId, it.mediaId, it.kind, it.createdAt))
        }
        file.progress.forEach {
            db.progress().upsert(
                ProgressEntity(it.profileId, it.mediaId, it.kind, it.title, it.poster, it.positionMs, it.durationMs, it.updatedAt)
            )
        }
        settings.applyBackup(file.settings)
        if (file.profiles.isNotEmpty()) settings.setProfile(file.profiles.first().id)
        if (file.sources.isNotEmpty()) settings.setSource(file.sources.first().id)
        settings.acceptDisclaimer()
    }
}
