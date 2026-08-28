package com.ray.iptv.ui.mobile

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.animateScrollBy
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items as lazyItems
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Timer
import coil.size.Precision
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Icon
import androidx.tv.material3.Text
import kotlinx.coroutines.launch
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.ray.iptv.data.local.CategoryEntity
import com.ray.iptv.data.local.ChannelEntity
import com.ray.iptv.data.local.FavoriteEntity
import com.ray.iptv.data.local.ProgressEntity
import com.ray.iptv.data.local.VodEntity
import com.ray.iptv.data.repo.RaySettings
import com.ray.iptv.ui.RayViewModel
import com.ray.iptv.ui.ShowcaseEpgChip
import com.ray.iptv.ui.input.rayClickable
import com.ray.iptv.ui.motion.rayBounceOverscroll
import com.ray.iptv.ui.theme.LocalGlass
import com.ray.iptv.ui.theme.sectionGradient
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun MobileHomeScreen(
    tr: Boolean,
    vm: RayViewModel,
    settings: RaySettings,
    continueWatching: List<ProgressEntity>,
    movies: List<VodEntity>,
    series: List<VodEntity>,
    movieCats: List<CategoryEntity>,
    seriesCats: List<CategoryEntity>,
    favorites: List<FavoriteEntity>,
    recentLive: List<ProgressEntity>,
    liveSample: List<ChannelEntity>,
    onPlayVod: (VodEntity) -> Unit,
    onPlaySeries: (VodEntity) -> Unit,
    onOpenMovie: (VodEntity) -> Unit,
    onOpenSeries: (VodEntity) -> Unit,
    onResume: (ProgressEntity) -> Unit,
    onPlayLive: (ChannelEntity) -> Unit
) {
    var trendFilms by remember { mutableStateOf<List<VodEntity>>(emptyList()) }
    var trendSeries by remember { mutableStateOf<List<VodEntity>>(emptyList()) }
    var mixedFilms by remember { mutableStateOf<List<VodEntity>>(emptyList()) }
    var mixedSeries by remember { mutableStateOf<List<VodEntity>>(emptyList()) }
    var mixedLive by remember { mutableStateOf<List<ChannelEntity>>(emptyList()) }
    var favFilms by remember { mutableStateOf<List<VodEntity>>(emptyList()) }
    var favSeries by remember { mutableStateOf<List<VodEntity>>(emptyList()) }
    var favLive by remember { mutableStateOf<List<ChannelEntity>>(emptyList()) }
    var topFilms by remember { mutableStateOf<List<VodEntity>>(emptyList()) }
    var topSeries by remember { mutableStateOf<List<VodEntity>>(emptyList()) }
    var movieRows by remember { mutableStateOf<List<Pair<CategoryEntity, List<VodEntity>>>>(emptyList()) }
    var seriesRows by remember { mutableStateOf<List<Pair<CategoryEntity, List<VodEntity>>>>(emptyList()) }
    var upcoming by remember { mutableStateOf<List<ShowcaseEpgChip>>(emptyList()) }
    var matches by remember { mutableStateOf<List<ShowcaseEpgChip>>(emptyList()) }
    var matchesReady by remember { mutableStateOf(false) }
    var seeAllTitle by remember { mutableStateOf<String?>(null) }
    var seeAllKey by remember { mutableStateOf<String?>(null) }
    var seeAllKind by remember { mutableStateOf("MOVIE") }
    var seeAllItems by remember { mutableStateOf<List<VodEntity>>(emptyList()) }
    LaunchedEffect(settings.homeTrendFilms, settings.homeTrendSeries, settings.homeMixedFilms, settings.homeMixedSeries, settings.homeMixedLive, settings.homeFavoriteFilms, settings.homeFavoriteSeries, settings.homeFavorites, favorites, movieCats, seriesCats) {
        if (settings.homeTrendFilms) trendFilms = vm.showcaseTrend("MOVIE")
        if (settings.homeTrendSeries) trendSeries = vm.showcaseTrend("SERIES")
        if (settings.homeMixedFilms) mixedFilms = vm.showcaseMixed("MOVIE")
        if (settings.homeMixedSeries) mixedSeries = vm.showcaseMixed("SERIES")
        if (settings.homeMixedLive) mixedLive = vm.showcaseMixedLive()
        if (settings.homeFavoriteFilms || settings.homeFavorites) favFilms = vm.showcaseFavorites("MOVIE")
        if (settings.homeFavoriteSeries || settings.homeFavorites) favSeries = vm.showcaseFavorites("SERIES")
        if (settings.homeFavorites || settings.homeFavoriteFilms || settings.homeFavoriteSeries) favLive = vm.showcaseFavoriteLive()
        topFilms = vm.showcaseCategory("MOVIE", "popular", 20)
        topSeries = vm.showcaseCategory("SERIES", "popular", 20)
        if (settings.homeMovies) movieRows = vm.showcaseCategoryRows("MOVIE", movieCats)
        if (settings.homeSeries) seriesRows = vm.showcaseCategoryRows("SERIES", seriesCats)
    }
    val epgStats by vm.epgStats.collectAsState()
    LaunchedEffect(settings.homeUpcomingEpg, settings.homeUpcomingMatches, settings.lastEpgRefreshMs, favorites, epgStats.programmes, liveSample.size) {
        if (!settings.homeUpcomingMatches) {
            matches = emptyList()
            matchesReady = false
        }
        if (!settings.homeUpcomingEpg) upcoming = emptyList()
        while (true) {
            if (settings.homeUpcomingEpg) upcoming = vm.showcaseEpg(false)
            if (settings.homeUpcomingMatches) {
                matches = vm.showcaseEpg(true)
                matchesReady = true
            }
            delay(15_000)
        }
    }
    LaunchedEffect(seeAllKey, seeAllKind) {
        val key = seeAllKey ?: return@LaunchedEffect
        val loaded = vm.showcaseCategory(seeAllKind, key, 0)
        if (loaded.isNotEmpty()) seeAllItems = loaded
    }
    BackHandler(enabled = seeAllTitle != null) {
        seeAllTitle = null
        seeAllKey = null
    }
    if (seeAllTitle != null) {
        MobileSeeAllGrid(
            title = seeAllTitle.orEmpty(),
            tr = tr,
            series = seeAllKind == "SERIES",
            preserveOrder = seeAllKey == "last50",
            items = seeAllItems,
            favorites = favorites,
            onBack = { seeAllTitle = null; seeAllKey = null },
            onOpen = { item ->
                if (item.kind == "SERIES") onOpenSeries(item) else onOpenMovie(item)
            }
        )
        return
    }
    val hero = remember(movies, series) {
        buildList {
            val m = movies.take(10)
            val s = series.take(10)
            val n = maxOf(m.size, s.size)
            for (i in 0 until n) {
                if (i < m.size) add(m[i])
                if (i < s.size) add(s[i])
            }
        }.take(15)
    }
    val pager = rememberPagerState(pageCount = { hero.size.coerceAtLeast(1) })
    val favIds = remember(favorites) { favorites.mapTo(HashSet(favorites.size)) { it.mediaId } }
    val continueVod = remember(continueWatching) {
        continueWatching.filter { it.kind != "LIVE" }.take(16)
    }
    LaunchedEffect(hero.size) {
        if (hero.size < 2) return@LaunchedEffect
        while (true) {
            delay(6500)
            pager.animateScrollToPage((pager.currentPage + 1) % hero.size)
        }
    }
    val homeListState = rememberLazyListState()
    LaunchedEffect(Unit) {
        homeListState.scrollToItem(0, 0)
    }
    LaunchedEffect(hero.isNotEmpty()) {
        if (hero.isNotEmpty()) {
            homeListState.scrollToItem(0, 0)
        }
    }
    LazyColumn(
        state = homeListState,
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 150.dp)
    ) {
        item(key = "hero") {
            if (hero.isNotEmpty()) {
                Column {
                    HorizontalPager(
                        state = pager,
                        beyondViewportPageCount = 0,
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp)
                    ) { page ->
                        val item = hero[page]
                        val next = hero.getOrNull((page + 1) % hero.size)
                        MobileHeroCard(
                            title = item.name,
                            image = item.poster,
                            rating = item.rating,
                            year = item.year,
                            nextTitle = next?.name.orEmpty(),
                            nextImage = next?.poster.orEmpty(),
                            tr = tr,
                            onPlay = {
                                if (item.kind == "SERIES") onPlaySeries(item) else onPlayVod(item)
                            },
                            onSave = {
                                if (item.kind == "SERIES") onOpenSeries(item) else onOpenMovie(item)
                            },
                            onOpen = {
                                if (item.kind == "SERIES") onOpenSeries(item) else onOpenMovie(item)
                            }
                        )
                    }
                    Row(
                        Modifier.fillMaxWidth().padding(top = 10.dp),
                        horizontalArrangement = Arrangement.Center
                    ) {
                        repeat(hero.size) { i ->
                            val on = i == pager.currentPage
                            Box(
                                Modifier
                                    .padding(horizontal = 3.dp)
                                    .height(6.dp)
                                    .width(if (on) 18.dp else 6.dp)
                                    .clip(CircleShape)
                                    .background(if (on) MobileCyan else Color.White.copy(alpha = 0.28f))
                            )
                        }
                    }
                }
            }
        }
        if (settings.homeUpcomingEpg && upcoming.isNotEmpty()) {
            item(key = "upcoming-epg") {
                MobileSectionFrame {
                    Column {
                        MobileStripTitle(if (tr) "Sıradaki Yayınlar" else "Upcoming broadcasts", true)
                        UpcomingEpgRow(upcoming, tr) { vm.playLiveId(it.channelId) }
                    }
                }
            }
        }
        if (settings.homeContinue && continueVod.isNotEmpty()) {
            item(key = "continue") {
                MobileSectionFrame {
                    Column {
                        MobileStripTitle(if (tr) "İzlemeye Devam Et" else "Continue watching", true)
                        LazyRow(contentPadding = PaddingValues(horizontal = 14.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            itemsIndexed(continueVod, key = { _, it -> it.mediaId }) { _, item ->
                                MobilePosterCard(
                                    title = item.title,
                                    poster = item.poster,
                                    playOverlay = true,
                                    onClick = { onResume(item) }
                                )
                            }
                        }
                    }
                }
            }
        }
        if (settings.homeAiRecommendations) {
            item(key = "ai") {
                val recLive = liveSample.take(8)
                val recSeries = series.take(8)
                MobileSectionFrame {
                    Column {
                        MobileStripTitle(if (tr) "Ray AI: Senin İçin Önerilenler" else "Ray AI: Recommended for you", padded = true)
                        LazyRow(contentPadding = PaddingValues(horizontal = 14.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            itemsIndexed(recLive, key = { _, it -> "l-${it.id}" }) { _, ch ->
                                MobilePosterCard(
                                    title = ch.name,
                                    poster = ch.logo,
                                    live = true,
                                    tr = tr,
                                    onClick = { onPlayLive(ch) }
                                )
                            }
                            itemsIndexed(recSeries, key = { _, it -> "s-${it.id}" }) { _, item ->
                                MobilePosterCard(
                                    title = item.name,
                                    poster = item.poster,
                                    series = true,
                                    tr = tr,
                                    onClick = { onOpenSeries(item) }
                                )
                            }
                        }
                    }
                }
            }
        }
        val combinedFavsOn = settings.homeFavorites || settings.homeFavoriteFilms || settings.homeFavoriteSeries
        if (combinedFavsOn && (favFilms.isNotEmpty() || favSeries.isNotEmpty() || favLive.isNotEmpty())) {
            item(key = "favorites") {
                MobileSectionFrame {
                    Column {
                        MobileStripTitle(if (tr) "Favoriler" else "Favorites", true)
                        LazyRow(contentPadding = PaddingValues(horizontal = 14.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            itemsIndexed(favLive.take(12), key = { _, it -> "fav-l-${it.id}" }) { _, ch ->
                                MobilePosterCard(ch.name, ch.logo, live = true, tr = tr, heart = true, onClick = { onPlayLive(ch) })
                            }
                            itemsIndexed(favFilms.take(16), key = { _, it -> "fav-m-${it.id}" }) { _, item ->
                                MobilePosterCard(item.name, item.poster, rating = item.rating, heart = true, heartOutline = true, onClick = { onOpenMovie(item) })
                            }
                            itemsIndexed(favSeries.take(16), key = { _, it -> "fav-s-${it.id}" }) { _, item ->
                                MobilePosterCard(item.name, item.poster, rating = item.rating, series = true, tr = tr, heart = true, heartOutline = true, onClick = { onOpenSeries(item) })
                            }
                        }
                    }
                }
            }
        }
        if (settings.homeTrendFilms && trendFilms.isNotEmpty()) {
            item(key = "trend-films") {
                FramedVodStrip(if (tr) "Trend Filmler" else "Trending movies", trendFilms, favIds, onOpenMovie) {
                    seeAllTitle = if (tr) "Trend Filmler" else "Trending movies"
                    seeAllKind = "MOVIE"
                    seeAllKey = "trend"
                    seeAllItems = trendFilms
                }
            }
        }
        if (settings.homeTrendSeries && trendSeries.isNotEmpty()) {
            item(key = "trend-series") {
                FramedVodStrip(if (tr) "Trend Diziler" else "Trending series", trendSeries, favIds, onOpenSeries) {
                    seeAllTitle = if (tr) "Trend Diziler" else "Trending series"
                    seeAllKind = "SERIES"
                    seeAllKey = "trend"
                    seeAllItems = trendSeries
                }
            }
        }
        if (topFilms.isNotEmpty() && settings.homeMovies) {
            item(key = "top-films") {
                FramedVodStrip(if (tr) "En Çok Oy Alan Filmler" else "Top rated movies", topFilms, favIds, onOpenMovie) {
                    seeAllTitle = if (tr) "En Çok Oy Alan Filmler" else "Top rated movies"
                    seeAllKind = "MOVIE"
                    seeAllKey = "popular"
                    seeAllItems = topFilms
                }
            }
        }
        if (topSeries.isNotEmpty() && settings.homeSeries) {
            item(key = "top-series") {
                FramedVodStrip(if (tr) "En Çok Oy Alan Diziler" else "Top rated series", topSeries, favIds, onOpenSeries) {
                    seeAllTitle = if (tr) "En Çok Oy Alan Diziler" else "Top rated series"
                    seeAllKind = "SERIES"
                    seeAllKey = "popular"
                    seeAllItems = topSeries
                }
            }
        }
        if (settings.homeMixedFilms && mixedFilms.isNotEmpty()) {
            item(key = "mixed-films") {
                FramedVodStrip(if (tr) "Karışık Filmler" else "Mixed movies", mixedFilms, favIds, onOpenMovie) {
                    seeAllTitle = if (tr) "Karışık Filmler" else "Mixed movies"
                    seeAllKind = "MOVIE"
                    seeAllKey = "mixed"
                    seeAllItems = mixedFilms
                }
            }
        }
        if (settings.homeMixedSeries && mixedSeries.isNotEmpty()) {
            item(key = "mixed-series") {
                FramedVodStrip(if (tr) "Karışık Diziler" else "Mixed series", mixedSeries, favIds, onOpenSeries) {
                    seeAllTitle = if (tr) "Karışık Diziler" else "Mixed series"
                    seeAllKind = "SERIES"
                    seeAllKey = "mixed"
                    seeAllItems = mixedSeries
                }
            }
        }
        if (settings.homeMixedLive && mixedLive.isNotEmpty()) {
            item(key = "mixed-live") {
                MobileSectionFrame {
                    Column {
                        MobileStripTitle(if (tr) "Karışık Canlı TV" else "Mixed Live TV", true)
                        LazyRow(contentPadding = PaddingValues(horizontal = 14.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            itemsIndexed(mixedLive.take(20), key = { _, it -> "mix-live-${it.id}" }) { _, ch ->
                                MobilePosterCard(
                                    title = ch.name,
                                    poster = ch.logo,
                                    live = true,
                                    tr = tr,
                                    onClick = { onPlayLive(ch) }
                                )
                            }
                        }
                    }
                }
            }
        }
        if (settings.homeRecentLive && recentLive.isNotEmpty()) {
            item(key = "recent-live") {
                MobileSectionFrame {
                    Column {
                        MobileStripTitle(if (tr) "Son İzlenen Kanallar" else "Recently watched channels", true)
                        LazyRow(contentPadding = PaddingValues(horizontal = 14.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            itemsIndexed(recentLive.take(20), key = { _, it -> "rl-${it.mediaId}" }) { _, item ->
                                MobilePosterCard(item.title, item.poster, live = true, tr = tr, playOverlay = true, onClick = { onResume(item) })
                            }
                        }
                    }
                }
            }
        }
        if (settings.homeMovies && movies.isNotEmpty()) {
            item(key = "latest-movies") {
                FramedVodStrip(if (tr) "Son eklenen filmler" else "Latest movies", movies.take(20), favIds, onOpenMovie) {
                    seeAllTitle = if (tr) "Son eklenen filmler" else "Latest movies"
                    seeAllKind = "MOVIE"
                    seeAllKey = "last50"
                    seeAllItems = movies
                }
            }
        }
        if (settings.homeSeries && series.isNotEmpty()) {
            item(key = "latest-series") {
                FramedVodStrip(if (tr) "Son eklenen 50 dizi" else "Latest 50 series", series.take(20), favIds, onOpenSeries) {
                    seeAllTitle = if (tr) "Son eklenen diziler" else "Latest series"
                    seeAllKind = "SERIES"
                    seeAllKey = "last50"
                    seeAllItems = series
                }
            }
        }
        if (movieRows.isNotEmpty()) {
            lazyItems(movieRows, key = { "movie-${it.first.id}" }) { (cat, list) ->
                FramedVodStrip(cat.name.trim(), list, favIds, onOpenMovie, series = false) {
                    seeAllTitle = cat.name
                    seeAllKind = "MOVIE"
                    seeAllKey = cat.id
                    seeAllItems = list
                }
            }
        }
        if (seriesRows.isNotEmpty()) {
            lazyItems(seriesRows, key = { "series-${it.first.id}" }) { (cat, list) ->
                FramedVodStrip(cat.name.trim(), list, favIds, onOpenSeries, series = true) {
                    seeAllTitle = cat.name
                    seeAllKind = "SERIES"
                    seeAllKey = cat.id
                    seeAllItems = list
                }
            }
        }
        if (settings.homeUpcomingMatches && (!matchesReady || matches.isNotEmpty())) {
            item(key = "matches") {
                MobileSectionFrame {
                    Column {
                        MobileStripTitle(if (tr) "Sıradaki Maçlar" else "Upcoming matches", true)
                        if (!matchesReady && matches.isEmpty()) {
                            Text(
                                if (tr) "Yükleniyor…" else "Loading…",
                                color = Color.White.copy(alpha = 0.55f),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                                modifier = Modifier.padding(start = 14.dp, bottom = 10.dp)
                            )
                        }
                        if (matches.isEmpty()) {
                            UpcomingMatchesPlaceholderRow()
                        } else {
                            UpcomingMatchesRow(matches, tr) { vm.playLiveId(it.channelId) }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun FramedVodStrip(
    title: String,
    items: List<VodEntity>,
    favIds: Set<String>,
    onOpen: (VodEntity) -> Unit,
    series: Boolean = false,
    onSeeAll: () -> Unit
) {
    MobileSectionFrame {
        Column {
            MobileStripTitle(title, true, onSeeAll)
            LazyRow(contentPadding = PaddingValues(horizontal = 14.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                itemsIndexed(items.take(20), key = { _, it -> it.id }) { _, item ->
                    MobilePosterCard(
                        title = item.name,
                        poster = item.poster,
                        rating = item.rating,
                        series = series || item.kind == "SERIES",
                        heart = item.id in favIds,
                        heartOutline = true,
                        onClick = { onOpen(item) }
                    )
                }
            }
        }
    }
}

@Composable
fun MobileSeeAllGrid(
    title: String,
    items: List<VodEntity>,
    favorites: List<FavoriteEntity>,
    onBack: () -> Unit,
    onOpen: (VodEntity) -> Unit,
    tr: Boolean = true,
    series: Boolean = false,
    preserveOrder: Boolean = false,
    bottomPad: Dp = 24.dp
) {
    val letters = "ABCDEFGHIJKLMNOPQRSTUVWXYZ#"
    var query by remember { mutableStateOf("") }
    var searchOpen by remember { mutableStateOf(false) }
    var genre by remember { mutableStateOf<String?>(null) }
    var year by remember { mutableStateOf<String?>(null) }
    var minRating by remember { mutableStateOf<Double?>(null) }
    var openMenu by remember { mutableStateOf<String?>(null) }
    val gridState = rememberLazyGridState()
    val scope = rememberCoroutineScope()
    val heading = remember(title, series, tr) {
        val raw = title.trim()
        when {
            raw.startsWith("FILM:", true) || raw.startsWith("DİZİ:", true) || raw.startsWith("SERIES:", true) -> raw
            series -> if (tr) "DİZİ: $raw" else "SERIES: $raw"
            else -> "FILM: $raw"
        }
    }
    val genres = remember(items) {
        items.flatMap { seeAllGenres(it) }.distinct().sorted()
    }
    val years = remember(items) {
        items.map { seeAllYear(it) }.filter { it.isNotBlank() }.distinct().sortedDescending()
    }
    val filtered = remember(items, query, genre, year, minRating, preserveOrder, series) {
        val q = query.trim()
        val list = items.filter { item ->
            if (q.isNotBlank() && !item.name.contains(q, ignoreCase = true)) return@filter false
            if (genre != null && genre !in seeAllGenres(item)) return@filter false
            if (year != null && seeAllYear(item) != year) return@filter false
            if (minRating != null && seeAllRating(item) < minRating!!) return@filter false
            true
        }
        if (preserveOrder) list else list.sortedBy { it.name.trim().lowercase(Locale.getDefault()) }
    }
    val firstByLetter = remember(filtered, preserveOrder) {
        if (preserveOrder) emptyMap()
        else buildMap {
            filtered.forEachIndexed { i, item ->
                putIfAbsent(seeAllAzLetter(item.name), i)
            }
        }
    }
    Box(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize()) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(start = 6.dp, end = 8.dp, top = 4.dp, bottom = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.AutoMirrored.Filled.ArrowBack, null, tint = Color.White,
                    modifier = Modifier.size(44.dp).padding(10.dp).rayClickable(onBack)
                )
                if (searchOpen) {
                    BasicTextField(
                        value = query,
                        onValueChange = { query = it },
                        singleLine = true,
                        textStyle = TextStyle(color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.SemiBold),
                        cursorBrush = SolidColor(MobileCyan),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                        modifier = Modifier.weight(1f).padding(end = 8.dp),
                        decorationBox = { inner ->
                            Box {
                                if (query.isBlank()) {
                                    Text(if (tr) "Bu kategoride ara" else "Search in category", color = Color.White.copy(alpha = 0.4f), fontSize = 15.sp)
                                }
                                inner()
                            }
                        }
                    )
                    Icon(
                        Icons.Filled.Close, null, tint = Color.White,
                        modifier = Modifier.size(44.dp).padding(10.dp).rayClickable(onClick = { query = ""; searchOpen = false })
                    )
                } else {
                    Text(
                        heading,
                        color = Color.White,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 17.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f).padding(end = 8.dp)
                    )
                    Box(
                        Modifier
                            .size(40.dp)
                            .rayClickable(onClick = { searchOpen = true }),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Filled.Search, null, tint = Color.White, modifier = Modifier.size(24.dp))
                        if (query.isNotBlank()) {
                            Box(
                                Modifier.align(Alignment.TopEnd).padding(6.dp).size(8.dp).clip(CircleShape).background(MobileCyan)
                            )
                        }
                    }
                }
            }
            Row(
                Modifier.fillMaxWidth().padding(start = 14.dp, end = 14.dp, top = 4.dp, bottom = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                SeeAllFilterChip(
                    label = if (genre == null) (if (tr) "Tür" else "Genre") else genre!!,
                    active = genre != null,
                    modifier = Modifier.weight(1f)
                ) { openMenu = if (openMenu == "genre") null else "genre" }
                SeeAllFilterChip(
                    label = year ?: if (tr) "Yıl" else "Year",
                    active = year != null,
                    modifier = Modifier.weight(1f)
                ) { openMenu = if (openMenu == "year") null else "year" }
                SeeAllFilterChip(
                    label = if (minRating == null) (if (tr) "Puan" else "Rating") else "IMDb ${minRating!!.toInt()}+",
                    active = minRating != null,
                    modifier = Modifier.weight(1f)
                ) { openMenu = if (openMenu == "rating") null else "rating" }
            }
            Box(
                Modifier
                    .weight(1f)
                    .clipToBounds()
                    .rayBounceOverscroll()
            ) {
                if (filtered.isEmpty()) {
                    Text(
                        if (tr) "Sonuç yok" else "No results",
                        color = Color.White.copy(alpha = 0.55f),
                        modifier = Modifier.align(Alignment.Center)
                    )
                } else {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(3),
                        state = gridState,
                        contentPadding = PaddingValues(start = 12.dp, end = 28.dp, top = 4.dp, bottom = bottomPad),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(filtered, key = { it.id }) { item ->
                            MobilePosterCard(
                                title = item.name,
                                poster = item.poster,
                                rating = item.rating,
                                modifier = Modifier.fillMaxWidth(),
                                series = series || item.kind == "SERIES",
                                heart = favorites.any { it.mediaId == item.id },
                                heartOutline = true,
                                onClick = { onOpen(item) }
                            )
                        }
                    }
                }
                if (!preserveOrder && filtered.isNotEmpty()) {
                    SeeAllAzBar(
                        letters = letters,
                        present = firstByLetter.keys,
                        modifier = Modifier.align(Alignment.CenterEnd).fillMaxHeight().padding(vertical = 6.dp, horizontal = 2.dp),
                        onLetter = { letter ->
                            firstByLetter[letter]?.let { idx ->
                                scope.launch { gridState.scrollToItem(idx) }
                            }
                        }
                    )
                }
            }
        }
        if (openMenu != null) {
            val options = when (openMenu) {
                "genre" -> listOf(null to if (tr) "Tüm Türler" else "All genres") + genres.map { it to it }
                "year" -> listOf(null to if (tr) "Tüm Yıllar" else "All years") + years.map { it to it }
                else -> listOf(null to if (tr) "Tüm Puanlar" else "All ratings") +
                    listOf(5.0, 6.0, 7.0, 8.0, 9.0).map { it.toString() to "IMDb ${it.toInt()}+" }
            }
            Box(
                Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.45f)).rayClickable(onClick = { openMenu = null })
            )
            Column(
                Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 108.dp, start = 20.dp, end = 20.dp)
                    .widthIn(max = 360.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(MobileCard)
                    .border(1.dp, Color.White.copy(alpha = 0.16f), RoundedCornerShape(16.dp))
                    .heightIn(max = 360.dp)
                    .verticalScroll(rememberScrollState())
                    .padding(vertical = 6.dp)
            ) {
                options.forEach { (value, label) ->
                    val on = when (openMenu) {
                        "genre" -> genre == value
                        "year" -> year == value
                        else -> minRating?.toString() == value
                    }
                    Text(
                        label,
                        color = if (on) MobileCyan else Color.White,
                        fontWeight = if (on) FontWeight.Bold else FontWeight.Medium,
                        modifier = Modifier
                            .fillMaxWidth()
                            .rayClickable(onClick = {
                                when (openMenu) {
                                    "genre" -> genre = value
                                    "year" -> year = value
                                    else -> minRating = value?.toDoubleOrNull()
                                }
                                openMenu = null
                            })
                            .padding(horizontal = 16.dp, vertical = 12.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun SeeAllFilterChip(label: String, active: Boolean, modifier: Modifier = Modifier, onClick: () -> Unit) {
    val shape = RoundedCornerShape(20.dp)
    Row(
        modifier
            .height(36.dp)
            .clip(shape)
            .background(Color.Black.copy(alpha = if (active) 0.28f else 0.22f))
            .border(1.dp, if (active) LocalGlass.current.accent.copy(alpha = 0.85f) else Color.White.copy(alpha = 0.18f), shape)
            .rayClickable(onClick)
            .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        Text(
            label,
            color = Color.White.copy(alpha = if (active) 1f else 0.88f),
            fontSize = 13.sp,
            fontWeight = if (active) FontWeight.Bold else FontWeight.Medium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f, fill = false)
        )
        Icon(
            Icons.Filled.ArrowDropDown,
            null,
            tint = Color.White.copy(alpha = 0.78f),
            modifier = Modifier.size(18.dp)
        )
    }
}

@Composable
private fun SeeAllAzBar(
    letters: String,
    present: Set<String>,
    modifier: Modifier = Modifier,
    onLetter: (String) -> Unit
) {
    var active by remember { mutableStateOf<String?>(null) }
    fun pick(y: Float, h: Int) {
        if (h <= 0) return
        val idx = (y / h * letters.length).toInt().coerceIn(0, letters.length - 1)
        val letter = letters[idx].toString()
        if (active != letter) {
            active = letter
            if (letter in present) onLetter(letter)
        }
    }
    Box(modifier) {
        Column(
            Modifier
                .fillMaxHeight()
                .width(22.dp)
                .pointerInput(present) {
                    detectTapGestures(
                        onPress = { offset ->
                            pick(offset.y, size.height)
                            tryAwaitRelease()
                            active = null
                        }
                    )
                }
                .pointerInput(present) {
                    detectVerticalDragGestures(
                        onDragEnd = { active = null },
                        onDragCancel = { active = null },
                        onVerticalDrag = { change, _ ->
                            pick(change.position.y, size.height)
                        }
                    )
                },
            verticalArrangement = Arrangement.SpaceEvenly,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            letters.forEach { ch ->
                val letter = ch.toString()
                val has = letter in present
                Text(
                    letter,
                    color = Color.White.copy(
                        alpha = when {
                            active == letter && has -> 0.95f
                            has -> 0.55f
                            else -> 0.22f
                        }
                    ),
                    fontSize = 9.sp,
                    fontWeight = if (active == letter) FontWeight.ExtraBold else FontWeight.SemiBold,
                    lineHeight = 9.sp
                )
            }
        }
        if (active != null && active in present) {
            Box(
                Modifier
                    .align(Alignment.CenterStart)
                    .offset(x = (-42).dp)
                    .size(46.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.94f)),
                contentAlignment = Alignment.Center
            ) {
                Text(active!!, color = Color(0xFF1A1A1A), fontSize = 22.sp, fontWeight = FontWeight.ExtraBold)
            }
        }
    }
}

private fun seeAllGenres(item: VodEntity): List<String> {
    val fromField = item.genre.split(',', '|', '/', ';').map { it.trim() }.filter { it.isNotBlank() }
    return fromField.ifEmpty { listOfNotNull(item.categoryName.trim().takeIf { it.isNotBlank() }) }
}

private fun seeAllYear(item: VodEntity): String {
    val y = item.year.trim().take(4)
    if (y.length == 4 && y.all { it.isDigit() }) return y
    return Regex("""(19|20)\d{2}""").find(item.name)?.value.orEmpty()
}

private fun seeAllRating(item: VodEntity): Double =
    item.rating.replace(',', '.').filter { it.isDigit() || it == '.' }.toDoubleOrNull() ?: 0.0

private fun seeAllAzLetter(name: String): String {
    val c = name.trim().firstOrNull()?.uppercaseChar() ?: return "#"
    val mapped = when (c) {
        'Ç' -> 'C'
        'Ğ' -> 'G'
        'İ', 'I', 'ı' -> 'I'
        'Ö' -> 'O'
        'Ş' -> 'S'
        'Ü' -> 'U'
        else -> c
    }
    return if (mapped in 'A'..'Z') mapped.toString() else "#"
}

@Composable
fun UpcomingEpgRow(items: List<ShowcaseEpgChip>, tr: Boolean, onPlay: (ShowcaseEpgChip) -> Unit) {
    val listState = rememberLazyListState()
    val userHeld = remember { mutableStateOf(false) }
    val resumeAt = remember { mutableLongStateOf(0L) }
    var nowMs by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(5_000)
            nowMs = System.currentTimeMillis()
        }
    }
    LaunchedEffect(items.size, listState) {
        if (items.size < 2) return@LaunchedEffect
        while (true) {
            delay(16)
            if (userHeld.value || System.currentTimeMillis() < resumeAt.value) continue
            if (listState.layoutInfo.totalItemsCount < 2) continue
            if (!listState.canScrollForward) {
                listState.scrollToItem(0)
            } else {
                listState.scrollBy(1.2f)
            }
        }
    }
    val fmt = remember { SimpleDateFormat("HH:mm", Locale.getDefault()) }
    val accent = LocalGlass.current.accent
    LazyRow(
        state = listState,
        modifier = Modifier
            .height(110.dp)
            .pointerInput(Unit) {
                awaitEachGesture {
                    awaitFirstDown(requireUnconsumed = false)
                    userHeld.value = true
                    do {
                        val event = awaitPointerEvent(PointerEventPass.Final)
                    } while (event.changes.any { it.pressed })
                    userHeld.value = false
                    resumeAt.value = System.currentTimeMillis() + 2_000
                }
            },
        contentPadding = PaddingValues(horizontal = 14.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        itemsIndexed(items, key = { _, it -> it.channelId + it.startMs }) { _, chip ->
            UpcomingEpgCard(chip, tr, nowMs, fmt, accent) { onPlay(chip) }
        }
    }
}

@Composable
private fun UpcomingEpgCard(
    chip: ShowcaseEpgChip,
    tr: Boolean,
    nowMs: Long,
    fmt: SimpleDateFormat,
    accent: Color,
    onPlay: () -> Unit
) {
    val target = if (chip.live) chip.endMs else chip.startMs
    val countdown = formatUpcomingCountdown(target - nowMs, chip.live, tr)
    Row(
        Modifier
            .width(280.dp)
            .fillMaxHeight()
            .clip(RoundedCornerShape(16.dp))
            .background(Color.White.copy(alpha = 0.05f))
            .border(1.dp, Color.White.copy(alpha = 0.10f), RoundedCornerShape(16.dp))
            .rayClickable(onClick = onPlay)
    ) {
        Box(
            Modifier
                .width(80.dp)
                .fillMaxHeight()
                .background(Color.Black.copy(alpha = 0.40f)),
            contentAlignment = Alignment.Center
        ) {
            AsyncImage(
                chip.logo,
                null,
                Modifier
                    .padding(8.dp)
                    .fillMaxSize(),
                contentScale = ContentScale.Fit
            )
        }
        Column(
            Modifier
                .weight(1f)
                .fillMaxHeight()
                .padding(horizontal = 12.dp, vertical = 10.dp)
        ) {
            Text(
                if (chip.live) {
                    if (tr) "Şu An Yayında" else "On air"
                } else {
                    if (tr) "Sıradaki" else "Up next"
                },
                color = Color.White.copy(alpha = 0.70f),
                fontSize = 10.sp,
                fontWeight = FontWeight.Medium
            )
            Spacer(Modifier.height(2.dp))
            Text(
                chip.title,
                color = Color.White,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(4.dp))
            Text(
                "${fmt.format(Date(chip.startMs))} - ${fmt.format(Date(chip.endMs))}",
                color = Color.White.copy(alpha = 0.60f),
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium
            )
            Spacer(Modifier.weight(1f))
            Row(
                Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(accent.copy(alpha = 0.20f))
                    .border(1.dp, accent.copy(alpha = 0.50f), RoundedCornerShape(6.dp))
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Filled.Timer, null, tint = accent, modifier = Modifier.size(12.dp))
                Spacer(Modifier.width(4.dp))
                Text(countdown, color = accent, fontSize = 11.sp, fontWeight = FontWeight.ExtraBold)
            }
        }
    }
}

@Composable
fun UpcomingMatchesRow(items: List<ShowcaseEpgChip>, tr: Boolean, onPlay: (ShowcaseEpgChip) -> Unit) {
    var nowMs by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(5_000)
            nowMs = System.currentTimeMillis()
        }
    }
    val g = LocalGlass.current
    LazyRow(
        contentPadding = PaddingValues(horizontal = 14.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        itemsIndexed(items, key = { _, it -> it.channelId + it.startMs }) { _, chip ->
            val title = chip.title.trim().ifBlank { chip.channelName }
            val countdown = formatMatchCountdown(chip.startMs - nowMs, tr)
            val secondary = if (chip.channelName.isBlank()) countdown else "$countdown · ${chip.channelName}"
            Column(
                Modifier
                    .widthIn(min = 108.dp, max = 200.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Brush.linearGradient(g.sectionGradient()), RoundedCornerShape(12.dp))
                    .border(1.dp, Color.White.copy(alpha = 0.12f), RoundedCornerShape(12.dp))
                    .rayClickable(onClick = { onPlay(chip) })
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    title,
                    color = Color.White,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    secondary,
                    color = Color.White.copy(alpha = 0.62f),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Composable
fun UpcomingMatchesPlaceholderRow() {
    LazyRow(
        contentPadding = PaddingValues(horizontal = 14.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        userScrollEnabled = false
    ) {
        itemsIndexed(List(8) { it }, key = { i, _ -> "match-ph-$i" }) { _, _ ->
            Box(
                Modifier
                    .width(132.dp)
                    .height(52.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color.White.copy(alpha = 0.08f))
            )
        }
    }
}

private fun formatUpcomingCountdown(diffMs: Long, live: Boolean, tr: Boolean): String {
    if (diffMs <= 0L) {
        return if (live) {
            if (tr) "Bitiyor" else "Ending"
        } else {
            if (tr) "Başlıyor" else "Starting"
        }
    }
    val totalSec = diffMs / 1000
    val h = totalSec / 3600
    val m = (totalSec % 3600) / 60
    val s = totalSec % 60
    return when {
        h > 0 -> if (tr) "$h sa $m dk" else "${h}h ${m}m"
        m > 0 -> if (tr) "$m dk $s sn" else "${m}m ${s}s"
        else -> if (tr) "$s sn" else "${s}s"
    }
}

private fun formatMatchCountdown(diffMs: Long, tr: Boolean): String {
    if (diffMs <= 0L) return if (tr) "Canlı" else "Live"
    val totalSec = diffMs / 1000
    val h = totalSec / 3600
    val m = ((totalSec % 3600) / 60).toString().padStart(2, '0')
    val s = (totalSec % 60).toString().padStart(2, '0')
    return if (h > 0) {
        if (tr) "Başlıyor: $h:$m:$s" else "Starts: $h:$m:$s"
    } else {
        if (tr) "Başlıyor: $m:$s" else "Starts: $m:$s"
    }
}

@Composable
fun MobileStripTitle(title: String, padded: Boolean = false, onSeeAll: (() -> Unit)? = null, tr: Boolean = true) {
    val accent = LocalGlass.current.accent
    Row(
        Modifier.fillMaxWidth().padding(start = if (padded) 14.dp else 16.dp, end = 6.dp, bottom = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            Modifier
                .width(3.dp)
                .height(16.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(Brush.verticalGradient(listOf(accent, accent.copy(alpha = 0.35f))))
        )
        Spacer(Modifier.width(10.dp))
        Text(
            title.uppercase(),
            color = Color.White,
            fontWeight = FontWeight.ExtraBold,
            fontSize = 13.sp,
            letterSpacing = 1.1.sp,
            modifier = Modifier.weight(1f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        if (onSeeAll != null) {
            Text(
                if (tr) "Tümünü Gör" else "See all",
                color = accent,
                fontWeight = FontWeight.Bold,
                fontSize = 12.5.sp,
                modifier = Modifier.rayClickable(onSeeAll).padding(horizontal = 8.dp, vertical = 4.dp)
            )
        }
    }
}

@Composable
fun MobileHeroCard(

    title: String,
    image: String,
    rating: String,
    year: String,
    nextTitle: String,
    nextImage: String,
    tr: Boolean,
    onPlay: () -> Unit,
    onSave: () -> Unit,
    showSave: Boolean = true,
    onOpen: (() -> Unit)? = null
) {
    val context = LocalContext.current
    Box(
        Modifier
            .fillMaxWidth()
            .aspectRatio(16f / 9.2f)
            .clip(RoundedCornerShape(22.dp))
    ) {
        val posterRequest = remember(image) {
            ImageRequest.Builder(context)
                .data(image)
                .precision(Precision.INEXACT)
                .allowHardware(true)
                .crossfade(true)
                .build()
        }
        AsyncImage(posterRequest, null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
        Box(
            Modifier.fillMaxSize().background(
                Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.90f)))
            )
        )
        if (onOpen != null) {
            Box(Modifier.fillMaxSize().rayClickable(onOpen))
        }
        Row(
            Modifier
                .align(Alignment.BottomStart)
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(
                Modifier
                    .weight(1f, fill = false)
                    .padding(end = 12.dp)
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                    if (rating.isNotBlank()) {
                        Row(
                            Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color.Black.copy(alpha = 0.55f))
                                .padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(Icons.Filled.Star, null, tint = Color(0xFFFFC107), modifier = Modifier.size(12.dp))
                            Text(rating.take(3), color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.ExtraBold)
                        }
                    }
                    if (year.isNotBlank()) {
                        Text(year, color = Color.White.copy(alpha = 0.9f), fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
                Spacer(Modifier.height(4.dp))
                Text(
                    title,
                    color = Color.White,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 20.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Row(
                        Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color.White)
                            .rayClickable(onPlay)
                            .padding(horizontal = 14.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Filled.PlayArrow, null, tint = Color.Black, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(4.dp))
                        Text(if (tr) "İzle" else "Watch", color = Color.Black, fontWeight = FontWeight.Bold)
                    }
                    if (showSave) {
                        Box(
                            Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color.Black.copy(alpha = 0.45f))
                                .rayClickable(onSave),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Filled.BookmarkBorder, null, tint = Color.White, modifier = Modifier.size(20.dp))
                        }
                    }
                }
            }
            if (nextTitle.isNotBlank()) {
                Row(
                    Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color.Black.copy(alpha = 0.65f))
                        .padding(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val nextRequest = remember(nextImage) {
                        ImageRequest.Builder(context)
                            .data(nextImage)
                            .precision(Precision.INEXACT)
                            .allowHardware(true)
                            .crossfade(true)
                            .build()
                    }
                    AsyncImage(nextRequest, null, Modifier.size(36.dp).clip(RoundedCornerShape(8.dp)), contentScale = ContentScale.Crop)
                    Spacer(Modifier.width(6.dp))
                    Column(Modifier.width(84.dp)) {
                        Text(if (tr) "Sıradaki" else "Next", color = Color.White.copy(alpha = 0.7f), fontSize = 9.sp)
                        Text(nextTitle, color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                    Spacer(Modifier.width(6.dp))
                    Box(Modifier.width(3.dp).height(36.dp).clip(RoundedCornerShape(2.dp)).background(LocalGlass.current.accent))
                }
            }
        }
    }
}


@Composable
fun MobilePosterCard(
    title: String,
    poster: String,
    modifier: Modifier = Modifier.width(118.dp),
    rating: String = "",
    playOverlay: Boolean = false,
    live: Boolean = false,
    series: Boolean = false,
    tr: Boolean = true,
    heart: Boolean = false,
    heartOutline: Boolean = false,
    progressPercent: Int = 0,
    onClick: () -> Unit
) {
    val posterShape = RoundedCornerShape(10.dp)
    val glass = LocalGlass.current
    val context = LocalContext.current
    val posterPx = with(LocalDensity.current) { 160.dp.roundToPx() }
    val posterRequest = remember(poster, posterPx) {
        ImageRequest.Builder(context)
            .data(poster)
            .size(posterPx, (posterPx * 3) / 2)
            .crossfade(false)
            .build()
    }
    Column(modifier.rayClickable(onClick)) {
        Box(
            Modifier
                .fillMaxWidth()
                .aspectRatio(2f / 3f)
                .background(Color(0xFF1A1A1A), posterShape)
                .border(1.dp, glass.stroke.copy(alpha = 0.85f), posterShape)
        ) {
            Box(Modifier.fillMaxSize().clip(posterShape)) {
            AsyncImage(
                posterRequest,
                null,
                Modifier.fillMaxSize(),
                contentScale = if (live) ContentScale.Fit else ContentScale.Crop
            )
            if (progressPercent in 1..99 && !live) {
                Box(
                    Modifier
                        .align(Alignment.TopStart)
                        .padding(4.dp)
                        .size(30.dp)
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.55f))
                        .border(0.6.dp, Color.White.copy(alpha = 0.22f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text("%$progressPercent", color = Color.White, fontSize = 8.sp, fontWeight = FontWeight.ExtraBold)
                }
            } else if (playOverlay) {
                Box(
                    Modifier.padding(6.dp).size(22.dp).clip(CircleShape).background(Color.Black.copy(alpha = 0.55f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Filled.PlayArrow, null, tint = Color.White, modifier = Modifier.size(14.dp))
                }
            }
            if (live) MobileBadge(if (tr) "CANLI" else "LIVE", MobileLiveRed, Modifier.align(Alignment.TopStart).padding(6.dp))
            if (series) MobileBadge(if (tr) "DİZİ" else "SERIES", MobileSeriesPurple, Modifier.align(Alignment.TopStart).padding(6.dp))
            if (heart || heartOutline) {
                Icon(
                    if (heart) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                    null,
                    tint = if (heart) glass.accent else Color.White.copy(alpha = 0.85f),
                    modifier = Modifier.align(Alignment.TopEnd).padding(4.dp).size(16.dp)
                )
            }
            if (rating.isNotBlank()) {
                Row(
                    Modifier
                        .align(Alignment.BottomEnd)
                        .padding(4.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(Color.Black.copy(alpha = 0.55f))
                        .border(0.6.dp, Color.White.copy(alpha = 0.12f), RoundedCornerShape(6.dp))
                        .padding(horizontal = 6.dp, vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(3.dp)
                ) {
                    Icon(Icons.Filled.Star, null, tint = Color(0xFFFFC107), modifier = Modifier.size(11.dp))
                    Text("IMDb", color = Color.White.copy(alpha = 0.78f), fontSize = 9.sp, fontWeight = FontWeight.ExtraBold)
                    Text(rating.take(3), color = Color.White, fontSize = 10.5.sp, fontWeight = FontWeight.ExtraBold)
                }
            }
            if (playOverlay) {
                Box(
                    Modifier
                        .align(Alignment.BottomStart)
                        .fillMaxWidth()
                        .background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.78f))))
                        .padding(horizontal = 6.dp, vertical = 6.dp)
                ) {
                    Text(title, color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
            }
        }
        if (!playOverlay) {
        Spacer(Modifier.height(6.dp))
        Text(
            title,
            color = Color.White.copy(alpha = 0.62f),
            fontSize = 11.5.sp,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = 0.1.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        }
    }
}

fun progressPct(item: ProgressEntity): Int {
    if (item.durationMs <= 0L) return 0
    return ((item.positionMs * 100L) / item.durationMs).toInt().coerceIn(0, 99)
}
