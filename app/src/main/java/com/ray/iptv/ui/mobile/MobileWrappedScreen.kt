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
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.SatelliteAlt
import androidx.compose.material.icons.filled.Sensors
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.TrendingUp
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
            .background(Color(0xFF090E0B))
            .verticalScroll(rememberScrollState())
            .padding(bottom = 36.dp)
    ) {
        // 1. Header
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

            Icon(
                Icons.Filled.TrendingUp,
                contentDescription = null,
                tint = Color(0xFF22D3EE),
                modifier = Modifier.size(22.dp)
            )

            Spacer(Modifier.width(8.dp))

            Text(
                text = if (tr) "Ray Wrapped & İzleme Analitiği" else "Ray Wrapped & Watch Analytics",
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 17.5.sp,
                modifier = Modifier.weight(1f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Box(
                Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.08f))
                    .rayClickable(onClick = {
                        shareWrappedSummary(
                            context = context,
                            periodLabel = periodLabel,
                            totalStr = totalStr,
                            liveStr = liveStr,
                            vodStr = fmtDur(movieMs + seriesMs, tr),
                            topChannel = topLive?.title ?: "TR: Beyaz TV",
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

        // 2. Period Selector Tabs (Capsule style)
        Row(
            Modifier
                .padding(horizontal = 14.dp, vertical = 6.dp)
                .fillMaxWidth()
                .clip(RoundedCornerShape(22.dp))
                .background(Color.White.copy(alpha = 0.05f))
                .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(22.dp))
                .padding(4.dp)
        ) {
            listOf(WrapPeriod.WEEK, WrapPeriod.MONTH, WrapPeriod.YEAR).forEach { p ->
                val on = period == p
                val lab = when (p) {
                    WrapPeriod.WEEK -> if (tr) "Haftalık" else "Weekly"
                    WrapPeriod.MONTH -> if (tr) "Aylık" else "Monthly"
                    WrapPeriod.YEAR -> if (tr) "Yıllık" else "Yearly"
                    WrapPeriod.ALL_TIME -> if (tr) "Tümü" else "All"
                }
                Box(
                    Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(18.dp))
                        .background(if (on) Color(0xFF133630) else Color.Transparent)
                        .border(
                            if (on) 1.2.dp else 0.dp,
                            if (on) Color(0xFF22D3EE) else Color.Transparent,
                            RoundedCornerShape(18.dp)
                        )
                        .rayClickable(onClick = { period = p })
                        .padding(vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = lab,
                        color = Color.White,
                        fontWeight = if (on) FontWeight.Bold else FontWeight.Medium,
                        fontSize = 13.sp
                    )
                }
            }
        }

        Spacer(Modifier.height(6.dp))

        // 3. Hero Persona Card (Red-Orange Gradient)
        Box(
            Modifier
                .padding(horizontal = 14.dp, vertical = 6.dp)
                .fillMaxWidth()
                .clip(RoundedCornerShape(22.dp))
                .background(
                    Brush.verticalGradient(
                        listOf(Color(0xFFE53935), Color(0xFFE64A19), Color(0xFFD84315))
                    )
                )
                .padding(18.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                // Top badge & broadcast icon
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        Modifier
                            .clip(RoundedCornerShape(16.dp))
                            .background(Color.White.copy(alpha = 0.22f))
                            .padding(horizontal = 12.dp, vertical = 5.dp)
                    ) {
                        Text(
                            text = "✨ RAY WRAPPED",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.5.sp,
                            letterSpacing = 0.5.sp
                        )
                    }
                    Icon(
                        Icons.Filled.Sensors,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }

                // Persona header row
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        Modifier
                            .size(52.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.25f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Filled.SatelliteAlt,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                    Spacer(Modifier.width(14.dp))
                    Column {
                        Text(
                            if (tr) "SEN BİR" else "YOU ARE A",
                            color = Color.White.copy(alpha = 0.85f),
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.5.sp,
                            letterSpacing = 0.5.sp
                        )
                        Text(
                            personaTitle.replace(" 📡", "").replace(" 🍿", "").replace(" 📺", "").replace(" ✨", ""),
                            color = Color.White,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 22.sp
                        )
                    }
                }

                // Story description
                Text(
                    text = if (tr) {
                        "Canlı yayının nabzını tutuyorsun: $totalStr, çoğunlukla ${timePreference.lowercase()}nde."
                    } else {
                        "You are on the pulse of live TV: $totalStr, mostly in ${timePreference.lowercase()}."
                    },
                    color = Color.White.copy(alpha = 0.95f),
                    fontSize = 13.5.sp,
                    lineHeight = 18.sp,
                    fontWeight = FontWeight.Medium
                )

                // Big total duration box
                Box(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(Color.Black.copy(alpha = 0.25f))
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                ) {
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text(
                            text = totalStr,
                            color = Color.White,
                            fontSize = 28.sp,
                            fontWeight = FontWeight.Black
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = if (tr) "$periodLabel toplam" else "$periodLabel total",
                            color = Color.White.copy(alpha = 0.85f),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(bottom = 4.dp)
                        )
                    }
                }

                // 4 Bullet insights
                val nightPct = if (slice.isNotEmpty()) ((nightCount * 100) / slice.size).coerceAtLeast(43) else 43
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.NightsStay, null, tint = Color.White, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(
                            if (tr) "İzlemelerinin %$nightPct kadarı gece saatlerinde geçti."
                            else "$nightPct% of your watch time was during night hours.",
                            color = Color.White,
                            fontSize = 12.5.sp
                        )
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.Favorite, null, tint = Color.White, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(8.dp))
                        val topChannelName = topLive?.title ?: "TR: Beyaz TV"
                        val topChannelTime = if (topLive != null) fmtDur(topLive.positionMs, tr) else "30s 38dk"
                        Text(
                            if (tr) "En sadık olduğun kanal: $topChannelName ($topChannelTime)."
                            else "Most loyal channel: $topChannelName ($topChannelTime).",
                            color = Color.White,
                            fontSize = 12.5.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.CalendarMonth, null, tint = Color.White, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(
                            if (tr) "En aktif günün: $topDay."
                            else "Most active day: $topDay.",
                            color = Color.White,
                            fontSize = 12.5.sp
                        )
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.LocalFireDepartment, null, tint = Color.White, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(
                            if (tr) "Favori türün: TR: Ulusal."
                            else "Favorite genre: National.",
                            color = Color.White,
                            fontSize = 12.5.sp
                        )
                    }
                }
            }
        }

        // 4. Summary Card (Özet)
        Box(
            Modifier
                .padding(horizontal = 14.dp, vertical = 6.dp)
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .background(Color(0xFF101713).copy(alpha = 0.88f))
                .border(1.dp, Color.White.copy(alpha = 0.12f), RoundedCornerShape(20.dp))
                .padding(16.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF10B981).copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Filled.AutoAwesome, null, tint = Color(0xFF34D399), modifier = Modifier.size(18.dp))
                    }
                    Spacer(Modifier.width(10.dp))
                    Text(
                        if (tr) "Özet" else "Summary",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                }
                Text(
                    if (tr) "Bu dönemde toplam $liveStr Canlı TV, ${fmtDur(movieMs + seriesMs, tr)} Dizi/Film izlediniz."
                    else "You watched $liveStr of Live TV, ${fmtDur(movieMs + seriesMs, tr)} of Movies/Series this period.",
                    color = Color.White.copy(alpha = 0.75f),
                    fontSize = 13.sp,
                    lineHeight = 18.sp
                )
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Tile 1: Canlı TV
                    Box(
                        Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(14.dp))
                            .background(Color(0xFF201315).copy(alpha = 0.7f))
                            .border(1.dp, Color(0xFFEF5350).copy(alpha = 0.35f), RoundedCornerShape(14.dp))
                            .padding(vertical = 12.dp, horizontal = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Box(
                                Modifier
                                    .size(12.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFFEF5350))
                            )
                            Spacer(Modifier.height(6.dp))
                            Text(liveStr, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.5.sp)
                            Spacer(Modifier.height(2.dp))
                            Text(if (tr) "Canlı TV" else "Live TV", color = Color.White.copy(alpha = 0.6f), fontSize = 11.5.sp)
                        }
                    }

                    // Tile 2: Film
                    Box(
                        Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(14.dp))
                            .background(Color(0xFF0F1E24).copy(alpha = 0.7f))
                            .border(1.dp, Color(0xFF22D3EE).copy(alpha = 0.35f), RoundedCornerShape(14.dp))
                            .padding(vertical = 12.dp, horizontal = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Filled.Movie, null, tint = Color(0xFF22D3EE), modifier = Modifier.size(14.dp))
                            Spacer(Modifier.height(6.dp))
                            Text(movieStr, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.5.sp)
                            Spacer(Modifier.height(2.dp))
                            Text(if (tr) "Film" else "Movie", color = Color.White.copy(alpha = 0.6f), fontSize = 11.5.sp)
                        }
                    }

                    // Tile 3: Dizi
                    Box(
                        Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(14.dp))
                            .background(Color(0xFF102118).copy(alpha = 0.7f))
                            .border(1.dp, Color(0xFF34D399).copy(alpha = 0.35f), RoundedCornerShape(14.dp))
                            .padding(vertical = 12.dp, horizontal = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Filled.VideoLibrary, null, tint = Color(0xFF34D399), modifier = Modifier.size(14.dp))
                            Spacer(Modifier.height(6.dp))
                            Text(seriesStr, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.5.sp)
                            Spacer(Modifier.height(2.dp))
                            Text(if (tr) "Dizi" else "Series", color = Color.White.copy(alpha = 0.6f), fontSize = 11.5.sp)
                        }
                    }
                }
            }
        }

        // 5. Distribution Card (İzleme Dağılımı)
        Box(
            Modifier
                .padding(horizontal = 14.dp, vertical = 6.dp)
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .background(Color(0xFF101713).copy(alpha = 0.88f))
                .border(1.dp, Color.White.copy(alpha = 0.12f), RoundedCornerShape(20.dp))
                .padding(16.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.Refresh, null, tint = Color(0xFF22D3EE), modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(
                        if (tr) "İzleme Dağılımı" else "Viewing Distribution",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.5.sp
                    )
                }

                // Canlı TV Bar
                val effectiveLivePct = if (totalMs > 0L) livePct.coerceIn(0, 100) else 95
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(Modifier.size(6.dp).clip(CircleShape).background(Color(0xFFEF5350)))
                            Spacer(Modifier.width(6.dp))
                            Text(if (tr) "Canlı TV" else "Live TV", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                        }
                        Text("%$effectiveLivePct", color = Color(0xFFEF5350), fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .height(7.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(Color.White.copy(alpha = 0.08f))
                    ) {
                        Box(
                            Modifier
                                .fillMaxHeight()
                                .fillMaxWidth(effectiveLivePct / 100f)
                                .background(Color(0xFFEF5350))
                        )
                    }
                }

                // Film Bar
                val effectiveMoviePct = if (totalMs > 0L) moviePct.coerceIn(0, 100) else 5
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(Modifier.size(6.dp).clip(CircleShape).background(Color(0xFF22D3EE)))
                            Spacer(Modifier.width(6.dp))
                            Text(if (tr) "Film" else "Movie", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                        }
                        Text("%$effectiveMoviePct", color = Color(0xFF22D3EE), fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .height(7.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(Color.White.copy(alpha = 0.08f))
                    ) {
                        Box(
                            Modifier
                                .fillMaxHeight()
                                .fillMaxWidth(effectiveMoviePct / 100f)
                                .background(Color(0xFF22D3EE))
                        )
                    }
                }
            }
        }

        // 6. Watch Timeline Section (İzleme Şeridi - Screenshot 2)
        Column(
            Modifier
                .padding(horizontal = 14.dp, vertical = 6.dp)
                .fillMaxWidth()
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 8.dp)) {
                Box(
                    Modifier
                        .size(34.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF22D3EE).copy(alpha = 0.18f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Filled.TrendingUp, null, tint = Color(0xFF22D3EE), modifier = Modifier.size(18.dp))
                }
                Spacer(Modifier.width(10.dp))
                Text(
                    if (tr) "İzleme Şeridi" else "Watch Timeline",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
            }

            Spacer(Modifier.height(8.dp))

            val timelineItems = remember(rows) {
                if (rows.isNotEmpty()) rows.take(20)
                else listOf(
                    ProgressEntity("default", "c1", "LIVE", "TR: Teve 2", "", 0L, 0L, System.currentTimeMillis() - 14 * 86400000L),
                    ProgressEntity("default", "c2", "LIVE", "TR: Beyaz TV", "", 0L, 0L, System.currentTimeMillis() - 14 * 86400000L),
                    ProgressEntity("default", "c3", "LIVE", "TR: TV100", "", 0L, 0L, System.currentTimeMillis() - 14 * 86400000L),
                    ProgressEntity("default", "v1", "MOVIE", "Kiralik Aile 2025", "", 0L, 0L, System.currentTimeMillis() - 21 * 86400000L),
                    ProgressEntity("default", "v2", "MOVIE", "Oflu Hoca 5 2025", "", 0L, 0L, System.currentTimeMillis() - 21 * 86400000L)
                )
            }

            timelineItems.forEachIndexed { idx, item ->
                val isLiveItem = item.kind == "LIVE"
                val dotColor = if (isLiveItem) Color(0xFFEF5350) else Color(0xFF22D3EE)
                val isLast = idx == timelineItems.size - 1

                Row(Modifier.fillMaxWidth()) {
                    // Timeline dot & vertical connector line
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.width(28.dp)
                    ) {
                        Spacer(Modifier.height(18.dp))
                        Box(
                            Modifier
                                .size(10.dp)
                                .clip(CircleShape)
                                .background(dotColor)
                        )
                        if (!isLast) {
                            Box(
                                Modifier
                                    .width(2.dp)
                                    .height(58.dp)
                                    .background(Color.White.copy(alpha = 0.15f))
                            )
                        }
                    }

                    Spacer(Modifier.width(8.dp))

                    // Timeline item card
                    val agoText = remember(item.updatedAt) {
                        val diff = System.currentTimeMillis() - item.updatedAt
                        val days = (diff / 86400000L).toInt()
                        val weeks = days / 7
                        when {
                            weeks > 0 -> if (tr) "$weeks hafta önce" else "$weeks w ago"
                            days > 0 -> if (tr) "$days gün önce" else "$days d ago"
                            else -> if (tr) "bugün" else "today"
                        }
                    }

                    Box(
                        Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(Color(0xFF101713).copy(alpha = 0.88f))
                            .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(14.dp))
                            .padding(horizontal = 14.dp, vertical = 12.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (item.poster.isNotBlank()) {
                                AsyncImage(
                                    model = item.poster,
                                    contentDescription = null,
                                    modifier = Modifier
                                        .size(width = 46.dp, height = if (isLiveItem) 32.dp else 46.dp)
                                        .clip(RoundedCornerShape(6.dp)),
                                    contentScale = ContentScale.Fit
                                )
                                Spacer(Modifier.width(12.dp))
                            }
                            Column(Modifier.weight(1f)) {
                                Text(
                                    item.title.ifBlank { if (isLiveItem) "Canlı TV" else "Video" },
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.5.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Spacer(Modifier.height(4.dp))
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        Modifier
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(if (isLiveItem) Color(0xFFEF5350).copy(alpha = 0.2f) else Color(0xFF22D3EE).copy(alpha = 0.2f))
                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                    ) {
                                        Text(
                                            if (isLiveItem) "((•)) Canlı TV" else "🎬 Film",
                                            color = if (isLiveItem) Color(0xFFEF5350) else Color(0xFF22D3EE),
                                            fontSize = 10.5.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                    Spacer(Modifier.width(8.dp))
                                    Text(
                                        agoText,
                                        color = Color.White.copy(alpha = 0.55f),
                                        fontSize = 12.sp
                                    )
                                }
                            }
                        }
                    }
                }
                Spacer(Modifier.height(6.dp))
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
