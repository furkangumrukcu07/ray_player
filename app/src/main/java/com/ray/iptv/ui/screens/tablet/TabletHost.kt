package com.ray.iptv.ui.screens.tablet

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import com.ray.iptv.ui.mobile.MobileDynamicSearchSheet
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LiveTv
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlaylistPlay
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Tv
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Icon
import androidx.tv.material3.Text
import coil.compose.AsyncImage
import com.ray.iptv.data.repo.AppLang
import com.ray.iptv.ui.Dest
import com.ray.iptv.ui.LiveBrowsePhase
import com.ray.iptv.ui.Overlay
import com.ray.iptv.ui.RayViewModel
import com.ray.iptv.ui.admin.AdminHost
import com.ray.iptv.ui.chat.ChatRoomsScreen
import com.ray.iptv.ui.glass.GlassPanel
import com.ray.iptv.ui.glass.RayWallpaper
import com.ray.iptv.ui.i18n.Copy
import com.ray.iptv.ui.motion.RaySwitch
import com.ray.iptv.ui.screens.catalog.SearchScreen
import com.ray.iptv.ui.screens.catalog.VodCinemaScreen
import com.ray.iptv.ui.screens.guide.GuideScreen
import com.ray.iptv.ui.screens.live.LiveScreen
import com.ray.iptv.ui.screens.player.RayPlayerRoute
import com.ray.iptv.ui.screens.playlists.PlaylistsScreen
import com.ray.iptv.ui.screens.settings.SettingsScreen
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.delay

@Composable
fun TabletHost(
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
    val activeSource by vm.activeSource.collectAsState()
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
    val livePhase by vm.livePhase.collectAsState()
    val moviePhase by vm.moviePhase.collectAsState()
    val seriesPhase by vm.seriesPhase.collectAsState()
    val extras by vm.vodExtras.collectAsState()
    val extrasId by vm.vodExtrasId.collectAsState()
    val browseNow by vm.browseNow.collectAsState()
    val browseUpcoming by vm.browseUpcoming.collectAsState()
    val browsePreviewUrl by vm.browsePreviewUrl.collectAsState()
    val browseNowMap by vm.browseNowMap.collectAsState()
    val browseGuideSlots by vm.browseGuideSlots.collectAsState()
    val zap by vm.zapBuffer.collectAsState()
    val epgSources by vm.epgSources.collectAsState()
    val sync by vm.catalog.sync.collectAsState()
    val contentFocusTrigger by vm.contentFocusTrigger.collectAsState()
    val account by vm.account.collectAsState()
    val q by vm.searchQuery.collectAsState()
    val sLive by vm.searchLive.collectAsState()
    val sMov by vm.searchMovies.collectAsState()
    val sSer by vm.searchSeries.collectAsState()
    val searchHist by vm.searchHistory.collectAsState()
    val searchBusy by vm.searchBusy.collectAsState()

    val tr = settings.lang == AppLang.TR
    val inPlayer = dest == Dest.PLAYER && playback != null

    com.ray.iptv.ui.input.ImmersivePlayback(active = inPlayer, hideSystemBars = inPlayer)

    BackHandler(enabled = !inPlayer) {

        when {
            overlay != Overlay.NONE -> vm.closeOverlay()
            dest == Dest.MOVIES && movie != null -> vm.closeDetail()
            dest == Dest.SERIES && show != null -> vm.closeDetail()
            dest == Dest.MOVIES && moviePhase == LiveBrowsePhase.CONTENT -> vm.backFromMovieContent()
            dest == Dest.SERIES && seriesPhase == LiveBrowsePhase.CONTENT -> vm.backFromSeriesContent()
            dest == Dest.LIVE && livePhase == LiveBrowsePhase.CONTENT -> vm.backFromLiveContent()
            dest != Dest.CONTINUE -> vm.go(Dest.CONTINUE)
            else -> requestExit()
        }
    }

    Box(Modifier.fillMaxSize()) {
        RayWallpaper()

        if (inPlayer) {
            RayPlayerRoute(vm, strings)
        } else {
            Row(Modifier.fillMaxSize()) {
                // 1. Left Vertical Navigation Rail
                TabletNavRail(
                    tr = tr,
                    currentDest = dest,
                    overlay = overlay,
                    profilePhotoUrl = account.photoUrl.takeIf { it.isNotBlank() && account.signedIn },
                    onGo = { target ->
                        if (overlay != Overlay.NONE) vm.closeOverlay()
                        vm.go(target)
                    },
                    onProfile = {
                        if (overlay != Overlay.NONE) vm.closeOverlay()
                        vm.go(Dest.SETTINGS)
                    }
                )


                // 2. Main Content Area + Top Header Bar
                Column(
                    Modifier
                        .fillMaxHeight()
                        .weight(1f)
                ) {
                    // Tablet Top Header Bar
                    TabletTopBar(
                        tr = tr,
                        activePlaylistName = activeSource?.name ?: (if (tr) "Aktif Liste" else "Active Playlist"),
                        isSyncing = sync.catalog,
                        onSearchClick = { vm.showOverlay(Overlay.SEARCH) },
                        onPlaylistClick = { vm.go(Dest.PLAYLISTS) }
                    )

                    // Destination Switcher
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .weight(1f)
                    ) {
                        RaySwitch(dest, Modifier.fillMaxSize(), effect = settings.pageTransitionEffect) { targetDest ->
                            when (targetDest) {
                                Dest.CONTINUE -> TabletHomeScreen(
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
                                    onExpandRail = { },
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
                                    railExpanded = false,
                                    contentFocusTrigger = contentFocusTrigger,
                                    onExit = { vm.go(Dest.CONTINUE) }
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
                                    onExpandRail = { },
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
                                    railExpanded = false,
                                    contentFocusTrigger = contentFocusTrigger,
                                    onExit = { vm.go(Dest.CONTINUE) }
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
                                    onExpandRail = { },
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
                                    railExpanded = false,
                                    contentFocusTrigger = contentFocusTrigger,
                                    onExit = { vm.go(Dest.CONTINUE) }
                                )
                                Dest.SETTINGS -> SettingsScreen(
                                    vm, settings, sources, profiles, epgSources, groups,
                                    visibleLiveCats, dls, strings,
                                    railExpanded = false,
                                    onExpandRail = { vm.go(Dest.CONTINUE) },
                                    onExit = { vm.go(Dest.CONTINUE) }
                                )
                                Dest.PLAYLISTS -> PlaylistsScreen(
                                    copy = strings,
                                    sources = sources,
                                    activeId = settings.activeSourceId,
                                    combineLists = settings.combineM3u,
                                    syncing = sync.catalog,
                                    onActivate = vm::selectSource,
                                    onToggle = vm::toggleSourceEnabled,
                                    onBackToRail = { vm.go(Dest.CONTINUE) },
                                    railExpanded = false,
                                    onExit = { vm.go(Dest.CONTINUE) }
                                )
                                Dest.ADMIN -> AdminHost(vm, tr) { vm.go(Dest.CONTINUE) }
                                Dest.CHAT -> ChatRoomsScreen(
                                    vm = vm,
                                    tr = tr,
                                    onExit = { vm.go(Dest.CONTINUE) }
                                )
                                else -> Unit
                            }
                        }
                    }
                }
            }
        }

        // Full Screen Overlays
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
    }
}

