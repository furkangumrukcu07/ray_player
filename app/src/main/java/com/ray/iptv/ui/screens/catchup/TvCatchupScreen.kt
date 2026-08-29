package com.ray.iptv.ui.screens.catchup

import androidx.activity.compose.BackHandler
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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Replay
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
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Icon
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import coil.compose.AsyncImage
import com.ray.iptv.data.local.ChannelEntity
import com.ray.iptv.data.local.EpgEntity
import com.ray.iptv.ui.glass.GlassPanel
import com.ray.iptv.ui.i18n.Copy
import com.ray.iptv.ui.input.tryFocus
import com.ray.iptv.ui.theme.LocalGlass
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

private data class DayTab(
    val dayOffset: Int,
    val label: String,
    val subLabel: String,
    val startMs: Long,
    val endMs: Long
)

@Composable
fun TvCatchupScreen(
    copy: Copy,
    channels: List<ChannelEntity>,
    loadProgrammes: suspend (ChannelEntity, Long, Long) -> List<EpgEntity>,
    onPlayCatchup: (ChannelEntity, EpgEntity) -> Unit,
    onPlayLive: (ChannelEntity) -> Unit,
    onBackToRail: () -> Unit,
    railExpanded: Boolean,
    contentFocusTrigger: Long = 0L,
    time24h: Boolean = true,
    tr: Boolean = true
) {
    val g = LocalGlass.current
    val scope = rememberCoroutineScope()

    // Filter channels: Prioritize channels with archive or all channels
    val catchupChannels = remember(channels) {
        val withArchive = channels.filter { it.hasArchive }
        if (withArchive.isNotEmpty()) withArchive else channels
    }

    var selectedChannel by remember { mutableStateOf<ChannelEntity?>(catchupChannels.firstOrNull()) }
    var selectedDayOffset by remember { mutableIntStateOf(0) }
    var programmes by remember { mutableStateOf<List<EpgEntity>>(emptyList()) }
    var selectedProgramme by remember { mutableStateOf<EpgEntity?>(null) }
    var isLoadingProgrammes by remember { mutableStateOf(false) }

    val channelListFocusRequester = remember { FocusRequester() }
    val dayListFocusRequester = remember { FocusRequester() }
    val programmeListFocusRequester = remember { FocusRequester() }
    val playButtonFocusRequester = remember { FocusRequester() }

    // Generate last 8 days (Today, -1, -2, ..., -7)
    val dayTabs = remember(tr) {
        val list = mutableListOf<DayTab>()
        val dayFmt = SimpleDateFormat("d MMM", if (tr) Locale("tr", "TR") else Locale.getDefault())
        val dayNameFmt = SimpleDateFormat("EEE", if (tr) Locale("tr", "TR") else Locale.getDefault())

        for (offset in 0..7) {
            val c = Calendar.getInstance().apply {
                add(Calendar.DAY_OF_YEAR, -offset)
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
            val startMs = c.timeInMillis
            val endMs = startMs + 24 * 3600_000L - 1000L

            val label = when (offset) {
                0 -> if (tr) "Bugün" else "Today"
                1 -> if (tr) "Dün" else "Yesterday"
                else -> dayNameFmt.format(Date(startMs)).replaceFirstChar { it.uppercase() }
            }
            val subLabel = dayFmt.format(Date(startMs))
            list.add(DayTab(offset, label, subLabel, startMs, endMs))
        }
        list
    }

    // Load programmes when channel or day changes
    LaunchedEffect(selectedChannel?.id, selectedDayOffset) {
        val ch = selectedChannel ?: return@LaunchedEffect
        val day = dayTabs.getOrNull(selectedDayOffset) ?: return@LaunchedEffect
        isLoadingProgrammes = true
        val list = loadProgrammes(ch, day.startMs, day.endMs)
        programmes = list.sortedBy { it.startMs }
        selectedProgramme = programmes.firstOrNull()
        isLoadingProgrammes = false
    }

    // Auto-focus first channel when screen enters or content focus triggered
    LaunchedEffect(contentFocusTrigger, railExpanded) {
        if (!railExpanded) {
            repeat(12) {
                delay(30)
                if (channelListFocusRequester.tryFocus()) return@LaunchedEffect
            }
        }
    }

    val timeFmt = remember(time24h) {
        SimpleDateFormat(if (time24h) "HH:mm" else "h:mm a", Locale.getDefault())
    }

    BackHandler {
        onBackToRail()
    }

    Row(
        Modifier
            .fillMaxSize()
            .padding(10.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // ==========================================
        // 1. SOL KOLON: KANALLAR LİSTESİ
        // ==========================================
        GlassPanel(
            strong = true,
            radius = 12.dp,
            modifier = Modifier
                .width(270.dp)
                .fillMaxHeight()
        ) {
            Column(
                Modifier
                    .fillMaxSize()
                    .padding(10.dp)
            ) {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 6.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        Icons.Filled.Replay,
                        contentDescription = null,
                        tint = g.accent,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        if (tr) "Tekrar İzle" else "Catch-up TV",
                        color = g.text,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                    Spacer(Modifier.weight(1f))
                    Box(
                        Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(Color.White.copy(alpha = 0.08f))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            "${catchupChannels.size}",
                            color = g.muted,
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold)
                        )
                    }
                }

                Spacer(Modifier.height(6.dp))

                LazyColumn(
                    Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    itemsIndexed(catchupChannels, key = { _, ch -> ch.id }) { idx, ch ->
                        val isSelected = selectedChannel?.id == ch.id
                        var isFocused by remember { mutableStateOf(false) }

                        val itemMod = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .then(if (idx == 0) Modifier.focusRequester(channelListFocusRequester) else Modifier)
                            .onFocusChanged {
                                isFocused = it.isFocused
                                if (it.isFocused) {
                                    selectedChannel = ch
                                }
                            }
                            .onPreviewKeyEvent { e ->
                                if (e.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
                                when (e.key) {
                                    Key.DirectionLeft -> {
                                        onBackToRail()
                                        true
                                    }
                                    Key.DirectionRight, Key.Enter, Key.DirectionCenter -> {
                                        selectedChannel = ch
                                        programmeListFocusRequester.tryFocus()
                                        true
                                    }
                                    else -> false
                                }
                            }

                        GlassPanel(
                            focused = isFocused,
                            strong = isSelected,
                            accentFill = isSelected && !isFocused,
                            fillAlpha = if (isFocused || isSelected) 1f else 0.12f,
                            radius = 8.dp,
                            onClick = {
                                selectedChannel = ch
                                programmeListFocusRequester.tryFocus()
                            },
                            modifier = itemMod
                        ) {
                            Row(
                                Modifier
                                    .fillMaxSize()
                                    .padding(horizontal = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                if (ch.logo.isNotBlank()) {
                                    AsyncImage(
                                        model = ch.logo,
                                        contentDescription = ch.name,
                                        modifier = Modifier
                                            .size(28.dp)
                                            .clip(RoundedCornerShape(6.dp)),
                                        contentScale = ContentScale.Fit
                                    )
                                } else {
                                    Box(
                                        Modifier
                                            .size(28.dp)
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(Color.White.copy(alpha = 0.08f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            ch.name.take(1).uppercase(),
                                            color = Color.White,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 12.sp
                                        )
                                    }
                                }

                                Text(
                                    ch.name,
                                    color = if (isFocused || isSelected) Color.White else g.text,
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                    ),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f)
                                )

                                if (ch.hasArchive) {
                                    Box(
                                        Modifier
                                            .clip(RoundedCornerShape(4.dp))
                                            .background(g.accent.copy(alpha = 0.2f))
                                            .border(0.8.dp, g.accent.copy(alpha = 0.6f), RoundedCornerShape(4.dp))
                                            .padding(horizontal = 4.dp, vertical = 1.dp)
                                    ) {
                                        Text(
                                            "REC",
                                            color = g.accent,
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.ExtraBold
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // ==========================================
        // 2. ORTA KOLON: GÜN SEKMELERİ & PROGRAM AKIŞI
        // ==========================================
        GlassPanel(
            strong = true,
            radius = 12.dp,
            modifier = Modifier
                .weight(1.15f)
                .fillMaxHeight()
        ) {
            Column(
                Modifier
                    .fillMaxSize()
                    .padding(10.dp)
            ) {
                // Gün Sekmeleri (Pills)
                LazyRow(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    contentPadding = PaddingValues(vertical = 2.dp)
                ) {
                    itemsIndexed(dayTabs, key = { _, day -> day.dayOffset }) { idx, day ->
                        val isDaySelected = selectedDayOffset == day.dayOffset
                        var isDayFocused by remember { mutableStateOf(false) }

                        val dayMod = Modifier
                            .height(38.dp)
                            .then(if (idx == 0) Modifier.focusRequester(dayListFocusRequester) else Modifier)
                            .onFocusChanged {
                                isDayFocused = it.isFocused
                                if (it.isFocused) {
                                    selectedDayOffset = day.dayOffset
                                }
                            }
                            .onPreviewKeyEvent { e ->
                                if (e.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
                                when (e.key) {
                                    Key.DirectionDown -> {
                                        programmeListFocusRequester.tryFocus()
                                        true
                                    }
                                    Key.DirectionLeft -> {
                                        if (idx == 0) {
                                            channelListFocusRequester.tryFocus()
                                            true
                                        } else false
                                    }
                                    else -> false
                                }
                            }

                        GlassPanel(
                            focused = isDayFocused,
                            strong = isDaySelected,
                            accentFill = isDaySelected && !isDayFocused,
                            fillAlpha = if (isDayFocused || isDaySelected) 1f else 0.15f,
                            radius = 19.dp,
                            onClick = {
                                selectedDayOffset = day.dayOffset
                                programmeListFocusRequester.tryFocus()
                            },
                            modifier = dayMod
                        ) {
                            Row(
                                Modifier
                                    .fillMaxHeight()
                                    .padding(horizontal = 12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    day.label,
                                    color = if (isDayFocused || isDaySelected) Color.White else g.text,
                                    style = MaterialTheme.typography.labelLarge.copy(
                                        fontWeight = if (isDaySelected) FontWeight.Bold else FontWeight.Medium
                                    )
                                )
                                Text(
                                    day.subLabel,
                                    color = if (isDayFocused || isDaySelected) Color.White.copy(alpha = 0.8f) else g.muted,
                                    style = MaterialTheme.typography.labelSmall
                                )
                            }
                        }
                    }
                }

                Spacer(Modifier.height(8.dp))

                // Program Listesi
                if (isLoadingProgrammes) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(
                            if (tr) "Yayın akışı yükleniyor..." else "Loading guide...",
                            color = g.muted,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                } else if (programmes.isEmpty()) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Filled.History, contentDescription = null, tint = g.muted, modifier = Modifier.size(36.dp))
                            Spacer(Modifier.height(8.dp))
                            Text(
                                if (tr) "Bu tarih için kayıtlı yayın akışı bulunamadı" else "No guide data for this date",
                                color = g.muted,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                } else {
                    val nowMs = System.currentTimeMillis()
                    LazyColumn(
                        Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        itemsIndexed(programmes, key = { _, p -> "${p.channelId}_${p.startMs}" }) { idx, p ->
                            val isProgSelected = selectedProgramme?.startMs == p.startMs
                            var isProgFocused by remember { mutableStateOf(false) }

                            val isLive = nowMs in p.startMs..p.endMs
                            val isPast = p.endMs < nowMs

                            val progMod = Modifier
                                .fillMaxWidth()
                                .height(54.dp)
                                .then(if (idx == 0) Modifier.focusRequester(programmeListFocusRequester) else Modifier)
                                .onFocusChanged {
                                    isProgFocused = it.isFocused
                                    if (it.isFocused) {
                                        selectedProgramme = p
                                    }
                                }
                                .onPreviewKeyEvent { e ->
                                    if (e.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
                                    when (e.key) {
                                        Key.DirectionLeft -> {
                                            channelListFocusRequester.tryFocus()
                                            true
                                        }
                                        Key.DirectionRight -> {
                                            playButtonFocusRequester.tryFocus()
                                            true
                                        }
                                        Key.DirectionUp -> {
                                            if (idx == 0) {
                                                dayListFocusRequester.tryFocus()
                                                true
                                            } else false
                                        }
                                        Key.Enter, Key.DirectionCenter -> {
                                            val ch = selectedChannel
                                            if (ch != null) {
                                                if (isPast) onPlayCatchup(ch, p) else onPlayLive(ch)
                                            }
                                            true
                                        }
                                        else -> false
                                    }
                                }

                            GlassPanel(
                                focused = isProgFocused,
                                strong = isProgSelected,
                                accentFill = isProgSelected && !isProgFocused,
                                fillAlpha = if (isProgFocused || isProgSelected) 1f else 0.12f,
                                radius = 8.dp,
                                onClick = {
                                    val ch = selectedChannel
                                    if (ch != null) {
                                        if (isPast) onPlayCatchup(ch, p) else onPlayLive(ch)
                                    }
                                },
                                modifier = progMod
                            ) {
                                Row(
                                    Modifier
                                        .fillMaxSize()
                                        .padding(horizontal = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    // Zaman Rozeti
                                    Box(
                                        Modifier
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(if (isLive) g.accent.copy(alpha = 0.2f) else Color.White.copy(alpha = 0.06f))
                                            .padding(horizontal = 6.dp, vertical = 3.dp)
                                    ) {
                                        Text(
                                            "${timeFmt.format(Date(p.startMs))} - ${timeFmt.format(Date(p.endMs))}",
                                            color = if (isLive) g.accent else g.muted,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }

                                    // Başlık
                                    Text(
                                        p.title.ifBlank { if (tr) "İsimsiz Yayın" else "Untitled" },
                                        color = if (isProgFocused || isProgSelected) Color.White else g.text,
                                        style = MaterialTheme.typography.bodyMedium.copy(
                                            fontWeight = if (isProgSelected || isLive) FontWeight.Bold else FontWeight.Normal
                                        ),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.weight(1f)
                                    )

                                    // Durum Rozeti (Canlı / Tekrar)
                                    if (isLive) {
                                        Box(
                                            Modifier
                                                .clip(RoundedCornerShape(4.dp))
                                                .background(Color(0xFFE53935))
                                                .padding(horizontal = 6.dp, vertical = 2.dp)
                                        ) {
                                            Text(
                                                if (tr) "CANLI" else "LIVE",
                                                color = Color.White,
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.ExtraBold
                                            )
                                        }
                                    } else if (isPast) {
                                        Icon(
                                            Icons.Filled.Replay,
                                            contentDescription = null,
                                            tint = if (isProgFocused || isProgSelected) g.accent else g.muted,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // ==========================================
        // 3. SAĞ KOLON: SİNEMATİK ÖNİZLEME & OYNAT
        // ==========================================
        GlassPanel(
            strong = true,
            radius = 12.dp,
            modifier = Modifier
                .weight(0.95f)
                .fillMaxHeight()
        ) {
            val prog = selectedProgramme
            val chan = selectedChannel

            if (chan != null && prog != null) {
                val nowMs = System.currentTimeMillis()
                val isPast = prog.endMs < nowMs
                val isLive = nowMs in prog.startMs..prog.endMs
                val durationMin = ((prog.endMs - prog.startMs) / 60_000L).coerceAtLeast(1L)

                Column(
                    Modifier
                        .fillMaxSize()
                        .padding(14.dp),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(Modifier.weight(1f)) {
                        // Kanal Bilgisi Başlığı
                        Row(
                            Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            if (chan.logo.isNotBlank()) {
                                AsyncImage(
                                    model = chan.logo,
                                    contentDescription = chan.name,
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(RoundedCornerShape(8.dp)),
                                    contentScale = ContentScale.Fit
                                )
                            }
                            Column(Modifier.weight(1f)) {
                                Text(
                                    chan.name,
                                    color = g.text,
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    if (isLive) (if (tr) "Şu an Canlı Yayında" else "Currently Live")
                                    else if (isPast) (if (tr) "Kaçırılan Yayın (Kayıt)" else "Catch-up Archive")
                                    else (if (tr) "Gelecek Yayın" else "Upcoming"),
                                    color = if (isLive) Color(0xFFE53935) else g.accent,
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold)
                                )
                            }
                        }

                        Spacer(Modifier.height(14.dp))

                        // Program Adı
                        Text(
                            prog.title.ifBlank { if (tr) "İsimsiz Program" else "Untitled Show" },
                            color = Color.White,
                            style = MaterialTheme.typography.headlineSmall.copy(
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 20.sp
                            ),
                            maxLines = 3,
                            overflow = TextOverflow.Ellipsis
                        )

                        Spacer(Modifier.height(8.dp))

                        // Zaman ve Süre Rozetleri
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(Color.White.copy(alpha = 0.08f))
                                    .padding(horizontal = 8.dp, vertical = 3.dp)
                            ) {
                                Text(
                                    "⏰ ${timeFmt.format(Date(prog.startMs))} - ${timeFmt.format(Date(prog.endMs))}",
                                    color = Color.White.copy(alpha = 0.9f),
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold)
                                )
                            }

                            Box(
                                Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(Color.White.copy(alpha = 0.08f))
                                    .padding(horizontal = 8.dp, vertical = 3.dp)
                            ) {
                                Text(
                                    "⏱ $durationMin ${if (tr) "dk" else "min"}",
                                    color = g.muted,
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold)
                                )
                            }
                        }

                        Spacer(Modifier.height(12.dp))

                        // Program Açıklaması / Özeti
                        GlassPanel(
                            strong = false,
                            radius = 8.dp,
                            fillAlpha = 0.15f,
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f)
                        ) {
                            LazyColumn(
                                Modifier
                                    .fillMaxSize()
                                    .padding(10.dp)
                            ) {
                                item {
                                    Text(
                                        prog.plot.ifBlank {
                                            if (tr) "Bu yayın için detaylı açıklama veya özet bilgisi bulunmuyor."
                                            else "No detailed description available for this broadcast."
                                        },
                                        color = g.muted,
                                        style = MaterialTheme.typography.bodySmall.copy(lineHeight = 18.sp)
                                    )
                                }
                            }
                        }
                    }

                    Spacer(Modifier.height(12.dp))

                    // Büyük Oynat / Baştan İzle Butonu
                    var isButtonFocused by remember { mutableStateOf(false) }
                    val buttonMod = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .focusRequester(playButtonFocusRequester)
                        .onFocusChanged { isButtonFocused = it.isFocused }
                        .onPreviewKeyEvent { e ->
                            if (e.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
                            when (e.key) {
                                Key.DirectionLeft -> {
                                    programmeListFocusRequester.tryFocus()
                                    true
                                }
                                Key.Enter, Key.DirectionCenter -> {
                                    if (isPast) onPlayCatchup(chan, prog) else onPlayLive(chan)
                                    true
                                }
                                else -> false
                            }
                        }

                    GlassPanel(
                        focused = isButtonFocused,
                        strong = true,
                        accentFill = true,
                        radius = 10.dp,
                        onClick = {
                            if (isPast) onPlayCatchup(chan, prog) else onPlayLive(chan)
                        },
                        modifier = buttonMod
                    ) {
                        Row(
                            Modifier.fillMaxSize(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                if (isPast) Icons.Filled.Replay else Icons.Filled.PlayArrow,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(22.dp)
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                if (isPast) (if (tr) "Baştan İzle (Catch-up)" else "Watch from Start")
                                else (if (tr) "Canlı Yayını İzle" else "Watch Live Stream"),
                                color = Color.White,
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                            )
                        }
                    }
                }
            } else {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        if (tr) "Detayları görüntülemek için bir program seçin" else "Select a programme to view details",
                        color = g.muted,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }
    }
}
