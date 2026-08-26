package com.ray.iptv.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.onFocusChanged
import com.ray.iptv.ui.input.rayFocusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Icon
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import coil.compose.AsyncImage
import com.ray.iptv.ui.glass.GlassPanel
import com.ray.iptv.ui.theme.LocalGlass

@Composable
fun PosterCard(
    title: String,
    image: String,
    subtitle: String = "",
    width: Dp = 148.dp,
    poster: Boolean = true,
    progress: Float = -1f,
    onClick: () -> Unit,
    onLongClick: (() -> Unit)? = null
) {
    var focused by remember { mutableStateOf(false) }
    val g = LocalGlass.current
    GlassPanel(
        focused = focused,
        radius = 16.dp,
        onClick = onClick,
        onLongClick = onLongClick,
        modifier = Modifier
            .width(width)
            .onFocusChanged { focused = it.isFocused }
    ) {
            Column {
                Box(
                    Modifier
                        .fillMaxWidth()
                        .aspectRatio(if (poster) 2f / 3f else 16f / 9f)
                        .clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
                        .background(g.panelStrong)
                ) {
                    if (image.isNotBlank()) {
                        AsyncImage(
                            model = image,
                            contentDescription = title,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    }
                    Box(
                        Modifier
                            .fillMaxSize()
                            .background(
                                Brush.verticalGradient(
                                    listOf(Color.Transparent, Color.Black.copy(alpha = 0.45f))
                                )
                            )
                    )
                    if (progress in 0f..1f) {
                        Box(
                            Modifier
                                .align(Alignment.BottomStart)
                                .fillMaxWidth()
                                .height(3.dp)
                                .background(g.muted.copy(alpha = 0.35f))
                        ) {
                            Box(
                                Modifier
                                    .fillMaxWidth(progress)
                                    .height(3.dp)
                                    .background(g.accent)
                            )
                        }
                    }
                }
                Text(
                    title,
                    style = MaterialTheme.typography.titleMedium,
                    color = g.text,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp)
                )
                if (subtitle.isNotBlank()) {
                    Text(
                        subtitle,
                        style = MaterialTheme.typography.bodyMedium,
                        color = g.muted,
                        maxLines = 1,
                        modifier = Modifier.padding(horizontal = 10.dp).padding(bottom = 10.dp)
                    )
                } else {
                    Spacer(Modifier.height(4.dp))
                }
            }
        }
}

@Composable
fun GlassChip(label: String, selected: Boolean, onClick: () -> Unit) {
    var focused by remember { mutableStateOf(false) }
    val g = LocalGlass.current
    GlassPanel(
        focused = focused,
        strong = true,
        accentFill = selected,
        radius = 22.dp,
        scaleOnFocus = false,
        onClick = onClick,
        modifier = Modifier.onFocusChanged { focused = it.isFocused }
    ) {
        Text(
            label,
            color = if (selected) Color.White else g.text,
            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            maxLines = 1
        )
    }
}

private val primaryLabels = setOf(
    "Kaydet", "Save", "Oynat", "Play", "OK", "Tamam", "İlerle", "Continue",
    "Kurulumu bitir", "Finish setup", "Listeyi Yükle", "Load list", "Devam",
    "Resume", "Watch", "Catch-up", "İzle"
)
private val destructiveLabels = setOf("Sil", "Del", "Delete", "Tümünü sil")

@Composable
fun GlassButton(
    label: String,
    modifier: Modifier = Modifier,
    primary: Boolean = false,
    destructive: Boolean = false,
    compact: Boolean = false,
    selected: Boolean = false,
    icon: ImageVector? = null,
    focusRequester: FocusRequester? = null,
    onClick: () -> Unit
) {
    var focused by remember { mutableStateOf(false) }
    val g = LocalGlass.current
    val glyph = label.length <= 2
    val tight = compact || glyph
    val filled = primary || selected || label.contains('✓') || label in primaryLabels
    val danger = destructive || label in destructiveLabels
    GlassPanel(
        focused = focused,
        strong = true,
        accentFill = filled && !danger,
        radius = if (tight) 12.dp else 14.dp,
        scaleOnFocus = !glyph,
        onClick = onClick,
        modifier = modifier.rayFocusRequester(focusRequester).onFocusChanged { focused = it.isFocused }
    ) {
        val color = when {
            danger -> g.danger
            filled -> Color.White
            focused -> g.accent
            else -> g.text
        }
        Row(
            modifier = Modifier
                .align(Alignment.Center)
                .padding(
                    horizontal = if (tight) 12.dp else 18.dp,
                    vertical = if (tight) 8.dp else 12.dp
                ),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            if (icon != null) {
                Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(16.dp))
                if (label.isNotBlank() && !glyph) Spacer(Modifier.width(8.dp))
            }
            if (label.isNotBlank()) {
                Text(
                    label,
                    color = color,
                    style = MaterialTheme.typography.labelLarge.copy(
                        fontWeight = if (filled || focused) FontWeight.SemiBold else FontWeight.Medium,
                        fontSize = if (tight) MaterialTheme.typography.labelLarge.fontSize else 16.sp
                    ),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
fun GlassToggle(checked: Boolean, modifier: Modifier = Modifier, onClick: (() -> Unit)? = null) {
    var focused by remember { mutableStateOf(false) }
    GlassPanel(
        focused = focused,
        strong = true,
        accentFill = checked,
        radius = 16.dp,
        scaleOnFocus = false,
        onClick = onClick,
        modifier = modifier
            .width(52.dp)
            .height(32.dp)
            .onFocusChanged { focused = it.isFocused }
    ) {
        Box(Modifier.fillMaxSize().padding(3.dp)) {
            Box(
                Modifier
                    .size(26.dp)
                    .align(if (checked) Alignment.CenterEnd else Alignment.CenterStart)
                    .clip(CircleShape)
                    .background(Color.White)
            )
        }
    }
}

@Composable
fun tickingClock(): String {
    var now by remember { mutableStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) {
        while (true) {
            now = System.currentTimeMillis()
            val untilNextMinute = 60_000L - (now % 60_000L)
            kotlinx.coroutines.delay(untilNextMinute.coerceIn(1_000L, 60_000L))
        }
    }
    return java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault()).format(java.util.Date(now))
}
