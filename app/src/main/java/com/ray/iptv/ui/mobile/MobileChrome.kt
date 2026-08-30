package com.ray.iptv.ui.mobile

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlinx.coroutines.delay
import androidx.compose.animation.animateColor
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.PlaylistPlay
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Forum
import androidx.compose.material.icons.filled.LiveTv
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.ViewTimeline
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathMeasure
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Icon
import androidx.tv.material3.Text
import coil.compose.AsyncImage
import com.ray.iptv.R
import com.ray.iptv.data.repo.DockbarStyle
import com.ray.iptv.ui.Dest
import com.ray.iptv.ui.components.tickingClock
import com.ray.iptv.ui.input.rayClickable
import com.ray.iptv.ui.theme.LocalGlass
import com.ray.iptv.ui.theme.capsuleForeground
import com.ray.iptv.ui.theme.capsuleGradient
import com.ray.iptv.ui.theme.capsuleStroke
import com.ray.iptv.ui.theme.dockFill
import com.ray.iptv.ui.theme.sectionGradient
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

val MobileCyan: Color
    @Composable get() = LocalGlass.current.accent
val MobileLiveRed = Color(0xFFE53935)
val MobileSeriesPurple = Color(0xFF7C4DFF)
val MobileGlass = Color(0xD10C0C0C)
val MobileCard = Color(0xCC121614)

@Composable
fun mobileShortDate(tr: Boolean): String {
    val clock = tickingClock()
    val loc = if (tr) Locale("tr", "TR") else Locale.getDefault()
    val line = remember(clock, loc) {
        SimpleDateFormat("EEE d MMM", loc).format(Date())
    }
    return line.replaceFirstChar { it.uppercase(loc) }
}

@Composable
fun MobileGlassCapsule(
    modifier: Modifier = Modifier,
    radius: Dp = 14.dp,
    content: @Composable RowScope.() -> Unit
) {
    val shape = RoundedCornerShape(radius)
    val g = LocalGlass.current
    Row(
        modifier
            .height(56.dp)
            .background(Brush.linearGradient(g.capsuleGradient()), shape)
            .border(1.dp, g.capsuleStroke(), shape)
            .padding(horizontal = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        content = content
    )
}

@Composable
fun RayAnimatedUmbrella(
    modifier: Modifier = Modifier.size(32.dp)
) {
    val infiniteTransition = rememberInfiniteTransition(label = "umbrella-anim")
    val canopyGreen = Color(0xFF00E676)
    val canopyBlue = Color(0xFF29B6F6)

    val umbrellaColor by infiniteTransition.animateColor(
        initialValue = canopyGreen,
        targetValue = canopyBlue,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 10_000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "umbrella-color"
    )

    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height

        // Sap (J-hook)
        val shaftPath = Path().apply {
            moveTo(w * 0.50f, h * 0.20f)
            lineTo(w * 0.50f, h * 0.78f)
            cubicTo(
                w * 0.50f, h * 0.94f,
                w * 0.32f, h * 0.94f,
                w * 0.32f, h * 0.82f
            )
        }
        drawPath(
            path = shaftPath,
            color = Color.White.copy(alpha = 0.92f),
            style = Stroke(width = w * 0.075f, cap = StrokeCap.Round)
        )

        // Gölgelik (3 dilim)
        val colorLeft = umbrellaColor
        val colorCenter = Color.White.copy(alpha = 0.92f)
        val colorRight = umbrellaColor.copy(alpha = 0.82f)

        // Sol Panel
        val leftPanel = Path().apply {
            moveTo(w * 0.50f, h * 0.08f)
            cubicTo(w * 0.28f, h * 0.10f, w * 0.05f, h * 0.32f, w * 0.04f, h * 0.56f)
            quadraticTo(w * 0.18f, h * 0.49f, w * 0.33f, h * 0.56f)
            cubicTo(w * 0.38f, h * 0.36f, w * 0.44f, h * 0.18f, w * 0.50f, h * 0.08f)
            close()
        }
        drawPath(leftPanel, color = colorLeft)

        // Orta Panel
        val centerPanel = Path().apply {
            moveTo(w * 0.50f, h * 0.08f)
            cubicTo(w * 0.44f, h * 0.18f, w * 0.38f, h * 0.36f, w * 0.33f, h * 0.56f)
            quadraticTo(w * 0.50f, h * 0.48f, w * 0.67f, h * 0.56f)
            cubicTo(w * 0.62f, h * 0.36f, w * 0.56f, h * 0.18f, w * 0.50f, h * 0.08f)
            close()
        }
        drawPath(centerPanel, color = colorCenter)

        // Sağ Panel
        val rightPanel = Path().apply {
            moveTo(w * 0.50f, h * 0.08f)
            cubicTo(w * 0.56f, h * 0.18f, w * 0.62f, h * 0.36f, w * 0.67f, h * 0.56f)
            quadraticTo(w * 0.82f, h * 0.49f, w * 0.96f, h * 0.56f)
            cubicTo(w * 0.95f, h * 0.32f, w * 0.72f, h * 0.10f, w * 0.50f, h * 0.08f)
            close()
        }
        drawPath(rightPanel, color = colorRight)
    }
}

