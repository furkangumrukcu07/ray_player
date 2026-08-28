package com.ray.iptv.ui.components

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlaylistPlay
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Tune
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
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.tv.material3.Icon
import androidx.tv.material3.Text
import com.ray.iptv.data.repo.BackupFile
import com.ray.iptv.ui.input.rayClickable
import kotlinx.coroutines.delay

data class BackupDiffItem(
    val title: String,
    val localCount: Int,
    val cloudCount: Int,
    val icon: ImageVector,
    val customValue: String? = null
)

@Composable
fun CloudRestorePreviewDialog(
    tr: Boolean,
    backup: BackupFile,
    localSourcesCount: Int,
    localProfilesCount: Int,
    localFavoritesCount: Int,
    localProgressCount: Int,
    localEpgCount: Int = 0,
    onMerge: () -> Unit,
    onOverwrite: () -> Unit,
    onDismiss: () -> Unit
) {
    val mergeFocus = remember { FocusRequester() }
    val overwriteFocus = remember { FocusRequester() }
    val cancelFocus = remember { FocusRequester() }

    var mergeFocused by remember { mutableStateOf(false) }
    var overwriteFocused by remember { mutableStateOf(false) }
    var cancelFocused by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        delay(150)
        try {
            mergeFocus.requestFocus()
        } catch (_: Exception) {}
    }

    val items = listOf(
        BackupDiffItem(
            title = if (tr) "Oynatma Listeleri (M3U & Xtream)" else "Playlists (M3U & Xtream)",
            localCount = localSourcesCount,
            cloudCount = backup.sources.size,
            icon = Icons.Filled.PlaylistPlay
        ),
        BackupDiffItem(
            title = if (tr) "Uygulama Ayarları & Tercihler" else "Settings & Preferences",
            localCount = 0,
            cloudCount = 0,
            icon = Icons.Filled.Tune,
            customValue = if (tr) "Tüm Yapılandırma" else "All Configuration"
        ),
        BackupDiffItem(
            title = if (tr) "Favori İçerikler (Canlı, Film, Dizi)" else "Favorites (Live, Movies, Series)",
            localCount = localFavoritesCount,
            cloudCount = backup.favorites.size,
            icon = Icons.Filled.Favorite
        ),
        BackupDiffItem(
            title = if (tr) "İzleme Geçmişi (Kaldığınız Yerler)" else "Watch History & Progress",
            localCount = localProgressCount,
            cloudCount = backup.progress.size,
            icon = Icons.Filled.History
        ),
        BackupDiffItem(
            title = if (tr) "Kullanıcı Profilleri & PIN" else "Profiles & PIN Locks",
            localCount = localProfilesCount,
            cloudCount = backup.profiles.size,
            icon = Icons.Filled.Person
        ),
        BackupDiffItem(
            title = if (tr) "EPG TV Rehber Kaynakları" else "EPG Guide Sources",
            localCount = localEpgCount,
            cloudCount = backup.epgSources.size,
            icon = Icons.Filled.Schedule
        )
    )

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        BackHandler { onDismiss() }

        Box(
            Modifier
                .width(480.dp)
                .clip(RoundedCornerShape(24.dp))
                .background(Color(0xFF0E281F).copy(alpha = 0.96f))
                .border(1.dp, Color(0xFF34D399).copy(alpha = 0.35f), RoundedCornerShape(24.dp))
                .padding(22.dp)
        ) {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        Modifier
                            .size(44.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFF22D3EE).copy(alpha = 0.15f))
                            .border(1.dp, Color(0xFF22D3EE).copy(alpha = 0.35f), RoundedCornerShape(12.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Filled.CloudDownload,
                            contentDescription = null,
                            tint = Color(0xFF22D3EE),
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text(
                            text = if (tr) "Bulut Yedeği Geri Yükleme" else "Cloud Backup Restore Preview",
                            color = Color.White,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = if (tr) "Yerel verileriniz ile bulut yedeği karşılaştırıldı" else "Comparison between local and cloud backup",
                            color = Color.White.copy(alpha = 0.60f),
                            fontSize = 12.sp
                        )
                    }
                }

                Spacer(Modifier.height(16.dp))

                // Diff Table
                Column(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color.White.copy(alpha = 0.05f))
                        .border(1.dp, Color.White.copy(alpha = 0.10f), RoundedCornerShape(16.dp))
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items.forEach { item ->
                        Row(
                            Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    item.icon,
                                    contentDescription = null,
                                    tint = Color(0xFF34D399),
                                    modifier = Modifier.size(17.dp)
                                )
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    item.title,
                                    color = Color.White.copy(alpha = 0.90f),
                                    fontSize = 12.5.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                            val valueText = item.customValue ?: (if (tr) "Mevcut: ${item.localCount} ➔ Bulut: ${item.cloudCount}" else "Local: ${item.localCount} ➔ Cloud: ${item.cloudCount}")
                            Text(
                                text = valueText,
                                color = Color(0xFF22D3EE),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                Spacer(Modifier.height(18.dp))

                // Action Buttons (Birleştir vs Üzerine Yaz vs İptal)
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    // Merge Button (Primary)
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .focusRequester(mergeFocus)
                            .onFocusChanged { mergeFocused = it.isFocused }
                            .focusable()
                            .clip(RoundedCornerShape(14.dp))
                            .background(
                                if (mergeFocused) Color(0xFF34D399) else Color.White
                            )
                            .border(
                                width = if (mergeFocused) 2.5.dp else 1.dp,
                                color = if (mergeFocused) Color.White else Color.Transparent,
                                shape = RoundedCornerShape(14.dp)
                            )
                            .rayClickable(onClick = onMerge)
                            .padding(vertical = 12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (tr) "Birleştir (Mevcut Verileri Koru)" else "Merge (Keep Local & Add Cloud)",
                            color = Color(0xFF0F172A),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }

                    // Overwrite Button (Danger / Clean Restore)
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .focusRequester(overwriteFocus)
                            .onFocusChanged { overwriteFocused = it.isFocused }
                            .focusable()
                            .clip(RoundedCornerShape(14.dp))
                            .background(
                                if (overwriteFocused) Color(0xFFEF4444) else Color(0xFFEF4444).copy(alpha = 0.20f)
                            )
                            .border(
                                width = if (overwriteFocused) 2.5.dp else 1.dp,
                                color = if (overwriteFocused) Color.White else Color(0xFFEF4444).copy(alpha = 0.50f),
                                shape = RoundedCornerShape(14.dp)
                            )
                            .rayClickable(onClick = onOverwrite)
                            .padding(vertical = 12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (tr) "Tümünün Üzerine Yaz (Tam Geri Yükle)" else "Overwrite All (Clean Restore)",
                            color = if (overwriteFocused) Color.White else Color(0xFFEF4444),
                            fontSize = 13.5.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    // Cancel Button
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .focusRequester(cancelFocus)
                            .onFocusChanged { cancelFocused = it.isFocused }
                            .focusable()
                            .clip(RoundedCornerShape(14.dp))
                            .background(
                                if (cancelFocused) Color.White.copy(alpha = 0.22f) else Color.Transparent
                            )
                            .border(
                                width = if (cancelFocused) 2.dp else 1.dp,
                                color = if (cancelFocused) Color(0xFF22D3EE) else Color.White.copy(alpha = 0.15f),
                                shape = RoundedCornerShape(14.dp)
                            )
                            .rayClickable(onClick = onDismiss)
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (tr) "Vazgeç" else "Cancel",
                            color = if (cancelFocused) Color(0xFF22D3EE) else Color.White.copy(alpha = 0.70f),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }
    }
}
