package com.ray.iptv.ui.screens.guide

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.unit.dp
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.ray.iptv.data.local.ChannelEntity
import com.ray.iptv.data.local.EpgEntity
import com.ray.iptv.ui.components.GlassButton
import com.ray.iptv.ui.glass.GlassPanel
import com.ray.iptv.ui.i18n.Copy
import com.ray.iptv.ui.input.tryFocus
import com.ray.iptv.ui.theme.LocalGlass
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun GuideScreen(
    copy: Copy,
    channels: List<ChannelEntity>,
    load: suspend (ChannelEntity) -> List<EpgEntity>,
    onPlay: (ChannelEntity) -> Unit,
    onCatchup: (ChannelEntity, EpgEntity) -> Unit,
    onImport: () -> Unit,
    time24h: Boolean = true
) {
    val g = LocalGlass.current
    var selected by remember { mutableStateOf<ChannelEntity?>(null) }
    var programmes by remember { mutableStateOf<List<EpgEntity>>(emptyList()) }
    val fmt = remember(time24h) { SimpleDateFormat(if (time24h) "HH:mm" else "h:mm a", Locale.getDefault()) }
    val firstChannelFocus = remember { FocusRequester() }
    val firstProgrammeFocus = remember { FocusRequester() }

    LaunchedEffect(selected) {
        val ch = selected ?: channels.firstOrNull()
        selected = ch
        programmes = if (ch != null) load(ch) else emptyList()
    }

    // Auto-focus first channel on open
    LaunchedEffect(channels) {
        if (channels.isNotEmpty()) {
            repeat(12) {
                delay(40)
                if (firstChannelFocus.tryFocus()) return@LaunchedEffect
            }
        }
    }

    Column(Modifier.fillMaxSize()) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(copy.guide, style = MaterialTheme.typography.headlineLarge, color = g.text)
            GlassButton(copy.importXmltv) { onImport() }
        }
        Spacer(Modifier.height(14.dp))
        Row(Modifier.fillMaxSize(), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            GlassPanel(strong = true, radius = 18.dp, modifier = Modifier.weight(1f).fillMaxSize()) {
                LazyColumn(Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    itemsIndexed(channels.take(200), key = { _, ch -> ch.id }) { idx, ch ->
                        GlassButton(
                            (if (selected?.id == ch.id) "●  " else "") + ch.name,
                            modifier = if (idx == 0) Modifier.focusRequester(firstChannelFocus) else Modifier
                        ) { selected = ch }
                    }
                }
            }
            GlassPanel(radius = 18.dp, modifier = Modifier.weight(1.4f).fillMaxSize()) {
                LazyColumn(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    item {
                        Text(selected?.name ?: copy.noChannel, style = MaterialTheme.typography.headlineMedium, color = g.text)
                        Spacer(Modifier.height(8.dp))
                    }
                    if (programmes.isEmpty()) {
                        item { Text(copy.noGuideRows, color = g.muted) }
                    }
                    itemsIndexed(programmes, key = { _, p -> p.id }) { idx, p ->
                        val live = p.startMs <= System.currentTimeMillis() && p.endMs > System.currentTimeMillis()
                        val past = p.endMs < System.currentTimeMillis()
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .onPreviewKeyEvent { e ->
                                    if (e.type == KeyEventType.KeyDown && e.key == Key.DirectionLeft) {
                                        firstChannelFocus.tryFocus()
                                        true
                                    } else false
                                },
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text("${fmt.format(Date(p.startMs))}–${fmt.format(Date(p.endMs))}", color = g.accent, modifier = Modifier.padding(top = 12.dp))
                            Column(Modifier.weight(1f)) {
                                Text(p.title, color = g.text, style = MaterialTheme.typography.titleMedium)
                                Text(p.plot, color = g.muted, style = MaterialTheme.typography.bodyMedium, maxLines = 2)
                            }
                            if (live) GlassButton(
                                copy.watch,
                                modifier = if (idx == 0) Modifier.focusRequester(firstProgrammeFocus) else Modifier
                            ) { selected?.let(onPlay) }
                            else if (past && (selected?.hasArchive == true)) GlassButton(
                                copy.catchup,
                                modifier = if (idx == 0) Modifier.focusRequester(firstProgrammeFocus) else Modifier
                            ) { selected?.let { onCatchup(it, p) } }
                        }
                    }
                }
            }
        }
    }
}

