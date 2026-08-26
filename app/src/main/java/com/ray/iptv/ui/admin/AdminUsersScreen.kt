package com.ray.iptv.ui.admin

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Icon
import androidx.tv.material3.Text
import com.ray.iptv.data.admin.AdminUser
import com.ray.iptv.ui.RayViewModel
import com.ray.iptv.ui.input.rayClickable
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun AdminUsersScreen(
    vm: RayViewModel,
    tr: Boolean,
    onBack: () -> Unit,
    onOpen: (AdminUser) -> Unit
) {
    var loading by remember { mutableStateOf(true) }
    var query by remember { mutableStateOf("") }
    var filter by remember { mutableStateOf("all") }
    var all by remember { mutableStateOf<List<AdminUser>>(emptyList()) }
    val scope = rememberCoroutineScope()
    fun reload() {
        scope.launch {
            loading = true
            all = runCatching { vm.admin.listUsers() }.getOrDefault(emptyList())
            loading = false
        }
    }
    LaunchedEffect(Unit) { reload() }
    val shown = remember(all, query, filter) { filterUsers(all, query, filter) }
    AdminBackdrop {
        Column(Modifier.fillMaxSize()) {
            AdminTopBar(
                title = if (tr) "Kullanıcı Yönetimi" else "User management",
                subtitle = "${all.size} ${if (tr) "Toplam Kullanıcı" else "total users"}",
                onBack = onBack,
                trailing = {
                    Icon(Icons.Filled.Refresh, null, tint = Color.White, modifier = Modifier.size(40.dp).padding(8.dp).rayClickable(onClick = { reload() }))
                }
            )
            Column(Modifier.padding(horizontal = 16.dp)) {
                AdminDarkField(query, { query = it }, if (tr) "Ara" else "Search", if (tr) "Email, UID, Ad veya Cihaz Modeli ara..." else "Search email, UID, name or device…")
                Spacer(Modifier.height(12.dp))
                Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(
                        "all" to (if (tr) "Tümü" else "All") to Color(0xFF448AFF),
                        "registered" to (if (tr) "Kayıtlılar" else "Registered") to Color(0xFFE040FB),
                        "premium" to "Premium" to Color(0xFF4CAF50),
                        "recent_premium" to (if (tr) "Yeni Premiumlar" else "New premium") to Color(0xFF009688),
                        "free" to (if (tr) "Ücretsiz" else "Free") to Color.Gray,
                        "anonymous" to (if (tr) "Anonim (Trial)" else "Anonymous") to Color(0xFFFF9800),
                        "banned" to (if (tr) "Banlı" else "Banned") to Color(0xFFFF5252)
                    ).forEach { (pair, color) ->
                        val (id, label) = pair
                        AdminChip(label, color, filter == id) { filter = id }
                    }
                }
                Spacer(Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Badge("${all.count { it.isPremium }}", "Premium", Color(0xFF69F0AE))
                    Badge("${all.count { it.isAnonymous }}", if (tr) "Anonim" else "Anon", Color(0xFFFF9800))
                    Badge("${all.count { it.isBanned }}", if (tr) "Banlı" else "Banned", Color(0xFFFF5252))
                }
            }
            Spacer(Modifier.height(8.dp))
            when {
                loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(if (tr) "Yükleniyor…" else "Loading…", color = Color.White)
                }
                shown.isEmpty() -> AdminEmpty(if (tr) "Kullanıcı bulunamadı." else "No users found.")
                else -> LazyColumn(Modifier.fillMaxSize().padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    items(shown, key = { it.uid }) { u ->
                        UserCard(u, tr, onOpen)
                    }
                    item { Spacer(Modifier.height(24.dp)) }
                }
            }
        }
    }
}

