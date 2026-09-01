package com.ray.iptv.ui.screens.catalog

import android.view.KeyEvent
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.RocketLaunch
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Theaters
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material.icons.filled.Whatshot
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Icon
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import coil.compose.AsyncImage
import com.ray.iptv.data.local.CategoryEntity
import com.ray.iptv.data.local.FavoriteEntity
import com.ray.iptv.data.local.VodEntity
import com.ray.iptv.ui.glass.GlassPanel
import com.ray.iptv.ui.i18n.Copy
import com.ray.iptv.ui.theme.LocalGlass
import kotlinx.coroutines.launch

private data class HubRail(
    val id: String,
    val title: String,
    val icon: ImageVector,
    val items: List<VodEntity>
)

private enum class HubFilter(val titleTr: String, val titleEn: String) {
    ALL("Tümü", "All"),
    MOVIES("Filmler", "Movies"),
    SERIES("Diziler", "Series"),
    ACTION("Aksiyon & Macera", "Action"),
    SCIFI("Bilim Kurgu", "Sci-Fi"),
    TOP_RATED("IMDb 8+", "Top Rated"),
    FAVORITES("Favoriler", "Favorites")
}

@Composable
fun VodCinemaHubScreen(
    copy: Copy,
    movies: List<VodEntity>,
    series: List<VodEntity>,
    movieCategories: List<CategoryEntity>,
    seriesCategories: List<CategoryEntity>,
    favorites: List<FavoriteEntity> = emptyList(),
    onPlay: (VodEntity) -> Unit,
    onOpenDetail: (VodEntity) -> Unit,
    onFav: (VodEntity) -> Unit,
    onExpandRail: () -> Unit,
    railExpanded: Boolean,
    contentFocusTrigger: Long = 0L,
    onExit: () -> Unit
) {
    val g = LocalGlass.current
    val favIds = remember(favorites) { favorites.map { it.mediaId }.toSet() }
    var selectedFilter by remember { mutableStateOf(HubFilter.ALL) }
    var focusedItem by remember { mutableStateOf<VodEntity?>(null) }
    val playBtnFocusRequester = remember { FocusRequester() }
    val firstItemFocusRequester = remember { FocusRequester() }
    val scope = rememberCoroutineScope()
    val listState = rememberLazyListState()

    // Default hero item: best rated or first movie
    val defaultHero = remember(movies, series) {
        movies.maxByOrNull { it.rating.toDoubleOrNull() ?: 0.0 }
            ?: series.maxByOrNull { it.rating.toDoubleOrNull() ?: 0.0 }
            ?: movies.firstOrNull()
            ?: series.firstOrNull()
    }

    val activeHero = focusedItem ?: defaultHero

    // Build curated rails
    val rails = remember(movies, series, favIds, selectedFilter) {
        val list = mutableListOf<HubRail>()

        val favItems = (movies + series).filter { favIds.contains(it.id) }
        if (favItems.isNotEmpty() && (selectedFilter == HubFilter.ALL || selectedFilter == HubFilter.FAVORITES)) {
            list.add(HubRail("fav", "❤️ Favorilerim (${favItems.size})", Icons.Filled.Favorite, favItems))
        }

        val popMovies = movies.sortedByDescending { it.rating.toDoubleOrNull() ?: 0.0 }.take(25)
        if (popMovies.isNotEmpty() && (selectedFilter == HubFilter.ALL || selectedFilter == HubFilter.MOVIES)) {
            list.add(HubRail("pop_movies", "🔥 Popüler & Öne Çıkan Filmler", Icons.Filled.LocalFireDepartment, popMovies))
        }

        val topSeries = series.sortedByDescending { it.addedUnix }.take(25)
        if (topSeries.isNotEmpty() && (selectedFilter == HubFilter.ALL || selectedFilter == HubFilter.SERIES)) {
            list.add(HubRail("new_series", "📺 Yeni Eklenen Diziler", Icons.Filled.Tv, topSeries))
        }

        val actionItems = (movies + series).filter {
            val gLow = it.genre.lowercase()
            val cLow = it.categoryName.lowercase()
            gLow.contains("action") || gLow.contains("aksiyon") || gLow.contains("macera") ||
                cLow.contains("action") || cLow.contains("aksiyon") || cLow.contains("macera")
        }.take(25)
        if (actionItems.isNotEmpty() && (selectedFilter == HubFilter.ALL || selectedFilter == HubFilter.ACTION)) {
            list.add(HubRail("action", "💥 Aksiyon & Macera", Icons.Filled.Whatshot, actionItems))
        }

        val scifiItems = (movies + series).filter {
            val gLow = it.genre.lowercase()
            val cLow = it.categoryName.lowercase()
            gLow.contains("sci") || gLow.contains("kurgu") || gLow.contains("fantastik") ||
                cLow.contains("sci") || cLow.contains("kurgu") || cLow.contains("fantastik")
        }.take(25)
        if (scifiItems.isNotEmpty() && (selectedFilter == HubFilter.ALL || selectedFilter == HubFilter.SCIFI)) {
            list.add(HubRail("scifi", "🚀 Bilim Kurgu & Fantastik", Icons.Filled.RocketLaunch, scifiItems))
        }

        val topRated = (movies + series).filter { (it.rating.toDoubleOrNull() ?: 0.0) >= 7.8 }
            .sortedByDescending { it.rating.toDoubleOrNull() ?: 0.0 }.take(25)
        if (topRated.isNotEmpty() && (selectedFilter == HubFilter.ALL || selectedFilter == HubFilter.TOP_RATED)) {
            list.add(HubRail("top_rated", "⭐ IMDb 8+ Başyapıtlar", Icons.Filled.Star, topRated))
        }

        val turkishItems = (movies + series).filter {
            val nLow = it.name.lowercase()
            val cLow = it.categoryName.lowercase()
            cLow.contains("yerli") || cLow.contains("türk") || nLow.contains("yerli") || nLow.contains("türk")
        }.take(25)
        if (turkishItems.isNotEmpty() && selectedFilter == HubFilter.ALL) {
            list.add(HubRail("turkish", "🇹🇷 Yerli Sinema & Dizi Arşivi", Icons.Filled.Theaters, turkishItems))
        }

        // Additional category rails
        val customCats = movieCategories.filter { !it.hidden && it.name.isNotBlank() }.take(6)
        for (cat in customCats) {
            val catMovies = movies.filter { it.categoryId == cat.id || it.categoryName.equals(cat.name, true) }.take(20)
            if (catMovies.size >= 3 && selectedFilter == HubFilter.ALL) {
                list.add(HubRail("cat_${cat.id}", "🎬 ${cat.name}", Icons.Filled.Movie, catMovies))
            }
        }

        list
    }

    LaunchedEffect(contentFocusTrigger) {
        if (!railExpanded) {
            playBtnFocusRequester.requestFocus()
        }
    }

    BackHandler {
        if (!railExpanded) {
            onExpandRail()
        } else {
            onExit()
        }
    }

    Box(
        Modifier
            .fillMaxSize()
            .background(Color(0xFF070B14))
    ) {
        // Dynamic Ambient Backdrop
        if (activeHero != null) {
            val bgImage = activeHero.poster
            if (bgImage.isNotBlank()) {
                AsyncImage(
                    model = bgImage,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(420.dp)
                        .align(Alignment.TopCenter)
                )
                // Gradient Scrim
                Box(
                    Modifier
                        .fillMaxWidth()
                        .height(420.dp)
                        .background(
                            Brush.verticalGradient(
                                listOf(
                                    Color(0xFF070B14).copy(alpha = 0.35f),
                                    Color(0xFF070B14).copy(alpha = 0.85f),
                                    Color(0xFF070B14)
                                )
                            )
                        )
                )
                Box(
                    Modifier
                        .fillMaxWidth()
                        .height(420.dp)
                        .background(
                            Brush.horizontalGradient(
                                listOf(
                                    Color(0xFF070B14).copy(alpha = 0.95f),
                                    Color(0xFF070B14).copy(alpha = 0.7f),
                                    Color.Transparent
                                )
                            )
                        )
                )
            }
        }

        // Main Content Structure
        Column(
            Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp, vertical = 12.dp)
        ) {
            // 1. Top Filter Pills
            Row(
                Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                HubFilter.values().forEach { filter ->
                    val isSelected = filter == selectedFilter
                    var pillFocused by remember { mutableStateOf(false) }
                    val isTr = copy.cinemaHub == "Film & Dizi"
                    val label = if (isTr) filter.titleTr else filter.titleEn

                    Box(
                        Modifier
                            .onFocusChanged { pillFocused = it.isFocused }
                            .clip(RoundedCornerShape(20.dp))
                            .background(
                                when {
                                    pillFocused -> Color(0xFF00F0FF).copy(alpha = 0.35f)
                                    isSelected -> Color(0xFF00F0FF).copy(alpha = 0.22f)
                                    else -> Color.White.copy(alpha = 0.08f)
                                }
                            )
                            .border(
                                width = if (pillFocused) 2.dp else if (isSelected) 1.2.dp else 0.6.dp,
                                color = if (pillFocused) Color(0xFF00F0FF) else if (isSelected) Color(0xFF38BDF8) else Color.White.copy(alpha = 0.15f),
                                shape = RoundedCornerShape(20.dp)
                            )
                            .clickable { selectedFilter = filter }
                            .focusable()
                            .padding(horizontal = 16.dp, vertical = 7.dp)
                    ) {
                        Text(
                            text = label,
                            color = if (pillFocused || isSelected) Color.White else Color.White.copy(alpha = 0.75f),
                            fontWeight = if (isSelected || pillFocused) FontWeight.Bold else FontWeight.Medium,
                            fontSize = 13.sp
                        )
                    }
                }
            }

            Spacer(Modifier.height(14.dp))

            // 2. Cinematic Hero Banner
            if (activeHero != null) {
                HeroBanner(
                    vod = activeHero,
                    isFav = favIds.contains(activeHero.id),
                    playFocusRequester = playBtnFocusRequester,
                    onPlay = { onPlay(activeHero) },
                    onDetail = { onOpenDetail(activeHero) },
                    onFav = { onFav(activeHero) }
                )
            } else {
                Spacer(Modifier.height(180.dp))
            }

            Spacer(Modifier.height(14.dp))

            // 3. Category Poster Rails (LazyColumn)
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(20.dp),
                contentPadding = PaddingValues(bottom = 32.dp)
            ) {
                itemsIndexed(rails) { railIndex, rail ->
                    HubRailRow(
                        rail = rail,
                        favorites = favIds,
                        isFirstRail = railIndex == 0,
                        firstItemFocusRequester = if (railIndex == 0) firstItemFocusRequester else null,
                        onFocused = { item -> focusedItem = item },
                        onClick = { item -> onOpenDetail(item) },
                        onExpandRail = onExpandRail
                    )
                }
            }
        }
    }
}

