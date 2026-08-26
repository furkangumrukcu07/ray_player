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
    EpgMixKind.REPLAY,
    EpgMixKind.SPORT,
    EpgMixKind.DOCUMENTARY,
    EpgMixKind.FILM,
    EpgMixKind.SERIES,
    EpgMixKind.NEWS,
    EpgMixKind.ALL
)

@Composable
fun MobileEpgMixScreen(
    vm: RayViewModel,
    tr: Boolean,
    onBack: () -> Unit,
    onPlayLive: (ChannelEntity) -> Unit,
    onCatchup: (ChannelEntity, EpgEntity) -> Unit
) {
    var selectedKind by remember { mutableStateOf(EpgMixKind.REPLAY) }
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

    Column(
        Modifier
            .fillMaxSize()
            .background(Color(0xFF090E0B))
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
                    .background(Color.White.copy(alpha = 0.08f))
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

            Text(
                text = if (tr) "Tekrar & EPG Mix" else "Replay & EPG Mix",
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                lineHeight = 22.sp,
                modifier = Modifier.weight(1f)
            )

            Box(
                Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(if (isSearchOpen) MobileCyan else Color.White.copy(alpha = 0.08f))
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
                    .clip(RoundedCornerShape(14.dp))
                    .background(Color(0xFF101713))
                    .border(1.dp, MobileCyan.copy(alpha = 0.5f), RoundedCornerShape(14.dp))
                    .padding(horizontal = 12.dp, vertical = 10.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.Search, contentDescription = null, tint = MobileCyan, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    BasicTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        textStyle = TextStyle(color = Color.White, fontSize = 14.sp),
                        cursorBrush = SolidColor(MobileCyan),
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

        // Category Filter Chips (Matching Mina IPTV style with Count Badge)
        LazyRow(
            contentPadding = PaddingValues(horizontal = 14.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(MixOrder) { kind ->
                val selected = selectedKind == kind
                val count = if (selected) filteredItems.size else null
                Row(
                    Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(if (selected) Color(0xFF133630) else Color.White.copy(alpha = 0.05f))
                        .border(
                            if (selected) 1.2.dp else 1.dp,
                            if (selected) Color(0xFF22D3EE) else Color.White.copy(alpha = 0.12f),
                            RoundedCornerShape(20.dp)
                        )
                        .rayClickable(onClick = { selectedKind = kind })
                        .padding(start = 12.dp, top = 6.dp, end = 12.dp, bottom = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        kind.icon(),
                        contentDescription = null,
                        tint = if (selected) Color(0xFF22D3EE) else Color.White,
                        modifier = Modifier.size(15.dp)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        kind.label(tr),
                        color = Color.White,
                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                        fontSize = 13.sp,
                        lineHeight = 16.sp
                    )
                    if (count != null && count > 0) {
                        Spacer(Modifier.width(6.dp))
                        Box(
                            Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .background(Color(0xFF22D3EE).copy(alpha = 0.25f))
                                .padding(horizontal = 6.dp, vertical = 2.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                "$count",
                                color = Color(0xFF22D3EE),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                lineHeight = 13.sp
                            )
                        }
                    }
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
                            .background(if (selected) Color(0xFF133630).copy(alpha = 0.7f) else Color.White.copy(alpha = 0.04f))
                            .border(
                                1.dp,
                                if (selected) Color(0xFF22D3EE).copy(alpha = 0.7f) else Color.White.copy(alpha = 0.08f),
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
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(filteredItems, key = { it.channel.id + "_" + it.programme.id + "_" + it.programme.startMs }) { row ->
                    val p = row.programme
                    val isLive = p.startMs <= now && p.endMs > now
                    val isPast = p.endMs <= now
                    val isCatchup = isPast && (p.hasCatchup || row.channel.hasArchive)

                    val startStr = timeFmt.format(Date(p.startMs))
                    val endStr = timeFmt.format(Date(p.endMs))

                    val timeAgoStr = remember(p.endMs, now) {
                        val diffMs = (now - p.endMs).coerceAtLeast(0L)
                        val diffMins = (diffMs / (1000 * 60)).toInt()
                        val diffHours = diffMins / 60
                        val diffDays = diffHours / 24

                        when {
                            diffDays > 0 -> if (tr) "$diffDays gün önce" else "$diffDays d ago"
                            diffHours > 0 -> if (tr) "$diffHours saat önce" else "$diffHours h ago"
                            diffMins > 0 -> if (tr) "$diffMins dk önce" else "$diffMins min ago"
                            else -> if (tr) "az önce" else "just now"
                        }
                    }

                    Box(
                        Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(18.dp))
                            .background(Color(0xFF101713).copy(alpha = 0.88f))
                            .border(1.dp, Color.White.copy(alpha = 0.12f), RoundedCornerShape(18.dp))
                            .rayClickable(onClick = {
                                if (isCatchup) {
                                    onCatchup(row.channel, p)
                                } else {
                                    onPlayLive(row.channel)
                                }
                            })
                            .padding(horizontal = 16.dp, vertical = 14.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            // Programme Info Column (Matching Mina IPTV Layout)
                            Column(Modifier.weight(1f)) {
                                // 1. Title (Program Adı)
                                Text(
                                    text = p.title.ifBlank { if (tr) "Program Bilgisi Yok" else "No Program Info" },
                                    color = Color.White,
                                    fontSize = 15.5.sp,
                                    lineHeight = 19.sp,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )

                                Spacer(Modifier.height(3.dp))

                                // 2. Channel Name (Kanal İsmi with Cyan TV icon)
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        Icons.Filled.LiveTv,
                                        contentDescription = null,
                                        tint = Color(0xFF22D3EE),
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(Modifier.width(6.dp))
                                    Text(
                                        text = row.channel.name,
                                        color = Color.White.copy(alpha = 0.85f),
                                        fontSize = 13.sp,
                                        lineHeight = 16.sp,
                                        fontWeight = FontWeight.Medium,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }

                                Spacer(Modifier.height(3.dp))

                                // 3. Time Interval (Saat Aralığı)
                                Text(
                                    text = "$startStr — $endStr",
                                    color = Color.White.copy(alpha = 0.55f),
                                    fontSize = 12.sp,
                                    lineHeight = 15.sp
                                )

                                Spacer(Modifier.height(3.dp))

                                // 4. Tekrar / Canlı Durumu
                                if (isLive) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Box(
                                            Modifier
                                                .size(6.dp)
                                                .clip(CircleShape)
                                                .background(Color(0xFFEF4444))
                                        )
                                        Spacer(Modifier.width(5.dp))
                                        Text(
                                            text = if (tr) "Canlı Yayın · Şimdi" else "Live · Now",
                                            color = Color(0xFFEF4444),
                                            fontSize = 11.5.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                } else {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            Icons.Filled.Replay,
                                            contentDescription = null,
                                            tint = Color(0xFF22D3EE),
                                            modifier = Modifier.size(12.dp)
                                        )
                                        Spacer(Modifier.width(4.dp))
                                        Text(
                                            text = if (tr) "Tekrar · $timeAgoStr" else "Replay · $timeAgoStr",
                                            color = Color.White.copy(alpha = 0.65f),
                                            fontSize = 11.5.sp,
                                            lineHeight = 14.sp
                                        )
                                    }
                                }
                            }

                            Spacer(Modifier.width(12.dp))

                            // Right Side: Channel Logo (Mina IPTV style)
                            Box(
                                Modifier
                                    .width(60.dp)
                                    .height(38.dp),
                                contentAlignment = Alignment.CenterEnd
                            ) {
                                if (row.channel.logo.isNotBlank()) {
                                    AsyncImage(
                                        model = row.channel.logo,
                                        contentDescription = null,
                                        modifier = Modifier.size(width = 56.dp, height = 36.dp),
                                        contentScale = ContentScale.Fit
                                    )
                                } else {
                                    Icon(
                                        Icons.Filled.LiveTv,
                                        contentDescription = null,
                                        tint = Color.White.copy(alpha = 0.35f),
                                        modifier = Modifier.size(24.dp)
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
    EpgMixKind.REPLAY -> if (tr) "Tekrar" else "Replay"
    EpgMixKind.SPORT -> if (tr) "Spor" else "Sport"
    EpgMixKind.DOCUMENTARY -> if (tr) "Belgesel" else "Documentary"
    EpgMixKind.FILM -> if (tr) "Film" else "Film"
    EpgMixKind.SERIES -> if (tr) "Dizi" else "Series"
    EpgMixKind.NEWS -> if (tr) "Haber" else "News"
    EpgMixKind.ALL -> if (tr) "Tümü" else "All"
}

private fun EpgMixKind.icon(): ImageVector = when (this) {
    EpgMixKind.REPLAY -> Icons.Filled.Replay
    EpgMixKind.SPORT -> Icons.Filled.SportsSoccer
    EpgMixKind.DOCUMENTARY -> Icons.AutoMirrored.Filled.MenuBook
    EpgMixKind.FILM -> Icons.Filled.Movie
    EpgMixKind.SERIES -> Icons.Filled.Tv
    EpgMixKind.NEWS -> Icons.Filled.Newspaper
    EpgMixKind.ALL -> Icons.Filled.ViewTimeline
}
