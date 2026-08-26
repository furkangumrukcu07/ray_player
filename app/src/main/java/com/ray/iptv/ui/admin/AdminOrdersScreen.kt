package com.ray.iptv.ui.admin

import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Star
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
import com.ray.iptv.data.admin.AdminOrder
import com.ray.iptv.ui.RayViewModel
import com.ray.iptv.ui.input.rayClickable
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Composable
fun AdminOrdersScreen(vm: RayViewModel, tr: Boolean, onBack: () -> Unit) {
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf("") }
    var orders by remember { mutableStateOf<List<AdminOrder>>(emptyList()) }
    val scope = rememberCoroutineScope()
    fun reload() {
        scope.launch {
            loading = true
            error = ""
            runCatching { vm.admin.listOrders() }
                .onSuccess { orders = it }
                .onFailure { error = it.message.orEmpty() }
            loading = false
        }
    }
    LaunchedEffect(Unit) { reload() }
    AdminBackdrop {
        Column(Modifier.fillMaxSize()) {
            AdminTopBar(
                title = if (tr) "Satın Alımlar (Siparişler)" else "Purchases (Orders)",
                onBack = onBack,
                trailing = {
                    Icon(Icons.Filled.Refresh, null, tint = Color.White, modifier = Modifier.size(40.dp).padding(8.dp).rayClickable(onClick = { reload() }))
                }
            )
            when {
                loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(if (tr) "Yükleniyor…" else "Loading…", color = Color(0xFFF59E0B))
                }
                error.isNotBlank() -> AdminEmpty(if (tr) "Hata: $error" else "Error: $error")
                orders.isEmpty() -> AdminEmpty(
                    if (tr) "Henüz hiç satın alım veya aktif premium lisansı yok."
                    else "No purchases or active premium licenses yet."
                )
                else -> LazyColumn(Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
                    items(orders, key = { it.uid + it.purchaseDate + it.source }) { order ->
                        OrderRow(order, tr)
                    }
                }
            }
        }
    }
}

@Composable
private fun OrderRow(order: AdminOrder, tr: Boolean) {
    val title = orderTitle(order, tr)
    val sub = orderSub(order, tr)
    Row(Modifier.fillMaxWidth().padding(vertical = 12.dp), verticalAlignment = Alignment.Top) {
        Column(Modifier.weight(3f)) {
            Text(title, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
            Spacer(Modifier.height(4.dp))
            Text(sub, color = Color.White.copy(alpha = 0.54f), fontSize = 13.sp)
            Spacer(Modifier.height(4.dp))
            Text(order.email.ifBlank { if (tr) "Bilinmeyen Kullanıcı" else "Unknown user" }, color = Color(0xFF448AFF), fontSize = 12.sp)
        }
        Column(Modifier.weight(1f), horizontalAlignment = Alignment.End) {
            Text(relativeTime(order.purchaseDate, tr), color = Color.White.copy(alpha = 0.54f), fontSize = 13.sp)
            Spacer(Modifier.height(8.dp))
            StatusIcon(order.source)
        }
    }
}

@Composable
private fun StatusIcon(source: String) {
    val (color, icon) = when (source) {
        "play" -> Color(0xFF4CAF50) to Icons.Filled.CheckCircle
        "admin_grant" -> Color(0xFFFF9800) to Icons.Filled.Star
        "code" -> Color(0xFF2196F3) to Icons.Filled.QrCode
        else -> Color.Gray to Icons.Filled.Info
    }
    Box(Modifier.size(28.dp).clip(CircleShape).background(color.copy(alpha = 0.15f)), contentAlignment = Alignment.Center) {
        Icon(icon, null, tint = color, modifier = Modifier.size(18.dp))
    }
}

private fun orderTitle(order: AdminOrder, tr: Boolean): String = when (order.source) {
    "play" -> when {
        order.productId == "mina_buy_coffee" || order.productId == "ray_buy_coffee" -> if (tr) "Kahve Ismarlandı ☕" else "Coffee bought ☕"
        order.productId.contains("6devices") -> if (tr) "+3 Ek Cihaz" else "+3 extra devices"
        order.productId.contains("plus") -> "Premium Plus"
        else -> "Premium"
    }
    "admin_grant" -> if (tr) "Admin Tarafından Verildi" else "Granted by admin"
    "code" -> if (tr) "Lisans Kodu (Hediye)" else "License code (gift)"
    else -> if (tr) "Bilinmeyen Satın Alım" else "Unknown purchase"
}

private fun orderSub(order: AdminOrder, tr: Boolean): String = when (order.source) {
    "play" -> order.playOrderId.ifBlank { if (tr) "Sipariş ID Bulunamadı" else "Order ID missing" }
    "admin_grant" -> order.adminNote.ifBlank { if (tr) "Yönetici Hediyesi" else "Admin gift" }
    "code" -> order.adminNote.ifBlank { if (tr) "Kod Kullanıldı" else "Code used" }
    else -> order.uid
}

internal fun relativeTime(raw: String, tr: Boolean): String {
    if (raw.isBlank()) return if (tr) "Bilinmiyor" else "Unknown"
    val instant = runCatching {
        OffsetDateTime.parse(raw).toInstant()
    }.recoverCatching {
        Instant.parse(raw)
    }.recoverCatching {
        DateTimeFormatter.ISO_LOCAL_DATE_TIME.withZone(ZoneId.systemDefault()).parse(raw, Instant::from)
    }.getOrNull() ?: return if (tr) "Bilinmiyor" else "Unknown"
    val diff = System.currentTimeMillis() - instant.toEpochMilli()
    val min = diff / 60_000
    val hour = min / 60
    val day = hour / 24
    return when {
        day > 365 -> if (tr) "${day / 365} yıl önce" else "${day / 365}y ago"
        day > 30 -> if (tr) "${day / 30} ay önce" else "${day / 30}mo ago"
        day > 0 -> if (tr) "$day gün önce" else "${day}d ago"
        hour > 0 -> if (tr) "$hour saat önce" else "${hour}h ago"
        min > 0 -> if (tr) "$min dakika önce" else "${min}m ago"
        else -> if (tr) "az önce" else "just now"
    }
}
