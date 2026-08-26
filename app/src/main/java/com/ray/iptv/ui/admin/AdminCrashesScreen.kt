package com.ray.iptv.ui.admin

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Icon
import androidx.tv.material3.Text
import com.ray.iptv.data.admin.CrashReport
import com.ray.iptv.ui.RayViewModel
import com.ray.iptv.ui.input.rayClickable

@Composable
fun AdminCrashesScreen(vm: RayViewModel, tr: Boolean, onBack: () -> Unit) {
    var reports by remember { mutableStateOf<List<CrashReport>>(emptyList()) }
    val ctx = LocalContext.current
    val crashlytics = "https://console.firebase.google.com/project/_/crashlytics"
    fun openConsole() {
        runCatching { ctx.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(crashlytics))) }
    }
    LaunchedEffect(Unit) {
        reports = runCatching { vm.admin.listCrashes() }.getOrDefault(emptyList())
    }
    AdminBackdrop {
        Column(Modifier.fillMaxSize()) {
            AdminTopBar(
                title = if (tr) "Hata ve Çökme Raporları" else "Crash reports",
                onBack = onBack,
                trailing = {
                    Icon(Icons.Filled.OpenInNew, null, tint = Color.White, modifier = Modifier.size(40.dp).padding(8.dp).rayClickable(onClick = { openConsole() }))
                }
            )
            AdminGlassCard(Modifier.padding(16.dp), onClick = { openConsole() }) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        Modifier.size(56.dp).clip(CircleShape).background(Color(0xFFFF5252)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Filled.BugReport, null, tint = Color.White, modifier = Modifier.size(32.dp))
                    }
                    Spacer(Modifier.size(16.dp))
                    Column(Modifier.weight(1f)) {
                        Text("Firebase Crashlytics", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(4.dp))
                        Text(
                            if (tr) "Canlı ve detaylı çökme raporlarını tarayıcıda açmak için dokunun."
                            else "Tap to open live crash reports in the browser.",
                            color = Color.White.copy(alpha = 0.7f),
                            fontSize = 13.sp
                        )
                    }
                    Icon(Icons.Filled.OpenInNew, null, tint = Color.White.copy(alpha = 0.54f))
                }
            }
            Text(
                if (tr) "Eski Dahili Kayıtlar" else "Legacy in-app logs",
                color = Color.White.copy(alpha = 0.54f),
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )
            if (reports.isEmpty()) {
                AdminEmpty(if (tr) "Henüz kaydedilmiş bir hata raporu bulunmuyor." else "No crash reports yet.")
            } else {
                LazyColumn(Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
                    items(reports, key = { it.id }) { CrashCard(it) }
                }
            }
        }
    }
}

@Composable
private fun CrashCard(item: CrashReport) {
    var open by remember { mutableStateOf(false) }
    Column(
        Modifier
            .fillMaxWidth()
            .padding(bottom = 12.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(Color.White.copy(alpha = 0.05f))
            .rayClickable(onClick = { open = !open })
            .padding(16.dp)
    ) {
        Text(
            item.errorMessage,
            color = if (item.fatal) Color(0xFFFF5252) else Color(0xFFFFAB40),
            fontWeight = FontWeight.Bold,
            maxLines = 2
        )
        Spacer(Modifier.height(8.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Filled.AccessTime, null, tint = Color.White.copy(alpha = 0.54f), modifier = Modifier.size(14.dp))
            Text(" ${item.dateString}   ", color = Color.White.copy(alpha = 0.54f), fontSize = 12.sp)
            Icon(Icons.Filled.Memory, null, tint = Color.White.copy(alpha = 0.54f), modifier = Modifier.size(14.dp))
            Text(" ${item.ram}   ", color = Color.White.copy(alpha = 0.54f), fontSize = 12.sp)
            Icon(Icons.Filled.PhoneAndroid, null, tint = Color.White.copy(alpha = 0.54f), modifier = Modifier.size(14.dp))
            Text(" ${item.platform}", color = Color.White.copy(alpha = 0.54f), fontSize = 12.sp)
        }
        AnimatedVisibility(open) {
            Text(
                item.stackTrace.ifBlank { "Stack trace yok." },
                color = Color.White.copy(alpha = 0.7f),
                fontFamily = FontFamily.Monospace,
                fontSize = 11.sp,
                modifier = Modifier.fillMaxWidth().padding(top = 12.dp).background(Color.Black.copy(alpha = 0.26f)).padding(16.dp)
            )
        }
    }
}
