package com.ray.iptv.ui.mobile

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Theaters
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalUriHandler
import com.ray.iptv.data.meta.CastPerson
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Icon
import androidx.tv.material3.Text
import coil.compose.AsyncImage
import com.ray.iptv.data.local.CategoryEntity
import com.ray.iptv.data.local.EpisodeEntity
import com.ray.iptv.data.local.FavoriteEntity
import com.ray.iptv.data.local.ProgressEntity
import com.ray.iptv.data.local.VodEntity
import com.ray.iptv.data.meta.VodMeta
import com.ray.iptv.ui.RayViewModel
import com.ray.iptv.ui.input.rayClickable
import com.ray.iptv.ui.theme.LocalGlass
import com.ray.iptv.ui.theme.capsuleForeground
import com.ray.iptv.ui.theme.capsuleGradient
import com.ray.iptv.ui.theme.capsuleStroke
import com.ray.iptv.ui.theme.sectionGradient
import kotlinx.coroutines.delay

@Composable
fun MobileCinemaScreen(
    tr: Boolean,
    vm: RayViewModel,
    seriesMode: Boolean,
    onSeriesMode: (Boolean) -> Unit,
    onSearch: () -> Unit,
    movies: List<VodEntity>,
    series: List<VodEntity>,
    movieCats: List<CategoryEntity>,
    seriesCats: List<CategoryEntity>,
    favorites: List<FavoriteEntity>,
    continueWatching: List<ProgressEntity>,
    onOpen: (VodEntity) -> Unit,
    onPlay: (VodEntity) -> Unit,
    onFav: (VodEntity) -> Unit,
    onResume: (ProgressEntity) -> Unit
) {
    val items = if (seriesMode) series else movies
    val cats = if (seriesMode) seriesCats else movieCats
    val kind = if (seriesMode) "SERIES" else "MOVIE"
    val pager = rememberPagerState(pageCount = { items.size.coerceAtLeast(1).coerceAtMost(8) })
    val hero = items.take(8)
    var trend by remember { mutableStateOf<List<VodEntity>>(emptyList()) }
    var mixed by remember { mutableStateOf<List<VodEntity>>(emptyList()) }
    var topRated by remember { mutableStateOf<List<VodEntity>>(emptyList()) }
    var catRows by remember { mutableStateOf<List<Pair<CategoryEntity, List<VodEntity>>>>(emptyList()) }
    var seeAllTitle by remember { mutableStateOf<String?>(null) }
    var seeAllItems by remember { mutableStateOf<List<VodEntity>>(emptyList()) }
    var seeAllKey by remember { mutableStateOf<String?>(null) }
    val favKind = if (seriesMode) "SERIES" else "MOVIE"
    val favItems = remember(items, favorites, favKind) {
        val ids = favorites.filter { it.kind == favKind }.map { it.mediaId }.toSet()
        items.filter { it.id in ids }.ifEmpty {
            favorites.filter { it.kind == favKind }.mapNotNull { f -> items.firstOrNull { it.id == f.mediaId } }
        }
    }
    val recent = continueWatching.filter {
        if (seriesMode) it.kind == "SERIES" || it.kind == "EPISODE" else it.kind == "MOVIE"
    }.take(20)
    val progressById = remember(continueWatching) {
        continueWatching.associate { it.mediaId to progressPct(it) }
    }
    LaunchedEffect(hero.size, seriesMode) {
        if (hero.size < 2) return@LaunchedEffect
        while (true) {
            delay(6500)
            pager.animateScrollToPage((pager.currentPage + 1) % hero.size)
        }
    }
    LaunchedEffect(seriesMode, cats) {
        trend = vm.showcaseTrend(kind)
        mixed = vm.showcaseMixed(kind)
        topRated = vm.showcaseCategory(kind, "popular", 20)
        catRows = vm.showcaseCategoryRows(kind, cats)
    }
    LaunchedEffect(seeAllKey, seriesMode) {
        val key = seeAllKey ?: return@LaunchedEffect
        val loaded = vm.showcaseCategory(kind, key, 0)
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
            series = seriesMode,
            preserveOrder = seeAllKey == null || seeAllKey == "last50",
            items = seeAllItems,
            favorites = favorites,
            onBack = { seeAllTitle = null; seeAllKey = null },
            onOpen = onOpen
        )
        return
    }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(bottom = 24.dp)) {
        val g = LocalGlass.current
        val fg = g.capsuleForeground()
        Box(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 8.dp)
                .height(48.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(Brush.linearGradient(g.capsuleGradient()), RoundedCornerShape(16.dp))
                .border(1.dp, g.capsuleStroke(), RoundedCornerShape(16.dp))
        ) {
            Row(Modifier.fillMaxSize().padding(4.dp), verticalAlignment = Alignment.CenterVertically) {
                CinemaSeg(if (tr) "Film" else "Movies", !seriesMode, Modifier.weight(1f).padding(end = 22.dp)) {
                    onSeriesMode(false)
                }
                CinemaSeg(if (tr) "Dizi" else "Series", seriesMode, Modifier.weight(1f).padding(start = 22.dp)) {
                    onSeriesMode(true)
                }
            }
            Box(
                Modifier
                    .align(Alignment.Center)
                    .size(40.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(fg.copy(alpha = 0.10f))
                    .border(0.8.dp, fg.copy(alpha = 0.22f), RoundedCornerShape(12.dp))
                    .rayClickable(onSearch),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Filled.Search, null, tint = fg, modifier = Modifier.size(20.dp))
            }
        }
        if (hero.isNotEmpty()) {
            HorizontalPager(state = pager, modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp)) { page ->
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
                    onPlay = { onPlay(item) },
                    onSave = { onFav(item) },
                    showSave = false,
                    onOpen = { onOpen(item) }
                )
            }
            Row(Modifier.fillMaxWidth().padding(top = 10.dp), horizontalArrangement = Arrangement.Center) {
                repeat(hero.size) { i ->
                    val on = i == pager.currentPage
                    Box(
                        Modifier.padding(horizontal = 3.dp).height(6.dp).width(if (on) 18.dp else 6.dp)
                            .clip(CircleShape).background(if (on) MobileCyan else Color.White.copy(alpha = 0.28f))
                    )
                }
            }
        }
        if (recent.isNotEmpty()) {
            CinemaSection(if (tr) "Son izlenenler" else "Recently watched", tr, {
                seeAllTitle = if (tr) "Son izlenenler" else "Recently watched"
                seeAllKey = null
                seeAllItems = recent.mapNotNull { p -> items.firstOrNull { it.id == p.mediaId } }
            }) {
                LazyRow(contentPadding = PaddingValues(horizontal = 14.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    items(recent, key = { it.mediaId }) { p ->
                        MobilePosterCard(p.title, p.poster, playOverlay = true, progressPercent = progressPct(p), onClick = { onResume(p) })
                    }
                }
                Spacer(Modifier.height(4.dp))
            }
        }
        if (favItems.isNotEmpty()) {
            CinemaSection(if (tr) "Favoriler" else "Favorites", tr, {
                seeAllTitle = if (tr) "Favoriler" else "Favorites"
                seeAllKey = "fav"
                seeAllItems = favItems
            }) {
                PosterRow(favItems, favorites, onOpen, progressById)
            }
        }
        CinemaSection(
            if (seriesMode) (if (tr) "Son eklenen 50 dizi" else "Latest 50 series")
            else (if (tr) "Son eklenen 50 film" else "Latest 50 movies"),
            tr,
            {
                seeAllTitle = if (seriesMode) (if (tr) "Son eklenen 50 dizi" else "Latest 50 series") else if (tr) "Son eklenen 50 film" else "Latest 50 movies"
                seeAllKey = "last50"
                seeAllItems = items
            }
        ) {
            PosterRow(items.take(20), favorites, onOpen, progressById)
        }
        if (topRated.isNotEmpty()) {
            CinemaSection(if (tr) "En çok oy alanlar" else "Top rated", tr, {
                seeAllTitle = if (tr) "En çok oy alanlar" else "Top rated"
                seeAllKey = "popular"
                seeAllItems = topRated
            }) {
                PosterRow(topRated, favorites, onOpen, progressById)
            }
        }
        if (trend.isNotEmpty()) {
            CinemaSection(if (tr) "Trend" else "Trending", tr, {
                seeAllTitle = "Trend"
                seeAllKey = "trend"
                seeAllItems = trend
            }) {
                PosterRow(trend, favorites, onOpen, progressById)
            }
        }
        if (mixed.isNotEmpty()) {
            CinemaSection(if (tr) "Karışık" else "Mixed", tr, {
                seeAllTitle = if (tr) "Karışık" else "Mixed"
                seeAllKey = "mixed"
                seeAllItems = mixed
            }) {
                PosterRow(mixed, favorites, onOpen, progressById)
            }
        }
        catRows.forEach { (cat, list) ->
            CinemaSection(cat.name.trim(), tr, {
                seeAllTitle = cat.name
                seeAllKey = cat.id
                seeAllItems = list
            }) {
                PosterRow(list, favorites, onOpen, progressById)
            }
        }
    }
}

