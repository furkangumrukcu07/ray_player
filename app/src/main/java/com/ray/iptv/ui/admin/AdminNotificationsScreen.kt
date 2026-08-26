package com.ray.iptv.ui.admin

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AlarmAdd
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.NotificationsOff
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Send
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Icon
import androidx.tv.material3.Text
import com.ray.iptv.data.admin.NotificationRecord
import com.ray.iptv.ui.RayViewModel
import com.ray.iptv.ui.input.rayClickable
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

private data class Segment(val id: String, val label: String, val color: Color)

@Composable
fun AdminNotificationsScreen(vm: RayViewModel, tr: Boolean, onBack: () -> Unit) {
    var tab by remember { mutableIntStateOf(0) }
    val segments = listOf(
        Segment("all", if (tr) "Tümü" else "All", Color(0xFF6366F1)),
        Segment("premium", "Premium", Color(0xFFF59E0B)),
        Segment("trial", "Trial", Color(0xFF10B981)),
        Segment("tv", "TV", Color(0xFF3B82F6)),
        Segment("mobile", if (tr) "Mobil" else "Mobile", Color(0xFFEC4899))
    )
    AdminBackdrop {
        Column(Modifier.fillMaxSize()) {
            AdminTopBar(if (tr) "Bildirim Merkezi" else "Notification center", onBack = onBack)
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf(
                    (if (tr) "Hızlı Gönder" else "Quick send") to Icons.Filled.Send,
                    (if (tr) "Zamanla" else "Schedule") to Icons.Filled.Schedule,
                    (if (tr) "Geçmiş" else "History") to Icons.Filled.History
                ).forEachIndexed { i, (label, icon) ->
                    val on = tab == i
                    Row(
                        Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (on) Color(0xFF6366F1).copy(alpha = 0.2f) else Color.White.copy(alpha = 0.04f))
                            .border(1.dp, if (on) Color(0xFF6366F1) else Color.White.copy(alpha = 0.08f), RoundedCornerShape(12.dp))
                            .rayClickable(onClick = { tab = i })
                            .padding(vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(icon, null, tint = if (on) Color(0xFF6366F1) else Color.White.copy(alpha = 0.54f), modifier = Modifier.size(16.dp))
                        Spacer(Modifier.size(6.dp))
                        Text(label, color = if (on) Color(0xFF6366F1) else Color.White.copy(alpha = 0.54f), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
            when (tab) {
                0 -> QuickTab(vm, tr, segments)
                1 -> ScheduleTab(vm, tr, segments)
                else -> HistoryTab(vm, tr)
            }
        }
    }
}

@Composable
private fun QuickTab(vm: RayViewModel, tr: Boolean, segments: List<Segment>) {
    var title by remember { mutableStateOf("") }
    var body by remember { mutableStateOf("") }
    var segment by remember { mutableStateOf("all") }
    var loading by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp)) {
        AdminGlassCard {
            Text(if (tr) "Hızlı Bildirim Gönder" else "Send a quick notification", color = Color(0xFF6366F1), fontWeight = FontWeight.Bold, fontSize = 16.sp)
            Spacer(Modifier.height(20.dp))
            AdminDarkField(title, { title = it }, if (tr) "Bildirim Başlığı" else "Title", if (tr) "Örn: Yeni özellik yayında!" else "e.g. New feature is live")
            Spacer(Modifier.height(14.dp))
            AdminDarkField(body, { body = it }, if (tr) "Bildirim İçeriği" else "Body", if (tr) "Kullanıcılara gösterilecek mesaj..." else "Message shown to users…", singleLine = false, minLines = 3)
            Spacer(Modifier.height(20.dp))
            Text(if (tr) "Hedef Segment" else "Segment", color = Color.White.copy(alpha = 0.7f), fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                segments.forEach { AdminChip(it.label, it.color, segment == it.id) { segment = it.id } }
            }
            Spacer(Modifier.height(24.dp))
            AdminGradientButton(if (tr) "Bildirimi Gönder" else "Send notification", Icons.Filled.Send, loading, listOf(Color(0xFF6366F1), Color(0xFF8B5CF6))) {
                if (title.isBlank() || body.isBlank()) {
                    vm.toast.value = if (tr) "Başlık ve içerik zorunludur." else "Title and body are required."
                    return@AdminGradientButton
                }
                scope.launch {
                    loading = true
                    val r = vm.admin.sendSegmented(segment, title.trim(), body.trim())
                    vm.toast.value = r.message
                    if (r.ok) {
                        title = ""
                        body = ""
                    }
                    loading = false
                }
            }
        }
    }
}

@Composable
private fun ScheduleTab(vm: RayViewModel, tr: Boolean, segments: List<Segment>) {
    var title by remember { mutableStateOf("") }
    var body by remember { mutableStateOf("") }
    var segment by remember { mutableStateOf("all") }
    var atMs by remember { mutableStateOf<Long?>(null) }
    var loading by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val ctx = LocalContext.current
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp)) {
        AdminGlassCard {
            Text(if (tr) "Zamanlanmış Bildirim" else "Scheduled notification", color = Color(0xFF10B981), fontWeight = FontWeight.Bold, fontSize = 16.sp)
            Spacer(Modifier.height(20.dp))
            AdminDarkField(title, { title = it }, if (tr) "Bildirim Başlığı" else "Title", if (tr) "Başlık girin..." else "Title…")
            Spacer(Modifier.height(14.dp))
            AdminDarkField(body, { body = it }, if (tr) "Bildirim İçeriği" else "Body", if (tr) "Mesaj içeriği..." else "Message…", singleLine = false, minLines = 3)
            Spacer(Modifier.height(20.dp))
            Text(if (tr) "Hedef Segment" else "Segment", color = Color.White.copy(alpha = 0.7f), fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                segments.forEach { AdminChip(it.label, it.color, segment == it.id) { segment = it.id } }
            }
            Spacer(Modifier.height(20.dp))
            Row(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(Color(0xFF1E293B))
                    .border(1.dp, if (atMs != null) Color(0xFF10B981) else Color.White.copy(alpha = 0.08f), RoundedCornerShape(14.dp))
                    .rayClickable(onClick = {
                        val cal = Calendar.getInstance()
                        DatePickerDialog(ctx, { _, y, m, d ->
                            TimePickerDialog(ctx, { _, h, min ->
                                cal.set(y, m, d, h, min, 0)
                                atMs = cal.timeInMillis
                            }, cal.get(Calendar.HOUR_OF_DAY), cal.get(Calendar.MINUTE), true).show()
                        }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)).show()
                    })
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Filled.CalendarToday, null, tint = if (atMs != null) Color(0xFF10B981) else Color.White.copy(alpha = 0.38f), modifier = Modifier.size(20.dp))
                Spacer(Modifier.size(12.dp))
                Text(
                    atMs?.let { SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault()).format(Date(it)) }
                        ?: if (tr) "Tarih ve saat seçin" else "Pick date and time",
                    color = if (atMs != null) Color.White else Color.White.copy(alpha = 0.38f),
                    fontSize = 15.sp
                )
            }
            Spacer(Modifier.height(24.dp))
            AdminGradientButton(if (tr) "Bildirimi Zamanla" else "Schedule", Icons.Filled.AlarmAdd, loading, listOf(Color(0xFF10B981), Color(0xFF059669))) {
                val whenMs = atMs
                if (title.isBlank() || body.isBlank() || whenMs == null) {
                    vm.toast.value = if (tr) "Tüm alanları doldurun ve tarih seçin." else "Fill all fields and pick a date."
                    return@AdminGradientButton
                }
                scope.launch {
                    loading = true
                    val r = vm.admin.scheduleNotification(segment, title.trim(), body.trim(), whenMs)
                    vm.toast.value = r.message
                    if (r.ok) {
                        title = ""
                        body = ""
                        atMs = null
                    }
                    loading = false
                }
            }
        }
    }
}

