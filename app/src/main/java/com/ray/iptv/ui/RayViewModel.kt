package com.ray.iptv.ui

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.withTimeoutOrNull

import com.ray.iptv.data.catalog.SeriesNameGrouping
import com.ray.iptv.data.local.CategoryEntity
import com.ray.iptv.data.local.ChannelEntity
import com.ray.iptv.data.local.DownloadEntity
import com.ray.iptv.data.local.EpisodeEntity
import com.ray.iptv.data.local.EpgEntity
import com.ray.iptv.data.local.GroupEntity
import com.ray.iptv.data.local.ProfileEntity
import com.ray.iptv.data.local.ProgressEntity
import com.ray.iptv.data.local.SourceEntity
import com.ray.iptv.data.local.VodEntity
import com.ray.iptv.data.meta.TmdbOmdbService
import com.ray.iptv.data.meta.VodMeta
import com.ray.iptv.player.AndroidPlaybackSocHints
import com.ray.iptv.player.CatchupUrl
import com.ray.iptv.player.PlayErrorKind
import com.ray.iptv.player.StreamHints
import com.ray.iptv.player.SubtitleLanguages
import com.ray.iptv.player.XtreamStreamUrls
import com.ray.iptv.data.remote.XtreamAccountSnapshot
import com.ray.iptv.data.parser.M3uXtreamSniffer
import com.ray.iptv.net.PlaylistHttp
import com.ray.iptv.data.account.AccountRepository
import com.ray.iptv.data.account.AccountSession
import com.ray.iptv.data.account.GoogleAuthClient
import com.ray.iptv.data.account.RayAdmin
import com.ray.iptv.data.admin.AdminRepository
import com.ray.iptv.data.repo.AppLang
import com.ray.iptv.data.repo.AspectMode
import com.ray.iptv.data.repo.BackupRepository
import com.ray.iptv.data.repo.CatalogRepository
import com.ray.iptv.data.repo.EpgSourceMode
import com.ray.iptv.data.repo.EpgStats
import com.ray.iptv.data.repo.GlassStyle
import com.ray.iptv.data.repo.CatchupPreset
import com.ray.iptv.data.repo.DockbarStyle
import com.ray.iptv.data.repo.LayoutMode
import com.ray.iptv.data.repo.PageTransitionEffect
import com.ray.iptv.data.repo.Parental
import com.ray.iptv.data.repo.PlaybackEngine
import com.ray.iptv.data.repo.ProfileRepository
import com.ray.iptv.data.repo.RaySettings
import com.ray.iptv.data.repo.SettingsRepository
import com.ray.iptv.data.repo.StartupScreen
import com.ray.iptv.data.repo.SplashStyle
import com.ray.iptv.data.repo.StreamFormat
import com.ray.iptv.data.repo.UserAgentPreset
import com.ray.iptv.data.repo.VodInfoEngine
import com.ray.iptv.data.repo.XtreamVodDetail
import com.ray.iptv.net.ImageCacheConfig
import com.ray.iptv.net.PlaybackIdentity
import com.ray.iptv.net.SslBypass
import com.ray.iptv.player.RayPlayer
import coil.ImageLoader
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class RayViewModel @Inject constructor(

    @ApplicationContext private val app: Context,
    val catalog: CatalogRepository,
    val profiles: ProfileRepository,
    val settingsRepo: SettingsRepository,
    val player: RayPlayer,
    private val backup: BackupRepository,
    private val vodMeta: TmdbOmdbService,
    private val imageLoader: ImageLoader,
    val admin: AdminRepository,
    private val accounts: AccountRepository,
    private val googleAuth: GoogleAuthClient,
    val firebaseService: com.ray.iptv.data.firebase.FirebaseService,
    val openSubtitles: com.ray.iptv.data.meta.OpenSubtitlesService,
    val speedTestService: com.ray.iptv.net.SpeedTestService,
    val dataUsageService: com.ray.iptv.net.DataUsageService,
    val licensingRepo: com.ray.iptv.data.repo.LicensingRepository,
    val chatRepo: com.ray.iptv.data.chat.CommunityChatRepository
) : ViewModel() {

    val communityMessages: StateFlow<List<com.ray.iptv.data.chat.CommunityChatMessage>> = chatRepo.listenMessages()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun sendCommunityMessage(text: String, onComplete: ((Boolean, String?) -> Unit)? = null) {
        val sess = account.value
        val authUser = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser
        val uid = sess.uid.ifBlank { authUser?.uid.orEmpty() }
        val email = sess.email.ifBlank { authUser?.email.orEmpty() }
        val name = sess.displayName.ifBlank { authUser?.displayName.orEmpty() }
        val photo = sess.photoUrl.ifBlank { authUser?.photoUrl?.toString().orEmpty() }

        if (uid.isBlank() || (!sess.signedIn && email.isBlank() && (authUser == null || authUser.isAnonymous))) {
            onComplete?.invoke(false, "Lütfen önce Google ile oturum açın.")
            return
        }

        viewModelScope.launch {
            val res = chatRepo.sendMessage(
                text = text,
                senderUid = uid,
                senderName = name,
                senderEmail = email,
                senderPhotoUrl = photo,
                isPremium = sess.isPremium || licensingState.value.isPremium,
                isAdmin = sess.isAdmin
            )
            if (res.isSuccess) {
                onComplete?.invoke(true, null)
            } else {
                onComplete?.invoke(false, res.exceptionOrNull()?.message)
            }
        }
    }

    fun deleteCommunityMessage(messageId: String, onComplete: ((Boolean) -> Unit)? = null) {
        viewModelScope.launch {
            val res = chatRepo.deleteMessage(messageId)
            onComplete?.invoke(res.isSuccess)
        }
    }

    val licensingState: StateFlow<com.ray.iptv.data.repo.LicensingState> = licensingRepo.state

    fun refreshLicensing() = viewModelScope.launch { licensingRepo.refresh() }
    suspend fun redeemLicenseCode(code: String): Result<String> = licensingRepo.redeemLicenseCode(code)

    private val settingsHydrated = MutableStateFlow(false)
    val settingsReady: StateFlow<Boolean> = settingsHydrated
    val settings: StateFlow<RaySettings> = settingsRepo.settings
        .onEach { settingsHydrated.value = true }
        .stateIn(viewModelScope, SharingStarted.Eagerly, RaySettings())

    val account: StateFlow<AccountSession> = accounts.session
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AccountSession())

    val profileList = profiles.observe()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val sources = catalog.sources()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val dest = MutableStateFlow(Dest.CONTINUE)
    private var playerOriginDest: Dest? = null
    val overlay = MutableStateFlow(Overlay.NONE)
    val playback = MutableStateFlow<Playback?>(null)
    val pendingNext = MutableStateFlow<NextUpPrompt?>(null)
    private val introSkipMs = java.util.concurrent.ConcurrentHashMap<String, Long>()
    val selectedMovie = MutableStateFlow<VodEntity?>(null)
    val selectedSeries = MutableStateFlow<VodEntity?>(null)
    val vodExtras = MutableStateFlow(VodMeta("", "", "", "", ""))
    val vodExtrasId = MutableStateFlow("")
    val vodMetaLoading = MutableStateFlow(false)
    private var previewJob: Job? = null
    private val vodOpenGen = AtomicInteger(0)
    private var liveHoverJob: Job? = null
    private var actuallyPlayJob: Job? = null
    private var zapRelativeJob: Job? = null
    private var pendingZapIndex: Int = -1
    private var guideSlotsJob: Job? = null
    private var lastGuideChunk: List<String> = emptyList()
    val resumePrompt = MutableStateFlow<Pair<VodEntity, ProgressEntity>?>(null)
    val liveCategoryId = MutableStateFlow("")
    val movieCategoryId = MutableStateFlow("last50")
    val seriesCategoryId = MutableStateFlow("last50")
    val searchQuery = MutableStateFlow("")
    val searchLive = MutableStateFlow<List<ChannelEntity>>(emptyList())
    val searchMovies = MutableStateFlow<List<VodEntity>>(emptyList())
    val searchSeries = MutableStateFlow<List<VodEntity>>(emptyList())
    val searchBusy = MutableStateFlow(false)
    val searchHistory = settingsRepo.searchHistory
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    private var searchJob: Job? = null
    val episodes = MutableStateFlow<List<EpisodeEntity>>(emptyList())
    val pinChallenge = MutableStateFlow<(() -> Unit)?>(null)
    val toast = MutableStateFlow("")
    val zapBuffer = MutableStateFlow("")
    val capturingRemoteKey = MutableStateFlow(false)
    val nowNext = MutableStateFlow<Pair<EpgEntity?, EpgEntity?>>(null to null)
    val browseNow = MutableStateFlow<EpgEntity?>(null)
    val browseUpcoming = MutableStateFlow<List<EpgEntity>>(emptyList())
    val browsePreviewUrl = MutableStateFlow("")
    val browseNowMap = MutableStateFlow<Map<String, EpgEntity>>(emptyMap())
    val browseGuideSlots = MutableStateFlow<Map<String, List<EpgEntity?>>>(emptyMap())
    val livePhase = MutableStateFlow(LiveBrowsePhase.CATEGORIES)
    val moviePhase = MutableStateFlow(LiveBrowsePhase.CATEGORIES)
    val seriesPhase = MutableStateFlow(LiveBrowsePhase.CATEGORIES)
    val railExpanded = MutableStateFlow(true)
    private var sleepJob: Job? = null
    val epgStats = MutableStateFlow(EpgStats())
    private val guideRefreshing = AtomicBoolean(false)
    private var sessionLive = false
    private var sessionUrl = ""
    private var sessionStart = 0L
    private var liveSessionBeganAt = 0L
    private var sessionUa = ""
    private var sessionRef = ""
    private var sessionId = ""
    private var sessionEngine = PlaybackEngine.BETTER
    private var sessionFormat = StreamFormat.HLS
    private var sessionSoftware = false
    private var fallbackStep = 0
    private var rememberedThisSession = false
    private var lastPlayError = ""
    private var liveWatchJob: Job? = null
    private var lastRecoverAt = 0L
    private var stallTicks = 0
    private var sameUrlRetries = 0

    val activeSource: StateFlow<SourceEntity?> = combine(settings, sources) { s, list ->
        val on = list.filter { it.enabled }
        on.firstOrNull { it.id == s.activeSourceId } ?: on.firstOrNull() ?: list.firstOrNull()
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val activeProfile: StateFlow<ProfileEntity?> = combine(settings, profileList) { s, list ->
        list.firstOrNull { it.id == s.activeProfileId } ?: list.firstOrNull()
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val liveCategories = combine(activeSource, settings) { src, s -> src to s }
        .flatMapLatest { (src, s) ->
            when {
                src == null -> flowOf(emptyList())
                s.combineM3u -> catalog.allLiveCategories()
                else -> catalog.categories(src.id, "LIVE")
            }
        }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val movieCategories = combine(activeSource, settings) { src, s -> src to s }
        .flatMapLatest { (src, s) ->
            when {
                src == null -> flowOf(emptyList())
                s.combineM3u -> catalog.allMovieCategories()
                else -> catalog.categories(src.id, "MOVIE")
            }
        }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val seriesCategories = combine(activeSource, settings) { src, s -> src to s }
        .flatMapLatest { (src, s) ->
            when {
                src == null -> flowOf(emptyList())
                s.combineM3u -> catalog.allSeriesCategories()
                else -> catalog.categories(src.id, "SERIES")
            }
        }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val liveLimit = MutableStateFlow(catalog.livePageSize())
    val movieLimit = MutableStateFlow(catalog.vodPageSize())
    val seriesLimit = MutableStateFlow(catalog.vodPageSize())

    val liveTotal = combine(activeSource, settings) { src, s -> src to s }
        .flatMapLatest { (src, s) ->
            when {
                src == null -> flowOf(0)
                s.combineM3u -> catalog.liveVisibleCount("", true)
                else -> catalog.liveVisibleCount(src.id, false)
            }
        }.flowOn(Dispatchers.Default)
        .stateIn(viewModelScope, SharingStarted.Eagerly, 0)

    val liveCounts = combine(activeSource, settings) { src, s -> src to s }
        .flatMapLatest { (src, s) ->
            when {
                src == null -> flowOf(emptyList())
                s.combineM3u -> catalog.liveCounts("", true)
                else -> catalog.liveCounts(src.id, false)
            }
        }.map { rows -> rows.associate { row -> row.categoryId to row.total } }
        .flowOn(Dispatchers.Default)
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyMap())

    val favorites = activeProfile.flatMapLatest { p ->
        if (p == null) flowOf(emptyList()) else catalog.favorites(p.id)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val movies = combine(activeSource, settings, movieCategoryId, movieLimit) { src, s, cat, limit ->
        Triple(src to s.combineM3u, cat, limit)
    }.combine(favorites) { triple, favs -> triple to favs }
        .flatMapLatest { (srcCatLimit, favs) ->
            val (srcCombine, cat, limit) = srcCatLimit
            val (src, combine) = srcCombine
            when {
                src == null -> flowOf(emptyList())
                cat == "fav" -> flow {
                    val ids = favs.filter { it.kind == "MOVIE" }.map { it.mediaId }
                    val map = catalog.vodByIds(ids).associateBy { it.id }
                    emit(ids.mapNotNull { map[it] })
                }
                cat == "last50" -> flow { emit(catalog.vodLastAdded(src.id, "MOVIE", combine)) }
                cat == "popular" -> flow { emit(catalog.vodTopRated(src.id, "MOVIE", combine)) }
                cat == "trend" -> flow { emit(catalog.vodTrend(src.id, "MOVIE", combine)) }
                else -> catalog.vodPage(src.id, "MOVIE", cat, combine, limit)
            }
        }.combine(settings) { list, s -> list to s }
        .combine(activeProfile) { pair, p ->
            val (list, s) = pair
            if (s.hideAdult || p?.isKids == true) {
                list.filter { !Parental.isAnyAdult(it.name, it.categoryName, it.genre) }
            } else list
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val movieTotal = combine(activeSource, settings) { src, s -> src to s }
        .flatMapLatest { (src, s) ->
            when {
                src == null -> flowOf(0)
                s.combineM3u -> catalog.vodKindCount("", "MOVIE", true)
                else -> catalog.vodKindCount(src.id, "MOVIE", false)
            }
        }.flowOn(Dispatchers.Default)
        .stateIn(viewModelScope, SharingStarted.Eagerly, 0)

    val movieCounts = combine(activeSource, settings) { src, s -> src to s }
        .flatMapLatest { (src, s) ->
            when {
                src == null -> flowOf(emptyList())
                s.combineM3u -> catalog.vodCounts("", "MOVIE", true)
                else -> catalog.vodCounts(src.id, "MOVIE", false)
            }
        }.map { rows -> rows.associate { row -> row.categoryId to row.total } }
        .flowOn(Dispatchers.Default)
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyMap())

    val series = combine(activeSource, settings, seriesCategoryId, seriesLimit) { src, s, cat, limit ->
        Triple(src to s.combineM3u, cat, limit)
    }.combine(favorites) { triple, favs -> triple to favs }
        .flatMapLatest { (srcCatLimit, favs) ->
            val (srcCombine, cat, limit) = srcCatLimit
            val (src, combine) = srcCombine
            when {
                src == null -> flowOf(emptyList())
                cat == "fav" -> flow {
                    val ids = favs.filter { it.kind == "SERIES" }.map { it.mediaId }
                    val map = catalog.vodByIds(ids).associateBy { it.id }
                    emit(ids.mapNotNull { map[it] })
                }
                cat == "last50" -> flow { emit(catalog.vodLastAdded(src.id, "SERIES", combine)) }
                cat == "popular" -> flow { emit(catalog.vodTopRated(src.id, "SERIES", combine)) }
                cat == "trend" -> flow { emit(catalog.vodTrend(src.id, "SERIES", combine)) }
                else -> catalog.vodPage(src.id, "SERIES", cat, combine, limit)
            }
        }.map { list ->
            withContext(Dispatchers.Default) {
                SeriesNameGrouping.collapseForBrowse(list)
            }
        }
        .combine(settings) { list, s -> list to s }
        .combine(activeProfile) { pair, p ->
            val (list, s) = pair
            if (s.hideAdult || p?.isKids == true) {
                list.filter { !Parental.isAnyAdult(it.name, it.categoryName, it.genre) }
            } else list
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val seriesTotal = combine(activeSource, settings) { src, s -> src to s }
        .flatMapLatest { (src, s) ->
            when {
                src == null -> flowOf(0)
                s.combineM3u -> catalog.vodKindCount("", "SERIES", true)
                else -> catalog.vodKindCount(src.id, "SERIES", false)
            }
        }.flowOn(Dispatchers.Default)
        .stateIn(viewModelScope, SharingStarted.Eagerly, 0)

    val seriesCounts = combine(activeSource, settings) { src, s -> src to s }
        .flatMapLatest { (src, s) ->
            when {
                src == null -> flowOf(emptyList())
                s.combineM3u -> catalog.vodCounts("", "SERIES", true)
                else -> catalog.vodCounts(src.id, "SERIES", false)
            }
        }.map { rows -> rows.associate { row -> row.categoryId to row.total } }
        .flowOn(Dispatchers.Default)
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyMap())

    val continueWatching = combine(
        activeProfile.flatMapLatest { p ->
            if (p == null) flowOf(emptyList()) else catalog.continueWatching(p.id)
        },
        settings,
        activeProfile
    ) { list, s, p ->
        if (s.hideAdult || p?.isKids == true) list.filter { !Parental.isAdult(it.title) } else list
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val recentLive = activeProfile.flatMapLatest { p ->
        if (p == null) flowOf(emptyList()) else catalog.recentLive(p.id)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val liveChannels = combine(activeSource, settings, liveCategoryId, liveLimit) { src, s, cat, limit ->
        Triple(src to s.combineM3u, cat, limit)
    }.combine(favorites) { triple, favs -> triple to favs }
        .combine(recentLive) { pair, recent -> Triple(pair.first, pair.second, recent) }
        .flatMapLatest { (srcCatLimit, favs, recent) ->
            val (srcCombine, cat, limit) = srcCatLimit
            val (src, combine) = srcCombine
            when {
                src == null -> flowOf(emptyList())
                cat.startsWith("group:") -> catalog.groupChannels(cat.removePrefix("group:"))
                cat == "fav" -> flow {
                    val ids = favs.filter { it.kind == "LIVE" }.map { it.mediaId }
                    emit(catalog.channelsByIds(ids))
                }
                cat == "recent" -> flow {
                    val ids = recent.map { it.mediaId }
                    emit(catalog.channelsByIds(ids))
                }
                else -> catalog.channelsPage(src.id, cat, combine, limit)
            }
        }.combine(settings) { list, s -> list to s }
        .combine(activeProfile) { pair, p ->
            val (list, s) = pair
            if (s.hideAdult || p?.isKids == true) {
                list.filter { !Parental.isAnyAdult(it.name, it.categoryName) }
            } else list
        }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val downloads = activeProfile.flatMapLatest { p ->
        if (p == null) flowOf(emptyList()) else catalog.downloads(p.id)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val groups = activeProfile.flatMapLatest { p ->
        if (p == null) flowOf(emptyList()) else catalog.groups(p.id)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val epgSources = catalog.epgSources()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    init {
        viewModelScope.launch { catalog.ensureSourceOrder() }
        viewModelScope.launch { settingsRepo.ensureLayoutMode() }
        viewModelScope.launch { settingsRepo.maybeForceTsLiveFormatForWeakHardware() }
        viewModelScope.launch { refreshEpgStats() }
        viewModelScope.launch {
            val s = settingsRepo.settings.first()
            val initial = if (s.layoutMode == LayoutMode.MOBILE || s.layoutMode == LayoutMode.TABLET) {
                if (s.startup == StartupScreen.LIVE) Dest.CONTINUE else s.startup.toDest()
            } else {
                s.startup.toDest()
            }
            dest.value = initial
            when (dest.value) {
                Dest.LIVE -> {
                    railExpanded.value = false
                    livePhase.value = LiveBrowsePhase.CATEGORIES
                }
                Dest.MOVIES -> {
                    railExpanded.value = false
                    moviePhase.value = LiveBrowsePhase.CATEGORIES
                }
                Dest.SERIES -> {
                    railExpanded.value = false
                    seriesPhase.value = LiveBrowsePhase.CATEGORIES
                }
                Dest.PLAYLISTS, Dest.SETTINGS, Dest.CATCHUP, Dest.CINEMA_HUB -> railExpanded.value = false
                Dest.CONTINUE, Dest.WRAPPED, Dest.EPG_MIX, Dest.CHAT, Dest.ADMIN, Dest.PAYWALL -> railExpanded.value = true
                Dest.PLAYER -> Unit
            }
            if (s.startup == StartupScreen.GUIDE) dest.value = Dest.CATCHUP
            armSleepTimer(s.sleepUntilMs)
        }
        viewModelScope.launch {
            liveCategoryId.collect {
                liveLimit.value = catalog.livePageSize()
                lastGuideChunk = emptyList()
            }
        }
        viewModelScope.launch { movieCategoryId.collect { movieLimit.value = catalog.vodPageSize() } }
        viewModelScope.launch { seriesCategoryId.collect { seriesLimit.value = catalog.vodPageSize() } }
        viewModelScope.launch {
            settingsRepo.settings.collect {
                SslBypass.enabled = it.ignoreSsl
                PlaybackIdentity.userAgent = settingsRepo.playbackUserAgent(it)
                ImageCacheConfig.maxMb = it.imageCacheMb
                maybeCleanImages(it)
            }
        }
        viewModelScope.launch {
            accounts.session.collect { sess ->
                val authUser = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser
                val uid = when {
                    sess.uid.isNotBlank() -> sess.uid
                    authUser?.uid != null -> authUser.uid
                    else -> {
                        val androidId = try {
                            android.provider.Settings.Secure.getString(app.contentResolver, android.provider.Settings.Secure.ANDROID_ID).orEmpty().take(12)
                        } catch (_: Exception) { "" }
                        if (androidId.isNotBlank()) "dev-$androidId" else "user-${sess.email.hashCode().toString().take(8)}"
                    }
                }
                val email = sess.email.ifBlank { authUser?.email.orEmpty() }
                val displayName = sess.displayName.ifBlank { authUser?.displayName.orEmpty() }
                val photoUrl = sess.photoUrl.ifBlank { authUser?.photoUrl?.toString().orEmpty() }
                val isSigned = sess.signedIn || email.isNotBlank() || (authUser != null && !authUser.isAnonymous)

                firebaseService.syncUserProfile(
                    uid = uid,
                    email = email,
                    displayName = displayName,
                    photoUrl = photoUrl,
                    isPremium = sess.isPremium,
                    isAnonymous = !isSigned,
                    licenseCode = sess.licenseCode
                )
                licensingRepo.refresh()
            }
        }
        viewModelScope.launch {
            while (isActive) {
                val sess = account.value
                val authUser = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser
                val uid = when {
                    sess.uid.isNotBlank() -> sess.uid
                    authUser?.uid != null -> authUser.uid
                    else -> {
                        val androidId = try {
                            android.provider.Settings.Secure.getString(app.contentResolver, android.provider.Settings.Secure.ANDROID_ID).orEmpty().take(12)
                        } catch (_: Exception) { "" }
                        if (androidId.isNotBlank()) "dev-$androidId" else "user-${sess.email.hashCode().toString().take(8)}"
                    }
                }
                val email = sess.email.ifBlank { authUser?.email.orEmpty() }
                val name = sess.displayName.ifBlank { authUser?.displayName.orEmpty() }
                val photoUrl = sess.photoUrl.ifBlank { authUser?.photoUrl?.toString().orEmpty() }

                firebaseService.syncPresence(
                    uid = uid,
                    name = name,
                    email = email,
                    photoUrl = photoUrl
                )
                delay(45_000L)
            }
        }

        viewModelScope.launch {
            var handledEnded = false
            player.state.collect { st ->
                if (st.ended && sessionLive && st.playWhenReady) {
                    handledEnded = false
                    recoverLiveKeepAlive()
                } else {
                    if (st.ended && !handledEnded && settings.value.autoplayNext) {
                        handledEnded = true
                        prepareNextEpisode()
                    }
                    if (!st.ended) handledEnded = false
                }
                if (st.error.isNotBlank() && st.error != lastPlayError) {
                    lastPlayError = st.error
                    retryPlaybackFallback(st.errorKind)
                }
                if (st.error.isBlank()) lastPlayError = ""
                if (st.playing && st.error.isBlank() && st.buffering.not()) {
                    sameUrlRetries = 0
                    stallTicks = 0
                    rememberSmartEngineIfNeeded()
                }
            }
        }
        viewModelScope.launch {
            toast.collect { msg ->
                if (msg.isNotBlank()) {
                    kotlinx.coroutines.delay(2300)
                    if (toast.value == msg) toast.value = ""
                }
            }
        }
        viewModelScope.launch {
            while (true) {
                kotlinx.coroutines.delay(60_000)
                val s = settings.value
                if (s.autoRefreshHours > 0) {
                    val due = s.lastRefreshMs + s.autoRefreshHours * 3_600_000L
                    if (System.currentTimeMillis() >= due) refresh()
                }
            }
        }
    }

    val contentFocusTrigger = MutableStateFlow(0L)

    fun go(d: Dest) {
        if (dest.value == Dest.PLAYER && d != Dest.PLAYER && !settings.value.backgroundPlayback) {
            player.pause()
        }
        overlay.value = Overlay.NONE
        dest.value = d
        contentFocusTrigger.value = System.currentTimeMillis()
        when (d) {
            Dest.LIVE -> {
                railExpanded.value = false
                livePhase.value = LiveBrowsePhase.CATEGORIES
            }
            Dest.MOVIES -> {
                railExpanded.value = false
                moviePhase.value = LiveBrowsePhase.CATEGORIES
                closeDetail()
            }
            Dest.SERIES -> {
                railExpanded.value = false
                seriesPhase.value = LiveBrowsePhase.CATEGORIES
                closeDetail()
            }
            Dest.PLAYLISTS, Dest.SETTINGS, Dest.CONTINUE, Dest.CATCHUP, Dest.CINEMA_HUB -> railExpanded.value = false
            Dest.WRAPPED, Dest.EPG_MIX, Dest.CHAT, Dest.ADMIN, Dest.PAYWALL -> railExpanded.value = true
            Dest.PLAYER -> Unit
        }
    }

    fun enterLiveContent() {
        livePhase.value = LiveBrowsePhase.CONTENT
        railExpanded.value = false
    }

    fun backFromLiveContent() {
        livePhase.value = LiveBrowsePhase.CATEGORIES
        railExpanded.value = false
    }

    fun enterMovieContent() {
        moviePhase.value = LiveBrowsePhase.CONTENT
        railExpanded.value = false
    }

    fun backFromMovieContent() {
        closeDetail()
        moviePhase.value = LiveBrowsePhase.CATEGORIES
        railExpanded.value = false
    }

    fun enterSeriesContent() {
        seriesPhase.value = LiveBrowsePhase.CONTENT
        railExpanded.value = false
    }

    fun backFromSeriesContent() {
        closeDetail()
        seriesPhase.value = LiveBrowsePhase.CATEGORIES
        railExpanded.value = false
    }

    fun expandRail() {
        railExpanded.value = true
    }

    fun collapseRail() {
        railExpanded.value = false
    }

    fun toggleRail() {
        railExpanded.value = !railExpanded.value
    }

    fun showOverlay(o: Overlay) { overlay.value = o }

    fun closeOverlay() {
        overlay.value = Overlay.NONE
        searchQuery.value = ""
        searchLive.value = emptyList()
        searchMovies.value = emptyList()
        searchSeries.value = emptyList()
        searchBusy.value = false
        searchJob?.cancel()
    }

    private var lastExitMs = 0L

    fun consumeExitBack(): Boolean {
        val now = System.currentTimeMillis()
        if (now - lastExitMs in 1 until 2_000) {
            lastExitMs = 0L
            return true
        }
        lastExitMs = now
        toast.value = strings.pressAgainExit
        return false
    }

    fun removeContinue(item: ProgressEntity) {
        viewModelScope.launch {
            val p = activeProfile.value ?: return@launch
            catalog.deleteProgress(p.id, item.mediaId)
        }
    }

    var syncToCloudOnComplete = false

    fun acceptDisclaimer() = viewModelScope.launch { settingsRepo.acceptDisclaimer() }

    fun completeSetup() = viewModelScope.launch {
        if (profiles.all().isEmpty()) {
            val p = profiles.create("Home", null, false)
            settingsRepo.setProfile(p.id)
        }
        settingsRepo.acceptDisclaimer()
        settingsRepo.setOnboarded(true)
        val s = settings.value
        dest.value = if (s.layoutMode == LayoutMode.MOBILE || s.layoutMode == LayoutMode.TABLET) {
            if (s.startup == StartupScreen.LIVE) Dest.CONTINUE else s.startup.toDest()
        } else {
            s.startup.toDest()
        }

        if (syncToCloudOnComplete && account.value.signedIn) {
            syncToCloudOnComplete = false
            backupToCloud()
        }
    }


    fun restartSetup() {
        viewModelScope.launch { settingsRepo.setOnboarded(false) }
    }

    fun addDemoPlaylist() = viewModelScope.launch {
        catalog.beginCatalogSync()
        runCatching {
            withContext(Dispatchers.IO) {
                val id = catalog.addDemoPlaylist()
                settingsRepo.setCombineM3u(false)
                settingsRepo.setSource(id)
                catalog.syncSource(id)
            }
            toast.value = playlistSavedMsg()
        }.onFailure { failCatalogLoad(it) }
    }

    fun createProfile(name: String, pin: String, kids: Boolean) = viewModelScope.launch {
        val p = profiles.create(name, pin.ifBlank { null }, kids)
        settingsRepo.setProfile(p.id)
        settingsRepo.setOnboarded()
    }

    fun selectProfile(id: String) = viewModelScope.launch { settingsRepo.setProfile(id) }
    fun deleteProfile(id: String) = viewModelScope.launch { profiles.delete(id) }

    fun addXtream(name: String, base: String, user: String, pass: String) =
        saveXtream(null, name, base, user, pass)

    fun saveXtream(existingId: String?, name: String, base: String, user: String, pass: String) = viewModelScope.launch {
        catalog.beginCatalogSync()
        runCatching {
            withContext(Dispatchers.IO) {
                val id = catalog.addXtream(name, base, user, pass, existingId)
                settingsRepo.setCombineM3u(false)
                settingsRepo.setSource(id)
                catalog.syncSource(id)
            }
            toast.value = playlistSavedMsg()
            triggerAutoCloudBackupIfSignedIn()
        }.onFailure { failCatalogLoad(it) }
    }

    fun addM3u(name: String, url: String) = saveM3u(null, name, url)

    fun saveM3u(existingId: String?, name: String, url: String) = viewModelScope.launch {
        val trimmed = PlaylistHttp.normalizeUrl(url)
        if (trimmed.isBlank()) {
            val tr = settings.value.lang == AppLang.TR
            toast.value = if (tr) "Lütfen geçerli bir URL girin" else "Please enter a valid URL"
            return@launch
        }
        catalog.beginCatalogSync()
        runCatching {
            withContext(Dispatchers.IO) {
                M3uXtreamSniffer.liveFormatHint(trimmed)?.let { settingsRepo.applyAutoDetectedLiveStreamFormat(it) }
                val id = catalog.addM3u(name, trimmed, existingId)
                catalog.source(id)?.baseUrl?.let { resolved ->
                    M3uXtreamSniffer.liveFormatHint(resolved)?.let { settingsRepo.applyAutoDetectedLiveStreamFormat(it) }
                }
                settingsRepo.setCombineM3u(false)
                settingsRepo.setSource(id)
                val src = catalog.source(id)
                if (src?.kind != "XTREAM") catalog.syncSource(id)
            }
            toast.value = playlistSavedMsg()
            triggerAutoCloudBackupIfSignedIn()
        }.onFailure { failCatalogLoad(it) }
    }

    fun addLocalM3u(name: String, uri: String) = saveLocalM3u(null, name, uri)

    fun saveLocalM3u(existingId: String?, name: String, uri: String) = viewModelScope.launch {
        catalog.beginCatalogSync()
        runCatching {
            withContext(Dispatchers.IO) {
                val id = catalog.addLocalM3u(name, uri, existingId)
                settingsRepo.setCombineM3u(false)
                settingsRepo.setSource(id)
                catalog.syncSource(id)
            }
            toast.value = playlistSavedMsg()
            triggerAutoCloudBackupIfSignedIn()
        }.onFailure { failCatalogLoad(it) }
    }


    fun addStalker(name: String, portal: String, mac: String) = saveStalker(null, name, portal, mac)

    fun saveStalker(existingId: String?, name: String, portal: String, mac: String) = viewModelScope.launch {
        catalog.beginCatalogSync()
        runCatching {
            withContext(Dispatchers.IO) {
                val id = catalog.addStalker(name, portal, mac, existingId)
                settingsRepo.setCombineM3u(false)
                settingsRepo.setSource(id)
                catalog.syncSource(id)
            }
            toast.value = playlistSavedMsg()
        }.onFailure { failCatalogLoad(it) }
    }

    fun renameSource(id: String, name: String) = viewModelScope.launch { catalog.renameSource(id, name) }

    fun removeSource(id: String) = viewModelScope.launch {
        runCatching {
            val all = catalog.allSources()
            if (all.size <= 1) {
                toast.value = if (isEn()) {
                    "At least one list must remain. You cannot delete the last list; use Edit to change it."
                } else {
                    "En az bir liste kalmalı. Son listeyi silemezsiniz; içeriğini değiştirmek için Düzenle kullanın."
                }
                return@launch
            }
            if (settings.value.activeSourceId == id) {
                val next = all.firstOrNull { it.id != id && it.enabled } ?: all.first { it.id != id }
                settingsRepo.setSource(next.id)
            }
            catalog.removeSource(id)
            toast.value = strings.listRemoved
        }.onFailure { toast.value = playlistError(it) }
    }

    fun toggleSourceEnabled(id: String) = viewModelScope.launch {
        val src = catalog.source(id) ?: return@launch
        val turnOn = !src.enabled
        runCatching {
            catalog.setSourceEnabled(id, turnOn)
            if (!turnOn && settings.value.activeSourceId == id) {
                catalog.allSources().firstOrNull { it.enabled && it.id != id }?.let { settingsRepo.setSource(it.id) }
            }
            toast.value = if (turnOn) strings.listEnabled else strings.listDisabled
        }.onFailure { toast.value = playlistError(it) }
    }

    suspend fun loadChannelsForCategory(catId: String): List<ChannelEntity> = withContext(Dispatchers.IO) {
        val src = activeSource.value ?: return@withContext emptyList()
        val s = settings.value
        val list = when {
            catId == "all" || catId.isBlank() -> {
                if (s.combineM3u) catalog.allLiveChannelsDirect()
                else catalog.listChannels(src.id, "")
            }
            catId == "fav" -> {
                val favIds = favorites.value.filter { it.kind == "LIVE" }.map { it.mediaId }
                catalog.channelsByIds(favIds)
            }
            catId == "recent" -> {
                val p = activeProfile.value
                val recent = if (p != null) catalog.recentLive(p.id).firstOrNull() ?: emptyList() else emptyList()
                val ids = recent.map { it.mediaId }
                catalog.channelsByIds(ids)
            }
            catId.startsWith("group:") -> {
                catalog.groupChannels(catId.removePrefix("group:")).firstOrNull() ?: emptyList()
            }
            else -> {
                if (s.combineM3u) catalog.listChannelsCombine(catId)
                else catalog.listChannels(src.id, catId)
            }
        }
        val profile = activeProfile.value
        if (s.hideAdult || profile?.isKids == true) {
            list.filter { !Parental.isAnyAdult(it.name, it.categoryName) }
        } else list
    }

    fun moveSource(id: String, delta: Int) = viewModelScope.launch {
        catalog.moveSource(id, delta)
        toast.value = strings.listOrderUpdated
    }

    fun refreshSource(id: String) = viewModelScope.launch {
        val src = catalog.source(id) ?: return@launch
        if (src.kind == "M3U_FILE") {
            toast.value = if (isEn()) {
                "Local file lists cannot be refreshed. You need to pick the file again."
            } else {
                "Yerel dosya listeleri yenilenemez. Dosyayı tekrar seçmen gerekir."
            }
            return@launch
        }
        catalog.beginCatalogSync()
        runCatching {
            withContext(Dispatchers.IO) {
                settingsRepo.setCombineM3u(false)
                settingsRepo.setSource(id)
                catalog.syncSource(id)
            }
            if (!settings.value.silentSync) {
                toast.value = strings.libraryRefreshed
            }
        }.onFailure { failCatalogLoad(it) }
    }

    private val strings: com.ray.iptv.ui.i18n.Copy get() = com.ray.iptv.ui.i18n.copy(settings.value.lang)
    private fun isEn() = settings.value.lang.name == "EN"

    private val virtualVodCats = setOf("fav", "last50", "popular", "trend")

    private fun failCatalogLoad(t: Throwable) {
        val msg = playlistError(t).ifBlank { t.message.orEmpty() }
        catalog.failCatalogSync(msg)
        toast.value = msg
    }

    suspend fun fetchXtreamAccount(src: SourceEntity): XtreamAccountSnapshot? {
        val snap = catalog.fetchXtreamAccount(src) ?: return null
        snap.user?.let { u ->
            val exp = u.expiryEpochSec?.toString().orEmpty()
            settingsRepo.setXtreamAccount(u.status, exp)
        }
        return snap
    }

    private fun playlistSavedMsg() = strings.listSaved

    private fun playlistError(t: Throwable): String = when (t.message) {
        "LAST_ENABLED" -> if (isEn()) {
            "At least one list must stay on. You cannot disable the last active list."
        } else {
            "En az bir liste açık kalmalı. Son açık listeyi kapatamazsınız."
        }
        "MAX_PLAYLISTS" -> if (isEn()) {
            "You can add at most 32 lists."
        } else {
            "En fazla 32 liste eklenebilir."
        }
        else -> t.message.orEmpty()
    }

    fun refresh() = viewModelScope.launch {
        val silent = settings.value.silentSync
        runCatching {
            withContext(Dispatchers.IO) {
                activeSource.value?.id?.let { catalog.syncSource(it, showDialog = false) }
                catalog.lastXtreamAccount?.let { (st, exp) -> settingsRepo.setXtreamAccount(st, exp) }
                settingsRepo.setLastRefresh(System.currentTimeMillis())
            }
        }.onSuccess {
            if (!silent) toast.value = strings.libraryRefreshed
        }.onFailure {
            if (!silent) toast.value = it.message.orEmpty()
        }
    }

    fun refreshGuide(force: Boolean = true) = viewModelScope.launch {
        if (!settings.value.epgEnabled) {
            if (!settings.value.silentSync) toast.value = strings.epgOff
            return@launch
        }
        if (!guideRefreshing.compareAndSet(false, true)) {
            toast.value = strings.epgRefreshing
            return@launch
        }
        try {
            val s = settings.value
            val allowFiles = s.epgSourceMode != EpgSourceMode.XTREAM && !s.xtreamEpgOnly
            val allowXtream = s.epgSourceMode != EpgSourceMode.XMLTV
            val allowGlobal = s.epgSourceMode != EpgSourceMode.XTREAM
            val lang = s.lang.code
            if (!force && s.epgRefreshDays > 0 && s.lastEpgRefreshMs > 0) {
                val freshUntil = s.lastEpgRefreshMs + s.epgRefreshDays * 86_400_000L
                if (System.currentTimeMillis() < freshUntil) {
                    refreshEpgStats()
                    return@launch
                }
            }
            withContext(Dispatchers.IO) {
                catalog.importAllXmltv(
                    includeFiles = allowFiles,
                    includeXtream = allowXtream,
                    includeGlobal = allowGlobal,
                    forceGlobal = force,
                    langFallback = lang
                )
            }
            settingsRepo.setLastEpgRefresh(System.currentTimeMillis())
            refreshEpgStats()
            if (!settings.value.silentSync) {
                toast.value = strings.epgRefreshed
            }
        } catch (t: Throwable) {
            if (t is kotlinx.coroutines.CancellationException) throw t
            toast.value = t.message?.takeIf { it.isNotBlank() }
                ?: if (isEn()) "TV guide refresh failed" else "TV rehberi yenilenemedi"
        } finally {
            guideRefreshing.set(false)
        }
    }

    fun refreshEpgStats() = viewModelScope.launch {
        epgStats.value = withContext(Dispatchers.IO) { catalog.epgStats() }
    }

    fun playChannel(ch: ChannelEntity) {
        if (hidingAdult() && Parental.isAnyAdult(ch.name, ch.categoryName)) return
        val cat = liveCategories.value.firstOrNull { it.id == ch.categoryId }
        if (cat?.locked == true && settings.value.parentalPinHash.isNotBlank()) {
            pinChallenge.value = { actuallyPlayChannel(ch) }
            return
        }
        actuallyPlayChannel(ch)
    }

    private fun actuallyPlayChannel(ch: ChannelEntity) {
        liveHoverJob?.cancel()
        browsePreviewUrl.value = ""
        actuallyPlayJob?.cancel()
        actuallyPlayJob = viewModelScope.launch {
            val url = catalog.resolvePlayUrl(ch)
            val p = Playback(
                url = url,
                title = ch.name,
                subtitle = ch.categoryName,
                poster = ch.logo,
                mediaId = ch.id,
                kind = "LIVE",
                live = true,
                channelNumber = ch.number,
                sourceId = ch.sourceId,
                userAgent = ch.userAgent,
                referer = ch.referer,
                hasArchive = ch.hasArchive,
                remoteId = ch.remoteId
            )
            if (dest.value != Dest.PLAYER) playerOriginDest = dest.value
            playback.value = p
            dest.value = Dest.PLAYER
            overlay.value = Overlay.NONE
            livePhase.value = LiveBrowsePhase.CONTENT
            prepareAndPlay(
                url = url,
                startMs = 0L,
                userAgent = ch.userAgent.ifBlank { settingsRepo.playbackUserAgent(settings.value) },
                referer = ch.referer,
                live = true,
                mediaId = ch.id
            )
            refreshNowNext(ch.id)
            activeProfile.value?.let {
                catalog.saveProgress(it.id, ch.id, "LIVE", ch.name, ch.logo, 0, 0)
            }
        }
    }

    fun playVod(item: VodEntity, start: Long = 0, force: Boolean = false) {
        if (hidingAdult() && Parental.isAnyAdult(item.name, item.categoryName, item.genre)) return
        if (item.kind == "SERIES") {
            openSeries(item)
            return
        }
        if (!force && start == 0L) {
            viewModelScope.launch {
                val profile = activeProfile.value ?: return@launch actuallyPlayVod(item, 0)
                val prog = catalog.progressOf(profile.id, item.id)
                if (prog != null && prog.positionMs > 30_000 && prog.durationMs > 0 &&
                    prog.positionMs < prog.durationMs * 9 / 10
                ) {
                    resumePrompt.value = item to prog
                    return@launch
                }
                actuallyPlayVod(item, 0)
            }
            return
        }
        actuallyPlayVod(item, start)
    }

    fun confirmResume(fromStart: Boolean) {
        val pair = resumePrompt.value ?: return
        resumePrompt.value = null
        playVod(pair.first, if (fromStart) 0L else pair.second.positionMs, force = true)
    }

    private fun actuallyPlayVod(item: VodEntity, start: Long) {
        if (dest.value != Dest.PLAYER) playerOriginDest = dest.value
        val p = Playback(
            url = item.streamUrl,
            title = item.name,
            subtitle = listOf(item.year, item.genre).filter { it.isNotBlank() }.joinToString(" · "),
            poster = item.poster,
            mediaId = item.id,
            kind = "MOVIE",
            startMs = start,
            sourceId = item.sourceId
        )
        playback.value = p
        dest.value = Dest.PLAYER
        startPlayback(item.streamUrl, start)
    }

    fun playEpisode(ep: EpisodeEntity, series: VodEntity, start: Long = 0) {
        if (hidingAdult() && Parental.isAnyAdult(series.name, series.categoryName, series.genre, ep.name)) return
        if (dest.value != Dest.PLAYER) playerOriginDest = dest.value
        playback.value = Playback(
            url = ep.streamUrl,
            title = series.name,
            subtitle = "S${ep.season}E${ep.episode}  ${ep.name}",
            poster = series.poster,
            mediaId = ep.id,
            kind = "EPISODE",
            startMs = start,
            sourceId = ep.sourceId,
            seriesId = series.id
        )
        dest.value = Dest.PLAYER
        startPlayback(ep.streamUrl, start)
    }

    fun playDownload(item: DownloadEntity) {
        if (item.status != "DONE" || item.path.isBlank()) return
        if (hidingAdult() && Parental.isAdult(item.title)) return
        playback.value = Playback(
            url = "file://${item.path}",
            title = item.title,
            poster = item.poster,
            mediaId = item.mediaId,
            kind = "MOVIE"
        )
        dest.value = Dest.PLAYER
        startPlayback("file://${item.path}")
    }

    fun previewVod(item: VodEntity) {
        if (selectedMovie.value != null || selectedSeries.value != null) return
        if (vodExtrasId.value == item.id && vodExtras.value.plot.isNotBlank()) return
        val gen = vodOpenGen.get()
        previewJob?.cancel()
        previewJob = viewModelScope.launch {
            delay(180)
            if (gen != vodOpenGen.get()) return@launch
            if (selectedMovie.value != null || selectedSeries.value != null) return@launch
            enrichVod(item)
            if (gen != vodOpenGen.get()) return@launch
            if (selectedMovie.value != null || selectedSeries.value != null) return@launch
            vodExtras.value = vodMeta.lastExtras
            vodExtrasId.value = item.id
        }
    }

    fun openMovie(item: VodEntity, fromHome: Boolean = false) {
        val gen = vodOpenGen.incrementAndGet()
        previewJob?.cancel()
        if (item.categoryId.isNotBlank()) movieCategoryId.value = item.categoryId
        selectedMovie.value = item
        selectedSeries.value = null
        episodes.value = emptyList()
        vodExtrasId.value = item.id
        vodExtras.value = vodMeta.seed(item, settings.value)
        vodMetaLoading.value = true
        moviePhase.value = LiveBrowsePhase.CONTENT
        if (fromHome) {
            railExpanded.value = false
            dest.value = Dest.MOVIES
        }
        viewModelScope.launch { finishVodMeta(item, gen, series = false) }
    }
    fun openSeries(item: VodEntity, fromHome: Boolean = false) {
        if (hidingAdult() && Parental.isAnyAdult(item.name, item.categoryName, item.genre)) return
        val gen = vodOpenGen.incrementAndGet()
        previewJob?.cancel()
        if (item.categoryId.isNotBlank()) seriesCategoryId.value = item.categoryId
        selectedSeries.value = item
        selectedMovie.value = null
        episodes.value = emptyList()
        vodExtrasId.value = item.id
        vodExtras.value = vodMeta.seed(item, settings.value)
        vodMetaLoading.value = true
        seriesPhase.value = LiveBrowsePhase.CONTENT
        if (fromHome) {
            railExpanded.value = false
            dest.value = Dest.SERIES
        }
        viewModelScope.launch {
            val cached = withContext(Dispatchers.IO) { catalog.listEpisodes(item.id) }
            if (gen != vodOpenGen.get() || selectedSeries.value?.id != item.id) return@launch
            if (cached.isNotEmpty()) episodes.value = cached
            coroutineScope {
                launch { finishVodMeta(item, gen, series = true) }
                launch {
                    catalog.loadSeriesEpisodes(item)
                    if (gen != vodOpenGen.get() || selectedSeries.value?.id != item.id) return@launch
                    val loaded = catalog.listEpisodes(item.id)
                    if (gen != vodOpenGen.get() || selectedSeries.value?.id != item.id) return@launch
                    if (loaded.isNotEmpty()) episodes.value = loaded
                    val current = selectedSeries.value ?: item
                    val filled = vodMeta.fillEpisodeGaps(current, loaded, settings.value)
                    if (filled !== loaded) catalog.upsertEpisodes(filled)
                    if (gen != vodOpenGen.get() || selectedSeries.value?.id != item.id) return@launch
                    episodes.value = filled
                }
            }
        }
    }

    private suspend fun finishVodMeta(item: VodEntity, gen: Int, series: Boolean) = kotlinx.coroutines.coroutineScope {
        vodMetaLoading.value = true
        try {
            val xtream = catalog.fetchXtreamVodDetail(item)
            if (gen != vodOpenGen.get()) return@coroutineScope
            val openId = if (series) selectedSeries.value?.id else selectedMovie.value?.id
            if (openId != item.id) return@coroutineScope
            applyXtreamExtras(item, xtream)
            val xtreamMetaItem = item.copy(
                plot = xtream.plot.ifBlank { item.plot },
                poster = xtream.poster.ifBlank { item.poster },
                rating = xtream.rating.ifBlank { item.rating },
                genre = xtream.genre.ifBlank { item.genre },
                year = xtream.year.ifBlank { item.year }
            )
            if (series) selectedSeries.value = xtreamMetaItem else selectedMovie.value = xtreamMetaItem
            vodExtrasId.value = item.id

            val enriched = vodMeta.enrich(item, settings.value, xtream)
            if (gen != vodOpenGen.get()) return@coroutineScope
            val stillOpen = if (series) selectedSeries.value?.id else selectedMovie.value?.id
            if (stillOpen != item.id) return@coroutineScope
            if (series) selectedSeries.value = enriched else selectedMovie.value = enriched
            vodExtras.value = vodMeta.lastExtras
            vodExtrasId.value = item.id
        } finally {
            if (gen == vodOpenGen.get()) {
                vodMetaLoading.value = false
            }
        }
    }

    private fun applyXtreamExtras(item: VodEntity, xtream: XtreamVodDetail) {
        if (xtream.plot.isBlank() && xtream.rating.isBlank() && xtream.genre.isBlank() &&
            xtream.poster.isBlank() && xtream.trailer.isBlank() && xtream.cast.isBlank() &&
            xtream.backdrop.isBlank() && xtream.runtime.isBlank()
        ) return
        val cur = if (vodExtrasId.value == item.id) vodExtras.value else vodMeta.seed(item, settings.value)
        vodExtras.value = cur.copy(
            plot = xtream.plot.ifBlank { cur.plot }.ifBlank { item.plot },
            poster = xtream.poster.ifBlank { cur.poster }.ifBlank { item.poster },
            rating = xtream.rating.ifBlank { cur.rating }.ifBlank { item.rating },
            genre = xtream.genre.ifBlank { cur.genre }.ifBlank { item.genre },
            year = xtream.year.ifBlank { cur.year }.ifBlank { item.year },
            backdrop = xtream.backdrop.ifBlank { xtream.poster }.ifBlank { cur.backdrop }.ifBlank { item.poster },
            runtime = xtream.runtime.ifBlank { cur.runtime },
            cast = xtream.cast.ifBlank { cur.cast },
            director = xtream.director.ifBlank { cur.director },
            country = xtream.country.ifBlank { cur.country },
            people = if (cur.people.isEmpty() && xtream.cast.isNotBlank()) {
                xtream.cast.split(',', ';', '|').map { it.trim() }.filter { it.isNotBlank() }.map { com.ray.iptv.data.meta.CastPerson(it, "") }
            } else cur.people,
            trailerUrl = xtream.trailer.ifBlank { cur.trailerUrl }
        )
        vodExtrasId.value = item.id
    }

    fun seedVodMeta(item: VodEntity?): VodMeta {
        if (item == null) return VodMeta("", "", "", "", "")
        return vodMeta.seed(item, settings.value)
    }

    private suspend fun enrichVod(item: VodEntity): VodEntity {
        val xtream = catalog.fetchXtreamVodDetail(item)
        return vodMeta.enrich(item, settings.value, xtream)
    }

    fun closeDetail() {
        selectedMovie.value = null
        selectedSeries.value = null
        episodes.value = emptyList()
        vodExtrasId.value = ""
        vodExtras.value = VodMeta("", "", "", "", "")
        vodMetaLoading.value = false
    }

    fun backFromPlayer() {
        val snap = player.snapshot()
        val pb = playback.value
        val profile = activeProfile.value
        if (pb != null && profile != null && !pb.live) {
            viewModelScope.launch {
                catalog.saveProgress(profile.id, pb.mediaId, pb.kind, pb.title, pb.poster, snap.position, snap.duration)
            }
        }
        if (pb != null && pb.live && profile != null && liveSessionBeganAt > 0L) {
            val extra = (System.currentTimeMillis() - liveSessionBeganAt).coerceAtLeast(0L)
            viewModelScope.launch {
                val prev = catalog.progressOf(profile.id, pb.mediaId)
                val acc = (prev?.positionMs ?: 0L) + extra
                catalog.saveProgress(profile.id, pb.mediaId, "LIVE", pb.title, pb.poster, acc, acc)
            }
        }
        liveWatchJob?.cancel()
        sessionLive = false
        liveSessionBeganAt = 0L
        player.stop()
        val origin = playerOriginDest
        playerOriginDest = null
        dest.value = origin ?: when (pb?.kind) {
            "LIVE" -> Dest.LIVE
            "SERIES", "EPISODE" -> if (settings.value.layoutMode == LayoutMode.MOBILE) Dest.MOVIES else Dest.SERIES
            "MOVIE" -> Dest.MOVIES
            else -> Dest.CONTINUE
        }
    }

    fun zapDigit(d: Char) {
        zapBuffer.value = (zapBuffer.value + d).takeLast(4)
        viewModelScope.launch {
            val current = zapBuffer.value
            kotlinx.coroutines.delay(1200)
            if (zapBuffer.value == current) {
                val n = current.toIntOrNull() ?: return@launch
                val src = activeSource.value
                val ch = if (settings.value.combineM3u) catalog.channelByNumber("", n)
                else src?.let { catalog.channelByNumber(it.id, n) }
                ch?.let { playChannel(it) }
                zapBuffer.value = ""
            }
        }
    }

    fun search(q: String, immediate: Boolean = false) {
        searchQuery.value = q
        searchJob?.cancel()
        val trimmed = q.trim()
        if (trimmed.isEmpty()) {
            searchLive.value = emptyList()
            searchMovies.value = emptyList()
            searchSeries.value = emptyList()
            searchBusy.value = false
            return
        }
        searchBusy.value = true
        searchJob = viewModelScope.launch {
            if (!immediate) delay(400)
            if (!isActive) return@launch
            val src = activeSource.value ?: run {
                searchBusy.value = false
                return@launch
            }
            val r = catalog.search(src.id, trimmed)
            if (!isActive) return@launch
            val hide = hidingAdult()
            searchLive.value = if (hide) r.first.filter { !Parental.isAnyAdult(it.name, it.categoryName) } else r.first
            searchMovies.value = if (hide) r.second.filter { !Parental.isAnyAdult(it.name, it.categoryName, it.genre) } else r.second
            val filteredSeries = if (hide) r.third.filter { !Parental.isAnyAdult(it.name, it.categoryName, it.genre) } else r.third
            searchSeries.value = withContext(Dispatchers.Default) {
                SeriesNameGrouping.collapseForBrowse(filteredSeries)
            }
            searchBusy.value = false
        }
    }

    fun recordSearchQuery() {
        viewModelScope.launch { settingsRepo.recordSearch(searchQuery.value) }
    }

    fun removeRecentSearch(q: String) {
        viewModelScope.launch { settingsRepo.removeSearch(q) }
    }

    fun clearSearchHistory() {
        viewModelScope.launch { settingsRepo.clearSearchHistory() }
    }

    fun pickSearchLive(ch: ChannelEntity) {
        recordSearchQuery()
        closeOverlay()
        if (ch.categoryId.isNotBlank()) liveCategoryId.value = ch.categoryId
        dest.value = Dest.LIVE
        playChannel(ch)
    }

    fun pickSearchMovie(item: VodEntity) {
        recordSearchQuery()
        closeOverlay()
        openMovie(item, fromHome = true)
    }

    fun pickSearchSeries(item: VodEntity) {
        recordSearchQuery()
        closeOverlay()
        openSeries(item, fromHome = true)
    }

    fun toggleFav(mediaId: String, kind: String, on: Boolean = true) = viewModelScope.launch {
        val p = activeProfile.value ?: return@launch
        val currently = catalog.isFavoriteNow(p.id, mediaId)
        catalog.setFavorite(p.id, mediaId, kind, !currently)
    }

    fun setGlass(v: GlassStyle) = viewModelScope.launch { settingsRepo.setGlass(v) }
    fun setStartup(v: StartupScreen) = viewModelScope.launch { settingsRepo.setStartup(v) }
    fun setSplashStyle(v: SplashStyle) = viewModelScope.launch { settingsRepo.setSplashStyle(v) }
    fun setPin(pin: String) = viewModelScope.launch {
        settingsRepo.setPin(if (pin.isBlank()) "" else ProfileRepository.hashPin(pin))
    }
    fun setHideAdult(v: Boolean) = viewModelScope.launch {
        settingsRepo.setHideAdult(v)
        if (v) leaveAdultCategories()
    }
    fun setHideLocked(v: Boolean) = viewModelScope.launch { settingsRepo.setHideLocked(v) }
    fun setPreview(v: Boolean) = viewModelScope.launch { settingsRepo.setPreview(v) }
    fun setAutoplay(v: Boolean) = viewModelScope.launch { settingsRepo.setAutoplay(v) }
    fun setCatchupTz(v: Boolean) = viewModelScope.launch { settingsRepo.setCatchupTz(v) }
    fun setLang(v: AppLang) = viewModelScope.launch { settingsRepo.setLang(v) }
    fun setAspect(v: AspectMode) = viewModelScope.launch { settingsRepo.setAspect(v) }
    fun setSpeed(v: Float) = viewModelScope.launch {
        settingsRepo.setSpeed(v)
        player.setSpeed(v)
    }
    fun setCombineM3u(v: Boolean) = viewModelScope.launch { settingsRepo.setCombineM3u(v) }
    fun setOsdHide(v: Int) = viewModelScope.launch { settingsRepo.setOsdHide(v) }
    fun setOsdOpacity(v: Int) = viewModelScope.launch { settingsRepo.setOsdOpacity(v) }
    fun setLiveBuffer(v: Int) = viewModelScope.launch { settingsRepo.setLiveBuffer(v) }
    fun setStreamFormat(v: StreamFormat) = viewModelScope.launch { settingsRepo.setStreamFormat(v) }
    fun setUserAgent(v: UserAgentPreset) = viewModelScope.launch { settingsRepo.setUserAgent(v) }
    fun setIgnoreSsl(v: Boolean) = viewModelScope.launch { settingsRepo.setIgnoreSsl(v) }
    fun setSoftwareDecoder(v: Boolean) = viewModelScope.launch { settingsRepo.setSoftwareDecoder(v) }
    fun setLiveEngine(v: PlaybackEngine) = viewModelScope.launch { settingsRepo.setLiveEngine(v) }
    fun setVodPlaybackEngine(v: PlaybackEngine) = viewModelScope.launch { settingsRepo.setVodPlaybackEngine(v) }
    fun setSmartPlayerSelection(v: Boolean) = viewModelScope.launch { settingsRepo.setSmartPlayerSelection(v) }
    fun setMediaKitLowPowerHwdec(v: Boolean) = viewModelScope.launch { settingsRepo.setMediaKitLowPowerHwdec(v) }
    fun setExternalPlayerEnabled(v: Boolean) = viewModelScope.launch { settingsRepo.setExternalPlayerEnabled(v) }
    fun setExternalPlayer(pkg: String, label: String) = viewModelScope.launch { settingsRepo.setExternalPlayer(pkg, label) }
    fun setSubtitleFont(v: String) = viewModelScope.launch { settingsRepo.setSubtitleFont(v) }
    fun pauseFromBackground() { player.pause() }
    fun resumeFromBackground() {
        if (dest.value == Dest.PLAYER && !settings.value.backgroundPlayback) {
            player.resume()
        }
    }
    fun setBackgroundPlayback(v: Boolean) = viewModelScope.launch { settingsRepo.setBackgroundPlayback(v) }
    fun setPipMode(v: Boolean) = viewModelScope.launch { settingsRepo.setPipMode(v) }
    fun setStripPrefix(v: Boolean) = viewModelScope.launch { settingsRepo.setStripPrefix(v) }
    fun setSilentSync(v: Boolean) = viewModelScope.launch { settingsRepo.setSilentSync(v) }
    fun setAutoRefreshHours(v: Int) = viewModelScope.launch { settingsRepo.setAutoRefreshHours(v) }
    fun setLaunchOnBoot(v: Boolean) = viewModelScope.launch { settingsRepo.setLaunchOnBoot(v) }
    fun setAdaptiveHaptics(v: Boolean) = viewModelScope.launch { settingsRepo.setAdaptiveHaptics(v) }
    fun setSleepMinutes(v: Int) = viewModelScope.launch {
        settingsRepo.setSleepMinutes(v)
        val until = if (v <= 0) 0L else System.currentTimeMillis() + v * 60_000L
        armSleepTimer(until)
        val en = settings.value.lang.name == "EN"
        toast.value = when {
            v <= 0 -> if (en) "Sleep timer turned off." else "Uyku zamanlayıcısı kapatıldı."
            else -> if (en) "Timer set to $v minutes." else "Zamanlayıcı $v dakika olarak ayarlandı."
        }
    }

    fun setAppFontKey(v: String) = viewModelScope.launch { settingsRepo.setAppFontKey(v) }
    fun setLayoutMode(v: LayoutMode) = viewModelScope.launch {
        settingsRepo.setLayoutMode(v)
        if ((v == LayoutMode.MOBILE || v == LayoutMode.TABLET) && dest.value != Dest.PLAYER) dest.value = Dest.CONTINUE
    }


    fun cycleAspect() {
        setAspect(
            when (settings.value.aspect) {
                AspectMode.FIT -> AspectMode.ZOOM
                AspectMode.ZOOM -> AspectMode.FILL
                AspectMode.FILL -> AspectMode.STRETCH
                AspectMode.STRETCH -> AspectMode.FIT
            }
        )
    }

    fun playLastWatchedLive() {
        val item = recentLive.value.firstOrNull() ?: return
        resumeItem(item)
    }

    fun playLastWatched() {
        val live = recentLive.value.firstOrNull()
        val vod = continueWatching.value.firstOrNull { it.kind != "LIVE" }
        val item = listOfNotNull(live, vod).maxByOrNull { it.updatedAt } ?: return
        resumeItem(item)
    }

    fun playLiveId(id: String) = viewModelScope.launch {
        catalog.channel(id)?.let { playChannel(it) }
    }

    suspend fun wrappedRows(): List<ProgressEntity> {
        val p = activeProfile.value ?: return emptyList()
        return catalog.progressList(p.id)
    }

    suspend fun showcaseTrend(kind: String): List<VodEntity> {
        val src = activeSource.value ?: return emptyList()
        return catalog.vodTrend(src.id, kind, settings.value.combineM3u)
    }

    suspend fun showcaseMixed(kind: String): List<VodEntity> {
        val src = activeSource.value ?: return emptyList()
        return catalog.vodMixed(src.id, kind, settings.value.combineM3u)
    }

    suspend fun showcaseMixedLive(): List<ChannelEntity> {
        val src = activeSource.value ?: return emptyList()
        return catalog.liveMixed(src.id, settings.value.combineM3u)
    }

    suspend fun showcaseFavorites(kind: String): List<VodEntity> {
        val ids = favorites.value.filter { it.kind == kind }.map { it.mediaId }
        if (ids.isEmpty()) return emptyList()
        val map = catalog.vodByIds(ids).associateBy { it.id }
        return ids.mapNotNull { map[it] }
    }

    suspend fun showcaseFavoriteLive(): List<ChannelEntity> {
        val ids = favorites.value.filter { it.kind == "LIVE" }.map { it.mediaId }
        if (ids.isEmpty()) return emptyList()
        val map = catalog.channelsByIds(ids).associateBy { it.id }
        return ids.mapNotNull { map[it] }
    }

    suspend fun showcaseCategory(kind: String, catId: String, limit: Int = 16): List<VodEntity> {
        val src = activeSource.value ?: return emptyList()
        val combine = settings.value.combineM3u
        val all = when (catId) {
            "fav" -> showcaseFavorites(kind)
            "last50" -> catalog.vodLastAdded(src.id, kind, combine)
            "popular" -> catalog.vodTopRated(src.id, kind, combine)
            "trend" -> catalog.vodTrend(src.id, kind, combine)
            "mixed" -> catalog.vodMixed(src.id, kind, combine)
            else -> catalog.vodInCategory(src.id, kind, catId, combine)
        }
        return if (limit > 0) all.take(limit) else all
    }

    suspend fun showcaseCategoryRows(
        kind: String,
        cats: List<CategoryEntity>,
        perRow: Int = 16
    ): List<Pair<CategoryEntity, List<VodEntity>>> {
        return cats.mapNotNull { cat ->
            val items = showcaseCategory(kind, cat.id, perRow)
            if (items.isEmpty()) null else cat to items
        }
    }

    suspend fun episodeProgress(seriesId: String): Map<String, ProgressEntity> {
        val p = activeProfile.value ?: return emptyMap()
        val ids = catalog.listEpisodes(seriesId).map { it.id }.toSet()
        if (ids.isEmpty()) return emptyMap()
        return catalog.progressList(p.id).filter { it.mediaId in ids }.associateBy { it.mediaId }
    }

    suspend fun seriesOf(id: String): VodEntity? = catalog.vodItem(id)

    suspend fun episodesOf(id: String): List<EpisodeEntity> = catalog.listEpisodes(id)

    fun playSeriesDirect(item: VodEntity) = viewModelScope.launch {
        if (hidingAdult() && Parental.isAnyAdult(item.name, item.categoryName, item.genre)) return@launch
        withContext(Dispatchers.IO) { catalog.loadSeriesEpisodes(item) }
        val first = catalog.listEpisodes(item.id).firstOrNull()
        if (first == null) {
            openSeries(item)
            return@launch
        }
        playEpisode(first, item)
    }

    suspend fun showcaseEpgMix(kind: EpgMixKind): List<ShowcaseEpgMixItem> {
        val now = epgClock()
        if (kind == EpgMixKind.REPLAY) {
            val rows = catalog.epgRecentlyEnded(now - 12L * 3600_000, now)
            if (rows.isEmpty()) return emptyList()
            val chans = catalog.channelsByIds(rows.map { it.channelId }.distinct()).associateBy { it.id }
            return rows.mapNotNull { p ->
                val ch = chans[p.channelId] ?: return@mapNotNull null
                if (!p.hasCatchup && !ch.hasArchive) return@mapNotNull null
                ShowcaseEpgMixItem(ch, p, EpgMixKind.REPLAY)
            }.take(150)
        }
        val rows = catalog.epgUpcomingRange(now - 3600_000L, now + 12L * 3600_000)
        if (rows.isEmpty()) return emptyList()
        val chans = catalog.channelsByIds(rows.map { it.channelId }.distinct()).associateBy { it.id }
        if (kind == EpgMixKind.ALL) {
            return rows.mapNotNull { p ->
                val ch = chans[p.channelId] ?: return@mapNotNull null
                val hit = classifyEpgMix(p.title, ch.name, ch.categoryName) ?: EpgMixKind.ALL
                ShowcaseEpgMixItem(ch, p, hit)
            }.distinctBy { it.channel.id + it.programme.startMs }.take(150)
        }
        return rows.mapNotNull { p ->
            val ch = chans[p.channelId] ?: return@mapNotNull null
            val hit = classifyEpgMix(p.title, ch.name, ch.categoryName) ?: return@mapNotNull null
            if (hit != kind) return@mapNotNull null
            ShowcaseEpgMixItem(ch, p, hit)
        }.distinctBy { it.channel.id + it.programme.startMs }.take(150)
    }

    suspend fun showcaseEpgMixForDay(kind: EpgMixKind, dayOffset: Int): List<ShowcaseEpgMixItem> {
        val now = epgClock()
        val cal = java.util.Calendar.getInstance()
        cal.add(java.util.Calendar.DAY_OF_YEAR, dayOffset)
        cal.set(java.util.Calendar.HOUR_OF_DAY, 0)
        cal.set(java.util.Calendar.MINUTE, 0)
        cal.set(java.util.Calendar.SECOND, 0)
        cal.set(java.util.Calendar.MILLISECOND, 0)
        val dayStart = cal.timeInMillis
        val dayEnd = if (dayOffset == 0) now else (dayStart + 24L * 3600_000)

        if (kind == EpgMixKind.REPLAY || dayOffset < 0) {
            val rows = catalog.epgRecentlyEnded(dayStart, dayEnd)
            if (rows.isEmpty()) return emptyList()
            val chans = catalog.channelsByIds(rows.map { it.channelId }.distinct()).associateBy { it.id }
            return rows.mapNotNull { p ->
                val ch = chans[p.channelId] ?: return@mapNotNull null
                if (!p.hasCatchup && !ch.hasArchive) return@mapNotNull null
                ShowcaseEpgMixItem(ch, p, EpgMixKind.REPLAY)
            }.take(150)
        }
        return showcaseEpgMix(kind)
    }

    suspend fun showcaseEpg(matchesOnly: Boolean): List<ShowcaseEpgChip> {
        val now = epgClock()
        if (matchesOnly) {
            val rows = catalog.epgUpcomingRange(now - 3600_000L, now + 12L * 3600_000)
            if (rows.isEmpty()) return emptyList()
            val rawKeys = rows.map { it.channelId }.distinct()
            val chans = catalog.channelsByAnyKeys(rawKeys)
            val byId = chans.associateBy { it.id }
            val byRemote = chans.filter { it.remoteId.isNotBlank() }.associateBy { it.remoteId }
            val byEpg = chans.filter { it.epgId.isNotBlank() }.associateBy { it.epgId }
            val chips = rows.mapNotNull { p ->
                if (p.endMs <= now) return@mapNotNull null
                val ch = byId[p.channelId] ?: byRemote[p.channelId] ?: byEpg[p.channelId] ?: return@mapNotNull null
                val sport = classifyEpgMix(p.title, ch.name, ch.categoryName) == EpgMixKind.SPORT ||
                    isSportProgramme(p.title, ch.categoryName, ch.name)
                if (!sport) return@mapNotNull null
                val title = p.title.trim().ifBlank { ch.name }
                ShowcaseEpgChip(
                    ch.id, ch.name, ch.logo, title, p.startMs, p.endMs,
                    p.startMs <= now && p.endMs > now
                )
            }.distinctBy { it.channelId + it.startMs }.sortedBy { it.startMs }.take(36)
            return if (chips.size > 1) chips.shuffled() else chips
        }
        val until = now + 8L * 3600_000
        val rows = catalog.epgUpcomingRange(now, until)
        val rawKeys = rows.map { it.channelId }.distinct()
        val chans = catalog.channelsByAnyKeys(rawKeys)
        val byId = chans.associateBy { it.id }
        val byRemote = chans.filter { it.remoteId.isNotBlank() }.associateBy { it.remoteId }
        val byEpg = chans.filter { it.epgId.isNotBlank() }.associateBy { it.epgId }
        val byCh = rows.groupBy { p ->
            val ch = byId[p.channelId] ?: byRemote[p.channelId] ?: byEpg[p.channelId]
            ch?.id ?: p.channelId
        }
        val favIds = favorites.value.filter { it.kind == "LIVE" }.map { it.mediaId }
        val favSet = favIds.toHashSet()
        val orderedIds = (favIds.filter { it in byCh } + byCh.keys.filter { it !in favSet }).distinct()
        val chips = ArrayList<ShowcaseEpgChip>(16)
        for (id in orderedIds) {
            val ch = byId[id] ?: catalog.channel(id) ?: continue
            val ordered = byCh[id]?.sortedBy { it.startMs }.orEmpty()
            val live = ordered.firstOrNull { it.startMs <= now && it.endMs > now }
            val next = ordered.firstOrNull { it.startMs > now }
            val prog = next ?: live ?: continue
            chips += ShowcaseEpgChip(
                ch.id, ch.name, ch.logo, prog.title, prog.startMs, prog.endMs, next == null
            )
            if (chips.size >= 14) break
        }
        if (chips.size < 10) {
            val have = chips.map { it.channelId }.toHashSet()
            for (id in favIds) {
                if (id in have) continue
                val ch = catalog.channel(id) ?: continue
                val next = catalog.nextEpg(id, now)
                val live = if (next == null) catalog.nowEpg(id, now) else null
                val prog = next ?: live ?: continue
                chips += ShowcaseEpgChip(
                    ch.id, ch.name, ch.logo, prog.title, prog.startMs, prog.endMs, next == null
                )
                if (chips.size >= 12) break
            }
        }
        return chips.sortedBy { it.startMs }.take(12)
    }

    private fun armSleepTimer(untilMs: Long) {
        sleepJob?.cancel()
        if (untilMs <= 0L) return
        val wait = untilMs - System.currentTimeMillis()
        if (wait <= 0L) {
            viewModelScope.launch { fireSleepTimer() }
            return
        }
        sleepJob = viewModelScope.launch {
            kotlinx.coroutines.delay(wait)
            fireSleepTimer()
        }
    }

    private suspend fun fireSleepTimer() {
        player.pause()
        dest.value = Dest.CONTINUE
        settingsRepo.clearSleepTimer()
        toast.value = if (settings.value.lang.name == "EN") {
            "Time is up; playback paused and returned to home."
        } else {
            "Süre doldu; oynatıcı duraklatıldı, ana ekrana dönüldü."
        }
    }
    fun setEpgEnabled(v: Boolean) = viewModelScope.launch { settingsRepo.setEpgEnabled(v) }
    fun setEpg24h(v: Boolean) = viewModelScope.launch { settingsRepo.setEpg24h(v) }
    fun setEpgOffset(v: Int) = viewModelScope.launch { settingsRepo.setEpgOffset(v) }
    fun setXtreamEpgOnly(v: Boolean) = viewModelScope.launch { settingsRepo.setXtreamEpgOnly(v) }
    fun setSubtitleSize(v: Int) = viewModelScope.launch { settingsRepo.setSubtitleSize(v) }
    fun setSubtitleOutline(v: Boolean) = viewModelScope.launch { settingsRepo.setSubtitleOutline(v) }
    fun setSubtitleAuto(v: Boolean) = viewModelScope.launch { settingsRepo.setSubtitleAuto(v) }
    fun setVodInfoEngine(v: VodInfoEngine) = viewModelScope.launch { settingsRepo.setVodInfoEngine(v) }
    fun setTranslateMeta(v: Boolean) = viewModelScope.launch { settingsRepo.setTranslateMeta(v) }
    fun setEpgSourceMode(v: EpgSourceMode) = viewModelScope.launch { settingsRepo.setEpgSourceMode(v) }
    fun setEpgRefreshDays(v: Int) = viewModelScope.launch { settingsRepo.setEpgRefreshDays(v) }
    fun setEpgOffsetMinutes(v: Int) = viewModelScope.launch { settingsRepo.setEpgOffsetMinutes(v) }
    fun setSubtitleColor(v: String) = viewModelScope.launch { settingsRepo.setSubtitleColor(v) }
    fun pickSubtitle(id: String) {
        player.selectText(id, fromUser = true)
        if (sessionLive) return
        val token = if (id == "no") {
            ""
        } else {
            val track = player.state.value.textTracks.firstOrNull { it.id == id }
            SubtitleLanguages.tokenOf(track?.language.orEmpty(), track?.label.orEmpty())
        }
        viewModelScope.launch { settingsRepo.setPreferredSubtitleToken(token) }
    }
    fun clearPreferredSubtitle() = viewModelScope.launch { settingsRepo.setPreferredSubtitleToken("") }
    fun setZapInvert(v: Boolean) = viewModelScope.launch { settingsRepo.setZapInvert(v) }
    fun setLowEnd(v: Boolean) = viewModelScope.launch { settingsRepo.setLowEnd(v) }
    fun setHomeContinue(v: Boolean) = viewModelScope.launch { settingsRepo.setHomeContinue(v) }
    fun setHomeAiRecommendations(v: Boolean) = viewModelScope.launch { settingsRepo.setHomeAiRecommendations(v) }
    fun setHomeUpcomingEpg(v: Boolean) = viewModelScope.launch { settingsRepo.setHomeUpcomingEpg(v) }
    fun setHomeTrendFilms(v: Boolean) = viewModelScope.launch { settingsRepo.setHomeTrendFilms(v) }
    fun setHomeTrendSeries(v: Boolean) = viewModelScope.launch { settingsRepo.setHomeTrendSeries(v) }
    fun setHomeFavoriteFilms(v: Boolean) = viewModelScope.launch { settingsRepo.setHomeFavoriteFilms(v) }
    fun setHomeFavoriteSeries(v: Boolean) = viewModelScope.launch { settingsRepo.setHomeFavoriteSeries(v) }
    fun setHomeMixedFilms(v: Boolean) = viewModelScope.launch { settingsRepo.setHomeMixedFilms(v) }
    fun setHomeMixedSeries(v: Boolean) = viewModelScope.launch { settingsRepo.setHomeMixedSeries(v) }
    fun setHomeMixedLive(v: Boolean) = viewModelScope.launch { settingsRepo.setHomeMixedLive(v) }
    fun setHomeUpcomingMatches(v: Boolean) = viewModelScope.launch { settingsRepo.setHomeUpcomingMatches(v) }
    fun setHomeLastWatchedButton(v: Boolean) = viewModelScope.launch { settingsRepo.setHomeLastWatchedButton(v) }
    fun setPageTransitionEffect(v: PageTransitionEffect) = viewModelScope.launch { settingsRepo.setPageTransitionEffect(v) }
    fun setDockbarStyle(v: DockbarStyle) = viewModelScope.launch { settingsRepo.setDockbarStyle(v) }
    fun setHomeRecentLive(v: Boolean) = viewModelScope.launch { settingsRepo.setHomeRecentLive(v) }
    fun setHomeLive(v: Boolean) = viewModelScope.launch { settingsRepo.setHomeLive(v) }
    fun setHomeMovies(v: Boolean) = viewModelScope.launch { settingsRepo.setHomeMovies(v) }
    fun setHomeSeries(v: Boolean) = viewModelScope.launch { settingsRepo.setHomeSeries(v) }
    fun setHomeFavorites(v: Boolean) = viewModelScope.launch { settingsRepo.setHomeFavorites(v) }
    fun setHomeDownloads(v: Boolean) = viewModelScope.launch { settingsRepo.setHomeDownloads(v) }
    fun setRailLive(v: Boolean) = viewModelScope.launch { settingsRepo.setRailLive(v) }
    fun setRailMovies(v: Boolean) = viewModelScope.launch { settingsRepo.setRailMovies(v) }
    fun setRailSeries(v: Boolean) = viewModelScope.launch { settingsRepo.setRailSeries(v) }
    fun setRailContinue(v: Boolean) = viewModelScope.launch { settingsRepo.setRailContinue(v) }
    fun setRailPlaylists(v: Boolean) = viewModelScope.launch { settingsRepo.setRailPlaylists(v) }
    fun setRailRepeat(v: Boolean) = viewModelScope.launch { settingsRepo.setRailRepeat(v) }
    fun setRailCinemaHub(v: Boolean) = viewModelScope.launch { settingsRepo.setRailCinemaHub(v) }
    fun setCustomUserAgent(v: String) = viewModelScope.launch { settingsRepo.setCustomUserAgent(v) }
    fun setImageCacheMb(v: Int) = viewModelScope.launch { settingsRepo.setImageCacheMb(v) }
    fun resetAllSettings() = viewModelScope.launch {
        withContext(Dispatchers.IO) {
            val p = activeProfile.value
            if (p != null) catalog.clearWatchHistory(p.id)
            catalog.clearAllPlaylists()
            settingsRepo.resetAll()
            googleAuth.signOut()
        }
        toast.value = strings.allReset
    }

    fun getGoogleSignInIntent(): Intent = googleAuth.getGoogleSignInIntent()

    suspend fun handleGoogleSignInIntent(data: Intent?): Boolean {
        val (acc, errCode) = googleAuth.extractAccountFromIntent(data)
        if (acc != null) {
            val ok = googleAuth.handleSignInResult(acc)
            if (ok) {
                toast.value = if (isEn()) "Signed in as ${acc.email}" else "Oturum açıldı: ${acc.email}"
            }
            return ok
        } else {
            val msg = when (errCode) {
                10 -> if (isEn()) "Google Sign-In Error (Code 10): SHA-1 Fingerprint missing in Firebase Console." else "Google Giriş Hatası (Kod 10): Firebase Console'da SHA-1 Parmak İzi Eklenmemiş."
                12500 -> if (isEn()) "Google Sign-In Error (Code 12500): Play Services error." else "Google Giriş Hatası (Kod 12500): Play Hizmetleri Güncellemesi Gerekebilir."
                12501 -> if (isEn()) "Sign-in cancelled." else "Oturum açma iptal edildi."
                null -> if (isEn()) "Sign-in cancelled." else "Oturum açma iptal edildi."
                else -> if (isEn()) "Google Sign-In Error (Code $errCode)" else "Google Oturum Açma Hatası (Kod: $errCode)"
            }
            toast.value = msg
            return false
        }
    }

    fun signInLocal(email: String, displayName: String = "") = viewModelScope.launch {
        runCatching { googleAuth.signInLocal(email, displayName) }
            .onSuccess {
                toast.value = if (RayAdmin.isAdmin(email)) {
                    if (isEn()) "Admin session started." else "Admin oturumu açıldı."
                } else {
                    strings.signedIn
                }
                if (RayAdmin.isAdmin(email) && settings.value.layoutMode == LayoutMode.MOBILE) {
                    dest.value = Dest.ADMIN
                }
            }
            .onFailure {
                toast.value = strings.signInFailed
            }
    }

    fun signOutAccount() = viewModelScope.launch {
        googleAuth.signOut()
        toast.value = strings.signedOut
    }

    fun clearWatchHistory() = viewModelScope.launch {
        val p = activeProfile.value ?: return@launch
        catalog.clearWatchHistory(p.id)
        toast.value = strings.watchHistoryCleared
    }

    fun clearPlaylistsOnly() = viewModelScope.launch {
        catalog.clearAllPlaylists()
        toast.value = strings.playlistsCleared
    }

    fun clearRamCache() {
        viewModelScope.launch {
            runCatching { imageLoader.memoryCache?.clear() }
            toast.value = strings.cacheCleared
        }
    }

    fun deleteDownload(item: DownloadEntity) = viewModelScope.launch {
        catalog.deleteDownload(item.id)
        runCatching { if (item.path.isNotBlank()) java.io.File(item.path).delete() }
        toast.value = strings.downloadRemoved
    }

    fun assignRemoteKey(keyCode: Int, action: String) = viewModelScope.launch {
        val json = JSONObject(settings.value.keyMapJson.ifBlank { "{}" })
        json.keys().asSequence().toList().forEach { k ->
            if (json.optString(k) == action) json.remove(k)
        }
        json.put(keyCode.toString(), action)
        settingsRepo.setKeyMapJson(json.toString())
        toast.value = strings.keyAssigned
    }

    fun clearRemoteKey(action: String) = viewModelScope.launch {
        val json = JSONObject(settings.value.keyMapJson.ifBlank { "{}" })
        json.keys().asSequence().toList().forEach { k ->
            if (json.optString(k) == action) json.remove(k)
        }
        settingsRepo.setKeyMapJson(json.toString())
        toast.value = strings.keyMappingCleared
    }

    fun handleMappedKey(keyCode: Int): Boolean {
        if (capturingRemoteKey.value) return false
        val action = JSONObject(settings.value.keyMapJson.ifBlank { "{}" }).optString(keyCode.toString())
        if (action.isNullOrBlank()) return false
        when (action) {
            "search" -> showOverlay(Overlay.SEARCH)
            "guide" -> go(Dest.CATCHUP)
            "playlists" -> go(Dest.PLAYLISTS)
            "favorites" -> {
                liveCategoryId.value = "fav"
                go(Dest.LIVE)
            }
            "refresh" -> refresh()
            "zap_back" -> zapBack()
            else -> return false
        }
        return true
    }

    fun zapBack() {
        val current = playback.value?.mediaId
        val prev = recentLive.value.firstOrNull { it.mediaId != current } ?: return
        resumeItem(prev)
    }
    fun selectSource(id: String) = viewModelScope.launch {
        runCatching {
            val src = catalog.source(id) ?: return@launch
            if (!src.enabled) catalog.setSourceEnabled(id, true)
            settingsRepo.setCombineM3u(false)
            settingsRepo.setSource(id)
            toast.value = strings.listActivated
        }.onFailure { toast.value = playlistError(it) }
    }

    fun pinCat(id: String, v: Boolean) = viewModelScope.launch { catalog.pinCategory(id, v) }
    fun hideCat(id: String, v: Boolean) = viewModelScope.launch { catalog.hideCategory(id, v) }
    fun lockCat(id: String, v: Boolean) = viewModelScope.launch { catalog.lockCategory(id, v) }

    fun saveCategoryLayout(items: List<CategoryEntity>) = viewModelScope.launch {
        catalog.saveCategoryLayout(items)
        toast.value = strings.categoryHidingSaved
    }

    fun saveLiveChannelLayout(hiddenIds: Set<String>, orderByCategory: Map<String, List<String>>) = viewModelScope.launch {
        catalog.saveLiveChannelLayout(hiddenIds, orderByCategory)
        toast.value = strings.channelLayoutSaved
    }

    fun saveParentalPin(pin: String, recovery: String) = viewModelScope.launch {
        settingsRepo.setPin(ProfileRepository.hashPin(pin))
        settingsRepo.setPinRecoveryHash(ProfileRepository.hashPin(recovery.trim().lowercase()))
        toast.value = strings.pinSaved
    }

    fun clearParental() = viewModelScope.launch {
        settingsRepo.setPin("")
        settingsRepo.setPinRecoveryHash("")
    }

    fun visibleCats(list: List<CategoryEntity>): List<CategoryEntity> {
        val s = settings.value
        val hide = s.hideAdult || activeProfile.value?.isKids == true
        return list.filter { cat ->
            if (cat.hidden) return@filter false
            if (hide && Parental.isAdult(cat.name)) return@filter false
            if (s.hideLocked && cat.locked) return@filter false
            true
        }
    }

    private fun hidingAdult(): Boolean =
        settings.value.hideAdult || activeProfile.value?.isKids == true

    private fun leaveAdultCategories() {
        val live = liveCategories.value.firstOrNull { it.id == liveCategoryId.value }
        if (live != null && Parental.isAdult(live.name)) liveCategoryId.value = ""
        val movie = movieCategories.value.firstOrNull { it.id == movieCategoryId.value }
        if (movie != null && Parental.isAdult(movie.name)) movieCategoryId.value = "last50"
        val series = seriesCategories.value.firstOrNull { it.id == seriesCategoryId.value }
        if (series != null && Parental.isAdult(series.name)) seriesCategoryId.value = "last50"
    }

    fun confirmPin(input: String): Boolean {
        val ok = settings.value.parentalPinHash.isBlank() ||
            settings.value.parentalPinHash == ProfileRepository.hashPin(input)
        if (ok) {
            pinChallenge.value?.invoke()
            pinChallenge.value = null
        }
        return ok
    }

    suspend fun nowOn(channelId: String): EpgEntity? = catalog.nowEpg(channelId, epgClock())

    suspend fun nowMap(ids: List<String>): Map<String, EpgEntity> {
        if (ids.isEmpty()) return emptyMap()
        return catalog.nowEpgMany(ids, epgClock()).associateBy { it.channelId }
    }
    suspend fun guideFor(channelId: String, from: Long, to: Long) = catalog.epgWindow(channelId, from + epgShift(), to + epgShift())

    fun playCatchup(ch: ChannelEntity, programme: EpgEntity) = viewModelScope.launch {
        val src = catalog.source(ch.sourceId) ?: activeSource.value ?: return@launch
        if (src.kind != "XTREAM") {
            toast.value = strings.catchupXtreamRequired
            return@launch
        }
        val s = settings.value
        val url = CatchupUrl.build(
            source = src,
            streamId = ch.remoteId,
            programme = programme,
            preset = s.catchupPreset,
            custom = s.catchupTemplate,
            deviceTz = s.catchupTimezoneDevice
        )
        if (url.isNullOrBlank()) {
            toast.value = strings.catchupOff
            return@launch
        }
        playback.value = Playback(
            url = url,
            title = ch.name,
            subtitle = programme.title,
            poster = ch.logo,
            mediaId = ch.id,
            kind = "LIVE",
            live = false,
            sourceId = ch.sourceId,
            hasArchive = true,
            remoteId = ch.remoteId,
            channelNumber = ch.number
        )
        overlay.value = Overlay.NONE
        dest.value = Dest.PLAYER
        startPlayback(url)
    }

    fun rewindLive(deltaMs: Long) {
        val pb = playback.value ?: return
        if (!pb.live || !pb.hasArchive) {
            player.seekBy(deltaMs)
            return
        }
        viewModelScope.launch {
            val src = catalog.source(pb.sourceId) ?: return@launch
            if (src.kind != "XTREAM") {
                player.seekBy(deltaMs)
                return@launch
            }
            val startMs = System.currentTimeMillis() + deltaMs
            val fmt = SimpleDateFormat("yyyy-MM-dd:HH-mm", Locale.US)
            fmt.timeZone = if (settings.value.catchupTimezoneDevice) TimeZone.getDefault() else TimeZone.getTimeZone("UTC")
            val url = catalog.timeshift(src, pb.remoteId, fmt.format(Date(startMs)), 120)
            playback.value = pb.copy(url = url, live = false)
            startPlayback(url, userAgent = pb.userAgent, referer = pb.referer)
        }
    }

    fun loadMoreLive() {
        if (liveCategoryId.value == "fav" || liveCategoryId.value == "recent") return
        if (liveChannels.value.size < liveLimit.value) return
        val total = liveTotal.value
        if (total > 0 && liveChannels.value.size >= total) return
        val cap = catalog.liveBrowseCap()
        if (liveLimit.value >= cap) return
        liveLimit.value = (liveLimit.value + catalog.livePageSize()).coerceAtMost(cap)
    }

    fun loadMoreMovies() {
        if (movieCategoryId.value in virtualVodCats) return
        if (movies.value.size < movieLimit.value) return
        val total = movieTotal.value
        if (total > 0 && movies.value.size >= total) return
        val cap = catalog.vodBrowseCap()
        if (movieLimit.value >= cap) return
        movieLimit.value = (movieLimit.value + catalog.vodPageSize()).coerceAtMost(cap)
    }

    fun loadMoreSeries() {
        if (seriesCategoryId.value in virtualVodCats) return
        if (series.value.size < seriesLimit.value) return
        val total = seriesTotal.value
        if (total > 0 && series.value.size >= total) return
        val cap = catalog.vodBrowseCap()
        if (seriesLimit.value >= cap) return
        seriesLimit.value = (seriesLimit.value + catalog.vodPageSize()).coerceAtMost(cap)
    }

    suspend fun layoutChannels(categoryId: String): List<ChannelEntity> {
        val src = activeSource.value ?: return emptyList()
        val list = catalog.listChannels(src.id, categoryId)
        val cap = catalog.liveBrowseCap()
        return if (list.size > cap) list.take(cap) else list
    }

    fun zapRelative(delta: Int) {
        val pb = playback.value ?: return
        if (pb.kind != "LIVE") return
        val list = liveChannels.value
        if (list.isEmpty()) return
        val baseIdx = if (pendingZapIndex in list.indices) pendingZapIndex else list.indexOfFirst { it.id == pb.mediaId }
        if (baseIdx < 0) return
        val step = if (settings.value.zapInvert) -delta else delta
        val targetIdx = (baseIdx + step).coerceIn(0, list.lastIndex)
        if (targetIdx == baseIdx && pendingZapIndex == targetIdx) return
        pendingZapIndex = targetIdx
        val next = list[targetIdx]
        
        // Update OSD title/poster immediately so UI feels instantaneous
        playback.value = pb.copy(
            title = next.name,
            subtitle = next.categoryName,
            poster = next.logo,
            mediaId = next.id,
            channelNumber = next.number
        )
        refreshNowNext(next.id)
        
        zapRelativeJob?.cancel()
        zapRelativeJob = viewModelScope.launch {
            delay(180)
            pendingZapIndex = -1
            actuallyPlayChannel(next)
        }
    }

    fun resumeItem(item: ProgressEntity) {
        viewModelScope.launch {
            when (item.kind) {
                "LIVE" -> catalog.channel(item.mediaId)?.let { playChannel(it) }
                "MOVIE" -> catalog.vodItem(item.mediaId)?.let { playVod(it, item.positionMs, force = true) }
                "EPISODE" -> {
                    val ep = catalog.episode(item.mediaId) ?: return@launch
                    val series = catalog.vodItem(ep.seriesId) ?: return@launch
                    playEpisode(ep, series, item.positionMs)
                }
            }
        }
    }

    fun queueDownload(item: VodEntity) = viewModelScope.launch {
        val p = activeProfile.value ?: return@launch
        catalog.queueDownload(p.id, item)
        toast.value = strings.downloadQueued
    }

    fun queueDownload(ep: EpisodeEntity, series: VodEntity) = viewModelScope.launch {
        val p = activeProfile.value ?: return@launch
        catalog.queueDownload(p.id, ep, series.poster)
        toast.value = strings.episodeDownloadQueued
    }

    fun deleteGroup(id: String) = viewModelScope.launch { catalog.deleteGroup(id) }

    fun setCatchupPreset(v: CatchupPreset) = viewModelScope.launch { settingsRepo.setCatchupPreset(v) }
    fun setCatchupTemplate(v: String) = viewModelScope.launch { settingsRepo.setCatchupTemplate(v) }
    fun setOsdSizeTier(v: Int) = viewModelScope.launch { settingsRepo.setOsdSizeTier(v) }
    fun setImageCleanDays(v: Int) = viewModelScope.launch { settingsRepo.setImageCleanDays(v) }
    fun setRecoveryWord(word: String) = viewModelScope.launch {
        settingsRepo.setPinRecoveryHash(if (word.isBlank()) "" else ProfileRepository.hashPin(word.trim().lowercase()))
    }
    fun recoverPin(word: String): Boolean {
        val hash = settings.value.pinRecoveryHash
        if (hash.isBlank() || hash != ProfileRepository.hashPin(word.trim().lowercase())) return false
        viewModelScope.launch {
            settingsRepo.setPin("")
            settingsRepo.setPinRecoveryHash("")
        }
        toast.value = strings.pinCleared
        return true
    }
    fun addGlobalEpg(code: String) = viewModelScope.launch {
        catalog.refreshBackupEpg(
            force = true,
            langFallback = settings.value.lang.code,
            extraCountries = setOf(code)
        )
        refreshEpgStats()
        toast.value = if (isEn()) "$code EPGShare01 backup loaded" else "$code EPGShare01 yedek yüklendi"
    }
    fun openTrailer() {
        val url = vodExtras.value.trailerUrl
        if (url.isBlank()) {
            toast.value = strings.noTrailer
            return
        }
        runCatching {
            app.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        }
    }

    private fun maybeCleanImages(s: RaySettings) {
        if (s.imageCleanDays <= 0) return
        val due = s.lastImageCleanMs + s.imageCleanDays * 86_400_000L
        if (System.currentTimeMillis() < due && s.lastImageCleanMs > 0) return
        viewModelScope.launch(Dispatchers.IO) {
            app.cacheDir.resolve("image_cache").deleteRecursively()
            settingsRepo.setLastImageClean(System.currentTimeMillis())
        }
    }

    fun playVodExternal(item: VodEntity) {
        val s = settings.value
        runCatching {
            app.startActivity(
                Intent(Intent.ACTION_VIEW).apply {
                    setDataAndType(Uri.parse(item.streamUrl), "video/*")
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    if (s.externalPlayerPackage.isNotBlank()) setPackage(s.externalPlayerPackage)
                }
            )
        }.onFailure {
            toast.value = strings.externalPlayerError
        }
    }

    fun openExternal() {
        val pb = playback.value ?: return
        val s = settings.value
        runCatching {
            app.startActivity(
                Intent(Intent.ACTION_VIEW).apply {
                    setDataAndType(Uri.parse(pb.url), "video/*")
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    if (s.externalPlayerPackage.isNotBlank()) setPackage(s.externalPlayerPackage)
                }
            )
            player.pause()
        }.onFailure {
            toast.value = strings.externalPlayerError
        }
    }

    fun exportBackup(uri: Uri) = viewModelScope.launch {
        runCatching { withContext(Dispatchers.IO) { backup.exportTo(uri) } }.onFailure { toast.value = it.message.orEmpty() }
            .onSuccess { toast.value = strings.backupSaved }
    }

    fun importBackup(uri: Uri) = viewModelScope.launch {
        runCatching { withContext(Dispatchers.IO) { backup.importFrom(uri) } }.onFailure { toast.value = it.message.orEmpty() }
            .onSuccess { toast.value = strings.backupRestored }
    }

    val lastCloudBackupTime = MutableStateFlow<Long?>(null)
    val cloudBackupSummary = MutableStateFlow<com.ray.iptv.data.firebase.CloudBackupSummary?>(null)
    val isCloudBusy = MutableStateFlow(false)
    val cloudProgressMsg = MutableStateFlow("")

    fun refreshCloudBackupTime() = viewModelScope.launch {
        val uid = account.value.uid.ifBlank { null } ?: return@launch
        val summary = withContext(Dispatchers.IO) { firebaseService.getCloudBackupSummary(uid) }
        cloudBackupSummary.value = summary
        lastCloudBackupTime.value = summary?.updatedAt
        checkAutoBackup()
    }


    fun setAutoBackupInterval(v: com.ray.iptv.data.repo.AutoBackupInterval) = viewModelScope.launch {
        settingsRepo.setAutoBackupInterval(v)
        checkAutoBackup()
    }

    fun checkAutoBackup() = viewModelScope.launch {
        val s = account.value
        val interval = settings.value.autoBackupInterval
        if (!s.signedIn || s.uid.isBlank() || interval == com.ray.iptv.data.repo.AutoBackupInterval.OFF) return@launch
        if (sources.value.isEmpty()) return@launch
        val last = settings.value.lastAutoBackupTime
        val now = System.currentTimeMillis()
        val requiredIntervalMs = interval.days * 86_400_000L
        if (now - last >= requiredIntervalMs) {
            val jsonStr = withContext(Dispatchers.IO) { backup.exportJson() }
            val res = withContext(Dispatchers.IO) { firebaseService.backupToCloud(s.uid, jsonStr, s.email) }
            res.onSuccess { time ->
                settingsRepo.setLastAutoBackupTime(now)
                lastCloudBackupTime.value = time
            }
        }
    }


    fun deleteCloudData() = viewModelScope.launch {
        val s = account.value
        val tr = settings.value.lang == AppLang.TR
        if (!s.signedIn || s.uid.isBlank()) return@launch
        isCloudBusy.value = true
        cloudProgressMsg.value = if (tr) "Bulut verisi siliniyor..." else "Deleting cloud data..."
        try {
            val res = withContext(Dispatchers.IO) { firebaseService.deleteCloudData(s.uid) }
            res.onSuccess {
                lastCloudBackupTime.value = null
                cloudBackupSummary.value = null
                toast.value = strings.cloudBackupDeleted
            }.onFailure {
                toast.value = strings.cloudBackupDeleteFailed
            }
        } finally {
            isCloudBusy.value = false
            cloudProgressMsg.value = ""
        }
    }

    fun deleteAccount() = viewModelScope.launch {
        val tr = settings.value.lang == AppLang.TR
        isCloudBusy.value = true
        cloudProgressMsg.value = if (tr) "Hesap ve bulut verisi siliniyor..." else "Deleting account and cloud data..."
        try {
            val res = withContext(Dispatchers.IO) { firebaseService.deleteAccount() }
            res.onSuccess {
                googleAuth.signOut()
                lastCloudBackupTime.value = null
                cloudBackupSummary.value = null
                toast.value = strings.accountDeleted
            }.onFailure {
                toast.value = strings.accountDeleteFailed
            }
        } finally {
            isCloudBusy.value = false
            cloudProgressMsg.value = ""
        }
    }



    // Speed Test
    val speedTestState = speedTestService.state
    fun startSpeedTest() = viewModelScope.launch {
        speedTestService.startTest(activeSource.value?.baseUrl)
    }
    fun stopSpeedTest() = speedTestService.stopTest()

    // Data Usage
    val dataUsageState = dataUsageService.state
    fun toggleDataSaver(enabled: Boolean) = dataUsageService.toggleDataSaver(enabled)
    fun resetDataUsage() = dataUsageService.resetStats()

    // Cloud Restore Diff & Preview
    val pendingCloudBackup = MutableStateFlow<com.ray.iptv.data.repo.BackupFile?>(null)
    val pendingCloudJson = MutableStateFlow<String?>(null)

    fun fetchCloudBackupForPreview() = viewModelScope.launch {
        val s = account.value
        if (!s.signedIn || (s.uid.isBlank() && s.email.isBlank())) return@launch
        isCloudBusy.value = true
        cloudProgressMsg.value = "Bulut yedeği indiriliyor..."
        val res = withContext(Dispatchers.IO) { firebaseService.restoreFromCloud(s.uid, s.email) }
        isCloudBusy.value = false
        res.onSuccess { jsonStr ->
            runCatching {
                val parsed = backup.parseBackup(jsonStr)
                pendingCloudJson.value = jsonStr
                pendingCloudBackup.value = parsed
            }.onFailure {
                toast.value = if (isEn()) "Invalid backup format" else "Yedek formatı okunamadı"
            }
        }.onFailure {
            toast.value = strings.cloudBackupNotFound
        }
    }

    fun applyCloudRestore(overwrite: Boolean) = viewModelScope.launch {
        val jsonStr = pendingCloudJson.value ?: return@launch
        isCloudBusy.value = true
        cloudProgressMsg.value = if (isEn()) (if (overwrite) "Overwriting..." else "Merging...") else (if (overwrite) "Üzerine yazılıyor..." else "Birleştiriliyor...")
        runCatching {
            withContext(Dispatchers.IO) { backup.importJsonWithMode(jsonStr, overwrite) }
        }.onSuccess {
            toast.value = if (isEn()) (if (overwrite) "Backup fully restored" else "Backup merged successfully") else (if (overwrite) "Yedek tamamen yüklendi" else "Yedek başarıyla birleştirildi")
            pendingCloudBackup.value = null
            pendingCloudJson.value = null
        }.onFailure {
            toast.value = it.message.orEmpty()
        }
        isCloudBusy.value = false
    }

    fun dismissCloudRestorePreview() {
        pendingCloudBackup.value = null
        pendingCloudJson.value = null
    }

    suspend fun fetchCloudBackupData(): Pair<com.ray.iptv.data.repo.BackupFile, String>? {
        val s = runCatching {
            withTimeoutOrNull(4000) {
                accounts.session.first { it.signedIn && it.uid.isNotBlank() }
            }
        }.getOrNull() ?: account.value

        val firebaseUser = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser
        val targetUid = when {
            s.signedIn && s.uid.isNotBlank() -> s.uid
            firebaseUser != null && firebaseUser.uid.isNotBlank() -> firebaseUser.uid
            else -> ""
        }
        val targetEmail = when {
            s.signedIn && s.email.isNotBlank() -> s.email
            firebaseUser != null && !firebaseUser.email.isNullOrBlank() -> firebaseUser.email!!
            else -> ""
        }
        if (targetUid.isBlank() && targetEmail.isBlank()) return null
        return withContext(Dispatchers.IO) {
            val res = firebaseService.restoreFromCloud(targetUid, targetEmail)
            res.getOrNull()?.let { jsonStr ->
                runCatching {
                    val parsed = backup.parseBackup(jsonStr)
                    Pair(parsed, jsonStr)
                }.getOrNull()
            }
        }
    }

    val isOnboardingRestoring = MutableStateFlow(false)
    val onboardingRestoreProgress = MutableStateFlow<String?>(null)

    fun handleGoogleSignInAndAutoRestore(data: Intent?) = viewModelScope.launch {
        isOnboardingRestoring.value = true
        val tr = settings.value.lang == AppLang.TR
        onboardingRestoreProgress.value = if (tr) "Google hesabı kontrol ediliyor..." else "Checking Google account..."
        try {
            val (acc, errCode) = googleAuth.extractAccountFromIntent(data)
            if (acc != null) {
                val ok = googleAuth.handleSignInResult(acc)
                if (ok) {
                    onboardingRestoreProgress.value = if (tr) "Bulut yedeği aranıyor..." else "Searching cloud backup..."
                    val backupData = fetchCloudBackupData()
                    if (backupData != null) {
                        val jsonStr = backupData.second
                        val restored = restoreOnboardingCloud(jsonStr) { stage ->
                            onboardingRestoreProgress.value = stage
                        }
                        if (restored) {
                            delay(600)
                            completeSetup()
                        } else {
                            toast.value = if (tr) "Bulut yedeği yüklenemedi. Lütfen oynatma listenizi girin." else "Failed to load cloud backup. Please enter your playlist."
                        }
                    } else {
                        syncToCloudOnComplete = true
                        toast.value = if (tr) "Google ile oturum açıldı. Bulutta kayıtlı yedek bulunamadı, lütfen oynatma listenizi girin." else "Signed in with Google. No backup found in cloud, please enter your playlist."
                    }
                } else {
                    toast.value = if (tr) "Google oturumu açılamadı." else "Google sign-in failed."
                }
            } else {
                val msg = when (errCode) {
                    10 -> if (isEn()) "Google Sign-In Error (Code 10): SHA-1 Fingerprint missing in Firebase Console." else "Google Giriş Hatası (Kod 10): Firebase Console'da SHA-1 Parmak İzi Eklenmemiş."
                    12500 -> if (isEn()) "Google Sign-In Error (Code 12500): Play Services error." else "Google Giriş Hatası (Kod 12500): Play Hizmetleri Güncellemesi Gerekebilir."
                    12501 -> if (isEn()) "Sign-in cancelled." else "Oturum açma iptal edildi."
                    null -> if (isEn()) "Sign-in cancelled." else "Oturum açma iptal edildi."
                    else -> if (isEn()) "Google Sign-In Error (Code $errCode)" else "Google Oturum Açma Hatası (Kod: $errCode)"
                }
                toast.value = msg
            }
        } catch (e: Exception) {
            Log.e("RayViewModel", "Onboarding Google restore error: ${e.message}", e)
            toast.value = if (tr) "Hata: ${e.message}. Lütfen listenizi girin." else "Error: ${e.message}. Please enter your playlist."
        } finally {
            isOnboardingRestoring.value = false
            onboardingRestoreProgress.value = null
        }
    }

    suspend fun restoreOnboardingCloud(
        jsonStr: String,
        onProgress: (String) -> Unit
    ): Boolean {
        val tr = settings.value.lang == AppLang.TR
        return try {
            onProgress(if (tr) "1/3 Ayarlar ve profiller yükleniyor..." else "1/3 Restoring settings & profiles...")
            withContext(Dispatchers.IO) {
                backup.importJsonWithMode(jsonStr, overwrite = true)
            }
            onProgress(if (tr) "2/3 Oynatma listeleri ve kanallar aktarılıyor..." else "2/3 Importing playlists and channels...")
            refresh()
            delay(500)
            onProgress(if (tr) "3/3 Katalog senkronize ediliyor..." else "3/3 Syncing catalog...")
            val sourceList = sources.value
            if (sourceList.isNotEmpty()) {
                val active = sourceList.firstOrNull { it.id == settings.value.activeSourceId } ?: sourceList.first()
                selectSource(active.id)
            }

            // EPG güncellemesini arka plana at (kullanıcı kurulumda ekstra beklemesin)
            viewModelScope.launch(Dispatchers.IO) {
                try {
                    val s = settings.value
                    if (s.epgEnabled) {
                        catalog.importAllXmltv(
                            includeFiles = s.epgSourceMode != EpgSourceMode.XTREAM && !s.xtreamEpgOnly,
                            includeXtream = s.epgSourceMode != EpgSourceMode.XMLTV,
                            includeGlobal = s.epgSourceMode != EpgSourceMode.XTREAM,
                            forceGlobal = false,
                            langFallback = s.lang.code
                        )
                        settingsRepo.setLastEpgRefresh(System.currentTimeMillis())
                        refreshEpgStats()
                    }
                } catch (e: Exception) {
                    Log.w("RayViewModel", "Background EPG refresh during restore: ${e.message}")
                }
            }

            catalog.acknowledgeSync()
            onProgress(if (tr) "✓ Kurulum ve geri yükleme başarıyla tamamlandı!" else "✓ Setup and restore completed successfully!")
            delay(400)
            true
        } catch (e: Exception) {
            toast.value = e.message ?: "Geri yükleme hatası"
            false
        }
    }

    fun triggerAutoCloudBackupIfSignedIn() {
        viewModelScope.launch {
            val s = runCatching {
                withTimeoutOrNull(2000) { accounts.session.first { it.signedIn && it.uid.isNotBlank() } }
            }.getOrNull() ?: account.value
            val firebaseUid = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid
            val targetUid = if (s.signedIn && s.uid.isNotBlank()) s.uid else firebaseUid.orEmpty()
            if (targetUid.isNotBlank()) {
                try {
                    val jsonStr = withContext(Dispatchers.IO) { backup.exportJson() }
                    withContext(Dispatchers.IO) { firebaseService.backupToCloud(targetUid, jsonStr) }
                    Log.d("RayViewModel", "Auto cloud backup finished successfully")
                } catch (e: Exception) {
                    Log.w("RayViewModel", "Auto cloud backup failed: ${e.message}")
                }
            }
        }
    }

    val selectedActor = MutableStateFlow<com.ray.iptv.data.meta.ActorProfileResult?>(null)
    val actorMatchedVods = MutableStateFlow<List<com.ray.iptv.data.local.VodEntity>>(emptyList())
    val isActorLoading = MutableStateFlow(false)

    fun openActorProfile(person: com.ray.iptv.data.meta.CastPerson) = viewModelScope.launch {
        isActorLoading.value = true
        selectedActor.value = null
        actorMatchedVods.value = emptyList()
        val result = withContext(Dispatchers.IO) { vodMeta.fetchActorProfile(person.name, person.tmdbPersonId) }
        selectedActor.value = result
        val matches = withContext(Dispatchers.IO) { catalog.findContentByActor(person.name, result.filmographyTitles) }
        actorMatchedVods.value = matches
        isActorLoading.value = false
    }

    fun openActorProfileByName(name: String) = openActorProfile(com.ray.iptv.data.meta.CastPerson(name = name))

    fun closeActorProfile() {
        selectedActor.value = null
        actorMatchedVods.value = emptyList()
        isActorLoading.value = false
    }

    val openSubtitlesResults = MutableStateFlow<List<com.ray.iptv.data.meta.OpenSubtitleResult>>(emptyList())
    val isOpenSubtitlesLoading = MutableStateFlow(false)

    fun searchOpenSubtitles(title: String, tmdbId: Int = 0, imdbId: String = "") = viewModelScope.launch {
        isOpenSubtitlesLoading.value = true
        openSubtitlesResults.value = emptyList()
        val lang = if (settings.value.lang == AppLang.TR) "tr" else "en"
        val results = openSubtitles.searchSubtitles(title, tmdbId, imdbId, lang)
        openSubtitlesResults.value = results
        isOpenSubtitlesLoading.value = false
    }

    fun downloadAndApplySubtitle(sub: com.ray.iptv.data.meta.OpenSubtitleResult) = viewModelScope.launch {
        toast.value = if (settings.value.lang == AppLang.TR) "Altyazı indiriliyor..." else "Downloading subtitle..."
        val file = openSubtitles.downloadSubtitle(sub.fileId)
        if (file != null && file.exists()) {
            toast.value = if (settings.value.lang == AppLang.TR) "Altyazı yüklendi!" else "Subtitle loaded!"
            val pb = playback.value
            if (pb != null) {
                playback.value = pb.copy(externalSubtitleUri = file.absolutePath)
            }
        } else {
            toast.value = if (settings.value.lang == AppLang.TR) "Altyazı indirilemedi" else "Failed to download subtitle"
        }
    }

    fun backupToCloud() = viewModelScope.launch {
        val s = runCatching {
            withTimeoutOrNull(2000) { accounts.session.first { it.signedIn && it.uid.isNotBlank() } }
        }.getOrNull() ?: account.value
        val firebaseUser = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser
        val targetUid = if (s.signedIn && s.uid.isNotBlank()) s.uid else firebaseUser?.uid.orEmpty()
        val targetEmail = if (s.signedIn && s.email.isNotBlank()) s.email else firebaseUser?.email.orEmpty()
        val tr = settings.value.lang == AppLang.TR
        if (targetUid.isBlank() && targetEmail.isBlank()) {
            toast.value = strings.signInGooglePrompt
            return@launch
        }
        isCloudBusy.value = true
        cloudProgressMsg.value = if (tr) "Verileriniz Google buluta yedekleniyor..." else "Backing up your data to Google cloud..."
        try {
            val jsonStr = withContext(Dispatchers.IO) { backup.exportJson() }
            val res = withContext(Dispatchers.IO) { firebaseService.backupToCloud(targetUid, jsonStr, targetEmail) }
            res.onSuccess { time ->
                lastCloudBackupTime.value = time
                val summary = withContext(Dispatchers.IO) { firebaseService.getCloudBackupSummary(targetUid) }
                cloudBackupSummary.value = summary
                toast.value = strings.cloudBackupSaved
            }.onFailure { err ->
                toast.value = (if (tr) "Bulut yedeği hatası: " else "Cloud backup error: ") + err.message
            }
        } finally {
            isCloudBusy.value = false
            cloudProgressMsg.value = ""
        }
    }

    suspend fun restoreFromCloud(): Boolean {
        val s = runCatching {
            withTimeoutOrNull(2000) { accounts.session.first { it.signedIn && it.uid.isNotBlank() } }
        }.getOrNull() ?: account.value
        val firebaseUser = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser
        val targetUid = if (s.signedIn && s.uid.isNotBlank()) s.uid else firebaseUser?.uid.orEmpty()
        val targetEmail = if (s.signedIn && s.email.isNotBlank()) s.email else firebaseUser?.email.orEmpty()
        val tr = settings.value.lang == AppLang.TR
        if (targetUid.isBlank() && targetEmail.isBlank()) {
            toast.value = strings.restoreGooglePrompt
            return false
        }
        isCloudBusy.value = true
        cloudProgressMsg.value = if (tr) "Yedekleriniz Google buluttan indiriliyor ve yükleniyor..." else "Downloading and restoring your backup from Google cloud..."
        return try {
            val res = withContext(Dispatchers.IO) { firebaseService.restoreFromCloud(targetUid, targetEmail) }
            var ok = false
            res.onSuccess { jsonStr ->
                withContext(Dispatchers.IO) { backup.importJson(jsonStr) }
                refresh()
                toast.value = strings.cloudBackupRestored
                ok = true
            }.onFailure { err ->
                if (err is NoSuchElementException || err.message == "NO_BACKUP") {
                    toast.value = if (tr) "Google ile oturum açıldı. Bulutta kayıtlı yedek bulunamadı." else "Signed in with Google. No backup found in cloud."
                } else {
                    toast.value = (if (tr) "Geri yükleme: " else "Restore: ") + (err.message ?: "Bulutta yedek bulunamadı")
                }
            }
            ok
        } finally {
            isCloudBusy.value = false
            cloudProgressMsg.value = ""
        }
    }


    fun addEpgSource(name: String, url: String) = viewModelScope.launch { catalog.addEpgSource(name, url) }
    fun removeEpgSource(id: String) = viewModelScope.launch { catalog.removeEpgSource(id) }
    fun applyEpgMatch(channelId: String, epgId: String) = viewModelScope.launch { catalog.applyEpgMatch(channelId, epgId) }
    fun autoMatchEpg() = viewModelScope.launch {
        var count = 0
        withContext(Dispatchers.IO) {
            val suggestions = catalog.suggestEpgMatches()
            count = suggestions.size
            suggestions.forEach { catalog.applyEpgMatch(it.channel.id, it.epgId) }
            catalog.importAllXmltv(
                includeFiles = settings.value.epgSourceMode != EpgSourceMode.XTREAM && !settings.value.xtreamEpgOnly,
                includeXtream = settings.value.epgSourceMode != EpgSourceMode.XMLTV,
                includeGlobal = settings.value.epgSourceMode != EpgSourceMode.XTREAM,
                forceGlobal = false,
                langFallback = settings.value.lang.code
            )
        }
        toast.value = String.format(java.util.Locale.getDefault(), strings.smartEpgApplied, count)
    }

    fun createGroup(name: String) = viewModelScope.launch {
        activeProfile.value?.let { catalog.createGroup(it.id, name) }
    }
    fun addToGroup(groupId: String, channelId: String) = viewModelScope.launch {
        catalog.addToGroup(groupId, channelId)
    }

    fun refreshNowNext(channelId: String) = viewModelScope.launch {
        nowNext.value = catalog.nowEpg(channelId, epgClock()) to catalog.nextEpg(channelId, epgClock())
    }

    private fun skipLivePreviewPlayer(): Boolean {
        return !settings.value.previewLive
    }

    fun hoverLive(ch: ChannelEntity?) {
        liveHoverJob?.cancel()
        if (ch == null) {
            browseNow.value = null
            browseUpcoming.value = emptyList()
            browsePreviewUrl.value = ""
            return
        }
        liveHoverJob = viewModelScope.launch {
            delay(180)
            if (!isActive) return@launch
            val clock = epgClock()
            val (now, upcoming) = withContext(Dispatchers.IO) {
                catalog.nowEpg(ch.id, clock) to
                    catalog.epgWindow(ch.id, clock - 30 * 60_000L, clock + 12 * 3600_000L)
                        .filter { it.endMs > clock }.take(8)
            }
            if (!isActive) return@launch
            browseNow.value = now
            browseUpcoming.value = upcoming
            if (!settings.value.previewLive) {
                browsePreviewUrl.value = ""
                return@launch
            }
            delay(2000)
            if (!isActive) return@launch
            val url = catalog.resolvePlayUrl(ch)
            if (!isActive) return@launch
            if (browsePreviewUrl.value != url) browsePreviewUrl.value = url
        }
    }

    fun loadListNow(ids: List<String>) {
        loadGuideSlots(ids)
    }

    fun loadGuideSlots(ids: List<String>) {
        val chunk = ids.take(60)
        if (chunk == lastGuideChunk) return
        lastGuideChunk = chunk
        guideSlotsJob?.cancel()
        guideSlotsJob = viewModelScope.launch {
            delay(260)
            if (!isActive) return@launch
            val clock = epgClock()
            val cal = java.util.Calendar.getInstance()
            cal.timeInMillis = clock
            cal.set(java.util.Calendar.MINUTE, 0)
            cal.set(java.util.Calendar.SECOND, 0)
            cal.set(java.util.Calendar.MILLISECOND, 0)
            val hourStart = cal.timeInMillis
            val times = listOf(clock, hourStart + 3_600_000L, hourStart + 7_200_000L)
            if (chunk.isEmpty()) {
                browseNowMap.value = emptyMap()
                browseGuideSlots.value = emptyMap()
                return@launch
            }
            val maps = withContext(Dispatchers.IO) {
                times.map { at -> catalog.nowEpgMany(chunk, at).associateBy { it.channelId } }
            }
            if (!isActive) return@launch
            browseNowMap.value = maps[0]
            browseGuideSlots.value = chunk.associateWith { id -> maps.map { it[id] } }
        }
    }

    private fun epgShift(): Long =
        settings.value.epgOffsetHours * 3_600_000L + settings.value.epgOffsetMinutes * 60_000L
    private fun epgClock(): Long = System.currentTimeMillis() + epgShift()

    private fun startPlayback(url: String, start: Long = 0L, userAgent: String = "", referer: String = "") {
        pendingNext.value = null
        val pb = playback.value
        prepareAndPlay(
            url = url,
            startMs = start,
            userAgent = userAgent,
            referer = referer,
            live = false,
            mediaId = pb?.mediaId.orEmpty()
        )
    }

    private fun prepareAndPlay(
        url: String,
        startMs: Long,
        userAgent: String,
        referer: String,
        live: Boolean,
        mediaId: String
    ) {
        val s = settings.value
        sessionLive = live
        sessionUrl = url
        sessionStart = startMs
        liveSessionBeganAt = if (live) System.currentTimeMillis() else 0L
        sessionUa = userAgent.ifBlank { settingsRepo.playbackUserAgent(s) }
        sessionRef = referer
        sessionId = mediaId
        fallbackStep = 0
        rememberedThisSession = false
        lastPlayError = ""
        sameUrlRetries = 0
        stallTicks = 0
        lastRecoverAt = 0L
        val mem = if (s.smartPlayerSelection) smartMemory()[mediaId] else null
        val engine = when (mem) {
            "mk", "mk-sw" -> PlaybackEngine.MEDIA_KIT
            "better" -> PlaybackEngine.BETTER
            else -> if (live) s.liveEngine else s.vodPlaybackEngine
        }
        val software = s.softwareDecoder || mem == "mk-sw"
        if (!live && s.externalPlayerEnabled) openExternal()
        applyEnginePlay(engine, s.effectiveStreamFormat(), software)
        armLiveWatch()
    }

    private fun applyEnginePlay(engine: PlaybackEngine, format: StreamFormat, software: Boolean) {
        val s = settings.value
        sessionEngine = engine
        sessionFormat = format
        sessionSoftware = software
        player.configure(
            software,
            s.liveBufferSeconds,
            s.subtitleAuto,
            live = sessionLive,
            lowEnd = s.lowEndMode,
            engine = engine,
            mediaKitLowPower = s.mediaKitLowPowerHwdec,
            ignoreSsl = s.ignoreSsl,
            subtitleSize = s.subtitleSize,
            subtitleOutline = s.subtitleOutline,
            subtitleColor = s.subtitleColor,
            subtitleFont = s.subtitleFont,
            preferredSubtitle = s.preferredSubtitleToken
        )
        player.play(
            sessionUrl,
            sessionStart,
            userAgent = sessionUa,
            referer = sessionRef,
            speed = s.speed,
            format = format
        )
    }

    /** Mina canlı kesinti zinciri: keep-alive → HLS↔TS → get.php/live → yazılım decoder → motor. */
    private fun retryPlaybackFallback(kind: PlayErrorKind = PlayErrorKind.SOURCE) {
        if (sessionUrl.isBlank()) return
        if (kind == PlayErrorKind.FORBIDDEN) return
        if (kind == PlayErrorKind.MISSING && !sessionLive) return
        if (!canRecoverNow()) return
        if (kind == PlayErrorKind.NETWORK && sessionLive && sameUrlRetries < 2 && fallbackStep == 0) {
            sameUrlRetries++
            applyEnginePlay(sessionEngine, sessionFormat, sessionSoftware)
            return
        }
        if (kind == PlayErrorKind.DECODER && !sessionSoftware && fallbackStep == 0) {
            fallbackStep = 3
            applyEnginePlay(sessionEngine, sessionFormat, software = true)
            return
        }
        fallbackStep++
        when (fallbackStep) {
            1 -> {
                val swapped = if (sessionLive) XtreamStreamUrls.swapLiveTsHls(sessionUrl) else null
                if (swapped != null && swapped != sessionUrl) {
                    sessionUrl = swapped
                    sessionFormat = when {
                        StreamHints.hls(swapped) -> StreamFormat.HLS
                        StreamHints.mpegTs(swapped) -> StreamFormat.TS
                        else -> sessionFormat
                    }
                    applyEnginePlay(sessionEngine, sessionFormat, sessionSoftware)
                } else {
                    val flipped = when (sessionFormat) {
                        StreamFormat.HLS -> StreamFormat.TS
                        StreamFormat.TS -> StreamFormat.HLS
                        StreamFormat.AUTO -> StreamFormat.TS
                    }
                    sessionFormat = flipped
                    applyEnginePlay(sessionEngine, flipped, sessionSoftware)
                }
            }
            2 -> {
                val path = if (sessionLive) XtreamStreamUrls.swapGetPhpAndLivePath(sessionUrl) else null
                if (path != null && path != sessionUrl) {
                    sessionUrl = path
                    applyEnginePlay(sessionEngine, sessionFormat, sessionSoftware)
                } else {
                    applyEnginePlay(sessionEngine, sessionFormat, software = true)
                }
            }
            3 -> applyEnginePlay(sessionEngine, sessionFormat, software = true)
            4 -> applyEnginePlay(
                if (sessionEngine == PlaybackEngine.BETTER) PlaybackEngine.MEDIA_KIT else PlaybackEngine.BETTER,
                sessionFormat,
                sessionSoftware
            )
            else -> {
                if (sessionLive) applyEnginePlay(PlaybackEngine.MEDIA_KIT, sessionFormat, software = true)
            }
        }
    }

    private fun recoverLiveKeepAlive() {
        if (!sessionLive || sessionUrl.isBlank()) return
        if (!canRecoverNow()) return
        if (sameUrlRetries < 2) {
            sameUrlRetries++
            applyEnginePlay(sessionEngine, sessionFormat, sessionSoftware)
            return
        }
        retryPlaybackFallback(PlayErrorKind.NETWORK)
    }

    private fun canRecoverNow(): Boolean {
        val now = System.currentTimeMillis()
        if (now - lastRecoverAt < 4_000L) return false
        lastRecoverAt = now
        return true
    }

    private fun armLiveWatch() {
        liveWatchJob?.cancel()
        if (!sessionLive) return
        liveWatchJob = viewModelScope.launch {
            stallTicks = 0
            while (isActive && sessionLive) {
                delay(2_000)
                val st = player.state.value
                if (!st.playWhenReady) {
                    stallTicks = 0
                    continue
                }
                if (st.playing && !st.buffering && st.error.isBlank()) {
                    stallTicks = 0
                    continue
                }
                if (st.error.isNotBlank()) continue
                if (st.ended) {
                    recoverLiveKeepAlive()
                    continue
                }
                if (st.buffering || !st.playing) {
                    stallTicks++
                    if (stallTicks >= 8) {
                        stallTicks = 0
                        recoverLiveKeepAlive()
                    }
                }
            }
        }
    }

    private fun rememberSmartEngineIfNeeded() {
        val s = settings.value
        if (!s.smartPlayerSelection || rememberedThisSession || sessionId.isBlank()) return
        if (sessionEngine != PlaybackEngine.MEDIA_KIT) return
        rememberedThisSession = true
        val tag = if (sessionSoftware) "mk-sw" else "mk"
        val map = smartMemory()
        map[sessionId] = tag
        val json = JSONObject()
        map.forEach { (k, v) -> json.put(k, v) }
        viewModelScope.launch { settingsRepo.setSmartPlayerMemory(json.toString()) }
    }

    private fun smartMemory(): MutableMap<String, String> {
        val out = mutableMapOf<String, String>()
        runCatching {
            val obj = JSONObject(settings.value.smartPlayerMemory.ifBlank { "{}" })
            obj.keys().forEach { key -> out[key] = obj.optString(key) }
        }
        return out
    }

    private fun playNextEpisode() {
        val pb = playback.value ?: return
        if (pb.kind != "EPISODE") return
        viewModelScope.launch {
            val ep = catalog.episode(pb.mediaId) ?: return@launch
            val next = catalog.nextEpisode(ep) ?: return@launch
            val series = catalog.vodItem(ep.seriesId) ?: return@launch
            playEpisode(next, series)
        }
    }

    private fun prepareNextEpisode() {
        viewModelScope.launch {
            val pb = playback.value ?: return@launch
            if (pb.kind != "EPISODE") return@launch
            val ep = catalog.episode(pb.mediaId) ?: return@launch
            val next = catalog.nextEpisode(ep) ?: return@launch
            pendingNext.value = NextUpPrompt(
                title = next.name.ifBlank { "S${next.season}E${next.episode}" },
                series = true
            )
        }
    }

    fun confirmNextEpisode() {
        pendingNext.value = null
        playNextEpisode()
    }

    fun cancelNextEpisode() {
        pendingNext.value = null
    }

    fun introTargetMs(): Long {
        val pb = playback.value ?: return 0L
        if (pb.live || pb.kind == "LIVE") return 0L
        if (pb.kind != "EPISODE" && pb.kind != "SERIES") return 0L
        val id = pb.seriesId.ifBlank { pb.mediaId }
        return introSkipMs[id] ?: 90_000L
    }

    fun learnIntroSkip(atMs: Long) {
        val pb = playback.value ?: return
        if (pb.live) return
        val id = pb.seriesId.ifBlank { pb.mediaId }
        if (atMs in 20_000..240_000) introSkipMs[id] = atMs
    }

    fun skipIntro() {
        val t = introTargetMs()
        if (t > 0) player.seek(t)
    }

    override fun onCleared() {
        super.onCleared()
        liveHoverJob?.cancel()
        zapRelativeJob?.cancel()
        guideSlotsJob?.cancel()
        previewJob?.cancel()
        searchJob?.cancel()
        player.release()
    }
}

private fun classifyEpgMix(title: String, channel: String, category: String): EpgMixKind? {
    val t = "$title $channel $category".lowercase()
    fun hits(needles: List<String>) = needles.count { t.contains(it) }
    val scores = listOf(
        EpgMixKind.SPORT to hits(
            listOf(
                "spor", "sport", "futbol", "football", "soccer", "basket", "nba", "maç", "mac ",
                " match", "vs.", " lig", "ucl", "premier", "tenis", "tennis", "voleybol", "nfl",
                "ufc", "boks", "boxing", "f1", "motogp", "hockey", "hentbol", "golf"
            )
        ),
        EpgMixKind.DOCUMENTARY to hits(
            listOf(
                "belgesel", "documentary", "discovery", "nat geo", "national geographic",
                "history channel", "animal planet", "da vinci", "love nature", "smithsonian"
            )
        ),
        EpgMixKind.SERIES to hits(
            listOf(
                "dizi", "series", "sezon", "season", "bölüm", "bolum", "episode", "ep.",
                "netflix", "disney+", "tabii", "exxen", "gain"
            )
        ),
        EpgMixKind.FILM to hits(
            listOf("film", "movie", "sinema", "cinema", "box office", "oscar", "hollywood", "premiere", "vod")
        ),
        EpgMixKind.NEWS to hits(
            listOf(
                "haber", "news", "gündem", "gundem", "bulletin", "cnn", "bbc", "ntv", "trt haber",
                "habertürk", "haberturk", "a haber", "breaking"
            )
        )
    )
    val best = scores.maxByOrNull { it.second } ?: return null
    return if (best.second > 0) best.first else null
}

private fun isSportProgramme(title: String, category: String, channel: String): Boolean {
    val t = "$title $category $channel".lowercase()
    return listOf(
        "maç", "match", " vs", "vs.", "spor", "sport", "futbol", "football", "basket", "nba",
        "lig", "ucl", "premier", "tenis", "tennis", "voleybol", "volleyball", "nfl", "ufc",
        "boks", "boxing", "f1", "motogp", "hockey", "hentbol", "golf", "şampiyonlar", "super lig"
    ).any { t.contains(it) }
}

private fun StartupScreen.toDest(): Dest = when (this) {
    StartupScreen.LIVE -> Dest.LIVE
    StartupScreen.MOVIES -> Dest.MOVIES
    StartupScreen.SERIES -> Dest.SERIES
    StartupScreen.GUIDE -> Dest.CATCHUP
    StartupScreen.HOME -> Dest.CONTINUE
}
