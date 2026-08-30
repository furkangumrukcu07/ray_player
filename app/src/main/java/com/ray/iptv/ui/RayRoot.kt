package com.ray.iptv.ui

import android.app.Activity
import android.view.KeyEvent
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import androidx.compose.runtime.saveable.rememberSaveable
import com.ray.iptv.data.repo.AppLang
import com.ray.iptv.data.repo.LayoutMode
import com.ray.iptv.ui.components.GlassButton
import com.ray.iptv.ui.components.RaySplashScreen
import com.ray.iptv.ui.components.RayToastHost
import com.ray.iptv.ui.glass.DarkGlassPopupTheme
import com.ray.iptv.ui.glass.GlassPanel
import com.ray.iptv.ui.glass.RayWallpaper
import com.ray.iptv.ui.i18n.copy
import com.ray.iptv.ui.input.ImmersivePlayback
import com.ray.iptv.ui.input.LocalAdaptiveHaptics
import com.ray.iptv.ui.input.LocalTouchUi
import com.ray.iptv.ui.input.isTelevisionDevice
import com.ray.iptv.ui.input.rememberTouchUi
import com.ray.iptv.ui.mobile.MobileEpgMixScreen
import com.ray.iptv.ui.mobile.MobileHost
import com.ray.iptv.ui.mobile.MobileWrappedScreen
import com.ray.iptv.ui.motion.RayOverlay
import com.ray.iptv.ui.motion.RaySwitch
import com.ray.iptv.ui.screens.catalog.SearchScreen
import com.ray.iptv.ui.screens.catalog.VodCinemaScreen
import com.ray.iptv.ui.screens.guide.GuideScreen
import com.ray.iptv.ui.screens.home.HomeScreen
import com.ray.iptv.ui.screens.live.LiveScreen
import com.ray.iptv.ui.screens.onboarding.OnboardingFlow
import com.ray.iptv.ui.screens.player.RayPlayerRoute
import com.ray.iptv.ui.screens.playlists.PlaylistsScreen
import com.ray.iptv.ui.screens.settings.PlaylistLoadDialog
import com.ray.iptv.ui.screens.settings.SettingsScreen
import com.ray.iptv.ui.shell.RayShell
import com.ray.iptv.ui.theme.LocalGlass
import com.ray.iptv.ui.theme.RayTheme

