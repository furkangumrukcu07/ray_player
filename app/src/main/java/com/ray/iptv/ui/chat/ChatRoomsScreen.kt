package com.ray.iptv.ui.chat

import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.Forum
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.SupportAgent
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.tv.material3.Icon
import androidx.tv.material3.Text
import com.ray.iptv.ui.Dest
import com.ray.iptv.ui.RayViewModel
import com.ray.iptv.ui.admin.AdminBackdrop
import com.ray.iptv.ui.admin.AdminDarkField
import com.ray.iptv.ui.admin.AdminGradientButton
import com.ray.iptv.ui.components.GlassButton
import com.ray.iptv.ui.input.rayClickable
import kotlinx.coroutines.launch
import androidx.compose.material.icons.filled.Login

private data class ChatRoom(val code: String, val name: String, val flag: String)

@Composable
fun ChatRoomsScreen(vm: RayViewModel, tr: Boolean, onExit: () -> Unit) {
    val account by vm.account.collectAsState()
    BackHandler(onBack = onExit)
    AdminBackdrop {
        Column(Modifier.fillMaxSize()) {
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = Color.White, modifier = Modifier.size(42.dp).padding(8.dp).rayClickable(onExit))
                Icon(Icons.Filled.Forum, null, tint = Color.White, modifier = Modifier.size(22.dp))
                Spacer(Modifier.size(8.dp))
                Text(if (tr) "Sohbet" else "Chat", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.ExtraBold, modifier = Modifier.weight(1f))
            }
            if (!account.signedIn) {
                ChatSignInGate(vm, tr)
            } else {
                RoomList(vm, tr, account.isAdmin)
            }
        }
    }
}

@Composable
private fun ChatSignInGate(vm: RayViewModel, tr: Boolean) {
    var open by remember { mutableStateOf(false) }
    var busy by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                Modifier.size(72.dp).clip(RoundedCornerShape(20.dp)).background(Color.White.copy(alpha = 0.06f)).border(1.dp, Color.White.copy(alpha = 0.14f), RoundedCornerShape(20.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Filled.Lock, null, tint = Color.White.copy(alpha = 0.7f), modifier = Modifier.size(36.dp))
            }
            Spacer(Modifier.height(18.dp))
            Text(if (tr) "Sohbete katılmak için giriş yapın" else "Sign in to join the chat", color = Color.White, fontSize = 17.sp, fontWeight = FontWeight.ExtraBold, textAlign = TextAlign.Center)
            Spacer(Modifier.height(10.dp))
            Text(
                if (tr) "Sohbet odalarına yazmak ve yöneticiye mesaj göndermek için Google ile oturum açın. Firebase bağlanınca gerçek Google girişi aktif olacak."
                else "Sign in with Google to join rooms and message the admin. Real Google sign-in will activate once Firebase is wired.",
                color = Color.White.copy(alpha = 0.72f),
                fontSize = 13.5.sp,
                textAlign = TextAlign.Center,
                lineHeight = 18.sp
            )
            Spacer(Modifier.height(22.dp))
    val googleSignInLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        scope.launch {
            val ok = vm.handleGoogleSignInIntent(result.data)
            if (!ok) open = true
        }
    }

    AdminGradientButton(
        if (busy) (if (tr) "Oturum açılıyor…" else "Signing in…") else (if (tr) "Google ile oturum aç" else "Sign in with Google"),
        Icons.Filled.Login,
        busy,
        listOf(Color(0xFF6366F1), Color(0xFF8B5CF6))
    ) {
        googleSignInLauncher.launch(vm.getGoogleSignInIntent())
    }
        }
    }
    if (open) LocalSignInDialog(tr, onDismiss = { open = false }) { email, name ->
        open = false
        vm.signInLocal(email, name)
    }
}

