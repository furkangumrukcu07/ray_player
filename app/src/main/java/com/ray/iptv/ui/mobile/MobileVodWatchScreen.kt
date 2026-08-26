package com.ray.iptv.ui.mobile

import androidx.compose.foundation.background
import com.ray.iptv.ui.player.playerGestureDrag
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.material.icons.filled.HighQuality
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.foundation.layout.offset
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AspectRatio
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.ClosedCaption
import androidx.compose.material.icons.filled.FastForward
import androidx.compose.material.icons.filled.FastRewind
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import kotlinx.coroutines.delay
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
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
import com.ray.iptv.data.repo.AspectMode
import com.ray.iptv.data.repo.PlaybackEngine
import com.ray.iptv.player.RayPlayer
import com.ray.iptv.ui.NextUpPrompt
import com.ray.iptv.ui.Playback
import com.ray.iptv.ui.RayViewModel
import com.ray.iptv.ui.input.rayClickable

private enum class VodPanelTab { CATS, ITEMS }
private enum class SeriesPanelTab { INFO, EPISODES }

@Composable
fun MobileVodWatchScreen(
    tr: Boolean,
    vm: RayViewModel,
    playback: Playback,
    rayPlayer: RayPlayer,
    aspect: AspectMode,
    movies: List<VodEntity>,
    movieCats: List<CategoryEntity>,
    favorites: List<FavoriteEntity>,
    continueWatching: List<ProgressEntity>,
    extras: VodMeta,
    selectedSeries: VodEntity?,
    episodes: List<EpisodeEntity>,
    pendingNext: NextUpPrompt?,
    onBack: () -> Unit,
    onPausePlay: () -> Unit,
    onAspect: () -> Unit,
    onFav: () -> Unit,
    favorite: Boolean,
    onPlayVod: (VodEntity) -> Unit,
    onPlayEpisode: (EpisodeEntity, VodEntity) -> Unit,
    onSkipIntro: () -> Unit,
    onConfirmNext: () -> Unit,
    onCancelNext: () -> Unit,
    introTargetMs: Long
) {
    var st by remember(playback.url) { mutableStateOf(rayPlayer.snapshot()) }
    LaunchedEffect(playback.url) {
        while (true) {
            st = rayPlayer.snapshot()
            delay(400)
        }
    }
    val settings by vm.settings.collectAsState()
    val seriesMode = playback.kind == "EPISODE" || playback.kind == "SERIES"
    var seriesItem by remember(playback.seriesId, selectedSeries?.id) { mutableStateOf(selectedSeries) }
    var seriesEps by remember(playback.seriesId) { mutableStateOf(episodes) }
    var subSheet by remember { mutableStateOf(false) }
    var showQualitySheet by remember { mutableStateOf(false) }
    LaunchedEffect(playback.seriesId, playback.kind) {
        if (!seriesMode || playback.seriesId.isBlank()) return@LaunchedEffect
        if (seriesItem?.id != playback.seriesId) {
            seriesItem = vm.seriesOf(playback.seriesId)
        }
        if (seriesEps.isEmpty() || seriesEps.firstOrNull()?.seriesId != playback.seriesId) {
            seriesEps = vm.episodesOf(playback.seriesId)
        }
    }
    LaunchedEffect(episodes, selectedSeries?.id) {
        if (selectedSeries?.id == playback.seriesId && episodes.isNotEmpty()) seriesEps = episodes
        if (selectedSeries?.id == playback.seriesId) seriesItem = selectedSeries
    }
    val skipMs = introTargetMs
    val showSkip = seriesMode && skipMs > 4_000L && st.position in 4_000L until skipMs && pendingNext == null
    val gestureState = com.ray.iptv.ui.player.rememberPlayerGestureState()

    Box(Modifier.fillMaxSize().background(Color.Black)) {
    Column(Modifier.fillMaxSize()) {
        Box(
            Modifier
                .fillMaxWidth()
                .aspectRatio(16f / 9f)
                .then(com.ray.iptv.ui.player.playerGestureDrag(gestureState))
        ) {
            MobileVideoSurface(
                rayPlayer,
                aspect,
                Modifier.fillMaxSize(),
                subtitleSize = settings.subtitleSize,
                subtitleOutline = settings.subtitleOutline,
                subtitleColor = settings.subtitleColor,
                subtitleFont = settings.subtitleFont
            )
            com.ray.iptv.ui.player.PlayerGlassLevelOverlay(gestureState)
            Icon(
                Icons.AutoMirrored.Filled.ArrowBack,
                null,
                tint = Color.White,
                modifier = Modifier
                    .padding(10.dp)
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.5f))
                    .rayClickable(onClick = onBack)
                    .padding(6.dp)
            )
            if (playback.poster.isNotBlank()) {
                AsyncImage(
                    playback.poster, null,
                    Modifier.align(Alignment.TopEnd).padding(10.dp).size(42.dp).clip(RoundedCornerShape(8.dp)),
                    contentScale = ContentScale.Fit
                )
            }
            if (showSkip) {
                Box(
                    Modifier
                        .align(Alignment.BottomEnd)
                        .padding(12.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color.Black.copy(alpha = 0.72f))
                        .border(1.dp, MobileCyan, RoundedCornerShape(12.dp))
                        .rayClickable(onClick = { rayPlayer.seek(skipMs) })
                        .padding(horizontal = 14.dp, vertical = 8.dp)
                ) {
                    Text(if (tr) "Jeneriği Atla" else "Skip Intro", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }
            }
            pendingNext?.let { next ->
                Column(
                    Modifier
                        .align(Alignment.BottomCenter)
                        .padding(12.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(Color.Black.copy(alpha = 0.78f))
                        .padding(12.dp)
                ) {
                    Text(if (tr) "Sıradaki bölüm" else "Next episode", color = MobileCyan, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    Text(next.title, color = Color.White, maxLines = 2, overflow = TextOverflow.Ellipsis, fontSize = 13.sp)
                    Spacer(Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            if (tr) "Oynat" else "Play",
                            color = Color.Black,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.clip(RoundedCornerShape(8.dp)).background(MobileCyan).rayClickable(onConfirmNext).padding(horizontal = 12.dp, vertical = 6.dp)
                        )
                        Text(
                            if (tr) "Vazgeç" else "Cancel",
                            color = Color.White,
                            modifier = Modifier.clip(RoundedCornerShape(8.dp)).background(Color.White.copy(alpha = 0.16f)).rayClickable(onCancelNext).padding(horizontal = 12.dp, vertical = 6.dp)
                        )
                    }
                }
            }
        }
        Column(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 8.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(MobileCard)
                .padding(12.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(playback.title, color = Color.White, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    if (playback.subtitle.isNotBlank()) {
                        Text(playback.subtitle, color = Color.White.copy(alpha = 0.65f), fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }
                Row(
                    Modifier.clip(RoundedCornerShape(8.dp)).background(Color(0xFF1565C0)).padding(horizontal = 6.dp, vertical = 3.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        if (st.engine == PlaybackEngine.MEDIA_KIT) Icons.Filled.Memory else Icons.Filled.Bolt,
                        null, tint = Color.White, modifier = Modifier.size(12.dp)
                    )
                    Spacer(Modifier.width(3.dp))
                    Text(if (st.engine == PlaybackEngine.MEDIA_KIT) "MediaKit" else "Better", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                }
            }
            Spacer(Modifier.height(8.dp))
            VodSeekBar(
                position = st.position,
                duration = st.duration,
                onSeek = { rayPlayer.seek(it) }
            )
            Spacer(Modifier.height(10.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.FastRewind, null, tint = Color.White, modifier = Modifier.size(28.dp).rayClickable(onClick = { rayPlayer.seekBy(-10_000) }))
                Box(
                    Modifier.size(52.dp).clip(CircleShape).background(MobileCyan).rayClickable(onPausePlay),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(if (st.playing) Icons.Filled.Pause else Icons.Filled.PlayArrow, null, tint = Color.White, modifier = Modifier.size(28.dp))
                }
                Icon(Icons.Filled.FastForward, null, tint = Color.White, modifier = Modifier.size(28.dp).rayClickable(onClick = { rayPlayer.seekBy(10_000) }))
                Icon(Icons.Filled.HighQuality, null, tint = if (showQualitySheet) MobileCyan else Color.White, modifier = Modifier.size(22.dp).rayClickable(onClick = { showQualitySheet = true }))
                Icon(Icons.Filled.ClosedCaption, null, tint = Color.White, modifier = Modifier.size(22.dp).rayClickable(onClick = { subSheet = true }))
                Icon(Icons.Filled.AspectRatio, null, tint = Color.White, modifier = Modifier.size(22.dp).rayClickable(onAspect))
                Icon(
                    if (favorite) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                    null,
                    tint = if (favorite) Color(0xFFFF5252) else Color.White,
                    modifier = Modifier.size(22.dp).rayClickable(onFav)
                )
            }
        }
        Box(Modifier.weight(1f).padding(horizontal = 8.dp, vertical = 4.dp)) {
            if (seriesMode) {
                PortraitSeriesPanel(
                    tr = tr,
                    extras = extras,
                    series = seriesItem,
                    episodes = seriesEps,
                    playingId = playback.mediaId,
                    onActorClick = { name -> vm.openActorProfileByName(name) },
                    onPlayEpisode = { ep ->
                        val s = seriesItem ?: return@PortraitSeriesPanel
                        onPlayEpisode(ep, s)
                    }
                )
            } else {
                PortraitVodPanel(
                    tr = tr,
                    vm = vm,
                    cats = movieCats,
                    seedItems = movies,
                    continueWatching = continueWatching.filter { it.kind == "MOVIE" },
                    favorites = favorites,
                    playingId = playback.mediaId,
                    kind = "MOVIE",
                    onPlay = onPlayVod
                )
            }
        }
    }
    if (subSheet) {
        val off = if (tr) "Kapalı" else "Off"
        val tracks = listOf(
            com.ray.iptv.player.TrackOption("no", off, st.textTracks.none { it.selected })
        ) + st.textTracks
        Box(
            Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.55f))
                .rayClickable(onClick = { subSheet = false })
        ) {
            Column(
                Modifier
                    .align(Alignment.Center)
                    .padding(24.dp)
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(MobileCard)
                    .padding(16.dp)
            ) {
                Text(if (tr) "Altyazı" else "Subtitles", color = Color.White, fontWeight = FontWeight.ExtraBold, fontSize = 18.sp)
                Spacer(Modifier.height(10.dp))
                tracks.forEach { track ->
                    Text(
                        track.label,
                        color = Color.White,
                        fontWeight = if (track.selected) FontWeight.Bold else FontWeight.Medium,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (track.selected) MobileCyan.copy(alpha = 0.35f) else Color.White.copy(alpha = 0.08f))
                            .rayClickable(onClick = {
                                vm.pickSubtitle(track.id)
                                subSheet = false
                            })
                            .padding(horizontal = 12.dp, vertical = 10.dp)
                    )
                    Spacer(Modifier.height(6.dp))
                }
            }
        }
    }

    if (showQualitySheet) {
        Box(
            Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.65f))
                .rayClickable(onClick = { showQualitySheet = false }),
            contentAlignment = Alignment.BottomCenter
        ) {
            Column(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp))
                    .background(MobileCard)
                    .rayClickable(onClick = {})
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
                        if (tr) "Mevcut Video Kaliteleri:" else "Available Video Qualities:",
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
                                Text("Aktif", color = MobileCyan, fontWeight = FontWeight.Bold, fontSize = 12.sp)
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
                            Text(if (st.videoSize.isNotBlank()) st.videoSize else "Otomatik (HD/FHD)", color = MobileCyan, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }
                        Spacer(Modifier.height(6.dp))
                        Text(
                            if (tr) "VOD yayın kalitesi sunucu kaynağından dinamik (Auto HD/FHD) olarak aktarılmaktadır."
                            else "VOD stream quality is streamed dynamically from the server (Auto HD/FHD).",
                            color = Color.White.copy(alpha = 0.6f),
                            fontSize = 12.sp
                        )
                    }
                }
                Spacer(Modifier.height(10.dp))
            }
        }
    }
    }
}

