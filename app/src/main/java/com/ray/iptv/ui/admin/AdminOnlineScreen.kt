package com.ray.iptv.ui.admin

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.filled.ChatBubble
import androidx.compose.material.icons.filled.Message
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Send
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.tv.material3.Icon
import androidx.tv.material3.Text
import coil.compose.AsyncImage
import com.ray.iptv.data.admin.OnlineUser
import com.ray.iptv.ui.RayViewModel
import com.ray.iptv.ui.components.GlassButton
import com.ray.iptv.ui.glass.DarkGlassPopupTheme
import com.ray.iptv.ui.input.rayClickable
import kotlinx.coroutines.launch

@Composable
fun AdminOnlineScreen(vm: RayViewModel, tr: Boolean, onBack: () -> Unit) {
    var users by remember { mutableStateOf<List<OnlineUser>>(emptyList()) }
    var target by remember { mutableStateOf<OnlineUser?>(null) }
    LaunchedEffect(Unit) {
        users = runCatching { vm.admin.listOnline() }.getOrDefault(emptyList())
    }
    AdminBackdrop {
        Column(Modifier.fillMaxSize()) {
            AdminTopBar(if (tr) "Çevrimiçi Kullanıcılar" else "Online users", onBack = onBack)
            if (users.isEmpty()) {
                AdminEmpty(if (tr) "Şu anda çevrimiçi kullanıcı yok." else "No one is online right now.")
            } else {
                LazyColumn(Modifier.fillMaxSize().padding(16.dp)) {
                    items(users, key = { it.uid }) { u ->
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .padding(bottom = 12.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .background(Color.White.copy(alpha = 0.05f))
                                .border(1.dp, Color.White.copy(alpha = 0.10f), RoundedCornerShape(16.dp))
                                .padding(horizontal = 20.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                Modifier.size(48.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.08f)),
                                contentAlignment = Alignment.Center
                            ) {
                                if (u.photoUrl.isNotBlank()) {
                                    AsyncImage(u.photoUrl, null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                                } else {
                                    Icon(Icons.Filled.Person, null, tint = Color.White.copy(alpha = 0.7f))
                                }
                            }
                            Spacer(Modifier.size(12.dp))
                            Column(Modifier.weight(1f)) {
                                Text(u.name, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                if (u.email.isNotBlank()) Text(u.email, color = Color.White.copy(alpha = 0.6f), fontSize = 13.sp)
                            }
                            Box(
                                Modifier.size(42.dp).clip(CircleShape).background(Color(0xFF448AFF).copy(alpha = 0.10f)).rayClickable(onClick = { target = u }),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Filled.ChatBubble, null, tint = Color(0xFF448AFF), modifier = Modifier.size(20.dp))
                            }
                        }
                    }
                }
            }
        }
    }
    target?.let { u ->
        MessageDialog(tr, u, onDismiss = { target = null }) { text ->
            val notice = vm.admin.sendAdminMessage(u.uid, text)
            vm.toast.value = notice.message
            if (notice.ok) target = null
        }
    }
}

@Composable
private fun MessageDialog(tr: Boolean, user: OnlineUser, onDismiss: () -> Unit, onSend: suspend (String) -> Unit) {
    var text by remember { mutableStateOf("") }
    var sending by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    DarkGlassPopupTheme {
        Dialog(onDismissRequest = onDismiss) {
            Column(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(24.dp))
                    .background(Color(0xFF10131B).copy(alpha = 0.96f))
                    .border(1.2.dp, Color(0xFF64D2FF).copy(alpha = 0.35f), RoundedCornerShape(24.dp))
                    .padding(24.dp)
            ) {
                Icon(Icons.Filled.Message, null, tint = Color(0xFF64D2FF), modifier = Modifier.size(48.dp).align(Alignment.CenterHorizontally))
                Spacer(Modifier.height(16.dp))
                Text("${user.name}${if (tr) "'a Mesaj" else " — message"}", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold, modifier = Modifier.align(Alignment.CenterHorizontally))
                Spacer(Modifier.height(16.dp))
                AdminDarkField(text, { text = it }, "", if (tr) "Mesajınızı yazın..." else "Write a message…", singleLine = false, minLines = 3)
                Spacer(Modifier.height(24.dp))
                Row {
                    GlassButton(if (tr) "İptal" else "Cancel") { onDismiss() }
                    Spacer(Modifier.weight(1f))
                    AdminGradientButton(
                        if (tr) "Gönder" else "Send",
                        Icons.Filled.Send,
                        sending,
                        listOf(Color(0xFF64D2FF), Color(0xFF0A84FF))
                    ) {
                        val t = text.trim()
                        if (t.isEmpty()) return@AdminGradientButton
                        scope.launch {
                            sending = true
                            onSend(t)
                            sending = false
                        }
                    }
                }
            }
        }
    }
}
