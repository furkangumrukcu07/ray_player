package com.ray.iptv.ui.screens.home

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.ui.graphics.Color
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.History
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Icon
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import coil.compose.AsyncImage
import com.ray.iptv.data.local.ProgressEntity
import com.ray.iptv.ui.components.GlassButton
import com.ray.iptv.ui.glass.GlassPanel
import com.ray.iptv.ui.i18n.Copy
import com.ray.iptv.ui.input.LocalTouchUi
import com.ray.iptv.ui.theme.LocalGlass

@Composable
fun HomeScreen(
    copy: Copy,
    continueWatching: List<ProgressEntity>,
    onResume: (ProgressEntity) -> Unit,
    onDelete: (ProgressEntity) -> Unit,
    onExpandRail: () -> Unit,
    railExpanded: Boolean,
    onExit: () -> Unit
) {
    val g = LocalGlass.current
    val focus = LocalFocusManager.current
    var moviesTab by remember { mutableStateOf(true) }
    val movies = remember(continueWatching) { continueWatching.filter { it.kind == "MOVIE" } }
    val series = remember(continueWatching) {
        continueWatching.filter { it.kind == "EPISODE" || it.kind == "SERIES" }
    }
    val rows = if (moviesTab) movies else series

    BackHandler {
        if (railExpanded) onExit()
        else {
            onExpandRail()
            focus.moveFocus(FocusDirection.Left)
        }
    }

    GlassPanel(
        strong = true,
        radius = 22.dp,
        modifier = Modifier.fillMaxSize()
    ) {
        Column(Modifier.fillMaxSize().padding(horizontal = 22.dp, vertical = 16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.History, contentDescription = null, tint = g.accent, modifier = Modifier.size(28.dp))
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        copy.cont,
                        color = g.text,
                        style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold)
                    )
                    Text(copy.continueSubtitle, color = g.muted, style = MaterialTheme.typography.bodyMedium)
                }
            }
            Spacer(Modifier.height(18.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                GlassButton(
                    "${copy.movies} (${movies.size})",
                    selected = moviesTab,
                    modifier = Modifier.onPreviewKeyEvent { e ->
                        if (e.type == KeyEventType.KeyDown && e.key == Key.DirectionLeft) {
                            onExpandRail()
                            focus.moveFocus(FocusDirection.Left)
                            true
                        } else false
                    },
                    onClick = { moviesTab = true }
                )
                GlassButton(
                    "${copy.series} (${series.size})",
                    selected = !moviesTab,
                    onClick = { moviesTab = false }
                )
            }
            Spacer(Modifier.height(16.dp))
            if (rows.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(copy.noContinue, color = g.muted, style = MaterialTheme.typography.bodyLarge)
                }
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    itemsIndexed(rows, key = { _, it -> it.mediaId }) { index, item ->
                        ContinueRow(copy, item, index + 1, onResume, onDelete, onExpandRail)
                    }
                }
            }
        }
    }
}