@Composable
private fun CinemaSeg(label: String, on: Boolean, modifier: Modifier, onClick: () -> Unit) {
    val fg = LocalGlass.current.capsuleForeground()
    Box(
        modifier
            .fillMaxSize()
            .clip(RoundedCornerShape(12.dp))
            .background(if (on) MobileCyan.copy(alpha = 0.55f) else Color.Transparent)
            .rayClickable(onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(label, color = if (on) Color.White else fg, fontWeight = FontWeight.Bold, fontSize = 15.sp)
    }
}

@Composable
private fun CinemaSection(
    title: String,
    tr: Boolean,
    onSeeAll: () -> Unit,
    content: @Composable () -> Unit
) {
    MobileSectionFrame {
        Column {
            MobileStripTitle(title, true, onSeeAll, tr)
            content()
        }
    }
}

@Composable
private fun PosterRow(
    items: List<VodEntity>,
    favorites: List<FavoriteEntity>,
    onOpen: (VodEntity) -> Unit,
    progressById: Map<String, Int> = emptyMap()
) {
    LazyRow(contentPadding = PaddingValues(horizontal = 14.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        items(items, key = { it.id }) { item ->
            MobilePosterCard(
                title = item.name,
                poster = item.poster,
                rating = item.rating,
                heart = favorites.any { it.mediaId == item.id },
                heartOutline = true,
                progressPercent = progressById[item.id] ?: 0,
                onClick = { onOpen(item) }
            )
        }
    }
    Spacer(Modifier.height(12.dp))
}

@Composable
private fun Modifier.shimmerPulse(): Modifier {
    val transition = rememberInfiniteTransition(label = "shimmer_pulse")
    val alpha by transition.animateFloat(
        initialValue = 0.12f,
        targetValue = 0.45f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 700, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "alpha"
    )
    return this.background(Color.White.copy(alpha = alpha))
}

@Composable
fun MobileVodDetailScreen(
    tr: Boolean,
    item: VodEntity,
    extras: VodMeta,
    episodes: List<EpisodeEntity>,
    favorite: Boolean,
    downloaded: Boolean,
    onBack: () -> Unit,
    onFav: () -> Unit,
    onPlay: () -> Unit,
    onDownload: () -> Unit,
    onTrailer: () -> Unit,
    onPlayEpisode: (EpisodeEntity) -> Unit,
    onDownloadEpisode: (EpisodeEntity) -> Unit,
    episodeProgress: Map<String, ProgressEntity> = emptyMap(),
    downloadedEpisodeIds: Set<String> = emptySet(),
    similar: List<VodEntity>,
    onOpenSimilar: (VodEntity) -> Unit,
    loading: Boolean = false,
    onActorClick: ((CastPerson) -> Unit)? = null
) {
    val g = LocalGlass.current
    val uriHandler = LocalUriHandler.current
    val plot = extras.plot.ifBlank { item.plot }
    val rating = extras.rating.ifBlank { item.rating }
    val year = extras.year.ifBlank { item.year }
    val runtime = extras.runtime
    val genres = (extras.genre.ifBlank { item.genre }).split(',', '|', '/')
        .map { it.trim() }
        .filter { it.isNotBlank() }
    val backdrop = extras.backdrop.ifBlank { item.poster }
    val poster = extras.poster.ifBlank { item.poster }
    var plotOpen by remember(plot) { mutableStateOf(false) }
    val metaLine = listOf(year, runtime, extras.country, extras.language).filter { it.isNotBlank() }.joinToString("  •  ")

    Box(Modifier.fillMaxSize().background(Color(0xFF0B0F14))) {
        if (backdrop.isNotBlank()) {
            AsyncImage(
                backdrop,
                null,
                Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        scaleX = 1.05f
                        scaleY = 1.05f
                        alpha = 0.35f
                    },
                contentScale = ContentScale.Crop
            )
        }
        Box(
            Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        0.00f to Color(0xFF0B0F14).copy(alpha = 0.55f),
                        0.40f to Color(0xFF0B0F14).copy(alpha = 0.90f),
                        1.00f to Color(0xFF0B0F14)
                    )
                )
        )
        Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding()) {
            Row(
                Modifier.fillMaxWidth().padding(start = 6.dp, end = 6.dp, top = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                DetailGlassIcon(Icons.AutoMirrored.Filled.ArrowBack, onBack)
                Spacer(Modifier.weight(1f))
                DetailGlassIcon(
                    if (favorite) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                    onFav,
                    tint = if (favorite) Color(0xFFEF4444) else Color.White
                )
            }
            Column(
                Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 28.dp)
            ) {
                BoxWithConstraints(Modifier.fillMaxWidth()) {
                    val posterW = (maxWidth * 0.28f).coerceIn(108.dp, 148.dp)
                    Row(verticalAlignment = Alignment.Top) {
                        DetailPoster(poster, posterW)
                        Spacer(Modifier.width(14.dp))
                        Column(Modifier.weight(1f)) {
                            Text(
                                item.name,
                                color = Color.White,
                                fontWeight = FontWeight.Black,
                                fontSize = 22.sp,
                                lineHeight = 26.sp,
                                letterSpacing = (-0.2).sp,
                                maxLines = 3,
                                overflow = TextOverflow.Ellipsis
                            )
                            Spacer(Modifier.height(10.dp))
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                if (rating.isNotBlank()) DetailImdbBadge(rating)
                                if (extras.tmdbId > 0 && rating.isNotBlank()) DetailTmdbBadge(rating)
                                if (extras.certification.isNotBlank()) DetailCertBadge(extras.certification)
                                if (loading && rating.isBlank()) {
                                    Box(Modifier.size(56.dp, 20.dp).clip(RoundedCornerShape(6.dp)).shimmerPulse())
                                    Box(Modifier.size(56.dp, 20.dp).clip(RoundedCornerShape(6.dp)).shimmerPulse())
                                }
                            }
                            if (metaLine.isNotBlank()) {
                                Spacer(Modifier.height(8.dp))
                                Text(
                                    metaLine,
                                    color = Color.White.copy(alpha = 0.85f),
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                            } else if (loading) {
                                Spacer(Modifier.height(8.dp))
                                Box(Modifier.size(180.dp, 14.dp).clip(RoundedCornerShape(4.dp)).shimmerPulse())
                            }
                            if (extras.awards.isNotBlank()) {
                                Spacer(Modifier.height(8.dp))
                                Row(
                                    Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(Color(0xFFFEF08A).copy(alpha = 0.12f))
                                        .border(0.8.dp, Color(0xFFFACC15).copy(alpha = 0.35f), RoundedCornerShape(10.dp))
                                        .padding(horizontal = 10.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        Icons.Filled.EmojiEvents,
                                        contentDescription = null,
                                        tint = Color(0xFFFACC15),
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(Modifier.width(6.dp))
                                    Text(
                                        extras.awards,
                                        color = Color.White.copy(alpha = 0.90f),
                                        fontSize = 11.5.sp,
                                        fontWeight = FontWeight.Medium,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                            Spacer(Modifier.height(14.dp))
                            Row(
                                Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                DetailPrimaryButton(
                                    label = if (tr) "İzle" else "Watch",
                                    icon = Icons.Filled.PlayArrow,
                                    modifier = Modifier.weight(1f),
                                    onClick = onPlay
                                )
                                DetailGlassButton(
                                    label = if (downloaded) {
                                        if (tr) "İndirildi" else "Downloaded"
                                    } else if (tr) "İndir" else "Download",
                                    icon = Icons.Filled.Download,
                                    modifier = Modifier.weight(1f),
                                    onClick = onDownload
                                )
                            }
                        }
                    }
                }
                if (genres.isNotEmpty()) {
                    Spacer(Modifier.height(14.dp))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(genres.take(10)) { label ->
                            Text(
                                label,
                                color = Color.White,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(20.dp))
                                    .background(Brush.linearGradient(g.capsuleGradient()), RoundedCornerShape(20.dp))
                                    .border(1.dp, g.capsuleStroke(), RoundedCornerShape(20.dp))
                                    .padding(horizontal = 12.dp, vertical = 6.dp)
                            )
                        }
                    }
                } else if (loading) {
                    Spacer(Modifier.height(14.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        repeat(3) {
                            Box(Modifier.size(72.dp, 26.dp).clip(RoundedCornerShape(20.dp)).shimmerPulse())
                        }
                    }
                }
                if (plot.isNotBlank()) {
                    Spacer(Modifier.height(20.dp))
                    DetailSectionTitle(if (tr) "Özet" else "Synopsis")
                    Spacer(Modifier.height(8.dp))
                    DetailGlassPanel {
                        Text(
                            plot,
                            color = Color.White.copy(alpha = 0.90f),
                            fontSize = 14.sp,
                            lineHeight = 20.sp,
                            maxLines = if (plotOpen) Int.MAX_VALUE else 5,
                            overflow = if (plotOpen) TextOverflow.Clip else TextOverflow.Ellipsis
                        )
                        if (plot.length > 180) {
                            Spacer(Modifier.height(8.dp))
                            Text(
                                if (plotOpen) {
                                    if (tr) "Daha az" else "Show less"
                                } else if (tr) "Devamını oku" else "Read more",
                                color = g.accent,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                modifier = Modifier.rayClickable(onClick = { plotOpen = !plotOpen })
                            )
                        }
                    }
                } else if (loading) {
                    Spacer(Modifier.height(20.dp))
                    DetailSectionTitle(if (tr) "Özet" else "Synopsis")
                    Spacer(Modifier.height(8.dp))
                    DetailGlassPanel {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Box(Modifier.fillMaxWidth().height(14.dp).clip(RoundedCornerShape(4.dp)).shimmerPulse())
                            Box(Modifier.fillMaxWidth(0.92f).height(14.dp).clip(RoundedCornerShape(4.dp)).shimmerPulse())
                            Box(Modifier.fillMaxWidth(0.65f).height(14.dp).clip(RoundedCornerShape(4.dp)).shimmerPulse())
                        }
                    }
                }
                if (extras.trailers.isNotEmpty()) {
                    Spacer(Modifier.height(20.dp))
                    DetailSectionTitle(if (tr) "Fragmanlar & Videolar" else "Trailers & Videos")
                    Spacer(Modifier.height(10.dp))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        items(extras.trailers) { t ->
                            Column(
                                Modifier
                                    .width(200.dp)
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(Brush.linearGradient(g.capsuleGradient()), RoundedCornerShape(14.dp))
                                    .border(1.dp, g.capsuleStroke(), RoundedCornerShape(14.dp))
                                    .rayClickable(onClick = {
                                        try { uriHandler.openUri(t.watchUrl) } catch (_: Exception) {}
                                    })
                                    .padding(8.dp)
                            ) {
                                Box(
                                    Modifier
                                        .fillMaxWidth()
                                        .height(112.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(Color.Black.copy(alpha = 0.4f))
                                ) {
                                    if (t.thumbnailUrl.isNotBlank()) {
                                        AsyncImage(
                                            model = t.thumbnailUrl,
                                            contentDescription = null,
                                            contentScale = ContentScale.Crop,
                                            modifier = Modifier.fillMaxSize()
                                        )
                                    }
                                    Box(
                                        Modifier
                                            .size(42.dp)
                                            .clip(CircleShape)
                                            .background(g.accent.copy(alpha = 0.90f))
                                            .align(Alignment.Center),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            Icons.Filled.PlayArrow,
                                            contentDescription = null,
                                            tint = Color.White,
                                            modifier = Modifier.size(24.dp)
                                        )
                                    }
                                }
                                Spacer(Modifier.height(6.dp))
                                Text(
                                    t.title,
                                    color = Color.White,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                if (t.subtitle.isNotBlank()) {
                                    Text(
                                        t.subtitle,
                                        color = Color.White.copy(alpha = 0.55f),
                                        fontSize = 11.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                    }
                } else if (extras.trailerUrl.isNotBlank()) {
                    Spacer(Modifier.height(12.dp))
                    DetailGlassButton(
                        label = if (tr) "Fragman" else "Trailer",
                        icon = Icons.Filled.Theaters,
                        modifier = Modifier.fillMaxWidth(),
                        onClick = onTrailer
                    )
                }
                if (extras.people.isNotEmpty()) {
                    Spacer(Modifier.height(20.dp))
                    DetailSectionTitle(if (tr) "Oyuncular" else "Cast")
                    Spacer(Modifier.height(10.dp))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        items(extras.people.take(16), key = { it.name + it.character }) { p ->
                            Row(
                                Modifier
                                    .width(210.dp)
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(Brush.linearGradient(g.capsuleGradient()), RoundedCornerShape(16.dp))
                                    .border(1.dp, g.capsuleStroke(), RoundedCornerShape(16.dp))
                                    .rayClickable(onClick = { onActorClick?.invoke(p) })
                                    .padding(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                AsyncImage(
                                    p.photo, null,
                                    Modifier.size(46.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.08f)),
                                    contentScale = ContentScale.Crop
                                )
                                Spacer(Modifier.width(10.dp))
                                Column(Modifier.weight(1f)) {
                                    Text(p.name, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    Text(p.character, color = Color.White.copy(alpha = 0.55f), fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                }
                            }
                        }
                    }
                } else if (loading) {
                    Spacer(Modifier.height(20.dp))
                    DetailSectionTitle(if (tr) "Oyuncular" else "Cast")
                    Spacer(Modifier.height(10.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        repeat(3) {
                            Box(Modifier.size(200.dp, 60.dp).clip(RoundedCornerShape(16.dp)).shimmerPulse())
                        }
                    }
                }
                if (item.kind == "SERIES") {
                    val seasons = remember(episodes) { episodes.map { it.season }.distinct().sorted() }
                    var season by remember(seasons) { mutableIntStateOf(seasons.firstOrNull() ?: 1) }
                    val inSeason = episodes.filter { it.season == season }
                    Spacer(Modifier.height(20.dp))
                    DetailSectionTitle(if (tr) "Bölümler" else "Episodes")
                    if (episodes.isEmpty()) {
                        Spacer(Modifier.height(10.dp))
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            repeat(3) {
                                Box(Modifier.fillMaxWidth().height(54.dp).clip(RoundedCornerShape(12.dp)).shimmerPulse())
                            }
                        }
                    } else {
                    if (seasons.size > 1) {
                        Spacer(Modifier.height(10.dp))
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(seasons) { s ->
                                val on = s == season
                                Text(
                                    if (tr) "Sezon $s" else "Season $s",
                                    color = if (on) Color(0xFF0F172A) else Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp,
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(16.dp))
                                        .background(if (on) Color.White else Color.White.copy(alpha = 0.10f))
                                        .border(1.dp, if (on) Color.White else g.capsuleStroke(), RoundedCornerShape(16.dp))
                                        .rayClickable(onClick = { season = s })
                                        .padding(horizontal = 12.dp, vertical = 6.dp)
                                )
                            }
                        }
                    }
                    Spacer(Modifier.height(10.dp))
                    inSeason.forEach { ep ->
                        val prog = episodeProgress[ep.id]
                        val frac = if (prog != null && prog.durationMs > 0) {
                            (prog.positionMs.toFloat() / prog.durationMs).coerceIn(0f, 1f)
                        } else 0f
                        DetailGlassPanel(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 8.dp)
                                .rayClickable(onClick = { onPlayEpisode(ep) })
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Column(Modifier.weight(1f)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            "S${ep.season}E${ep.episode}",
                                            color = g.accent,
                                            fontWeight = FontWeight.ExtraBold,
                                            fontSize = 13.sp,
                                            modifier = Modifier.widthIn(min = 64.dp)
                                        )
                                        Spacer(Modifier.width(8.dp))
                                        Text(
                                            ep.name.ifBlank { if (tr) "Bölüm ${ep.episode}" else "Episode ${ep.episode}" },
                                            color = Color.White,
                                            fontWeight = FontWeight.SemiBold,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                            modifier = Modifier.weight(1f)
                                        )
                                    }
                                    if (frac > 0f) {
                                        Spacer(Modifier.height(8.dp))
                                        Box(
                                            Modifier
                                                .fillMaxWidth()
                                                .height(3.dp)
                                                .clip(RoundedCornerShape(2.dp))
                                                .background(Color.White.copy(alpha = 0.16f))
                                        ) {
                                            Box(Modifier.fillMaxWidth(frac).height(3.dp).background(g.accent))
                                        }
                                    }
                                }
                                Icon(
                                    Icons.Filled.Download,
                                    null,
                                    tint = if (downloadedEpisodeIds.contains(ep.id)) g.accent else Color.White.copy(alpha = 0.72f),
                                    modifier = Modifier.size(22.dp).rayClickable(onClick = { onDownloadEpisode(ep) })
                                )
                            }
                        }
                    }
                    }
                }
                if (similar.isNotEmpty()) {
                    Spacer(Modifier.height(20.dp))
                    DetailSectionTitle(if (tr) "Bunlar da ilginizi çekebilir" else "You might also like")
                    Spacer(Modifier.height(10.dp))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        items(similar, key = { it.id }) { s ->
                            MobilePosterCard(
                                title = s.name,
                                poster = s.poster,
                                modifier = Modifier.width(102.dp),
                                heartOutline = true,
                                onClick = { onOpenSimilar(s) }
                            )
                        }
                    }
                } else if (loading) {
                    Spacer(Modifier.height(20.dp))
                    DetailSectionTitle(if (tr) "Bunlar da ilginizi çekebilir" else "You might also like")
                    Spacer(Modifier.height(10.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        repeat(4) {
                            Box(
                                Modifier
                                    .width(102.dp)
                                    .aspectRatio(2f / 3f)
                                    .clip(RoundedCornerShape(12.dp))
                                    .shimmerPulse()
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DetailPoster(url: String, width: androidx.compose.ui.unit.Dp) {
    val shape = RoundedCornerShape(14.dp)
    Box(
        Modifier
            .width(width)
            .aspectRatio(2f / 3f)
            .shadow(16.dp, shape, clip = false, ambientColor = Color.Black.copy(alpha = 0.45f), spotColor = Color.Black.copy(alpha = 0.45f))
            .clip(shape)
            .background(Color(0xFF1A1A1A))
            .border(1.dp, Color.White.copy(alpha = 0.28f), shape)
    ) {
        AsyncImage(url, null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
    }
}

@Composable
private fun DetailGlassIcon(icon: ImageVector, onClick: () -> Unit, tint: Color = Color.White) {
    val g = LocalGlass.current
    val shape = CircleShape
    Box(
        Modifier
            .size(44.dp)
            .clip(shape)
            .background(Brush.linearGradient(g.capsuleGradient()), shape)
            .border(1.dp, g.capsuleStroke(), shape)
            .rayClickable(onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, null, tint = tint, modifier = Modifier.size(22.dp))
    }
}

@Composable
private fun DetailGlassPanel(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    val g = LocalGlass.current
    val shape = RoundedCornerShape(18.dp)
    Column(
        modifier
            .fillMaxWidth()
            .clip(shape)
            .background(Brush.linearGradient(g.sectionGradient()), shape)
            .border(1.dp, g.capsuleStroke().copy(alpha = 0.55f), shape)
            .padding(12.dp),
        content = content
    )
}

@Composable
private fun DetailSectionTitle(text: String) {
    Text(text, color = Color.White, fontWeight = FontWeight.ExtraBold, fontSize = 17.sp)
}

@Composable
private fun DetailImdbBadge(value: String) {
    Text(
        "IMDb  $value",
        color = Color.Black,
        fontSize = 12.sp,
        fontWeight = FontWeight.Black,
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(Color(0xFFF5C518))
            .padding(horizontal = 8.dp, vertical = 4.dp)
    )
}

@Composable
private fun DetailTmdbBadge(value: String) {
    val c = Color(0xFF01B4E4)
    Row(
        Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(c.copy(alpha = 0.18f))
            .border(0.8.dp, c.copy(alpha = 0.5f), RoundedCornerShape(6.dp))
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(Icons.Filled.Star, null, tint = c, modifier = Modifier.size(14.dp))
        Spacer(Modifier.width(4.dp))
        Text("TMDB $value", color = c, fontSize = 12.sp, fontWeight = FontWeight.ExtraBold)
    }
}

@Composable
private fun DetailCertBadge(cert: String) {
    Box(
        Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(Color.White.copy(alpha = 0.14f))
            .border(0.8.dp, Color.White.copy(alpha = 0.32f), RoundedCornerShape(6.dp))
            .padding(horizontal = 7.dp, vertical = 3.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            cert,
            color = Color.White,
            fontWeight = FontWeight.Bold,
            fontSize = 11.5.sp
        )
    }
}

@Composable
private fun DetailPrimaryButton(label: String, icon: ImageVector, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Row(
        modifier
            .heightIn(min = 46.dp)
            .shadow(10.dp, RoundedCornerShape(22.dp), clip = false, ambientColor = Color.White.copy(alpha = 0.22f), spotColor = Color.White.copy(alpha = 0.22f))
            .clip(RoundedCornerShape(22.dp))
            .background(Color.White)
            .rayClickable(onClick)
            .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        Icon(icon, null, tint = Color(0xFF0F172A), modifier = Modifier.size(24.dp))
        Spacer(Modifier.width(4.dp))
        Text(label, color = Color(0xFF0F172A), fontWeight = FontWeight.ExtraBold, fontSize = 15.sp)
    }
}

@Composable
private fun DetailGlassButton(label: String, icon: ImageVector, modifier: Modifier = Modifier, onClick: () -> Unit) {
    val g = LocalGlass.current
    val fg = g.capsuleForeground()
    val shape = RoundedCornerShape(22.dp)
    Row(
        modifier
            .heightIn(min = 46.dp)
            .clip(shape)
            .background(Brush.linearGradient(g.capsuleGradient()), shape)
            .border(1.dp, g.capsuleStroke(), shape)
            .rayClickable(onClick)
            .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        Icon(icon, null, tint = fg, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(6.dp))
        Text(label, color = fg, fontWeight = FontWeight.ExtraBold, fontSize = 14.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}
