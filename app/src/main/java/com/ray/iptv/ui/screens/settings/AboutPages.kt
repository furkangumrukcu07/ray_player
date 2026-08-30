package com.ray.iptv.ui.screens.settings

import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.activity.compose.BackHandler
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.dp
import com.ray.iptv.ui.input.rayClickable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.LaptopMac
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.AutoFixHigh
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.DesktopWindows
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Forum
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.MailOutline
import androidx.compose.material.icons.filled.Notes
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PrivacyTip
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Quiz
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.SupportAgent
import androidx.compose.material.icons.filled.SystemUpdateAlt
import androidx.compose.material.icons.filled.Wallpaper
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.tv.material3.Icon
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.ray.iptv.ui.RayViewModel
import com.ray.iptv.ui.components.GlassButton
import com.ray.iptv.ui.components.GlassToggle
import com.ray.iptv.ui.glass.DarkGlassPopupTheme
import com.ray.iptv.ui.glass.GlassPanel
import com.ray.iptv.ui.theme.LocalGlass
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import androidx.compose.ui.focus.FocusRequester
import java.net.HttpURLConnection
import java.net.URL

private enum class AboutSub { LIST, CONTACT, FAQ, PRIVACY, APPS }

@Composable
internal fun AboutRoot(
    vm: RayViewModel,
    tr: Boolean,
    focusRequester: FocusRequester? = null,
    onBack: () -> Unit
) {
    var sub by remember { mutableStateOf(AboutSub.LIST) }
    BackHandler(enabled = sub != AboutSub.LIST) {
        sub = if (sub == AboutSub.FAQ) AboutSub.CONTACT else AboutSub.LIST
    }
    when (sub) {
        AboutSub.LIST -> AboutHomePage(
            vm = vm,
            tr = tr,
            onBack = onBack,
            focusRequester = focusRequester,
            onContact = { sub = AboutSub.CONTACT },
            onPrivacy = { sub = AboutSub.PRIVACY },
            onApps = { sub = AboutSub.APPS }
        )
        AboutSub.CONTACT -> ContactUsPage(
            vm = vm,
            tr = tr,
            onBack = { sub = AboutSub.LIST },
            onFaq = { sub = AboutSub.FAQ }
        )
        AboutSub.FAQ -> FaqPage(tr) { sub = AboutSub.CONTACT }
        AboutSub.PRIVACY -> PrivacyPolicyPage(tr) { sub = AboutSub.LIST }
        AboutSub.APPS -> OtherAppsPage(vm, tr) { sub = AboutSub.LIST }
    }
}

@Composable
private fun AboutHomePage(
    vm: RayViewModel,
    tr: Boolean,
    onBack: () -> Unit,
    focusRequester: FocusRequester? = null,
    onContact: () -> Unit,
    onPrivacy: () -> Unit,
    onApps: () -> Unit
) {
    val ctx = LocalContext.current
    val version = remember {
        runCatching { ctx.packageManager.getPackageInfo(ctx.packageName, 0).versionName }
            .getOrNull().orEmpty().ifBlank { "1.3.17" }
    }
    val pkg = remember { ctx.packageName }
    val scope = rememberCoroutineScope()
    var checking by remember { mutableStateOf(false) }
    var changelog by remember { mutableStateOf(false) }
    var admin by remember { mutableStateOf(false) }
    var update by remember { mutableStateOf<UpdateResult?>(null) }
    LazyColumn(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item {
            AboutHeader(
                title = if (tr) "Hakkında" else "About",
                hint = if (tr) {
                    "Ray IPTV Player $version\nSürüm notları, güncelleme ve iletişim"
                } else {
                    "Ray IPTV Player $version\nRelease notes, updates, and contact"
                },
                onBack = onBack,
                focusRequester = focusRequester
            )
        }
        item {
            AboutTile(
                icon = Icons.Filled.Notes,
                title = if (tr) "Sürüm notları" else "Release notes",
                subtitle = if (tr) "Bu sürümde neler değişti" else "What changed in this version"
            ) { changelog = true }
        }
        item {
            AboutTile(
                icon = Icons.Filled.SystemUpdateAlt,
                title = if (checking) {
                    if (tr) "Denetleniyor…" else "Checking…"
                } else {
                    if (tr) "Güncelleme denetle" else "Check for updates"
                },
                subtitle = if (tr) "Play Store’da yeni sürüm var mı bak" else "See if a newer version is on the Play Store",
                enabled = !checking
            ) {
                checking = true
                scope.launch {
                    val store = fetchPlayStoreVersion(pkg)
                    checking = false
                    update = when {
                        store.isNullOrBlank() -> UpdateResult.Failed
                        isStoreNewer(version, store) -> UpdateResult.Available(store)
                        else -> UpdateResult.Latest
                    }
                }
            }
        }
        item {
            AboutTile(
                icon = Icons.Filled.SupportAgent,
                title = if (tr) "Bize Ulaşın" else "Contact Us",
                subtitle = if (tr) "Telegram kanalımız ve sorun bildirimi" else "Our Telegram channel and issue reporting",
                onClick = onContact
            )
        }
        item {
            AboutTile(
                icon = Icons.Filled.AdminPanelSettings,
                title = if (tr) "Yönetici" else "Administrator",
                subtitle = if (tr) "Geliştirici iletişim bilgileri" else "Developer contact details"
            ) { admin = true }
        }
        item {
            AboutTile(
                icon = Icons.Filled.PrivacyTip,
                title = if (tr) "Gizlilik politikası" else "Privacy policy",
                subtitle = if (tr) "Uygulama kullanım koşulları ve gizlilik sözleşmesi" else "App terms of use and privacy agreement",
                onClick = onPrivacy
            )
        }
        item {
            AboutTile(
                icon = Icons.Filled.Apps,
                title = if (tr) "Diğer Uygulamalarımız" else "Our Other Apps",
                subtitle = if (tr) "Windows ve Web uygulamalarımız" else "Our Windows and Web apps",
                onClick = onApps
            )
        }
        item {
            AboutTile(
                icon = Icons.Filled.AutoFixHigh,
                title = if (tr) "Kurulum Sihirbazını Başlat" else "Start Setup Wizard",
                subtitle = if (tr) "İlk kurulum adımlarını yeniden çalıştır" else "Run the first-setup steps again"
            ) { vm.restartSetup() }
        }
        item { Spacer(Modifier.height(12.dp)) }
    }
    if (changelog) {
        ScrollTextDialog(
            title = if (tr) "Sürüm notları" else "Release notes",
            body = if (tr) rayChangelogTr else rayChangelogEn,
            close = if (tr) "Kapat" else "Close"
        ) { changelog = false }
    }
    if (admin) AdminAboutDialog(vm, tr) { admin = false }
    update?.let { result ->
        UpdateResultDialog(result, version, pkg, tr, vm) { update = null }
    }
}

