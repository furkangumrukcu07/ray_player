package com.ray.iptv.ui.mobile

import androidx.compose.foundation.background
import com.ray.iptv.ui.player.playerGestureDrag
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import android.content.Intent
import androidx.compose.ui.platform.LocalContext
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.automirrored.filled.NavigateBefore
import androidx.compose.material.icons.automirrored.filled.NavigateNext
import androidx.compose.material.icons.filled.AspectRatio
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Cast
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.FastForward
import androidx.compose.material.icons.filled.FastRewind
import androidx.compose.material.icons.filled.HighQuality
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Replay
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.ViewStream
import androidx.compose.material.icons.filled.ViewTimeline
import androidx.compose.foundation.clickable
import androidx.compose.material.icons.Icons
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.annotation.OptIn
import androidx.media3.common.util.UnstableApi
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import androidx.tv.material3.Icon
import androidx.tv.material3.Text
import coil.compose.AsyncImage
import com.ray.iptv.data.local.CategoryEntity
import com.ray.iptv.data.local.ChannelEntity
import com.ray.iptv.data.local.EpgEntity
import com.ray.iptv.data.local.FavoriteEntity
import com.ray.iptv.data.local.ProgressEntity
import com.ray.iptv.data.repo.AspectMode
import com.ray.iptv.data.repo.PlaybackEngine
import com.ray.iptv.data.repo.StreamFormat
import com.ray.iptv.player.MediaKitComposeSurface
import com.ray.iptv.player.RayPlayer
import com.ray.iptv.player.TrackOption
import com.ray.iptv.ui.Playback
import com.ray.iptv.ui.input.rayClickable
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private enum class LiveTab { CATS, CHANS, EPG }

private val PortraitPanel = Color(0xE60B0F14)
private val PortraitPanelBorder = Color.White.copy(alpha = 0.18f)