@Composable
private fun HeroBanner(
    vod: VodEntity,
    isFav: Boolean,
    playFocusRequester: FocusRequester,
    onPlay: () -> Unit,
    onDetail: () -> Unit,
    onFav: () -> Unit
) {
    Column(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Title
        Text(
            text = vod.name,
            color = Color.White,
            style = MaterialTheme.typography.headlineLarge.copy(
                fontWeight = FontWeight.Black,
                fontSize = 28.sp,
                letterSpacing = 0.4.sp
            ),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )

        // Metadata Chips Row
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val rVal = vod.rating.toDoubleOrNull() ?: 0.0
            if (rVal > 0.0) {
                Box(
                    Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(Color(0xFFF1C40F))
                        .padding(horizontal = 7.dp, vertical = 2.5.dp)
                ) {
                    Text(
                        "IMDb ${"%.1f".format(rVal)}",
                        color = Color.Black,
                        fontWeight = FontWeight.Black,
                        fontSize = 11.sp
                    )
                }
            }

            if (vod.year.isNotBlank() && vod.year != "0") {
                MetaBadge(vod.year)
            }

            MetaBadge(if (vod.kind == "SERIES") "DİZİ" else "4K HDR")

            if (vod.genre.isNotBlank()) {
                vod.genre.split(',', '/', '·').take(3).forEach { g ->
                    val clean = g.trim()
                    if (clean.isNotBlank()) {
                        MetaBadge(clean, transparent = true)
                    }
                }
            }
        }

        // Plot preview
        if (vod.plot.isNotBlank()) {
            Text(
                text = vod.plot,
                color = Color.White.copy(alpha = 0.8f),
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontSize = 13.sp,
                    lineHeight = 18.sp
                ),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.fillMaxWidth(0.75f)
            )
        }

        // Action Buttons Row
        Row(
            Modifier.padding(top = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Play Button
            var playFocused by remember { mutableStateOf(false) }
            Box(
                Modifier
                    .focusRequester(playFocusRequester)
                    .onFocusChanged { playFocused = it.isFocused }
                    .clip(RoundedCornerShape(12.dp))
                    .background(
                        if (playFocused) {
                            Brush.horizontalGradient(listOf(Color(0xFF00F0FF), Color(0xFF38BDF8)))
                        } else {
                            Brush.horizontalGradient(listOf(Color(0xFF00F0FF).copy(alpha = 0.85f), Color(0xFF0284C7).copy(alpha = 0.85f)))
                        }
                    )
                    .border(
                        width = if (playFocused) 2.dp else 0.dp,
                        color = if (playFocused) Color.White else Color.Transparent,
                        shape = RoundedCornerShape(12.dp)
                    )
                    .clickable { onPlay() }
                    .focusable()
                    .padding(horizontal = 20.dp, vertical = 9.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        Icons.Filled.PlayArrow,
                        contentDescription = null,
                        tint = Color.Black,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        "Şimdi İzle",
                        color = Color.Black,
                        fontWeight = FontWeight.Black,
                        fontSize = 13.sp
                    )
                }
            }

            // Details Button
            var detailFocused by remember { mutableStateOf(false) }
            Box(
                Modifier
                    .onFocusChanged { detailFocused = it.isFocused }
                    .clip(RoundedCornerShape(12.dp))
                    .background(
                        if (detailFocused) Color.White.copy(alpha = 0.28f) else Color.White.copy(alpha = 0.12f)
                    )
                    .border(
                        width = if (detailFocused) 2.dp else 0.8.dp,
                        color = if (detailFocused) Color(0xFF00F0FF) else Color.White.copy(alpha = 0.25f),
                        shape = RoundedCornerShape(12.dp)
                    )
                    .clickable { onDetail() }
                    .focusable()
                    .padding(horizontal = 18.dp, vertical = 9.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        Icons.Filled.Info,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(17.dp)
                    )
                    Text(
                        "Detaylar",
                        color = Color.White,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 13.sp
                    )
                }
            }

            // Fav Button
            var favFocused by remember { mutableStateOf(false) }
            Box(
                Modifier
                    .onFocusChanged { favFocused = it.isFocused }
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(
                        if (favFocused) Color.White.copy(alpha = 0.28f) else Color.White.copy(alpha = 0.12f)
                    )
                    .border(
                        width = if (favFocused) 2.dp else 0.8.dp,
                        color = if (favFocused) Color(0xFF00F0FF) else Color.White.copy(alpha = 0.25f),
                        shape = CircleShape
                    )
                    .clickable { onFav() }
                    .focusable(),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    if (isFav) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                    contentDescription = null,
                    tint = if (isFav) Color(0xFFE74C3C) else Color.White,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@Composable
private fun MetaBadge(text: String, transparent: Boolean = false) {
    Box(
        Modifier
            .clip(RoundedCornerShape(5.dp))
            .background(if (transparent) Color.White.copy(alpha = 0.08f) else Color.White.copy(alpha = 0.15f))
            .border(0.6.dp, Color.White.copy(alpha = 0.25f), RoundedCornerShape(5.dp))
            .padding(horizontal = 6.dp, vertical = 2.dp)
    ) {
        Text(
            text,
            color = Color.White,
            fontWeight = FontWeight.SemiBold,
            fontSize = 10.5.sp
        )
    }
}

@Composable
private fun HubRailRow(
    rail: HubRail,
    favorites: Set<String>,
    isFirstRail: Boolean,
    firstItemFocusRequester: FocusRequester?,
    onFocused: (VodEntity) -> Unit,
    onClick: (VodEntity) -> Unit,
    onExpandRail: () -> Unit
) {
    Column(
        Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Rail Title
        Row(
            Modifier.padding(horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                rail.icon,
                contentDescription = null,
                tint = Color(0xFF00F0FF),
                modifier = Modifier.size(18.dp)
            )
            Text(
                rail.title,
                color = Color.White,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
            )
        }

        // Horizontal Posters Carousel
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            contentPadding = PaddingValues(horizontal = 4.dp, vertical = 6.dp)
        ) {
            itemsIndexed(rail.items, key = { _, item -> "${rail.id}_${item.id}" }) { idx, item ->
                HubPosterCard(
                    item = item,
                    isFav = favorites.contains(item.id),
                    focusRequester = if (isFirstRail && idx == 0) firstItemFocusRequester else null,
                    onFocus = { onFocused(item) },
                    onClick = { onClick(item) },
                    onLeftBoundary = {
                        if (idx == 0) onExpandRail()
                    }
                )
            }
        }
    }
}

