package com.ray.iptv.ui.mobile

import android.content.Context
import android.content.Intent
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.LiveTv
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.NightsStay
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SatelliteAlt
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material.icons.filled.WbSunny
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Icon
import androidx.tv.material3.Text
import coil.compose.AsyncImage
import com.ray.iptv.data.local.ProgressEntity
import com.ray.iptv.ui.RayViewModel
import com.ray.iptv.ui.input.rayClickable
import kotlinx.coroutines.delay
import java.util.Calendar
import java.util.concurrent.TimeUnit
import kotlin.math.roundToInt

private enum class WrapPeriod { WEEK, MONTH, YEAR, ALL_TIME }

@Composable
fun MobileWrappedScreen(
    vm: RayViewModel,
    tr: Boolean,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    var period by remember { mutableStateOf(WrapPeriod.MONTH) }
    var rows by remember { mutableStateOf<List<ProgressEntity>>(emptyList()) }
    var isStoryOpen by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        rows = vm.wrappedRows()
    }

    val now = System.currentTimeMillis()
    val since = when (period) {
        WrapPeriod.WEEK -> now - TimeUnit.DAYS.toMillis(7)
        WrapPeriod.MONTH -> now - TimeUnit.DAYS.toMillis(30)
        WrapPeriod.YEAR -> now - TimeUnit.DAYS.toMillis(365)
        WrapPeriod.ALL_TIME -> 0L
    }
    val slice = remember(rows, since) { rows.filter { it.updatedAt >= since } }
    val liveMs = slice.filter { it.kind == "LIVE" }.sumOf { it.positionMs.coerceAtLeast(0L) }
    val movieMs = slice.filter { it.kind == "MOVIE" }.sumOf { it.positionMs.coerceAtLeast(0L) }
    val seriesMs = slice.filter { it.kind == "EPISODE" || it.kind == "SERIES" }.sumOf { it.positionMs.coerceAtLeast(0L) }
    val totalMs = (liveMs + movieMs + seriesMs).coerceAtLeast(0L)
    val safeTotal = totalMs.coerceAtLeast(1L)

    val livePct = ((liveMs * 100.0) / safeTotal).roundToInt()
    val moviePct = ((movieMs * 100.0) / safeTotal).roundToInt()
    val seriesPct = ((seriesMs * 100.0) / safeTotal).roundToInt()

    val topLive = slice.filter { it.kind == "LIVE" }.maxByOrNull { it.positionMs }
    val topVod = slice.filter { it.kind != "LIVE" }.maxByOrNull { it.positionMs }

    val cal = Calendar.getInstance()
    val dayCounts = IntArray(7)
    slice.forEach {
        cal.timeInMillis = it.updatedAt
        dayCounts[cal.get(Calendar.DAY_OF_WEEK) - 1]++
    }
    val topDayIdx = dayCounts.indices.maxByOrNull { dayCounts[it] } ?: 0
    val dayNamesTr = listOf("Pazar", "Pazartesi", "Salı", "Çarşamba", "Perşembe", "Cuma", "Cumartesi")
    val dayNamesEn = listOf("Sunday", "Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday")
    val topDay = if (tr) dayNamesTr[topDayIdx] else dayNamesEn[topDayIdx]

    val morningCount = slice.count {
        cal.timeInMillis = it.updatedAt
        cal.get(Calendar.HOUR_OF_DAY) in 5..11
    }
    val eveningCount = slice.count {
        cal.timeInMillis = it.updatedAt
        cal.get(Calendar.HOUR_OF_DAY) in 17..23
    }
    val nightCount = slice.count {
        cal.timeInMillis = it.updatedAt
        cal.get(Calendar.HOUR_OF_DAY) in 0..4
    }

    val timePreference = when {
        nightCount >= morningCount && nightCount >= eveningCount -> if (tr) "Gece Baykuşu 🦉" else "Night Owl 🦉"
        morningCount >= eveningCount -> if (tr) "Sabah Kuşu 🌅" else "Early Bird 🌅"
        else -> if (tr) "Akşam Keyfi 🌆" else "Prime Time 🌆"
    }

    val personaTitle = when {
        liveMs >= movieMs + seriesMs && liveMs > 0 -> if (tr) "Canlı Yayın Gurmesi 📡" else "Live Broadcast Guru 📡"
        movieMs >= seriesMs && movieMs > 0 -> if (tr) "Sinema Tutkunu 🍿" else "Cinema Enthusiast 🍿"
        seriesMs > 0 -> if (tr) "Dizi Bağımlısı 📺" else "Binge Watcher 📺"
        else -> if (tr) "Televizyon Meraklısı ✨" else "TV Explorer ✨"
    }

    val periodLabel = when (period) {
        WrapPeriod.WEEK -> if (tr) "Haftalık" else "Weekly"
        WrapPeriod.MONTH -> if (tr) "Aylık" else "Monthly"
        WrapPeriod.YEAR -> if (tr) "Yıllık" else "Yearly"
        WrapPeriod.ALL_TIME -> if (tr) "Tüm Zamanlar" else "All Time"
    }

    val totalStr = fmtDur(totalMs, tr)
    val liveStr = fmtDur(liveMs, tr)
    val movieStr = fmtDur(movieMs, tr)
    val seriesStr = fmtDur(seriesMs, tr)

    val cyan = Color(0xFF00E5FF)
    val purple = Color(0xFFAB47BC)
    val coral = Color(0xFFFF5722)

    if (isStoryOpen) {
        WrappedStoryPlayer(
            tr = tr,
            periodLabel = periodLabel,
            personaTitle = personaTitle,
            totalStr = totalStr,
            liveStr = liveStr,
            movieStr = movieStr,
            seriesStr = seriesStr,
            topChannel = topLive?.title ?: if (tr) "Canlı TV" else "Live TV",
            topChannelLogo = topLive?.poster.orEmpty(),
            topVodTitle = topVod?.title ?: "",
            topVodPoster = topVod?.poster.orEmpty(),
            timePreference = timePreference,
            topDay = topDay,
            livePct = livePct,
            moviePct = moviePct,
            seriesPct = seriesPct,
            onClose = { isStoryOpen = false },
            onShare = {
                shareWrappedSummary(
                    context = context,
                    periodLabel = periodLabel,
                    totalStr = totalStr,
                    liveStr = liveStr,
                    vodStr = fmtDur(movieMs + seriesMs, tr),
                    topChannel = topLive?.title ?: "—",
                    persona = personaTitle,
                    topDay = topDay,
                    tr = tr
                )
            }
        )
        return
    }

    Column(
        Modifier
            .fillMaxSize()
            .background(Color(0xFF0B0F19))
            .verticalScroll(rememberScrollState())
            .padding(bottom = 36.dp)
    ) {
        // Header
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
                    text = if (tr) "✨ Ray Wrapped" else "✨ Ray Wrapped",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 17.sp,
                    lineHeight = 20.sp,
                    maxLines = 1
                )
                Text(
                    text = if (tr) "İzleme alışkanlıklarınız ve TV analitiğiniz" else "Your viewing habits and TV analytics",
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
                    .background(Color.White.copy(alpha = 0.12f))
                    .rayClickable(onClick = {
                        shareWrappedSummary(
                            context = context,
                            periodLabel = periodLabel,
                            totalStr = totalStr,
                            liveStr = liveStr,
                            vodStr = fmtDur(movieMs + seriesMs, tr),
                            topChannel = topLive?.title ?: "—",
                            persona = personaTitle,
                            topDay = topDay,
                            tr = tr
                        )
                    }),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Filled.Share,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(19.dp)
                )
            }
        }

        // Period Selector Tabs
        Row(
            Modifier
                .padding(horizontal = 14.dp, vertical = 6.dp)
                .fillMaxWidth()
                .clip(RoundedCornerShape(22.dp))
                .background(Color.White.copy(alpha = 0.08f))
                .padding(4.dp)
        ) {
            WrapPeriod.entries.forEach { p ->
                val on = period == p
                val lab = when (p) {
                    WrapPeriod.WEEK -> if (tr) "Haftalık" else "Weekly"
                    WrapPeriod.MONTH -> if (tr) "Aylık" else "Monthly"
                    WrapPeriod.YEAR -> if (tr) "Yıllık" else "Yearly"
                    WrapPeriod.ALL_TIME -> if (tr) "Tümü" else "All Time"
                }
                Box(
                    Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(18.dp))
                        .background(if (on) cyan else Color.Transparent)
                        .rayClickable(onClick = { period = p })
                        .padding(vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = lab,
                        color = if (on) Color.Black else Color.White.copy(alpha = 0.85f),
                        fontWeight = if (on) FontWeight.Bold else FontWeight.Medium,
                        fontSize = 12.sp,
                        lineHeight = 14.sp
                    )
                }
            }
        }

        Spacer(Modifier.height(10.dp))

        // Start Story Interactive Button
        Box(
            Modifier
                .padding(horizontal = 14.dp)
                .fillMaxWidth()
                .clip(RoundedCornerShape(18.dp))
                .background(
                    Brush.horizontalGradient(
                        listOf(Color(0xFF8E2DE2), Color(0xFF4A00E0))
                    )
                )
                .border(1.dp, Color.White.copy(alpha = 0.25f), RoundedCornerShape(18.dp))
                .rayClickable(onClick = { isStoryOpen = true })
                .padding(14.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.22f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Filled.AutoAwesome,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(22.dp)
                    )
                }
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        text = if (tr) "Hikayeni Başlat" else "Start Your Story",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        lineHeight = 18.sp
                    )
                    Text(
                        text = if (tr) "$periodLabel izleme özetini Instagram hikayesi gibi izle" else "View your $periodLabel summary as a story",
                        color = Color.White.copy(alpha = 0.85f),
                        fontSize = 12.sp,
                        lineHeight = 15.sp
                    )
                }
                Icon(
                    Icons.Filled.PlayArrow,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(24.dp)
                )
            }
        }

        Spacer(Modifier.height(14.dp))

        // Hero Persona Card
        Box(
            Modifier
                .padding(horizontal = 14.dp)
                .fillMaxWidth()
                .clip(RoundedCornerShape(22.dp))
                .background(
                    Brush.linearGradient(
                        listOf(Color(0xFFFF5722), Color(0xFFD81B60), Color(0xFF8E24AA))
                    )
                )
                .padding(18.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "✨ RAY WRAPPED $periodLabel",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp,
                        lineHeight = 13.sp,
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(Color.White.copy(alpha = 0.22f))
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                    Icon(
                        Icons.Filled.EmojiEvents,
                        contentDescription = null,
                        tint = Color(0xFFFFD54F),
                        modifier = Modifier.size(24.dp)
                    )
                }

                Text(
                    text = personaTitle,
                    color = Color.White,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 21.sp,
                    lineHeight = 25.sp
                )

                Text(
                    text = if (tr) {
                        "Bu dönemde toplam $totalStr televizyon izlediniz. En aktif izleme vaktiniz: $timePreference."
                    } else {
                        "You watched $totalStr of TV this period. Preferred time: $timePreference."
                    },
                    color = Color.White.copy(alpha = 0.92f),
                    fontSize = 13.sp,
                    lineHeight = 17.sp
                )

                Box(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color.Black.copy(alpha = 0.35f))
                        .padding(12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (tr) "Toplam Süre: $totalStr" else "Total Time: $totalStr",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        lineHeight = 18.sp
                    )
                }
            }
        }

        Spacer(Modifier.height(14.dp))

        // Grid Stats
        Text(
            text = if (tr) "İzleme Detayları" else "Viewing Breakdown",
            color = Color.White,
            fontWeight = FontWeight.Bold,
            fontSize = 15.sp,
            lineHeight = 18.sp,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 2.dp)
        )

        Spacer(Modifier.height(6.dp))

        Row(
            Modifier
                .padding(horizontal = 14.dp)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            StatGridCard(
                icon = Icons.Filled.LiveTv,
                title = if (tr) "Canlı TV" else "Live TV",
                value = liveStr,
                pct = livePct,
                accentColor = Color(0xFFEF5350),
                modifier = Modifier.weight(1f)
            )
            StatGridCard(
                icon = Icons.Filled.Movie,
                title = if (tr) "Film" else "Movies",
                value = movieStr,
                pct = moviePct,
                accentColor = cyan,
                modifier = Modifier.weight(1f)
            )
            StatGridCard(
                icon = Icons.Filled.VideoLibrary,
                title = if (tr) "Dizi" else "Series",
                value = seriesStr,
                pct = seriesPct,
                accentColor = Color(0xFFAB47BC),
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(Modifier.height(12.dp))

        // Highlights Card
        Column(
            Modifier
                .padding(horizontal = 14.dp)
                .fillMaxWidth()
                .clip(RoundedCornerShape(18.dp))
                .background(Color(0xFF161E2E))
                .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(18.dp))
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = if (tr) "Öne Çıkanlar & Alışkanlıklar" else "Highlights & Habits",
                color = cyan,
                fontWeight = FontWeight.Bold,
                fontSize = 13.5.sp,
                lineHeight = 16.sp
            )

            HighlightRow(
                icon = Icons.Filled.Favorite,
                title = if (tr) "En Çok İzlenen Kanal" else "Most Watched Channel",
                detail = topLive?.title ?: if (tr) "Henüz veri yok" else "No data yet",
                extra = if (topLive != null) fmtDur(topLive.positionMs, tr) else ""
            )

            HighlightRow(
                icon = Icons.Filled.CalendarMonth,
                title = if (tr) "En Aktif Günün" else "Most Active Day",
                detail = topDay,
                extra = ""
            )

            HighlightRow(
                icon = Icons.Filled.WbSunny,
                title = if (tr) "İzleme Saati Tercihi" else "Time of Day",
                detail = timePreference,
                extra = ""
            )
        }

        Spacer(Modifier.height(12.dp))

        // Distribution Bars
        Column(
            Modifier
                .padding(horizontal = 14.dp)
                .fillMaxWidth()
                .clip(RoundedCornerShape(18.dp))
                .background(Color(0xFF161E2E))
                .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(18.dp))
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = if (tr) "Kategoriye Göre Dağılım" else "Category Distribution",
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 13.5.sp,
                lineHeight = 16.sp
            )

            DistProgressBar(
                label = if (tr) "Canlı TV" else "Live TV",
                pct = livePct,
                color = Color(0xFFEF5350)
            )
            DistProgressBar(
                label = if (tr) "Filmler" else "Movies",
                pct = moviePct,
                color = cyan
            )
            DistProgressBar(
                label = if (tr) "Diziler" else "Series",
                pct = seriesPct,
                color = purple
            )
        }

        Spacer(Modifier.height(16.dp))

        // Big Share Button at Bottom
        Box(
            Modifier
                .padding(horizontal = 14.dp)
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(cyan)
                .rayClickable(onClick = {
                    shareWrappedSummary(
                        context = context,
                        periodLabel = periodLabel,
                        totalStr = totalStr,
                        liveStr = liveStr,
                        vodStr = fmtDur(movieMs + seriesMs, tr),
                        topChannel = topLive?.title ?: "—",
                        persona = personaTitle,
                        topDay = topDay,
                        tr = tr
                    )
                })
                .padding(vertical = 14.dp),
            contentAlignment = Alignment.Center
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Filled.Share,
                    contentDescription = null,
                    tint = Color.Black,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = if (tr) "Özetini Arkadaşlarınla Paylaş" else "Share Summary with Friends",
                    color = Color.Black,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    lineHeight = 17.sp
                )
            }
        }
    }
}

