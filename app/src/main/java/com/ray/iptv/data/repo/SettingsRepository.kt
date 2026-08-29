package com.ray.iptv.data.repo

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.ray.iptv.player.AndroidPlaybackSocHints
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.rayStore by preferencesDataStore("ray_settings")

enum class StartupScreen { HOME, LIVE, MOVIES, SERIES, GUIDE }
enum class GlassStyle {
    TV_LITE,
    MACOS_TV,
    DARK,
    AMOLED,
    FLY_UI,
    SEMC,
    DARK_FLAT,
    FLAT_BLACK,
    GLASS_GRI,
    MACOS_GLASS,
    IOS27,
    MAC_TEMA,
    GLASSMORPHISM,
    MINT,
    LIGHT
}
enum class AppLang(
    val code: String,
    val nativeName: String,
    val tmdb: String,
    val rtl: Boolean = false
) {
    TR("tr", "Türkçe", "tr-TR"),
    EN("en", "English", "en-US"),
    FR("fr", "Français", "fr-FR"),
    AR("ar", "العربية", "ar-SA", true),
    ZH("zh", "中文", "zh-CN"),
    RU("ru", "Русский", "ru-RU"),
    JA("ja", "日本語", "ja-JP"),
    ES("es", "Español", "es-ES"),
    KO("ko", "한국어", "ko-KR"),
    HE("he", "עברית", "he-IL", true),
    DA("da", "Dansk", "da-DK"),
    SV("sv", "Svenska", "sv-SE"),
    HI("hi", "हिन्दी", "hi-IN"),
    TH("th", "ไทย", "th-TH"),
    IT("it", "Italiano", "it-IT"),
    PT("pt", "Português", "pt-PT"),
    ID("id", "Bahasa Indonesia", "id-ID"),
    DE("de", "Deutsch", "de-DE"),
    FA("fa", "فارسی", "fa-IR", true),
    PL("pl", "Polski", "pl-PL"),
    NL("nl", "Nederlands", "nl-NL"),
    UK("uk", "Українська", "uk-UA"),
    VI("vi", "Tiếng Việt", "vi-VN"),
    EL("el", "Ελληνικά", "el-GR"),
    RO("ro", "Română", "ro-RO"),
    SQ("sq", "Shqip", "sq-AL"),
    NO("no", "Norsk", "nb-NO"),
    FI("fi", "Suomi", "fi-FI"),
    BS("bs", "Bosanski", "bs-BA"),
    AZ("az", "Azərbaycanca", "az-AZ"),
    KU("ku", "Kurdî", "tr-TR"),
    HU("hu", "Magyar", "hu-HU"),
    KK("kk", "Қазақша", "kk-KZ"),
    UZ("uz", "Oʻzbekcha", "uz-UZ");

    val translateCode: String get() = code
}
enum class AspectMode { FIT, ZOOM, FILL, STRETCH }
enum class StreamFormat { AUTO, HLS, TS }
enum class PlaybackEngine { BETTER, MEDIA_KIT }
enum class UserAgentPreset { DEFAULT, CHROME, VLC, EXOPLAYER, KODI, TIZEN, WEBOS, ANDROID_TV, APPLE_TV, ROKU, OKHTTP, SAFARI, CUSTOM }
enum class VodInfoEngine { AUTO, XTREAM_ONLY, TMDB_OMDB_ONLY }
enum class EpgSourceMode { AUTO, XTREAM, XMLTV }
enum class CatchupPreset { OFF, XTREAM_PATH, TIMESHIFT_PHP, CUSTOM }
enum class LayoutMode { MOBILE, TV, TABLET }

enum class AutoBackupInterval(val days: Int, val trLabel: String, val enLabel: String) {
    OFF(0, "Kapalı", "Off"),
    DAILY(1, "Her Gün (1 Gün)", "Every Day (1 Day)"),
    EVERY_2_DAYS(2, "2 Günde Bir", "Every 2 Days"),
    EVERY_3_DAYS(3, "3 Günde Bir", "Every 3 Days"),
    EVERY_4_DAYS(4, "4 Günde Bir", "Every 4 Days")
}

enum class PageTransitionEffect { IOS, FADE_SCALE, JELLY }

enum class DockbarStyle { ORIGINAL, CAPSULE, MODERN_GLASS }

fun detectDefaultLayoutMode(context: Context): LayoutMode {
    val pm = context.packageManager
    val ui = context.resources.configuration.uiMode and android.content.res.Configuration.UI_MODE_TYPE_MASK
    if (ui == android.content.res.Configuration.UI_MODE_TYPE_TELEVISION) return LayoutMode.TV
    if (pm.hasSystemFeature(android.content.pm.PackageManager.FEATURE_LEANBACK)) return LayoutMode.TV
    if (pm.hasSystemFeature(android.content.pm.PackageManager.FEATURE_TELEVISION)) return LayoutMode.TV
    val cfg = context.resources.configuration
    val short = cfg.smallestScreenWidthDp
    val long = maxOf(cfg.screenWidthDp, cfg.screenHeightDp)
    val touch = pm.hasSystemFeature(android.content.pm.PackageManager.FEATURE_TOUCHSCREEN)
    if (!touch && long >= 720) return LayoutMode.TV
    if (short in 1 until 600 && long >= 900 && !touch) return LayoutMode.TV

    // Comprehensive Tablet Detection (sw600dp, Large/XLarge screen layout, diagonal >= 6.5", landscape width >= 900dp)
    val screenLayoutSize = cfg.screenLayout and android.content.res.Configuration.SCREENLAYOUT_SIZE_MASK
    val isLargeOrXLarge = screenLayoutSize >= android.content.res.Configuration.SCREENLAYOUT_SIZE_LARGE

    val dm = context.resources.displayMetrics
    val xdpi = if (dm.xdpi > 0f) dm.xdpi else dm.densityDpi.toFloat()
    val ydpi = if (dm.ydpi > 0f) dm.ydpi else dm.densityDpi.toFloat()
    val widthInches = dm.widthPixels / xdpi
    val heightInches = dm.heightPixels / ydpi
    val diagonalInches = Math.sqrt((widthInches * widthInches + heightInches * heightInches).toDouble())

    if (touch && (short >= 580 || isLargeOrXLarge || diagonalInches >= 6.5 || long >= 900)) {
        return LayoutMode.TABLET
    }
    return LayoutMode.MOBILE
}



