package com.ray.iptv.ui.admin

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.ManageAccounts
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Icon
import androidx.tv.material3.Text
import coil.compose.AsyncImage
import com.ray.iptv.ui.RayViewModel
import com.ray.iptv.ui.input.rayClickable
import kotlinx.coroutines.launch

@Composable
fun AdminPanelScreen(
    vm: RayViewModel,
    tr: Boolean,
    onBack: () -> Unit,
    onOrders: () -> Unit,
    onUsers: () -> Unit,
    onCrashes: () -> Unit,
    onOnline: () -> Unit,
    onNotifications: () -> Unit
) {
    val account by vm.account.collectAsState()
    val stats by vm.admin.stats.collectAsState()
    var loading by remember { mutableStateOf(false) }
    var status by remember { mutableStateOf(if (tr) "İstatistikler yükleniyor…" else "Loading stats…") }
    var fetched by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    val ctx = LocalContext.current
    LaunchedEffect(Unit) {
        loading = true
        runCatching { vm.admin.refreshStats() }
        status = if (vm.admin.firebaseReady) {
            if (tr) "Yüklendi." else "Loaded."
        } else {
            if (tr) "Yerel önizleme · Firebase sonra bağlanacak." else "Local preview · Firebase will be wired later."
        }
        loading = false
    }
    AdminBackdrop {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
                Column(
                    Modifier
                        .widthIn(max = 600.dp)
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                        .padding(24.dp)
                        .clip(RoundedCornerShape(24.dp))
                        .background(Color.White.copy(alpha = 0.05f))
                        .border(1.dp, Color.White.copy(alpha = 0.10f), RoundedCornerShape(24.dp))
                        .padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        Modifier
                            .size(96.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF448AFF).copy(alpha = 0.10f))
                            .border(3.dp, Color(0xFF448AFF).copy(alpha = 0.30f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        if (account.photoUrl.isNotBlank()) {
                            AsyncImage(account.photoUrl, null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                        } else {
                            Icon(Icons.Filled.AdminPanelSettings, null, tint = Color(0xFF448AFF), modifier = Modifier.size(64.dp))
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                    Text(account.displayName.ifBlank { account.email }, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    Text(account.email, color = Color.White.copy(alpha = 0.6f), fontSize = 13.sp)
                    Spacer(Modifier.height(28.dp))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                        StatCol(if (tr) "Üretilen" else "Issued", stats.total.toString(), Color(0xFF448AFF))
                        StatCol(if (tr) "Kullanılan" else "Used", stats.used.toString(), Color(0xFFFF5252))
                        StatCol(if (tr) "Kalan" else "Left", stats.remaining.toString(), Color(0xFF69F0AE))
                    }
                    Spacer(Modifier.height(32.dp))
                    val tiles = listOf(
                        GridSpec(if (tr) "Satın Alımlar" else "Purchases", Icons.Filled.ShoppingCart, Color(0xFFF59E0B), onOrders),
                        GridSpec(if (tr) "Kullanıcı Yönetimi" else "User management", Icons.Filled.ManageAccounts, Color(0xFF448AFF), onUsers),
                        GridSpec(if (tr) "Hata Raporları" else "Crash reports", Icons.Filled.BugReport, Color(0xFFFF5252), onCrashes),
                        GridSpec(if (tr) "Kullanılmamış Kod" else "Unused code", Icons.Filled.Download, Color(0xFF69F0AE), {
                            scope.launch {
                                loading = true
                                status = if (tr) "Kullanılmamış bir kod aranıyor…" else "Looking for an unused code…"
                                fetched = null
                                val code = vm.admin.fetchUnusedCode()
                                fetched = code
                                status = if (code != null) {
                                    if (tr) "🎉 Kod başarıyla getirildi!" else "Code fetched."
                                } else {
                                    if (tr) "Kullanılmamış kod kalmadı! Lütfen yeni kod üretin." else "No unused codes left."
                                }
                                loading = false
                            }
                        }),
                        GridSpec(if (tr) "Çevrimiçi Üyeler" else "Online members", Icons.Filled.People, Color(0xFFE040FB), onOnline),
                        GridSpec(if (tr) "Bildirim Merkezi" else "Notification center", Icons.Filled.Notifications, Color(0xFF7C4DFF), onNotifications)
                    )
                    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        tiles.chunked(2).forEach { row ->
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                                row.forEach { tile ->
                                    Box(Modifier.weight(1f)) { GridCard(tile) }
                                }
                                if (row.size == 1) Spacer(Modifier.weight(1f))
                            }
                        }
                    }
                    fetched?.let { code ->
                        Column(
                            Modifier
                                .fillMaxWidth()
                                .padding(top = 24.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .background(Color(0xFF69F0AE).copy(alpha = 0.15f))
                                .border(1.5.dp, Color(0xFF69F0AE).copy(alpha = 0.5f), RoundedCornerShape(16.dp))
                                .padding(20.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(if (tr) "Kullanılabilir Kod" else "Available code", color = Color(0xFF69F0AE), fontSize = 14.sp, fontWeight = FontWeight.Bold)
                            Spacer(Modifier.height(8.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(code, color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Black, letterSpacing = 2.sp)
                                Icon(
                                    Icons.Filled.ContentCopy,
                                    null,
                                    tint = Color(0xFF69F0AE),
                                    modifier = Modifier
                                        .padding(start = 8.dp)
                                        .size(36.dp)
                                        .padding(6.dp)
                                        .rayClickable(onClick = {
                                            val cm = ctx.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                            cm.setPrimaryClip(ClipData.newPlainText("license", code))
                                            vm.toast.value = if (tr) "Kod kopyalandı!" else "Code copied."
                                        })
                                )
                            }
                        }
                    }
                    if (status.isNotBlank()) {
                        Spacer(Modifier.height(24.dp))
                        Text(status, color = Color.White.copy(alpha = 0.7f), fontSize = 14.sp, textAlign = TextAlign.Center)
                    }
                }
            }
        }
}

private data class GridSpec(val title: String, val icon: ImageVector, val color: Color, val onTap: () -> Unit)

@Composable
private fun StatCol(label: String, value: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, color = color, fontSize = 28.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(4.dp))
        Text(label, color = Color.White.copy(alpha = 0.6f), fontSize = 14.sp)
    }
}

@Composable
private fun GridCard(spec: GridSpec) {
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(spec.color.copy(alpha = 0.10f))
            .border(1.dp, spec.color.copy(alpha = 0.30f), RoundedCornerShape(20.dp))
            .rayClickable(spec.onTap)
            .padding(16.dp)
            .height(110.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(spec.icon, null, tint = spec.color, modifier = Modifier.size(36.dp))
        Spacer(Modifier.height(12.dp))
        Text(spec.title, color = spec.color, fontSize = 15.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
    }
}