@Composable
private fun VodSeekBar(position: Long, duration: Long, onSeek: (Long) -> Unit) {
    var draggingPos by remember { mutableStateOf<Long?>(null) }
    val displayPos = draggingPos ?: position
    val frac = if (duration > 0) (displayPos.toFloat() / duration.toFloat()).coerceIn(0f, 1f) else 0f

    Column {
        BoxWithConstraints(
            Modifier
                .fillMaxWidth()
                .height(36.dp)
                .pointerInput(duration) {
                    detectHorizontalDragGestures(
                        onDragStart = { offset ->
                            if (duration > 0) {
                                draggingPos = ((offset.x / size.width.toFloat()) * duration).toLong().coerceIn(0L, duration)
                            }
                        },
                        onDragEnd = {
                            draggingPos?.let { onSeek(it) }
                            draggingPos = null
                        },
                        onDragCancel = {
                            draggingPos = null
                        },
                        onHorizontalDrag = { change, dragAmount ->
                            change.consume()
                            if (duration > 0) {
                                val currentMs = draggingPos ?: position
                                val deltaMs = ((dragAmount / size.width.toFloat()) * duration).toLong()
                                draggingPos = (currentMs + deltaMs).coerceIn(0L, duration)
                            }
                        }
                    )
                }
                .pointerInput(duration) {
                    detectTapGestures(
                        onTap = { offset ->
                            if (duration > 0) {
                                val targetMs = ((offset.x / size.width.toFloat()) * duration).toLong().coerceIn(0L, duration)
                                onSeek(targetMs)
                            }
                        }
                    )
                },
            contentAlignment = Alignment.CenterStart
        ) {
            val totalWidthPx = constraints.maxWidth.toFloat()
            val density = LocalDensity.current
            val thumbOffsetDp = with(density) { (frac * totalWidthPx).toDp() }

            // Track Background
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(Color.White.copy(alpha = 0.22f))
            )
            // Progress Bar
            Box(
                Modifier
                    .fillMaxWidth(frac)
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(MobileCyan)
            )
            // Thumb Handle
            if (duration > 0) {
                Box(
                    Modifier
                        .offset(x = (thumbOffsetDp - 8.dp).coerceAtLeast(0.dp))
                        .size(16.dp)
                        .clip(CircleShape)
                        .background(Color.White)
                        .border(2.dp, MobileCyan, CircleShape)
                )
            }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(
                fmtMs(displayPos),
                color = if (draggingPos != null) MobileCyan else Color.White.copy(alpha = 0.7f),
                fontSize = 11.sp,
                fontWeight = if (draggingPos != null) FontWeight.Bold else FontWeight.Normal
            )
            Text(fmtMs(duration), color = Color.White.copy(alpha = 0.7f), fontSize = 11.sp)
        }
    }
}

