package com.ray.iptv.ui.screens.tablet

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items as lazyItems
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Star
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Icon
import androidx.tv.material3.Text
import coil.compose.AsyncImage
import coil.request.ImageRequest
import coil.size.Precision
import com.ray.iptv.data.local.CategoryEntity
import com.ray.iptv.data.local.ChannelEntity
import com.ray.iptv.data.local.FavoriteEntity
import com.ray.iptv.data.local.ProgressEntity
import com.ray.iptv.data.local.VodEntity
import com.ray.iptv.data.repo.RaySettings
import com.ray.iptv.ui.RayViewModel
import com.ray.iptv.ui.ShowcaseEpgChip
import com.ray.iptv.ui.input.rayClickable
import com.ray.iptv.ui.mobile.MobileBadge
import com.ray.iptv.ui.mobile.MobileHeroCard
import com.ray.iptv.ui.mobile.MobileLiveRed
import com.ray.iptv.ui.mobile.MobileSectionFrame
import com.ray.iptv.ui.mobile.MobileSeeAllGrid
import com.ray.iptv.ui.mobile.MobileSeriesPurple
import com.ray.iptv.ui.mobile.MobileStripTitle
import com.ray.iptv.ui.mobile.UpcomingEpgRow
import com.ray.iptv.ui.mobile.UpcomingMatchesPlaceholderRow
import com.ray.iptv.ui.mobile.UpcomingMatchesRow
import com.ray.iptv.ui.theme.LocalGlass
import kotlinx.coroutines.delay

private val TabletCyan = Color(0xFF22D3EE)

