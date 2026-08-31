package com.ray.iptv.ui.screens.catalog

import androidx.compose.animation.AnimatedVisibility
import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyHorizontalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed as gridItemsIndexed
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.DownloadDone
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.MovieFilter
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PlayCircleOutline
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.Sort
import androidx.compose.material.icons.filled.SortByAlpha
import androidx.compose.material.icons.filled.Star
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.scale
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Icon
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import coil.compose.AsyncImage
import com.ray.iptv.data.local.CategoryEntity
import com.ray.iptv.data.local.DownloadEntity
import com.ray.iptv.data.local.EpisodeEntity
import com.ray.iptv.data.local.FavoriteEntity
import com.ray.iptv.data.local.VodEntity
import com.ray.iptv.data.meta.CastPerson
import com.ray.iptv.data.meta.VodMeta
import com.ray.iptv.ui.glass.GlassPanel
import com.ray.iptv.ui.i18n.Copy
import com.ray.iptv.ui.input.rayFocusRequester
import com.ray.iptv.ui.input.tryFocus
import com.ray.iptv.ui.motion.RayCrossfade
import com.ray.iptv.ui.motion.RaySwitch
import com.ray.iptv.ui.motion.rayPanelEnter
import com.ray.iptv.ui.motion.rayPanelExit
import com.ray.iptv.ui.motion.rayRailEnter
import com.ray.iptv.ui.motion.rayRailExit
import com.ray.iptv.ui.theme.LocalGlass
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.isActive

private enum class VodSort { NAME, RATING, RANDOM, ADDED }

