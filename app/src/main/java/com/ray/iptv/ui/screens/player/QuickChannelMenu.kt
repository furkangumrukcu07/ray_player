package com.ray.iptv.ui.screens.player

import android.view.KeyEvent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
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
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontStyle
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
import com.ray.iptv.data.local.ChannelEntity
import com.ray.iptv.data.local.EpgEntity
import com.ray.iptv.ui.glass.GlassPanel
import com.ray.iptv.ui.i18n.Copy
import com.ray.iptv.ui.theme.LocalGlass
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import androidx.activity.compose.BackHandler

private data class QuickTape(
    val id: String,
    val name: String,
    val channels: List<ChannelEntity>
)

private val CyanAccent = Color(0xFF18FFFF)

@Composable
fun QuickChannelMenu(
    channels: List<ChannelEntity>,
    categories: List<CategoryEntity>,
    playingId: String,
    playingUrl: String,
    copy: Copy,
    loadNowMap: suspend (List<String>) -> Map<String, EpgEntity>,
    onPick: (ChannelEntity) -> Unit,
    onClose: () -> Unit
) {
    val tapes = remember(channels, categories) { buildTapes(channels, categories) }
    if (tapes.isEmpty()) return
    val start = remember(playingId, tapes) {
        tapes.indexOfFirst { tape -> tape.channels.any { it.id == playingId } }.coerceAtLeast(0)
    }
    var catIndex by remember { mutableIntStateOf(start) }
    var tick by remember { mutableIntStateOf(0) }
    var focusedId by remember { mutableStateOf(playingId) }
    val tape = tapes[catIndex.coerceIn(0, tapes.lastIndex)]
    var nowMap by remember { mutableStateOf<Map<String, EpgEntity>>(emptyMap()) }
    val listState = rememberLazyListState()
    val focusReq = remember { FocusRequester() }
    val clock = remember { SimpleDateFormat("HH:mm", Locale.getDefault()) }

    LaunchedEffect(Unit) { tick++ }
    LaunchedEffect(tick) {
        delay(7_000)
        onClose()
    }
    LaunchedEffect(tape.id, tape.channels.size, tape.channels.firstOrNull()?.id, tape.channels.lastOrNull()?.id, playingId) {
        val ids = tape.channels.map { it.id }
        val target = ids.indexOfFirst { it == playingId }.let { if (it >= 0) it else 0 }
        val from = (target - 10).coerceAtLeast(0)
        val to = (target + 30).coerceAtMost(ids.size)
        nowMap = if (ids.isEmpty()) emptyMap() else loadNowMap(ids.subList(from, to))
        focusedId = tape.channels.getOrNull(target)?.id ?: focusedId
        if (tape.channels.isNotEmpty()) {
            listState.scrollToItem(target)
            delay(40)
            runCatching { focusReq.requestFocus() }
        }
    }

    fun bump() { tick++ }
    fun shift(delta: Int) {
        if (tapes.size <= 1) return
        bump()
        catIndex = (catIndex + delta + tapes.size) % tapes.size
    }

    val focused = tape.channels.firstOrNull { it.id == focusedId } ?: tape.channels.firstOrNull()
    val focusedEpg = focused?.let { nowMap[it.id] }
    val focusIndex = tape.channels.indexOfFirst { it.id == focusedId }.coerceAtLeast(0)

    BoxWithConstraints(
        Modifier.fillMaxSize()
    ) {
        val compact = maxWidth < 700.dp
        val railW = if (compact) 300.dp else 560.dp
        val pillW = if (compact) 268.dp else 260.dp
        Box(
            Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.45f))
                .pointerInput(Unit) { detectTapGestures { onClose() } }
        )
        Row(
            Modifier
                .fillMaxSize()
                .padding(
                    start = if (compact) 12.dp else 24.dp,
                    top = if (compact) 16.dp else 32.dp,
                    end = if (compact) 12.dp else 32.dp,
                    bottom = if (compact) 16.dp else 28.dp
                ),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(
                Modifier
                    .width(railW)
                    .fillMaxHeight()
                    .onPreviewKeyEvent { ev ->
                        if (ev.nativeKeyEvent.keyCode == KeyEvent.KEYCODE_BACK) {
                            if (ev.nativeKeyEvent.action == KeyEvent.ACTION_DOWN) {
                                onClose()
                            }
                            return@onPreviewKeyEvent true
                        }
                        if (ev.nativeKeyEvent.action != KeyEvent.ACTION_DOWN) return@onPreviewKeyEvent false
                        when (ev.nativeKeyEvent.keyCode) {
                            KeyEvent.KEYCODE_DPAD_LEFT -> { shift(-1); true }
                            KeyEvent.KEYCODE_DPAD_RIGHT -> { shift(1); true }
                            else -> false
                        }
                    }
            ) {
                CategoryPill(
                    name = tape.name,
                    showArrows = tapes.size > 1,
                    onPrev = { shift(-1) },
                    onNext = { shift(1) }
                )
                Spacer(Modifier.height(16.dp))
                LazyColumn(
                    state = listState,
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    itemsIndexed(tape.channels, key = { _, ch -> ch.id }) { index, ch ->
                        ChannelRow(
                            channel = ch,
                            epg = nowMap[ch.id],
                            focused = focusedId == ch.id,
                            playing = ch.id == playingId,
                            pillWidth = pillW,
                            showEpg = !compact,
                            liveBadge = copy.liveBadge,
                            modifier = Modifier
                                .then(if (index == focusIndex) Modifier.focusRequester(focusReq) else Modifier)
                                .onFocusChanged {
                                    if (it.isFocused) {
                                        focusedId = ch.id
                                        bump()
                                    }
                                },
                            onClick = {
                                bump()
                                focusedId = ch.id
                                onPick(ch)
                            }
                        )
                    }
                }
            }
            if (!compact && focused != null) {
                InfoCard(
                    channel = focused,
                    epg = focusedEpg,
                    url = if (focused.id == playingId) playingUrl else focused.streamUrl,
                    copy = copy,
                    clock = clock
                )
            }
        }
    }
}

