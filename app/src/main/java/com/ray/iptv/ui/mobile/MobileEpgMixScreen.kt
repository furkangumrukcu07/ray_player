package com.ray.iptv.ui.mobile

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.LiveTv
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.Newspaper
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Replay
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SportsSoccer
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material.icons.filled.ViewTimeline
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Icon
import androidx.tv.material3.Text
import coil.compose.AsyncImage
import com.ray.iptv.data.local.ChannelEntity
import com.ray.iptv.data.local.EpgEntity
import com.ray.iptv.ui.EpgMixKind
import com.ray.iptv.ui.RayViewModel
import com.ray.iptv.ui.ShowcaseEpgMixItem
import com.ray.iptv.ui.input.rayClickable
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val MixOrder = listOf(
    EpgMixKind.ALL,
    EpgMixKind.REPLAY,
    EpgMixKind.SPORT,
    EpgMixKind.DOCUMENTARY,
    EpgMixKind.FILM,
    EpgMixKind.SERIES,
    EpgMixKind.NEWS
)

@Composable
fun MobileEpgMixScreen(
    vm: RayViewModel,
    tr: Boolean,
    onBack: () -> Unit,
    onPlayLive: (ChannelEntity) -> Unit,
    onCatchup: (ChannelEntity, EpgEntity) -> Unit
) {
    var selectedKind by remember { mutableStateOf(EpgMixKind.ALL) }
    var selectedDayOffset by remember { mutableIntStateOf(0) }
    var searchQuery by remember { mutableStateOf("") }
    var isSearchOpen by remember { mutableStateOf(false) }
    val focusManager = LocalFocusManager.current

    var itemsList by remember { mutableStateOf<List<ShowcaseEpgMixItem>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(selectedKind, selectedDayOffset) {
        isLoading = true
        itemsList = if (selectedDayOffset == 0) {
            vm.showcaseEpgMix(selectedKind)
        } else {
            vm.showcaseEpgMixForDay(selectedKind, selectedDayOffset)
        }
        isLoading = false
    }

    val filteredItems = remember(itemsList, searchQuery) {
        if (searchQuery.isBlank()) {
            itemsList
        } else {
            val q = searchQuery.trim().lowercase()
            itemsList.filter {
                it.channel.name.lowercase().contains(q) ||
                it.programme.title.lowercase().contains(q) ||
                it.programme.plot.lowercase().contains(q)
            }
        }
    }

    val now = remember { System.currentTimeMillis() }
    val timeFmt = remember { SimpleDateFormat("HH:mm", Locale.getDefault()) }

    val cyan = Color(0xFF00E5FF)
    val purple = Color(0xFFAB47BC)
    val red = Color(0xFFEF4444)

    Column(
        Modifier
            .fillMaxSize()
            .background(Color(0xFF0B0F19))
    ) {
        // Top Header
        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.12f))
                    .rayClickable(onClick = onBack),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(Modifier.width(12.dp))

            Column(Modifier.weight(1f)) {
                Text(
                    text = if (tr) "Tekrar & EPG Rehberi" else "Replay & EPG Guide",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 17.sp,
                    lineHeight = 20.sp,
                    maxLines = 1
                )
                Text(
                    text = if (tr) "Yayın akışı, canlı TV ve geriye dönük tekrarlar" else "TV guide, live broadcasts and catch-up",
                    color = Color.White.copy(alpha = 0.65f),
                    fontSize = 11.5.sp,
                    lineHeight = 14.sp,
                    maxLines = 1
                )
            }

            Box(
                Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(if (isSearchOpen) cyan else Color.White.copy(alpha = 0.12f))
                    .rayClickable(onClick = {
                        isSearchOpen = !isSearchOpen
                        if (!isSearchOpen) searchQuery = ""
                    }),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (isSearchOpen) Icons.Filled.Close else Icons.Filled.Search,
                    contentDescription = null,
                    tint = if (isSearchOpen) Color.Black else Color.White,
                    modifier = Modifier.size(19.dp)
                )
            }
        }

        // Search Field if Open
        if (isSearchOpen) {
            Box(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 4.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color.White.copy(alpha = 0.08f))
                    .border(1.dp, cyan.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
                    .padding(horizontal = 12.dp, vertical = 10.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.Search, contentDescription = null, tint = cyan, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    BasicTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        textStyle = TextStyle(color = Color.White, fontSize = 14.sp),
                        cursorBrush = SolidColor(cyan),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                        keyboardActions = KeyboardActions(onSearch = { focusManager.clearFocus() }),
                        modifier = Modifier.weight(1f),
                        decorationBox = { innerTextField ->
                            if (searchQuery.isEmpty()) {
                                Text(
                                    if (tr) "Kanal veya program ara..." else "Search channel or programme...",
                                    color = Color.White.copy(alpha = 0.45f),
                                    fontSize = 13.5.sp,
                                    lineHeight = 16.sp
                                )
                            }
                            innerTextField()
                        }
                    )
                    if (searchQuery.isNotEmpty()) {
                        Icon(
                            Icons.Filled.Close,
                            contentDescription = null,
                            tint = Color.White.copy(alpha = 0.7f),
                            modifier = Modifier
                                .size(18.dp)
                                .rayClickable(onClick = { searchQuery = "" })
                        )
                    }
                }
            }
            Spacer(Modifier.height(6.dp))
        }

        // Category Filter Chips
        LazyRow(
            contentPadding = PaddingValues(horizontal = 14.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(MixOrder) { kind ->
                val selected = selectedKind == kind
                Row(
                    Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(if (selected) cyan else Color.White.copy(alpha = 0.08f))
                        .border(
                            1.dp,
                            if (selected) cyan else Color.White.copy(alpha = 0.12f),
                            RoundedCornerShape(20.dp)
                        )
                        .rayClickable(onClick = { selectedKind = kind })
                        .padding(horizontal = 13.dp, vertical = 7.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        kind.icon(),
                        contentDescription = null,
                        tint = if (selected) Color.Black else Color.White,
                        modifier = Modifier.size(15.dp)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        kind.label(tr),
                        color = if (selected) Color.Black else Color.White,
                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                        fontSize = 12.5.sp,
                        lineHeight = 15.sp
                    )
                }
            }
        }

        // Day Selector Chips (for Catch-up / Replay or browsing past days)
        if (selectedKind == EpgMixKind.REPLAY || selectedKind == EpgMixKind.ALL) {
            Spacer(Modifier.height(6.dp))
            LazyRow(
                contentPadding = PaddingValues(horizontal = 14.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                val dayOptions = listOf(
                    -3 to (if (tr) "3 Gün Önce" else "3 Days Ago"),
                    -2 to (if (tr) "2 Gün Önce" else "2 Days Ago"),
                    -1 to (if (tr) "Dün" else "Yesterday"),
                    0 to (if (tr) "Bugün" else "Today"),
                    1 to (if (tr) "Yarın" else "Tomorrow")
                )
                items(dayOptions) { (offset, label) ->
                    val selected = selectedDayOffset == offset
                    Box(
                        Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (selected) purple.copy(alpha = 0.35f) else Color.White.copy(alpha = 0.05f))
                            .border(
                                1.dp,
                                if (selected) purple else Color.White.copy(alpha = 0.08f),
                                RoundedCornerShape(10.dp)
                            )
                            .rayClickable(onClick = { selectedDayOffset = offset })
                            .padding(horizontal = 10.dp, vertical = 5.dp)
                    ) {
                        Text(
                            label,
                            color = if (selected) Color.White else Color.White.copy(alpha = 0.7f),
                            fontSize = 11.5.sp,
                            lineHeight = 14.sp,
                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(8.dp))

        // Content List
        if (isLoading) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    if (tr) "EPG rehberi yükleniyor..." else "Loading TV guide...",
                    color = Color.White.copy(alpha = 0.6f),
                    fontSize = 13.5.sp,
                    lineHeight = 16.sp
                )
            }
        } else if (filteredItems.isEmpty()) {
            Box(
                Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(
                        Icons.Filled.ViewTimeline,
                        contentDescription = null,
                        tint = Color.White.copy(alpha = 0.3f),
                        modifier = Modifier.size(48.dp)
                    )
                    Text(
                        if (searchQuery.isNotEmpty()) {
                            if (tr) "\"$searchQuery\" ile eşleşen yayın bulunamadı" else "No broadcasts matching \"$searchQuery\""
                        } else {
                            if (tr) "Bu kategoride gösterilecek EPG programı bulunamadı" else "No EPG programmes found in this category"
                        },
                        color = Color.White.copy(alpha = 0.7f),
                        fontSize = 13.5.sp,
                        lineHeight = 17.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        if (tr) "EPG kaynaklarını Ayarlar > EPG bölümünden güncelleyebilirsiniz."
                        else "You can update your EPG sources in Settings > EPG.",
                        color = Color.White.copy(alpha = 0.45f),
                        fontSize = 12.sp,
                        lineHeight = 15.sp
                    )
                }
            }
        } else {
            LazyColumn(
                contentPadding = PaddingValues(start = 14.dp, end = 14.dp, top = 4.dp, bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(filteredItems, key = { it.channel.id + "_" + it.programme.id + "_" + it.programme.startMs }) { row ->
                    val p = row.programme
                    val isLive = p.startMs <= now && p.endMs > now
                    val isPast = p.endMs <= now
                    val isCatchup = isPast && (p.hasCatchup || row.channel.hasArchive)

                    val startStr = timeFmt.format(Date(p.startMs))
                    val endStr = timeFmt.format(Date(p.endMs))

                    val liveProgress = if (isLive && p.endMs > p.startMs) {
                        ((now - p.startMs).toFloat() / (p.endMs - p.startMs).toFloat()).coerceIn(0f, 1f)
                    } else 0f

                    Box(
                        Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(Color(0xFF161E2E))
                            .border(1.dp, if (isLive) red.copy(alpha = 0.35f) else Color.White.copy(alpha = 0.08f), RoundedCornerShape(14.dp))
                            .rayClickable(onClick = {
                                if (isCatchup) {
                                    onCatchup(row.channel, p)
                                } else {
                                    onPlayLive(row.channel)
                                }
                            })
                            .padding(12.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            // Channel Logo Box
                            Box(
                                Modifier
                                    .size(46.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(Color.Black.copy(alpha = 0.4f))
                                    .border(0.5.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(10.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                if (row.channel.logo.isNotBlank()) {
                                    AsyncImage(
                                        model = row.channel.logo,
                                        contentDescription = null,
                                        modifier = Modifier.size(36.dp),
                                        contentScale = ContentScale.Fit
                                    )
                                } else {
                                    Icon(
                                        Icons.Filled.LiveTv,
                                        contentDescription = null,
                                        tint = Color.White.copy(alpha = 0.5f),
                                        modifier = Modifier.size(22.dp)
                                    )
                                }
                            }

                            Spacer(Modifier.width(12.dp))

                            // Programme Info Column (No Overlap)
                            Column(Modifier.weight(1f)) {
                                // Channel Name
                                Text(
                                    text = row.channel.name,
                                    color = cyan,
                                    fontSize = 12.5.sp,
                                    lineHeight = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )

                                Spacer(Modifier.height(3.dp))

                                // Programme Title
                                Text(
                                    text = p.title.ifBlank { if (tr) "Program Bilgisi Yok" else "No Program Info" },
                                    color = Color.White,
                                    fontSize = 13.sp,
                                    lineHeight = 17.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )

                                Spacer(Modifier.height(4.dp))

                                // Progress Bar if Live
                                if (isLive) {
                                    Box(
                                        Modifier
                                            .fillMaxWidth()
                                            .height(3.dp)
                                            .clip(RoundedCornerShape(2.dp))
                                            .background(Color.White.copy(alpha = 0.15f))
                                    ) {
                                        Box(
                                            Modifier
                                                .fillMaxHeight()
                                                .fillMaxWidth(liveProgress)
                                                .background(red)
                                        )
                                    }
                                    Spacer(Modifier.height(4.dp))
                                }

                                // Time Interval
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        Icons.Filled.Schedule,
                                        contentDescription = null,
                                        tint = Color.White.copy(alpha = 0.5f),
                                        modifier = Modifier.size(12.dp)
                                    )
                                    Spacer(Modifier.width(4.dp))
                                    Text(
                                        text = "$startStr - $endStr",
                                        color = Color.White.copy(alpha = 0.65f),
                                        fontSize = 11.5.sp,
                                        lineHeight = 14.sp
                                    )
                                }
                            }

                            Spacer(Modifier.width(10.dp))

                            // Action Badge
                            Box(
                                Modifier
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(
                                        when {
                                            isLive -> red.copy(alpha = 0.2f)
                                            isCatchup -> purple.copy(alpha = 0.25f)
                                            else -> Color.White.copy(alpha = 0.08f)
                                        }
                                    )
                                    .border(
                                        1.dp,
                                        when {
                                            isLive -> red.copy(alpha = 0.6f)
                                            isCatchup -> purple.copy(alpha = 0.6f)
                                            else -> Color.White.copy(alpha = 0.15f)
                                        },
                                        RoundedCornerShape(10.dp)
                                    )
                                    .padding(horizontal = 9.dp, vertical = 6.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = when {
                                            isLive -> Icons.Filled.PlayArrow
                                            isCatchup -> Icons.Filled.Replay
                                            else -> Icons.Filled.Schedule
                                        },
                                        contentDescription = null,
                                        tint = when {
                                            isLive -> red
                                            isCatchup -> Color(0xFFE1BEE7)
                                            else -> Color.White.copy(alpha = 0.8f)
                                        },
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(Modifier.width(4.dp))
                                    Text(
                                        text = when {
                                            isLive -> if (tr) "CANLI" else "LIVE"
                                            isCatchup -> if (tr) "TEKRAR" else "REPLAY"
                                            else -> startStr
                                        },
                                        color = when {
                                            isLive -> red
                                            isCatchup -> Color(0xFFE1BEE7)
                                            else -> Color.White.copy(alpha = 0.85f)
                                        },
                                        fontSize = 11.sp,
                                        lineHeight = 13.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun EpgMixKind.label(tr: Boolean): String = when (this) {
    EpgMixKind.ALL -> if (tr) "Tümü" else "All"
    EpgMixKind.REPLAY -> if (tr) "Tekrar (Catch-up)" else "Replay"
    EpgMixKind.SPORT -> if (tr) "Spor" else "Sport"
    EpgMixKind.DOCUMENTARY -> if (tr) "Belgesel" else "Documentary"
    EpgMixKind.FILM -> if (tr) "Film" else "Film"
    EpgMixKind.SERIES -> if (tr) "Dizi" else "Series"
    EpgMixKind.NEWS -> if (tr) "Haber" else "News"
}

private fun EpgMixKind.icon(): ImageVector = when (this) {
    EpgMixKind.ALL -> Icons.Filled.ViewTimeline
    EpgMixKind.REPLAY -> Icons.Filled.Replay
    EpgMixKind.SPORT -> Icons.Filled.SportsSoccer
    EpgMixKind.DOCUMENTARY -> Icons.AutoMirrored.Filled.MenuBook
    EpgMixKind.FILM -> Icons.Filled.Movie
    EpgMixKind.SERIES -> Icons.Filled.Tv
    EpgMixKind.NEWS -> Icons.Filled.Newspaper
}