@Composable
private fun ContinueRow(
    copy: Copy,
    item: ProgressEntity,
    index: Int,
    onResume: (ProgressEntity) -> Unit,
    onDelete: (ProgressEntity) -> Unit,
    onExpandRail: () -> Unit
) {
    var focused by remember { mutableStateOf(false) }
    val g = LocalGlass.current
    val focus = LocalFocusManager.current
    val pct = if (item.durationMs > 0) {
        ((item.positionMs * 100) / item.durationMs).toInt().coerceIn(0, 100)
    } else 0
    val progress = if (item.durationMs > 0) {
        (item.positionMs.toFloat() / item.durationMs).coerceIn(0f, 1f)
    } else 0f
    val parsed = remember(item.title, item.kind) { continueMeta(item) }
    val touch = LocalTouchUi.current
    val watchedMin = (item.positionMs / 60_000L).toInt()
    val totalMin = (item.durationMs / 60_000L).toInt()
    GlassPanel(
        focused = focused,
        radius = 14.dp,
        onClick = { onResume(item) },
        onLongClick = { onDelete(item) },
        modifier = Modifier
            .fillMaxWidth()
            .height(96.dp)
            .onFocusChanged { focused = it.isFocused }
            .onPreviewKeyEvent { e ->
                if (e.type == KeyEventType.KeyDown && e.key == Key.DirectionLeft) {
                    onExpandRail()
                    focus.moveFocus(FocusDirection.Left)
                    true
                } else false
            }
    ) {
        Row(
            Modifier.fillMaxSize().padding(10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Box(
                Modifier
                    .size(28.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(if (focused) g.accent.copy(alpha = 0.22f) else Color.White.copy(alpha = 0.08f)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "$index",
                    color = if (focused) g.accent else g.muted,
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
                )
            }
            Box(
                Modifier
                    .width(120.dp)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(8.dp))
                    .border(0.5.dp, Color.White.copy(alpha = 0.12f), RoundedCornerShape(8.dp))
                    .background(g.panelStrong)
            ) {
                if (item.poster.isNotBlank()) {
                    AsyncImage(
                        model = item.poster,
                        contentDescription = item.title,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                }
            }
            Column(Modifier.weight(1f)) {
                Text(
                    parsed.title,
                    color = g.text,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (parsed.meta.isNotBlank()) {
                    Spacer(Modifier.height(2.dp))
                    Text(parsed.meta, color = g.accent, style = MaterialTheme.typography.bodySmall, maxLines = 1)
                }
                Spacer(Modifier.height(4.dp))
                Text(
                    copy.continueWatched.format(pct, watchedMin, totalMin),
                    color = g.muted,
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 1
                )
                Spacer(Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Box(
                        Modifier
                            .weight(1f)
                            .height(5.dp)
                            .clip(RoundedCornerShape(99.dp))
                            .background(Color.White.copy(alpha = 0.12f))
                    ) {
                        Box(
                            Modifier
                                .fillMaxWidth(progress)
                                .height(5.dp)
                                .background(g.accent, RoundedCornerShape(99.dp))
                        )
                    }
                    Text("$pct%", color = g.muted, style = MaterialTheme.typography.labelLarge)
                }
            }
            GlassButton(
                copy.delete,
                compact = true,
                modifier = Modifier.focusProperties { canFocus = touch },
                onClick = { onDelete(item) }
            )
        }
    }
}

private data class ContinueMeta(val title: String, val meta: String)

private val yearRx = Regex("""\b(19\d{2}|20\d{2})\b""")
private val seRx = Regex("""\b(S\d+\s*E\d+|S\d+\s*B\d+|Sezon\s*\d+\s*Bölüm\s*\d+)\b""", RegexOption.IGNORE_CASE)

private fun continueMeta(item: ProgressEntity): ContinueMeta {
    var title = item.title
    val year = yearRx.find(title)?.value
    if (year != null) title = title.replace(year, "").trim()
    val se = seRx.find(title)?.value
    if (se != null) title = title.replace(se, "").trim()
    title = title.replace(Regex("""\s+"""), " ").trim(' ', '-', ':', '(', '[')
    val meta = listOfNotNull(
        year,
        se?.uppercase()?.replace(Regex("""\s+"""), ""),
        if (item.kind == "MOVIE") null else if (se == null && item.kind == "EPISODE") null else null
    ).filter { it.isNotBlank() }.joinToString("  ·  ")
    return ContinueMeta(title.ifBlank { item.title }, meta)
}

@Composable
fun Shelf(title: String, content: androidx.compose.foundation.lazy.LazyListScope.() -> Unit) {
    val g = LocalGlass.current
    Column {
        Text(title, style = MaterialTheme.typography.headlineMedium, color = g.text, modifier = Modifier.padding(bottom = 12.dp))
        LazyRow(horizontalArrangement = Arrangement.spacedBy(14.dp), content = content)
    }
}