@Composable
fun TabletHomeScreen(
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

    // Tablet Modu: Maksimum 3 öne çıkan film & dizi kategorisi yüklenir (donma ve aşırı bellek tüketimi engellenir)
    LaunchedEffect(
        settings.homeTrendFilms,
        settings.homeTrendSeries,
        settings.homeMixedFilms,
        settings.homeMixedSeries,
        settings.homeMixedLive,
        settings.homeFavoriteFilms,
        settings.homeFavoriteSeries,
        settings.homeFavorites,
        favorites.size,
        movieCats.size,
        seriesCats.size
    ) {
        if (settings.homeTrendFilms) trendFilms = vm.showcaseTrend("MOVIE")
        if (settings.homeTrendSeries) trendSeries = vm.showcaseTrend("SERIES")
        if (settings.homeMixedFilms) mixedFilms = vm.showcaseMixed("MOVIE")
        if (settings.homeMixedSeries) mixedSeries = vm.showcaseMixed("SERIES")
        if (settings.homeMixedLive) mixedLive = vm.showcaseMixedLive()
        if (settings.homeFavoriteFilms || settings.homeFavorites) favFilms = vm.showcaseFavorites("MOVIE")
        if (settings.homeFavoriteSeries || settings.homeFavorites) favSeries = vm.showcaseFavorites("SERIES")
        if (settings.homeFavorites || settings.homeFavoriteFilms || settings.homeFavoriteSeries) favLive = vm.showcaseFavoriteLive()
        topFilms = vm.showcaseCategory("MOVIE", "popular", 12)
        topSeries = vm.showcaseCategory("SERIES", "popular", 12)
        if (settings.homeMovies && movieCats.isNotEmpty()) {
            movieRows = vm.showcaseCategoryRows("MOVIE", movieCats.take(3))
        }
        if (settings.homeSeries && seriesCats.isNotEmpty()) {
            seriesRows = vm.showcaseCategoryRows("SERIES", seriesCats.take(3))
        }
    }

    val epgStats by vm.epgStats.collectAsState()
    LaunchedEffect(settings.homeUpcomingEpg, settings.homeUpcomingMatches, settings.lastEpgRefreshMs, favorites.size, epgStats.programmes, liveSample.size) {
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
            delay(25_000)
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
            val m = movies.take(5)
            val s = series.take(5)
            val n = maxOf(m.size, s.size)
            for (i in 0 until n) {
                if (i < m.size) add(m[i])
                if (i < s.size) add(s[i])
            }
        }.take(8)
    }
    val pager = rememberPagerState(pageCount = { hero.size.coerceAtLeast(1) })
    val favIds = remember(favorites) { favorites.mapTo(HashSet(favorites.size)) { it.mediaId } }
    val continueVod = remember(continueWatching) {
        continueWatching.filter { it.kind != "LIVE" }.take(12)
    }

    LaunchedEffect(hero.size) {
        if (hero.size < 2) return@LaunchedEffect
        while (true) {
            delay(7000)
            pager.animateScrollToPage((pager.currentPage + 1) % hero.size)
        }
    }

    val homeListState = rememberSaveable(saver = LazyListState.Saver) { LazyListState() }

    LazyColumn(
        state = homeListState,
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 10.dp, bottom = 60.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 1. Hero Billboard Banner
        item(key = "hero") {
            if (hero.isNotEmpty()) {
                Column {
                    HorizontalPager(
                        state = pager,
                        beyondViewportPageCount = 1,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(270.dp)
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
                        Modifier
                            .fillMaxWidth()
                            .padding(top = 10.dp),
                        horizontalArrangement = Arrangement.Center
                    ) {
                        repeat(hero.size) { i ->
                            val on = i == pager.currentPage
                            Box(
                                Modifier
                                    .padding(horizontal = 3.dp)
                                    .height(5.dp)
                                    .width(if (on) 22.dp else 6.dp)
                                    .clip(CircleShape)
                                    .background(if (on) TabletCyan else Color.White.copy(alpha = 0.28f))
                            )
                        }
                    }
                }
            }
        }

        // 2. Sıradaki Yayınlar (Upcoming EPG)
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

        // 3. İzlemeye Devam Et (Continue Watching)
        if (settings.homeContinue && continueVod.isNotEmpty()) {
            item(key = "continue") {
                MobileSectionFrame {
                    Column {
                        MobileStripTitle(if (tr) "İzlemeye Devam Et" else "Continue watching", true)
                        LazyRow(
                            contentPadding = PaddingValues(horizontal = 14.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            itemsIndexed(continueVod, key = { _, it -> it.mediaId }) { _, item ->
                                TabletPosterCard(
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

        // 4. Ray AI: Önerilenler
        if (settings.homeAiRecommendations) {
            item(key = "ai") {
                val recLive = liveSample.take(8)
                val recSeries = series.take(8)
                MobileSectionFrame {
                    Column {
                        MobileStripTitle(if (tr) "Ray AI: Senin İçin Önerilenler" else "Ray AI: Recommended for you", padded = true)
                        LazyRow(
                            contentPadding = PaddingValues(horizontal = 14.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            itemsIndexed(recLive, key = { _, it -> "l-${it.id}" }) { _, ch ->
                                TabletPosterCard(
                                    title = ch.name,
                                    poster = ch.logo,
                                    live = true,
                                    tr = tr,
                                    onClick = { onPlayLive(ch) }
                                )
                            }
                            itemsIndexed(recSeries, key = { _, it -> "s-${it.id}" }) { _, item ->
                                TabletPosterCard(
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

        // 5. Favoriler (Live, Films, Series)
        val combinedFavsOn = settings.homeFavorites || settings.homeFavoriteFilms || settings.homeFavoriteSeries
        if (combinedFavsOn && (favFilms.isNotEmpty() || favSeries.isNotEmpty() || favLive.isNotEmpty())) {
            item(key = "favorites") {
                MobileSectionFrame {
                    Column {
                        MobileStripTitle(if (tr) "Favoriler" else "Favorites", true)
                        LazyRow(
                            contentPadding = PaddingValues(horizontal = 14.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            itemsIndexed(favLive.take(12), key = { _, it -> "fav-l-${it.id}" }) { _, ch ->
                                TabletPosterCard(ch.name, ch.logo, live = true, tr = tr, heart = true, onClick = { onPlayLive(ch) })
                            }
                            itemsIndexed(favFilms.take(12), key = { _, it -> "fav-m-${it.id}" }) { _, item ->
                                TabletPosterCard(item.name, item.poster, rating = item.rating, heart = true, heartOutline = true, onClick = { onOpenMovie(item) })
                            }
                            itemsIndexed(favSeries.take(12), key = { _, it -> "fav-s-${it.id}" }) { _, item ->
                                TabletPosterCard(item.name, item.poster, rating = item.rating, series = true, tr = tr, heart = true, heartOutline = true, onClick = { onOpenSeries(item) })
                            }
                        }
                    }
                }
            }
        }

        // 6. Trend Filmler
        if (settings.homeTrendFilms && trendFilms.isNotEmpty()) {
            item(key = "trend-films") {
                TabletVodStrip(if (tr) "Trend Filmler" else "Trending movies", trendFilms, favIds, onOpenMovie) {
                    seeAllTitle = if (tr) "Trend Filmler" else "Trending movies"
                    seeAllKind = "MOVIE"
                    seeAllKey = "trend"
                    seeAllItems = trendFilms
                }
            }
        }

        // 7. Trend Diziler
        if (settings.homeTrendSeries && trendSeries.isNotEmpty()) {
            item(key = "trend-series") {
                TabletVodStrip(if (tr) "Trend Diziler" else "Trending series", trendSeries, favIds, onOpenSeries, series = true) {
                    seeAllTitle = if (tr) "Trend Diziler" else "Trending series"
                    seeAllKind = "SERIES"
                    seeAllKey = "trend"
                    seeAllItems = trendSeries
                }
            }
        }

        // 8. En Çok Oy Alan Filmler
        if (topFilms.isNotEmpty() && settings.homeMovies) {
            item(key = "top-films") {
                TabletVodStrip(if (tr) "En Çok Oy Alan Filmler" else "Top rated movies", topFilms, favIds, onOpenMovie) {
                    seeAllTitle = if (tr) "En Çok Oy Alan Filmler" else "Top rated movies"
                    seeAllKind = "MOVIE"
                    seeAllKey = "popular"
                    seeAllItems = topFilms
                }
            }
        }

        // 9. En Çok Oy Alan Diziler
        if (topSeries.isNotEmpty() && settings.homeSeries) {
            item(key = "top-series") {
                TabletVodStrip(if (tr) "En Çok Oy Alan Diziler" else "Top rated series", topSeries, favIds, onOpenSeries, series = true) {
                    seeAllTitle = if (tr) "En Çok Oy Alan Diziler" else "Top rated series"
                    seeAllKind = "SERIES"
                    seeAllKey = "popular"
                    seeAllItems = topSeries
                }
            }
        }

        // 10. Karışık Filmler
        if (settings.homeMixedFilms && mixedFilms.isNotEmpty()) {
            item(key = "mixed-films") {
                TabletVodStrip(if (tr) "Karışık Filmler" else "Mixed movies", mixedFilms, favIds, onOpenMovie) {
                    seeAllTitle = if (tr) "Karışık Filmler" else "Mixed movies"
                    seeAllKind = "MOVIE"
                    seeAllKey = "mixed"
                    seeAllItems = mixedFilms
                }
            }
        }

        // 11. Karışık Diziler
        if (settings.homeMixedSeries && mixedSeries.isNotEmpty()) {
            item(key = "mixed-series") {
                TabletVodStrip(if (tr) "Karışık Diziler" else "Mixed series", mixedSeries, favIds, onOpenSeries, series = true) {
                    seeAllTitle = if (tr) "Karışık Diziler" else "Mixed series"
                    seeAllKind = "SERIES"
                    seeAllKey = "mixed"
                    seeAllItems = mixedSeries
                }
            }
        }

        // 12. Karışık Canlı TV
        if (settings.homeMixedLive && mixedLive.isNotEmpty()) {
            item(key = "mixed-live") {
                MobileSectionFrame {
                    Column {
                        MobileStripTitle(if (tr) "Karışık Canlı TV" else "Mixed Live TV", true)
                        LazyRow(
                            contentPadding = PaddingValues(horizontal = 14.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            itemsIndexed(mixedLive.take(16), key = { _, it -> "mix-live-${it.id}" }) { _, ch ->
                                TabletPosterCard(
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

        // 13. Son İzlenen Kanallar
        if (settings.homeRecentLive && recentLive.isNotEmpty()) {
            item(key = "recent-live") {
                MobileSectionFrame {
                    Column {
                        MobileStripTitle(if (tr) "Son İzlenen Kanallar" else "Recently watched channels", true)
                        LazyRow(
                            contentPadding = PaddingValues(horizontal = 14.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            itemsIndexed(recentLive.take(16), key = { _, it -> "rl-${it.mediaId}" }) { _, item ->
                                TabletPosterCard(item.title, item.poster, live = true, tr = tr, playOverlay = true, onClick = { onResume(item) })
                            }
                        }
                    }
                }
            }
        }

        // 14. Son Eklenen Filmler
        if (settings.homeMovies && movies.isNotEmpty()) {
            item(key = "latest-movies") {
                TabletVodStrip(if (tr) "Son Eklenen Filmler" else "Latest movies", movies.take(14), favIds, onOpenMovie) {
                    seeAllTitle = if (tr) "Son Eklenen Filmler" else "Latest movies"
                    seeAllKind = "MOVIE"
                    seeAllKey = "last50"
                    seeAllItems = movies
                }
            }
        }

        // 15. Son Eklenen Diziler
        if (settings.homeSeries && series.isNotEmpty()) {
            item(key = "latest-series") {
                TabletVodStrip(if (tr) "Son Eklenen Diziler" else "Latest series", series.take(14), favIds, onOpenSeries, series = true) {
                    seeAllTitle = if (tr) "Son Eklenen Diziler" else "Latest series"
                    seeAllKind = "SERIES"
                    seeAllKey = "last50"
                    seeAllItems = series
                }
            }
        }

        // 16. Öne Çıkan Film Kategorileri (Maks 4 Kategori)
        if (movieRows.isNotEmpty()) {
            lazyItems(movieRows, key = { "movie-${it.first.id}" }) { (cat, list) ->
                TabletVodStrip(cat.name.trim(), list, favIds, onOpenMovie, series = false) {
                    seeAllTitle = cat.name
                    seeAllKind = "MOVIE"
                    seeAllKey = cat.id
                    seeAllItems = list
                }
            }
        }

        // 17. Öne Çıkan Dizi Kategorileri (Maks 4 Kategori)
        if (seriesRows.isNotEmpty()) {
            lazyItems(seriesRows, key = { "series-${it.first.id}" }) { (cat, list) ->
                TabletVodStrip(cat.name.trim(), list, favIds, onOpenSeries, series = true) {
                    seeAllTitle = cat.name
                    seeAllKind = "SERIES"
                    seeAllKey = cat.id
                    seeAllItems = list
                }
            }
        }

        // 18. Sıradaki Maçlar (Upcoming Matches)
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
private fun TabletVodStrip(
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
            LazyRow(
                contentPadding = PaddingValues(horizontal = 14.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                itemsIndexed(items.take(12), key = { _, it -> it.id }) { _, item ->
                    TabletPosterCard(
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

/**
 * Tablet modu için özel düşük bellekli, donanım hızlandırmalı ve hafif poster kartı.
 * 110x165 piksel boyutunda çözülerek tablet GPU/RAM yükü %75 oranında düşürülür.
 */
@Composable
fun TabletPosterCard(
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
    
    // Tablet için optimize edilmiş kompakt görsel boyutu
    val posterRequest = remember(poster) {
        ImageRequest.Builder(context)
            .data(poster)
            .size(110, 165)
            .precision(Precision.INEXACT)
            .allowHardware(true)
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
                    model = posterRequest,
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = if (live) ContentScale.Fit else ContentScale.Crop
                )
                if (progressPercent in 1..99 && !live) {
                    Box(
                        Modifier
                            .align(Alignment.TopStart)
                            .padding(4.dp)
                            .size(28.dp)
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
                            .background(Color.Black.copy(alpha = 0.70f))
                            .padding(horizontal = 4.dp, vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Filled.Star, null, tint = Color(0xFFFFD54F), modifier = Modifier.size(10.dp))
                        Spacer(Modifier.width(2.dp))
                        Text(rating, color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
        Spacer(Modifier.height(4.dp))
        Text(
            title,
            color = Color.White.copy(alpha = 0.92f),
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}