@Composable
fun RayRoot(vm: RayViewModel = hiltViewModel()) {
    var showSplash by rememberSaveable { mutableStateOf(true) }
    val settings by vm.settings.collectAsState()
    val settingsReady by vm.settingsReady.collectAsState()
    val strings = copy(settings.lang)
    val touch = rememberTouchUi()
    val layout = if (settings.lang.rtl) LayoutDirection.Rtl else LayoutDirection.Ltr
    val context = LocalContext.current
    val socHints = remember(context) { com.ray.iptv.player.AndroidPlaybackSocHints.get(context) }
    val isTv = remember(context) { context.isTelevisionDevice() }
    val reduceEffects = settings.lowEndMode
    CompositionLocalProvider(
        LocalTouchUi provides touch,
        LocalLayoutDirection provides layout,
        LocalAdaptiveHaptics provides (settings.adaptiveHaptics && settings.layoutMode == LayoutMode.MOBILE)
    ) {
    RayTheme(
        settings.glass,
        reduceEffects = reduceEffects,
        fontKey = settings.appFontKey
    ) {
        val profiles by vm.profileList.collectAsState()
        val sources by vm.sources.collectAsState()
        val dest by vm.dest.collectAsState()
        val overlay by vm.overlay.collectAsState()
        val activity = context as? com.ray.iptv.MainActivity
        DisposableEffect(activity, dest, settings.pipMode, settings.layoutMode) {
            activity?.pipEligible = {
                dest == Dest.PLAYER && settings.pipMode && settings.layoutMode != LayoutMode.TV
            }
            onDispose {
                activity?.pipEligible = null
            }
        }
        val lifecycleOwner = LocalLifecycleOwner.current
        DisposableEffect(lifecycleOwner, dest, settings.backgroundPlayback, settings.pipMode) {
            val observer = LifecycleEventObserver { _, event ->
                val inPip = activity?.isInPictureInPictureMode == true
                if ((event == Lifecycle.Event.ON_PAUSE || event == Lifecycle.Event.ON_STOP) && !settings.backgroundPlayback && !inPip) {
                    vm.pauseFromBackground()
                } else if (event == Lifecycle.Event.ON_RESUME && !settings.backgroundPlayback) {
                    vm.resumeFromBackground()
                }
            }
            lifecycleOwner.lifecycle.addObserver(observer)
            onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
        }
        val playback by vm.playback.collectAsState()
        val profile by vm.activeProfile.collectAsState()
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
        val movieTotal by vm.movieTotal.collectAsState()
        val movieCounts by vm.movieCounts.collectAsState()
        val series by vm.series.collectAsState()
        val seriesTotal by vm.seriesTotal.collectAsState()
        val seriesCounts by vm.seriesCounts.collectAsState()
        val browseNow by vm.browseNow.collectAsState()
        val browseUpcoming by vm.browseUpcoming.collectAsState()
        val browsePreviewUrl by vm.browsePreviewUrl.collectAsState()
        val browseNowMap by vm.browseNowMap.collectAsState()
        val browseGuideSlots by vm.browseGuideSlots.collectAsState()
        val livePhase by vm.livePhase.collectAsState()
        val moviePhase by vm.moviePhase.collectAsState()
        val seriesPhase by vm.seriesPhase.collectAsState()
        val railExpanded by vm.railExpanded.collectAsState()
        val cont by vm.continueWatching.collectAsState()
        val recent by vm.recentLive.collectAsState()
        val favs by vm.favorites.collectAsState()
        val dls by vm.downloads.collectAsState()
        val groups by vm.groups.collectAsState()
        val movie by vm.selectedMovie.collectAsState()
        val show by vm.selectedSeries.collectAsState()
        val eps by vm.episodes.collectAsState()
        val liveCat by vm.liveCategoryId.collectAsState()
        val movieCat by vm.movieCategoryId.collectAsState()
        val seriesCat by vm.seriesCategoryId.collectAsState()
        val q by vm.searchQuery.collectAsState()
        val sLive by vm.searchLive.collectAsState()
        val sMov by vm.searchMovies.collectAsState()
        val sSer by vm.searchSeries.collectAsState()
        val searchHist by vm.searchHistory.collectAsState()
        val searchBusy by vm.searchBusy.collectAsState()
        val zap by vm.zapBuffer.collectAsState()
        val pin by vm.pinChallenge.collectAsState()
        val toast by vm.toast.collectAsState()
        val extras by vm.vodExtras.collectAsState()
        val extrasId by vm.vodExtrasId.collectAsState()
        val pendingNext by vm.pendingNext.collectAsState()
        val resume by vm.resumePrompt.collectAsState()
        val sync by vm.catalog.sync.collectAsState()
        val nowNext by vm.nowNext.collectAsState()
        val epgSources by vm.epgSources.collectAsState()
        val account by vm.account.collectAsState()
        val contentFocusTrigger by vm.contentFocusTrigger.collectAsState()
        val requestExit: () -> Unit = {
            if (vm.consumeExitBack()) {
                val act = context as? Activity
                act?.finishAffinity()
            }
        }
        val mobileLayout = settings.layoutMode == LayoutMode.MOBILE
        val tabletLayout = settings.layoutMode == LayoutMode.TABLET

        ImmersivePlayback(dest == Dest.PLAYER && playback != null && !mobileLayout)


        LaunchedEffect(playback?.mediaId) {
            playback?.takeIf { it.kind == "LIVE" }?.let { vm.refreshNowNext(it.mediaId) }
        }

        Box(
            Modifier
                .fillMaxSize()
                .onPreviewKeyEvent { ev ->
                    if (!settings.onboardingDone) return@onPreviewKeyEvent false
                    if (ev.nativeKeyEvent.action != KeyEvent.ACTION_DOWN) return@onPreviewKeyEvent false
                    vm.handleMappedKey(ev.nativeKeyEvent.keyCode)
                }
        ) {
        when {
            !settingsReady -> RayWallpaper()
            !settings.onboardingDone -> OnboardingFlow(vm, strings)
            dest == Dest.PLAYER && playback != null && !mobileLayout && !tabletLayout -> RayPlayerRoute(vm, strings)
            mobileLayout -> MobileHost(vm, strings, requestExit)
            tabletLayout -> com.ray.iptv.ui.screens.tablet.TabletHost(vm, strings, requestExit)
            else -> RayShell(

                current = dest,
                copy = strings,
                syncMessage = if (sync.catalog) "" else sync.message.ifBlank { sync.error },
                account = account,
                showLive = settings.railLive,
                showMovies = settings.railMovies,
                showSeries = settings.railSeries,
                showContinue = settings.railContinue,
                showPlaylists = settings.railPlaylists,
                showRepeat = settings.railRepeat,
                railExpanded = railExpanded,
                railHidden = (dest == Dest.MOVIES && (moviePhase == LiveBrowsePhase.CONTENT || movie != null)) ||
                    (dest == Dest.SERIES && (seriesPhase == LiveBrowsePhase.CONTENT || show != null)),
                searchSelected = overlay == Overlay.SEARCH,
                repeatSelected = dest == Dest.CATCHUP,
                onGo = vm::go,
                onSearch = { vm.showOverlay(Overlay.SEARCH) },
                onRepeat = { vm.go(Dest.CATCHUP) },
                onRailFocused = vm::expandRail,
                onToggleRail = vm::toggleRail
            ) {
                Box(Modifier.fillMaxSize()) {
                    RaySwitch(dest, Modifier.fillMaxSize()) { d ->
                    when (d) {
                        Dest.CONTINUE -> HomeScreen(
                            copy = strings,
                            continueWatching = if (settings.homeContinue) cont else emptyList(),
                            onResume = vm::resumeItem,
                            onDelete = vm::removeContinue,
                            onExpandRail = vm::expandRail,
                            railExpanded = railExpanded,
                            contentFocusTrigger = contentFocusTrigger,
                            onExit = requestExit
                        )
                        Dest.LIVE -> LiveScreen(
                            copy = strings,
                            categories = visibleLiveCats,
                            groups = groups,
                            channels = channels,
                            allChannels = channels,
                            allCount = liveTotal,
                            categoryCounts = liveCounts,
                            favorites = favs,
                            recent = recent,
                            selectedCategory = liveCat,
                            zap = zap,
                            preview = settings.previewLive,
                            now = browseNow,
                            upcoming = browseUpcoming,
                            previewUrl = browsePreviewUrl,
                            nowByChannel = browseNowMap,
                            guideSlots = browseGuideSlots,
                            showCategories = livePhase == LiveBrowsePhase.CATEGORIES,
                            onCategory = { vm.liveCategoryId.value = it },
                            onPickCategory = vm::enterLiveContent,
                            onBackToCategories = vm::backFromLiveContent,
                            onExpandRail = vm::expandRail,
                            onPlay = vm::playChannel,
                            onHover = vm::hoverLive,
                            onLoadNowMap = vm::loadListNow,
                            onPin = { vm.pinCat(it.id, !it.pinned) },
                            onHide = { vm.hideCat(it.id, !it.hidden) },
                            onLock = { vm.lockCat(it.id, !it.locked) },
                            onFav = { vm.toggleFav(it.id, "LIVE", true) },
                            onAddGroup = { gid, ch -> vm.addToGroup(gid, ch.id) },
                            onLoadMore = vm::loadMoreLive,
                            stripPrefix = settings.stripChannelPrefix,
                            railExpanded = railExpanded,
                            contentFocusTrigger = contentFocusTrigger,
                            onExit = requestExit
                        )
                        Dest.MOVIES -> VodCinemaScreen(
                            isSeries = false,
                            copy = strings,
                            categories = visibleMovieCats,
                            items = movies,
                            allItems = movies,
                            allCount = movieTotal,
                            categoryCounts = movieCounts,
                            selectedCategory = movieCat,
                            showCategories = moviePhase == LiveBrowsePhase.CATEGORIES && movie == null,
                            pinned = movie,
                            extras = extras,
                            extrasId = extrasId,
                            onPreview = vm::previewVod,
                            favorites = favs,
                            onCategory = { vm.movieCategoryId.value = it },
                            onPickCategory = vm::enterMovieContent,
                            onBackToCategories = vm::backFromMovieContent,
                            onExpandRail = vm::expandRail,
                            onOpen = vm::openMovie,
                            onClosePin = vm::closeDetail,
                            onPlay = vm::playVod,
                            onExternal = vm::playVodExternal,
                            onFav = { vm.toggleFav(it.id, "MOVIE") },
                            onDownload = vm::queueDownload,
                            onTrailer = vm::openTrailer,
                            downloads = dls,
                            onLoadMore = vm::loadMoreMovies,
                            onSearch = { vm.showOverlay(Overlay.SEARCH) },
                            railExpanded = railExpanded,
                            contentFocusTrigger = contentFocusTrigger,
                            onExit = requestExit
                        )
                        Dest.SERIES -> VodCinemaScreen(
                            isSeries = true,
                            copy = strings,
                            categories = visibleSeriesCats,
                            items = series,
                            allItems = series,
                            allCount = seriesTotal,
                            categoryCounts = seriesCounts,
                            selectedCategory = seriesCat,
                            showCategories = seriesPhase == LiveBrowsePhase.CATEGORIES && show == null,
                            pinned = show,
                            extras = extras,
                            extrasId = extrasId,
                            onPreview = vm::previewVod,
                            episodes = eps,
                            favorites = favs,
                            onCategory = { vm.seriesCategoryId.value = it },
                            onPickCategory = vm::enterSeriesContent,
                            onBackToCategories = vm::backFromSeriesContent,
                            onExpandRail = vm::expandRail,
                            onOpen = vm::openSeries,
                            onClosePin = vm::closeDetail,
                            onPlay = vm::playVod,
                            onExternal = vm::playVodExternal,
                            onPlayEpisode = { ep -> show?.let { vm.playEpisode(ep, it) } },
                            onFav = { vm.toggleFav(it.id, "SERIES") },
                            onDownload = vm::queueDownload,
                            onDownloadEpisode = { ep -> show?.let { vm.queueDownload(ep, it) } },
                            onTrailer = vm::openTrailer,
                            downloads = dls,
                            onLoadMore = vm::loadMoreSeries,
                            onSearch = { vm.showOverlay(Overlay.SEARCH) },
                            railExpanded = railExpanded,
                            contentFocusTrigger = contentFocusTrigger,
                            onExit = requestExit
                        )
                        Dest.PLAYLISTS -> PlaylistsScreen(
                            copy = strings,
                            sources = sources,
                            activeId = settings.activeSourceId,
                            combineLists = settings.combineM3u,
                            syncing = sync.running && !sync.catalog,
                            onActivate = vm::selectSource,
                            onToggle = vm::toggleSourceEnabled,
                            onBackToRail = vm::expandRail,
                            railExpanded = railExpanded,
                            contentFocusTrigger = contentFocusTrigger,
                            onExit = requestExit
                        )
                        Dest.SETTINGS -> SettingsScreen(
                            vm, settings, sources, profiles, epgSources, groups,
                            visibleLiveCats, dls, strings,
                            railExpanded = railExpanded,
                            contentFocusTrigger = contentFocusTrigger,
                            onExpandRail = vm::expandRail,
                            onExit = requestExit
                        )

                        Dest.CATCHUP -> com.ray.iptv.ui.screens.catchup.TvCatchupScreen(
                            copy = strings,
                            channels = channels,
                            loadProgrammes = { ch, startMs, endMs ->
                                vm.guideFor(ch.id, startMs, endMs)
                            },
                            onPlayCatchup = { ch, p -> vm.playCatchup(ch, p) },
                            onPlayLive = vm::playChannel,
                            onBackToRail = vm::expandRail,
                            railExpanded = railExpanded,
                            contentFocusTrigger = contentFocusTrigger,
                            time24h = settings.epg24h,
                            tr = settings.lang == AppLang.TR
                        )

                        Dest.PLAYER -> Unit
                        Dest.WRAPPED -> MobileWrappedScreen(vm, settings.lang == AppLang.TR) { vm.go(Dest.CONTINUE) }
                        Dest.EPG_MIX -> MobileEpgMixScreen(
                            vm = vm,
                            tr = settings.lang == AppLang.TR,
                            onBack = { vm.go(Dest.CONTINUE) },
                            onPlayLive = vm::playChannel,
                            onCatchup = { ch, p -> vm.playCatchup(ch, p) }
                        )
                        Dest.CHAT, Dest.ADMIN -> Unit
                        Dest.PAYWALL -> {
                            val licensingState by vm.licensingState.collectAsState()
                            com.ray.iptv.ui.screens.paywall.RayPaywallScreen(
                                licensingState = licensingState,
                                copy = strings,
                                onRedeemCode = vm::redeemLicenseCode,
                                onDismiss = { vm.go(Dest.CONTINUE) }
                            )
                        }
                    }
                    }
                }
            }
        }
                    RayOverlay(
                        visible = overlay == Overlay.SEARCH && settings.onboardingDone && !mobileLayout && !tabletLayout,
                        modifier = Modifier.fillMaxSize()
                    ) {
                        BackHandler { vm.closeOverlay() }
                        Box(Modifier.fillMaxSize()) {
                            Box(
                                Modifier
                                    .fillMaxSize()
                                    .background(Color.Black.copy(alpha = 0.54f))
                                    .clickable(
                                        indication = null,
                                        interactionSource = remember { MutableInteractionSource() }
                                    ) { vm.closeOverlay() }
                            )
                            GlassPanel(
                                strong = true,
                                radius = 14.dp,
                                modifier = Modifier
                                    .align(Alignment.Center)
                                    .fillMaxWidth(0.72f)
                                    .fillMaxHeight(0.8f)
                                    .padding(horizontal = 8.dp)
                            ) {
                                Box(Modifier.padding(16.dp)) {
                                    SearchScreen(
                                        copy = strings,
                                        query = q,
                                        live = sLive,
                                        movies = sMov,
                                        series = sSer,
                                        onQuery = vm::search,
                                        onLive = vm::pickSearchLive,
                                        onMovie = vm::pickSearchMovie,
                                        onSeries = vm::pickSearchSeries,
                                        recents = searchHist,
                                        searching = searchBusy,
                                        liveCats = visibleLiveCats,
                                        movieCats = visibleMovieCats,
                                        seriesCats = visibleSeriesCats,
                                        onRecent = { vm.search(it, immediate = true) },
                                        onRemoveRecent = vm::removeRecentSearch,
                                        onClearRecents = vm::clearSearchHistory
                                    )
                                }
                            }
                        }
                    }

                    RayToastHost(toast, Modifier.align(Alignment.Center))
                    DarkGlassPopupTheme {
                        if (pin != null) PinOverlay(cancel = strings.cancel, onSubmit = { vm.confirmPin(it) }, onCancel = { vm.pinChallenge.value = null })
                        resume?.let { (item, prog) ->
                            GlassPanel(strong = true, modifier = Modifier.align(Alignment.Center).padding(24.dp)) {
                                Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                    Text(item.name, color = LocalGlass.current.text, style = MaterialTheme.typography.headlineMedium)
                                    Text(
                                        strings.resumePrompt,
                                        color = LocalGlass.current.muted
                                    )
                                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                        GlassButton(strings.resumeButton) { vm.confirmResume(false) }
                                        GlassButton(strings.startOver) { vm.confirmResume(true) }
                                    }
                                }
                            }
                        }
                        PlaylistLoadDialog(
                            sync = sync,
                            tr = settings.lang == AppLang.TR,
                            onDismiss = { vm.catalog.acknowledgeSync() }
                        )
                    }
                }
            }
        }

        if (showSplash) {
            RaySplashScreen(
                tr = settings.lang == AppLang.TR,
                style = settings.splashStyle,
                onFinished = { showSplash = false }
            )
        }
    }

@Composable
private fun PinOverlay(cancel: String, onSubmit: (String) -> Boolean, onCancel: () -> Unit) {
    val g = LocalGlass.current
    var value by remember { mutableStateOf("") }
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        GlassPanel(strong = true, radius = 14.dp) {
            Column(Modifier.padding(28.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("PIN", style = MaterialTheme.typography.headlineMedium, color = g.text)
                BasicTextField(
                    value = value,
                    onValueChange = { value = it },
                    textStyle = MaterialTheme.typography.headlineLarge.copy(color = g.text),
                    cursorBrush = SolidColor(g.accent)
                )
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    GlassButton("OK") { onSubmit(value) }
                    GlassButton(cancel) { onCancel() }
                }
            }
        }
    }
}
