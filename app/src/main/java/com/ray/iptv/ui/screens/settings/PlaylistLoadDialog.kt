package com.ray.iptv.ui.screens.settings

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.LiveTv
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.PlaylistPlay
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.tv.material3.Icon
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.ray.iptv.data.repo.SyncState
import com.ray.iptv.ui.components.GlassButton
import com.ray.iptv.ui.glass.DarkGlassPopupTheme
import com.ray.iptv.ui.glass.GlassPanel
import com.ray.iptv.ui.theme.LocalGlass
import kotlinx.coroutines.delay

@Composable
fun PlaylistLoadDialog(
    sync: SyncState,
    tr: Boolean,
    onDismiss: () -> Unit
) {
    val visible = sync.catalog && (sync.running || sync.done || sync.error.isNotBlank())
    if (!visible) return

    val failed = sync.error.isNotBlank()
    val done = sync.done && !failed
    val g = LocalGlass.current
    val okFocus = remember { FocusRequester() }

    LaunchedEffect(sync.running, sync.done, sync.error) {
        if (done && !sync.running) {
            delay(2200)
            onDismiss()
        }
    }
    LaunchedEffect(done, failed) {
        if (done || failed) runCatching { okFocus.requestFocus() }
    }

    val title = when {
        failed -> if (tr) "Liste yüklenemedi" else "Playlist failed"
        done -> if (tr) "Liste başarıyla yüklendi" else "Playlist loaded"
        else -> if (tr) "Playlist yükleniyor" else "Loading playlist"
    }
    val subtitle = when {
        failed -> if (tr) "Bağlantıyı veya adresi kontrol edin." else "Check the connection or playlist URL."
        done -> if (tr) "Aşağıda yüklenen içeriklerin özetini görebilirsiniz." else "Here is a summary of what was loaded."
        else -> if (tr) "Canlı kanallar, filmler ve diziler hazırlanıyor…" else "Preparing live channels, movies and series…"
    }

    DarkGlassPopupTheme {
        val g = LocalGlass.current
        Dialog(
            onDismissRequest = { if (!sync.running) onDismiss() },
            properties = DialogProperties(
                dismissOnBackPress = !sync.running,
                dismissOnClickOutside = false,
                usePlatformDefaultWidth = false
            )
        ) {
            Box(
                Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.70f)),
                contentAlignment = Alignment.Center
            ) {
                GlassPanel(
                    strong = true,
                    radius = 14.dp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .widthIn(max = 420.dp)
                        .padding(horizontal = 18.dp)
                ) {
            Column(Modifier.padding(horizontal = 20.dp, vertical = 18.dp)) {
                Row(verticalAlignment = Alignment.Top) {
                    val accent = if (failed) g.danger else g.accent
                    Box(
                        Modifier
                            .size(46.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(accent.copy(alpha = 0.28f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            if (failed) Icons.Filled.ErrorOutline else Icons.Filled.PlaylistPlay,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(26.dp)
                        )
                    }
                    Spacer(Modifier.size(14.dp))
                    Column(Modifier.weight(1f)) {
                        Text(title, color = g.text, style = MaterialTheme.typography.headlineSmall)
                        Spacer(Modifier.height(4.dp))
                        Text(subtitle, color = g.muted, style = MaterialTheme.typography.bodyMedium)
                    }
                }
                Spacer(Modifier.height(16.dp))
                if (failed) {
                    Text(sync.error, color = g.text, style = MaterialTheme.typography.bodyMedium)
                } else {
                    LoadRow(
                        icon = Icons.Filled.LiveTv,
                        label = if (tr) "Canlı kanallar" else "Live channels",
                        accent = Color(0xFF6EC8FF),
                        count = sync.liveCount,
                        ready = sync.liveReady,
                        tr = tr
                    )
                    Spacer(Modifier.height(10.dp))
                    LoadRow(
                        icon = Icons.Filled.Movie,
                        label = if (tr) "Filmler" else "Movies",
                        accent = Color(0xFFFFC773),
                        count = sync.movieCount,
                        ready = sync.moviesReady,
                        tr = tr
                    )
                    Spacer(Modifier.height(10.dp))
                    LoadRow(
                        icon = Icons.Filled.VideoLibrary,
                        label = if (tr) "Diziler" else "Series",
                        accent = Color(0xFFB089FF),
                        count = sync.seriesCount,
                        ready = sync.seriesReady,
                        tr = tr
                    )
                }
                if (done || failed) {
                    Spacer(Modifier.height(16.dp))
                    Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.CenterEnd) {
                        GlassButton(
                            if (tr) "Tamam" else "OK",
                            modifier = Modifier.focusRequester(okFocus)
                        ) { onDismiss() }
                    }
                }
            }
        }
    }
}
}
}

@Composable
private fun LoadRow(
    icon: ImageVector,
    label: String,
    accent: Color,
    count: Int,
    ready: Boolean,
    tr: Boolean
) {
    val g = LocalGlass.current
    GlassPanel(strong = true, radius = 14.dp, modifier = Modifier.fillMaxWidth()) {
        Row(
            Modifier.padding(horizontal = 12.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(accent.copy(alpha = 0.22f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = accent, modifier = Modifier.size(20.dp))
            }
            Text(label, color = g.text, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
            if (ready) {
                Icon(Icons.Filled.Check, contentDescription = null, tint = accent, modifier = Modifier.size(18.dp))
                Text(
                    if (tr) "$count adet" else "$count items",
                    color = g.muted,
                    style = MaterialTheme.typography.bodyMedium
                )
            } else {
                TinySpinner(accent)
                Text(
                    if (tr) "Yükleniyor…" else "Loading…",
                    color = g.muted,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}

@Composable
private fun TinySpinner(color: Color) {
    val spin = rememberInfiniteTransition(label = "pl-spin")
    val deg by spin.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "pl-rot"
    )
    Canvas(Modifier.size(20.dp).rotate(deg)) {
        drawArc(
            color = color,
            startAngle = 16f,
            sweepAngle = 280f,
            useCenter = false,
            style = Stroke(width = 2.6.dp.toPx(), cap = StrokeCap.Round)
        )
    }
}