@Composable
private fun StatGridCard(
    icon: ImageVector,
    title: String,
    value: String,
    pct: Int,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    Column(
        modifier
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0xFF161E2E))
            .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(16.dp))
            .padding(10.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            Modifier
                .size(32.dp)
                .clip(CircleShape)
                .background(accentColor.copy(alpha = 0.18f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = null, tint = accentColor, modifier = Modifier.size(16.dp))
        }
        Spacer(Modifier.height(6.dp))
        Text(title, color = Color.White.copy(alpha = 0.7f), fontSize = 11.5.sp, lineHeight = 13.sp)
        Spacer(Modifier.height(2.dp))
        Text(value, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp, lineHeight = 15.sp)
        Spacer(Modifier.height(2.dp))
        Text("%$pct", color = accentColor, fontWeight = FontWeight.Bold, fontSize = 11.sp, lineHeight = 13.sp)
    }
}

@Composable
private fun HighlightRow(
    icon: ImageVector,
    title: String,
    detail: String,
    extra: String
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth()
    ) {
        Box(
            Modifier
                .size(32.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(Color.White.copy(alpha = 0.07f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
        }
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            Text(title, color = Color.White.copy(alpha = 0.6f), fontSize = 11.sp, lineHeight = 13.sp)
            Text(detail, color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 13.sp, lineHeight = 16.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        if (extra.isNotBlank()) {
            Text(extra, color = Color(0xFF00E5FF), fontWeight = FontWeight.Bold, fontSize = 12.sp, lineHeight = 14.sp)
        }
    }
}

@Composable
private fun DistProgressBar(label: String, pct: Int, color: Color) {
    Column {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(label, color = Color.White.copy(alpha = 0.8f), fontSize = 12.sp, lineHeight = 14.sp)
            Text("%$pct", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp, lineHeight = 14.sp)
        }
        Spacer(Modifier.height(4.dp))
        Box(
            Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(Color.White.copy(alpha = 0.10f))
        ) {
            Box(
                Modifier
                    .fillMaxHeight()
                    .fillMaxWidth((pct.coerceIn(0, 100) / 100f))
                    .background(color)
            )
        }
    }
}

// ----------------------------------------------------
// FULLSCREEN INSTAGRAM/SPOTIFY WRAPPED STORY PLAYER
// ----------------------------------------------------
@Composable
private fun WrappedStoryPlayer(
    tr: Boolean,
    periodLabel: String,
    personaTitle: String,
    totalStr: String,
    liveStr: String,
    movieStr: String,
    seriesStr: String,
    topChannel: String,
    topChannelLogo: String,
    topVodTitle: String,
    topVodPoster: String,
    timePreference: String,
    topDay: String,
    livePct: Int,
    moviePct: Int,
    seriesPct: Int,
    onClose: () -> Unit,
    onShare: () -> Unit
) {
    val totalStories = 6
    var currentStory by remember { mutableIntStateOf(0) }
    var isPaused by remember { mutableStateOf(false) }

    LaunchedEffect(currentStory, isPaused) {
        if (!isPaused) {
            delay(5500L)
            if (currentStory < totalStories - 1) {
                currentStory++
            } else {
                onClose()
            }
        }
    }

    val storyGradients = listOf(
        listOf(Color(0xFF8E2DE2), Color(0xFF4A00E0)),
        listOf(Color(0xFFFF5722), Color(0xFFFF9800)),
        listOf(Color(0xFF00C9FF), Color(0xFF92FE9D)),
        listOf(Color(0xFFF857A6), Color(0xFFFF5858)),
        listOf(Color(0xFF11998E), Color(0xFF38EF7D)),
        listOf(Color(0xFF1A2980), Color(0xFF26D0CE))
    )

    val currentGradient = storyGradients.getOrElse(currentStory) { storyGradients[0] }

    Box(
        Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(currentGradient))
            .pointerInput(Unit) {
                detectTapGestures(
                    onPress = {
                        isPaused = true
                        tryAwaitRelease()
                        isPaused = false
                    },
                    onTap = { offset ->
                        val screenWidth = size.width
                        if (offset.x < screenWidth * 0.35f) {
                            if (currentStory > 0) currentStory--
                        } else {
                            if (currentStory < totalStories - 1) currentStory++ else onClose()
                        }
                    }
                )
            }
            .padding(16.dp)
    ) {
        Column(Modifier.fillMaxSize()) {
            // Story Progress Indicators
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                for (i in 0 until totalStories) {
                    Box(
                        Modifier
                            .weight(1f)
                            .height(3.5.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(Color.White.copy(alpha = if (i < currentStory) 1f else if (i == currentStory) 0.8f else 0.3f))
                    )
                }
            }

            Spacer(Modifier.height(14.dp))

            // Header (Title & Close)
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Filled.AutoAwesome,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = "RAY WRAPPED • $periodLabel",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        lineHeight = 14.sp
                    )
                }

                Box(
                    Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.35f))
                        .rayClickable(onClick = onClose),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Filled.Close, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
                }
            }

            // Animated Story Content
            Box(
                Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                AnimatedContent(
                    targetState = currentStory,
                    transitionSpec = {
                        fadeIn(tween(350)) togetherWith fadeOut(tween(350))
                    },
                    label = "storyContent"
                ) { storyIdx ->
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp)
                    ) {
                        when (storyIdx) {
                            0 -> {
                                Text(
                                    text = if (tr) "2026 / $periodLabel" else "2026 / $periodLabel",
                                    color = Color.White.copy(alpha = 0.8f),
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Spacer(Modifier.height(12.dp))
                                Text(
                                    text = if (tr) "İzleme Hikayene\nHoş Geldin! 🎉" else "Welcome to Your\nViewing Story! 🎉",
                                    color = Color.White,
                                    fontSize = 32.sp,
                                    lineHeight = 38.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    textAlign = TextAlign.Center
                                )
                                Spacer(Modifier.height(16.dp))
                                Text(
                                    text = if (tr) "Bu dönemde seninle harika anlar paylaştık. İşte senin televizyon dünyan!" else "We shared great TV moments with you. Here is your summary!",
                                    color = Color.White.copy(alpha = 0.9f),
                                    fontSize = 14.sp,
                                    lineHeight = 18.sp,
                                    textAlign = TextAlign.Center
                                )
                            }
                            1 -> {
                                Box(
                                    Modifier
                                        .size(80.dp)
                                        .clip(CircleShape)
                                        .background(Color.White.copy(alpha = 0.2f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Filled.Timer, contentDescription = null, tint = Color.White, modifier = Modifier.size(44.dp))
                                }
                                Spacer(Modifier.height(16.dp))
                                Text(
                                    text = if (tr) "Televizyon Başında" else "You Spent",
                                    color = Color.White.copy(alpha = 0.85f),
                                    fontSize = 16.sp
                                )
                                Spacer(Modifier.height(8.dp))
                                Text(
                                    text = totalStr,
                                    color = Color.White,
                                    fontSize = 36.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    textAlign = TextAlign.Center
                                )
                                Spacer(Modifier.height(8.dp))
                                Text(
                                    text = if (tr) "vakit geçirdin! ⏱️" else "watching TV! ⏱️",
                                    color = Color.White.copy(alpha = 0.85f),
                                    fontSize = 16.sp
                                )
                            }
                            2 -> {
                                Box(
                                    Modifier
                                        .size(80.dp)
                                        .clip(RoundedCornerShape(20.dp))
                                        .background(Color.Black.copy(alpha = 0.4f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (topChannelLogo.isNotBlank()) {
                                        AsyncImage(
                                            model = topChannelLogo,
                                            contentDescription = null,
                                            modifier = Modifier.size(60.dp),
                                            contentScale = ContentScale.Fit
                                        )
                                    } else {
                                        Icon(Icons.Filled.LiveTv, contentDescription = null, tint = Color.White, modifier = Modifier.size(44.dp))
                                    }
                                }
                                Spacer(Modifier.height(16.dp))
                                Text(
                                    text = if (tr) "Senin 1 Numaralı Kanalın:" else "Your #1 Live Channel:",
                                    color = Color.White.copy(alpha = 0.85f),
                                    fontSize = 15.sp
                                )
                                Spacer(Modifier.height(8.dp))
                                Text(
                                    text = topChannel,
                                    color = Color.White,
                                    fontSize = 28.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    textAlign = TextAlign.Center
                                )
                                Spacer(Modifier.height(8.dp))
                                Text(
                                    text = if (tr) "En çok bu kanalda vakit geçirdin! ⭐️" else "You tuned into this channel the most! ⭐️",
                                    color = Color.White.copy(alpha = 0.9f),
                                    fontSize = 14.sp
                                )
                            }
                            3 -> {
                                Box(
                                    Modifier
                                        .size(80.dp)
                                        .clip(CircleShape)
                                        .background(Color.White.copy(alpha = 0.2f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Filled.EmojiEvents, contentDescription = null, tint = Color(0xFFFFD54F), modifier = Modifier.size(44.dp))
                                }
                                Spacer(Modifier.height(16.dp))
                                Text(
                                    text = if (tr) "Senin İzleme Kişiliğin:" else "Your Viewing Persona:",
                                    color = Color.White.copy(alpha = 0.85f),
                                    fontSize = 15.sp
                                )
                                Spacer(Modifier.height(8.dp))
                                Text(
                                    text = personaTitle,
                                    color = Color.White,
                                    fontSize = 26.sp,
                                    lineHeight = 30.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    textAlign = TextAlign.Center
                                )
                                Spacer(Modifier.height(12.dp))
                                Text(
                                    text = if (tr) "En çok $topDay günleri ve $timePreference vaktinde izledin!" else "Most active on $topDay during $timePreference!",
                                    color = Color.White.copy(alpha = 0.9f),
                                    fontSize = 14.sp,
                                    lineHeight = 18.sp,
                                    textAlign = TextAlign.Center
                                )
                            }
                            4 -> {
                                Text(
                                    text = if (tr) "İzleme Dağılımın 📊" else "Your Watch Mix 📊",
                                    color = Color.White,
                                    fontSize = 26.sp,
                                    fontWeight = FontWeight.ExtraBold
                                )
                                Spacer(Modifier.height(20.dp))
                                Column(
                                    Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(18.dp))
                                        .background(Color.Black.copy(alpha = 0.35f))
                                        .padding(16.dp),
                                    verticalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    DistProgressBar(if (tr) "Canlı TV" else "Live TV", livePct, Color(0xFFEF5350))
                                    DistProgressBar(if (tr) "Filmler" else "Movies", moviePct, Color(0xFF00E5FF))
                                    DistProgressBar(if (tr) "Diziler" else "Series", seriesPct, Color(0xFFAB47BC))
                                }
                            }
                            5 -> {
                                Box(
                                    Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(22.dp))
                                        .background(Color.Black.copy(alpha = 0.45f))
                                        .border(1.dp, Color.White.copy(alpha = 0.25f), RoundedCornerShape(22.dp))
                                        .padding(18.dp)
                                ) {
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        Text(
                                            text = "✨ RAY WRAPPED • $periodLabel",
                                            color = Color(0xFF00E5FF),
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 12.sp
                                        )
                                        Text(
                                            text = personaTitle,
                                            color = Color.White,
                                            fontWeight = FontWeight.ExtraBold,
                                            fontSize = 20.sp,
                                            textAlign = TextAlign.Center
                                        )
                                        Text(
                                            text = "⏱️ $totalStr",
                                            color = Color.White,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 16.sp
                                        )
                                        Text(
                                            text = "⭐️ 1 Nolu Kanal: $topChannel\n📅 En Aktif Gün: $topDay",
                                            color = Color.White.copy(alpha = 0.85f),
                                            fontSize = 13.sp,
                                            textAlign = TextAlign.Center,
                                            lineHeight = 17.sp
                                        )
                                    }
                                }
                                Spacer(Modifier.height(20.dp))
                                Box(
                                    Modifier
                                        .clip(RoundedCornerShape(16.dp))
                                        .background(Color.White)
                                        .rayClickable(onClick = onShare)
                                        .padding(horizontal = 24.dp, vertical = 12.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Filled.Share, contentDescription = null, tint = Color.Black, modifier = Modifier.size(18.dp))
                                        Spacer(Modifier.width(8.dp))
                                        Text(
                                            text = if (tr) "Hikayeni Paylaş" else "Share Story",
                                            color = Color.Black,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 14.sp
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
}

private fun fmtDur(ms: Long, tr: Boolean): String {
    val h = TimeUnit.MILLISECONDS.toHours(ms)
    val m = TimeUnit.MILLISECONDS.toMinutes(ms) % 60
    return if (h > 0) {
        if (tr) "${h}s ${m}dk" else "${h}h ${m}m"
    } else {
        if (tr) "${m}dk" else "${m}m"
    }
}

private fun shareWrappedSummary(
    context: Context,
    periodLabel: String,
    totalStr: String,
    liveStr: String,
    vodStr: String,
    topChannel: String,
    persona: String,
    topDay: String,
    tr: Boolean
) {
    try {
        val summary = if (tr) {
            """
                ✨ Ray IPTV Wrapped ($periodLabel) 🚀
                
                ⏱️ Toplam İzleme: $totalStr
                📺 Canlı TV: $liveStr
                🎬 Film & Dizi: $vodStr
                ⭐️ 1 Numaralı Kanalım: $topChannel
                🏆 İzleme Tipim: $persona
                📅 En Aktif Günüm: $topDay
                
                🔥 Ray IPTV ile televizyon keyfini yaşa!
            """.trimIndent()
        } else {
            """
                ✨ Ray IPTV Wrapped ($periodLabel) 🚀
                
                ⏱️ Total Watch Time: $totalStr
                📺 Live TV: $liveStr
                🎬 Movies & Series: $vodStr
                ⭐️ My #1 Channel: $topChannel
                🏆 Persona: $persona
                📅 Most Active Day: $topDay
                
                🔥 Enjoy television with Ray IPTV!
            """.trimIndent()
        }

        val sendIntent = Intent().apply {
            action = Intent.ACTION_SEND
            putExtra(Intent.EXTRA_TEXT, summary)
            type = "text/plain"
        }
        val chooser = Intent.createChooser(sendIntent, if (tr) "Wrapped Özetini Paylaş" else "Share Wrapped Summary")
        context.startActivity(chooser)
    } catch (_: Exception) {}
}