@Composable
fun MobileBrandChip(onTap: () -> Unit) {
    val fg = LocalGlass.current.capsuleForeground()
    MobileGlassCapsule(Modifier.rayClickable(onTap)) {
        RayAnimatedUmbrella(
            modifier = Modifier.size(32.dp)
        )
        Spacer(Modifier.width(8.dp))
        Column {
            Text("Ray Player", color = fg, fontSize = 14.sp, fontWeight = FontWeight.Bold, lineHeight = 16.sp)
            Text("IPTV Player", color = fg.copy(alpha = 0.88f), fontSize = 12.sp, lineHeight = 14.sp)
        }
    }
}

@Composable
fun MobileClockSettingsChip(
    tr: Boolean,
    onSettings: () -> Unit,
    onSearch: (() -> Unit)? = null,
    onList: (() -> Unit)? = null,
    onPlaylist: (() -> Unit)? = null,
    onChat: (() -> Unit)? = null,
    avatarUrl: String? = null
) {
    val time = tickingClock()
    val date = mobileShortDate(tr)
    val fg = LocalGlass.current.capsuleForeground()

    var showAvatar by remember(avatarUrl) { mutableStateOf(false) }
    LaunchedEffect(avatarUrl) {
        if (!avatarUrl.isNullOrBlank()) {
            while (true) {
                delay(15_000L)
                showAvatar = !showAvatar
            }
        } else {
            showAvatar = false
        }
    }

    MobileGlassCapsule {
        if (onChat != null) {
            Icon(
                Icons.Filled.Forum,
                contentDescription = if (tr) "Topluluk Sohbeti" else "Community Chat",
                tint = Color(0xFF38BDF8),
                modifier = Modifier.size(22.dp).rayClickable(onChat)
            )
            Spacer(Modifier.width(8.dp))
        }
        if (onPlaylist != null) {
            Icon(
                Icons.AutoMirrored.Filled.PlaylistPlay,
                null,
                tint = fg,
                modifier = Modifier.size(24.dp).rayClickable(onPlaylist)
            )
            Spacer(Modifier.width(8.dp))
        }
        if (onSearch != null) {
            Icon(
                Icons.Filled.Search,
                null,
                tint = fg,
                modifier = Modifier.size(22.dp).rayClickable(onSearch)
            )
            Spacer(Modifier.width(8.dp))
        }
        if (onList != null) {
            Icon(
                Icons.Filled.ViewTimeline,
                null,
                tint = fg,
                modifier = Modifier.size(20.dp).rayClickable(onList)
            )
            Spacer(Modifier.width(8.dp))
        }
        Box(Modifier.width(1.dp).height(28.dp).background(fg.copy(alpha = 0.22f)))
        Spacer(Modifier.width(8.dp))
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(time, color = fg, fontSize = 20.sp, fontWeight = FontWeight.Bold, lineHeight = 20.sp)
            Text(date, color = fg.copy(alpha = 0.88f), fontSize = 11.sp, lineHeight = 13.sp)
        }
        Spacer(Modifier.width(8.dp))
        Box(Modifier.width(1.dp).height(28.dp).background(fg.copy(alpha = 0.22f)))
        Box(
            Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(fg.copy(alpha = 0.12f))
                .rayClickable(onSettings),
            contentAlignment = Alignment.Center
        ) {
            AnimatedContent(
                targetState = showAvatar && !avatarUrl.isNullOrBlank(),
                transitionSpec = {
                    (fadeIn(animationSpec = tween(400)) + scaleIn(initialScale = 0.82f, animationSpec = tween(400))) togetherWith
                    (fadeOut(animationSpec = tween(400)) + scaleOut(targetScale = 0.82f, animationSpec = tween(400)))
                },
                label = "settings-avatar-toggle"
            ) { isAvatar ->
                if (isAvatar && !avatarUrl.isNullOrBlank()) {
                    AsyncImage(
                        model = avatarUrl,
                        contentDescription = "Profile",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(26.dp)
                            .clip(CircleShape)
                            .border(1.dp, fg.copy(alpha = 0.45f), CircleShape)
                    )
                } else {
                    Icon(
                        Icons.Filled.Settings,
                        null,
                        tint = fg,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun MobileSettingsTopBar(
    title: String,
    onBack: () -> Unit
) {
    val g = LocalGlass.current
    val fg = g.capsuleForeground()
    val cap = Modifier
        .clip(RoundedCornerShape(14.dp))
        .background(Brush.linearGradient(g.capsuleGradient()), RoundedCornerShape(14.dp))
        .border(1.dp, g.capsuleStroke(), RoundedCornerShape(14.dp))
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            cap.rayClickable(onBack).padding(start = 10.dp, end = 16.dp, top = 8.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = fg, modifier = Modifier.size(22.dp))
            Spacer(Modifier.width(8.dp))
            Text(title, color = fg, fontSize = 18.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        Spacer(Modifier.weight(1f))
        Row(
            cap.padding(start = 16.dp, end = 12.dp, top = 8.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(tickingClock(), color = fg, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.width(10.dp))
            Box(Modifier.width(1.dp).height(18.dp).background(fg.copy(alpha = 0.25f)))
            Spacer(Modifier.width(10.dp))
            Image(
                painter = painterResource(R.drawable.ic_launcher_foreground),
                contentDescription = null,
                contentScale = ContentScale.Fit,
                modifier = Modifier.size(22.dp)
            )
        }
    }
}

@Composable
fun MobileTopBar(
    tr: Boolean,
    showBack: Boolean,
    onBack: () -> Unit,
    onBrand: () -> Unit,
    onSettings: () -> Unit,
    onSearch: (() -> Unit)? = null,
    onList: (() -> Unit)? = null,
    onPlaylist: (() -> Unit)? = null,
    onChat: (() -> Unit)? = null,
    avatarUrl: String? = null
) {
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        if (showBack) {
            val fg = LocalGlass.current.capsuleForeground()
            MobileGlassCapsule(Modifier.rayClickable(onBack), radius = 14.dp) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = fg, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(6.dp))
                Image(
                    painter = painterResource(R.drawable.ic_launcher_foreground),
                    contentDescription = null,
                    modifier = Modifier.size(28.dp)
                )
            }
        } else {
            MobileBrandChip(onBrand)
        }
        Spacer(Modifier.width(8.dp))
        MobileClockSettingsChip(tr, onSettings, onSearch, onList, onPlaylist, onChat, avatarUrl)
    }
}

private data class DockItem(val dest: Dest?, val icon: ImageVector, val label: String, val accent: Color, val guide: Boolean = false)

/** Mina `_BorderShimmerPainter`: pill çerçevede yürüyen beyaz neon nokta. */
private fun Modifier.walkingNeonBorder(
    progress: Float,
    corner: Dp,
    phase: Float = 0f
): Modifier = drawWithContent {
    drawContent()
    val radius = corner.toPx().coerceAtMost(size.minDimension / 2f)
    val stroke = 2.dp.toPx()
    val spot = 32.dp.toPx()
    val path = Path().apply {
        addRoundRect(
            RoundRect(0f, 0f, size.width, size.height, CornerRadius(radius, radius))
        )
    }
    val measure = PathMeasure()
    measure.setPath(path, false)
    val total = measure.length
    if (total <= 0f) return@drawWithContent
    val t = ((progress + phase) % 1f + 1f) % 1f
    val pos = measure.getPosition(t * total)
    if (pos.x.isNaN() || pos.y.isNaN()) return@drawWithContent
    val clip = Path().apply {
        addOval(
            androidx.compose.ui.geometry.Rect(
                pos.x - spot,
                pos.y - spot,
                pos.x + spot,
                pos.y + spot
            )
        )
    }
    clipPath(clip) {
        drawPath(
            path = path,
            brush = Brush.radialGradient(
                colorStops = arrayOf(
                    0f to Color.White.copy(alpha = 0.75f),
                    0.45f to Color.White.copy(alpha = 0.25f),
                    1f to Color.White.copy(alpha = 0f)
                ),
                center = pos,
                radius = spot
            ),
            style = Stroke(width = stroke, cap = StrokeCap.Round)
        )
    }
}

@Composable
fun MobileDockBar(
    tr: Boolean,
    current: Dest,
    lastLogo: String,
    onGo: (Dest) -> Unit,
    onSearch: () -> Unit,
    onLastWatched: () -> Unit,
    onGuide: () -> Unit,
    style: DockbarStyle = DockbarStyle.ORIGINAL,
    showLastWatched: Boolean = true
) {
    val g = LocalGlass.current
    val barH = 64.dp
    val barR = 34.dp
    val capsule = style == DockbarStyle.CAPSULE || style == DockbarStyle.MODERN_GLASS
    val barBg = if (style == DockbarStyle.MODERN_GLASS) g.panelStrong.copy(alpha = 0.88f) else g.dockFill()
    val barStroke = g.capsuleStroke().copy(alpha = 0.35f)
    val barShape = if (capsule) RoundedCornerShape(barR) else CircleShape
    val neon = rememberInfiniteTransition(label = "dockNeon")
    val neonProgress by neon.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 10_000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "dockNeonProgress"
    )
    val searchPhase = when (style) {
        DockbarStyle.MODERN_GLASS -> 0.3f
        DockbarStyle.CAPSULE -> 0.5f
        else -> 0f
    }
    val items = listOf(
        DockItem(Dest.LIVE, Icons.Filled.LiveTv, if (tr) "Canlı TV" else "Live TV", Color(0xFFEF5350)),
        DockItem(Dest.MOVIES, Icons.Filled.Movie, if (tr) "Film & Dizi" else "Movies & Series", Color(0xFFFFC107)),
        DockItem(null, Icons.Filled.ViewTimeline, if (tr) "Tekrar & EPG" else "Replay & EPG", Color(0xFFAB47BC), guide = true),
        DockItem(Dest.WRAPPED, Icons.Filled.AutoAwesome, if (tr) "Wrapped" else "Wrapped", Color(0xFF26C6DA))
    )
    Row(
        Modifier.fillMaxWidth().padding(start = 14.dp, end = 14.dp, bottom = 16.dp),
        verticalAlignment = Alignment.Bottom,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(
            Modifier
                .weight(1f)
                .height(barH)
                .walkingNeonBorder(neonProgress, barR)
                .clip(RoundedCornerShape(barR))
                .background(barBg)
                .border(1.dp, barStroke, RoundedCornerShape(barR))
                .padding(
                    horizontal = if (style == DockbarStyle.MODERN_GLASS) 6.dp else 0.dp,
                    vertical = if (style == DockbarStyle.MODERN_GLASS) 5.dp else 0.dp
                ),
            verticalAlignment = Alignment.CenterVertically
        ) {
            items.forEach { item ->
                val on = item.dest != null && current == item.dest
                val cell = Modifier
                    .weight(1f)
                    .then(
                        if (style == DockbarStyle.MODERN_GLASS) {
                            Modifier
                                .padding(horizontal = 2.dp)
                                .clip(RoundedCornerShape(24.dp))
                                .background(if (on) item.accent.copy(alpha = 0.2f) else Color.White.copy(alpha = 0.06f))
                                .border(1.dp, if (on) item.accent.copy(alpha = 0.6f) else Color.White.copy(alpha = 0.12f), RoundedCornerShape(24.dp))
                        } else Modifier
                    )
                    .rayClickable(onClick = {
                        if (item.guide) onGuide() else item.dest?.let(onGo)
                    })
                    .padding(vertical = 8.dp)
                Column(cell, horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(item.icon, null, tint = if (on) item.accent else Color.White, modifier = Modifier.size(22.dp))
                    Spacer(Modifier.height(4.dp))
                    Text(
                        item.label,
                        color = if (on) item.accent else Color.White.copy(alpha = 0.9f),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
        Column(
            Modifier.width(IntrinsicSize.Max),
            horizontalAlignment = Alignment.End,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (showLastWatched) {
                Box(
                    Modifier
                        .then(if (capsule) Modifier.fillMaxWidth().height(barH) else Modifier.size(barH))
                        .clip(barShape)
                        .background(barBg)
                        .border(1.dp, barStroke, barShape)
                        .rayClickable(onLastWatched),
                    contentAlignment = Alignment.Center
                ) {
                    if (lastLogo.isNotBlank()) {
                        AsyncImage(lastLogo, null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                    } else {
                        Icon(Icons.Filled.LiveTv, null, tint = Color.White, modifier = Modifier.size(28.dp))
                    }
                }
            }
            if (capsule) {
                Row(
                    Modifier
                        .height(barH)
                        .widthIn(min = barH)
                        .walkingNeonBorder(neonProgress, barR, searchPhase)
                        .clip(RoundedCornerShape(barR))
                        .background(barBg)
                        .border(1.dp, barStroke, RoundedCornerShape(barR))
                        .rayClickable(onSearch)
                        .padding(horizontal = 18.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(Icons.Filled.Search, null, tint = Color.White, modifier = Modifier.size(22.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(if (tr) "Ara" else "Search", color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                }
            } else {
                Box(
                    Modifier
                        .size(barH)
                        .clip(CircleShape)
                        .background(barBg)
                        .border(1.dp, barStroke, CircleShape)
                        .rayClickable(onSearch),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Filled.Search, null, tint = Color.White, modifier = Modifier.size(26.dp))
                }
            }
        }
    }
}

@Composable
fun MobileSectionFrame(content: @Composable () -> Unit) {
    val g = LocalGlass.current
    val shape = RoundedCornerShape(20.dp)
    Box(
        Modifier
            .fillMaxWidth()
            .padding(start = 12.dp, end = 12.dp, top = 14.dp)
            .shadow(
                elevation = 4.dp,
                shape = shape,
                clip = false,
                ambientColor = Color.Black.copy(alpha = 0.18f),
                spotColor = Color.Black.copy(alpha = 0.18f)
            )
            .background(Brush.linearGradient(g.sectionGradient()), shape)
            .border(1.dp, g.capsuleStroke().copy(alpha = 0.45f), shape)
            .padding(top = 12.dp, bottom = 14.dp)
    ) { content() }
}

@Composable
fun MobileBadge(text: String, color: Color, modifier: Modifier = Modifier) {
    Text(
        text,
        color = Color.White,
        fontSize = 10.sp,
        fontWeight = FontWeight.ExtraBold,
        modifier = modifier
            .clip(RoundedCornerShape(6.dp))
            .background(color)
            .padding(horizontal = 6.dp, vertical = 3.dp)
    )
}