@Composable
private fun ContactUsPage(vm: RayViewModel, tr: Boolean, onBack: () -> Unit, onFaq: () -> Unit) {
    val ctx = LocalContext.current
    val version = remember {
        runCatching { ctx.packageManager.getPackageInfo(ctx.packageName, 0).versionName }
            .getOrNull().orEmpty().ifBlank { "1.3.17" }
    }
    LazyColumn(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item {
            AboutHeader(
                title = if (tr) "Bize Ulaşın" else "Contact Us",
                hint = if (tr) "Telegram, SSS ve sorun bildirimi" else "Telegram, FAQ and issue reporting",
                onBack = onBack
            )
        }
        item {
            AboutTile(
                icon = Icons.Filled.Quiz,
                title = if (tr) "Sıkça Sorulan Sorular" else "Frequently Asked Questions",
                subtitle = if (tr) "Özellikler ve oynatma ayarları için rehber" else "Guide for features and playback settings",
                onClick = onFaq
            )
        }
        item {
            AboutTile(
                icon = Icons.Filled.Send,
                title = if (tr) "Telegram Adresimiz" else "Our Telegram",
                subtitle = if (tr) "Resmi Telegram kanalımız" else "Official Telegram channel"
            ) { openUrl(ctx, vm, TELEGRAM, tr) }
        }
        item {
            AboutTile(
                icon = Icons.Filled.Forum,
                title = if (tr) "Admine Mesaj Gönder" else "Message the admin",
                subtitle = if (tr) "WhatsApp ile yöneticiye yaz" else "Message the admin on WhatsApp"
            ) { openUrl(ctx, vm, WHATSAPP, tr) }
        }
        item {
            AboutTile(
                icon = Icons.Filled.MailOutline,
                title = if (tr) "Sorun Bildir" else "Report an issue",
                subtitle = if (tr) "E-posta ile sorununuzu bize iletin" else "Send us your issue by email"
            ) {
                val diag = buildString {
                    appendLine("Ray IPTV Player $version")
                    appendLine("package=${ctx.packageName}")
                    appendLine("sdk=${Build.VERSION.SDK_INT}  ${Build.MANUFACTURER} ${Build.MODEL}")
                }
                val uri = Uri.parse(
                    "mailto:$ISSUE_EMAIL?subject=${Uri.encode("Ray IPTV Player $version")}&body=${Uri.encode(diag)}"
                )
                runCatching {
                    ctx.startActivity(Intent(Intent.ACTION_SENDTO, uri))
                }.onFailure {
                    vm.toast.value = if (tr) "E-posta uygulaması bulunamadı" else "No email app found"
                }
            }
        }
        item { Spacer(Modifier.height(12.dp)) }
    }
}

@Composable
private fun FaqPage(tr: Boolean, onBack: () -> Unit) {
    var query by remember { mutableStateOf("") }
    var openKey by remember { mutableStateOf<String?>(null) }
    val entries = remember(tr) { faqEntries(tr) }
    val filtered = remember(query, entries) {
        val q = query.trim().lowercase()
        if (q.isEmpty()) entries else entries.filter {
            it.question.lowercase().contains(q) || it.answer.lowercase().contains(q)
        }
    }
    LazyColumn(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item {
            AboutHeader(
                title = if (tr) "Sıkça Sorulan Sorular" else "Frequently Asked Questions",
                hint = if (tr) "Soru ara…" else "Search questions…",
                onBack = onBack
            )
        }
        item {
            com.ray.iptv.ui.screens.onboarding.GlassField(
                if (tr) "Soru ara…" else "Search questions…",
                query
            ) { query = it }
        }
        if (filtered.isEmpty()) {
            item {
                Text(
                    if (tr) "Aramanızla eşleşen soru bulunamadı." else "No matching questions.",
                    color = LocalGlass.current.muted,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 12.dp)
                )
            }
        } else {
            itemsIndexed(filtered, key = { _, e -> e.key }) { _, e ->
                val open = openKey == e.key
                AboutTile(
                    icon = Icons.Filled.Quiz,
                    title = e.question,
                    subtitle = if (open) e.answer else e.answer.take(90).let { if (e.answer.length > 90) "$it…" else it }
                ) { openKey = if (open) null else e.key }
            }
        }
        item { Spacer(Modifier.height(12.dp)) }
    }
}

@Composable
private fun PrivacyPolicyPage(tr: Boolean, onBack: () -> Unit) {
    val g = LocalGlass.current
    val accent = g.accent
    val sections = if (tr) privacyTr else privacyEn
    Column(Modifier.fillMaxSize()) {
        AboutHeader(
            title = if (tr) "Gizlilik politikası" else "Privacy policy",
            hint = null,
            onBack = onBack
        )
        Column(
            Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            GlassPanel(strong = true, radius = 16.dp, modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    sections.forEachIndexed { i, (title, body) ->
                        if (i > 0) {
                            Spacer(Modifier.height(12.dp))
                            Text(title, color = accent, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        } else {
                            Text(body, color = g.muted, style = MaterialTheme.typography.bodyMedium)
                            return@forEachIndexed
                        }
                        Text(body, color = g.muted, style = MaterialTheme.typography.bodyMedium)
                    }
                    Spacer(Modifier.height(16.dp))
                    GlassButton(if (tr) "Kapat" else "Close", modifier = Modifier.align(Alignment.CenterHorizontally)) { onBack() }
                }
            }
        }
    }
}