data class RaySettings(
    val activeProfileId: String = "",
    val activeSourceId: String = "",
    val onboardingDone: Boolean = false,
    val disclaimerAccepted: Boolean = false,
    val startup: StartupScreen = StartupScreen.HOME,
    val glass: GlassStyle = GlassStyle.DARK,
    val parentalPinHash: String = "",
    val hideAdult: Boolean = true,
    val hideLocked: Boolean = true,
    val previewLive: Boolean = true,
    val autoplayNext: Boolean = true,
    val catchupTimezoneDevice: Boolean = true,
    val lang: AppLang = AppLang.TR,
    val aspect: AspectMode = AspectMode.STRETCH,
    val speed: Float = 1f,
    val combineM3u: Boolean = false,
    val osdHideSeconds: Int = 7,
    val osdOpacity: Int = 70,
    val liveBufferSeconds: Int = 0,
    val streamFormat: StreamFormat = StreamFormat.AUTO,
    val streamFormatAutoResolved: StreamFormat = StreamFormat.AUTO,
    val userAgentPreset: UserAgentPreset = UserAgentPreset.DEFAULT,
    val customUserAgent: String = "",
    val ignoreSsl: Boolean = true,
    val softwareDecoder: Boolean = false,
    val liveEngine: PlaybackEngine = PlaybackEngine.BETTER,
    val vodPlaybackEngine: PlaybackEngine = PlaybackEngine.MEDIA_KIT,
    val smartPlayerSelection: Boolean = false,
    val mediaKitLowPowerHwdec: Boolean = false,
    val smartPlayerMemory: String = "{}",
    val externalPlayerEnabled: Boolean = false,
    val externalPlayerPackage: String = "",
    val externalPlayerLabel: String = "",
    val subtitleFont: String = "sans",
    val backgroundPlayback: Boolean = false,
    val pipMode: Boolean = false,
    val stripChannelPrefix: Boolean = false,
    val silentSync: Boolean = true,
    val autoRefreshHours: Int = 24,
    val launchOnBoot: Boolean = false,
    val adaptiveHaptics: Boolean = true,
    val sleepMinutes: Int = 0,
    val sleepUntilMs: Long = 0L,
    val appFontKey: String = "sony",
    val layoutMode: LayoutMode = LayoutMode.TV,
    val epgEnabled: Boolean = true,
    val epg24h: Boolean = true,
    val epgOffsetHours: Int = 0,
    val xtreamEpgOnly: Boolean = false,
    val subtitleSize: Int = 22,
    val subtitleOutline: Boolean = true,
    val subtitleAuto: Boolean = false,
    val zapInvert: Boolean = false,
    val lowEndMode: Boolean = false,
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
    val pageTransitionEffect: PageTransitionEffect = PageTransitionEffect.IOS,
    val dockbarStyle: DockbarStyle = DockbarStyle.ORIGINAL,
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
    val lastRefreshMs: Long = 0L,
    val imageCacheMb: Int = 256,
    val vodInfoEngine: VodInfoEngine = VodInfoEngine.AUTO,
    val translateMeta: Boolean = true,
    val epgSourceMode: EpgSourceMode = EpgSourceMode.AUTO,
    val epgRefreshDays: Int = 1,
    val epgOffsetMinutes: Int = 0,
    val lastEpgRefreshMs: Long = 0L,
    val lastGlobalEpgFetchMs: Long = 0L,
    val subtitleColor: String = "white",
    val preferredSubtitleToken: String = "",
    val catchupPreset: CatchupPreset = CatchupPreset.XTREAM_PATH,
    val catchupTemplate: String = "",
    val osdSizeTier: Int = 1,
    val pinRecoveryHash: String = "",
    val imageCleanDays: Int = 0,
    val lastImageCleanMs: Long = 0L,
    val xtreamStatus: String = "",
    val xtreamExpires: String = "",
    val keyMapJson: String = "{}",
    val autoBackupInterval: AutoBackupInterval = AutoBackupInterval.DAILY,
    val lastAutoBackupTime: Long = 0L
) {
    fun effectiveStreamFormat(): StreamFormat =
        if (streamFormat != StreamFormat.AUTO) streamFormat else streamFormatAutoResolved
}