@Composable
private fun CategoryPill(
    name: String,
    showArrows: Boolean,
    onPrev: () -> Unit,
    onNext: () -> Unit
) {
    GlassPanel(
        strong = true,
        radius = 16.dp,
        modifier = Modifier.width(260.dp).height(40.dp)
    ) {
        Row(Modifier.fillMaxSize(), verticalAlignment = Alignment.CenterVertically) {
            if (showArrows) PillArrow(Icons.Filled.ChevronLeft, onPrev) else Spacer(Modifier.width(12.dp))
            Text(
                name,
                color = Color.White.copy(alpha = 0.95f),
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold, fontSize = 16.sp),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center,
                modifier = Modifier.weight(1f)
            )
            if (showArrows) PillArrow(Icons.Filled.ChevronRight, onNext) else Spacer(Modifier.width(12.dp))
        }
    }
}

@Composable
private fun PillArrow(icon: ImageVector, onClick: () -> Unit) {
    Box(
        Modifier
            .size(44.dp, 40.dp)
            .focusProperties { canFocus = false }
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, contentDescription = null, tint = Color.White.copy(alpha = 0.5f), modifier = Modifier.size(24.dp))
    }
}

@Composable
private fun ChannelRow(
    channel: ChannelEntity,
    epg: EpgEntity?,
    focused: Boolean,
    playing: Boolean,
    pillWidth: Dp,
    showEpg: Boolean,
    liveBadge: String,
    modifier: Modifier,
    onClick: () -> Unit
) {
    val g = LocalGlass.current
    Row(modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        GlassPanel(
            focused = focused,
            strong = playing,
            radius = 16.dp,
            scaleOnFocus = false,
            onClick = onClick,
            modifier = Modifier.width(pillWidth).height(56.dp)
        ) {
            Row(
                Modifier.fillMaxSize().padding(horizontal = 8.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color.White.copy(alpha = 0.05f)),
                    contentAlignment = Alignment.Center
                ) {
                    if (channel.logo.isNotBlank()) {
                        AsyncImage(
                            model = channel.logo,
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Fit
                        )
                    } else {
                        Text(
                            channel.name.take(1).uppercase(),
                            color = g.text,
                            style = MaterialTheme.typography.titleMedium
                        )
                    }
                }
                Spacer(Modifier.width(10.dp))
                Text(
                    channel.name,
                    color = Color.White,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 14.sp
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                if (focused) {
                    Box(
                        Modifier
                            .padding(start = 6.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(Brush.horizontalGradient(listOf(Color.Cyan, Color(0xFF448AFF))))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            liveBadge,
                            color = Color.White,
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 10.sp
                            )
                        )
                    }
                }
            }
        }
        if (showEpg) {
            Spacer(Modifier.width(12.dp))
            Box(
                Modifier
                    .weight(1f)
                    .height(56.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color.Black.copy(alpha = 0.15f))
                    .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(10.dp))
                    .padding(horizontal = 10.dp, vertical = 8.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.Center) {
                    Text(
                        epg?.title.orEmpty(),
                        color = if (focused) Color.White else Color.White.copy(alpha = 0.54f),
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontSize = 13.sp,
                            fontWeight = if (focused) FontWeight.SemiBold else FontWeight.Normal
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    val p = epgProgress(epg)
                    if (focused && p >= 0f) {
                        Spacer(Modifier.height(6.dp))
                        Box(
                            Modifier
                                .fillMaxWidth()
                                .height(2.dp)
                                .clip(RoundedCornerShape(1.dp))
                                .background(Color.White.copy(alpha = 0.24f))
                        ) {
                            Box(
                                Modifier
                                    .fillMaxWidth(p)
                                    .height(2.dp)
                                    .background(CyanAccent)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun InfoCard(
    channel: ChannelEntity,
    epg: EpgEntity?,
    url: String,
    copy: Copy,
    clock: SimpleDateFormat
) {
    val transport = streamTransport(url)
    GlassPanel(strong = true, radius = 18.dp, modifier = Modifier.width(340.dp)) {
        Column(Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color.White.copy(alpha = 0.08f)),
                    contentAlignment = Alignment.Center
                ) {
                    if (channel.logo.isNotBlank()) {
                        AsyncImage(
                            model = channel.logo,
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Fit
                        )
                    }
                }
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        channel.name,
                        color = Color.White,
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (epg != null && epg.startMs > 0 && epg.endMs > epg.startMs) {
                        Text(
                            "${clock.format(Date(epg.startMs))} - ${clock.format(Date(epg.endMs))}",
                            color = Color.White.copy(alpha = 0.7f),
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp)
                        )
                    }
                }
                MiniBadge(showBolt = true, text = "Exo")
                Spacer(Modifier.width(4.dp))
                if (transport.isNotBlank()) MiniBadge(showBolt = false, text = transport)
            }
            Spacer(Modifier.height(12.dp))
            if (epg != null && epg.title.isNotBlank()) {
                Text(
                    epg.title,
                    color = CyanAccent,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 14.sp
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                val p = epgProgress(epg)
                if (p >= 0f) {
                    Spacer(Modifier.height(6.dp))
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .height(2.5.dp)
                            .clip(RoundedCornerShape(1.5.dp))
                            .background(Color.White.copy(alpha = 0.24f))
                    ) {
                        Box(
                            Modifier
                                .fillMaxWidth(p)
                                .height(2.5.dp)
                                .background(CyanAccent)
                        )
                    }
                }
                if (epg.plot.isNotBlank()) {
                    Spacer(Modifier.height(10.dp))
                    Text(
                        epg.plot,
                        color = Color.White.copy(alpha = 0.7f),
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp, lineHeight = 14.sp),
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            } else {
                Text(
                    copy.noEpgForChannel,
                    color = Color.White.copy(alpha = 0.54f),
                    style = MaterialTheme.typography.bodySmall.copy(fontStyle = FontStyle.Italic)
                )
            }
        }
    }
}

@Composable
private fun MiniBadge(showBolt: Boolean, text: String) {
    Row(
        Modifier
            .clip(RoundedCornerShape(4.dp))
            .background(Color.White.copy(alpha = 0.10f))
            .border(1.dp, Color.White.copy(alpha = 0.30f), RoundedCornerShape(4.dp))
            .padding(horizontal = 5.dp, vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (showBolt) {
            Icon(Icons.Filled.Bolt, contentDescription = null, tint = Color.White, modifier = Modifier.size(10.dp))
            Spacer(Modifier.width(2.dp))
        }
        Text(text, color = Color.White, style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp))
    }
}

private fun epgProgress(epg: EpgEntity?): Float {
    if (epg == null || epg.endMs <= epg.startMs) return -1f
    val now = System.currentTimeMillis()
    return ((now - epg.startMs).toFloat() / (epg.endMs - epg.startMs).toFloat()).coerceIn(0f, 1f)
}

private fun streamTransport(url: String): String {
    val u = url.lowercase()
    return when {
        ".m3u8" in u || "type=m3u8" in u || "/hls/" in u -> "HLS"
        ".mpd" in u -> "DASH"
        ".ts" in u || "mpegts" in u || "format=ts" in u -> "TS"
        else -> ""
    }
}

private fun buildTapes(
    channels: List<ChannelEntity>,
    categories: List<CategoryEntity>
): List<QuickTape> {
    val live = channels.filter { !it.hidden }
    val grouped = live.groupBy { it.categoryId }
    val tapes = categories.mapNotNull { cat ->
        val list = grouped[cat.id].orEmpty()
        if (list.isEmpty()) null else QuickTape(cat.id, cat.name, list.sortedBy { it.number })
    }
    if (tapes.isNotEmpty()) return tapes
    if (live.isEmpty()) return emptyList()
    return live.groupBy { it.categoryName.ifBlank { "Live" } }
        .map { (name, list) -> QuickTape(name, name, list.sortedBy { it.number }) }
}