@Composable
private fun HubPosterCard(
    item: VodEntity,
    isFav: Boolean,
    focusRequester: FocusRequester?,
    onFocus: () -> Unit,
    onClick: () -> Unit,
    onLeftBoundary: () -> Unit
) {
    var isFocused by remember { mutableStateOf(false) }

    Column(
        Modifier
            .width(132.dp)
            .scale(if (isFocused) 1.08f else 1f)
            .then(if (focusRequester != null) Modifier.focusRequester(focusRequester) else Modifier)
            .onFocusChanged {
                isFocused = it.isFocused
                if (it.isFocused) onFocus()
            }
            .onPreviewKeyEvent { ev ->
                if (ev.nativeKeyEvent.action == KeyEvent.ACTION_DOWN && ev.nativeKeyEvent.keyCode == KeyEvent.KEYCODE_DPAD_LEFT) {
                    onLeftBoundary()
                }
                false
            }
            .clickable { onClick() }
            .focusable(),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        // Poster Box
        Box(
            Modifier
                .fillMaxWidth()
                .aspectRatio(2f / 3f)
                .clip(RoundedCornerShape(12.dp))
                .background(Color.White.copy(alpha = 0.06f))
                .border(
                    width = if (isFocused) 2.5.dp else 0.8.dp,
                    color = if (isFocused) Color(0xFF00F0FF) else Color.White.copy(alpha = 0.12f),
                    shape = RoundedCornerShape(12.dp)
                )
        ) {
            if (item.poster.isNotBlank()) {
                AsyncImage(
                    model = item.poster,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Icon(
                        Icons.Filled.Movie,
                        contentDescription = null,
                        tint = Color.White.copy(alpha = 0.35f),
                        modifier = Modifier.size(36.dp)
                    )
                }
            }

            // Rating Badge on Poster top right
            val rating = item.rating.toDoubleOrNull() ?: 0.0
            if (rating > 0.0) {
                Box(
                    Modifier
                        .align(Alignment.TopEnd)
                        .padding(5.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(Color.Black.copy(alpha = 0.75f))
                        .padding(horizontal = 4.dp, vertical = 1.5.dp)
                ) {
                    Text(
                        "★ ${"%.1f".format(rating)}",
                        color = Color(0xFFF1C40F),
                        fontWeight = FontWeight.Bold,
                        fontSize = 9.5.sp
                    )
                }
            }

            if (isFav) {
                Icon(
                    Icons.Filled.Favorite,
                    contentDescription = null,
                    tint = Color(0xFFE74C3C),
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(5.dp)
                        .size(14.dp)
                )
            }
        }

        // Title text below poster
        Text(
            text = item.name,
            color = if (isFocused) Color(0xFF00F0FF) else Color.White,
            fontWeight = if (isFocused) FontWeight.Bold else FontWeight.Medium,
            fontSize = 11.5.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}