// -------------------------------------------------------------------------------------------------
// Tablet Left Vertical Navigation Rail
// -------------------------------------------------------------------------------------------------
@Composable
private fun TabletNavRail(
    tr: Boolean,
    currentDest: Dest,
    overlay: Overlay,
    profilePhotoUrl: String?,
    onGo: (Dest) -> Unit,
    onProfile: () -> Unit
) {
    val cyan = Color(0xFF22D3EE)
    val emerald = Color(0xFF34D399)

    Box(
        Modifier
            .fillMaxHeight()
            .width(80.dp)
            .background(Color(0xFF071510).copy(alpha = 0.94f))
            .border(width = 1.dp, color = emerald.copy(alpha = 0.20f), shape = RoundedCornerShape(topEnd = 24.dp, bottomEnd = 24.dp))
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(vertical = 10.dp)
    ) {
        Column(
            Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Top: Profile Avatar
            Box(
                Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF0F2D22))
                    .border(1.5.dp, cyan.copy(alpha = 0.70f), CircleShape)
                    .clickable { onProfile() },
                contentAlignment = Alignment.Center
            ) {
                if (!profilePhotoUrl.isNullOrBlank()) {
                    AsyncImage(
                        model = profilePhotoUrl,
                        contentDescription = "Profile",
                        contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Icon(
                        Icons.Filled.Person,
                        contentDescription = null,
                        tint = cyan,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }

            Spacer(Modifier.height(12.dp))

            // Navigation Tabs (Home, Live, Movies, Series, Playlists)
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(6.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                TabletNavIconItem(
                    icon = Icons.Filled.Home,
                    label = if (tr) "Ana Sayfa" else "Home",
                    selected = currentDest == Dest.CONTINUE && overlay == Overlay.NONE,
                    onClick = { onGo(Dest.CONTINUE) }
                )
                TabletNavIconItem(
                    icon = Icons.Filled.Tv,
                    label = if (tr) "Canlı TV" else "Live TV",
                    selected = currentDest == Dest.LIVE && overlay == Overlay.NONE,
                    onClick = { onGo(Dest.LIVE) }
                )
                TabletNavIconItem(
                    icon = Icons.Filled.Movie,
                    label = if (tr) "Filmler" else "Movies",
                    selected = currentDest == Dest.MOVIES && overlay == Overlay.NONE,
                    onClick = { onGo(Dest.MOVIES) }
                )
                TabletNavIconItem(
                    icon = Icons.Filled.LiveTv,
                    label = if (tr) "Diziler" else "Series",
                    selected = currentDest == Dest.SERIES && overlay == Overlay.NONE,
                    onClick = { onGo(Dest.SERIES) }
                )
                TabletNavIconItem(
                    icon = Icons.Filled.PlaylistPlay,
                    label = if (tr) "Listeler" else "Playlists",
                    selected = currentDest == Dest.PLAYLISTS && overlay == Overlay.NONE,
                    onClick = { onGo(Dest.PLAYLISTS) }
                )
            }

            Spacer(Modifier.height(6.dp))

            // Bottom: Settings (Always pinned and visible)
            TabletNavIconItem(
                icon = Icons.Filled.Settings,
                label = if (tr) "Ayarlar" else "Settings",
                selected = currentDest == Dest.SETTINGS && overlay == Overlay.NONE,
                onClick = { onGo(Dest.SETTINGS) }
            )
        }
    }
}


@Composable
private fun TabletNavIconItem(
    icon: ImageVector,
    label: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    val cyan = Color(0xFF22D3EE)

    Box(
        Modifier
            .size(52.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(
                if (selected) cyan.copy(alpha = 0.22f) else Color.Transparent
            )
            .border(
                width = if (selected) 1.5.dp else 0.dp,
                color = if (selected) cyan else Color.Transparent,
                shape = RoundedCornerShape(16.dp)
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                icon,
                contentDescription = label,
                tint = if (selected) cyan else Color.White.copy(alpha = 0.65f),
                modifier = Modifier.size(24.dp)
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = label,
                color = if (selected) cyan else Color.White.copy(alpha = 0.65f),
                fontSize = 9.sp,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

// -------------------------------------------------------------------------------------------------
// Tablet Top Bar Component
// -------------------------------------------------------------------------------------------------
@Composable
private fun TabletTopBar(
    tr: Boolean,
    activePlaylistName: String,
    isSyncing: Boolean,
    onSearchClick: () -> Unit,
    onPlaylistClick: () -> Unit
) {
    val cyan = Color(0xFF22D3EE)
    val emerald = Color(0xFF34D399)

    var currentTime by remember { mutableStateOf("") }
    LaunchedEffect(Unit) {
        val sdf = SimpleDateFormat("HH:mm", Locale.getDefault())
        while (true) {
            currentTime = sdf.format(Date())
            delay(1000)
        }
    }

    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Search Pill Bar
        Box(
            Modifier
                .width(360.dp)
                .height(44.dp)
                .clip(RoundedCornerShape(22.dp))
                .background(Color(0xFF0F261E).copy(alpha = 0.85f))
                .border(1.dp, Color.White.copy(alpha = 0.12f), RoundedCornerShape(22.dp))
                .clickable { onSearchClick() }
                .padding(horizontal = 16.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            Row(
                Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Filled.Search,
                        contentDescription = null,
                        tint = Color.White.copy(alpha = 0.6f),
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(10.dp))
                    Text(
                        text = if (tr) "İçerik, kanal veya film ara..." else "Search movies, shows, channels...",
                        color = Color.White.copy(alpha = 0.50f),
                        fontSize = 13.sp
                    )
                }
                Icon(
                    Icons.Filled.Mic,
                    contentDescription = "Voice",
                    tint = cyan,
                    modifier = Modifier.size(18.dp)
                )
            }
        }

        // Right side: Active Playlist Pill & Clock
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Active Playlist Pill
            Box(
                Modifier
                    .clip(RoundedCornerShape(16.dp))
                    .background(emerald.copy(alpha = 0.15f))
                    .border(1.dp, emerald.copy(alpha = 0.35f), RoundedCornerShape(16.dp))
                    .clickable { onPlaylistClick() }
                    .padding(horizontal = 12.dp, vertical = 7.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        if (isSyncing) Icons.Filled.CloudSync else Icons.Filled.CloudDone,
                        contentDescription = null,
                        tint = emerald,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = activePlaylistName,
                        color = Color.White,
                        fontSize = 12.5.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            // Digital Clock
            Text(
                text = currentTime,
                color = Color.White.copy(alpha = 0.90f),
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}