@Composable
private fun Badge(count: String, label: String, color: Color) {
    Row(
        Modifier
            .clip(RoundedCornerShape(10.dp))
            .background(color.copy(alpha = 0.10f))
            .border(1.dp, color.copy(alpha = 0.30f), RoundedCornerShape(10.dp))
            .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(count, color = color, fontWeight = FontWeight.Bold, fontSize = 15.sp)
        Spacer(Modifier.size(5.dp))
        Text(label, color = color.copy(alpha = 0.8f), fontSize = 12.sp)
    }
}

@Composable
private fun UserCard(user: AdminUser, tr: Boolean, onOpen: (AdminUser) -> Unit) {
    val (statusColor, statusLabel) = when {
        user.isBanned -> Color(0xFFFF5252) to if (tr) "Banlı" else "Banned"
        user.isPremium -> Color(0xFF4CAF50) to "Premium"
        user.isAnonymous -> Color(0xFFFF9800) to if (tr) "Anonim" else "Anonymous"
        else -> Color.Gray to if (tr) "Ücretsiz" else "Free"
    }
    val login = if (user.lastLoginAt > 0) SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault()).format(Date(user.lastLoginAt)) else if (tr) "Bilinmiyor" else "Unknown"
    AdminGlassCard(onClick = { onOpen(user) }) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier.size(40.dp).clip(CircleShape).background(statusColor.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    if (user.isAnonymous) "?" else (user.displayName.ifBlank { user.email }.firstOrNull() ?: '?').uppercaseChar().toString(),
                    color = statusColor,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(Modifier.size(12.dp))
            Column(Modifier.weight(1f)) {
                Text(user.displayName.ifBlank { user.email }, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp, maxLines = 1)
                if (user.displayName.isNotBlank()) Text(user.email, color = Color.White.copy(alpha = 0.54f), fontSize = 12.sp, maxLines = 1)
            }
            Text(
                statusLabel,
                color = statusColor,
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp,
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(statusColor.copy(alpha = 0.2f))
                    .padding(horizontal = 10.dp, vertical = 4.dp)
            )
        }
        Spacer(Modifier.height(12.dp))
        Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)).background(Color.Black.copy(alpha = 0.26f)).padding(10.dp)) {
            Text("UID: ${user.uid}", color = Color.White.copy(alpha = 0.7f), fontSize = 12.sp)
            Text("${if (tr) "Son Giriş" else "Last login"}: $login", color = Color.White.copy(alpha = 0.7f), fontSize = 12.sp)
            Text(
                "${if (tr) "Cihaz" else "Device"}: ${user.lastDeviceName.ifBlank { if (tr) "Bilinmiyor" else "Unknown" }}${if (user.lastDeviceOs.isNotBlank()) " (${user.lastDeviceOs})" else ""}",
                color = Color.White.copy(alpha = 0.7f),
                fontSize = 12.sp
            )
        }
    }
}

private fun filterUsers(all: List<AdminUser>, query: String, type: String): List<AdminUser> {
    val seven = System.currentTimeMillis() - 7L * 86_400_000
    var list = when (type) {
        "registered" -> all.filter { !it.isAnonymous }
        "premium" -> all.filter { it.isPremium && !it.isBanned }
        "free" -> all.filter { !it.isPremium && !it.isBanned }
        "banned" -> all.filter { it.isBanned }
        "anonymous" -> all.filter { it.isAnonymous }
        "recent_premium" -> all.filter { it.isPremium && parseMillis(it.purchaseDate) > seven }
            .sortedByDescending { parseMillis(it.purchaseDate) }
        else -> all
    }
    val q = query.trim().lowercase()
    if (q.isNotEmpty()) {
        list = list.filter {
            it.email.contains(q, true) || it.uid.contains(q, true) ||
                it.displayName.contains(q, true) || it.lastDeviceName.contains(q, true) ||
                it.lastDeviceOs.contains(q, true)
        }
    }
    return list
}

private fun parseMillis(raw: String): Long = runCatching {
    SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US).parse(raw)?.time ?: 0L
}.getOrDefault(0L)
