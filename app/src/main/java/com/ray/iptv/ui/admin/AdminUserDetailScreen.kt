package com.ray.iptv.ui.admin

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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Devices
import androidx.compose.material.icons.filled.FormatListNumbered
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.tv.material3.Icon
import androidx.tv.material3.Text
import com.ray.iptv.data.admin.AdminUser
import com.ray.iptv.ui.RayViewModel
import com.ray.iptv.ui.components.GlassButton
import com.ray.iptv.ui.input.rayClickable
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun AdminUserDetailScreen(vm: RayViewModel, tr: Boolean, seed: AdminUser, onBack: () -> Unit) {
    var user by remember { mutableStateOf(seed) }
    var loading by remember { mutableStateOf(false) }
    var title by remember { mutableStateOf("") }
    var body by remember { mutableStateOf("") }
    var sending by remember { mutableStateOf(false) }
    var grantOpen by remember { mutableStateOf(false) }
    var limitOpen by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    fun reload() {
        scope.launch {
            loading = true
            user = vm.admin.userDetail(seed.uid) ?: user
            loading = false
        }
    }
    fun toast(notice: com.ray.iptv.data.admin.AdminNotice) {
        vm.toast.value = notice.message
        if (notice.ok) reload()
    }
    LaunchedEffect(seed.uid) { reload() }
    val (statusColor, statusLabel) = when {
        user.isBanned -> Color(0xFFFF5252) to if (tr) "Banlı" else "Banned"
        user.isPremium -> Color(0xFFF59E0B) to "Premium"
        user.isAnonymous -> Color(0xFFFF9800) to if (tr) "Anonim" else "Anonymous"
        else -> Color.Gray to if (tr) "Ücretsiz" else "Free"
    }
    val login = if (user.lastLoginAt > 0)
        SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault()).format(Date(user.lastLoginAt))
    else if (tr) "Bilinmiyor" else "Unknown"
    AdminBackdrop {
        Column(Modifier.fillMaxSize()) {
            AdminTopBar(
                title = if (tr) "Kullanıcı Detayı" else "User detail",
                subtitle = user.email,
                onBack = onBack,
                trailing = {
                    Icon(Icons.Filled.Refresh, null, tint = Color.White, modifier = Modifier.size(40.dp).padding(8.dp).rayClickable(onClick = { reload() }))
                }
            )
            Column(
                Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                AdminGlassCard {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            Modifier.size(64.dp).clip(CircleShape).background(statusColor.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                if (user.isAnonymous) "?" else (user.displayName.ifBlank { user.email }.firstOrNull() ?: '?').uppercaseChar().toString(),
                                color = statusColor,
                                fontWeight = FontWeight.Bold,
                                fontSize = 26.sp
                            )
                        }
                        Spacer(Modifier.size(16.dp))
                        Column(Modifier.weight(1f)) {
                            Text(user.displayName.ifBlank { user.email }, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp, maxLines = 1)
                            if (user.displayName.isNotBlank()) Text(user.email, color = Color.White.copy(alpha = 0.7f), fontSize = 13.sp, maxLines = 1)
                        }
                        Text(
                            statusLabel,
                            color = statusColor,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(statusColor.copy(alpha = 0.2f))
                                .border(1.dp, statusColor.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
                                .padding(horizontal = 14.dp, vertical = 8.dp)
                        )
                    }
                }
                Section(if (tr) "Kullanıcı Bilgileri" else "User info", Color(0xFF3B82F6)) {
                    AdminInfoRow("UID", user.uid)
                    AdminInfoRow(if (tr) "E-posta" else "Email", user.email)
                    AdminInfoRow(if (tr) "Ad Soyad" else "Name", user.displayName.ifBlank { "-" })
                    AdminInfoRow(if (tr) "Son Giriş" else "Last login", login)
                    AdminInfoRow(if (tr) "Son Cihaz" else "Last device", user.lastDeviceName.ifBlank { if (tr) "Bilinmiyor" else "Unknown" })
                    AdminInfoRow(if (tr) "Cihaz Limiti" else "Device limit", "${user.maxDevices} ${if (tr) "Cihaz" else "devices"}")
                }
                Section(if (tr) "Premium Durumu" else "Premium status", Color(0xFFF59E0B)) {
                    AdminInfoRow(if (tr) "Durum" else "Status", if (user.isPremium) "✦ Premium" else if (tr) "Ücretsiz" else "Free")
                    if (user.isPremium) {
                        AdminInfoRow(if (tr) "Kaynak" else "Source", user.premiumSource.ifBlank { "-" })
                        AdminInfoRow(if (tr) "Satın Alma" else "Purchase", user.purchaseDate.ifBlank { "-" })
                        AdminInfoRow(if (tr) "Bitiş Tarihi" else "Expires", user.premiumExpiry.ifBlank { if (tr) "Sınırsız" else "Unlimited" })
                    }
                }
                Section(if (tr) "Premium Yönetimi" else "Premium management", Color(0xFF10B981)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        ActionBtn(if (tr) "Premium Ver" else "Grant premium", Icons.Filled.Star, Color(0xFFF59E0B)) { grantOpen = true }
                        if (user.isPremium) {
                            ActionBtn(if (tr) "Premium İptal" else "Revoke", Icons.Filled.StarBorder, Color(0xFFFF5252)) {
                                scope.launch { toast(vm.admin.revokePremium(user.uid)) }
                            }
                        }
                    }
                }
                Section(if (tr) "Kişisel Bildirim" else "Personal notification", Color(0xFF6366F1)) {
                    AdminDarkField(title, { title = it }, if (tr) "Bildirim Başlığı" else "Title", if (tr) "Başlık girin..." else "Title…")
                    Spacer(Modifier.height(12.dp))
                    AdminDarkField(body, { body = it }, if (tr) "Bildirim İçeriği" else "Body", if (tr) "Mesaj içeriği..." else "Message…", singleLine = false, minLines = 3)
                    Spacer(Modifier.height(16.dp))
                    AdminGradientButton(
                        if (tr) "Bildirim Gönder" else "Send notification",
                        Icons.Filled.Send,
                        sending,
                        listOf(Color(0xFF6366F1), Color(0xFF8B5CF6))
                    ) {
                        if (title.isBlank() || body.isBlank()) {
                            vm.toast.value = if (tr) "Başlık ve içerik zorunludur." else "Title and body are required."
                            return@AdminGradientButton
                        }
                        scope.launch {
                            sending = true
                            toast(vm.admin.sendUserNotification(user.uid, title.trim(), body.trim()))
                            title = ""
                            body = ""
                            sending = false
                        }
                    }
                }
                Section(if (tr) "Diğer İşlemler" else "Other actions", Color.White.copy(alpha = 0.54f)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        ActionBtn(if (tr) "Cihazları Sıfırla" else "Reset devices", Icons.Filled.Devices, Color(0xFFFFAB40)) {
                            scope.launch { toast(vm.admin.manageUser(user.uid, "reset_devices")) }
                        }
                        ActionBtn(if (tr) "Cihaz Limiti Düzenle" else "Edit device limit", Icons.Filled.FormatListNumbered, Color(0xFF448AFF)) {
                            limitOpen = true
                        }
                        ActionBtn(
                            if (user.isBanned) (if (tr) "Ban Kaldır" else "Unban") else (if (tr) "Banla" else "Ban"),
                            if (user.isBanned) Icons.Filled.CheckCircle else Icons.Filled.Block,
                            if (user.isBanned) Color(0xFF4CAF50) else Color(0xFFFF5252)
                        ) {
                            scope.launch { toast(vm.admin.manageUser(user.uid, if (user.isBanned) "unban" else "ban")) }
                        }
                    }
                }
                Spacer(Modifier.height(24.dp))
            }
        }
    }
    if (grantOpen) GrantDialog(tr, onDismiss = { grantOpen = false }) { days, note ->
        grantOpen = false
        scope.launch { toast(vm.admin.grantPremium(user.uid, days, note)) }
    }
    if (limitOpen) LimitDialog(tr, user.maxDevices, { limitOpen = false }) { limit ->
        limitOpen = false
        scope.launch { toast(vm.admin.manageUser(user.uid, "set_device_limit", limit)) }
    }
}