@Composable
fun MobileLiveBrowseScreen(
    tr: Boolean,
    categories: List<CategoryEntity>,
    channels: List<ChannelEntity>,
    allCount: Int,
    categoryCounts: Map<String, Int>,
    favorites: List<FavoriteEntity>,
    recent: List<ProgressEntity>,
    selectedCategory: String,
    nowByChannel: Map<String, EpgEntity>,
    onCategory: (String) -> Unit,
    onPlay: (ChannelEntity) -> Unit,
    onLoadNowMap: (List<String>) -> Unit,
    stripPrefix: Boolean,
    pills: Boolean = false,
    playingId: String = "",
    onLoadMore: () -> Unit = {},
    startOnChannels: Boolean = false,
    dayProgrammes: List<EpgEntity> = emptyList(),
    archiveDayOffset: Int = 0,
    onArchiveDayChange: ((Int) -> Unit)? = null,
    onProgramme: ((EpgEntity) -> Unit)? = null,
    onBrowseChannels: () -> Unit = {},
    onBrowseCategories: () -> Unit = {}
) {
    val tabs = LiveTab.entries
    val pager = rememberPagerState(
        initialPage = if (startOnChannels) LiveTab.CHANS.ordinal else LiveTab.CATS.ordinal,
        pageCount = { tabs.size }
    )
    val scope = rememberCoroutineScope()
    val tab = tabs[pager.currentPage.coerceIn(tabs.indices)]
    fun go(next: LiveTab) {
        if (pager.currentPage != next.ordinal) {
            scope.launch { pager.animateScrollToPage(next.ordinal) }
        }
    }
    LaunchedEffect(startOnChannels, pills) {
        if (!pills) {
            if (startOnChannels) pager.scrollToPage(LiveTab.CHANS.ordinal)
            return@LaunchedEffect
        }
        if (startOnChannels) {
            if (pager.currentPage == LiveTab.CATS.ordinal) pager.scrollToPage(LiveTab.CHANS.ordinal)
        } else if (pager.currentPage != LiveTab.CATS.ordinal) {
            pager.scrollToPage(LiveTab.CATS.ordinal)
        }
    }
    LaunchedEffect(pager.currentPage, pills) {
        if (!pills) return@LaunchedEffect
        if (pager.currentPage == LiveTab.CATS.ordinal) onBrowseCategories() else onBrowseChannels()
    }
    val favCount = favorites.count { it.kind == "LIVE" }
    val catName = when (selectedCategory) {
        "", "all" -> if (tr) "Tüm kanallar" else "All channels"
        "fav" -> if (tr) "Favoriler" else "Favorites"
        "recent" -> if (tr) "Son İzlenenler" else "Recently watched"
        else -> categories.firstOrNull { it.id == selectedCategory }?.name ?: selectedCategory
    }
    LaunchedEffect(channels.map { it.id }) { onLoadNowMap(channels.map { it.id }) }
    val panelShape = RoundedCornerShape(22.dp)
    val watchShape = RoundedCornerShape(18.dp)
    Column(
        Modifier
            .fillMaxSize()
            .then(
                if (!pills) {
                    Modifier
                        .clip(watchShape)
                        .background(PortraitPanel)
                        .border(1.dp, Color.White.copy(alpha = 0.16f), watchShape)
                } else Modifier
            )
    ) {
        MobileLiveTabs(tr, tab, pills) { go(it) }
        if (!pills) {
            Box(Modifier.fillMaxWidth().height(1.dp).background(Color.White.copy(alpha = 0.13f)))
        }
        HorizontalPager(
            state = pager,
            modifier = if (pills) {
                Modifier
                    .weight(1f)
                    .padding(start = 12.dp, end = 12.dp, bottom = 10.dp)
                    .clip(panelShape)
                    .background(
                        Brush.linearGradient(
                            listOf(Color.White.copy(alpha = 0.10f), Color.White.copy(alpha = 0.03f))
                        )
                    )
                    .border(1.dp, Color.White.copy(alpha = 0.12f), panelShape)
            } else {
                Modifier.weight(1f)
            },
            beyondViewportPageCount = 1
        ) { page ->
            when (tabs[page]) {
            LiveTab.CATS -> {
                Column(Modifier.fillMaxSize()) {
                if (!pills) {
                    Text(
                        if (tr) "Kategoriler" else "Categories",
                        color = Color.White.copy(alpha = 0.55f),
                        fontSize = 12.sp,
                        modifier = Modifier.padding(horizontal = 18.dp, vertical = 8.dp)
                    )
                } else {
                    Spacer(Modifier.height(10.dp))
                }
                LazyColumn(Modifier.weight(1f).padding(horizontal = 8.dp, vertical = if (pills) 0.dp else 8.dp), verticalArrangement = Arrangement.spacedBy(if (pills) 10.dp else 6.dp)) {
                    item {
                        LiveCatRow(if (tr) "Tüm kanallar" else "All channels", allCount, selectedCategory.isEmpty(), pills = pills) {
                            onCategory(""); go(LiveTab.CHANS)
                        }
                    }
                    item {
                        LiveCatRow(if (tr) "Favoriler" else "Favorites", favCount, selectedCategory == "fav", heart = true, pills = pills) {
                            onCategory("fav"); go(LiveTab.CHANS)
                        }
                    }
                    item {
                        LiveCatRow(if (tr) "Son İzlenenler" else "Recently watched", recent.size, selectedCategory == "recent", history = true, pills = pills) {
                            onCategory("recent"); go(LiveTab.CHANS)
                        }
                    }
                    items(categories, key = { it.id }) { cat ->
                        LiveCatRow(cat.name, categoryCounts[cat.id] ?: 0, selectedCategory == cat.id, pills = pills) {
                            onCategory(cat.id); go(LiveTab.CHANS)
                        }
                    }
                    item { Spacer(Modifier.height(24.dp)) }
                }
                }
            }
            LiveTab.CHANS -> {
                Column(Modifier.fillMaxSize()) {
                if (pills) {
                    Text(
                        if (tr) "Kanallar" else "Channels",
                        color = Color.White.copy(alpha = 0.55f),
                        fontSize = 12.sp,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)
                    )
                } else {
                    Row(
                        Modifier.fillMaxWidth().padding(start = 12.dp, end = 8.dp, top = 10.dp, bottom = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Filled.Category, null, tint = Color.White.copy(alpha = 0.7f), modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(
                            catName,
                            color = Color.White.copy(alpha = 0.85f),
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            modifier = Modifier.weight(1f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            "${channels.size} ${if (tr) "kanal" else "ch"}",
                            color = Color.White.copy(alpha = 0.55f),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(Modifier.width(6.dp))
                        Icon(
                            Icons.Filled.SwapHoriz,
                            null,
                            tint = Color.White.copy(alpha = 0.85f),
                            modifier = Modifier
                                .size(30.dp)
                                .padding(6.dp)
                                .rayClickable(onClick = { go(LiveTab.CATS) })
                        )
                    }
                }
                val timeFmt = remember { java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault()) }
                LazyColumn(Modifier.weight(1f).padding(horizontal = if (pills) 12.dp else 8.dp), verticalArrangement = Arrangement.spacedBy(if (pills) 10.dp else 6.dp)) {
                    items(channels, key = { it.id }) { ch ->
                        val name = if (stripPrefix) ch.name.substringAfter(':').trim().ifBlank { ch.name } else ch.name
                        val epg = nowByChannel[ch.id]
                        val nowStr = if (epg != null && epg.title.isNotBlank()) {
                            val time = if (epg.startMs > 0L) " " + timeFmt.format(java.util.Date(epg.startMs)) else ""
                            "${epg.title}$time"
                        } else ""
                        LiveChannelRow(
                            name = name,
                            logo = ch.logo,
                            now = nowStr,
                            playing = ch.id == playingId,
                            onClick = { onPlay(ch) },
                            pills = pills,
                            number = ch.number
                        )
                    }
                    item {
                        Spacer(Modifier.height(24.dp))
                        LaunchedEffect(channels.size) { onLoadMore() }
                    }
                }
                }
            }
            LiveTab.EPG -> {
                Column(Modifier.fillMaxSize()) {
                    // ── Gün Seçici (7 gün geri, Mina IPTV EpgDaySelector gibi) ──
                    if (onArchiveDayChange != null) {
                        val dayOffsets = (-7..0).toList()
                        val fmt = remember { java.text.SimpleDateFormat("EEE d MMM", java.util.Locale.getDefault()) }
                        androidx.compose.foundation.lazy.LazyRow(
                            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 10.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(dayOffsets) { offset ->
                                val selected = offset == archiveDayOffset
                                val label = when (offset) {
                                    0 -> if (tr) "Bugün" else "Today"
                                    -1 -> if (tr) "Dün" else "Yesterday"
                                    else -> {
                                        val d = java.util.Calendar.getInstance().apply { add(java.util.Calendar.DAY_OF_YEAR, offset) }.time
                                        fmt.format(d)
                                    }
                                }
                                Box(
                                    Modifier
                                        .clip(RoundedCornerShape(20.dp))
                                        .background(
                                            if (selected) MobileCyan
                                            else Color.White.copy(alpha = 0.10f)
                                        )
                                        .then(
                                            if (!selected) Modifier.border(1.dp, Color.White.copy(alpha = 0.18f), RoundedCornerShape(20.dp))
                                            else Modifier
                                        )
                                        .rayClickable(onClick = { onArchiveDayChange(offset) })
                                        .padding(horizontal = 14.dp, vertical = 8.dp)
                                ) {
                                    Text(
                                        label,
                                        color = if (selected) Color.Black else Color.White,
                                        fontWeight = if (selected) FontWeight.ExtraBold else FontWeight.SemiBold,
                                        fontSize = 13.sp
                                    )
                                }
                            }
                        }
                        Box(Modifier.fillMaxWidth().height(1.dp).padding(horizontal = 12.dp).background(Color.White.copy(alpha = 0.08f)))
                    }
                    // ── Program Listesi ──
                    if (dayProgrammes.isNotEmpty()) {
                        val clock = System.currentTimeMillis()
                        val fmt = remember { SimpleDateFormat("HH:mm", Locale.getDefault()) }
                        LazyColumn(
                            Modifier.weight(1f).padding(horizontal = 12.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            contentPadding = androidx.compose.foundation.layout.PaddingValues(top = 8.dp, bottom = 24.dp)
                        ) {
                            items(dayProgrammes, key = { it.id }) { p ->
                                val liveNow = p.startMs <= clock && p.endMs > clock
                                val past = p.endMs <= clock
                                val canPlay = liveNow || (past && p.hasCatchup)
                                Row(
                                    Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(16.dp))
                                        .background(
                                            when {
                                                liveNow -> MobileCyan.copy(alpha = 0.15f)
                                                past && p.hasCatchup -> Color(0xFF6A1B9A).copy(alpha = 0.15f)
                                                else -> MobileCard
                                            }
                                        )
                                        .then(
                                            when {
                                                liveNow -> Modifier.border(1.5.dp, MobileCyan, RoundedCornerShape(16.dp))
                                                past && p.hasCatchup -> Modifier.border(1.dp, Color(0xFF9C27B0).copy(alpha = 0.5f), RoundedCornerShape(16.dp))
                                                else -> Modifier
                                            }
                                        )
                                        .rayClickable(onClick = { if (canPlay) onProgramme?.invoke(p) })
                                        .padding(14.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(Modifier.weight(1f)) {
                                        Text(
                                            "${fmt.format(Date(p.startMs))} - ${fmt.format(Date(p.endMs))}",
                                            color = Color.White.copy(alpha = 0.55f),
                                            fontSize = 12.sp
                                        )
                                        Spacer(Modifier.height(4.dp))
                                        Text(
                                            p.title.ifBlank { if (tr) "Program yok" else "No programme" },
                                            color = Color.White,
                                            fontWeight = FontWeight.Bold,
                                            maxLines = 2,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        if (p.plot.isNotBlank()) {
                                            Spacer(Modifier.height(4.dp))
                                            Text(
                                                p.plot,
                                                color = Color.White.copy(alpha = 0.5f),
                                                fontSize = 12.sp,
                                                maxLines = 2,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                        if (liveNow && p.endMs > p.startMs) {
                                            val progress = ((clock - p.startMs).toFloat() / (p.endMs - p.startMs).toFloat()).coerceIn(0f, 1f)
                                            Spacer(Modifier.height(8.dp))
                                            Box(
                                                Modifier.fillMaxWidth().height(3.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.15f))
                                            ) {
                                                Box(
                                                    Modifier.fillMaxWidth(progress).fillMaxHeight().clip(CircleShape).background(MobileCyan)
                                                )
                                            }
                                        }
                                    }
                                    Spacer(Modifier.width(10.dp))
                                    when {
                                        liveNow -> MobileBadge(if (tr) "CANLI" else "LIVE", MobileLiveRed)
                                        past && p.hasCatchup -> androidx.compose.foundation.layout.Column(
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                            verticalArrangement = Arrangement.spacedBy(2.dp)
                                        ) {
                                            Icon(Icons.Filled.Replay, null, tint = Color(0xFFCE93D8), modifier = Modifier.size(22.dp))
                                            Text(if (tr) "Tekrar" else "Replay", color = Color(0xFFCE93D8), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                        }
                                        past -> Icon(Icons.Filled.History, null, tint = Color.White.copy(alpha = 0.3f), modifier = Modifier.size(18.dp))
                                    }
                                }
                            }
                        }
                    } else {
                        // Kanal bazlı EPG özet listesi (gün seçici yoksa / veri yoksa)
                        val clock = remember { System.currentTimeMillis() }
                        val fmt = remember { SimpleDateFormat("HH:mm", Locale.getDefault()) }
                        LazyColumn(
                            Modifier.weight(1f).padding(horizontal = 12.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            contentPadding = androidx.compose.foundation.layout.PaddingValues(top = 8.dp, bottom = 24.dp)
                        ) {
                            items(channels, key = { it.id }) { ch ->
                                val epg = nowByChannel[ch.id]
                                val isPlaying = ch.id == playingId
                                Row(
                                    Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(14.dp))
                                        .background(if (isPlaying) MobileCyan.copy(alpha = 0.18f) else MobileCard)
                                        .border(
                                            if (isPlaying) 1.5.dp else 1.dp,
                                            if (isPlaying) MobileCyan.copy(alpha = 0.6f) else Color.White.copy(alpha = 0.10f),
                                            RoundedCornerShape(14.dp)
                                        )
                                        .rayClickable(onClick = { onPlay(ch) })
                                        .padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    AsyncImage(
                                        ch.logo, null,
                                        Modifier.size(46.dp).clip(RoundedCornerShape(10.dp)).background(Color(0xFF14181F)),
                                        contentScale = ContentScale.Fit
                                    )
                                    Spacer(Modifier.width(12.dp))
                                    Column(Modifier.weight(1f)) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(
                                                ch.name,
                                                color = Color.White,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 14.sp,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis,
                                                modifier = Modifier.weight(1f)
                                            )
                                            if (epg != null && epg.startMs > 0 && epg.endMs > 0) {
                                                Text(
                                                    "${fmt.format(Date(epg.startMs))} - ${fmt.format(Date(epg.endMs))}",
                                                    color = MobileCyan,
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.SemiBold
                                                )
                                            }
                                        }
                                        Spacer(Modifier.height(4.dp))
                                        if (epg != null && epg.title.isNotBlank()) {
                                            Text(
                                                epg.title,
                                                color = Color.White.copy(alpha = 0.88f),
                                                fontWeight = FontWeight.Medium,
                                                fontSize = 13.sp,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            if (epg.plot.isNotBlank()) {
                                                Text(
                                                    epg.plot,
                                                    color = Color.White.copy(alpha = 0.5f),
                                                    fontSize = 11.sp,
                                                    maxLines = 2,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                            }
                                            if (epg.startMs <= clock && epg.endMs > clock && epg.endMs > epg.startMs) {
                                                val progress = ((clock - epg.startMs).toFloat() / (epg.endMs - epg.startMs).toFloat()).coerceIn(0f, 1f)
                                                Spacer(Modifier.height(6.dp))
                                                Box(
                                                    Modifier.fillMaxWidth().height(3.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.15f))
                                                ) {
                                                    Box(
                                                        Modifier.fillMaxWidth(progress).fillMaxHeight().clip(CircleShape).background(MobileCyan)
                                                    )
                                                }
                                            }
                                        } else {
                                            Text(
                                                if (tr) "EPG rehber bilgisi mevcut değil" else "No EPG data available",
                                                color = Color.White.copy(alpha = 0.45f),
                                                fontSize = 12.sp
                                            )
                                        }
                                    }
                                    Spacer(Modifier.width(8.dp))
                                    Icon(
                                        Icons.Filled.PlayArrow,
                                        null,
                                        tint = if (isPlaying) MobileCyan else Color.White.copy(alpha = 0.6f),
                                        modifier = Modifier.size(22.dp)
                                    )
                                }
                            }
                            item { Spacer(Modifier.height(24.dp)) }
                        }
                    }
                }
            }
            }
        }
    }
}

@Composable
private fun MobileLiveTabs(tr: Boolean, tab: LiveTab, pills: Boolean, onTab: (LiveTab) -> Unit) {
    val tabs = listOf(
        LiveTab.CATS to if (tr) "Kategoriler" else "Categories",
        LiveTab.CHANS to if (tr) "Kanallar" else "Channels",
        LiveTab.EPG to "EPG"
    )
    if (pills) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(Color.White.copy(alpha = 0.06f))
                .border(1.dp, Color.White.copy(alpha = 0.12f), RoundedCornerShape(16.dp))
                .padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            tabs.forEach { (id, label) ->
                val on = tab == id
                Box(
                    Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (on) MobileCyan.copy(alpha = 0.65f) else Color.Transparent)
                        .rayClickable(onClick = { onTab(id) })
                        .padding(vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(label, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }
            }
        }
    } else {
        Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 0.dp)) {
            tabs.forEach { (id, label) ->
                val on = tab == id
                Box(
                    Modifier
                        .weight(1f)
                        .rayClickable(onClick = { onTab(id) })
                        .padding(vertical = 12.dp)
                        .then(
                            if (on) Modifier.border(
                                width = 0.dp,
                                color = Color.Transparent,
                                shape = RoundedCornerShape(0.dp)
                            ) else Modifier
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            label,
                            color = if (on) MobileCyan else Color.White.copy(alpha = 0.78f),
                            fontWeight = if (on) FontWeight.Bold else FontWeight.SemiBold,
                            fontSize = 14.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(Modifier.height(8.dp))
                        Box(
                            Modifier
                                .fillMaxWidth()
                                .height(2.5.dp)
                                .background(if (on) MobileCyan else Color.Transparent)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun LiveCatRow(
    name: String,
    count: Int,
    selected: Boolean,
    heart: Boolean = false,
    history: Boolean = false,
    pills: Boolean = false,
    onClick: () -> Unit
) {
    val shape = RoundedCornerShape(if (pills) 50.dp else 12.dp)
    Row(
        Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(
                if (pills) {
                    Brush.linearGradient(
                        listOf(
                            Color.White.copy(alpha = if (selected) 0.12f else 0.085f),
                            Color.White.copy(alpha = 0.025f)
                        )
                    )
                } else {
                    Brush.linearGradient(
                        listOf(
                            if (selected) MobileCyan.copy(alpha = 0.18f) else Color.White.copy(alpha = 0.05f),
                            if (selected) MobileCyan.copy(alpha = 0.18f) else Color.White.copy(alpha = 0.05f)
                        )
                    )
                }
            )
            .then(
                if (!pills) Modifier.border(
                    if (selected) 1.2.dp else 1.dp,
                    if (selected) MobileCyan.copy(alpha = 0.55f) else Color.White.copy(alpha = 0.08f),
                    shape
                )
                else if (selected) Modifier.border(1.dp, MobileCyan, shape)
                else Modifier.border(1.dp, Color.White.copy(alpha = 0.10f), shape)
            )
            .rayClickable(onClick)
            .padding(horizontal = if (pills) 16.dp else 12.dp, vertical = if (pills) 12.dp else 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (heart) Icon(Icons.Filled.Favorite, null, tint = Color.White, modifier = Modifier.size(18.dp))
        if (history) Icon(Icons.Filled.History, null, tint = Color.White, modifier = Modifier.size(18.dp))
        if (heart || history) Spacer(Modifier.width(10.dp))
        Text(name, color = Color.White, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
        Text("$count", color = Color.White.copy(alpha = 0.7f), fontWeight = FontWeight.Bold)
        if (!pills) {
            Spacer(Modifier.width(6.dp))
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null, tint = Color.White.copy(alpha = 0.4f), modifier = Modifier.size(18.dp))
        }
    }
}

@Composable
private fun LiveChannelRow(
    name: String,
    logo: String,
    now: String,
    playing: Boolean,
    onClick: () -> Unit,
    pills: Boolean = false,
    number: Int = 0
) {
    val shape = RoundedCornerShape(16.dp)
    Row(
        Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(
                if (playing) MobileCyan.copy(alpha = 0.16f)
                else Color.White.copy(alpha = 0.05f)
            )
            .border(
                1.dp,
                if (playing) MobileCyan.copy(alpha = 0.55f) else Color.White.copy(alpha = 0.08f),
                shape
            )
            .rayClickable(onClick)
            .padding(horizontal = 14.dp, vertical = if (now.isNotBlank()) 10.dp else 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            "%03d".format(number.coerceAtLeast(0)),
            color = MobileCyan,
            fontWeight = FontWeight.ExtraBold,
            fontSize = 14.sp,
            modifier = Modifier.width(42.dp)
        )
        Column(Modifier.weight(1f)) {
            Text(
                name,
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (now.isNotBlank()) {
                Spacer(Modifier.height(3.dp))
                Text(
                    now,
                    color = Color.White.copy(alpha = 0.65f),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Normal,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
        Spacer(Modifier.width(8.dp))
        if (logo.isNotBlank()) {
            AsyncImage(
                logo,
                null,
                Modifier
                    .height(28.dp)
                    .widthIn(max = 52.dp),
                contentScale = ContentScale.Fit
            )
            Spacer(Modifier.width(10.dp))
        }
        Box(
            Modifier
                .size(34.dp)
                .clip(CircleShape)
                .background(MobileCyan),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Filled.PlayArrow,
                null,
                tint = Color.Black,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
fun MobileLiveWatchScreen(
    tr: Boolean,
    playback: Playback,
    rayPlayer: RayPlayer,
    aspect: AspectMode,
    channels: List<ChannelEntity>,
    categories: List<CategoryEntity>,
    selectedCategory: String,
    categoryCounts: Map<String, Int>,
    favorites: List<FavoriteEntity>,
    recent: List<ProgressEntity>,
    nowByChannel: Map<String, EpgEntity>,
    streamLabel: String,
    onBack: () -> Unit,
    onPlay: (ChannelEntity) -> Unit,
    onPausePlay: () -> Unit,
    onRewind: (Long) -> Unit,
    onAspect: () -> Unit,
    onFav: () -> Unit,
    favorite: Boolean,
    onCategory: (String) -> Unit,
    onLoadNowMap: (List<String>) -> Unit,
    stripPrefix: Boolean,
    playingId: String = "",
    allCount: Int = 0,
    onLoadMore: () -> Unit = {},
    nowTitle: String = "",
    nextTitle: String = "",
    dayProgrammes: List<EpgEntity> = emptyList(),
    archiveDayOffset: Int = 0,
    onArchiveDayChange: ((Int) -> Unit)? = null,
    onCatchup: (EpgEntity) -> Unit = {},
    onToggleEngine: (() -> Unit)? = null
) {
    val st by rayPlayer.state.collectAsState()
    val context = LocalContext.current
    var showQuickSheet by remember { mutableStateOf(false) }
    var showQualitySheet by remember { mutableStateOf(false) }
    var showInfoDialog by remember { mutableStateOf(false) }

    DisposableEffect(Unit) {
        val activity = context as? android.app.Activity
        activity?.window?.addFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        onDispose {
            activity?.window?.clearFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
    }

    val currentIdx = remember(channels, playingId) { channels.indexOfFirst { it.id == playingId } }
    val onPrevChannel = {
        if (channels.isNotEmpty()) {
            val idx = if (currentIdx > 0) currentIdx - 1 else channels.lastIndex
            onPlay(channels[idx])
        }
    }
    val onNextChannel = {
        if (channels.isNotEmpty()) {
            val idx = if (currentIdx >= 0 && currentIdx < channels.lastIndex) currentIdx + 1 else 0
            onPlay(channels[idx])
        }
    }

    val gestureState = com.ray.iptv.ui.player.rememberPlayerGestureState()

    Box(Modifier.fillMaxSize().background(Color.Black)) {
        Column(Modifier.fillMaxSize()) {
            Box(
                Modifier
                    .fillMaxWidth()
                    .aspectRatio(16f / 9f)
                    .then(com.ray.iptv.ui.player.playerGestureDrag(gestureState))
            ) {
                MobileVideoSurface(rayPlayer, aspect, Modifier.fillMaxSize())
                com.ray.iptv.ui.player.PlayerGlassLevelOverlay(gestureState)
            }
            Column(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 10.dp, vertical = 8.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(MobileCard)
                    .border(1.dp, Color.White.copy(alpha = 0.12f), RoundedCornerShape(16.dp))
                    .padding(14.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(playback.title, color = Color.White, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                    MobileBadge(streamLabel, Color(0xFF6B4B9A))
                    Spacer(Modifier.width(6.dp))
                    Row(
                        Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFF1565C0))
                            .rayClickable(onClick = { onToggleEngine?.invoke() })
                            .padding(horizontal = 6.dp, vertical = 3.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            if (st.engine == PlaybackEngine.MEDIA_KIT) Icons.Filled.Memory else Icons.Filled.Bolt,
                            null, tint = Color.White, modifier = Modifier.size(12.dp)
                        )
                        Spacer(Modifier.width(3.dp))
                        Text(if (st.engine == PlaybackEngine.MEDIA_KIT) "MediaKit" else "Better", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }
                    Spacer(Modifier.width(6.dp))
                    MobileBadge(if (st.videoSize.isNotBlank()) st.videoSize else "HD", Color(0xFF455A64))
                }

                Spacer(Modifier.height(14.dp))

                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Önceki Kanal (Previous Channel)
                    Icon(
                        Icons.AutoMirrored.Filled.NavigateBefore,
                        null,
                        tint = Color.White,
                        modifier = Modifier.size(28.dp).rayClickable(onClick = onPrevChannel)
                    )

                    // Durdur / Oynat (Pause / Play)
                    Box(
                        Modifier.size(44.dp).clip(CircleShape).background(MobileCyan).rayClickable(onClick = onPausePlay),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(if (st.playing) Icons.Filled.Pause else Icons.Filled.PlayArrow, null, tint = Color.White, modifier = Modifier.size(24.dp))
                    }

                    // Sonraki Kanal (Next Channel)
                    Icon(
                        Icons.AutoMirrored.Filled.NavigateNext,
                        null,
                        tint = Color.White,
                        modifier = Modifier.size(28.dp).rayClickable(onClick = onNextChannel)
                    )

                    // Hızlı Kanal Listesi Panel (Quick Channels)
                    Icon(
                        Icons.AutoMirrored.Filled.List,
                        null,
                        tint = if (showQuickSheet) MobileCyan else Color.White,
                        modifier = Modifier.size(24.dp).rayClickable(onClick = { showQuickSheet = !showQuickSheet })
                    )

                    // Favori (Favorite)
                    Icon(
                        if (favorite) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                        null,
                        tint = if (favorite) Color(0xFFFF5252) else Color.White,
                        modifier = Modifier.size(22.dp).rayClickable(onClick = onFav)
                    )

                    // HQ / Yayın Kalitesi Seçimi (Quality / Tracks Selector)
                    Icon(
                        Icons.Filled.HighQuality,
                        null,
                        tint = if (showQualitySheet) MobileCyan else Color.White,
                        modifier = Modifier.size(24.dp).rayClickable(onClick = { showQualitySheet = true })
                    )

                    // Yayın Bilgisi (Info Dialog)
                    Icon(
                        Icons.Filled.Info,
                        null,
                        tint = if (showInfoDialog) MobileCyan else Color.White,
                        modifier = Modifier.size(22.dp).rayClickable(onClick = { showInfoDialog = true })
                    )

                    // Ekran Boyutu / Aspect Ratio (Resize)
                    Icon(
                        Icons.Filled.AspectRatio,
                        null,
                        tint = if (aspect != AspectMode.FIT) MobileCyan else Color.White,
                        modifier = Modifier.size(24.dp).rayClickable(onClick = onAspect)
                    )
                }
            }
            Box(Modifier.weight(1f)) {
                MobileLiveBrowseScreen(
                    tr = tr,
                    categories = categories,
                    channels = channels,
                    allCount = allCount,
                    categoryCounts = categoryCounts,
                    favorites = favorites,
                    recent = recent,
                    selectedCategory = selectedCategory,
                    nowByChannel = nowByChannel,
                    onCategory = onCategory,
                    onPlay = onPlay,
                    onLoadNowMap = onLoadNowMap,
                    stripPrefix = stripPrefix,
                    playingId = playingId,
                    onLoadMore = onLoadMore,
                    startOnChannels = true,
                    dayProgrammes = dayProgrammes,
                    archiveDayOffset = archiveDayOffset,
                    onArchiveDayChange = onArchiveDayChange,
                    onProgramme = onCatchup
                )
            }
        }

        // Hızlı Kanal Paneli Modal (Quick Channel Sheet)
        if (showQuickSheet) {
            Box(
                Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.65f))
                    .clickable { showQuickSheet = false },
                contentAlignment = Alignment.BottomCenter
            ) {
                Column(
                    Modifier
                        .fillMaxWidth()
                        .fillMaxHeight(0.6f)
                        .clip(RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp))
                        .background(MobileCard)
                        .clickable(enabled = false) {}
                        .padding(14.dp)
                ) {
                    Row(
                        Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            if (tr) "Hızlı Kanal Listesi (${channels.size})" else "Quick Channel List (${channels.size})",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                        Text(
                            if (tr) "Kapat" else "Close",
                            color = MobileCyan,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.rayClickable(onClick = { showQuickSheet = false })
                        )
                    }
                    Spacer(Modifier.height(10.dp))
                    LazyColumn(Modifier.fillMaxSize()) {
                        items(channels, key = { it.id }) { ch ->
                            val isCurrent = ch.id == playingId
                            Row(
                                Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(if (isCurrent) MobileCyan.copy(alpha = 0.2f) else Color.Transparent)
                                    .rayClickable(onClick = {
                                        onPlay(ch)
                                        showQuickSheet = false
                                    })
                                    .padding(horizontal = 10.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    "${ch.number}.",
                                    color = if (isCurrent) MobileCyan else Color.White.copy(alpha = 0.5f),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    ch.name,
                                    color = if (isCurrent) MobileCyan else Color.White,
                                    fontSize = 14.sp,
                                    fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal,
                                    modifier = Modifier.weight(1f),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                if (isCurrent) {
                                    Icon(Icons.Filled.PlayArrow, null, tint = MobileCyan, modifier = Modifier.size(18.dp))
                                }
                            }
                        }
                    }
                }
            }
        }

        // HQ Kalite / Çözünürlük Paneli Modal (Quality Sheet)
        if (showQualitySheet) {
            Box(
                Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.65f))
                    .clickable { showQualitySheet = false },
                contentAlignment = Alignment.BottomCenter
            ) {
                Column(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp))
                        .background(MobileCard)
                        .clickable(enabled = false) {}
                        .padding(16.dp)
                ) {
                    Row(
                        Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            if (tr) "Yayın Kalitesi & Çözünürlük" else "Stream Quality & Tracks",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                        Text(
                            if (tr) "Kapat" else "Close",
                            color = MobileCyan,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.rayClickable(onClick = { showQualitySheet = false })
                        )
                    }
                    Spacer(Modifier.height(14.dp))
                    if (st.videoTracks.isNotEmpty()) {
                        Text(
                            if (tr) "Mevcut Video Kalite Seviyeleri:" else "Available Video Track Qualities:",
                            color = Color.White.copy(alpha = 0.7f),
                            fontSize = 13.sp
                        )
                        Spacer(Modifier.height(8.dp))
                        st.videoTracks.forEach { track ->
                            Row(
                                Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(if (track.selected) MobileCyan.copy(alpha = 0.25f) else Color.White.copy(alpha = 0.05f))
                                    .rayClickable(onClick = {
                                        rayPlayer.selectVideo(track.id)
                                        showQualitySheet = false
                                    })
                                    .padding(horizontal = 12.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(track.label, color = if (track.selected) MobileCyan else Color.White, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                                if (track.selected) {
                                    MobileBadge("Aktif", MobileCyan)
                                }
                            }
                        }
                    } else {
                        Column(
                            Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color.White.copy(alpha = 0.05f))
                                .padding(12.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(if (tr) "Aktif Çözünürlük:" else "Active Resolution:", color = Color.White.copy(alpha = 0.7f), fontSize = 13.sp)
                                Spacer(Modifier.width(8.dp))
                                MobileBadge(if (st.videoSize.isNotBlank()) st.videoSize else "Otomatik (HD/FHD)", MobileCyan)
                            }
                            Spacer(Modifier.height(8.dp))
                            Text(
                                if (tr) "Canlı yayının kalitesi sunucu kaynağından dinamik (Auto HD/FHD) olarak aktarılmaktadır."
                                else "Live stream quality is streamed dynamically from the server (Auto HD/FHD).",
                                color = Color.White.copy(alpha = 0.6f),
                                fontSize = 12.sp
                            )
                        }
                    }
                    Spacer(Modifier.height(10.dp))
                }
            }
        }

        // Yayın Bilgisi Popup Modal (Info Dialog)
        if (showInfoDialog) {
            Box(
                Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.75f))
                    .clickable { showInfoDialog = false },
                contentAlignment = Alignment.Center
            ) {
                Column(
                    Modifier
                        .fillMaxWidth(0.92f)
                        .clip(RoundedCornerShape(20.dp))
                        .background(MobileCard)
                        .border(1.dp, Color.White.copy(alpha = 0.15f), RoundedCornerShape(20.dp))
                        .clickable(enabled = false) {}
                        .padding(18.dp)
                ) {
                    Row(
                        Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.Info, null, tint = MobileCyan, modifier = Modifier.size(22.dp))
                            Spacer(Modifier.width(8.dp))
                            Text(if (tr) "Yayın Bilgileri" else "Stream Information", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 17.sp)
                        }
                        Text(
                            if (tr) "Kapat" else "Close",
                            color = MobileCyan,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.rayClickable(onClick = { showInfoDialog = false })
                        )
                    }
                    Spacer(Modifier.height(14.dp))
                    InfoRow(if (tr) "Kanal Adı" else "Channel Name", playback.title)
                    InfoRow(if (tr) "Kategori" else "Category", playback.subtitle.ifBlank { "Genel" })
                    InfoRow(if (tr) "Çözünürlük" else "Resolution", if (st.videoSize.isNotBlank()) st.videoSize else "Otomatik (HD/FHD)")
                    InfoRow(if (tr) "Oynatıcı Motoru" else "Player Engine", if (st.engine == PlaybackEngine.MEDIA_KIT) "MediaKit (libmpv)" else "ExoPlayer (Better)")
                    InfoRow(if (tr) "Akış Formatı" else "Stream Format", streamLabel)
                    if (nowTitle.isNotBlank()) InfoRow(if (tr) "Şimdi Yayımlanan" else "Now Playing", nowTitle)
                    if (nextTitle.isNotBlank()) InfoRow(if (tr) "Sıradaki Yayın" else "Next Up", nextTitle)
                    InfoRow(if (tr) "Yayın Bağlantısı" else "Stream URL", playback.url)
                }
            }
        }
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Column(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Text(label, color = Color.White.copy(alpha = 0.55f), fontSize = 11.sp)
        Text(
            value,
            color = Color.White,
            fontWeight = FontWeight.Medium,
            fontSize = 13.sp,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@OptIn(UnstableApi::class)
@Composable
fun MobileVideoSurface(
    rayPlayer: RayPlayer,
    aspect: AspectMode,
    modifier: Modifier = Modifier,
    subtitleSize: Int = 22,
    subtitleOutline: Boolean = true,
    subtitleColor: String = "white",
    subtitleFont: String = "sans"
) {
    val st by rayPlayer.state.collectAsState()
    val resize = when (aspect) {
        AspectMode.FIT -> AspectRatioFrameLayout.RESIZE_MODE_FIT
        AspectMode.ZOOM -> AspectRatioFrameLayout.RESIZE_MODE_ZOOM
        AspectMode.FILL, AspectMode.STRETCH -> AspectRatioFrameLayout.RESIZE_MODE_FILL
    }
    if (st.engine == PlaybackEngine.MEDIA_KIT) {
        MediaKitComposeSurface(player = rayPlayer, aspect = aspect, modifier = modifier)
    } else {
        AndroidView(
            factory = { ctx ->
                PlayerView(ctx).apply {
                    useController = false
                    player = rayPlayer.exo
                    resizeMode = resize
                }
            },
            update = { view ->
                view.player = rayPlayer.exo
                view.resizeMode = resize
                view.subtitleView?.setFixedTextSize(android.util.TypedValue.COMPLEX_UNIT_SP, subtitleSize.toFloat())
                view.subtitleView?.setStyle(
                    androidx.media3.ui.CaptionStyleCompat(
                        when (subtitleColor.lowercase()) {
                            "yellow" -> android.graphics.Color.YELLOW
                            "cyan" -> android.graphics.Color.CYAN
                            "green" -> android.graphics.Color.GREEN
                            "orange" -> android.graphics.Color.rgb(255, 152, 0)
                            "pink" -> android.graphics.Color.MAGENTA
                            else -> android.graphics.Color.WHITE
                        },
                        android.graphics.Color.TRANSPARENT,
                        android.graphics.Color.TRANSPARENT,
                        if (subtitleOutline) androidx.media3.ui.CaptionStyleCompat.EDGE_TYPE_OUTLINE
                        else androidx.media3.ui.CaptionStyleCompat.EDGE_TYPE_NONE,
                        android.graphics.Color.BLACK,
                        when (subtitleFont) {
                            "serif" -> android.graphics.Typeface.SERIF
                            "mono" -> android.graphics.Typeface.MONOSPACE
                            else -> android.graphics.Typeface.SANS_SERIF
                        }
                    )
                )
            },
            modifier = modifier
        )
    }
}
