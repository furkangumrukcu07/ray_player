package com.ray.iptv.ui.screens.live

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.setValue
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.ray.iptv.data.local.CategoryEntity
import com.ray.iptv.data.local.ChannelEntity
import com.ray.iptv.data.local.EpgEntity
import com.ray.iptv.data.local.FavoriteEntity
import com.ray.iptv.data.local.GroupEntity
import com.ray.iptv.data.local.ProgressEntity
import com.ray.iptv.ui.components.GlassButton
import com.ray.iptv.ui.glass.GlassPanel
import com.ray.iptv.ui.i18n.Copy
import com.ray.iptv.ui.input.rayClickable
import com.ray.iptv.ui.input.rayFocusRequester
import com.ray.iptv.ui.input.tryFocus
import com.ray.iptv.ui.motion.RayCrossfade
import com.ray.iptv.ui.motion.rayPanelEnter
import com.ray.iptv.ui.motion.rayPanelExit
import com.ray.iptv.ui.motion.rayRailEnter
import com.ray.iptv.ui.motion.rayRailExit
import com.ray.iptv.ui.theme.LocalGlass
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.distinctUntilChanged
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@Composable
fun LiveScreen(
    copy: Copy,
    categories: List<CategoryEntity>,
    groups: List<GroupEntity>,
    channels: List<ChannelEntity>,
    allChannels: List<ChannelEntity>,
    allCount: Int = allChannels.size,
    categoryCounts: Map<String, Int> = emptyMap(),
    favorites: List<FavoriteEntity>,
    recent: List<ProgressEntity>,
    selectedCategory: String,
    zap: String,
    preview: Boolean,
    now: EpgEntity?,
    upcoming: List<EpgEntity>,
    previewUrl: String,
    nowByChannel: Map<String, EpgEntity>,
    guideSlots: Map<String, List<EpgEntity?>> = emptyMap(),
    showCategories: Boolean = true,
    onCategory: (String) -> Unit,
    onPickCategory: () -> Unit = {},
    onBackToCategories: () -> Unit = {},
    onExpandRail: () -> Unit = {},
    onPlay: (ChannelEntity) -> Unit,
    onHover: (ChannelEntity?) -> Unit,
    onLoadNowMap: (List<String>) -> Unit,
    onPin: (CategoryEntity) -> Unit,
    onHide: (CategoryEntity) -> Unit,
    onLock: (CategoryEntity) -> Unit,
    onFav: (ChannelEntity) -> Unit,
    onAddGroup: (String, ChannelEntity) -> Unit,
    onLoadMore: () -> Unit = {},
    stripPrefix: Boolean = false,
    railExpanded: Boolean = false,
    contentFocusTrigger: Long = 0L,
    onExit: () -> Unit = {}
) {
    val g = LocalGlass.current
    val focusManager = LocalFocusManager.current
    val catFocus = remember { FocusRequester() }
    val selectedCatFocus = remember { FocusRequester() }
    val listFocus = remember { FocusRequester() }
    val catListState = rememberLazyListState()
    val channelListState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    var hovered by remember { mutableStateOf<ChannelEntity?>(null) }
    var catMenu by remember { mutableStateOf<CategoryEntity?>(null) }
    var channelsFocused by remember { mutableStateOf(false) }
    var pendingChannelFocus by remember { mutableStateOf(false) }

    val favCount = remember(favorites) {
        favorites.count { it.kind == "LIVE" }
    }
    val recentCount = remember(recent) { recent.size }
    val catCounts = remember(categoryCounts) { categoryCounts }

    val categoryKeys = remember(categories, groups, recentCount) {
        buildList {
            add("")
            add("fav")
            if (recentCount > 0) add("recent")
            groups.forEach { add("group:${it.id}") }
            categories.forEach { add(it.id) }
        }
    }
    val targetCatIndex = remember(categoryKeys, selectedCategory) {
        val idx = categoryKeys.indexOf(selectedCategory)
        if (idx >= 0) idx else 0
    }

    val focusToSelectedCategory: () -> Unit = {
        channelsFocused = false
        pendingChannelFocus = false
        if (!showCategories) {
            onBackToCategories()
        }
        scope.launch {
            if (targetCatIndex in 0 until categoryKeys.size) {
                runCatching { catListState.scrollToItem(targetCatIndex) }
            }
            repeat(25) {
                delay(30)
                if (selectedCatFocus.tryFocus() || catFocus.tryFocus()) return@launch
            }
        }
    }

    BackHandler {
        when {
            !showCategories -> focusToSelectedCategory()
            channelsFocused -> focusToSelectedCategory()
            railExpanded -> onExit()
            else -> {
                onExpandRail()
                focusManager.moveFocus(FocusDirection.Left)
            }
        }
    }

    LaunchedEffect(selectedCategory) {
        hovered = null
        runCatching { channelListState.scrollToItem(0) }
    }

    LaunchedEffect(channels.size, channels.firstOrNull()?.id, channels.lastOrNull()?.id) {
        onLoadNowMap(channels.map { it.id })
        if (hovered == null || channels.none { it.id == hovered?.id }) {
            hovered = channels.firstOrNull()
        }
    }
    LaunchedEffect(hovered?.id) {
        onHover(hovered)
    }
    LaunchedEffect(pendingChannelFocus, channels.size) {
        if (pendingChannelFocus) {
            repeat(40) {
                if (listFocus.tryFocus()) {
                    pendingChannelFocus = false
                    channelsFocused = true
                    return@LaunchedEffect
                }
                delay(30)
            }
        }
    }
    LaunchedEffect(showCategories, railExpanded, contentFocusTrigger) {
        if (showCategories && !railExpanded && !pendingChannelFocus) {
            delay(20)
            if (targetCatIndex in 0 until categoryKeys.size) {
                runCatching { catListState.scrollToItem(targetCatIndex) }
            }
            repeat(30) {
                delay(35)
                if (selectedCatFocus.tryFocus() || catFocus.tryFocus()) return@LaunchedEffect
            }
        } else if (!showCategories && !railExpanded) {
            repeat(30) {
                delay(30)
                if (listFocus.tryFocus()) return@LaunchedEffect
            }
        }
    }

    val ch = hovered ?: channels.firstOrNull()
    val next = upcoming.firstOrNull { it.id != now?.id }

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
                CategoryPane(
                    copy = copy,
                    categories = categories,
                    groups = groups,
                    selectedCategory = selectedCategory,
                    allCount = allCount,
                    favCount = favCount,
                    recentCount = recentCount,
                    catCounts = catCounts,
                    catMenu = catMenu,
                    listState = catListState,
                    onCategory = onCategory,
                    onPick = {
                        hovered = null
                        channelsFocused = true
                        pendingChannelFocus = true
                        onPickCategory()
                        scope.launch {
                            repeat(30) {
                                delay(25)
                                if (listFocus.tryFocus()) {
                                    pendingChannelFocus = false
                                    return@launch
                                }
                            }
                        }
                    },
                    onMenu = { catMenu = it },
                    onPin = onPin,
                    onHide = { onHide(it); catMenu = null },
                    onLock = onLock,
                    onLeft = {
                        onExpandRail()
                        focusManager.moveFocus(FocusDirection.Left)
                    },
                    firstFocus = catFocus,
                    selectedFocus = selectedCatFocus,
                    onCatFocused = { 
                        channelsFocused = false 
                        pendingChannelFocus = false
                    },
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
                LiveMinaPanel(
                    copy = copy,
                    channels = channels,
                    zap = zap,
                    hovered = ch,
                    stripPrefix = stripPrefix,
                    preview = preview,
                    previewUrl = previewUrl,
                    now = now,
                    next = next,
                    guideSlots = guideSlots,
                    nowByChannel = nowByChannel,
                    groups = groups,
                    onHover = {
                        hovered = it
                        channelsFocused = true
                    },
                    onPlay = onPlay,
                    onFav = onFav,
                    onAddGroup = onAddGroup,
                    onLeftFromChannel = focusToSelectedCategory,
                    onLoadMore = onLoadMore,
                    listFocus = listFocus,
                    listState = channelListState,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }
}

@Composable
private fun LiveMinaPanel(
    copy: Copy,
    channels: List<ChannelEntity>,
    zap: String,
    hovered: ChannelEntity?,
    stripPrefix: Boolean,
    preview: Boolean,
    previewUrl: String,
    now: EpgEntity?,
    next: EpgEntity?,
    guideSlots: Map<String, List<EpgEntity?>>,
    nowByChannel: Map<String, EpgEntity>,
    groups: List<GroupEntity>,
    onHover: (ChannelEntity) -> Unit,
    onPlay: (ChannelEntity) -> Unit,
    onFav: (ChannelEntity) -> Unit,
    onAddGroup: (String, ChannelEntity) -> Unit,
    onLeftFromChannel: () -> Unit,
    onLoadMore: () -> Unit = {},
    listFocus: FocusRequester? = null,
    listState: LazyListState = rememberLazyListState(),
    modifier: Modifier = Modifier
) {
    BoxWithConstraints(modifier) {
        val heroH = ((maxHeight.value * 0.25f).coerceIn(150f, 220f) * 1.22f).dp
        val chCol = when {
            maxWidth < 420.dp -> 140.dp
            maxWidth < 600.dp -> 220.dp
            maxWidth < 800.dp -> 275.dp
            else -> 310.dp
        }
        var slotNow by remember { mutableLongStateOf(System.currentTimeMillis()) }
        LaunchedEffect(Unit) {
            while (true) {
                delay(60_000)
                slotNow = System.currentTimeMillis()
            }
        }
        val slots = slotTimes(slotNow)
        GlassPanel(
            strong = true,
            radius = 12.dp,
            modifier = Modifier
                .fillMaxSize()
                .onPreviewKeyEvent { e ->
                    if (e.type == KeyEventType.KeyDown && e.key == Key.DirectionLeft) {
                        onLeftFromChannel()
                        true
                    } else false
                }
        ) {
            Column(Modifier.fillMaxSize().padding(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                LiveHero(
                    copy = copy,
                    channel = hovered,
                    now = now,
                    next = next,
                    previewUrl = previewUrl,
                    preview = preview,
                    stripPrefix = stripPrefix,
                    height = heroH,
                    onPlay = { hovered?.let(onPlay) }
                )
                LiveGuide(
                    copy = copy,
                    channels = channels,
                    zap = zap,
                    hoveredId = hovered?.id,
                    stripPrefix = stripPrefix,
                    chCol = chCol,
                    slots = slots,
                    guideSlots = guideSlots,
                    nowByChannel = nowByChannel,
                    groups = groups,
                    onHover = onHover,
                    onPlay = onPlay,
                    onFav = onFav,
                    onAddGroup = onAddGroup,
                    onLeft = onLeftFromChannel,
                    onLoadMore = onLoadMore,
                    listFocus = listFocus,
                    listState = listState,
                    modifier = Modifier.weight(1f).fillMaxWidth()
                )
            }
        }
    }
}

@Composable
private fun LiveHero(
    copy: Copy,
    channel: ChannelEntity?,
    now: EpgEntity?,
    next: EpgEntity?,
    previewUrl: String,
    preview: Boolean,
    stripPrefix: Boolean,
    height: Dp,
    onPlay: () -> Unit
) {
    val g = LocalGlass.current
    val previewW = height * 16f / 9f
    val widePreview = if (now != null) previewW else previewW * 1.12f
    Row(
        Modifier.fillMaxWidth().height(height),
        horizontalArrangement = Arrangement.spacedBy(if (now != null) 12.dp else 16.dp)
    ) {
        Box(
            Modifier
                .width(widePreview)
                .fillMaxHeight()
                .clip(RoundedCornerShape(12.dp))
                .border(1.dp, Color.White.copy(alpha = 0.16f), RoundedCornerShape(12.dp))
                .focusProperties { canFocus = false }
                .rayClickable(onPlay)
        ) {
            LivePreviewSurface(
                url = previewUrl,
                logo = channel?.logo.orEmpty(),
                title = channel?.let { displayName(it.name, stripPrefix) }.orEmpty(),
                enabled = preview
            )
        }
        RayCrossfade(channel?.id.orEmpty(), Modifier.weight(1f).fillMaxHeight()) {
            GlassPanel(
                strong = false,
                radius = 12.dp,
                modifier = Modifier.fillMaxSize()
            ) {
                Box(Modifier.fillMaxSize().padding(horizontal = 12.dp, vertical = 10.dp)) {
                when {
                    channel == null -> {
                        Text(
                            copy.pickChannelPreview,
                            color = g.muted,
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier.align(Alignment.CenterStart)
                        )
                    }
                now != null -> {
                    Column(Modifier.fillMaxSize()) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            ChannelLogo(channel.logo, displayName(channel.name, stripPrefix), 36.dp)
                            Spacer(Modifier.width(8.dp))
                            Text(
                                displayName(channel.name, stripPrefix),
                                color = g.text,
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(2f)
                            )
                            Text(" · ", color = g.muted)
                            Text(
                                now.title,
                                color = g.text,
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(3f)
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                "${fmtClock(now.startMs)} – ${fmtClock(now.endMs)}",
                                color = g.muted,
                                style = MaterialTheme.typography.labelLarge
                            )
                        }
                        Spacer(Modifier.height(7.dp))
                        EpgNowProgress(now)
                        Spacer(Modifier.height(6.dp))
                        Text(
                            now.plot.trim().ifBlank { copy.noDescription },
                            color = g.muted,
                            style = MaterialTheme.typography.bodyMedium,
                            maxLines = 3,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )
                        if (next != null) {
                            Text(
                                "${copy.nextProgramme}: ${next.title.trim()} · ${fmtClock(next.startMs)} – ${fmtClock(next.endMs)}",
                                color = g.muted,
                                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
                else -> {
                    Row(Modifier.fillMaxSize(), verticalAlignment = Alignment.CenterVertically) {
                        ChannelLogo(channel.logo, displayName(channel.name, stripPrefix), 72.dp)
                        Spacer(Modifier.width(14.dp))
                        Column {
                            Text(
                                displayName(channel.name, stripPrefix),
                                color = g.text,
                                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(copy.noEpgForChannel, color = g.muted, style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
            }
            }
        }
    }
}
}

@Composable
private fun EpgNowProgress(now: EpgEntity) {
    val g = LocalGlass.current
    var nowMs by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(now.id, now.startMs, now.endMs) {
        while (true) {
            nowMs = System.currentTimeMillis()
            delay(1_000)
        }
    }
    val progress = if (now.endMs > now.startMs) {
        ((nowMs - now.startMs).toFloat() / (now.endMs - now.startMs)).coerceIn(0f, 1f)
    } else 0f
    Box(
        Modifier
            .fillMaxWidth()
            .height(4.dp)
            .clip(RoundedCornerShape(4.dp))
            .background(Color.White.copy(alpha = 0.12f))
    ) {
        Box(
            Modifier
                .fillMaxWidth(progress)
                .fillMaxSize()
                .background(g.accent.copy(alpha = 0.85f))
        )
    }
}

@Composable
private fun LiveGuide(
    copy: Copy,
    channels: List<ChannelEntity>,
    zap: String,
    hoveredId: String?,
    stripPrefix: Boolean,
    chCol: Dp,
    slots: List<Long>,
    guideSlots: Map<String, List<EpgEntity?>>,
    nowByChannel: Map<String, EpgEntity>,
    groups: List<GroupEntity>,
    onHover: (ChannelEntity) -> Unit,
    onPlay: (ChannelEntity) -> Unit,
    onFav: (ChannelEntity) -> Unit,
    onAddGroup: (String, ChannelEntity) -> Unit,
    onLeft: () -> Unit,
    onLoadMore: () -> Unit = {},
    listFocus: FocusRequester? = null,
    listState: LazyListState = rememberLazyListState(),
    modifier: Modifier = Modifier
) {
    val g = LocalGlass.current
    LaunchedEffect(channels.size, onLoadMore) {
        snapshotFlow {
            val last = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
            channels.isNotEmpty() && last >= channels.lastIndex - 16
        }.distinctUntilChanged().collect { nearEnd ->
            if (nearEnd) onLoadMore()
        }
    }
    val labels = listOf(
        copy.nowSlot,
        hourLabel(slots.getOrElse(1) { 0L }),
        hourLabel(slots.getOrElse(2) { 0L })
    )
    Column(modifier) {
        Row(Modifier.fillMaxWidth().height(36.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(
                copy.channelsTab,
                color = g.muted,
                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                modifier = Modifier.width(chCol).padding(start = 12.dp)
            )
            labels.forEachIndexed { i, label ->
                Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
                    Text(
                        label,
                        color = if (i == 0) g.text else g.muted,
                        style = MaterialTheme.typography.labelLarge.copy(
                            fontWeight = if (i == 0) FontWeight.Bold else FontWeight.SemiBold
                        )
                    )
                }
            }
        }
        LazyColumn(state = listState, modifier = Modifier.fillMaxSize()) {
            if (zap.isNotBlank()) {
                item {
                    Text(
                        zap,
                        color = g.accent,
                        style = MaterialTheme.typography.headlineMedium,
                        modifier = Modifier.padding(10.dp)
                    )
                }
            }
            if (channels.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp)
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color.White.copy(alpha = 0.04f))
                            .rayFocusRequester(listFocus)
                            .onPreviewKeyEvent { e ->
                                if (e.type == KeyEventType.KeyDown && e.key == Key.DirectionLeft) {
                                    onLeft(); true
                                } else false
                            },
                        contentAlignment = Alignment.CenterStart
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 14.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(g.accent)
                            )
                            Spacer(Modifier.width(10.dp))
                            Text(
                                text = copy.channelsTab,
                                color = g.muted,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                }
            }
            itemsIndexed(channels, key = { _, ch -> ch.id }) { index, ch ->
                val progs = guideSlots[ch.id]?.let { it + List(3 - it.size) { null } }?.take(3)
                    ?: listOf(nowByChannel[ch.id], null, null)
                GuideRow(
                    ch = ch,
                    number = if (ch.number > 0) ch.number else index + 1,
                    selected = hoveredId == ch.id,
                    stripPrefix = stripPrefix,
                    chCol = chCol,
                    programmes = progs,
                    onFocus = { onHover(ch) },
                    onClick = { onPlay(ch) },
                    onLong = {
                        onFav(ch)
                        groups.firstOrNull()?.let { onAddGroup(it.id, ch) }
                    },
                    onLeft = onLeft,
                    isFirst = index == 0,
                    isLast = index == channels.lastIndex,
                    focusRequester = if (index == 0) listFocus else null
                )

            }
        }
    }
}

@Composable
private fun GuideRow(
    ch: ChannelEntity,
    number: Int,
    selected: Boolean,
    stripPrefix: Boolean,
    chCol: Dp,
    programmes: List<EpgEntity?>,
    onFocus: () -> Unit,
    onClick: () -> Unit,
    onLong: () -> Unit,
    onLeft: () -> Unit,
    isFirst: Boolean = false,
    isLast: Boolean = false,
    focusRequester: FocusRequester? = null
) {
    val g = LocalGlass.current
    var focused by remember { mutableStateOf(false) }
    val active = focused || selected
    Row(
        Modifier
            .fillMaxWidth()
            .height(52.dp)
            .rayFocusRequester(focusRequester)
            .onFocusChanged {
                focused = it.isFocused
                if (it.isFocused) onFocus()
            }
            .onPreviewKeyEvent { e ->
                if (e.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
                when (e.key) {
                    Key.DirectionLeft -> {
                        onLeft()
                        true
                    }
                    Key.DirectionUp -> {
                        if (isFirst) true else false
                    }
                    Key.DirectionDown -> {
                        if (isLast) true else false
                    }
                    else -> false
                }
            }
            .rayClickable(onClick, onLong)
            .background(if (focused) g.accent.copy(alpha = 0.22f) else if (selected) Color.White.copy(alpha = 0.04f) else Color.Transparent)
            .border(
                width = if (focused) 1.2.dp else 0.4.dp,
                color = if (focused) g.accent else Color.White.copy(alpha = 0.035f)
            )
    ) {
        Row(
            Modifier.width(chCol).fillMaxHeight().padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                Modifier
                    .width(3.dp)
                    .fillMaxHeight()
                    .background(if (selected) g.accent else Color.Transparent)
            )
            Text(
                number.toString(),
                color = if (active) Color.White else g.muted,
                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                modifier = Modifier.width(28.dp)
            )
            ChannelLogo(ch.logo, displayName(ch.name, stripPrefix), 28.dp)
            Spacer(Modifier.width(8.dp))
            Text(
                displayName(ch.name, stripPrefix),
                color = if (active) Color.White else g.text.copy(alpha = 0.88f),
                style = MaterialTheme.typography.bodyLarge.copy(
                    fontWeight = if (active) FontWeight.Bold else FontWeight.SemiBold
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
        }
        programmes.take(3).forEachIndexed { i, prog ->
            EpgCell(
                programme = prog,
                nowSlot = i == 0,
                rowSelected = selected,
                modifier = Modifier.weight(1f).fillMaxHeight()
            )
        }
    }
}

@Composable
private fun EpgCell(
    programme: EpgEntity?,
    nowSlot: Boolean,
    rowSelected: Boolean,
    modifier: Modifier = Modifier
) {
    val g = LocalGlass.current
    val title = programme?.title?.trim().orEmpty()
    val empty = title.isEmpty()
    Box(
        modifier.padding(horizontal = 2.dp, vertical = 3.dp)
    ) {
        Box(
            Modifier
                .fillMaxSize()
                .clip(RoundedCornerShape(6.dp))
                .background(
                    when {
                        rowSelected && nowSlot -> g.accent.copy(alpha = 0.18f)
                        empty -> Color.Transparent
                        else -> Color.White.copy(alpha = 0.025f)
                    }
                )
                .border(
                    width = if (rowSelected && nowSlot) 1.2.dp else 0.5.dp,
                    color = if (rowSelected && nowSlot) g.accent.copy(alpha = 0.65f) else Color.White.copy(alpha = 0.045f),
                    shape = RoundedCornerShape(6.dp)
                )
                .padding(horizontal = 6.dp, vertical = 2.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            Text(
                if (empty) "—" else title,
                color = when {
                    empty -> Color.White.copy(alpha = 0.28f)
                    nowSlot -> Color.White.copy(alpha = 0.95f)
                    else -> Color.White.copy(alpha = 0.78f)
                },
                style = MaterialTheme.typography.bodySmall.copy(
                    fontSize = 12.sp,
                    fontWeight = if (nowSlot) FontWeight.SemiBold else FontWeight.Medium
                ),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun CategoryPane(
    copy: Copy,
    categories: List<CategoryEntity>,
    groups: List<GroupEntity>,
    selectedCategory: String,
    allCount: Int,
    favCount: Int,
    recentCount: Int,
    catCounts: Map<String, Int>,
    catMenu: CategoryEntity?,
    listState: LazyListState = rememberLazyListState(),
    onCategory: (String) -> Unit,
    onPick: () -> Unit,
    onMenu: (CategoryEntity) -> Unit,
    onPin: (CategoryEntity) -> Unit,
    onHide: (CategoryEntity) -> Unit,
    onLock: (CategoryEntity) -> Unit,
    onLeft: () -> Unit = {},
    firstFocus: FocusRequester? = null,
    selectedFocus: FocusRequester? = null,
    onCatFocused: () -> Unit = {},
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
                copy.live,
                color = g.muted,
                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
            )
            Spacer(Modifier.height(8.dp))
            LazyColumn(state = listState, verticalArrangement = Arrangement.spacedBy(2.dp)) {
                item {
                    val isSel = selectedCategory.isEmpty()
                    CatRow(
                        copy.allChannels,
                        allCount,
                        isSel,
                        onClick = { onCategory(""); onPick() },
                        onFocus = { onCatFocused(); onCategory("") },
                        onRight = onPick,
                        isFirst = true,
                        focusRequester = if (isSel) selectedFocus else firstFocus
                    )
                }
                item {
                    val isSel = selectedCategory == "fav"
                    CatRow(
                        copy.favorites,
                        favCount,
                        isSel,
                        onClick = { onCategory("fav"); onPick() },
                        onFocus = { onCatFocused(); onCategory("fav") },
                        onRight = onPick,
                        focusRequester = if (isSel) selectedFocus else null
                    )
                }
                if (recentCount > 0) {
                    item {
                        val isSel = selectedCategory == "recent"
                        CatRow(
                            copy.recentlyWatched,
                            recentCount,
                            isSel,
                            onClick = { onCategory("recent"); onPick() },
                            onFocus = { onCatFocused(); onCategory("recent") },
                            onRight = onPick,
                            focusRequester = if (isSel) selectedFocus else null
                        )
                    }
                }
                items(groups, key = { it.id }) { gr ->
                    val isSel = selectedCategory == "group:${gr.id}"
                    CatRow(
                        "★  ${gr.name}",
                        null,
                        isSel,
                        onClick = { onCategory("group:${gr.id}"); onPick() },
                        onFocus = { onCatFocused(); onCategory("group:${gr.id}") },
                        onRight = onPick,
                        focusRequester = if (isSel) selectedFocus else null
                    )
                }
                catMenu?.let { cat ->
                    item {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.padding(8.dp)
                        ) {
                            GlassButton(if (cat.pinned) "Unpin" else "Pin") { onPin(cat) }
                            GlassButton("Gizle") { onHide(cat) }
                            GlassButton(if (cat.locked) "Aç" else "Kilitle") { onLock(cat) }
                        }
                    }
                }
                items(categories, key = { it.id }) { cat ->
                    val isSel = selectedCategory == cat.id
                    CatRow(
                        (if (cat.pinned) "●  " else "") + cat.name + if (cat.locked) "  🔒" else "",
                        catCounts[cat.id] ?: 0,
                        isSel,
                        onClick = { onCategory(cat.id); onPick() },
                        onFocus = { onCatFocused(); onCategory(cat.id) },
                        onRight = onPick,
                        onLong = { onMenu(cat) },
                        focusRequester = if (isSel) selectedFocus else null
                    )
                }
            }
        }
    }
}

@Composable
private fun CatRow(
    label: String,
    count: Int?,
    selected: Boolean,
    onClick: () -> Unit,
    onFocus: () -> Unit = {},
    onRight: (() -> Unit)? = null,
    onLong: (() -> Unit)? = null,
    isFirst: Boolean = false,
    focusRequester: FocusRequester? = null
) {
    var focused by remember { mutableStateOf(false) }
    val g = LocalGlass.current
    val active = focused || selected
    GlassPanel(
        focused = focused,
        strong = selected,
        accentFill = selected && !focused,
        fillAlpha = if (active) 1f else 0.12f,
        radius = 8.dp,
        onClick = onClick,
        onLongClick = onLong,
        modifier = Modifier
            .fillMaxWidth()
            .height(52.dp)
            .rayFocusRequester(focusRequester)
            .onFocusChanged {
                focused = it.isFocused
                if (it.isFocused) onFocus()
            }
            .onPreviewKeyEvent { e ->
                if (e.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
                when (e.key) {
                    Key.DirectionRight, Key.DirectionCenter, Key.Enter -> {
                        if (onRight != null) {
                            onRight()
                            true
                        } else {
                            onClick()
                            true
                        }
                    }
                    Key.DirectionUp -> {
                        if (isFirst) true else false
                    }
                    else -> false
                }
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
            if (count != null) {
                Spacer(Modifier.width(8.dp))
                CategoryCountBadge(count = count, emphasized = active)
            }
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
            maxLines = 1
        )
    }
}

@Composable
private fun ChannelLogo(url: String, name: String, size: Dp) {
    val g = LocalGlass.current
    val ctx = LocalContext.current
    val px = with(LocalDensity.current) { (size * 2).roundToPx().coerceIn(48, 128) }
    Box(
        Modifier
            .size(size)
            .clip(RoundedCornerShape(7.dp))
            .background(g.panelStrong),
        contentAlignment = Alignment.Center
    ) {
        if (url.isNotBlank()) {
            AsyncImage(
                model = ImageRequest.Builder(ctx)
                    .data(url)
                    .size(px)
                    .crossfade(false)
                    .build(),
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        } else {
            Text(
                name.firstOrNull()?.uppercase() ?: "?",
                color = g.text,
                style = MaterialTheme.typography.labelLarge
            )
        }
    }
}

private fun displayName(name: String, strip: Boolean): String {
    if (!strip) return name
    return name.replace(Regex("""^[A-Z]{2,3}\s*[|:/-]\s*"""), "").trim().ifBlank { name }
}

private fun fmtClock(ms: Long): String =
    SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(ms))

private fun hourLabel(ms: Long): String = fmtClock(ms)

private fun slotTimes(now: Long): List<Long> {
    val cal = Calendar.getInstance()
    cal.timeInMillis = now
    cal.set(Calendar.MINUTE, 0)
    cal.set(Calendar.SECOND, 0)
    cal.set(Calendar.MILLISECOND, 0)
    val hourStart = cal.timeInMillis
    return listOf(now, hourStart + 3_600_000L, hourStart + 7_200_000L)
}