@Composable
private fun OtherAppsPage(vm: RayViewModel, tr: Boolean, onBack: () -> Unit) {
    val ctx = LocalContext.current
    LazyColumn(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            AboutHeader(
                title = if (tr) "Diğer Uygulamalarımız" else "Our Other Apps",
                hint = if (tr) "Windows ve Web uygulamalarımız" else "Our Windows and Web apps",
                onBack = onBack
            )
        }
        item {
            AboutTile(
                icon = Icons.Filled.DesktopWindows,
                title = if (tr) "Windows App İndir" else "Download Windows App",
                subtitle = if (tr) "Bilgisayarınız için masaüstü sürüm" else "Desktop edition for your PC"
            ) { openUrl(ctx, vm, WINDOWS_EXE, tr) }
        }
        item {
            AboutTile(
                icon = Icons.Filled.LaptopMac,
                title = "MACOS",
                subtitle = if (tr) "Macbook ve iMac cihazlar için" else "For MacBook and iMac"
            ) { openUrl(ctx, vm, MACOS_DMG, tr) }
        }
        item {
            AboutTile(
                icon = Icons.Filled.Language,
                title = "Minatek",
                subtitle = if (tr) "Web sitemiz ve diğer hizmetler" else "Our website and other services"
            ) { openUrl(ctx, vm, MINATEK, tr) }
        }
        item {
            AboutTile(
                icon = Icons.Filled.Wallpaper,
                title = "Mina Wallpaper",
                subtitle = if (tr) "HD & 4K Duvar Kağıtları Uygulaması" else "HD & 4K wallpaper app"
            ) { openUrl(ctx, vm, WALLPAPER, tr) }
        }
        item { Spacer(Modifier.height(12.dp)) }
    }
}

@Composable
private fun AdminAboutDialog(vm: RayViewModel, tr: Boolean, onDismiss: () -> Unit) {
    val ctx = LocalContext.current
    DarkGlassPopupTheme {
        val g = LocalGlass.current
        Dialog(onDismissRequest = onDismiss) {
            GlassPanel(strong = true, radius = 20.dp, modifier = Modifier.fillMaxWidth().heightIn(max = 560.dp)) {
            Column(
                Modifier.padding(20.dp).verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(g.accent.copy(alpha = 0.2f))
                            .border(1.dp, g.accent.copy(alpha = 0.5f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Filled.AdminPanelSettings, null, tint = g.accent, modifier = Modifier.size(20.dp))
                    }
                    Spacer(Modifier.width(12.dp))
                    Text(if (tr) "Yönetici" else "Administrator", color = g.text, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.ExtraBold)
                }
                Box(
                    Modifier
                        .size(108.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF1A1A1E)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Filled.Person, null, tint = Color.White.copy(alpha = 0.35f), modifier = Modifier.size(52.dp))
                }
                Text("Furkan Gumrukcu", color = g.text, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.ExtraBold)
                Text(
                    if (tr) "Uygulama yöneticisi" else "App administrator",
                    color = g.accent,
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.SemiBold
                )
                AdminContactRow(Icons.Filled.Chat, Color(0xFF25D366), "WhatsApp", "+90 544 645 06 07") {
                    openUrl(ctx, vm, WHATSAPP, tr)
                }
                AdminContactRow(Icons.Filled.Email, g.accent, if (tr) "E-posta" else "Email", ADMIN_EMAIL) {
                    runCatching {
                        ctx.startActivity(
                            Intent(
                                Intent.ACTION_SENDTO,
                                Uri.parse("mailto:$ADMIN_EMAIL?subject=${Uri.encode("Ray IPTV Player")}")
                            )
                        )
                    }.onFailure {
                        vm.toast.value = if (tr) "E-posta uygulaması açılamadı." else "Could not open the email app."
                    }
                }
                AdminContactRow(Icons.Filled.Public, Color.White.copy(alpha = 0.7f), if (tr) "Ülke" else "Country", if (tr) "Türkiye" else "Turkey")
                GlassPanel(radius = 14.dp, modifier = Modifier.fillMaxWidth()) {
                    Text(
                        if (tr) "Bu uygulama tarafıma aittir. Yaşadığınız herhangi bir sorunla ilgili olarak bana iletebilirsiniz."
                        else "This application belongs to me. You can contact me about any issue you experience.",
                        color = g.muted,
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(14.dp)
                    )
                }
                GlassButton(if (tr) "Kapat" else "Close") { onDismiss() }
            }
        }
    }
}
}