private fun fmtMs(ms: Long): String {
    val total = (ms / 1000).coerceAtLeast(0)
    val h = total / 3600
    val m = (total % 3600) / 60
    val s = total % 60
    return if (h > 0) "%d:%02d:%02d".format(h, m, s) else "%d:%02d".format(m, s)
}

@Composable
private fun PortraitVodPanel(
    tr: Boolean,
    vm: RayViewModel,
    cats: List<CategoryEntity>,
    seedItems: List<VodEntity>,
    continueWatching: List<ProgressEntity>,
    favorites: List<FavoriteEntity>,
    playingId: String,
    kind: String,
    onPlay: (VodEntity) -> Unit
) {
    var tab by remember { mutableStateOf(VodPanelTab.ITEMS) }
    var catId by remember { mutableStateOf("last50") }
    var items by remember { mutableStateOf(seedItems) }
    LaunchedEffect(catId, seedItems) {
        items = when (catId) {
            "recent" -> {
                val ids = continueWatching.map { it.mediaId }
                val map = seedItems.associateBy { it.id }
                val missing = ids.filter { it !in map }
                val extra = if (missing.isEmpty()) emptyList() else vm.showcaseCategory(kind, "last50", 50)
                val all = map + extra.associateBy { it.id }
                ids.mapNotNull { all[it] }
            }
            "last50" -> seedItems.ifEmpty { vm.showcaseCategory(kind, "last50", 50) }
            else -> vm.showcaseCategory(kind, catId, 0)
        }
    }
    Column(
        Modifier
            .fillMaxSize()
            .clip(RoundedCornerShape(18.dp))
            .background(Color(0xE60B0F14))
            .border(1.dp, Color.White.copy(alpha = 0.16f), RoundedCornerShape(18.dp))
    ) {
        Row(Modifier.padding(horizontal = 8.dp, vertical = 6.dp)) {
            PanelTab(if (tr) "Kategoriler" else "Categories", tab == VodPanelTab.CATS, Modifier.weight(1f)) { tab = VodPanelTab.CATS }
            PanelTab(if (tr) "İçerik" else "Items", tab == VodPanelTab.ITEMS, Modifier.weight(1f)) { tab = VodPanelTab.ITEMS }
        }
        Box(Modifier.height(1.dp).fillMaxWidth().background(Color.White.copy(alpha = 0.12f)))
        if (tab == VodPanelTab.CATS) {
            LazyColumn(contentPadding = PaddingValues(10.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                item {
                    CatRow(if (tr) "Son eklenenler" else "Latest", catId == "last50") { catId = "last50"; tab = VodPanelTab.ITEMS }
                }
                if (continueWatching.isNotEmpty()) {
                    item {
                        CatRow(if (tr) "Son izlenenler" else "Recently watched", catId == "recent") { catId = "recent"; tab = VodPanelTab.ITEMS }
                    }
                }
                items(cats, key = { it.id }) { c ->
                    CatRow(c.name, catId == c.id) { catId = c.id; tab = VodPanelTab.ITEMS }
                }
            }
        } else {
            LazyColumn(contentPadding = PaddingValues(10.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(items, key = { it.id }) { item ->
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (item.id == playingId) MobileCyan.copy(alpha = 0.18f) else MobileCard)
                            .then(if (item.id == playingId) Modifier.border(1.dp, MobileCyan, RoundedCornerShape(12.dp)) else Modifier)
                            .rayClickable(onClick = { onPlay(item) })
                            .padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        AsyncImage(item.poster, null, Modifier.size(52.dp).clip(RoundedCornerShape(8.dp)), contentScale = ContentScale.Crop)
                        Spacer(Modifier.width(10.dp))
                        Column(Modifier.weight(1f)) {
                            Text(item.name, color = Color.White, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text(
                                listOf(item.year, item.rating).filter { it.isNotBlank() }.joinToString(" · "),
                                color = Color.White.copy(alpha = 0.55f),
                                fontSize = 12.sp
                            )
                        }
                        if (favorites.any { it.mediaId == item.id }) {
                            Icon(Icons.Filled.Favorite, null, tint = Color(0xFFFF5252), modifier = Modifier.size(16.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PortraitSeriesPanel(
    tr: Boolean,
    extras: VodMeta,
    series: VodEntity?,
    episodes: List<EpisodeEntity>,
    playingId: String,
    onActorClick: (String) -> Unit = {},
    onPlayEpisode: (EpisodeEntity) -> Unit
) {
    var tab by remember { mutableStateOf(SeriesPanelTab.EPISODES) }
    val seasons = remember(episodes) { episodes.map { it.season }.distinct().sorted() }
    var season by remember(seasons) { mutableIntStateOf(seasons.firstOrNull() ?: 1) }
    LaunchedEffect(playingId, episodes) {
        episodes.firstOrNull { it.id == playingId }?.let { season = it.season }
    }
    val inSeason = episodes.filter { it.season == season }
    Column(
        Modifier
            .fillMaxSize()
            .clip(RoundedCornerShape(18.dp))
            .background(Color(0xE60B0F14))
            .border(1.dp, Color.White.copy(alpha = 0.16f), RoundedCornerShape(18.dp))
    ) {
        Row(Modifier.padding(horizontal = 8.dp, vertical = 6.dp)) {
            PanelTab(if (tr) "Bilgi" else "Info", tab == SeriesPanelTab.INFO, Modifier.weight(1f)) { tab = SeriesPanelTab.INFO }
            PanelTab(if (tr) "Bölümler" else "Episodes", tab == SeriesPanelTab.EPISODES, Modifier.weight(1f)) { tab = SeriesPanelTab.EPISODES }
        }
        Box(Modifier.height(1.dp).fillMaxWidth().background(Color.White.copy(alpha = 0.12f)))
        if (tab == SeriesPanelTab.INFO) {
            LazyColumn(contentPadding = PaddingValues(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                item {
                    Text(series?.name ?: "", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    val plot = extras.plot.ifBlank { series?.plot.orEmpty() }
                    if (plot.isNotBlank()) {
                        Spacer(Modifier.height(8.dp))
                        Text(if (tr) "Özet" else "Synopsis", color = MobileCyan, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(4.dp))
                        Text(plot, color = Color.White.copy(alpha = 0.85f), fontSize = 13.sp, lineHeight = 18.sp)
                    }
                    if (extras.cast.isNotBlank()) {
                        Spacer(Modifier.height(8.dp))
                        Text(if (tr) "Oyuncular" else "Cast", color = MobileCyan, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(6.dp))
                        val actorNames = remember(extras.cast) { extras.cast.split(',', '|', ';').map { it.trim() }.filter { it.isNotBlank() } }
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(actorNames) { name ->
                                Text(
                                    name,
                                    color = Color.White,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(Color.White.copy(alpha = 0.1f))
                                        .border(1.dp, MobileCyan.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
                                        .rayClickable(onClick = { onActorClick(name) })
                                        .padding(horizontal = 10.dp, vertical = 6.dp)
                                )
                            }
                        }
                    }
                }
            }
        } else {
            Column(Modifier.fillMaxSize()) {
                if (seasons.size > 1) {
                    LazyRow(contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(seasons) { s ->
                            val on = s == season
                            Text(
                                if (tr) "Sezon $s" else "Season $s",
                                color = if (on) Color.Black else Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(if (on) MobileCyan else Color.White.copy(alpha = 0.1f))
                                    .rayClickable(onClick = { season = s })
                                    .padding(horizontal = 12.dp, vertical = 6.dp)
                            )
                        }
                    }
                }
                LazyColumn(contentPadding = PaddingValues(10.dp), verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.weight(1f)) {
                    items(inSeason, key = { it.id }) { ep ->
                        val on = ep.id == playingId
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (on) MobileCyan.copy(alpha = 0.18f) else MobileCard)
                                .then(if (on) Modifier.border(1.dp, MobileCyan, RoundedCornerShape(12.dp)) else Modifier)
                                .rayClickable(onClick = { onPlayEpisode(ep) })
                                .padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("S${ep.season}E${ep.episode}", color = MobileCyan, fontWeight = FontWeight.Bold, modifier = Modifier.width(72.dp))
                            Text(ep.name.ifBlank { if (tr) "Bölüm ${ep.episode}" else "Episode ${ep.episode}" }, color = Color.White, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PanelTab(label: String, on: Boolean, modifier: Modifier, onClick: () -> Unit) {
    Box(
        modifier
            .clip(RoundedCornerShape(12.dp))
            .background(if (on) MobileCyan else Color.Transparent)
            .rayClickable(onClick)
            .padding(vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(label, color = if (on) Color.Black else Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
    }
}

@Composable
private fun CatRow(name: String, selected: Boolean, onClick: () -> Unit) {
    Text(
        name,
        color = if (selected) Color.Black else Color.White,
        fontWeight = FontWeight.Bold,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(if (selected) MobileCyan else MobileCard)
            .rayClickable(onClick)
            .padding(12.dp)
    )
}
