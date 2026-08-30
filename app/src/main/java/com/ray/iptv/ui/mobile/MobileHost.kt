package com.ray.iptv.ui.mobile

import android.content.res.Configuration
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Text
import com.ray.iptv.data.local.EpgEntity
import com.ray.iptv.data.repo.AppLang
import com.ray.iptv.data.repo.StreamFormat
import com.ray.iptv.ui.Dest
import com.ray.iptv.ui.LiveBrowsePhase
import com.ray.iptv.ui.Overlay
import com.ray.iptv.ui.RayViewModel
import com.ray.iptv.ui.admin.AdminHost
import com.ray.iptv.ui.chat.ChatRoomsScreen
import com.ray.iptv.ui.glass.GlassPanel
import com.ray.iptv.ui.glass.RayWallpaper
import com.ray.iptv.ui.i18n.Copy
import com.ray.iptv.ui.input.ImmersivePlayback
import com.ray.iptv.ui.input.rayHapticScroll
import com.ray.iptv.ui.motion.RayMobileOverscroll
import com.ray.iptv.ui.motion.RaySwitch
import com.ray.iptv.ui.motion.rayBounceOverscroll
import com.ray.iptv.ui.screens.catalog.SearchScreen
import com.ray.iptv.ui.screens.player.RayPlayerRoute
import com.ray.iptv.ui.screens.settings.SettingsScreen
import java.util.Calendar