@Composable
private fun AdminContactRow(
    icon: ImageVector,
    iconColor: Color,
    label: String,
    value: String,
    onClick: (() -> Unit)? = null
) {
    val g = LocalGlass.current
    GlassPanel(radius = 14.dp, onClick = onClick, modifier = Modifier.fillMaxWidth()) {
        Row(Modifier.padding(horizontal = 12.dp, vertical = 11.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier.size(36.dp).clip(CircleShape).background(iconColor.copy(alpha = 0.16f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, null, tint = iconColor, modifier = Modifier.size(20.dp))
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(label, color = g.muted, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.SemiBold)
                Text(value, color = g.text, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun UpdateResultDialog(
    result: UpdateResult,
    current: String,
    pkg: String,
    tr: Boolean,
    vm: RayViewModel,
    onDismiss: () -> Unit
) {
    val ctx = LocalContext.current
    DarkGlassPopupTheme {
        val g = LocalGlass.current
        val title: String
    val body: String
    when (result) {
        is UpdateResult.Available -> {
            title = if (tr) "Güncelleme mevcut" else "Update available"
            body = if (tr) {
                "Yeni sürüm (${result.store}) Play Store'da yayınlandı. Şimdi güncellemek ister misiniz?"
            } else {
                "Version ${result.store} is on the Play Store. Update now?"
            }
        }
        UpdateResult.Latest -> {
            title = if (tr) "Güncelsiniz" else "You're up to date"
            body = if (tr) "En güncel sürümü kullanıyorsunuz. ($current)" else "You are using the latest version. ($current)"
        }
        UpdateResult.Failed -> {
            title = if (tr) "Denetlenemedi" else "Check failed"
            body = if (tr) {
                "Güncelleme bilgisi alınamadı. İnternet bağlantınızı kontrol edip tekrar deneyin."
            } else {
                "Could not fetch update info. Check your connection and try again."
            }
        }
    }
    Dialog(onDismissRequest = onDismiss) {
        GlassPanel(strong = true, radius = 20.dp, modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(title, color = g.text, style = MaterialTheme.typography.headlineSmall)
                Text(body, color = g.muted, style = MaterialTheme.typography.bodyMedium)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (result is UpdateResult.Available) {
                        GlassButton(if (tr) "Mağazada aç" else "Open in store") {
                            runCatching {
                                ctx.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=$pkg")))
                            }.onFailure {
                                openUrl(ctx, vm, "https://play.google.com/store/apps/details?id=$pkg", tr)
                            }
                            onDismiss()
                        }
                    }
                    GlassButton(if (tr) "Kapat" else "Close") { onDismiss() }
                }
            }
        }
    }
}
}

@Composable
private fun ScrollTextDialog(title: String, body: String, close: String, onDismiss: () -> Unit) {
    Dialog(onDismissRequest = onDismiss) {
        Box(
            Modifier
                .fillMaxWidth()
                .heightIn(max = 560.dp)
                .clip(RoundedCornerShape(22.dp))
                .background(Color(0xFF10131B).copy(alpha = 0.96f))
                .border(1.2.dp, Color(0xFF64D2FF).copy(alpha = 0.35f), RoundedCornerShape(22.dp))
                .padding(20.dp)
        ) {
            Column(
                Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text(
                    title,
                    color = Color.White,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
                Column(
                    Modifier
                        .weight(1f, fill = false)
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                ) {
                    Text(
                        body,
                        color = Color.White.copy(alpha = 0.85f),
                        fontSize = 13.5.sp,
                        lineHeight = 20.sp
                    )
                }
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    Box(
                        Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFF64D2FF).copy(alpha = 0.18f))
                            .border(1.dp, Color(0xFF64D2FF).copy(alpha = 0.6f), RoundedCornerShape(12.dp))
                            .rayClickable(onClick = onDismiss)
                            .padding(horizontal = 24.dp, vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            close,
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AboutHeader(
    title: String,
    hint: String?,
    onBack: () -> Unit,
    focusRequester: FocusRequester? = null
) {
    val g = LocalGlass.current
    Column(Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            if (!LocalMobileSettingsChrome.current) {
                GlassButton("←", focusRequester = focusRequester) { onBack() }
                Text(title, color = g.text, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.ExtraBold, modifier = Modifier.weight(1f))
            }
        }
        if (!hint.isNullOrBlank()) {
            Spacer(Modifier.height(8.dp))
            Text(hint, color = g.muted, style = MaterialTheme.typography.bodySmall)
        }
        Spacer(Modifier.height(8.dp))
    }
}

@Composable
private fun AboutTile(
    icon: ImageVector,
    title: String,
    subtitle: String,
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    if (LocalMobileSettingsChrome.current) {
        MobileOptionTile(
            icon = icon,
            title = title,
            subtitle = subtitle,
            onClick = onClick,
            enabled = enabled
        )
        return
    }
    val g = LocalGlass.current
    var focused by remember { mutableStateOf(false) }
    GlassPanel(
        focused = focused,
        radius = 18.dp,
        onClick = if (enabled) onClick else null,
        modifier = Modifier.fillMaxWidth().onFocusChanged { focused = it.isFocused }
    ) {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(g.accent.copy(alpha = 0.18f))
                    .border(1.dp, g.accent.copy(alpha = 0.40f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(22.dp))
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(title, color = g.text, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.ExtraBold)
                Spacer(Modifier.height(2.dp))
                Text(subtitle, color = g.muted, style = MaterialTheme.typography.bodySmall, maxLines = 4, overflow = TextOverflow.Ellipsis)
            }
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, tint = g.muted)
        }
    }
}

private sealed class UpdateResult {
    data class Available(val store: String) : UpdateResult()
    data object Latest : UpdateResult()
    data object Failed : UpdateResult()
}

private data class FaqEntry(val key: String, val question: String, val answer: String)

private const val TELEGRAM = "https://t.me/minaiptvplayerpro"
private const val WHATSAPP = "https://wa.me/905446450607"
private const val ADMIN_EMAIL = "furkangumrukcu@outlook.com"
private const val ISSUE_EMAIL = "furkangumrukcu07@gmail.com"
private const val WINDOWS_EXE = "https://www.mediafire.com/file/lwp98iwwvrez5bj/Mina_IPTV_Player_Setup.exe/file"
private const val MACOS_DMG = "https://www.mediafire.com/file/3rwikide6hk7iat/Mina_IPTV_Player.dmg/file"
private const val MINATEK = "https://minatek.com.tr"
private const val WALLPAPER = "https://play.google.com/store/apps/details?id=com.mina.wallpaper.hd"

private fun openUrl(ctx: android.content.Context, vm: RayViewModel, url: String, tr: Boolean) {
    runCatching {
        ctx.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }.onFailure {
        vm.toast.value = if (tr) "Bağlantı açılamadı" else "Could not open the link"
    }
}

private suspend fun fetchPlayStoreVersion(packageName: String): String? = withContext(Dispatchers.IO) {
    runCatching {
        val url = URL("https://play.google.com/store/apps/details?id=$packageName&hl=en")
        val conn = (url.openConnection() as HttpURLConnection).apply {
            connectTimeout = 10_000
            readTimeout = 10_000
            instanceFollowRedirects = true
            setRequestProperty(
                "User-Agent",
                "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0 Safari/537.36"
            )
        }
        try {
            val body = conn.inputStream.bufferedReader().use { it.readText() }
            Regex("""\[\[\["(\d+(?:\.\d+)+)"\]\]""").find(body)?.groupValues?.get(1)
        } finally {
            conn.disconnect()
        }
    }.getOrNull()
}

private fun isStoreNewer(current: String, store: String): Boolean {
    val c = current.substringBefore('-').split('.').map { it.trim().toIntOrNull() ?: 0 }
    val s = store.split('.').map { it.trim().toIntOrNull() ?: 0 }
    val len = maxOf(c.size, s.size)
    for (i in 0 until len) {
        val cv = c.getOrElse(i) { 0 }
        val sv = s.getOrElse(i) { 0 }
        if (sv > cv) return true
        if (sv < cv) return false
    }
    return false
}

private const val rayChangelogTr = """v1.3.17
• Tam Cihaz & Yatay Mod (Landscape) Uyumluluğu: Açılış ekranları (Splash) ve seçim pencereleri Mobil (Dikey/Yatay), Tablet ve TV ekran çözünürlüklerine göre dinamik boyutlandırma ve akıcı yerleşim kazandı
• Açılış Ekranı (Splash Screen) Temaları: Ayarlar > Diğer Araçlar altına Doğal Yeşil Cam, Siber Sinema ve Obsidyen Aurora açılış ekranı seçim menüsü eklendi
• Görsel Önizlemeli Splash Seçici: Kullanıcılar açılış ekranlarını canlı küçük resim önizlemeleri ve açıklamalarıyla seçebilir
• Sıfır Gecikmeli & Siyah Ekransız Akıcı Açılış: Splash ekranı doğrudan katman olarak işlenerek açılış ve kapanışta oluşan siyah ekran geçişleri tamamen ortadan kaldırıldı; ana ekrana ipeksi yumuşak geçiş sağlandı

v1.3.16
• Modern Açılış Ekranı (Splash Screen) entegrasyonu ve önizlemeli tema seçici desteği

v1.3.15
• Modern Dark Glass Pop-up Standardı: Tüm popup, diyalog, spotlight arama ve onay pencereleri ana temadan bağımsız olarak Mac / Apple TV Koyu Cam (Dark Glass) tasarımına sabitlendi
• Kapsamlı Çoklu Dil Desteği: Bildirimler, Toast mesajları ve PIN pencereleri 34 dilin tamamına çevrilerek lokalize edildi

v1.3.14
• Oynatıcı Kararlılığı & Kopma Önleme: ExoPlayer HLS canlı yayın yapılandırması, keyframe video birleştirme toleransı (5000ms) ve canlı geri tampon (10s) güçlendirildi
• MediaKit (libmpv) İyileştirmesi: VOD akışlarında yavaş internet tamponlama gecikmeleri akıcı hale getirildi
• TV Tekrar İzle Ekranı: Pop-up yerine modern tam ekran şık arayüz entegrasyonu tamamlandı
• Tablet Ekranı Temizliği: Eski arayüz kalıntıları temizlendi, performans optimize edildi

v1.3.13
• TV Kumanda & Navigasyon: İlk açılışta ve sayfa geçişlerinde asenkron kategori yükleme sırasında odağın ana rail'e kaçma sorunu tüm ekranlarda (Canlı TV, Film, Dizi, İzlemeye Devam Et, Listeler, Ayarlar) tamamen çözüldü
• TV Hiyerarşik Odak Akışı: Kategori seçiminde OK / Sağ tuşla içeriğe geçiş, Sol / Geri tuşuyla sırasıyla kategoriye ve ana rail'e kusursuz geri dönüş sağlandı
• Modern Glass Spotlight Arama: Mobil ve tablet için eski tam ekran arama yerine kompakt, şık ve yarı saydam Glass Spotlight pop-up arama motoru entegre edildi
• Tablet Modu Performans & Tasarım: Ana ekran animasyonları ve LaunchedEffect anahtarları optimize edilerek takılmalar giderildi; Hero banner metin/buton taşmaları düzeltildi
• Mobil & Dock Bar Tasarımı: Mobil dock bar üzerinde akıcı yürüyen neon ışık animasyonu eklendi, cam tasarım dili güçlendirildi
• Bulut Yedekleme & Hesap: Google Drive / Firebase bulut hesap yedekleme ve yönetici algılama kararlılığı artırıldı

v1.3.0
• TV / TV box: büyük M3U ve Xtream listelerinde bellek taşması ve ana iş parçacığı kilitlenmesi azaltıldı
• Canlı izlerken ikinci önizleme oynatıcısı zayıf kutularda kapatıldı
• Yayın türü Otomatik: belirsiz canlı URL’ler TS olarak açılır; 404’te HLS↔TS yedeği çalışır

v1.2.0
• Mobil vitrin kabuğu: telefon için dokunmatik ana ekran, cyan dock ve marka çubuğu
• Canlı TV dikey izleme: kategori/kanal listesi, catch-up aynı ekranda
• Film & Dizi: kategori şeritleri, son eklenenler, yüksek puan, trend, karışık, favoriler, hepsini gör ızgarası
• İçerik detayı: fragman, sezon çipleri, bölüm indirme, izleme çubuğu
• Dikey VOD/dizi oynatıcı: 16:9 video + OSD; filmde kategoriler ve anında oynatma; dizide bilgi/bölümler, jenerik atla, sonraki bölüm
• EPG Mix (Tekrar & EPG): tekrar, spor, belgesel, film, dizi, haber
• Ana ekran şeritleri: izlemeye devam, Ray AI, EPG, maçlar, trend/favori/karışık film-dizi, son 50 dizi, Karışık Canlı TV
• Mobil ayarlar: Mina ile aynı hub ve alt sayfa kartları (çerçeve, daire ikon, ok/anahtar, üst çubuk saat+logo)
• Adaptif titreşim: mobil kaydırma ve dokunmada hafif haptic
• Performans sayfası: Cihaz / Önbellek / Bakım bölümleri
• Diğer araçlar: mobilde titreşim anahtarı; düşük donanım TV’de kalır
• Playlist ve yedekleme satırları aynı çerçeveli kart diline alındı

v1.1.0
• Hakkında: Mina ile aynı alt menü — sürüm notları, güncelleme denetle, bize ulaşın, yönetici, gizlilik, diğer uygulamalar, kurulum sihirbazı
• EPG: Xtream get_all_live_epg + xmltv.php ve EPGShare01 yedek rehber (kanal adına göre)
• Ayarlar: Mina TV ile aynı seçenek ağacı — sıfırlama menüsü, kumanda tuş atama, rail listeler sekmesi, OSD
• İndirilenler: çevrimdışı oynat ve sil
• Yerel JSON yedekleme (bulut hesabı yok)

v1.0.0
• Canlı TV, film, dizi, rehber, izlemeye devam
• Better / MediaKit oynatıcı motorları, altyazı ve ebeveyn PIN"""

private const val rayChangelogEn = """v1.3.17
• Full Device & Landscape Orientation Support: Splash screens and theme picker modal dynamically adapt to Mobile (Portrait/Landscape), Tablet, and TV screens with responsive scaling
• Splash Screen Theme Customizer: Added choice between Natural Green Glass, Cyber Cinema, and Obsidian Aurora splash themes under Settings > Other Tools
• Visual Preview Picker: Users can easily select startup themes with live thumbnail previews and design descriptions
• Zero-Flash Seamless Transition: Splash screen is now rendered as a non-destructive layer overlay, completely eliminating black screen flashes during startup and fading smoothly into the main screen

v1.3.16
• Modern Splash Screen integration and visual preview theme customizer

v1.3.15
• Modern Dark Glass Pop-up Standards: Standardized all popups, dialogs, spotlight search, and confirmation modals to Apple TV / Mac Dark Glass aesthetic across all themes
• Complete 34-Language Localization: Injected full native translations across all 34 supported languages for toast alerts, PIN overlays, and cloud restore previews

v1.3.14
• Playback Stability & Anti-Drop: Enhanced ExoPlayer HLS live configuration, keyframe joining tolerance (5000ms), and live back-buffer (10s)
• MediaKit (libmpv) Optimization: Smoothed VOD cache-pause buffering on low-bandwidth networks
• TV Catchup Experience: Transitioned from pop-up to a sleek dedicated full-screen layout
• Tablet Code Polish: Cleaned legacy overlay remnants and boosted overall responsiveness

v1.3.13
• TV Remote & Navigation: Fixed initial startup and async category loading focus escaping to main rail across all sections (Live TV, Movies, Series, Continue Watching, Playlists, Settings)
• TV Hierarchical Focus Flow: Smooth OK / Right navigation into content, and hierarchical Left / Back return to category and then main rail
• Modern Glass Spotlight Search: Replaced full-screen search on mobile & tablet with a sleek, compact, translucent Glass Spotlight search modal
• Tablet Optimization & UI Polish: Optimized home screen animations and LaunchedEffect keys to eliminate stutters; fixed Hero banner text/pill overflows
• Mobile & Dock Bar Aesthetics: Smooth walking neon glow along the mobile dock bar, enhanced glassmorphism look & feel
• Cloud Backup & Account Stability: Enhanced Google Drive / Firebase cloud backup detection and admin mode reliability

v1.3.0
• TV / TV boxes: lower memory use and less main-thread work when loading large M3U and Xtream lists
• Live preview player is skipped on weak boxes while watching
• Automatic stream type: ambiguous live URLs play as TS; 404 triggers HLS↔TS fallback

v1.2.0
• Mobile showcase shell: touch home, cyan dock and brand chip
• Portrait live watch: categories/channels and catch-up on the same screen
• Movies & Series: category strips, latest, top rated, trending, mixed, favorites, see-all grid
• Detail: trailer, season chips, episode download, progress bar
• Portrait VOD/series player: 16:9 video + OSD; movies play from categories/items; series info/episodes, skip intro, next episode
• EPG Mix (Replay & EPG): replay, sport, documentary, film, series, news
• Home strips: continue watching, Ray AI, EPG, matches, trending/favorite/mixed VOD, last 50 series, Mixed Live TV
• Mobile settings: Mina-matched hub and subpage tiles (frame, circle icon, chevron/switch, clock+logo top bar)
• Adaptive haptics on mobile scroll and tap
• Performance page: Device / Cache / Maintenance
• Other tools: haptics on mobile; low-end mode stays on TV
• Playlist and backup rows use the same framed cards

v1.1.0
• About: same submenu as Mina — release notes, check for updates, contact us, administrator, privacy, other apps, setup wizard
• EPG: Xtream get_all_live_epg + xmltv.php and EPGShare01 backup guide (by channel name)
• Settings aligned with Mina TV: reset menu, remote keys, rail playlists tab, OSD
• Downloads: play offline and delete
• Local JSON backup (no cloud account)

v1.0.0
• Live TV, movies, series, guide, continue watching
• Better / MediaKit engines, subtitles and parental PIN"""

private val privacyTr = listOf(
    "" to "Son Güncelleme: 20 Temmuz 2026\n\nBu Gizlilik Politikası, Ray IPTV Player uygulamasını kullandığınızda verilerinizin nasıl işlendiğini açıklar. Uygulamayı kullanarak bu uygulamaları kabul etmiş olursunuz.",
    "1. İçerik ve Telif Hakkı Bildirimi" to "Ray IPTV Player kendi başına HİÇBİR medya içeriği (canlı yayın, film, dizi vb.) SAĞLAMAZ. Uygulama boş bir medya oynatıcıdır ve yalnızca sizin eklediğiniz veya yasal olarak sahip olduğunuz M3U/Xtream listelerini çalıştırır. Telif hakkı ihlali içeren içeriğin oynatılmasından tamamen kullanıcı sorumludur.",
    "2. Toplanan Veriler ve Kullanım Amacı" to "Zorunlu bir bulut hesabı, analitik veya çökme raporu gönderimi yoktur.\n\n• Ayarlar, izleme ilerlemesi, favoriler ve yedekler cihazda tutulur.\n• EPG/XMLTV veya poster indirirken yalnızca sizin eklediğiniz sunuculara bağlanırız.\n• Güncelleme denetimi Play Store sayfasını okuyabilir.",
    "3. Veri Güvenliği" to "Liste kullanıcı adı ve şifreleri cihazdaki uygulama veritabanında saklanır. Yerel JSON yedek, sizin dışa aktardığınız dosyada durur; biz sunucuya yüklemeyiz.",
    "4. Veri Silme" to "Ayarlar > Uygulama Bilgileri > Hesabımı Sil veya tüm ayarları sil ile cihazdaki uygulama verilerini temizleyebilirsiniz. Bulutta zorunlu bir hesap tutulmaz.",
    "5. Üçüncü Taraflarla Paylaşım" to "Kişisel verileriniz reklamverenlerle satılmaz. Play Store, e-posta, Telegram/WhatsApp veya harici oynatıcı açıldığında ilgili sistem uygulaması devreye girer.",
    "6. İletişim" to "Sorularınız için Ayarlar > Hakkında > Bize Ulaşın bölümünü kullanın."
)

private val privacyEn = listOf(
    "" to "Last updated: July 20, 2026\n\nThis Privacy Policy explains how Ray IPTV Player handles your data. By using the app you accept these practices.",
    "1. Content and Copyright Notice" to "Ray IPTV Player DOES NOT PROVIDE any media catalog. It is an empty player for M3U/Xtream lists you add or legally own. You are responsible for any copyrighted or illegal playback.",
    "2. Collected Data" to "There is no required cloud account, analytics, or crash-reporting pipeline.\n\n• Settings, progress, favorites and backups stay on device.\n• EPG/XMLTV and posters are fetched only from servers you configure.\n• Update check may read the Play Store listing.",
    "3. Data Security" to "Playlist credentials stay in the on-device database. Local JSON backups are files you export; we do not upload them.",
    "4. Data Deletion" to "Use Settings > App info > Delete my account or erase all settings to clear local app data. There is no mandatory cloud account.",
    "5. Third parties" to "Personal data is not sold to advertisers. Opening Play Store, email, Telegram/WhatsApp or an external player uses those system apps.",
    "6. Contact" to "Use Settings > About > Contact Us."
)

private fun faqEntries(tr: Boolean): List<FaqEntry> = if (tr) listOf(
    FaqEntry("ts", "MPEG-TS moduna ne zaman geçmeliyim?", "MPEG-TS yayını tek parça akış olarak çeker; açılış hızlıdır ve birçok TV box ile uyumludur. Kanallar geç açılıyorsa veya yayın başlamıyorsa MPEG-TS deneyin."),
    FaqEntry("hls", "HLS (m3u8) moduna ne zaman geçmeliyim?", "HLS yayını küçük parçalara böler. İnternetiniz dalgalıysa HLS, MPEG-TS’e göre daha az donabilir. Çoklu kalite istiyorsanız HLS kullanın."),
    FaqEntry("auto", "Uygulama neden cihazımda otomatik MPEG-TS’e geçti?", "TV box veya düşük donanım algılanınca canlı biçim bir kez MPEG-TS’e ayarlanabilir. Ayarlar > Oynatma’dan HLS’e dönebilirsiniz."),
    FaqEntry("buf", "Tampon süresini neden ayarlamalıyım?", "Düşük tampon kanal geçişini hızlandırır ama donma riskini artırır. Sık donuyorsa değeri yükseltin."),
    FaqEntry("freeze", "Bir liste sürekli donuyor, başka liste sorunsuz. Neden?", "Çoğu zaman sağlayıcı sunucusudur. Aynı cihazla başka liste sorunsuzsa sorun listenin altyapısındadır."),
    FaqEntry("stop", "Canlı yayın aniden durursa ne oluyor?", "Oynatıcı kopunca yeniden bağlanmayı dener; gerekirse biçim veya motor değişimi de denenir."),
    FaqEntry("eng", "Better ve MediaKit arasındaki fark nedir?", "Better (ExoPlayer) çoğu cihazda verimlidir. MediaKit (libmpv) zorlu codec’lerde daha esnektir. Ayarlardan motor seçebilirsiniz."),
    FaqEntry("sw", "Yazılımsal kod çözücü ne zaman kullanılmalı?", "Yeşil/mor ekran veya donanım hatalarında yazılımsal çözücü daha uyumludur ama işlemciyi yorar."),
    FaqEntry("low", "Düşük donanım modu ne yapar?", "Ağır görsel efektleri kısar; zayıf cihazlarda arayüz daha akıcı olur."),
    FaqEntry("ua", "User Agent ayarını ne zaman değiştirmeliyim?", "Bazı paneller yalnızca belirli bir User-Agent ile yayın verir. Emin değilseniz varsayılanı kullanın."),
    FaqEntry("epg", "Program rehberi (EPG) nasıl çalışır?", "Önce Xtream/XMLTV birincil rehber, boşsa EPGShare01 yedek kanal adına göre doldurulur. Ayarlar > EPG’den kaynak ve yenilemeyi kontrol edin."),
    FaqEntry("ssl", "SSL sertifikasını yoksay ne işe yarar?", "Kendinden imzalı sertifikalı panellerde bağlantı hatasını aşmak için kullanılır."),
    FaqEntry("multi", "Birden fazla liste ekleyebilir miyim?", "Evet, en fazla 32 liste. Rail’deki Listeler sekmesinden geçiş yaparsınız."),
    FaqEntry("bak", "Ayarlarım yedeklenir mi?", "Yerel JSON yedekleme vardır (bulut hesabı zorunlu değildir). Ayarlar > Yedekleme’den dışa/içe aktarın."),
    FaqEntry("cu", "Geçmiş yayınları (catch-up) nasıl izlerim?", "Sağlayıcı timeshift destekliyorsa EPG’deki geçmiş programdan açılır. Simge yoksa arşiv sunulmuyordur."),
    FaqEntry("res", "Film/diziler kaldığım yerden devam ediyor mu?", "Evet. İzlemeye devam ve sonraki bölüm otomatik oynatma Ayarlar’dan yönetilir."),
    FaqEntry("sub", "Altyazıları nasıl açarım?", "Oynatıcı OSD’sinden altyazı seçin. Boyut, renk ve kenarlık oynatma ayarlarındadır."),
    FaqEntry("ext", "Yayını VLC / MX Player’da açabilir miyim?", "Harici oynatıcıyı açarsanız içerik seçtiğiniz uygulamada oynar. OSD ve kaldığın yer harici oynatıcıda yoktur."),
    FaqEntry("spd", "Oynatma hızını değiştirebilir miyim?", "Film ve dizilerde evet. Canlı yayınlarda hız kilidi vardır."),
    FaqEntry("zoom", "Görüntü sığmıyor / siyah çubuk var.", "Oynatıcıda sığdır / doldur / kapla / uzat modlarını deneyin."),
    FaqEntry("num", "Kanal numarasıyla geçebilir miyim?", "Kumandadan numarayı yazarak doğrudan kanala gidebilirsiniz."),
    FaqEntry("fav", "Favoriler nerede?", "Kanal veya içerikte favori ekleyin; ana ekran ve rail’den ulaşılır."),
    FaqEntry("cw", "İzlemeye devam nedir?", "Yarıda kalan film/dizi ve son canlı kanallar ana ekranda listelenir."),
    FaqEntry("dl", "İndirme nasıl çalışır?", "Desteklenen bölümleri indirip çevrimdışı oynatabilir veya silebilirsiniz."),
    FaqEntry("home", "Ana ekranı nasıl özelleştiririm?", "Ayarlar > Ana Ekran’dan şeritleri açıp kapatın."),
    FaqEntry("theme", "Temayı nereden değiştiririm?", "Ayarlar’daki tema seçici veya kurulum sihirbazı."),
    FaqEntry("pro", "Profiller ne işe yarar?", "Her kullanıcı için ayrı PIN, favori ve ilerleme tutulabilir."),
    FaqEntry("pin", "Ebeveyn PIN nasıl çalışır?", "Kilitli kategoriler ve +18 gizleme için PIN istenir. Hakkında’daki sihirbazla da kurulur."),
    FaqEntry("sleep", "Uyku zamanlayıcı nerede?", "Ayarlar > Diğer araçlar."),
    FaqEntry("lay", "Kanal sırasını nasıl değiştiririm?", "Ayarlar > Kanal Kategori Düzeni."),
    FaqEntry("epgs", "EPG ayarları nerede?", "Ayarlar > Diğer araçlar > EPG."),
    FaqEntry("spd2", "Hız testi nerede?", "Ayarlar > Diğer araçlar."),
    FaqEntry("lang", "Dili nasıl değiştiririm?", "Ayarlar’daki dil seçici.")
) else listOf(
    FaqEntry("ts", "When should I switch to MPEG-TS?", "MPEG-TS is a single continuous stream; it often opens faster on TV boxes. Try it if channels start slowly."),
    FaqEntry("hls", "When should I use HLS?", "HLS splits the stream into segments and can be more stable on shaky networks."),
    FaqEntry("auto", "Why did the app switch to MPEG-TS?", "TV-box or low-end devices may get MPEG-TS once. You can switch back in Playback settings."),
    FaqEntry("buf", "Why change the buffer?", "Lower buffer zaps faster but may stutter. Raise it if you freeze often."),
    FaqEntry("freeze", "One list freezes, another is fine.", "Usually the provider. If another list works on the same device, the server is the issue."),
    FaqEntry("stop", "Live playback stops suddenly.", "The player retries reconnect and may switch format or engine."),
    FaqEntry("eng", "Better vs MediaKit?", "Better (ExoPlayer) is efficient. MediaKit (libmpv) is more flexible for tough codecs."),
    FaqEntry("sw", "When to use software decoder?", "When hardware decode shows green/purple frames. It uses more CPU."),
    FaqEntry("low", "What does low-end mode do?", "It reduces heavy visuals so the UI stays smoother on weak devices."),
    FaqEntry("ua", "When to change User-Agent?", "Some panels require a specific UA. Keep default if unsure."),
    FaqEntry("epg", "How does EPG work?", "Primary Xtream/XMLTV first; if empty, EPGShare01 backup matches by channel name."),
    FaqEntry("ssl", "What does ignore SSL do?", "It allows panels with self-signed certificates."),
    FaqEntry("multi", "Can I add multiple lists?", "Yes, up to 32. Switch them from the Playlists rail tab."),
    FaqEntry("bak", "Are settings backed up?", "Local JSON backup (no required cloud account) under Settings > Backup."),
    FaqEntry("cu", "How do I watch catch-up?", "If the panel supports timeshift, open a past EPG programme. No icon means no archive."),
    FaqEntry("res", "Do movies resume?", "Yes. Continue watching and autoplay-next are in settings."),
    FaqEntry("sub", "How do I enable subtitles?", "From the player OSD. Size and color are in playback settings."),
    FaqEntry("ext", "Can I use VLC / MX Player?", "Enable the external player. OSD and resume will not apply there."),
    FaqEntry("spd", "Can I change playback speed?", "For VOD yes. Live stays locked at 1.0x."),
    FaqEntry("zoom", "Black bars / crop.", "Cycle fit / fill / cover / stretch in the player."),
    FaqEntry("num", "Can I type a channel number?", "Yes, from the remote number keys."),
    FaqEntry("fav", "Where are favorites?", "Heart an item; they appear on Home and the rail."),
    FaqEntry("cw", "What is continue watching?", "Unfinished VOD and recent live channels on Home."),
    FaqEntry("dl", "How do downloads work?", "Download supported episodes and play or delete them offline."),
    FaqEntry("home", "Customize Home?", "Settings > Home: toggle rows."),
    FaqEntry("theme", "Change theme?", "Theme picker in Settings or the setup wizard."),
    FaqEntry("pro", "What are profiles?", "Separate PIN, favorites and progress per user."),
    FaqEntry("pin", "Parental PIN?", "Asked for locked categories and adult hide. Also in the setup wizard."),
    FaqEntry("sleep", "Sleep timer?", "Settings > Other tools."),
    FaqEntry("lay", "Reorder channels?", "Settings > Channel layout."),
    FaqEntry("epgs", "EPG settings?", "Settings > Other tools > EPG."),
    FaqEntry("spd2", "Speed test?", "Settings > Other tools."),
    FaqEntry("lang", "Change language?", "Language picker in Settings.")
)
