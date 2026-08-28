package com.ray.iptv.ui.mobile

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.LiveTv
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Tv
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Icon
import androidx.tv.material3.Text
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.ray.iptv.data.local.ChannelEntity
import com.ray.iptv.data.local.VodEntity
import com.ray.iptv.ui.i18n.Copy
import com.ray.iptv.ui.input.rayClickable
import kotlinx.coroutines.delay

@Composable
fun MobileDynamicSearchSheet(
    query: String,
    searching: Boolean,
    liveResults: List<ChannelEntity>,
    movieResults: List<VodEntity>,
    seriesResults: List<VodEntity>,
    searchHistory: List<String>,
    tr: Boolean,
    onQueryChange: (String) -> Unit,
    onPlayLive: (ChannelEntity) -> Unit,
    onOpenMovie: (VodEntity) -> Unit,
    onOpenSeries: (VodEntity) -> Unit,
    onRecentClick: (String) -> Unit,
    onRemoveRecent: (String) -> Unit,
    onClearHistory: () -> Unit,
    onDismiss: () -> Unit
) {
    val cyan = Color(0xFF22D3EE)
    val emerald = Color(0xFF34D399)
    val focusRequester = remember { FocusRequester() }
    val focusManager = LocalFocusManager.current
    var selectedFilter by remember { mutableStateOf("ALL") } // ALL, LIVE, MOVIES, SERIES

    BackHandler(onBack = onDismiss)

    LaunchedEffect(Unit) {
        delay(120)
        focusRequester.requestFocus()
    }

    Box(
        Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.60f))
            .clickable(
                indication = null,
                interactionSource = remember { MutableInteractionSource() }
            ) { onDismiss() },
        contentAlignment = Alignment.TopCenter
    ) {
        // Modern Floating Glass Spotlight Card
        Box(
            Modifier
                .statusBarsPadding()
                .padding(top = 16.dp, start = 14.dp, end = 14.dp, bottom = 24.dp)
                .widthIn(max = 600.dp)
                .fillMaxWidth()
                .wrapContentHeight()
                .heightIn(min = 160.dp, max = 540.dp)
                .clickable(
                    indication = null,
                    interactionSource = remember { MutableInteractionSource() }
                ) { /* Consume clicks to prevent dismissing */ }
                .clip(RoundedCornerShape(22.dp))
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFF0F1B18).copy(alpha = 0.94f),
                            Color(0xFF08120F).copy(alpha = 0.96f)
                        )
                    )
                )
                .border(
                    width = 1.2.dp,
                    brush = Brush.linearGradient(
                        listOf(
                            Color.White.copy(alpha = 0.28f),
                            cyan.copy(alpha = 0.55f),
                            Color.White.copy(alpha = 0.12f)
                        )
                    ),
                    shape = RoundedCornerShape(22.dp)
                )
                .navigationBarsPadding()
                .imePadding()
        ) {
            Column(
                Modifier
                    .fillMaxWidth()
                    .wrapContentHeight()
            ) {
                // Drag Handle
                Box(
                    Modifier
                        .align(Alignment.CenterHorizontally)
                        .padding(top = 10.dp, bottom = 4.dp)
                        .width(36.dp)
                        .height(4.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.22f))
                )

                // Search Input Header Bar
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        Modifier
                            .weight(1f)
                            .height(46.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(Color.White.copy(alpha = 0.07f))
                            .border(
                                1.dp,
                                Brush.horizontalGradient(
                                    listOf(cyan.copy(alpha = 0.65f), emerald.copy(alpha = 0.35f))
                                ),
                                RoundedCornerShape(14.dp)
                            )
                            .padding(horizontal = 12.dp),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        Row(
                            Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Filled.Search,
                                contentDescription = null,
                                tint = cyan,
                                modifier = Modifier.size(19.dp)
                            )
                            Spacer(Modifier.width(10.dp))
                            Box(Modifier.weight(1f)) {
                                if (query.isEmpty()) {
                                    Text(
                                        text = if (tr) "Film, dizi veya kanal ara..." else "Search movies, series or channels...",
                                        color = Color.White.copy(alpha = 0.45f),
                                        fontSize = 13.5.sp
                                    )
                                }
                                BasicTextField(
                                    value = query,
                                    onValueChange = onQueryChange,
                                    singleLine = true,
                                    textStyle = TextStyle(
                                        color = Color.White,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.SemiBold
                                    ),
                                    cursorBrush = SolidColor(cyan),
                                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                                    keyboardActions = KeyboardActions(onSearch = { focusManager.clearFocus() }),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .focusRequester(focusRequester)
                                )
                            }
                            if (query.isNotEmpty()) {
                                Icon(
                                    Icons.Filled.Close,
                                    contentDescription = "Clear",
                                    tint = Color.White.copy(alpha = 0.7f),
                                    modifier = Modifier
                                        .size(20.dp)
                                        .clip(CircleShape)
                                        .clickable { onQueryChange("") }
                                        .padding(2.dp)
                                )
                            }
                        }
                    }

                    Spacer(Modifier.width(10.dp))

                    // Cancel / Close Pill Button
                    Box(
                        Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color.White.copy(alpha = 0.08f))
                            .border(0.8.dp, Color.White.copy(alpha = 0.12f), RoundedCornerShape(12.dp))
                            .clickable(onClick = onDismiss)
                            .padding(horizontal = 12.dp, vertical = 11.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (tr) "Vazgeç" else "Cancel",
                            color = Color.White.copy(alpha = 0.85f),
                            fontSize = 12.5.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                // Filter Category Chips (All, Live, Movies, Series)
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val allCount = liveResults.size + movieResults.size + seriesResults.size
                    SearchFilterPill(
                        label = if (tr) "Tümü" else "All",
                        count = if (query.isNotBlank()) allCount else null,
                        selected = selectedFilter == "ALL",
                        onClick = { selectedFilter = "ALL" }
                    )
                    SearchFilterPill(
                        label = if (tr) "Canlı TV" else "Live",
                        count = if (query.isNotBlank()) liveResults.size else null,
                        selected = selectedFilter == "LIVE",
                        onClick = { selectedFilter = "LIVE" }
                    )
                    SearchFilterPill(
                        label = if (tr) "Filmler" else "Movies",
                        count = if (query.isNotBlank()) movieResults.size else null,
                        selected = selectedFilter == "MOVIES",
                        onClick = { selectedFilter = "MOVIES" }
                    )
                    SearchFilterPill(
                        label = if (tr) "Diziler" else "Series",
                        count = if (query.isNotBlank()) seriesResults.size else null,
                        selected = selectedFilter == "SERIES",
                        onClick = { selectedFilter = "SERIES" }
                    )
                }


                // Main Content Area (History vs Results)
                if (query.isBlank()) {
                    // Recent Search History View
                    if (searchHistory.isNotEmpty()) {
                        Column(
                            Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 18.dp, vertical = 14.dp)
                        ) {
                            Row(
                                Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        Icons.Filled.History,
                                        contentDescription = null,
                                        tint = cyan,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(Modifier.width(6.dp))
                                    Text(
                                        text = if (tr) "Son Aramalar" else "Recent Searches",
                                        color = Color.White,
                                        fontSize = 13.5.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                Text(
                                    text = if (tr) "Tümünü Temizle" else "Clear All",
                                    color = Color.White.copy(alpha = 0.5f),
                                    fontSize = 11.5.sp,
                                    fontWeight = FontWeight.Medium,
                                    modifier = Modifier.clickable { onClearHistory() }
                                )
                            }

                            Spacer(Modifier.height(10.dp))

                            LazyRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                contentPadding = PaddingValues(bottom = 8.dp)
                            ) {
                                items(searchHistory) { tag ->
                                    Box(
                                        Modifier
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(Color.White.copy(alpha = 0.08f))
                                            .border(1.dp, Color.White.copy(alpha = 0.12f), RoundedCornerShape(12.dp))
                                            .clickable { onRecentClick(tag) }
                                            .padding(horizontal = 12.dp, vertical = 7.dp)
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(
                                                text = tag,
                                                color = Color.White.copy(alpha = 0.90f),
                                                fontSize = 12.5.sp
                                            )
                                            Spacer(Modifier.width(8.dp))
                                            Icon(
                                                Icons.Filled.Close,
                                                contentDescription = "Remove",
                                                tint = Color.White.copy(alpha = 0.5f),
                                                modifier = Modifier
                                                    .size(14.dp)
                                                    .clickable { onRemoveRecent(tag) }
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    } else {
                        // Empty Search State / Hint
                        Column(
                            Modifier
                                .fillMaxWidth()
                                .padding(vertical = 24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                Icons.Filled.Search,
                                contentDescription = null,
                                tint = cyan.copy(alpha = 0.40f),
                                modifier = Modifier.size(38.dp)
                            )
                            Spacer(Modifier.height(8.dp))
                            Text(
                                text = if (tr) "Hızlı Arama" else "Quick Search",
                                color = Color.White,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(
                                text = if (tr) "Film, dizi ve canlı kanalları anında bulun" else "Search instantly across movies, series and live channels",
                                color = Color.White.copy(alpha = 0.55f),
                                fontSize = 12.sp,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(horizontal = 24.dp)
                            )
                        }
                    }
                } else {

                    // Search Results List
                    val showLive = (selectedFilter == "ALL" || selectedFilter == "LIVE") && liveResults.isNotEmpty()
                    val showMovies = (selectedFilter == "ALL" || selectedFilter == "MOVIES") && movieResults.isNotEmpty()
                    val showSeries = (selectedFilter == "ALL" || selectedFilter == "SERIES") && seriesResults.isNotEmpty()
                    val totalFound = liveResults.size + movieResults.size + seriesResults.size

                    if (totalFound == 0 && !searching) {
                        Column(
                            Modifier
                                .fillMaxWidth()
                                .padding(top = 50.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = if (tr) "Sonuç Bulunamadı" else "No Results Found",
                                color = Color.White,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(
                                text = if (tr) "\"$query\" için içerik bulunamadı" else "No content found for \"$query\"",
                                color = Color.White.copy(alpha = 0.50f),
                                fontSize = 13.sp
                            )
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            // Live Channels Section
                            if (showLive) {
                                item(key = "search_live_header") {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            Icons.Filled.Tv,
                                            contentDescription = null,
                                            tint = emerald,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(Modifier.width(6.dp))
                                        Text(
                                            text = "${if (tr) "Canlı Kanallar" else "Live Channels"} (${liveResults.size})",
                                            color = Color.White,
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }

                                item(key = "search_live_row") {
                                    LazyRow(
                                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        items(liveResults, key = { it.id }) { ch ->
                                            SearchLiveChannelPill(ch, onPlay = { onPlayLive(ch) })
                                        }
                                    }
                                }
                            }

                            // Movies Section
                            if (showMovies) {
                                item(key = "search_movies_header") {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            Icons.Filled.Movie,
                                            contentDescription = null,
                                            tint = cyan,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(Modifier.width(6.dp))
                                        Text(
                                            text = "${if (tr) "Filmler" else "Movies"} (${movieResults.size})",
                                            color = Color.White,
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }

                                item(key = "search_movies_row") {
                                    LazyRow(
                                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        items(movieResults, key = { it.id }) { m ->
                                            SearchVodPosterCard(m, onClick = { onOpenMovie(m) })
                                        }
                                    }
                                }
                            }

                            // Series Section
                            if (showSeries) {
                                item(key = "search_series_header") {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            Icons.Filled.LiveTv,
                                            contentDescription = null,
                                            tint = Color(0xFFC084FC),
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(Modifier.width(6.dp))
                                        Text(
                                            text = "${if (tr) "Diziler" else "Series"} (${seriesResults.size})",
                                            color = Color.White,
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }

                                item(key = "search_series_row") {
                                    LazyRow(
                                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        items(seriesResults, key = { it.id }) { s ->
                                            SearchVodPosterCard(s, onClick = { onOpenSeries(s) })
                                        }
                                    }
                                }
                            }

                            item {
                                Spacer(Modifier.height(20.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SearchFilterPill(
    label: String,
    count: Int?,
    selected: Boolean,
    onClick: () -> Unit
) {
    val cyan = Color(0xFF22D3EE)

    Box(
        Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(
                if (selected) cyan.copy(alpha = 0.25f) else Color.White.copy(alpha = 0.08f)
            )
            .border(
                width = 1.dp,
                color = if (selected) cyan else Color.White.copy(alpha = 0.12f),
                shape = RoundedCornerShape(12.dp)
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 11.dp, vertical = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = if (count != null && count > 0) "$label ($count)" else label,
            color = if (selected) cyan else Color.White.copy(alpha = 0.85f),
            fontSize = 12.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
        )
    }
}

@Composable
private fun SearchLiveChannelPill(
    channel: ChannelEntity,
    onPlay: () -> Unit
) {
    val emerald = Color(0xFF34D399)

    Box(
        Modifier
            .width(160.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(Color.White.copy(alpha = 0.07f))
            .border(1.dp, Color.White.copy(alpha = 0.12f), RoundedCornerShape(12.dp))
            .clickable(onClick = onPlay)
            .padding(8.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Box(
                Modifier
                    .size(34.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color.White.copy(alpha = 0.08f)),
                contentAlignment = Alignment.Center
            ) {
                if (channel.logo.isNotBlank()) {
                    AsyncImage(
                        model = channel.logo,
                        contentDescription = channel.name,
                        contentScale = ContentScale.Fit,
                        modifier = Modifier.size(26.dp)
                    )
                } else {
                    Icon(
                        Icons.Filled.Tv,
                        contentDescription = null,
                        tint = emerald,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Spacer(Modifier.width(8.dp))

            Column(Modifier.weight(1f)) {
                Text(
                    text = channel.name,
                    color = Color.White,
                    fontSize = 11.5.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "CANLI",
                    color = Color(0xFFEF4444),
                    fontSize = 9.sp,
                    fontWeight = FontWeight.ExtraBold
                )
            }
        }
    }
}

@Composable
private fun SearchVodPosterCard(
    item: VodEntity,
    onClick: () -> Unit
) {
    Box(
        Modifier
            .width(108.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(Color.White.copy(alpha = 0.07f))
            .border(1.dp, Color.White.copy(alpha = 0.12f), RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
    ) {

        Column {
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(148.dp)
            ) {
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(item.poster)
                        .crossfade(true)
                        .build(),
                    contentDescription = item.name,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )

                if (item.rating.isNotBlank() && item.rating != "0") {
                    Row(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(4.dp)
                            .clip(RoundedCornerShape(5.dp))
                            .background(Color.Black.copy(alpha = 0.70f))
                            .padding(horizontal = 4.dp, vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Filled.Star,
                            contentDescription = null,
                            tint = Color(0xFFF59E0B),
                            modifier = Modifier.size(10.dp)
                        )
                        Spacer(Modifier.width(2.dp))
                        Text(
                            text = item.rating.take(3),
                            color = Color.White,
                            fontSize = 9.5.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Column(
                Modifier
                    .fillMaxWidth()
                    .padding(6.dp)
            ) {
                Text(
                    text = item.name,
                    color = Color.White,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = item.categoryName.ifBlank { item.genre }.ifBlank { if (item.kind == "SERIES") "Dizi" else "Film" },
                    color = Color.White.copy(alpha = 0.50f),
                    fontSize = 9.5.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}