@Composable
fun MobileHost(
    vm: RayViewModel,
    strings: Copy,
    requestExit: () -> Unit
) {
    val settings by vm.settings.collectAsState()
    val dest by vm.dest.collectAsState()
    val overlay by vm.overlay.collectAsState()
    val playback by vm.playback.collectAsState()
    val profile by vm.activeProfile.collectAsState()
    val profiles by vm.profileList.collectAsState()
    val sources by vm.sources.collectAsState()
    val liveCats by vm.liveCategories.collectAsState()
    val movieCats by vm.movieCategories.collectAsState()
    val seriesCats by vm.seriesCategories.collectAsState()
    val kidsProfile = profile?.isKids == true
    val visibleLiveCats = remember(liveCats, settings.hideAdult, settings.hideLocked, kidsProfile) {
        vm.visibleCats(liveCats)
    }
    val visibleMovieCats = remember(movieCats, settings.hideAdult, settings.hideLocked, kidsProfile) {
        vm.visibleCats(movieCats)
    }
    val visibleSeriesCats = remember(seriesCats, settings.hideAdult, settings.hideLocked, kidsProfile) {
        vm.visibleCats(seriesCats)
    }
    val channels by vm.liveChannels.collectAsState()
    val liveTotal by vm.liveTotal.collectAsState()
    val liveCounts by vm.liveCounts.collectAsState()
    val movies by vm.movies.collectAsState()
    val series by vm.series.collectAsState()
    val cont by vm.continueWatching.collectAsState()
    val recent by vm.recentLive.collectAsState()
    val favs by vm.favorites.collectAsState()
    val dls by vm.downloads.collectAsState()
    val groups by vm.groups.collectAsState()
    val movie by vm.selectedMovie.collectAsState()
    val show by vm.selectedSeries.collectAsState()
    val eps by vm.episodes.collectAsState()
    val liveCat by vm.liveCategoryId.collectAsState()
    val livePhase by vm.livePhase.collectAsState()
    val extras by vm.vodExtras.collectAsState()
    val extrasId by vm.vodExtrasId.collectAsState()
    val browseNowMap by vm.browseNowMap.collectAsState()
    val epgSources by vm.epgSources.collectAsState()
    val pendingNext by vm.pendingNext.collectAsState()
    val nowNext by vm.nowNext.collectAsState()
    val q by vm.searchQuery.collectAsState()
    val sLive by vm.searchLive.collectAsState()
    val sMov by vm.searchMovies.collectAsState()
    val sSer by vm.searchSeries.collectAsState()
    val searchHist by vm.searchHistory.collectAsState()
    val searchBusy by vm.searchBusy.collectAsState()
    val resume by vm.resumePrompt.collectAsState()
    val account by vm.account.collectAsState()
    val tr = settings.lang == AppLang.TR
    val inPlayer = dest == Dest.PLAYER && playback != null
    val liveWatch = inPlayer && playback?.kind == "LIVE"
    val vodWatch = inPlayer && playback?.kind != "LIVE"
    val landscape = LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE
    val landscapePlayer = inPlayer && landscape
    ImmersivePlayback(active = inPlayer, hideSystemBars = landscapePlayer)
    val activeSource by vm.activeSource.collectAsState()
    var showPlaylistPicker by remember { mutableStateOf(false) }
    val vodDetail = movie != null || show != null
    val showDock = dest == Dest.CONTINUE && !inPlayer && overlay == Overlay.NONE && !vodDetail
    val showHeader = (dest == Dest.CONTINUE || dest == Dest.LIVE || dest == Dest.MOVIES || dest == Dest.SERIES) && !inPlayer && overlay == Overlay.NONE && !vodDetail
    var seriesUi by remember { mutableStateOf(false) }
    val metaLoading by vm.vodMetaLoading.collectAsState()
    var epProgress by remember { mutableStateOf<Map<String, com.ray.iptv.data.local.ProgressEntity>>(emptyMap()) }
    var dayEpg by remember { mutableStateOf<List<EpgEntity>>(emptyList()) }
    var archiveDayOffset by remember { mutableStateOf(0) }
    val lastWatched = remember(recent, cont) {
        listOfNotNull(recent.firstOrNull(), cont.firstOrNull { it.kind != "LIVE" })
            .maxByOrNull { it.updatedAt }
    }
    LaunchedEffect(show?.id, eps.size) {
        val seriesId = show?.id
        epProgress = if (seriesId != null) vm.episodeProgress(seriesId) else emptyMap()
    }
    LaunchedEffect(playback?.mediaId, liveWatch, archiveDayOffset) {
        val id = playback?.mediaId
        if (liveWatch && !id.isNullOrBlank()) {
            val cal = Calendar.getInstance()
            cal.add(Calendar.DAY_OF_YEAR, archiveDayOffset)
            cal.set(Calendar.HOUR_OF_DAY, 0)
            cal.set(Calendar.MINUTE, 0)
            cal.set(Calendar.SECOND, 0)
            cal.set(Calendar.MILLISECOND, 0)
            val start = cal.timeInMillis
            dayEpg = vm.guideFor(id, start, start + 24L * 3_600_000)
        } else {
            dayEpg = emptyList()
            archiveDayOffset = 0
        }
    }
    LaunchedEffect(dest) {
        if (dest == Dest.SERIES) seriesUi = true
        if (dest == Dest.MOVIES) seriesUi = seriesUi && show != null
        if (dest == Dest.CONTINUE) {
            vm.movieCategoryId.value = "last50"
            vm.seriesCategoryId.value = "last50"
            vm.liveCategoryId.value = ""
        }
    }
    val openMessage: (() -> Unit)? = if (account.isAdmin) ({ vm.go(Dest.ADMIN) }) else null

    BackHandler(enabled = !landscapePlayer) {
        when {
            overlay != Overlay.NONE -> vm.closeOverlay()
            vodWatch || liveWatch -> vm.backFromPlayer()
            vodDetail -> vm.closeDetail()
            dest == Dest.LIVE && livePhase == LiveBrowsePhase.CONTENT -> vm.backFromLiveContent()
            dest == Dest.WRAPPED || dest == Dest.EPG_MIX || dest == Dest.LIVE || dest == Dest.MOVIES || dest == Dest.SERIES || dest == Dest.PLAYLISTS ->
                vm.go(Dest.CONTINUE)
            dest == Dest.CHAT || dest == Dest.SETTINGS || dest == Dest.ADMIN -> vm.go(Dest.CONTINUE)
            dest == Dest.CONTINUE -> requestExit()
            else -> vm.go(Dest.CONTINUE)
        }
    }

    val selectedActor by vm.selectedActor.collectAsState()
    val actorMatchedVods by vm.actorMatchedVods.collectAsState()
    val isActorLoading by vm.isActorLoading.collectAsState()

    if (showPlaylistPicker) {
        com.ray.iptv.ui.components.PlaylistPickerDialog(
            tr = tr,
            sources = sources,
            activeSourceId = activeSource?.id,
            onSelect = { vm.selectSource(it) },
            onDismiss = { showPlaylistPicker = false }
        )
    }

    val openSubtitlesResults by vm.openSubtitlesResults.collectAsState()
    val isOpenSubtitlesLoading by vm.isOpenSubtitlesLoading.collectAsState()
    var showOpenSubtitles by remember { mutableStateOf(false) }

    if (selectedActor != null || isActorLoading) {
        com.ray.iptv.ui.components.ActorDetailDialog(
            tr = tr,
            actor = selectedActor,
            loading = isActorLoading,
            matchedVods = actorMatchedVods,
            onSelectVod = { vod ->
                if (vod.kind == "SERIES") vm.openSeries(vod) else vm.openMovie(vod)
            },
            onDismiss = vm::closeActorProfile
        )
    }

    if (showOpenSubtitles || isOpenSubtitlesLoading) {
        com.ray.iptv.ui.components.OpenSubtitlesDialog(
            tr = tr,
            loading = isOpenSubtitlesLoading,
            results = openSubtitlesResults,
            onSelectSubtitle = { sub -> vm.downloadAndApplySubtitle(sub) },
            onDismiss = { showOpenSubtitles = false }
        )
    }

    RayMobileOverscroll {
    Box(
        Modifier
            .fillMaxSize()
            .then(if (dest == Dest.CONTINUE && !inPlayer) Modifier else Modifier.rayHapticScroll())
    ) {
        if (landscapePlayer) {
            RayPlayerRoute(vm, strings)
        } else {
        RayWallpaper(
            overlayTop = if (dest == Dest.SETTINGS || dest == Dest.ADMIN) 0.42f else 0.32f,
            overlayBottom = if (dest == Dest.SETTINGS || dest == Dest.ADMIN) 0.72f else 0.55f
        )
        Column(Modifier.fillMaxSize().statusBarsPadding()) {
            if (showHeader) {
                MobileTopBar(
                    tr = tr,
                    showBack = dest != Dest.CONTINUE,
                    onBack = {
                        if (dest == Dest.LIVE && livePhase == LiveBrowsePhase.CONTENT) {
                            vm.backFromLiveContent()
                        } else {
                            vm.go(Dest.CONTINUE)
                        }
                    },
                    onBrand = vm::refresh,
                    onSettings = { vm.go(Dest.SETTINGS) },
                    onSearch = if (dest == Dest.LIVE || dest == Dest.MOVIES || dest == Dest.SERIES) ({ vm.showOverlay(Overlay.SEARCH) }) else null,
                    onList = null,
                    onPlaylist = if (sources.size > 1) ({ showPlaylistPicker = true }) else null,
                    onChat = { vm.go(Dest.CHAT) },
                    avatarUrl = account.photoUrl.takeIf { it.isNotBlank() && account.signedIn }
                )
            }
            Box(
                Modifier
                    .weight(1f)
                    .clipToBounds()
                    .rayBounceOverscroll(enabled = !inPlayer)
            ) {
                if (liveWatch && playback != null) {
                    val pb = playback!!
                    val fmt = settings.streamFormat
                    val streamLabel = when {
                        fmt == StreamFormat.HLS || pb.url.contains("m3u8", true) -> "HLS"
                        fmt == StreamFormat.TS -> "TS"
                        else -> "HLS"
                    }
                    MobileLiveWatchScreen(
                        tr = tr,
                        playback = pb,
                        rayPlayer = vm.player,
                        aspect = settings.aspect,
                        channels = channels,
                        categories = visibleLiveCats,
                        selectedCategory = liveCat,
                        categoryCounts = liveCounts,
                        favorites = favs,
                        recent = recent,
                        nowByChannel = browseNowMap,
                        streamLabel = streamLabel,
                        playingId = pb.mediaId,
                        allCount = liveTotal,
                        onBack = vm::backFromPlayer,
                        onPlay = vm::playChannel,
                        onPausePlay = {
                            if (vm.player.state.value.playing) vm.player.pause() else vm.player.resume()
                        },
                        onRewind = vm::rewindLive,
                        onAspect = vm::cycleAspect,
                        onFav = { vm.toggleFav(pb.mediaId, "LIVE") },
                        favorite = favs.any { it.mediaId == pb.mediaId },
                        onCategory = { vm.liveCategoryId.value = it },
                        onLoadNowMap = vm::loadListNow,
                        onLoadMore = vm::loadMoreLive,
                        stripPrefix = settings.stripChannelPrefix,
                        nowTitle = nowNext.first?.title.orEmpty(),
                        nextTitle = nowNext.second?.title.orEmpty(),
                        dayProgrammes = dayEpg,
                        archiveDayOffset = archiveDayOffset,
                        onArchiveDayChange = { archiveDayOffset = it },
                        onCatchup = { p ->
                            channels.firstOrNull { it.id == pb.mediaId }?.let { vm.playCatchup(it, p) }
                        },
                        onToggleEngine = {
                            val newEng = if (vm.player.state.value.engine == com.ray.iptv.data.repo.PlaybackEngine.BETTER) com.ray.iptv.data.repo.PlaybackEngine.MEDIA_KIT else com.ray.iptv.data.repo.PlaybackEngine.BETTER
                            vm.setLiveEngine(newEng)
                            vm.toast.value = if (newEng == com.ray.iptv.data.repo.PlaybackEngine.MEDIA_KIT) "Motor: MediaKit (libmpv)" else "Motor: ExoPlayer (Better)"
                        }
                    )
                } else if (vodWatch && playback != null) {
                    val pb = playback!!
                    val favKind = when (pb.kind) {
                        "EPISODE", "SERIES" -> "SERIES"
                        else -> "MOVIE"
                    }
                    val favId = if (pb.kind == "EPISODE") pb.seriesId.ifBlank { pb.mediaId } else pb.mediaId
                    val watchExtras = if (extrasId == favId) extras else vm.seedVodMeta(show)
                    val watchEps = if (pb.kind == "EPISODE" && eps.all { it.seriesId == pb.seriesId }) eps else emptyList()
                    MobileVodWatchScreen(
                        tr = tr,
                        vm = vm,
                        playback = pb,
                        rayPlayer = vm.player,
                        aspect = settings.aspect,
                        movies = movies,
                        movieCats = visibleMovieCats,
                        favorites = favs,
                        continueWatching = cont,
                        extras = watchExtras,
                        selectedSeries = show,
                        episodes = watchEps,
                        pendingNext = pendingNext,
                        onBack = vm::backFromPlayer,
                        onPausePlay = {
                            if (vm.player.state.value.playing) vm.player.pause() else vm.player.resume()
                        },
                        onAspect = vm::cycleAspect,
                        onFav = { vm.toggleFav(favId, favKind) },
                        favorite = favs.any { it.mediaId == favId },
                        onPlayVod = vm::playVod,
                        onPlayEpisode = { ep, s -> vm.playEpisode(ep, s) },
                        onSkipIntro = vm::skipIntro,
                        onConfirmNext = vm::confirmNextEpisode,
                        onCancelNext = vm::cancelNextEpisode,
                        introTargetMs = vm.introTargetMs()
                    )
                } else {
                RaySwitch(dest, Modifier.fillMaxSize(), effect = settings.pageTransitionEffect) { targetDest ->
                    when (targetDest) {
                        Dest.SETTINGS -> SettingsScreen(
                            vm, settings, sources, profiles, epgSources, groups,
                            visibleLiveCats, dls, strings,
                            railExpanded = true,
                            onExpandRail = { vm.go(Dest.CONTINUE) },
                            onExit = { vm.go(Dest.CONTINUE) }
                        )
                        Dest.ADMIN -> AdminHost(vm, tr) { vm.go(Dest.CONTINUE) }
                        Dest.CHAT -> ChatRoomsScreen(vm, tr) { vm.go(Dest.CONTINUE) }
                        Dest.WRAPPED -> MobileWrappedScreen(vm, tr) { vm.go(Dest.CONTINUE) }
                        Dest.EPG_MIX -> MobileEpgMixScreen(
                            vm = vm,
                            tr = tr,
                            onBack = { vm.go(Dest.CONTINUE) },
                            onPlayLive = vm::playChannel,
                            onCatchup = { ch, p -> vm.playCatchup(ch, p) }
                        )
                        Dest.LIVE -> MobileLiveBrowseScreen(
                            tr = tr,
                            categories = visibleLiveCats,
                            channels = channels,
                            allCount = liveTotal,
                            categoryCounts = liveCounts,
                            favorites = favs,
                            recent = recent,
                            selectedCategory = liveCat,
                            nowByChannel = browseNowMap,
                            onCategory = {
                                vm.liveCategoryId.value = it
                                vm.enterLiveContent()
                            },
                            onPlay = vm::playChannel,
                            onLoadNowMap = vm::loadListNow,
                            onLoadMore = vm::loadMoreLive,
                            stripPrefix = settings.stripChannelPrefix,
                            pills = true,
                            startOnChannels = livePhase == LiveBrowsePhase.CONTENT,
                            onBrowseChannels = vm::enterLiveContent,
                            onBrowseCategories = vm::backFromLiveContent
                        )
                        Dest.MOVIES, Dest.SERIES -> {
                            MobileCinemaScreen(
                                tr = tr,
                                vm = vm,
                                seriesMode = targetDest == Dest.SERIES || seriesUi,
                                onSeriesMode = { on ->
                                    seriesUi = on
                                    vm.closeDetail()
                                    vm.go(if (on) Dest.SERIES else Dest.MOVIES)
                                },
                                onSearch = { vm.showOverlay(Overlay.SEARCH) },
                                movies = movies,
                                series = series,
                                movieCats = visibleMovieCats,
                                seriesCats = visibleSeriesCats,
                                favorites = favs,
                                continueWatching = cont,
                                onOpen = { item ->
                                    if (item.kind == "SERIES" || seriesUi) vm.openSeries(item) else vm.openMovie(item)
                                },
                                onPlay = { item ->
                                    if (item.kind == "SERIES" || seriesUi) vm.playSeriesDirect(item) else vm.playVod(item)
                                },
                                onFav = { item ->
                                    vm.toggleFav(item.id, if (item.kind == "SERIES" || seriesUi) "SERIES" else "MOVIE")
                                },
                                onResume = vm::resumeItem
                            )
                        }
                        else -> MobileHomeScreen(
                            tr = tr,
                            vm = vm,
                            settings = settings,
                            continueWatching = if (settings.homeContinue) cont else emptyList(),
                            movies = movies,
                            series = series,
                            movieCats = visibleMovieCats,
                            seriesCats = visibleSeriesCats,
                            favorites = favs,
                            recentLive = recent,
                            liveSample = channels.take(12),
                            onPlayVod = vm::playVod,
                            onPlaySeries = vm::playSeriesDirect,
                            onOpenMovie = { vm.openMovie(it, fromHome = true) },
                            onOpenSeries = { vm.openSeries(it, fromHome = true) },
                            onResume = vm::resumeItem,
                            onPlayLive = vm::playChannel
                        )
                    }
                }
                }
            }
        }
        if (showDock) {
            Box(
                Modifier
                    .align(Alignment.BottomCenter)
                    .navigationBarsPadding()
                    .padding(bottom = 4.dp)
            ) {
                MobileDockBar(
                    tr = tr,
                    current = dest,
                    lastLogo = lastWatched?.poster.orEmpty(),
                    onGo = { d ->
                        if (d == Dest.MOVIES) seriesUi = false
                        if (d == Dest.SERIES) seriesUi = true
                        vm.go(d)
                    },
                    onSearch = { vm.showOverlay(Overlay.SEARCH) },
                    onLastWatched = vm::playLastWatched,
                    onGuide = { vm.go(Dest.EPG_MIX) },
                    style = settings.dockbarStyle,
                    showLastWatched = settings.homeLastWatchedButton
                )
            }
        }
        AnimatedVisibility(
            visible = overlay == Overlay.SEARCH,
            enter = fadeIn(tween(200)),
            exit = fadeOut(tween(180))
        ) {
            MobileDynamicSearchSheet(
                query = q,
                searching = searchBusy,
                liveResults = sLive,
                movieResults = sMov,
                seriesResults = sSer,
                searchHistory = searchHist,
                tr = tr,
                onQueryChange = vm::search,
                onPlayLive = vm::pickSearchLive,
                onOpenMovie = vm::pickSearchMovie,
                onOpenSeries = vm::pickSearchSeries,
                onRecentClick = { vm.search(it, immediate = true) },
                onRemoveRecent = vm::removeRecentSearch,
                onClearHistory = vm::clearSearchHistory,
                onDismiss = vm::closeOverlay
            )
        }

        val detailIncoming = movie ?: show
        var detailPage by remember { mutableStateOf(detailIncoming) }
        androidx.compose.runtime.SideEffect {
            if (detailIncoming != null) detailPage = detailIncoming
        }
        AnimatedVisibility(
            visible = detailIncoming != null && !inPlayer,
            enter = slideInHorizontally(animationSpec = tween(320)) { it } + fadeIn(tween(180)),
            exit = slideOutHorizontally(animationSpec = tween(240)) { it } + fadeOut(tween(160)),
            modifier = Modifier.fillMaxSize()
        ) {
            val detailItem = detailIncoming ?: detailPage
            if (detailItem != null) {
                val kind = if (detailItem.kind == "SERIES") "SERIES" else "MOVIE"
                val pool = if (kind == "SERIES") series else movies
                val catMatches = pool.filter { it.categoryId == detailItem.categoryId && detailItem.categoryId.isNotBlank() && it.id != detailItem.id }
                val similar = if (catMatches.size >= 4) {
                    catMatches.take(16)
                } else {
                    val remaining = pool.filter { it.id != detailItem.id && !catMatches.contains(it) }
                    (catMatches + remaining).take(16)
                }
                val currentExtras = if (extrasId == detailItem.id) extras else vm.seedVodMeta(detailItem)
                val currentEpisodes = if (kind == "SERIES" && eps.all { it.seriesId == detailItem.id }) eps else emptyList()
                Box(Modifier.fillMaxSize().background(Color(0xFF0B0F14))) {
                    MobileVodDetailScreen(
                        tr = tr,
                        item = detailItem,
                        extras = currentExtras,
                        episodes = currentEpisodes,
                        favorite = favs.any { it.mediaId == detailItem.id },
                        downloaded = dls.any { it.mediaId == detailItem.id && it.status == "DONE" },
                        onBack = vm::closeDetail,
                        onFav = { vm.toggleFav(detailItem.id, kind) },
                        onPlay = {
                            if (kind == "SERIES") {
                                currentEpisodes.firstOrNull()?.let { ep -> vm.playEpisode(ep, detailItem) }
                            } else vm.playVod(detailItem)
                        },
                        onDownload = { vm.queueDownload(detailItem) },
                        onTrailer = vm::openTrailer,
                        onPlayEpisode = { ep -> vm.playEpisode(ep, detailItem) },
                        onDownloadEpisode = { ep -> vm.queueDownload(ep, detailItem) },
                        episodeProgress = epProgress,
                        downloadedEpisodeIds = dls.filter { it.status == "DONE" }.map { it.mediaId }.toSet(),
                        similar = similar,
                        onOpenSimilar = { other ->
                            if (other.kind == "SERIES") vm.openSeries(other) else vm.openMovie(other)
                        },
                        loading = metaLoading || extrasId != detailItem.id,
                        onActorClick = vm::openActorProfile
                    )
                }
            }
        }
        }
        resume?.let { (item, _) ->
            Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.45f)), contentAlignment = Alignment.Center) {
                Column(
                    Modifier
                        .padding(24.dp)
                        .background(MobileCard, RoundedCornerShape(18.dp))
                        .padding(20.dp)
                ) {
                    Text(item.name, color = Color.White)
                    Text(
                        if (tr) "Kaldığı yerden devam?" else "Resume from last position?",
                        color = Color.White.copy(alpha = 0.7f),
                        modifier = Modifier.padding(top = 8.dp, bottom = 12.dp)
                    )
                    Row {
                        Text(
                            if (tr) "Devam" else "Resume",
                            color = Color.Black,
                            modifier = Modifier
                                .background(MobileCyan, RoundedCornerShape(10.dp))
                                .clickable { vm.confirmResume(false) }
                                .padding(horizontal = 14.dp, vertical = 8.dp)
                        )
                        Spacer(Modifier.width(12.dp))
                        Text(
                            if (tr) "Baştan" else "Start over",
                            color = Color.White,
                            modifier = Modifier
                                .background(Color.White.copy(alpha = 0.12f), RoundedCornerShape(10.dp))
                                .clickable { vm.confirmResume(true) }
                                .padding(horizontal = 14.dp, vertical = 8.dp)
                        )
                    }
                }
            }
        }
    }
    }
}