@Composable
private fun HistoryTab(vm: RayViewModel, tr: Boolean) {
    var items by remember { mutableStateOf<List<NotificationRecord>>(emptyList()) }
    LaunchedEffect(Unit) {
        items = runCatching { vm.admin.notificationHistory() }.getOrDefault(emptyList())
    }
    if (items.isEmpty()) {
        Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            Icon(Icons.Filled.NotificationsOff, null, tint = Color.White.copy(alpha = 0.24f), modifier = Modifier.size(64.dp))
            Spacer(Modifier.height(16.dp))
            Text(if (tr) "Bildirim geçmişi boş" else "Notification history is empty", color = Color.White.copy(alpha = 0.38f), fontSize = 16.sp)
        }
    } else {
        LazyColumn(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            items(items, key = { it.id }) { rec ->
                val (color, icon, label) = when (rec.type) {
                    "scheduled" -> Triple(Color(0xFF10B981), Icons.Filled.Schedule, if (tr) "Zamanlanmış" else "Scheduled")
                    "user" -> Triple(Color(0xFFF59E0B), Icons.Filled.Person, if (tr) "Kullanıcı" else "User")
                    else -> Triple(Color(0xFF6366F1), Icons.Filled.Group, if (tr) "Segmentli" else "Segment")
                }
                AdminGlassCard {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(icon, null, tint = color, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.size(8.dp))
                        Text(label, color = color, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        Spacer(Modifier.weight(1f))
                        Text(rec.segment, color = Color.White.copy(alpha = 0.54f), fontSize = 12.sp)
                    }
                    Spacer(Modifier.height(8.dp))
                    Text(rec.title, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    if (rec.body.isNotBlank()) {
                        Spacer(Modifier.height(4.dp))
                        Text(rec.body, color = Color.White.copy(alpha = 0.7f), fontSize = 13.sp)
                    }
                    if (rec.sentAt > 0) {
                        Spacer(Modifier.height(8.dp))
                        Text(SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault()).format(Date(rec.sentAt)), color = Color.White.copy(alpha = 0.45f), fontSize = 12.sp)
                    }
                }
            }
        }
    }
}
