package com.ray.iptv.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.PlaylistPlay
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.tv.material3.Icon
import androidx.tv.material3.Text
import com.ray.iptv.data.local.SourceEntity
import com.ray.iptv.ui.input.rayClickable

@Composable
fun PlaylistPickerDialog(
    tr: Boolean,
    sources: List<SourceEntity>,
    activeSourceId: String?,
    onSelect: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val cyan = Color(0xFF22D3EE)
    val emerald = Color(0xFF34D399)

    Dialog(onDismissRequest = onDismiss) {
        Box(
            Modifier
                .widthIn(max = 420.dp)
                .clip(RoundedCornerShape(24.dp))
                .background(Color(0xFF0E281F).copy(alpha = 0.94f))
                .border(1.dp, emerald.copy(alpha = 0.35f), RoundedCornerShape(24.dp))
                .padding(20.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            Modifier
                                .size(38.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(cyan.copy(alpha = 0.16f))
                                .border(1.dp, cyan.copy(alpha = 0.28f), RoundedCornerShape(10.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.AutoMirrored.Filled.PlaylistPlay,
                                contentDescription = null,
                                tint = cyan,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        Spacer(Modifier.width(12.dp))
                        Text(
                            if (tr) "Oynatma Listeleri" else "Playlists",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 17.sp
                        )
                    }
                    Box(
                        Modifier
                            .size(32.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color.White.copy(alpha = 0.10f))
                            .rayClickable(onDismiss),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Filled.Close, contentDescription = null, tint = Color.White.copy(alpha = 0.8f), modifier = Modifier.size(18.dp))
                    }
                }

                Box(Modifier.fillMaxWidth().height(1.dp).background(Color.White.copy(alpha = 0.10f)))

                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.heightIn(max = 340.dp)
                ) {
                    items(sources, key = { it.id }) { src ->
                        val selected = src.id == activeSourceId
                        Box(
                            Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(16.dp))
                                .background(
                                    if (selected) Color(0xFF16382B).copy(alpha = 0.70f)
                                    else Color.White.copy(alpha = 0.05f)
                                )
                                .border(
                                    if (selected) 1.5.dp else 1.dp,
                                    if (selected) cyan else Color(0xFF4ADE80).copy(alpha = 0.15f),
                                    RoundedCornerShape(16.dp)
                                )
                                .rayClickable(onClick = {
                                    onSelect(src.id)
                                    onDismiss()
                                })
                                .padding(horizontal = 16.dp, vertical = 14.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(Modifier.weight(1f)) {
                                    Text(
                                        src.name.ifBlank { if (src.kind == "XTREAM") "Xtream Playlist" else "M3U Playlist" },
                                        color = if (selected) cyan else Color.White,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 15.sp
                                    )
                                    Spacer(Modifier.height(2.dp))
                                    Text(
                                        if (src.baseUrl.isNotBlank()) src.baseUrl else src.kind,
                                        color = Color.White.copy(alpha = 0.6f),
                                        fontSize = 12.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                if (selected) {
                                    Spacer(Modifier.width(10.dp))
                                    Icon(Icons.Filled.Check, contentDescription = null, tint = cyan, modifier = Modifier.size(22.dp))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