@Composable
fun VodCinemaScreen(
    isSeries: Boolean,
    copy: Copy,
    categories: List<CategoryEntity>,
    items: List<VodEntity>,
    allItems: List<VodEntity>,
    allCount: Int = allItems.size,
    categoryCounts: Map<String, Int> = emptyMap(),
    selectedCategory: String,
    showCategories: Boolean,
    pinned: VodEntity?,
    extras: VodMeta,
    extrasId: String = "",
    onPreview: (VodEntity) -> Unit = {},
    episodes: List<EpisodeEntity> = emptyList(),
    favorites: List<FavoriteEntity> = emptyList(),
    downloads: List<DownloadEntity> = emptyList(),
    onCategory: (String) -> Unit,
    onPickCategory: () -> Unit,
    onBackToCategories: () -> Unit,
    onExpandRail: () -> Unit,
    onOpen: (VodEntity) -> Unit,
    onClosePin: () -> Unit,
    onPlay: (VodEntity) -> Unit,
    onExternal: (VodEntity) -> Unit = {},
    onPlayEpisode: (EpisodeEntity) -> Unit = {},
    onFav: (VodEntity) -> Unit,
    onDownload: (VodEntity) -> Unit,
    onDownloadEpisode: (EpisodeEntity) -> Unit = {},
    onTrailer: () -> Unit,
    onLoadMore: () -> Unit = {},
    onSearch: () -> Unit = {},
    railExpanded: Boolean = false,
    contentFocusTrigger: Long = 0L,
    onExit: () -> Unit = {}
) {
    val scope = rememberCoroutineScope()
    val focus = LocalFocusManager.current
    val catFocus = remember { FocusRequester() }
    val selectedCatFocus = remember { FocusRequester() }
    val playFocus = remember { FocusRequester() }
    var hovered by remember { mutableStateOf<VodEntity?>(null) }
    var season by remember { mutableIntStateOf(0) }
    var sort by remember { mutableStateOf(VodSort.NAME) }
    var sortMenu by remember { mutableStateOf(false) }
    var randomSeed by remember { mutableIntStateOf(0) }
    val stripFocus = remember { FocusRequester() }
    val catListState = rememberLazyListState()

    val favCount = remember(favorites, isSeries) {
        favorites.count { it.kind == if (isSeries) "SERIES" else "MOVIE" }
    }
    val categoryKeys = remember(categories, favCount) {
        buildList {
            add("last50")
            add("fav")
            add("popular")
            add("trend")
            add("")
            categories.forEach { add(it.id) }
        }
    }
    val targetCatIndex = remember(categoryKeys, selectedCategory) {
        val idx = categoryKeys.indexOf(selectedCategory)
        if (idx >= 0) idx else 0
    }

    val focusToSelectedCategory: () -> Unit = {
        scope.launch {
            if (!showCategories) onBackToCategories()
            delay(50)
            if (targetCatIndex in 0 until categoryKeys.size) {
                runCatching { catListState.scrollToItem(targetCatIndex) }
            }
            repeat(35) {
                delay(30)
                if (selectedCatFocus.tryFocus()) return@launch
            }
            if (targetCatIndex == 0) {
                catFocus.tryFocus()
            }
        }
    }

    BackHandler {
        when {
            sortMenu -> sortMenu = false
            pinned != null -> onClosePin()
            !showCategories -> focusToSelectedCategory()
            railExpanded -> onExit()
            else -> {
                onExpandRail()
                focus.moveFocus(FocusDirection.Left)
            }
        }
    }

    val visible = remember(items, sort, randomSeed) {
        when (sort) {
            VodSort.NAME -> items.sortedBy { it.name.lowercase() }
            VodSort.RATING -> items.sortedByDescending { it.rating.toDoubleOrNull() ?: 0.0 }
            VodSort.RANDOM -> items.shuffled(kotlin.random.Random(randomSeed.toLong()))
            VodSort.ADDED -> items
        }
    }
    val pinnedEpisodes = remember(pinned?.id, episodes, isSeries) {
        if (!isSeries || pinned == null) episodes
        else episodes.filter { it.seriesId == pinned.id }
    }

    LaunchedEffect(pinned?.id, visible.size, visible.firstOrNull()?.id) {
        if (pinned != null) {
            hovered = pinned
            return@LaunchedEffect
        }
        if (hovered == null || visible.none { it.id == hovered?.id }) hovered = visible.firstOrNull()
    }
    LaunchedEffect(pinned?.id, pinnedEpisodes) {
        if (pinned != null) {
            hovered = pinned
            season = pinnedEpisodes.map { it.season }.distinct().sorted().firstOrNull() ?: 0
            playFocus.tryFocus()
        }
    }
    LaunchedEffect(showCategories, pinned?.id, railExpanded, contentFocusTrigger, selectedCategory) {
        if (showCategories && pinned == null && !railExpanded) {
            delay(50)
            if (targetCatIndex in 0 until categoryKeys.size) {
                runCatching { catListState.scrollToItem(targetCatIndex) }
            }
            repeat(35) {
                delay(30)
                if (selectedCatFocus.tryFocus()) return@LaunchedEffect
            }
            if (targetCatIndex == 0) {
                catFocus.tryFocus()
            }
        }
    }

    val hero = pinned ?: hovered ?: visible.firstOrNull()
    val heroExtras = extras.takeIf { extrasId == hero?.id }
    LaunchedEffect(hero?.id, pinned?.id) {
        if (pinned == null) hero?.let(onPreview)
    }
    LaunchedEffect(showCategories, pinned?.id, contentFocusTrigger, visible.size) {
        if (!showCategories && pinned == null) {
            repeat(25) {
                delay(30)
                if (stripFocus.tryFocus()) return@LaunchedEffect
            }
        }
    }


    val counts = remember(categoryCounts) { categoryCounts }
    val totalCount = if (allCount > 0) allCount else allItems.size
    val catTitle = when (selectedCategory) {
        "last50" -> if (isSeries) copy.last50Series else copy.last50Films
        "fav" -> if (isSeries) copy.favShows else copy.favFilms
        "popular" -> if (isSeries) copy.popular50Series else copy.popular50Films
        "trend" -> if (isSeries) copy.trendSeries else copy.trendFilms
        "" -> if (isSeries) copy.allSeries else copy.allFilms
        else -> categories.firstOrNull { it.id == selectedCategory }?.name
            ?: if (isSeries) copy.series else copy.movies
    }

    BoxWithConstraints(Modifier.fillMaxSize()) {
        val wide = maxWidth >= 560.dp
        Row(Modifier.fillMaxSize(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            AnimatedVisibility(
                visible = showCategories,
                enter = rayRailEnter(),
                exit = rayRailExit(),
                modifier = if (wide) {
                    Modifier
                        .width(236.dp)
                        .fillMaxHeight()
                } else {
                    Modifier
                        .weight(1f)
                        .fillMaxHeight()
                }
            ) {
                VodCatPane(
                    copy = copy,
                    title = if (isSeries) copy.series else copy.movies,
                    categories = categories,
                    selected = selectedCategory,
                    allCount = totalCount,
                    counts = counts,
                    onCategory = onCategory,
                    onPick = {
                        onPickCategory()
                        scope.launch {
                            delay(120)
                            repeat(30) {
                                if (stripFocus.tryFocus()) return@launch
                                delay(25)
                            }
                        }
                    },

                    onLeft = {

                        onExpandRail()
                        focus.moveFocus(FocusDirection.Left)
                    },
                    isSeries = isSeries,
                    favCount = favCount,
                    firstFocus = catFocus,
                    selectedFocus = selectedCatFocus,
                    listState = catListState,
                    modifier = Modifier.fillMaxSize()
                )
            }
            AnimatedVisibility(
                visible = !showCategories || wide,
                enter = rayPanelEnter(),
                exit = rayPanelExit(),
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
            ) {
                RaySwitch(showCategories, Modifier.fillMaxSize()) { cats ->
                if (cats) {
                    VodBrowsePane(
                        copy = copy,
                        isSeries = isSeries,
                        items = visible,
                        hero = hero,
                        extras = heroExtras,
                        onHover = { hovered = it },
                        onOpen = { item ->
                            hovered = item
                            onOpen(item)
                        },
                        onLeftFromFirst = focusToSelectedCategory,
                        onLoadMore = onLoadMore,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    VodCinemaPane(
                        copy = copy,
                        isSeries = isSeries,
                        items = visible,
                        hero = hero,
                        extras = heroExtras ?: VodMeta("", "", "", "", ""),
                        pinned = pinned,
                        categoryName = catTitle,
                        episodes = pinnedEpisodes,
                        season = season,
                        onSeason = { season = it },
                        favorites = favorites,
                        downloads = downloads,
                        playFocus = playFocus,
                        sort = sort,
                        onSearch = onSearch,
                        onOpenSort = { sortMenu = true },
                        onHover = { hovered = it },
                        onOpen = onOpen,
                        onClosePin = onClosePin,
                        onPlay = onPlay,
                        onExternal = onExternal,
                        onPlayEpisode = onPlayEpisode,
                        onFav = onFav,
                        onDownload = onDownload,
                        onDownloadEpisode = onDownloadEpisode,
                        onTrailer = onTrailer,
                        onLeftFromFirst = focusToSelectedCategory,
                        onLoadMore = onLoadMore,
                        firstFocus = stripFocus,
                        modifier = Modifier.fillMaxSize()
                    )
                }
                }
            }
        }
        if (sortMenu) {
            VodSortMenu(
                copy = copy,
                selected = sort,
                onSelect = {
                    if (it == VodSort.RANDOM) randomSeed++
                    sort = it
                    sortMenu = false
                },
                onDismiss = { sortMenu = false }
            )
        }
    }
}

@Composable
private fun VodCatPane(
    copy: Copy,
    title: String,
    categories: List<CategoryEntity>,
    selected: String,
    allCount: Int,
    counts: Map<String, Int>,
    onCategory: (String) -> Unit,
    onPick: () -> Unit,
    onLeft: () -> Unit,
    isSeries: Boolean,
    favCount: Int,
    firstFocus: FocusRequester? = null,
    selectedFocus: FocusRequester? = null,
    listState: androidx.compose.foundation.lazy.LazyListState = androidx.compose.foundation.lazy.rememberLazyListState(),
    modifier: Modifier = Modifier
) {
    val g = LocalGlass.current
    GlassPanel(
        strong = true,
        radius = 12.dp,
        modifier = modifier.onPreviewKeyEvent { e ->
            if (e.type == KeyEventType.KeyDown && e.key == Key.DirectionLeft) {
                onLeft(); true
            } else false
        }
    ) {
        Column(Modifier.padding(10.dp)) {
            Text(
                title,
                color = g.muted,
                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
            )
            Spacer(Modifier.height(8.dp))
            LazyColumn(state = listState, verticalArrangement = Arrangement.spacedBy(2.dp)) {
                item {
                    VodCatRow(
                        if (isSeries) copy.last50Series else copy.last50Films,
                        minOf(50, allCount),
                        selected == "last50",
                        onClick = { onCategory("last50"); onPick() },
                        onFocus = { if (selected != "last50") onCategory("last50") },
                        focusRequester = if (selected == "last50") selectedFocus else firstFocus
                    )
                }
                item {
                    VodCatRow(
                        if (isSeries) copy.favShows else copy.favFilms,
                        favCount,
                        selected == "fav",
                        onClick = { onCategory("fav"); onPick() },
                        onFocus = { if (selected != "fav") onCategory("fav") },
                        focusRequester = if (selected == "fav") selectedFocus else null
                    )
                }
                item {
                    VodCatRow(
                        if (isSeries) copy.popular50Series else copy.popular50Films,
                        minOf(50, allCount),
                        selected == "popular",
                        onClick = { onCategory("popular"); onPick() },
                        onFocus = { if (selected != "popular") onCategory("popular") },
                        focusRequester = if (selected == "popular") selectedFocus else null
                    )
                }
                item {
                    VodCatRow(
                        if (isSeries) copy.trendSeries else copy.trendFilms,
                        minOf(50, allCount),
                        selected == "trend",
                        onClick = { onCategory("trend"); onPick() },
                        onFocus = { if (selected != "trend") onCategory("trend") },
                        focusRequester = if (selected == "trend") selectedFocus else null
                    )
                }
                item {
                    VodCatRow(
                        if (isSeries) copy.allSeries else copy.allFilms,
                        allCount,
                        selected.isEmpty(),
                        onClick = { onCategory(""); onPick() },
                        onFocus = { if (selected.isNotEmpty()) onCategory("") },
                        focusRequester = if (selected.isEmpty()) selectedFocus else null
                    )
                }
                items(categories, key = { it.id }) { cat ->
                    VodCatRow(
                        cat.name,
                        counts[cat.id] ?: 0,
                        selected == cat.id,
                        onClick = { onCategory(cat.id); onPick() },
                        onFocus = { if (selected != cat.id) onCategory(cat.id) },
                        focusRequester = if (selected == cat.id) selectedFocus else null
                    )
                }
            }
        }
    }
}

@Composable
private fun VodCatRow(
    label: String,
    count: Int,
    selected: Boolean,
    onClick: () -> Unit,
    onFocus: () -> Unit,
    focusRequester: FocusRequester? = null
) {
    var focused by remember { mutableStateOf(false) }
    val g = LocalGlass.current
    val active = focused || selected
    GlassPanel(
        focused = focused,
        strong = selected,
        accentFill = selected && !focused,
        radius = 6.dp,
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .height(52.dp)
            .rayFocusRequester(focusRequester)
            .onFocusChanged {
                focused = it.isFocused
                if (it.isFocused) onFocus()
            }
            .onPreviewKeyEvent { e ->
                if (e.key == Key.DirectionRight || e.key == Key.DirectionCenter || e.key == Key.Enter || e.key == Key.NumPadEnter) {
                    if (e.type == KeyEventType.KeyDown) {
                        onClick()
                    }
                    return@onPreviewKeyEvent true
                }
                false
            }
    ) {

        Row(
            Modifier.fillMaxSize().padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                Modifier
                    .width(3.dp)
                    .height(22.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(if (selected) g.accent.copy(alpha = 0.72f) else Color.Transparent)
            )
            Spacer(Modifier.width(8.dp))
            Text(
                label,
                color = if (active) g.text else g.text.copy(alpha = 0.7f),
                style = MaterialTheme.typography.bodyLarge.copy(
                    fontWeight = if (active) FontWeight.Bold else FontWeight.Medium
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
            Spacer(Modifier.width(8.dp))
            CategoryCountBadge(count = count, emphasized = active)
        }
    }
}

@Composable
private fun CategoryCountBadge(count: Int, emphasized: Boolean) {
    val g = LocalGlass.current
    val label = count.toString()
    val digits = label.length
    val diameter = 24.dp
    val circle = digits <= 3
    val width = if (circle) diameter else (digits * 7 + 14).dp
    val fontSize = when {
        digits <= 2 -> 11.sp
        digits == 3 -> 10.sp
        else -> 9.sp
    }
    val shape = if (circle) CircleShape else RoundedCornerShape(percent = 50)
    val fill = if (emphasized) g.accent.copy(alpha = 0.22f) else Color.White.copy(alpha = 0.06f)
    val stroke = if (emphasized) g.accent.copy(alpha = 0.7f) else Color.White.copy(alpha = 0.12f)
    Box(
        modifier = Modifier
            .size(width = width, height = diameter)
            .clip(shape)
            .background(fill)
            .border(1.dp, stroke, shape),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            color = if (emphasized) g.text else g.muted,
            fontSize = fontSize,
            fontWeight = FontWeight.ExtraBold,
            lineHeight = fontSize,
            maxLines = 1,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun VodBrowsePane(
    copy: Copy,
    isSeries: Boolean,
    items: List<VodEntity>,
    hero: VodEntity?,
    extras: VodMeta?,
    onHover: (VodEntity) -> Unit,
    onOpen: (VodEntity) -> Unit,
    onLeftFromFirst: () -> Unit,
    onLoadMore: () -> Unit,
    modifier: Modifier = Modifier
) {
    val g = LocalGlass.current
    val art = extras?.backdrop?.ifBlank { hero?.poster.orEmpty() } ?: hero?.poster.orEmpty()

    Column(
        modifier = modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // 1. Üst Detay & Poster Çerçevesi (Top Hero Details Glass Frame)
        GlassPanel(
            strong = true,
            radius = 12.dp,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            Box(Modifier.fillMaxSize()) {
                if (art.isNotBlank()) {
                    RayCrossfade(hero?.id.orEmpty(), Modifier.fillMaxSize()) {
                        AsyncImage(
                            model = art,
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop,
                            alpha = 0.24f
                        )
                    }
                    Box(
                        Modifier.fillMaxSize().background(
                            Brush.horizontalGradient(
                                listOf(
                                    Color.Black.copy(alpha = 0.90f),
                                    Color.Black.copy(alpha = 0.72f),
                                    Color.Black.copy(alpha = 0.40f)
                                )
                            )
                        )
                    )
                }
                BoxWithConstraints(
                    Modifier
                        .fillMaxSize()
                        .padding(18.dp)
                ) {
                    if (hero == null) {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text(if (isSeries) copy.pickSeries else copy.pickMovie, color = g.muted)
                        }
                    } else {
                        val posterW = (maxHeight * 2f / 3f).coerceAtMost(160.dp).coerceAtLeast(70.dp)
                        Row(
                            Modifier.fillMaxSize(),
                            horizontalArrangement = Arrangement.spacedBy(18.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            CinemaPosterThumb(hero.poster.ifBlank { extras?.poster.orEmpty() }, posterW)
                            VodInfo(
                                copy = copy,
                                item = hero,
                                extras = extras,
                                pinned = false,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }
        }

        // 2. Alt "Sıradaki İçerikler" Çerçevesi (Bottom Up Next Strip Glass Frame)
        GlassPanel(
            strong = true,
            radius = 12.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                Text(
                    if (isSeries) copy.seriesUpNext else copy.moviesUpNext,
                    color = g.text,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                PosterStrip(
                    items = items,
                    empty = if (isSeries) copy.noSeries else copy.noMovies,
                    compact = true,
                    focusId = hero?.id,
                    onHover = onHover,
                    onOpen = onOpen,
                    onLeftFromFirst = onLeftFromFirst,
                    onLoadMore = onLoadMore
                )
            }
        }
    }
}

@Composable
private fun VodCinemaPane(
    copy: Copy,
    isSeries: Boolean,
    items: List<VodEntity>,
    hero: VodEntity?,
    extras: VodMeta,
    pinned: VodEntity?,
    categoryName: String,
    episodes: List<EpisodeEntity>,
    season: Int,
    onSeason: (Int) -> Unit,
    favorites: List<FavoriteEntity>,
    downloads: List<DownloadEntity>,
    playFocus: FocusRequester,
    sort: VodSort,
    onSearch: () -> Unit,
    onOpenSort: () -> Unit,
    onHover: (VodEntity) -> Unit,
    onOpen: (VodEntity) -> Unit,
    onClosePin: () -> Unit,
    onPlay: (VodEntity) -> Unit,
    onExternal: (VodEntity) -> Unit,
    onPlayEpisode: (EpisodeEntity) -> Unit,
    onFav: (VodEntity) -> Unit,
    onDownload: (VodEntity) -> Unit,
    onDownloadEpisode: (EpisodeEntity) -> Unit,
    onTrailer: () -> Unit,
    onLeftFromFirst: () -> Unit,
    onLoadMore: () -> Unit,
    firstFocus: FocusRequester? = null,
    modifier: Modifier = Modifier
) {
    val g = LocalGlass.current
    val art = extras.backdrop.ifBlank { hero?.poster.orEmpty() }
    val fav = hero != null && favorites.any { it.mediaId == hero.id }
    val dl = downloads.firstOrNull { it.mediaId == hero?.id }
    val restoreFocus = remember { FocusRequester() }
    var expandedRows by remember { mutableStateOf(false) }

    LaunchedEffect(pinned) {
        if (pinned != null) {
            expandedRows = false
        } else {
            repeat(30) {
                if ((firstFocus ?: restoreFocus).tryFocus()) return@LaunchedEffect
                delay(25)
            }
        }
    }
    Box(modifier.fillMaxSize().clip(RoundedCornerShape(16.dp))) {
        RayCrossfade(hero?.id.orEmpty(), Modifier.fillMaxSize()) {
            if (art.isNotBlank()) {
                AsyncImage(art, null, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
            } else {
                Box(Modifier.fillMaxSize().background(g.wallpaperDark))
            }
        }
        Box(
            Modifier.fillMaxSize().background(
                Brush.horizontalGradient(
                    listOf(
                        Color.Black.copy(alpha = 0.94f),
                        Color.Black.copy(alpha = 0.76f),
                        Color.Black.copy(alpha = 0.35f),
                        Color.Transparent
                    )
                )
            )
        )
        Box(
            Modifier.fillMaxSize().background(
                Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.88f)))
            )
        )
        val people = remember(extras) { castPeople(extras) }
        Column(Modifier.fillMaxSize().padding(horizontal = 22.dp, vertical = 14.dp)) {
            if (hero != null) {
                VodInfo(
                    copy = copy,
                    item = hero,
                    extras = extras,
                    pinned = pinned != null,
                    includeCast = pinned != null && !isSeries,
                    people = people,
                    episode = if (pinned != null && isSeries) {
                        episodes.filter { it.season == season }.firstOrNull()
                    } else null,
                    seasonCount = episodes.map { it.season }.distinct().size,
                    episodeCount = episodes.size,
                    compact = expandedRows && pinned == null,
                    modifier = Modifier.fillMaxWidth(if (pinned != null) 0.96f else if (expandedRows) 0.92f else 0.72f)
                )
            } else {
                Text(if (isSeries) copy.noSeries else copy.noMovies, color = g.muted)
            }
            if (pinned != null || !expandedRows) {
                Spacer(Modifier.weight(1f))
            } else {
                Spacer(Modifier.height(10.dp))
            }
            if (pinned == null) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth().padding(bottom = 6.dp)
                ) {
                    Text(
                        categoryName,
                        color = g.text,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color.White.copy(alpha = 0.08f))
                            .border(0.8.dp, Color.White.copy(alpha = 0.16f), RoundedCornerShape(8.dp))
                            .clickable { expandedRows = !expandedRows }
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Icon(
                            if (expandedRows) Icons.Filled.KeyboardArrowUp else Icons.Filled.KeyboardArrowDown,
                            contentDescription = null,
                            tint = g.accent,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(
                            if (expandedRows) "1 Satır" else "2 Satır",
                            color = Color.White.copy(alpha = 0.9f),
                            style = MaterialTheme.typography.labelMedium.copy(fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                        )
                    }
                }
                if (expandedRows) {
                    PosterStrip2Rows(
                        items = items,
                        empty = if (isSeries) copy.noSeries else copy.noMovies,
                        onHover = onHover,
                        onOpen = onOpen,
                        onLeftFromFirst = onLeftFromFirst,
                        onCollapseToSingleRow = { expandedRows = false },
                        onLoadMore = onLoadMore,
                        firstFocus = firstFocus ?: restoreFocus,
                        focusId = hero?.id
                    )
                } else {
                    PosterStrip(
                        items = items,
                        empty = if (isSeries) copy.noSeries else copy.noMovies,
                        compact = false,
                        firstFocus = firstFocus ?: restoreFocus,
                        focusId = hero?.id,
                        onHover = onHover,
                        onOpen = onOpen,
                        onLeftFromFirst = onLeftFromFirst,
                        onExpandTo2Rows = { expandedRows = true },
                        onLoadMore = onLoadMore
                    )
                }
            } else if (!isSeries) {
                MovieActionBar(
                    copy = copy,
                    fav = fav,
                    downloaded = dl?.status == "DONE",
                    downloading = dl?.status == "RUN" || dl?.status == "WAIT",
                    playFocus = playFocus,
                    onPlay = { onPlay(pinned) },
                    onExternal = { onExternal(pinned) },
                    onFav = { onFav(pinned) },
                    onTrailer = onTrailer,
                    onDownload = { onDownload(pinned) }
                )
            } else {
                SeriesPinned(
                    copy = copy,
                    episodes = episodes,
                    season = season,
                    onSeason = onSeason,
                    playFocus = playFocus,
                    onPlay = onPlayEpisode,
                    onDownload = onDownloadEpisode,
                    onLeft = onClosePin,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
        if (pinned == null) {
            Row(
                Modifier.align(Alignment.TopEnd).padding(top = 12.dp, end = 18.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                CinemaIconChip(Icons.Filled.Sort, copy.sort, sort != VodSort.NAME, onOpenSort)
                CinemaIconChip(Icons.Filled.Search, copy.search, false, onSearch)
            }
        }
    }
}

@Composable
private fun MovieActionBar(
    copy: Copy,
    fav: Boolean,
    downloaded: Boolean,
    downloading: Boolean,
    playFocus: FocusRequester,
    onPlay: () -> Unit,
    onExternal: () -> Unit,
    onFav: () -> Unit,
    onTrailer: () -> Unit,
    onDownload: () -> Unit
) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(bottom = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        CinemaAction(
            copy.play,
            Icons.Filled.PlayArrow,
            primary = true,
            onClick = onPlay,
            blockLeft = true,
            blockUp = true,
            blockDown = true,
            focusRequester = playFocus,
            modifier = Modifier.weight(1.15f)
        )
        CinemaAction(
            copy.external,
            Icons.Filled.OpenInNew,
            onClick = onExternal,
            blockUp = true,
            blockDown = true,
            modifier = Modifier.weight(1.1f)
        )
        CinemaAction(
            if (fav) copy.favorites else copy.addFavorite,
            if (fav) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
            onClick = onFav,
            blockUp = true,
            blockDown = true,
            modifier = Modifier.weight(1.15f)
        )
        CinemaAction(
            copy.trailer,
            Icons.Filled.MovieFilter,
            onClick = onTrailer,
            blockUp = true,
            blockDown = true,
            modifier = Modifier.weight(1f)
        )
        CinemaAction(
            when {
                downloaded -> copy.downloaded
                downloading -> "…"
                else -> copy.download
            },
            if (downloaded) Icons.Filled.DownloadDone else Icons.Filled.Download,
            onClick = { if (!downloaded && !downloading) onDownload() },
            blockRight = true,
            blockUp = true,
            blockDown = true,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun CinemaAction(
    label: String,
    icon: ImageVector,
    onClick: () -> Unit,
    primary: Boolean = false,
    blockLeft: Boolean = false,
    blockRight: Boolean = false,
    blockUp: Boolean = false,
    blockDown: Boolean = false,
    onLeft: (() -> Unit)? = null,
    focusRequester: FocusRequester? = null,
    modifier: Modifier = Modifier
) {
    var focused by remember { mutableStateOf(false) }
    val g = LocalGlass.current
    GlassPanel(
        focused = focused,
        strong = true,
        accentFill = primary,
        radius = 6.dp,
        scaleOnFocus = true,
        onClick = onClick,
        modifier = modifier
            .rayFocusRequester(focusRequester)
            .onFocusChanged { focused = it.isFocused }
            .onPreviewKeyEvent { e ->
                if (e.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
                when (e.key) {
                    Key.DirectionLeft -> {
                        if (onLeft != null) { onLeft(); true }
                        else blockLeft
                    }
                    Key.DirectionRight -> blockRight
                    Key.DirectionUp -> blockUp
                    Key.DirectionDown -> blockDown
                    else -> false
                }
            }
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                icon,
                contentDescription = null,
                tint = if (primary) Color.White else if (focused) g.accent else Color.White.copy(alpha = 0.94f),
                modifier = Modifier.size(20.dp)
            )
            Spacer(Modifier.width(8.dp))
            Text(
                label,
                color = if (primary) Color.White else if (focused) g.accent else Color.White.copy(alpha = 0.94f),
                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold, fontSize = 13.sp),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun CinemaIconChip(icon: ImageVector, label: String, active: Boolean, onClick: () -> Unit) {
    var focused by remember { mutableStateOf(false) }
    val g = LocalGlass.current
    GlassPanel(
        focused = focused,
        strong = true,
        accentFill = active,
        radius = 6.dp,
        onClick = onClick,
        modifier = Modifier.size(44.dp).onFocusChanged { focused = it.isFocused }
    ) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Icon(icon, label, tint = if (active || focused) Color.White else g.text, modifier = Modifier.size(22.dp))
        }
    }
}

@Composable
private fun SeriesPinned(
    copy: Copy,
    episodes: List<EpisodeEntity>,
    season: Int,
    onSeason: (Int) -> Unit,
    playFocus: FocusRequester,
    onPlay: (EpisodeEntity) -> Unit,
    onDownload: (EpisodeEntity) -> Unit,
    onLeft: () -> Unit,
    modifier: Modifier = Modifier
) {
    val g = LocalGlass.current
    val seasons = remember(episodes) { episodes.map { it.season }.distinct().sorted() }
    val inSeason = episodes.filter { it.season == season }.sortedBy { it.episode }
    Column(modifier.fillMaxWidth()) {
        if (seasons.isEmpty()) return
        LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            itemsIndexed(seasons, key = { _, s -> s }) { index, s ->
                SeasonChip(
                    label = "${copy.season} $s",
                    selected = s == season,
                    onClick = { onSeason(s) },
                    onLeft = if (index == 0) onLeft else null
                )
            }
        }
        Spacer(Modifier.height(14.dp))
        Text(
            copy.episodes,
            color = g.text,
            style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold, fontSize = 13.sp)
        )
        Spacer(Modifier.height(8.dp))
        LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp), contentPadding = PaddingValues(bottom = 8.dp)) {
            itemsIndexed(inSeason, key = { _, ep -> ep.id }) { index, ep ->
                EpisodeTile(
                    copy = copy,
                    ep = ep,
                    onPlay = { onPlay(ep) },
                    onDownload = { onDownload(ep) },
                    focusRequester = if (index == 0) playFocus else null
                )
            }
        }
    }
}

@Composable
private fun SeasonChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    onLeft: (() -> Unit)?
) {
    var focused by remember { mutableStateOf(false) }
    val cream = Color(0xFFE8E4DC)
    GlassPanel(
        focused = focused,
        strong = true,
        accentFill = false,
        radius = 10.dp,
        scaleOnFocus = true,
        onClick = onClick,
        modifier = Modifier
            .onFocusChanged { focused = it.isFocused }
            .onPreviewKeyEvent { e ->
                if (e.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
                if (onLeft != null && e.key == Key.DirectionLeft) { onLeft(); true } else false
            }
    ) {
        Box(
            Modifier
                .background(if (selected) cream else Color.Transparent, RoundedCornerShape(10.dp))
                .padding(horizontal = 10.dp, vertical = 6.dp)
        ) {
            Text(
                label,
                color = if (selected) Color(0xFF222222) else Color.White.copy(alpha = 0.92f),
                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold, fontSize = 11.5.sp)
            )
        }
    }
}

@Composable
private fun EpisodeTile(
    copy: Copy,
    ep: EpisodeEntity,
    onPlay: () -> Unit,
    onDownload: () -> Unit,
    focusRequester: FocusRequester? = null
) {
    var focused by remember { mutableStateOf(false) }
    val g = LocalGlass.current
    GlassPanel(
        focused = focused,
        strong = true,
        radius = 12.dp,
        onClick = onPlay,
        onLongClick = onDownload,
        modifier = Modifier
            .width(168.dp)
            .height(94.dp)
            .rayFocusRequester(focusRequester)
            .onFocusChanged { focused = it.isFocused }
    ) {
        Column(Modifier.fillMaxSize().padding(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Filled.PlayCircleOutline,
                    contentDescription = null,
                    tint = if (focused) Color.White else Color.White.copy(alpha = 0.7f),
                    modifier = Modifier.size(18.dp)
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    "${copy.episodeLabel} ${ep.episode}",
                    color = Color.White,
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.ExtraBold, fontSize = 12.sp),
                    maxLines = 1,
                    modifier = Modifier.weight(1f)
                )
            }
            Spacer(Modifier.height(4.dp))
            Text(
                ep.name.ifBlank { "E${ep.episode}" },
                color = Color.White,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold, fontSize = 11.sp)
            )
            if (ep.plot.isNotBlank()) {
                Spacer(Modifier.height(3.dp))
                Text(
                    ep.plot,
                    color = Color.White.copy(alpha = 0.62f),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.labelLarge.copy(fontSize = 9.5.sp)
                )
            }
        }
    }
}

@Composable
private fun VodInfo(
    copy: Copy,
    item: VodEntity,
    extras: VodMeta?,
    pinned: Boolean,
    modifier: Modifier = Modifier,
    includeCast: Boolean = false,
    people: List<CastPerson> = emptyList(),
    episode: EpisodeEntity? = null,
    seasonCount: Int = 0,
    episodeCount: Int = 0,
    compact: Boolean = false
) {
    val g = LocalGlass.current
    val raw = if (pinned && episode != null) episode.name.ifBlank { item.name } else item.name
    val (title, subtitle) = splitTitle(raw)
    val overview = extras?.plot?.ifBlank { item.plot }.orEmpty().ifBlank { item.plot }
    val plot = when {
        item.kind.equals("SERIES", true) && pinned ->
            overview.ifBlank { episode?.plot.orEmpty() }
        extras?.plot?.isNotBlank() == true -> extras.plot
        else -> item.plot
    }
    val badges = badgesFor(item, extras, seasonCount, episodeCount)
    val plotH = (LocalConfiguration.current.screenHeightDp * 0.22f).dp.coerceIn(80.dp, 180.dp)
    Column(modifier) {
        if (compact) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        title,
                        color = g.text,
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 19.sp
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (badges.isNotEmpty()) {
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            badges.take(3).forEach { MetaChip(it) }
                        }
                    }
                }
                val body = plot.ifBlank { copy.noPlot }
                Text(
                    body,
                    color = Color.White.copy(alpha = 0.85f),
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontSize = 12.sp,
                        lineHeight = 16.sp
                    ),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        } else {
            Text(
                title,
                color = g.text,
                style = MaterialTheme.typography.headlineLarge.copy(
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = if (pinned) 30.sp else 25.sp,
                    letterSpacing = (-0.3).sp,
                    lineHeight = if (pinned) 32.sp else 28.sp
                ),
                maxLines = if (pinned) 3 else 2,
                overflow = TextOverflow.Ellipsis
            )
            if (subtitle != null) {
                Spacer(Modifier.height(4.dp))
                Text(
                    subtitle,
                    color = g.accent,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 2.4.sp,
                        fontSize = if (pinned) 14.sp else 13.sp
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            if (badges.isNotEmpty()) {
                Spacer(Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    badges.forEach { MetaChip(it) }
                }
            }
            val awards = extras?.awards?.takeIf { it.isNotBlank() && it.uppercase() != "N/A" }
            if (pinned && awards != null) {
                Spacer(Modifier.height(6.dp))
                Text(
                    "🏆 $awards",
                    color = g.accent.copy(alpha = 0.92f),
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                )
            }
            val body = plot.ifBlank { copy.noPlot }
            if (pinned) {
                Spacer(Modifier.height(12.dp))
                AutoScrollPlot(
                    text = body,
                    modifier = Modifier.height(plotH)
                )
                if (includeCast && people.isNotEmpty()) {
                    Spacer(Modifier.height(12.dp))
                    CastStrip(people)
                }
            } else {
                Spacer(Modifier.height(10.dp))
                Text(
                    body,
                    color = Color.White.copy(alpha = 0.90f),
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontSize = 12.5.sp,
                        fontWeight = FontWeight.Normal,
                        lineHeight = 17.5.sp
                    ),
                    maxLines = 4,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun VodSortMenu(
    copy: Copy,
    selected: VodSort,
    onSelect: (VodSort) -> Unit,
    onDismiss: () -> Unit
) {
    val g = LocalGlass.current
    val first = remember { FocusRequester() }
    LaunchedEffect(Unit) { runCatching { first.requestFocus() } }
    Box(
        Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.55f))
            .onPreviewKeyEvent { e ->
                if (e.type == KeyEventType.KeyDown && (e.key == Key.Back || e.key == Key.Escape)) {
                    onDismiss(); true
                } else false
            },
        contentAlignment = Alignment.Center
    ) {
        GlassPanel(strong = true, radius = 16.dp, modifier = Modifier.width(360.dp).padding(24.dp)) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    copy.sort,
                    color = g.text,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, fontSize = 17.sp)
                )
                Spacer(Modifier.height(6.dp))
                listOf(
                    Triple(VodSort.NAME, Icons.Filled.SortByAlpha, copy.sortAlpha),
                    Triple(VodSort.RATING, Icons.Filled.Star, copy.sortRating),
                    Triple(VodSort.RANDOM, Icons.Filled.Shuffle, copy.sortRandom),
                    Triple(VodSort.ADDED, Icons.Filled.DateRange, copy.sortAdded)
                ).forEachIndexed { i, (mode, icon, label) ->
                    CinemaAction(
                        label = label,
                        icon = icon,
                        primary = selected == mode,
                        onClick = { onSelect(mode) },
                        blockUp = i == 0,
                        blockDown = i == 3
                    )
                }
            }
        }
    }
}

@Composable
private fun AutoScrollPlot(text: String, modifier: Modifier = Modifier) {
    val scroll = rememberScrollState()
    LaunchedEffect(text) {
        scroll.scrollTo(0)
        delay(1200)
        while (isActive) {
            if (scroll.maxValue <= 0) {
                delay(800)
                continue
            }
            if (scroll.value >= scroll.maxValue) {
                delay(1400)
                scroll.animateScrollTo(0, tween(600, easing = LinearEasing))
                delay(1000)
            } else {
                val remaining = (scroll.maxValue - scroll.value).coerceAtLeast(1)
                val duration = (remaining * 24).coerceIn(3500, 16000)
                scroll.animateScrollTo(scroll.maxValue, tween(duration, easing = LinearEasing))
            }
        }
    }
    Box(modifier.clipToBounds().verticalScroll(scroll)) {
        Text(
            text,
            color = Color.White.copy(alpha = 0.92f),
            style = MaterialTheme.typography.bodyLarge.copy(
                fontSize = 12.5.sp,
                fontWeight = FontWeight.Medium,
                lineHeight = 18.sp
            )
        )
    }
}

@Composable
private fun CastAvatar(person: CastPerson, size: Dp) {
    Box(
        Modifier.size(size).clip(CircleShape).background(Color.White.copy(alpha = 0.1f)),
        contentAlignment = Alignment.Center
    ) {
        if (person.photo.isNotBlank()) {
            AsyncImage(person.photo, person.name, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
        } else {
            Icon(Icons.Filled.Person, null, tint = Color.White.copy(alpha = 0.38f), modifier = Modifier.size(size * 0.55f))
        }
    }
}

@Composable
private fun CastStrip(people: List<CastPerson>) {
    if (people.isEmpty()) return
    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        modifier = Modifier.fillMaxWidth().height(74.dp)
    ) {
        items(people, key = { it.name + it.character }) { person ->
            Box(
                Modifier
                    .width(220.dp)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color.Black.copy(alpha = 0.25f))
                    .border(0.85.dp, Color.White.copy(alpha = 0.15f), RoundedCornerShape(12.dp))
                    .padding(8.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    CastAvatar(person, 56.dp)
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(
                            person.name,
                            color = Color.White,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                        )
                        if (person.character.isNotBlank()) {
                            Spacer(Modifier.height(2.dp))
                            Text(
                                person.character,
                                color = Color.White.copy(alpha = 0.6f),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                style = MaterialTheme.typography.labelLarge.copy(fontSize = 11.sp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MetaChip(label: String) {
    val isRating = label.contains("★") || label.contains("IMDb", ignoreCase = true) || label.contains("/10")
    val isHighQuality = label.contains("4K", ignoreCase = true) || label.contains("HDR", ignoreCase = true) || label.contains("UHD", ignoreCase = true) || label.contains("DOLBY", ignoreCase = true)

    val backgroundBrush = when {
        isRating -> Brush.horizontalGradient(
            listOf(Color(0xFFF5C518).copy(alpha = 0.22f), Color(0xFFE5A00D).copy(alpha = 0.10f))
        )
        isHighQuality -> Brush.horizontalGradient(
            listOf(Color.White.copy(alpha = 0.16f), Color.White.copy(alpha = 0.08f))
        )
        else -> Brush.horizontalGradient(
            listOf(Color.White.copy(alpha = 0.08f), Color.White.copy(alpha = 0.04f))
        )
    }

    val borderColor = when {
        isRating -> Color(0xFFF5C518).copy(alpha = 0.60f)
        isHighQuality -> Color.White.copy(alpha = 0.38f)
        else -> Color.White.copy(alpha = 0.16f)
    }

    val textColor = when {
        isRating -> Color(0xFFF5C518)
        isHighQuality -> Color.White
        else -> Color.White.copy(alpha = 0.90f)
    }

    Box(
        Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(backgroundBrush)
            .border(0.75.dp, borderColor, RoundedCornerShape(6.dp))
            .padding(horizontal = 9.dp, vertical = 4.dp)
    ) {
        Text(
            label,
            color = textColor,
            style = MaterialTheme.typography.labelLarge.copy(
                fontWeight = if (isRating || isHighQuality) FontWeight.Bold else FontWeight.SemiBold,
                fontSize = 11.5.sp,
                letterSpacing = 0.2.sp
            )
        )
    }
}

@Composable
private fun PosterStrip(
    items: List<VodEntity>,
    empty: String,
    compact: Boolean,
    onHover: (VodEntity) -> Unit,
    onOpen: (VodEntity) -> Unit,
    onLeftFromFirst: () -> Unit,
    onExpandTo2Rows: (() -> Unit)? = null,
    onLoadMore: () -> Unit = {},
    firstFocus: FocusRequester? = null,
    focusId: String? = null
) {
    val g = LocalGlass.current
    val w = if (compact) 72.dp else 88.dp
    val focusIndex = remember(items, focusId) {
        if (focusId.isNullOrBlank()) 0 else items.indexOfFirst { it.id == focusId }.coerceAtLeast(0)
    }
    val state = rememberLazyListState(initialFirstVisibleItemIndex = focusIndex.coerceAtLeast(0))
    LaunchedEffect(focusIndex, items.size) {
        if (focusIndex in items.indices) {
            runCatching { state.scrollToItem(focusIndex) }
        }
    }
    if (items.isEmpty()) {
        Box(Modifier.height(if (compact) 115.dp else 145.dp).fillMaxWidth(), contentAlignment = Alignment.Center) {
            Text(empty, color = g.muted)
        }
        return
    }
    LaunchedEffect(items.size, onLoadMore) {
        snapshotFlow {
            val last = state.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
            items.isNotEmpty() && last >= items.lastIndex - 12
        }.distinctUntilChanged().collect { nearEnd ->
            if (nearEnd) onLoadMore()
        }
    }
    LazyRow(
        state = state,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        contentPadding = PaddingValues(start = 4.dp, top = 4.dp, bottom = 8.dp, end = 20.dp),
        modifier = Modifier
            .fillMaxWidth()
            .pointerInput(Unit) {
                detectVerticalDragGestures { _, dragAmount ->
                    if (dragAmount > 16f) onExpandTo2Rows?.invoke()
                }
            }
    ) {
        itemsIndexed(items, key = { _, v -> v.id }) { index, item ->
            val takeFocus = firstFocus != null && index == focusIndex
            PosterTile(
                item = item,
                width = w,
                onFocused = {
                    onHover(item)
                    if (index >= items.lastIndex - 12) {
                        onLoadMore()
                    }
                },
                onClick = { onOpen(item) },
                onLeft = if (index == 0) onLeftFromFirst else null,
                onDown = onExpandTo2Rows,
                onRightAtEnd = onLoadMore,
                focusRequester = if (takeFocus) firstFocus else null
            )
        }
    }
}

@Composable
private fun PosterStrip2Rows(
    items: List<VodEntity>,
    empty: String,
    onHover: (VodEntity) -> Unit,
    onOpen: (VodEntity) -> Unit,
    onLeftFromFirst: () -> Unit,
    onCollapseToSingleRow: () -> Unit,
    onLoadMore: () -> Unit = {},
    firstFocus: FocusRequester? = null,
    focusId: String? = null
) {
    val g = LocalGlass.current
    val focusIndex = remember(items, focusId) {
        if (focusId.isNullOrBlank()) 0 else items.indexOfFirst { it.id == focusId }.coerceAtLeast(0)
    }
    val gridState = rememberLazyGridState(initialFirstVisibleItemIndex = focusIndex.coerceAtLeast(0))
    LaunchedEffect(focusIndex, items.size) {
        if (focusIndex in items.indices) {
            runCatching { gridState.scrollToItem(focusIndex) }
        }
    }
    val w = 82.dp
    if (items.isEmpty()) {
        Box(Modifier.height(250.dp).fillMaxWidth(), contentAlignment = Alignment.Center) {
            Text(empty, color = g.muted)
        }
        return
    }
    LaunchedEffect(items.size, onLoadMore) {
        snapshotFlow {
            val last = gridState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
            items.isNotEmpty() && last >= items.lastIndex - 18
        }.distinctUntilChanged().collect { nearEnd ->
            if (nearEnd) onLoadMore()
        }
    }
    LazyHorizontalGrid(
        rows = GridCells.Fixed(2),
        state = gridState,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        contentPadding = PaddingValues(start = 4.dp, top = 4.dp, bottom = 8.dp, end = 20.dp),
        modifier = Modifier
            .fillMaxWidth()
            .height(290.dp)
            .pointerInput(Unit) {
                detectVerticalDragGestures { _, dragAmount ->
                    if (dragAmount < -16f) onCollapseToSingleRow()
                }
            }
    ) {
        gridItemsIndexed(items, key = { _, v -> v.id }) { index, item ->
            val takeFocus = firstFocus != null && index == focusIndex
            val isFirstCol = index < 2
            val isTopRow = index % 2 == 0
            PosterTile(
                item = item,
                width = w,
                onFocused = {
                    onHover(item)
                    if (index >= items.lastIndex - 18) {
                        onLoadMore()
                    }
                },
                onClick = { onOpen(item) },
                onLeft = if (isFirstCol) onLeftFromFirst else null,
                onUp = if (isTopRow) onCollapseToSingleRow else null,
                onRightAtEnd = onLoadMore,
                focusRequester = if (takeFocus) firstFocus else null
            )
        }
    }
}

@Composable
private fun PosterTile(
    item: VodEntity,
    width: Dp,
    onFocused: () -> Unit,
    onClick: () -> Unit,
    onLeft: (() -> Unit)? = null,
    onDown: (() -> Unit)? = null,
    onUp: (() -> Unit)? = null,
    onRightAtEnd: (() -> Unit)? = null,
    focusRequester: FocusRequester? = null
) {
    var focused by remember { mutableStateOf(false) }
    val g = LocalGlass.current
    val scale by animateFloatAsState(
        targetValue = if (focused) 1.12f else 1.0f,
        animationSpec = tween(140),
        label = "poster-scale"
    )

    val borderModifier = if (focused) {
        Modifier.border(
            width = 2.dp,
            brush = Brush.horizontalGradient(
                listOf(Color(0xFF00F0FF), Color(0xFF38BDF8), Color(0xFF67E8F9))
            ),
            shape = RoundedCornerShape(10.dp)
        )
    } else {
        Modifier.border(
            width = 0.8.dp,
            color = Color.White.copy(alpha = 0.12f),
            shape = RoundedCornerShape(10.dp)
        )
    }

    Column(
        modifier = Modifier
            .width(width)
            .scale(scale)
            .padding(vertical = 2.dp)
    ) {
        Box(
            Modifier
                .fillMaxWidth()
                .aspectRatio(2f / 3f)
                .clip(RoundedCornerShape(10.dp))
                .then(borderModifier)
                .background(g.panelStrong)
        ) {
            GlassPanel(
                focused = focused,
                radius = 10.dp,
                onClick = onClick,
                modifier = Modifier
                    .fillMaxSize()
                    .rayFocusRequester(focusRequester)
                    .onFocusChanged {
                        focused = it.isFocused
                        if (it.isFocused) onFocused()
                    }
                    .onPreviewKeyEvent { e ->
                        if (e.type == KeyEventType.KeyDown) {
                            when {
                                onLeft != null && e.key == Key.DirectionLeft -> { onLeft(); true }
                                onDown != null && e.key == Key.DirectionDown -> { onDown(); true }
                                onUp != null && e.key == Key.DirectionUp -> { onUp(); true }
                                e.key == Key.DirectionRight -> {
                                    onRightAtEnd?.invoke()
                                    false
                                }
                                e.key == Key.DirectionCenter || e.key == Key.Enter || e.key == Key.NumPadEnter -> {
                                    onClick()
                                    true
                                }
                                else -> false
                            }
                        } else if (e.type == KeyEventType.KeyUp && (e.key == Key.DirectionCenter || e.key == Key.Enter || e.key == Key.NumPadEnter)) {
                            true
                        } else false
                    }
            ) {
                if (item.poster.isNotBlank()) {
                    AsyncImage(
                        item.poster,
                        item.name,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Box(Modifier.fillMaxSize().background(g.panelStrong), contentAlignment = Alignment.Center) {
                        Text(
                            item.name.take(1),
                            color = g.muted,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
        Spacer(Modifier.height(4.dp))
        Text(
            text = item.name,
            color = if (focused) Color(0xFF00F0FF) else Color.White.copy(alpha = 0.85f),
            style = MaterialTheme.typography.bodySmall.copy(
                fontSize = 11.sp,
                fontWeight = if (focused) FontWeight.Bold else FontWeight.Medium
            ),
            maxLines = 2,
            minLines = 2,
            lineHeight = 13.5.sp,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 2.dp)
        )
    }
}

@Composable
private fun CinemaPosterThumb(url: String, width: Dp) {
    val g = LocalGlass.current
    Box(
        Modifier
            .width(width)
            .aspectRatio(2f / 3f)
            .clip(RoundedCornerShape(14.dp))
            .border(1.2.dp, Color.White.copy(alpha = 0.18f), RoundedCornerShape(14.dp))
            .clipToBounds()
            .background(g.panelStrong)
    ) {
        if (url.isNotBlank()) {
            AsyncImage(url, null, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
        }
    }
}

private fun castPeople(extras: VodMeta?): List<CastPerson> {
    if (extras == null) return emptyList()
    if (extras.people.isNotEmpty()) return extras.people.take(10)
    return extras.cast.split(',', '|', ';').map { it.trim() }.filter { it.isNotBlank() }.take(10).map { CastPerson(it) }
}

private fun splitTitle(raw: String): Pair<String, String?> {
    val title = raw.trim()
    val i = title.indexOf(':')
    if (i <= 0 || i >= title.length - 1) return title to null
    val left = title.substring(0, i).trim()
    val right = title.substring(i + 1).trim()
    if (left.length < 2 || right.length < 2 || right.first().isDigit()) return title to null
    return left to right.uppercase()
}

private fun badgesFor(item: VodEntity, extras: VodMeta?, seasonCount: Int, episodeCount: Int): List<String> {
    val out = mutableListOf<String>()

    val rating = extras?.rating?.ifBlank { item.rating } ?: item.rating
    if (rating.isNotBlank() && rating.uppercase() != "N/A") {
        val num = rating.toDoubleOrNull()
        if (num != null && num > 0.0) {
            out += "★ %.1f".format(num)
        } else if (rating.isNotBlank()) {
            out += "★ $rating"
        }
    }

    val year = extras?.year?.ifBlank { item.year } ?: item.year
    if (year.isNotBlank() && year != "0") out += year

    extras?.certification?.takeIf { it.isNotBlank() && it.uppercase() != "N/A" }?.let { out += it }

    val genres = (extras?.genre?.ifBlank { item.genre } ?: item.genre)
        .split(',', '/', '|')
        .map { it.trim() }
        .filter { it.isNotBlank() && it.uppercase() != "N/A" }
        .distinct()
        .take(3)
    out.addAll(genres)

    extras?.runtime?.takeIf { it.isNotBlank() && it.uppercase() != "N/A" && it != "0 min" }?.let { out += it }

    if (seasonCount > 0) out += if (episodeCount > 0) "S$seasonCount · E$episodeCount" else "S$seasonCount"

    val hay = "${item.name} ${item.streamUrl} ${item.extension}".uppercase()
    when {
        hay.contains("2160") || hay.contains("UHD") || Regex("\\b4K\\b").containsMatchIn(hay) -> out += "4K"
        hay.contains("1080") || hay.contains("FHD") -> out += "1080p"
        hay.contains("720") -> out += "720p"
    }
    when {
        hay.contains("ATMOS") -> out += "Atmos"
        hay.contains("DOLBY") -> out += "Dolby"
        hay.contains("DTS") -> out += "DTS"
    }

    extras?.country?.takeIf { it.isNotBlank() && it.uppercase() != "N/A" }?.let {
        val c = it.split(',').firstOrNull()?.trim().orEmpty()
        if (c.isNotBlank() && out.size < 8) out += c
    }

    extras?.director?.takeIf { it.isNotBlank() && it.uppercase() != "N/A" }?.let {
        if (out.size < 8) out += "🎬 $it"
    }

    return out.distinct().take(8)
}
