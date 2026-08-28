package com.ray.iptv.ui.screens.catalog

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.PlayCircleOutline
import androidx.compose.material.icons.filled.Search
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import kotlinx.coroutines.delay
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Icon
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import coil.compose.AsyncImage
import com.ray.iptv.data.local.CategoryEntity
import com.ray.iptv.data.local.ChannelEntity
import com.ray.iptv.data.local.DownloadEntity
import com.ray.iptv.data.local.EpisodeEntity
import com.ray.iptv.data.local.FavoriteEntity
import com.ray.iptv.data.local.VodEntity
import com.ray.iptv.data.meta.VodMeta
import com.ray.iptv.ui.components.GlassButton
import com.ray.iptv.ui.components.GlassChip
import com.ray.iptv.ui.components.PosterCard
import com.ray.iptv.ui.glass.GlassPanel
import com.ray.iptv.ui.i18n.Copy
import com.ray.iptv.ui.input.rayFocusRequester
import com.ray.iptv.ui.input.tryFocus
import com.ray.iptv.ui.screens.home.Shelf
import com.ray.iptv.ui.theme.LocalGlass

@Composable
fun VodBrowser(
    title: String,
    categories: List<CategoryEntity>,
    items: List<VodEntity>,
    allItems: List<VodEntity> = items,
    selected: String,
    onCategory: (String) -> Unit,
    onOpen: (VodEntity) -> Unit,
    onPlay: (VodEntity) -> Unit = onOpen
) {
    val g = LocalGlass.current
    val counts = remember(allItems) { allItems.groupingBy { it.categoryId }.eachCount() }
    var focused by remember(items) { mutableStateOf(items.firstOrNull()) }
    LaunchedEffect(items) { if (focused == null || items.none { it.id == focused?.id }) focused = items.firstOrNull() }
    val hero = focused
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val landscape = maxWidth >= 720.dp
        if (landscape) {
            Row(Modifier.fillMaxSize(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                GlassPanel(strong = true, radius = 22.dp, modifier = Modifier.weight(0.31f).fillMaxHeight()) {
                    Column(Modifier.padding(10.dp)) {
                        Text(title, color = g.muted, style = MaterialTheme.typography.labelLarge, modifier = Modifier.padding(8.dp))
                        Spacer(Modifier.height(8.dp))
                        LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            item {
                                VodCatRow(copyAll(title), allItems.size, selected.isEmpty()) { onCategory("") }
                            }
                            items(categories, key = { it.id }) { c ->
                                VodCatRow(c.name, counts[c.id] ?: 0, selected == c.id) { onCategory(c.id) }
                            }
                        }
                    }
                }
                Box(Modifier.weight(0.69f).fillMaxHeight().clip(RoundedCornerShape(22.dp))) {
                    val art = hero?.poster.orEmpty()
                    if (art.isNotBlank()) {
                        AsyncImage(art, hero?.name, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                    } else {
                        Box(Modifier.fillMaxSize().background(g.wallpaperDark))
                    }
                    Box(
                        Modifier.fillMaxSize().background(
                            Brush.horizontalGradient(
                                listOf(Color.Black.copy(alpha = 0.88f), Color.Black.copy(alpha = 0.45f), Color.Transparent)
                            )
                        )
                    )
                    Box(
                        Modifier.fillMaxSize().background(
                            Brush.verticalGradient(
                                listOf(Color.Transparent, Color.Black.copy(alpha = 0.82f))
                            )
                        )
                    )
                    Column(Modifier.fillMaxSize().padding(22.dp)) {
                        if (hero != null) {
                            Text(
                                hero.name,
                                color = g.text,
                                style = MaterialTheme.typography.displayLarge.copy(fontWeight = FontWeight.ExtraBold),
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                            Spacer(Modifier.height(8.dp))
                            Text(
                                listOf(hero.year, hero.rating, hero.genre).filter { it.isNotBlank() }.joinToString("  ·  "),
                                color = g.muted
                            )
                            Spacer(Modifier.height(12.dp))
                            Text(hero.plot, color = g.text.copy(alpha = 0.86f), style = MaterialTheme.typography.bodyLarge, maxLines = 4, overflow = TextOverflow.Ellipsis)
                            Spacer(Modifier.height(16.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                GlassButton(if (title.contains("Dizi", true) || title.contains("Series", true)) title else "Oynat") { onPlay(hero) }
                                GlassButton("Detay") { onOpen(hero) }
                            }
                        }
                        Spacer(Modifier.weight(1f))
                        Text(title, color = g.muted, style = MaterialTheme.typography.labelLarge)
                        Spacer(Modifier.height(10.dp))
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                            items(items, key = { it.id }) { v ->
                                var f by remember { mutableStateOf(false) }
                                PosterCard(
                                    v.name,
                                    v.poster,
                                    v.year,
                                    width = 118.dp,
                                    onClick = {
                                        if (focused?.id == v.id) onOpen(v) else focused = v
                                    }
                                )
                            }
                        }
                    }
                }
            }
        } else {
            Column(Modifier.fillMaxSize()) {
                if (hero != null) {
                    Box(
                        Modifier.fillMaxWidth().aspectRatio(16f / 9f).clip(RoundedCornerShape(18.dp))
                    ) {
                        if (hero.poster.isNotBlank()) {
                            AsyncImage(hero.poster, hero.name, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                        }
                        Box(
                            Modifier.fillMaxSize().background(
                                Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.78f)))
                            )
                        )
                        Column(Modifier.align(Alignment.BottomStart).padding(14.dp)) {
                            Text(hero.name, color = Color.White, style = MaterialTheme.typography.headlineMedium, maxLines = 2)
                            Spacer(Modifier.height(8.dp))
                            GlassButton("Oynat") { onPlay(hero) }
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                }
                LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    item { GlassChip(copyAll(title), selected.isEmpty()) { onCategory("") } }
                    items(categories, key = { it.id }) { c ->
                        GlassChip("${c.name}  ${counts[c.id] ?: 0}", selected == c.id) { onCategory(c.id) }
                    }
                }
                Spacer(Modifier.height(16.dp))
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(150.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(bottom = 40.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(items, key = { it.id }) { v ->
                        PosterCard(v.name, v.poster, v.year, onClick = { focused = v; onOpen(v) })
                    }
                }
            }
        }
    }
}

@Composable
private fun VodCatRow(label: String, count: Int, selected: Boolean, onClick: () -> Unit) {
    var focused by remember { mutableStateOf(false) }
    val g = LocalGlass.current
    GlassPanel(
        focused = focused || selected,
        strong = selected,
        accentFill = selected,
        radius = 12.dp,
        onClick = onClick,
        modifier = Modifier.fillMaxWidth().onFocusChanged { focused = it.isFocused }
    ) {
        Row(Modifier.padding(horizontal = 12.dp, vertical = 9.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(label, color = g.text, style = MaterialTheme.typography.bodyLarge, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
            Text(count.toString(), color = g.muted, style = MaterialTheme.typography.labelLarge)
        }
    }
}

private fun copyAll(title: String) = if (title.contains("Series", true) || title.contains("Dizi", true)) "Tümü" else "Tümü"

@Composable
fun MovieDetail(
    item: VodEntity,
    copy: com.ray.iptv.ui.i18n.Copy,
    extras: VodMeta = VodMeta("", "", "", "", ""),
    siblings: List<VodEntity> = emptyList(),
    onPlay: () -> Unit,
    onFav: () -> Unit,
    onDownload: () -> Unit,
    onTrailer: () -> Unit = {},
    onOpen: (VodEntity) -> Unit = {},
    onBack: () -> Unit
) {
    val g = LocalGlass.current
    val art = extras.backdrop.ifBlank { item.poster }
    Box(Modifier.fillMaxSize().clip(RoundedCornerShape(20.dp))) {
        if (art.isNotBlank()) {
            AsyncImage(art, item.name, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
        } else {
            Box(Modifier.fillMaxSize().background(g.wallpaperDark))
        }
        Box(
            Modifier.fillMaxSize().background(
                Brush.horizontalGradient(listOf(Color.Black.copy(alpha = 0.88f), Color.Black.copy(alpha = 0.4f), Color.Transparent))
            )
        )
        Box(
            Modifier.fillMaxSize().background(
                Brush.verticalGradient(listOf(Color.Black.copy(alpha = 0.2f), Color.Black.copy(alpha = 0.82f)))
            )
        )
        Column(Modifier.fillMaxSize().padding(22.dp)) {
            Text(item.name, style = MaterialTheme.typography.displayLarge.copy(fontWeight = FontWeight.ExtraBold), color = g.text, maxLines = 2)
            Spacer(Modifier.height(8.dp))
            Text(
                listOf(item.year, extras.runtime, item.rating, item.genre).filter { it.isNotBlank() }.joinToString("   ·   "),
                color = g.muted
            )
            Spacer(Modifier.height(14.dp))
            Text(
                listOf(extras.plot, item.plot).maxByOrNull { it.length }.orEmpty()
                    .ifBlank { "No synopsis from this source." },
                color = g.text,
                style = MaterialTheme.typography.bodyLarge,
                maxLines = 8,
                overflow = TextOverflow.Ellipsis
            )
            if (extras.cast.isNotBlank()) {
                Spacer(Modifier.height(10.dp))
                Text(extras.cast, color = g.muted, style = MaterialTheme.typography.bodyLarge, maxLines = 2)
            }
            Spacer(Modifier.height(18.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                GlassButton(copy.play) { onPlay() }
                GlassButton(copy.favorites) { onFav() }
                GlassButton(copy.downloads) { onDownload() }
                if (extras.trailerUrl.isNotBlank()) GlassButton(if (copy.play == "Oynat") "Fragman" else "Trailer") { onTrailer() }
                GlassButton(copy.back) { onBack() }
            }
            Spacer(Modifier.weight(1f))
            if (siblings.isNotEmpty()) {
                Text(copy.movies, color = g.muted, style = MaterialTheme.typography.labelLarge)
                Spacer(Modifier.height(10.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    items(siblings, key = { it.id }) { v ->
                        PosterCard(v.name, v.poster, v.year, width = 110.dp, onClick = { onOpen(v) })
                    }
                }
            }
        }
    }
}

@Composable
fun SeriesDetail(
    item: VodEntity,
    episodes: List<EpisodeEntity>,
    copy: com.ray.iptv.ui.i18n.Copy,
    extras: VodMeta = VodMeta("", "", "", "", ""),
    siblings: List<VodEntity> = emptyList(),
    onPlay: (EpisodeEntity) -> Unit,
    onFav: () -> Unit,
    onDownload: (EpisodeEntity) -> Unit = {},
    onTrailer: () -> Unit = {},
    onOpen: (VodEntity) -> Unit = {},
    onBack: () -> Unit
) {
    val g = LocalGlass.current
    val art = extras.backdrop.ifBlank { item.poster }
    Column(Modifier.fillMaxSize()) {
        Box(Modifier.fillMaxWidth().heightIn(min = 180.dp, max = 260.dp).clip(RoundedCornerShape(20.dp))) {
            if (art.isNotBlank()) {
                AsyncImage(art, item.name, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
            }
            Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color.Black.copy(alpha = 0.15f), Color.Black.copy(alpha = 0.88f)))))
            Column(Modifier.align(Alignment.BottomStart).padding(18.dp)) {
                Text(item.name, style = MaterialTheme.typography.displayLarge.copy(fontWeight = FontWeight.ExtraBold), color = Color.White, maxLines = 2)
                if (extras.cast.isNotBlank()) Text(extras.cast, color = Color.White.copy(alpha = 0.7f), maxLines = 1)
                Spacer(Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    GlassButton(copy.favorites) { onFav() }
                    if (extras.trailerUrl.isNotBlank()) GlassButton("Fragman") { onTrailer() }
                    GlassButton(copy.back) { onBack() }
                }
            }
        }
        Spacer(Modifier.height(14.dp))
        Text(copy.episodes, style = MaterialTheme.typography.headlineMedium, color = g.text)
        Spacer(Modifier.height(10.dp))
        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.weight(1f)) {
            items(episodes, key = { it.id }) { ep ->
                var focused by remember { mutableStateOf(false) }
                GlassPanel(
                    focused = focused,
                    radius = 14.dp,
                    onClick = { onPlay(ep) },
                    modifier = Modifier.fillMaxWidth().onFocusChanged { focused = it.isFocused }
                ) {
                    Row(
                        Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            ep.episode.toString().padStart(3, '0'),
                            color = g.accent,
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            modifier = Modifier.width(40.dp)
                        )
                        if (ep.still.isNotBlank()) {
                            AsyncImage(
                                ep.still,
                                ep.name,
                                modifier = Modifier.size(width = 72.dp, height = 44.dp).clip(RoundedCornerShape(8.dp)),
                                contentScale = ContentScale.Crop
                            )
                        }
                        Column(Modifier.weight(1f)) {
                            Text("S${ep.season}E${ep.episode}  ${ep.name}", color = g.text, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            if (ep.plot.isNotBlank()) {
                                Text(ep.plot, color = g.muted, style = MaterialTheme.typography.bodyMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                        }
                        GlassButton("↓") { onDownload(ep) }
                    }
                }
            }
        }
        if (siblings.isNotEmpty()) {
            Spacer(Modifier.height(10.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                items(siblings, key = { it.id }) { v ->
                    PosterCard(v.name, v.poster, v.year, width = 100.dp, onClick = { onOpen(v) })
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SearchScreen(
    copy: Copy,
    query: String,
    live: List<ChannelEntity>,
    movies: List<VodEntity>,
    series: List<VodEntity>,
    onQuery: (String) -> Unit,
    onLive: (ChannelEntity) -> Unit,
    onMovie: (VodEntity) -> Unit,
    onSeries: (VodEntity) -> Unit,
    recents: List<String> = emptyList(),
    searching: Boolean = false,
    liveCats: List<CategoryEntity> = emptyList(),
    movieCats: List<CategoryEntity> = emptyList(),
    seriesCats: List<CategoryEntity> = emptyList(),
    onRecent: (String) -> Unit = {},
    onRemoveRecent: (String) -> Unit = {},
    onClearRecents: () -> Unit = {}
) {
    val g = LocalGlass.current
    val inputFocus = remember { FocusRequester() }
    val firstHit = remember { FocusRequester() }
    val recentsFocus = remember { FocusRequester() }
    var local by remember { mutableStateOf(query) }
    var inputFocused by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        delay(80)
        inputFocus.tryFocus()
        delay(200)
        inputFocus.tryFocus()
        delay(350)
        inputFocus.tryFocus()
    }
    val empty = local.isBlank()
    val total = live.size + movies.size + series.size
    Column(Modifier.fillMaxSize().padding(4.dp)) {
        GlassPanel(
            radius = 12.dp,
            focused = inputFocused,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Filled.Search, null, tint = if (inputFocused) g.accent else Color.White.copy(alpha = 0.7f), modifier = Modifier.size(22.dp))
                Spacer(Modifier.width(10.dp))
                Box(Modifier.weight(1f)) {
                    if (local.isEmpty()) {
                        Text(
                            copy.search,
                            color = g.muted.copy(alpha = 0.6f),
                            style = MaterialTheme.typography.headlineMedium
                        )
                    }
                    BasicTextField(
                        value = local,
                        onValueChange = {
                            local = it
                            onQuery(it)
                        },
                        textStyle = MaterialTheme.typography.headlineMedium.copy(color = g.text),
                        cursorBrush = SolidColor(g.accent),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                        keyboardActions = KeyboardActions(onSearch = {
                            if (total > 0) firstHit.tryFocus()
                            else if (recents.isNotEmpty()) recentsFocus.tryFocus()
                        }),
                        modifier = Modifier
                            .fillMaxWidth()
                            .focusRequester(inputFocus)
                            .onFocusChanged { inputFocused = it.isFocused }
                            .onPreviewKeyEvent { e ->
                                if (e.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
                                if (e.key == Key.DirectionDown) {
                                    when {
                                        total > 0 -> firstHit.tryFocus()
                                        recents.isNotEmpty() -> recentsFocus.tryFocus()
                                        else -> return@onPreviewKeyEvent false
                                    }
                                    true
                                } else false
                            }
                    )
                }
            }
        }
        when {
            searching -> Box(Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                Text("…", color = g.muted, style = MaterialTheme.typography.headlineMedium)
            }
            empty && recents.isNotEmpty() -> {
                Spacer(Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.History, null, tint = Color.White.copy(alpha = 0.75f), modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(
                        copy.searchRecent,
                        color = Color.White.copy(alpha = 0.75f),
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold, fontSize = 12.sp),
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        copy.searchClear,
                        color = Color.White.copy(alpha = 0.65f),
                        style = MaterialTheme.typography.labelLarge.copy(fontSize = 11.sp),
                        modifier = Modifier.clickable { onClearRecents() }.padding(horizontal = 6.dp, vertical = 3.dp)
                    )
                }
                Spacer(Modifier.height(6.dp))
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    recents.forEachIndexed { i, q ->
                        RecentSearchChip(
                            label = q,
                            onTap = {
                                local = q
                                onRecent(q)
                            },
                            onRemove = { onRemoveRecent(q) },
                            focusRequester = if (i == 0) recentsFocus else null,
                            onUpToInput = if (i == 0) ({ inputFocus.tryFocus() }) else null
                        )
                    }
                }
            }
            !empty -> LazyColumn(
                modifier = Modifier.weight(1f).padding(top = 8.dp),
                contentPadding = PaddingValues(bottom = 12.dp)
            ) {
                if (live.isNotEmpty()) {
                    item { SearchSectionHeader(copy.live) }
                    itemsIndexed(live, key = { _, c -> "l-${c.id}" }) { i, ch ->
                        SearchHitRow(
                            title = ch.name,
                            subtitle = ch.categoryName.ifBlank { liveCats.firstOrNull { it.id == ch.categoryId }?.name.orEmpty() },
                            image = ch.logo,
                            focusRequester = if (i == 0) firstHit else null,
                            onUpToInput = if (i == 0) ({ inputFocus.tryFocus() }) else null,
                            onClick = { onLive(ch) }
                        )
                    }
                }
                if (movies.isNotEmpty()) {
                    item { SearchSectionHeader(copy.movies) }
                    itemsIndexed(movies, key = { _, v -> "m-${v.id}" }) { i, v ->
                        val first = live.isEmpty() && i == 0
                        SearchHitRow(
                            title = v.name,
                            subtitle = movieCats.firstOrNull { it.id == v.categoryId }?.name.orEmpty(),
                            image = v.poster,
                            focusRequester = if (first) firstHit else null,
                            onUpToInput = if (first) ({ inputFocus.tryFocus() }) else null,
                            onClick = { onMovie(v) }
                        )
                    }
                }
                if (series.isNotEmpty()) {
                    item { SearchSectionHeader(copy.series) }
                    itemsIndexed(series, key = { _, v -> "s-${v.id}" }) { i, v ->
                        val first = live.isEmpty() && movies.isEmpty() && i == 0
                        SearchHitRow(
                            title = v.name,
                            subtitle = seriesCats.firstOrNull { it.id == v.categoryId }?.name.orEmpty(),
                            image = v.poster,
                            focusRequester = if (first) firstHit else null,
                            onUpToInput = if (first) ({ inputFocus.tryFocus() }) else null,
                            onClick = { onSeries(v) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SearchSectionHeader(title: String) {
    val g = LocalGlass.current
    Text(
        title.uppercase(),
        color = g.accent.copy(alpha = 0.9f),
        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.ExtraBold, fontSize = 12.sp, letterSpacing = 1.2.sp),
        modifier = Modifier.padding(start = 12.dp, top = 16.dp, end = 12.dp, bottom = 8.dp)
    )
}

@Composable
private fun SearchHitRow(
    title: String,
    subtitle: String,
    image: String,
    focusRequester: FocusRequester?,
    onUpToInput: (() -> Unit)?,
    onClick: () -> Unit
) {
    var focused by remember { mutableStateOf(false) }
    val g = LocalGlass.current
    GlassPanel(
        focused = focused,
        radius = 12.dp,
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 4.dp)
            .then(if (focusRequester != null) Modifier.focusRequester(focusRequester) else Modifier)
            .onFocusChanged { focused = it.isFocused }
            .onPreviewKeyEvent { e ->
                if (e.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
                when {
                    onUpToInput != null && e.key == Key.DirectionUp -> {
                        onUpToInput(); true
                    }
                    e.key == Key.DirectionCenter || e.key == Key.Enter || e.key == Key.NumPadEnter -> {
                        onClick(); true
                    }
                    else -> false
                }
            }
    ) {
        Row(
            Modifier.fillMaxWidth().padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color.White.copy(alpha = 0.08f)),
                contentAlignment = Alignment.Center
            ) {
                if (image.isNotBlank()) {
                    AsyncImage(image, title, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                } else {
                    Icon(Icons.Filled.PlayCircleOutline, null, tint = Color.White.copy(alpha = 0.38f), modifier = Modifier.size(22.dp))
                }
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    title,
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.bodyLarge.copy(fontWeight = if (focused) FontWeight.Bold else FontWeight.Medium)
                )
                if (subtitle.isNotBlank()) {
                    Spacer(Modifier.height(2.dp))
                    Text(subtitle, color = Color.White.copy(alpha = 0.7f), maxLines = 1, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.labelLarge)
                }
            }
            Icon(Icons.Filled.KeyboardArrowRight, null, tint = Color.White.copy(alpha = 0.38f))
        }
    }
}

@Composable
private fun RecentSearchChip(
    label: String,
    onTap: () -> Unit,
    onRemove: () -> Unit,
    focusRequester: FocusRequester? = null,
    onUpToInput: (() -> Unit)? = null
) {
    var focused by remember { mutableStateOf(false) }
    GlassPanel(
        focused = focused,
        radius = 20.dp,
        onClick = onTap,
        modifier = Modifier
            .rayFocusRequester(focusRequester)
            .onFocusChanged { focused = it.isFocused }
            .onPreviewKeyEvent { e ->
                if (e.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
                when {
                    onUpToInput != null && e.key == Key.DirectionUp -> {
                        onUpToInput(); true
                    }
                    e.key == Key.DirectionCenter || e.key == Key.Enter || e.key == Key.NumPadEnter -> {
                        onTap(); true
                    }
                    else -> false
                }
            }
    ) {
        Row(
            Modifier.padding(start = 12.dp, top = 6.dp, end = 4.dp, bottom = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                label,
                color = Color.White,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Medium, fontSize = 13.sp),
                modifier = Modifier.widthIn(max = 180.dp)
            )
            Spacer(Modifier.width(4.dp))
            Box(
                Modifier
                    .size(22.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .focusProperties { canFocus = false }
                    .clickable { onRemove() },
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Filled.Close, null, tint = Color.White.copy(alpha = 0.7f), modifier = Modifier.size(14.dp))
            }
        }
    }
}

@Composable
fun FavoritesScreen(favs: List<FavoriteEntity>, resolveTitle: (FavoriteEntity) -> Pair<String, String>, onOpen: (FavoriteEntity) -> Unit) {
    val g = LocalGlass.current
    Column {
        Text("Favorites", style = MaterialTheme.typography.headlineLarge, color = g.text)
        Spacer(Modifier.height(16.dp))
        LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            items(favs, key = { it.mediaId }) { f ->
                val (t, p) = resolveTitle(f)
                PosterCard(t, p, f.kind.lowercase(), onClick = { onOpen(f) })
            }
        }
        if (favs.isEmpty()) Text("Long-press Play later — or mark titles from details.", color = g.muted)
    }
}

@Composable
fun DownloadsScreen(items: List<DownloadEntity>) {
    val g = LocalGlass.current
    Column {
        Text("Downloads", style = MaterialTheme.typography.headlineLarge, color = g.text)
        Spacer(Modifier.height(12.dp))
        if (items.isEmpty()) {
            Text("Offline files will land here. Queue a movie from its detail page once a source is connected.", color = g.muted)
        } else {
            items.forEach { d ->
                Text("${d.title}  ·  ${d.status}", color = g.text, modifier = Modifier.padding(vertical = 8.dp))
            }
        }
    }
}