@Composable
fun LocalSignInDialog(tr: Boolean, onDismiss: () -> Unit, onOk: (String, String) -> Unit) {
    var email by remember { mutableStateOf("") }
    var name by remember { mutableStateOf("") }
    Dialog(onDismissRequest = onDismiss) {
        Column(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(Color(0xFF1E293B)).padding(20.dp)
        ) {
            Text(if (tr) "Geçici oturum" else "Temporary session", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
            Spacer(Modifier.height(8.dp))
            Text(
                if (tr) "Google entegrasyonu sonra tamamlanacak. Şimdilik e-posta ile yerel oturum açılır."
                else "Google integration comes later. For now this stores a local session by email.",
                color = Color.White.copy(alpha = 0.7f),
                fontSize = 13.sp
            )
            Spacer(Modifier.height(16.dp))
            AdminDarkField(email, { email = it }, if (tr) "E-posta" else "Email", "furkangumrukcu07@gmail.com")
            Spacer(Modifier.height(12.dp))
            AdminDarkField(name, { name = it }, if (tr) "Ad (opsiyonel)" else "Name (optional)", "")
            Spacer(Modifier.height(16.dp))
            Row {
                GlassButton(if (tr) "İptal" else "Cancel") { onDismiss() }
                Spacer(Modifier.weight(1f))
                GlassButton(if (tr) "Giriş" else "Sign in", primary = true) {
                    if (email.contains("@")) onOk(email.trim(), name.trim())
                }
            }
        }
    }
}

@Composable
private fun RoomList(vm: RayViewModel, tr: Boolean, isAdmin: Boolean) {
    val rooms = remember {
        listOf(
            ChatRoom("tr", "Türkçe", "🇹🇷"),
            ChatRoom("en", "English", "🇬🇧"),
            ChatRoom("fr", "Français", "🇫🇷"),
            ChatRoom("ar", "العربية", "🇸🇦"),
            ChatRoom("de", "Deutsch", "🇩🇪"),
            ChatRoom("es", "Español", "🇪🇸"),
            ChatRoom("ru", "Русский", "🇷🇺"),
            ChatRoom("it", "Italiano", "🇮🇹")
        )
    }
    LazyColumn(Modifier.fillMaxSize().padding(horizontal = 8.dp)) {
        if (isAdmin) {
            item {
                GlassTile(
                    icon = { Icon(Icons.Filled.AdminPanelSettings, null, tint = Color(0xFF7C4DFF), modifier = Modifier.size(28.dp)) },
                    title = "Admin Paneli",
                    subtitle = if (tr) "Yönetici araçları ve istatistikler" else "Admin tools and stats"
                ) { vm.go(Dest.ADMIN) }
            }
        }
        item {
            GlassTile(
                icon = { Icon(Icons.Filled.SupportAgent, null, tint = Color(0xFF448AFF), modifier = Modifier.size(26.dp)) },
                title = if (isAdmin) {
                    if (tr) "Kullanıcı Mesajları" else "User messages"
                } else {
                    if (tr) "Yöneticiye Mesaj Gönder" else "Message the admin"
                },
                subtitle = if (isAdmin) {
                    if (tr) "Kullanıcılardan gelen özel sohbetler" else "Private threads from users"
                } else {
                    if (tr) "Destek için yöneticiyle özel konuş" else "Private chat with support"
                }
            ) {
                vm.toast.value = if (tr) "Sohbet Firebase bağlanınca aktif olacak." else "Chat will go live once Firebase is wired."
            }
        }
        itemsIndexed(rooms) { _, room ->
            GlassTile(
                icon = { Text(room.flag, fontSize = 22.sp) },
                title = room.name,
                subtitle = if (tr) "${room.name} odası · Firebase sonra" else "${room.name} room · Firebase later"
            ) {
                vm.toast.value = if (tr) "Sohbet odaları Firebase bağlanınca açılacak." else "Chat rooms will open once Firebase is wired."
            }
        }
        item { Spacer(Modifier.height(28.dp)) }
    }
}

@Composable
private fun GlassTile(icon: @Composable () -> Unit, title: String, subtitle: String, onClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 4.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(Color.White.copy(alpha = 0.06f))
            .border(1.dp, Color.White.copy(alpha = 0.10f), RoundedCornerShape(16.dp))
            .rayClickable(onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(Modifier.size(36.dp), contentAlignment = Alignment.Center) { icon() }
        Spacer(Modifier.size(12.dp))
        Column(Modifier.weight(1f)) {
            Text(title, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
            Text(subtitle, color = Color.White.copy(alpha = 0.7f), fontSize = 13.sp, maxLines = 1)
        }
        Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null, tint = Color.White.copy(alpha = 0.54f))
    }
}