@Singleton
class SettingsRepository @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val ds = context.rayStore

    val settings: Flow<RaySettings> = ds.data.map { p ->
        RaySettings(
            activeProfileId = p[Keys.profile].orEmpty(),
            activeSourceId = p[Keys.source].orEmpty(),
            onboardingDone = p[Keys.onboard] ?: false,
            disclaimerAccepted = p[Keys.disclaimer] ?: false,
            startup = parseEnum(p[Keys.startup], StartupScreen.HOME),
            glass = parseEnum(p[Keys.glass], GlassStyle.DARK),
            parentalPinHash = p[Keys.pin].orEmpty(),
            hideAdult = p[Keys.hideAdult] ?: true,
            hideLocked = p[Keys.hideLocked] ?: true,
            previewLive = p[Keys.preview] ?: true,
            autoplayNext = p[Keys.autoplay] ?: true,
            catchupTimezoneDevice = p[Keys.catchupTz] ?: true,
            lang = parseEnum(p[Keys.lang], AppLang.TR),
            aspect = parseEnum(p[Keys.aspect], AspectMode.STRETCH),
            speed = p[Keys.speed] ?: 1f,
            combineM3u = p[Keys.combine] ?: false,
            osdHideSeconds = p[Keys.osdHide] ?: 7,
            osdOpacity = p[Keys.osdOpacity] ?: 70,
            liveBufferSeconds = p[Keys.liveBuffer] ?: 0,
            streamFormat = parseEnum(p[Keys.streamFmt], StreamFormat.AUTO),
            streamFormatAutoResolved = parseEnum(p[Keys.streamFmtAuto], StreamFormat.AUTO),
            userAgentPreset = parseEnum(p[Keys.ua], UserAgentPreset.DEFAULT),
            customUserAgent = p[Keys.uaCustom].orEmpty(),
            ignoreSsl = p[Keys.ignoreSsl] ?: true,
            softwareDecoder = p[Keys.swDec] ?: false,
            liveEngine = parseEnum(p[Keys.liveEngine], PlaybackEngine.BETTER),
            vodPlaybackEngine = parseEnum(p[Keys.pbVodEngine], PlaybackEngine.MEDIA_KIT),
            smartPlayerSelection = p[Keys.smartPlayer] ?: false,
            mediaKitLowPowerHwdec = p[Keys.mkLowPower] ?: false,
            smartPlayerMemory = p[Keys.smartMem].orEmpty().ifBlank { "{}" },
            externalPlayerEnabled = p[Keys.extOn] ?: false,
            externalPlayerPackage = p[Keys.extPkg].orEmpty(),
            externalPlayerLabel = p[Keys.extLabel].orEmpty(),
            subtitleFont = p[Keys.subFont] ?: "sans",
            backgroundPlayback = p[Keys.bgPlay] ?: false,
            pipMode = p[Keys.pipMode] ?: false,
            stripChannelPrefix = p[Keys.stripPrefix] ?: false,
            silentSync = p[Keys.silent] ?: true,
            autoRefreshHours = p[Keys.refreshH] ?: 24,
            launchOnBoot = p[Keys.boot] ?: false,
            adaptiveHaptics = p[Keys.haptics] ?: true,
            sleepMinutes = p[Keys.sleep] ?: 0,
            sleepUntilMs = p[Keys.sleepUntil] ?: 0L,
            appFontKey = p[Keys.appFont] ?: "sony",
            layoutMode = parseEnum(p[Keys.layout], detectDefaultLayoutMode(context)),
            epgEnabled = p[Keys.epgOn] ?: true,
            epg24h = p[Keys.epg24] ?: true,
            epgOffsetHours = p[Keys.epgOff] ?: 0,
            xtreamEpgOnly = p[Keys.xtreamEpg] ?: false,
            subtitleSize = p[Keys.subSize] ?: 22,
            subtitleOutline = p[Keys.subOut] ?: true,
            subtitleAuto = p[Keys.subAuto] ?: false,
            zapInvert = p[Keys.zapInv] ?: false,
            lowEndMode = p[Keys.lowEnd] ?: run {
                val hints = AndroidPlaybackSocHints.get(context)
                hints.oneGiBRamClass || hints.twoGiBRamClass || (hints.androidTv && (hints.totalRamBytes < AndroidPlaybackSocHints.THREE_GIB || hints.budgetTvBoxSoc || hints.playbackChallengedTv))
            },
            homeContinue = p[Keys.hCont] ?: true,
            homeAiRecommendations = p[Keys.hAi] ?: true,
            homeUpcomingEpg = p[Keys.hUpEpg] ?: true,
            homeTrendFilms = p[Keys.hTrendF] ?: true,
            homeTrendSeries = p[Keys.hTrendS] ?: true,
            homeFavoriteFilms = p[Keys.hFavF] ?: true,
            homeFavoriteSeries = p[Keys.hFavS] ?: true,
            homeMixedFilms = p[Keys.hMixF] ?: true,
            homeMixedSeries = p[Keys.hMixS] ?: true,
            homeMixedLive = p[Keys.hMixLive] ?: true,
            homeUpcomingMatches = p[Keys.hMatch] ?: true,
            homeLastWatchedButton = p[Keys.hLastBtn] ?: true,
            pageTransitionEffect = parseEnum(p[Keys.pageFx], PageTransitionEffect.IOS),
            dockbarStyle = parseEnum(p[Keys.dockStyle], DockbarStyle.ORIGINAL),
            homeRecentLive = p[Keys.hRecent] ?: true,
            homeLive = p[Keys.hLive] ?: true,
            homeMovies = p[Keys.hMov] ?: true,
            homeSeries = p[Keys.hSer] ?: true,
            homeFavorites = p[Keys.hFav] ?: true,
            homeDownloads = p[Keys.hDl] ?: true,
            railLive = p[Keys.rLive] ?: true,
            railMovies = p[Keys.rMov] ?: true,
            railSeries = p[Keys.rSer] ?: true,
            railContinue = p[Keys.rCont] ?: true,
            railPlaylists = p[Keys.rPlay] ?: true,
            railRepeat = p[Keys.rRepeat] ?: true,
            lastRefreshMs = p[Keys.lastRefresh] ?: 0L,
            imageCacheMb = p[Keys.imgCache] ?: 256,
            vodInfoEngine = parseEnum(p[Keys.vodEngine], VodInfoEngine.AUTO),
            translateMeta = p[Keys.translateMeta] ?: true,
            epgSourceMode = parseEnum(p[Keys.epgMode], EpgSourceMode.AUTO),
            epgRefreshDays = p[Keys.epgDays] ?: 1,
            epgOffsetMinutes = p[Keys.epgOffMin] ?: 0,
            lastEpgRefreshMs = p[Keys.epgLast] ?: 0L,
            lastGlobalEpgFetchMs = p[Keys.epgGlobalLast] ?: 0L,
            subtitleColor = p[Keys.subColor] ?: "white",
            preferredSubtitleToken = p[Keys.subPref].orEmpty(),
            catchupPreset = parseEnum(p[Keys.catchupPreset], CatchupPreset.XTREAM_PATH),
            catchupTemplate = p[Keys.catchupTpl].orEmpty(),
            osdSizeTier = p[Keys.osdTier] ?: 1,
            pinRecoveryHash = p[Keys.pinWord].orEmpty(),
            imageCleanDays = p[Keys.imgClean] ?: 0,
            lastImageCleanMs = p[Keys.imgCleanAt] ?: 0L,
            xtreamStatus = p[Keys.xtStatus].orEmpty(),
            xtreamExpires = p[Keys.xtExp].orEmpty(),
            keyMapJson = p[Keys.keyMap].orEmpty().ifBlank { "{}" },
            autoBackupInterval = parseEnum(p[Keys.autoBackupInt], AutoBackupInterval.DAILY),
            lastAutoBackupTime = p[Keys.lastAutoBackup] ?: 0L
        )
    }

    suspend fun setAutoBackupInterval(v: AutoBackupInterval) = ds.edit { it[Keys.autoBackupInt] = v.name }
    suspend fun setLastAutoBackupTime(v: Long) = ds.edit { it[Keys.lastAutoBackup] = v }

    private inline fun <reified T : Enum<T>> parseEnum(raw: String?, fallback: T): T =
        runCatching { java.lang.Enum.valueOf(T::class.java, raw ?: fallback.name) }.getOrDefault(fallback)

    suspend fun setProfile(id: String) = ds.edit { it[Keys.profile] = id }
    suspend fun setSource(id: String) = ds.edit { it[Keys.source] = id }
    suspend fun setOnboarded(done: Boolean = true) = ds.edit { it[Keys.onboard] = done }
    suspend fun acceptDisclaimer() = ds.edit { it[Keys.disclaimer] = true }
    suspend fun setStartup(v: StartupScreen) = ds.edit { it[Keys.startup] = v.name }
    suspend fun setGlass(v: GlassStyle) = ds.edit { it[Keys.glass] = v.name }
    suspend fun setPin(hash: String) = ds.edit { it[Keys.pin] = hash }
    suspend fun setHideAdult(v: Boolean) = ds.edit { it[Keys.hideAdult] = v }
    suspend fun setHideLocked(v: Boolean) = ds.edit { it[Keys.hideLocked] = v }
    suspend fun setPreview(v: Boolean) = ds.edit { it[Keys.preview] = v }
    suspend fun setAutoplay(v: Boolean) = ds.edit { it[Keys.autoplay] = v }
    suspend fun setCatchupTz(v: Boolean) = ds.edit { it[Keys.catchupTz] = v }
    suspend fun setLang(v: AppLang) = ds.edit { it[Keys.lang] = v.name }
    suspend fun setAspect(v: AspectMode) = ds.edit { it[Keys.aspect] = v.name }
    suspend fun setSpeed(v: Float) = ds.edit { it[Keys.speed] = v }
    suspend fun setCombineM3u(v: Boolean) = ds.edit { it[Keys.combine] = v }
    suspend fun setOsdHide(v: Int) = ds.edit { it[Keys.osdHide] = v }
    suspend fun setOsdOpacity(v: Int) = ds.edit { it[Keys.osdOpacity] = v }
    suspend fun setLiveBuffer(v: Int) = ds.edit { it[Keys.liveBuffer] = v }
    suspend fun setStreamFormat(v: StreamFormat) = ds.edit { it[Keys.streamFmt] = v.name }

    /** Mina: `output=ts|hls` ipucunu AUTO çözümüne yazar; kullanıcı HLS/TS seçtiyse dokunmaz. */
    suspend fun applyAutoDetectedLiveStreamFormat(hint: String) {
        val resolved = when (hint.lowercase()) {
            "ts" -> StreamFormat.TS
            "hls" -> StreamFormat.HLS
            else -> return
        }
        ds.edit {
            if (it[Keys.streamFmtAuto] == resolved.name) return@edit
            it[Keys.streamFmtAuto] = resolved.name
        }
    }

    /**
     * Mina `maybeForceTsLiveFormatForWeakHardware`: zayıf TV kutusu / düşük segmentte
     * AUTO çözümünü MPEG-TS yapar. Kullanıcının HLS/TS/AUTO seçimine dokunmaz.
     */
    suspend fun maybeForceTsLiveFormatForWeakHardware() {
        val hints = AndroidPlaybackSocHints.get(context)
        if (!hints.shouldForceTsLiveFormat()) return
        ds.edit {
            val auto = parseEnum(it[Keys.streamFmtAuto], StreamFormat.AUTO)
            if (auto == StreamFormat.AUTO) {
                it[Keys.streamFmtAuto] = StreamFormat.TS.name
            }
            it[Keys.tsForcedLowEnd] = true
        }
    }

    suspend fun setUserAgent(v: UserAgentPreset) = ds.edit { it[Keys.ua] = v.name }
    suspend fun setIgnoreSsl(v: Boolean) = ds.edit { it[Keys.ignoreSsl] = v }
    suspend fun setSoftwareDecoder(v: Boolean) = ds.edit { it[Keys.swDec] = v }
    suspend fun setLiveEngine(v: PlaybackEngine) = ds.edit { it[Keys.liveEngine] = v.name }
    suspend fun setVodPlaybackEngine(v: PlaybackEngine) = ds.edit { it[Keys.pbVodEngine] = v.name }
    suspend fun setSmartPlayerSelection(v: Boolean) = ds.edit { it[Keys.smartPlayer] = v }
    suspend fun setMediaKitLowPowerHwdec(v: Boolean) = ds.edit { it[Keys.mkLowPower] = v }
    suspend fun setSmartPlayerMemory(v: String) = ds.edit { it[Keys.smartMem] = v }
    suspend fun setExternalPlayerEnabled(v: Boolean) = ds.edit { it[Keys.extOn] = v }
    suspend fun setExternalPlayer(pkg: String, label: String) = ds.edit {
        it[Keys.extPkg] = pkg
        it[Keys.extLabel] = label
    }
    suspend fun setSubtitleFont(v: String) = ds.edit { it[Keys.subFont] = v }
    suspend fun setBackgroundPlayback(v: Boolean) = ds.edit { it[Keys.bgPlay] = v }
    suspend fun setPipMode(v: Boolean) = ds.edit { it[Keys.pipMode] = v }
    suspend fun setStripPrefix(v: Boolean) = ds.edit { it[Keys.stripPrefix] = v }
    suspend fun setSilentSync(v: Boolean) = ds.edit { it[Keys.silent] = v }
    suspend fun setAutoRefreshHours(v: Int) = ds.edit { it[Keys.refreshH] = v }
    suspend fun setLaunchOnBoot(v: Boolean) = ds.edit { it[Keys.boot] = v }
    suspend fun setAdaptiveHaptics(v: Boolean) = ds.edit { it[Keys.haptics] = v }
    suspend fun setSleepMinutes(v: Int) = ds.edit {
        it[Keys.sleep] = v
        it[Keys.sleepUntil] = if (v <= 0) 0L else System.currentTimeMillis() + v * 60_000L
    }
    suspend fun clearSleepTimer() = ds.edit {
        it[Keys.sleep] = 0
        it[Keys.sleepUntil] = 0L
    }
    suspend fun setAppFontKey(v: String) = ds.edit { it[Keys.appFont] = v }
    suspend fun setLayoutMode(v: LayoutMode) = ds.edit {
        it[Keys.layout] = v.name
        it[Keys.layoutExplicit] = true
    }
    suspend fun ensureLayoutMode() = ds.edit {
        val detected = detectDefaultLayoutMode(context)
        if (it[Keys.layout] == null) {
            it[Keys.layout] = detected.name
        } else if (detected == LayoutMode.TV && it[Keys.layout] != LayoutMode.TV.name) {
            it[Keys.layout] = LayoutMode.TV.name
        } else if (detected == LayoutMode.TABLET && it[Keys.layout] == LayoutMode.MOBILE.name && it[Keys.layoutExplicit] != true) {
            it[Keys.layout] = LayoutMode.TABLET.name
        }
    }

    suspend fun setEpgEnabled(v: Boolean) = ds.edit { it[Keys.epgOn] = v }
    suspend fun setEpg24h(v: Boolean) = ds.edit { it[Keys.epg24] = v }
    suspend fun setEpgOffset(v: Int) = ds.edit { it[Keys.epgOff] = v }
    suspend fun setXtreamEpgOnly(v: Boolean) = ds.edit { it[Keys.xtreamEpg] = v }
    suspend fun setSubtitleSize(v: Int) = ds.edit { it[Keys.subSize] = v }
    suspend fun setSubtitleOutline(v: Boolean) = ds.edit { it[Keys.subOut] = v }
    suspend fun setSubtitleAuto(v: Boolean) = ds.edit { it[Keys.subAuto] = v }
    suspend fun setVodInfoEngine(v: VodInfoEngine) = ds.edit { it[Keys.vodEngine] = v.name }
    suspend fun setTranslateMeta(v: Boolean) = ds.edit { it[Keys.translateMeta] = v }
    suspend fun setEpgSourceMode(v: EpgSourceMode) = ds.edit { it[Keys.epgMode] = v.name }
    suspend fun setEpgRefreshDays(v: Int) = ds.edit { it[Keys.epgDays] = v }
    suspend fun setEpgOffsetMinutes(v: Int) = ds.edit { it[Keys.epgOffMin] = v }
    suspend fun setLastEpgRefresh(v: Long) = ds.edit { it[Keys.epgLast] = v }
    suspend fun setLastGlobalEpgFetch(v: Long) = ds.edit { it[Keys.epgGlobalLast] = v }
    suspend fun setSubtitleColor(v: String) = ds.edit { it[Keys.subColor] = v }
    suspend fun setPreferredSubtitleToken(v: String) = ds.edit { it[Keys.subPref] = v.trim().lowercase() }
    suspend fun setCatchupPreset(v: CatchupPreset) = ds.edit { it[Keys.catchupPreset] = v.name }
    suspend fun setCatchupTemplate(v: String) = ds.edit { it[Keys.catchupTpl] = v }
    suspend fun setOsdSizeTier(v: Int) = ds.edit { it[Keys.osdTier] = v }
    suspend fun setPinRecoveryHash(v: String) = ds.edit { it[Keys.pinWord] = v }
    suspend fun setImageCleanDays(v: Int) = ds.edit { it[Keys.imgClean] = v }
    suspend fun setLastImageClean(v: Long) = ds.edit { it[Keys.imgCleanAt] = v }
    suspend fun setXtreamAccount(status: String, expires: String) = ds.edit {
        it[Keys.xtStatus] = status
        it[Keys.xtExp] = expires
    }
    suspend fun setZapInvert(v: Boolean) = ds.edit { it[Keys.zapInv] = v }
    suspend fun setLowEnd(v: Boolean) = ds.edit { it[Keys.lowEnd] = v }
    suspend fun setHomeContinue(v: Boolean) = ds.edit { it[Keys.hCont] = v }
    suspend fun setHomeAiRecommendations(v: Boolean) = ds.edit { it[Keys.hAi] = v }
    suspend fun setHomeUpcomingEpg(v: Boolean) = ds.edit { it[Keys.hUpEpg] = v }
    suspend fun setHomeTrendFilms(v: Boolean) = ds.edit { it[Keys.hTrendF] = v }
    suspend fun setHomeTrendSeries(v: Boolean) = ds.edit { it[Keys.hTrendS] = v }
    suspend fun setHomeFavoriteFilms(v: Boolean) = ds.edit { it[Keys.hFavF] = v }
    suspend fun setHomeFavoriteSeries(v: Boolean) = ds.edit { it[Keys.hFavS] = v }
    suspend fun setHomeMixedFilms(v: Boolean) = ds.edit { it[Keys.hMixF] = v }
    suspend fun setHomeMixedSeries(v: Boolean) = ds.edit { it[Keys.hMixS] = v }
    suspend fun setHomeMixedLive(v: Boolean) = ds.edit { it[Keys.hMixLive] = v }
    suspend fun setHomeUpcomingMatches(v: Boolean) = ds.edit { it[Keys.hMatch] = v }
    suspend fun setHomeLastWatchedButton(v: Boolean) = ds.edit { it[Keys.hLastBtn] = v }
    suspend fun setPageTransitionEffect(v: PageTransitionEffect) = ds.edit { it[Keys.pageFx] = v.name }
    suspend fun setDockbarStyle(v: DockbarStyle) = ds.edit { it[Keys.dockStyle] = v.name }
    suspend fun setHomeRecentLive(v: Boolean) = ds.edit { it[Keys.hRecent] = v }
    suspend fun setHomeLive(v: Boolean) = ds.edit { it[Keys.hLive] = v }
    suspend fun setHomeMovies(v: Boolean) = ds.edit { it[Keys.hMov] = v }
    suspend fun setHomeSeries(v: Boolean) = ds.edit { it[Keys.hSer] = v }
    suspend fun setHomeFavorites(v: Boolean) = ds.edit { it[Keys.hFav] = v }
    suspend fun setHomeDownloads(v: Boolean) = ds.edit { it[Keys.hDl] = v }
    suspend fun setRailLive(v: Boolean) = ds.edit { it[Keys.rLive] = v }
    suspend fun setRailMovies(v: Boolean) = ds.edit { it[Keys.rMov] = v }
    suspend fun setRailSeries(v: Boolean) = ds.edit { it[Keys.rSer] = v }
    suspend fun setRailContinue(v: Boolean) = ds.edit { it[Keys.rCont] = v }
    suspend fun setRailPlaylists(v: Boolean) = ds.edit { it[Keys.rPlay] = v }
    suspend fun setRailRepeat(v: Boolean) = ds.edit { it[Keys.rRepeat] = v }
    suspend fun setKeyMapJson(v: String) = ds.edit { it[Keys.keyMap] = v }
    suspend fun setCustomUserAgent(v: String) = ds.edit { it[Keys.uaCustom] = v }
    suspend fun setLastRefresh(v: Long) = ds.edit { it[Keys.lastRefresh] = v }
    suspend fun setImageCacheMb(v: Int) = ds.edit { it[Keys.imgCache] = v }
    suspend fun resetAll() = ds.edit { it.clear() }

    suspend fun applyBackup(s: BackupSettings) = ds.edit {
        it[Keys.startup] = s.startup
        it[Keys.glass] = s.glass
        it[Keys.hideAdult] = s.hideAdult
        it[Keys.hideLocked] = s.hideLocked
        it[Keys.preview] = s.previewLive
        it[Keys.autoplay] = s.autoplayNext
        it[Keys.catchupTz] = s.catchupTz
        it[Keys.lang] = s.lang
        it[Keys.aspect] = s.aspect
        it[Keys.speed] = s.speed.toFloatOrNull() ?: 1f
        it[Keys.combine] = s.combineM3u

        // Device Form Factor Protection: Never overwrite TV with phone layout or vice versa
        val detected = detectDefaultLayoutMode(context)
        val targetLayout = when (detected) {
            LayoutMode.TV -> LayoutMode.TV
            LayoutMode.TABLET -> if (s.layoutMode == LayoutMode.TV.name) LayoutMode.TV else LayoutMode.TABLET
            LayoutMode.MOBILE -> LayoutMode.MOBILE
        }
        it[Keys.layout] = targetLayout.name

        it[Keys.appFont] = s.appFontKey
        it[Keys.liveEngine] = s.liveEngine
        it[Keys.pbVodEngine] = s.vodPlaybackEngine
        it[Keys.lowEnd] = s.lowEndMode
        it[Keys.haptics] = s.adaptiveHaptics
        it[Keys.epgOn] = s.epgEnabled
        it[Keys.epgMode] = s.epgSourceMode
        it[Keys.boot] = s.launchOnBoot
        it[Keys.stripPrefix] = s.stripChannelPrefix
        it[Keys.autoBackupInt] = s.autoBackupInterval
        it[Keys.osdHide] = s.osdHideSeconds
        it[Keys.osdOpacity] = s.osdOpacity
        it[Keys.liveBuffer] = s.liveBufferSeconds
        it[Keys.streamFmt] = s.streamFormat
        it[Keys.ua] = s.userAgentPreset
        it[Keys.uaCustom] = s.customUserAgent
        it[Keys.ignoreSsl] = s.ignoreSsl
        it[Keys.swDec] = s.softwareDecoder
        it[Keys.smartPlayer] = s.smartPlayerSelection
        it[Keys.mkLowPower] = s.mediaKitLowPowerHwdec
        it[Keys.smartMem] = s.smartPlayerMemory
        it[Keys.extOn] = s.externalPlayerEnabled
        it[Keys.extPkg] = s.externalPlayerPackage
        it[Keys.extLabel] = s.externalPlayerLabel
        it[Keys.subFont] = s.subtitleFont
        it[Keys.subSize] = s.subtitleSize
        it[Keys.subOut] = s.subtitleOutline
        it[Keys.subAuto] = s.subtitleAuto
        it[Keys.subColor] = s.subtitleColor
        it[Keys.subPref] = s.preferredSubtitleToken
        it[Keys.bgPlay] = s.backgroundPlayback
        it[Keys.pipMode] = s.pipMode
        it[Keys.silent] = s.silentSync
        it[Keys.refreshH] = s.autoRefreshHours
        it[Keys.epg24] = s.epg24h
        it[Keys.epgOff] = s.epgOffsetHours
        it[Keys.xtreamEpg] = s.xtreamEpgOnly
        it[Keys.epgDays] = s.epgRefreshDays
        it[Keys.epgOffMin] = s.epgOffsetMinutes
        it[Keys.vodEngine] = s.vodInfoEngine
        it[Keys.translateMeta] = s.translateMeta
        it[Keys.catchupPreset] = s.catchupPreset
        it[Keys.catchupTpl] = s.catchupTemplate
        it[Keys.osdTier] = s.osdSizeTier
        it[Keys.zapInv] = s.zapInvert
        it[Keys.hCont] = s.homeContinue
        it[Keys.hAi] = s.homeAiRecommendations
        it[Keys.hUpEpg] = s.homeUpcomingEpg
        it[Keys.hTrendF] = s.homeTrendFilms
        it[Keys.hTrendS] = s.homeTrendSeries
        it[Keys.hFavF] = s.homeFavoriteFilms
        it[Keys.hFavS] = s.homeFavoriteSeries
        it[Keys.hMixF] = s.homeMixedFilms
        it[Keys.hMixS] = s.homeMixedSeries
        it[Keys.hMixLive] = s.homeMixedLive
        it[Keys.hMatch] = s.homeUpcomingMatches
        it[Keys.hLastBtn] = s.homeLastWatchedButton
        it[Keys.pageFx] = s.pageTransitionEffect
        it[Keys.dockStyle] = s.dockbarStyle
        it[Keys.hRecent] = s.homeRecentLive
        it[Keys.hLive] = s.homeLive
        it[Keys.hMov] = s.homeMovies
        it[Keys.hSer] = s.homeSeries
        it[Keys.hFav] = s.homeFavorites
        it[Keys.hDl] = s.homeDownloads
        it[Keys.rLive] = s.railLive
        it[Keys.rMov] = s.railMovies
        it[Keys.rSer] = s.railSeries
        it[Keys.rCont] = s.railContinue
        it[Keys.rPlay] = s.railPlaylists
        it[Keys.rRepeat] = s.railRepeat
        it[Keys.keyMap] = s.keyMapJson
    }




    fun playbackUserAgent(s: RaySettings): String = when (s.userAgentPreset) {
        UserAgentPreset.DEFAULT, UserAgentPreset.CHROME ->
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36"
        UserAgentPreset.VLC -> "VLC/3.0.20 LibVLC/3.0.20"
        UserAgentPreset.EXOPLAYER -> "ExoPlayer (Linux;Android 13) ExoPlayerLib/2.19.1"
        UserAgentPreset.KODI -> "Kodi/20.5 (Linux; Android 13; AOSP) Android/13 Sys_CPU/aarch64 App_Bitness/64 Version/20.5-(20.5.0)"
        UserAgentPreset.TIZEN ->
            "Mozilla/5.0 (SMART-TV; LINUX; Tizen 6.5) AppleWebKit/537.36 (KHTML, like Gecko) 85.0.4183.93/6.5 TV Safari/537.36"
        UserAgentPreset.WEBOS ->
            "Mozilla/5.0 (Web0S; Linux/SmartTV) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/14.0 Safari/605.1.15 WebAppManager"
        UserAgentPreset.ANDROID_TV ->
            "Mozilla/5.0 (Linux; Android 13; AFTKA) AppleWebKit/537.36 (KHTML, like Gecko) Silk/120.0.0.0 like Chrome/120.0.0.0 Safari/537.36"
        UserAgentPreset.APPLE_TV -> "AppleTV6,2/12.5.6"
        UserAgentPreset.ROKU -> "Roku/DVP-9.10 (519.10E04111A)"
        UserAgentPreset.OKHTTP -> "okhttp/4.12.0"
        UserAgentPreset.SAFARI ->
            "Mozilla/5.0 (iPhone; CPU iPhone OS 17_2 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/17.0 Mobile/15E148 Safari/604.1"
        UserAgentPreset.CUSTOM -> s.customUserAgent.ifBlank { "RayIPTV/1.0" }
    }

    private object Keys {
        val profile = stringPreferencesKey("profile")
        val source = stringPreferencesKey("source")
        val startup = stringPreferencesKey("startup")
        val glass = stringPreferencesKey("glass")
        val pin = stringPreferencesKey("pin")
        val onboard = booleanPreferencesKey("onboard")
        val disclaimer = booleanPreferencesKey("disclaimer")
        val hideAdult = booleanPreferencesKey("hide_adult")
        val hideLocked = booleanPreferencesKey("hide_locked")
        val preview = booleanPreferencesKey("preview")
        val autoplay = booleanPreferencesKey("autoplay")
        val catchupTz = booleanPreferencesKey("catchup_tz")
        val lang = stringPreferencesKey("lang")
        val aspect = stringPreferencesKey("aspect")
        val speed = floatPreferencesKey("speed")
        val combine = booleanPreferencesKey("combine_m3u")
        val osdHide = intPreferencesKey("osd_hide")
        val osdOpacity = intPreferencesKey("osd_opacity")
        val liveBuffer = intPreferencesKey("live_buffer")
        val streamFmt = stringPreferencesKey("stream_fmt")
        val streamFmtAuto = stringPreferencesKey("stream_fmt_auto")
        val tsForcedLowEnd = booleanPreferencesKey("live_fmt_ts_forced_low_end")
        val ua = stringPreferencesKey("ua")
        val uaCustom = stringPreferencesKey("ua_custom")
        val ignoreSsl = booleanPreferencesKey("ignore_ssl")
        val swDec = booleanPreferencesKey("sw_dec")
        val liveEngine = stringPreferencesKey("live_engine")
        val pbVodEngine = stringPreferencesKey("pb_vod_engine")
        val smartPlayer = booleanPreferencesKey("smart_player")
        val mkLowPower = booleanPreferencesKey("mk_low_power")
        val smartMem = stringPreferencesKey("smart_player_mem")
        val extOn = booleanPreferencesKey("ext_player_on")
        val extPkg = stringPreferencesKey("ext_player_pkg")
        val extLabel = stringPreferencesKey("ext_player_label")
        val subFont = stringPreferencesKey("sub_font")
        val bgPlay = booleanPreferencesKey("bg_play")
        val pipMode = booleanPreferencesKey("pip_mode")
        val stripPrefix = booleanPreferencesKey("strip_prefix")
        val silent = booleanPreferencesKey("silent")
        val refreshH = intPreferencesKey("refresh_h")
        val boot = booleanPreferencesKey("boot")
        val haptics = booleanPreferencesKey("adaptive_haptics")
        val sleep = intPreferencesKey("sleep")
        val sleepUntil = longPreferencesKey("sleep_until")
        val appFont = stringPreferencesKey("app_font")
        val layout = stringPreferencesKey("layout_mode")
        val epgOn = booleanPreferencesKey("epg_on")
        val epg24 = booleanPreferencesKey("epg_24")
        val epgOff = intPreferencesKey("epg_off")
        val xtreamEpg = booleanPreferencesKey("xtream_epg")
        val subSize = intPreferencesKey("sub_size")
        val subOut = booleanPreferencesKey("sub_out")
        val subAuto = booleanPreferencesKey("sub_auto")
        val zapInv = booleanPreferencesKey("zap_inv")
        val lowEnd = booleanPreferencesKey("low_end_on")
        val hCont = booleanPreferencesKey("h_cont")
        val hAi = booleanPreferencesKey("h_ai")
        val hUpEpg = booleanPreferencesKey("h_up_epg")
        val hTrendF = booleanPreferencesKey("h_trend_f")
        val hTrendS = booleanPreferencesKey("h_trend_s")
        val hFavF = booleanPreferencesKey("h_fav_f")
        val hFavS = booleanPreferencesKey("h_fav_s")
        val hMixF = booleanPreferencesKey("h_mix_f")
        val hMixS = booleanPreferencesKey("h_mix_s")
        val hMixLive = booleanPreferencesKey("h_mix_live")
        val hMatch = booleanPreferencesKey("h_match")
        val hLastBtn = booleanPreferencesKey("h_last_btn")
        val pageFx = stringPreferencesKey("page_fx")
        val dockStyle = stringPreferencesKey("dock_style")
        val hRecent = booleanPreferencesKey("h_recent")
        val hLive = booleanPreferencesKey("h_live")
        val hMov = booleanPreferencesKey("h_mov")
        val hSer = booleanPreferencesKey("h_ser")
        val hFav = booleanPreferencesKey("h_fav")
        val hDl = booleanPreferencesKey("h_dl")
        val lastRefresh = longPreferencesKey("last_refresh")
        val imgCache = intPreferencesKey("img_cache")
        val vodEngine = stringPreferencesKey("vod_engine")
        val translateMeta = booleanPreferencesKey("translate_meta")
        val epgMode = stringPreferencesKey("epg_mode")
        val epgDays = intPreferencesKey("epg_days")
        val epgOffMin = intPreferencesKey("epg_off_min")
        val epgLast = longPreferencesKey("epg_last")
        val epgGlobalLast = longPreferencesKey("epg_global_last")
        val subColor = stringPreferencesKey("sub_color")
        val subPref = stringPreferencesKey("sub_pref_lang")
        val catchupPreset = stringPreferencesKey("catchup_preset")
        val catchupTpl = stringPreferencesKey("catchup_tpl")
        val osdTier = intPreferencesKey("osd_tier")
        val pinWord = stringPreferencesKey("pin_word")
        val imgClean = intPreferencesKey("img_clean_days")
        val imgCleanAt = longPreferencesKey("img_clean_at")
        val xtStatus = stringPreferencesKey("xt_status")
        val xtExp = stringPreferencesKey("xt_exp")
        val rLive = booleanPreferencesKey("r_live")
        val rMov = booleanPreferencesKey("r_mov")
        val rSer = booleanPreferencesKey("r_ser")
        val rCont = booleanPreferencesKey("r_cont")
        val rPlay = booleanPreferencesKey("r_play")
        val rRepeat = booleanPreferencesKey("r_repeat")
        val keyMap = stringPreferencesKey("key_map")
        val searchHist = stringPreferencesKey("search_hist_home")
        val autoBackupInt = stringPreferencesKey("auto_backup_interval")
        val lastAutoBackup = longPreferencesKey("last_auto_backup_time")
        val layoutExplicit = booleanPreferencesKey("layout_explicit")
    }


    val searchHistory: Flow<List<String>> = ds.data.map { prefs ->
        prefs[Keys.searchHist].orEmpty().split('\u001f').map { it.trim() }.filter { it.isNotEmpty() }.take(5)
    }

    suspend fun recordSearch(query: String) {
        val q = query.trim().take(80)
        if (q.isEmpty()) return
        ds.edit { prefs ->
            val cur = prefs[Keys.searchHist].orEmpty().split('\u001f').map { it.trim() }.filter { it.isNotEmpty() }
            val next = listOf(q) + cur.filter { !it.equals(q, true) }
            prefs[Keys.searchHist] = next.take(5).joinToString("\u001f")
        }
    }

    suspend fun removeSearch(query: String) {
        val q = query.trim()
        if (q.isEmpty()) return
        ds.edit { prefs ->
            val next = prefs[Keys.searchHist].orEmpty().split('\u001f').map { it.trim() }
                .filter { it.isNotEmpty() && !it.equals(q, true) }
            prefs[Keys.searchHist] = next.joinToString("\u001f")
        }
    }

    suspend fun clearSearchHistory() {
        ds.edit { it.remove(Keys.searchHist) }
    }
}
