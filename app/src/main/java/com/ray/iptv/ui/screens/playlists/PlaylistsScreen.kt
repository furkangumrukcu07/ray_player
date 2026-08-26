package com.ray.iptv.ui.screens.playlists

import androidx.activity.compose.BackHandler
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.RadioButtonChecked
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.focus.FocusDirection
import androidx.tv.material3.Icon
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.ray.iptv.data.local.SourceEntity
import com.ray.iptv.ui.glass.GlassPanel
import com.ray.iptv.ui.i18n.Copy
import com.ray.iptv.ui.theme.LocalGlass

@Composable
fun PlaylistsScreen(
    copy: Copy,
    sources: List<SourceEntity>,
    activeId: String,
    combineLists: Boolean,
    syncing: Boolean,
    onActivate: (String) -> Unit,
    onToggle: (String) -> Unit,
    onBackToRail: () -> Unit,
    railExpanded: Boolean = false,
    onExit: () -> Unit = {}
) {
    val g = LocalGlass.current
    val focusManager = LocalFocusManager.current
    BackHandler {
        if (railExpanded) onExit()
        else {
            onBackToRail()
            focusManager.moveFocus(FocusDirection.Left)
        }
    }
    GlassPanel(
        strong = true,
        radius = 22.dp,
        modifier = Modifier.fillMaxSize()
    ) {
        Column(Modifier.fillMaxSize().padding(horizontal = 22.dp, vertical = 16.dp)) {
            Text(
                copy.playlists,
                color = g.text,
                style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold)
            )
            Spacer(Modifier.height(6.dp))
            Text(
                copy.playlistsSubtitle,
                color = g.muted,
                style = MaterialTheme.typography.bodyMedium
            )
            if (syncing) {
                Spacer(Modifier.height(8.dp))
                Text(
                    if (copy.playlists == "Playlists") "Updating content…" else "İçerik güncelleniyor…",
                    color = g.accent,
                    style = MaterialTheme.typography.bodySmall
                )
            }
            Spacer(Modifier.height(14.dp))
            if (sources.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(copy.playlistsEmpty, color = g.muted, style = MaterialTheme.typography.bodyLarge)
                }
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    itemsIndexed(sources, key = { _, src -> src.id }) { _, src ->
                        val active = src.enabled && !combineLists && src.id == activeId
                        PlaylistRailRow(
                            copy = copy,
                            src = src,
                            active = active,
                            onActivate = { onActivate(src.id) },
                            onToggle = { onToggle(src.id) },
                            onLeftToRail = {
                                onBackToRail()
                                focusManager.moveFocus(FocusDirection.Left)
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PlaylistRailRow(
    copy: Copy,
    src: SourceEntity,
    active: Boolean,
    onActivate: () -> Unit,
    onToggle: () -> Unit,
    onLeftToRail: () -> Unit
) {
    val g = LocalGlass.current
    var focused by remember { mutableStateOf(false) }
    GlassPanel(
        focused = focused,
        strong = active,
        radius = 12.dp,
        onClick = onActivate,
        modifier = Modifier
            .fillMaxWidth()
            .onFocusChanged { focused = it.isFocused }
            .onPreviewKeyEvent { e ->
                if (e.type == KeyEventType.KeyDown && e.key == Key.DirectionLeft && focused) {
                    onLeftToRail()
                    true
                } else false
            }
            .alpha(if (src.enabled || focused || active) 1f else 0.55f)
    ) {
        Row(
            Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                if (active) Icons.Filled.RadioButtonChecked else Icons.Filled.RadioButtonUnchecked,
                contentDescription = null,
                tint = if (active) g.accent else g.muted,
                modifier = Modifier.size(22.dp)
            )
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    src.name.ifBlank { src.baseUrl.ifBlank { src.id } },
                    color = g.text,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = if (active) FontWeight.ExtraBold else FontWeight.SemiBold
                    ),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(3.dp))
                Text(
                    kindLabel(src.kind, copy),
                    color = g.muted,
                    style = MaterialTheme.typography.bodySmall
                )
            }
            Spacer(Modifier.width(10.dp))
            if (active) {
                Box(
                    Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(g.accent.copy(alpha = 0.22f))
                        .padding(horizontal = 9.dp, vertical = 4.dp)
                ) {
                    Text(
                        copy.playlistsActive,
                        color = g.accent,
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.ExtraBold)
                    )
                }
            } else {
                RailSwitch(on = src.enabled, onToggle = onToggle)
            }
        }
    }
}

@Composable
private fun RailSwitch(on: Boolean, onToggle: () -> Unit) {
    val g = LocalGlass.current
    var focused by remember { mutableStateOf(false) }
    GlassPanel(
        focused = focused,
        strong = on,
        accentFill = on,
        radius = 14.dp,
        onClick = onToggle,
        modifier = Modifier
            .width(46.dp)
            .height(28.dp)
            .onFocusChanged { focused = it.isFocused }
    ) {
        Box(Modifier.fillMaxSize().padding(3.dp)) {
            Box(
                Modifier
                    .size(22.dp)
                    .align(if (on) Alignment.CenterEnd else Alignment.CenterStart)
                    .clip(CircleShape)
                    .background(if (on) Color.White else Color.White.copy(alpha = 0.55f))
            )
        }
    }
}

private fun kindLabel(kind: String, copy: Copy): String = when (kind) {
    "XTREAM" -> copy.playlistKindXtream
    "STALKER" -> copy.playlistKindStalker
    else -> copy.playlistKindM3u
}