@Composable
private fun Section(title: String, color: Color, content: @Composable () -> Unit) {
    AdminGlassCard {
        Text(title, color = color, fontWeight = FontWeight.Bold, fontSize = 15.sp)
        Spacer(Modifier.height(10.dp))
        content()
    }
}

@Composable
private fun ActionBtn(label: String, icon: ImageVector, color: Color, onClick: () -> Unit) {
    Row(
        Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(color.copy(alpha = 0.16f))
            .border(1.dp, color.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
            .rayClickable(onClick)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, null, tint = color, modifier = Modifier.size(18.dp))
        Spacer(Modifier.size(8.dp))
        Text(label, color = color, fontWeight = FontWeight.Bold, fontSize = 13.sp)
    }
}

@Composable
private fun GrantDialog(tr: Boolean, onDismiss: () -> Unit, onOk: (Int, String) -> Unit) {
    var days by remember { mutableIntStateOf(30) }
    var note by remember { mutableStateOf("") }
    Dialog(onDismissRequest = onDismiss) {
        Column(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(Color(0xFF1E293B)).padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(if (tr) "Premium Ver" else "Grant premium", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
            Text(if (tr) "Süre Seçin" else "Duration", color = Color.White.copy(alpha = 0.7f), fontSize = 13.sp)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(0 to if (tr) "Sınırsız" else "Unlimited", 7 to "7", 30 to "30", 90 to "90", 365 to "365").forEach { (v, label) ->
                    AdminChip(label, Color(0xFFF59E0B), days == v) { days = v }
                }
            }
            AdminDarkField(note, { note = it }, if (tr) "Not (opsiyonel)" else "Note", if (tr) "Not…" else "Note…")
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                GlassButton(if (tr) "İptal" else "Cancel") { onDismiss() }
                GlassButton(if (tr) "Onayla" else "Confirm", primary = true) { onOk(days, note) }
            }
        }
    }
}

@Composable
private fun LimitDialog(tr: Boolean, current: Int, onDismiss: () -> Unit, onOk: (Int) -> Unit) {
    var value by remember { mutableStateOf(current.toString()) }
    Dialog(onDismissRequest = onDismiss) {
        Column(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(Color(0xFF1E293B)).padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(if (tr) "Cihaz Limitini Düzenle" else "Edit device limit", color = Color.White, fontWeight = FontWeight.Bold)
            AdminDarkField(value, { value = it.filter { ch -> ch.isDigit() }.take(3) }, if (tr) "Yeni Limit" else "New limit", "3")
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                GlassButton(if (tr) "İptal" else "Cancel") { onDismiss() }
                GlassButton(if (tr) "Kaydet" else "Save", primary = true) {
                    value.toIntOrNull()?.let(onOk) ?: onDismiss()
                }
            }
        }
    }
}
