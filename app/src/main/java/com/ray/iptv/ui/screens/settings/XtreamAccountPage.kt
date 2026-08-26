package com.ray.iptv.ui.screens.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AllInclusive
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.Cast
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Devices
import androidx.compose.material.icons.filled.Dns
import androidx.compose.material.icons.filled.EventAvailable
import androidx.compose.material.icons.filled.EventBusy
import androidx.compose.material.icons.filled.HighQuality
import androidx.compose.material.icons.filled.HourglassBottom
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Science
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.SettingsEthernet
import androidx.compose.material.icons.filled.Tag
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Icon
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.ray.iptv.data.local.SourceEntity
import com.ray.iptv.data.remote.XtreamAccountSnapshot
import com.ray.iptv.data.repo.RaySettings
import com.ray.iptv.ui.RayViewModel
import com.ray.iptv.ui.components.GlassButton
import com.ray.iptv.ui.glass.GlassPanel
import com.ray.iptv.ui.theme.LocalGlass
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

private val OkGreen = Color(0xFF6EE7B7)
private val WarnYellow = Color(0xFFFCD34D)
private val BadRed = Color(0xFFFCA5A5)

@Composable
internal fun AccountPage(
    vm: RayViewModel,
    sources: List<SourceEntity>,
    settings: RaySettings,
    tr: Boolean
) {
    val src = sources.firstOrNull { it.id == settings.activeSourceId } ?: sources.firstOrNull()
    if (src == null) {
        Text(if (tr) "Kayıtlı kaynak yok." else "No source yet.", color = LocalGlass.current.muted)
        return
    }
    if (!src.kind.equals("XTREAM", true)) {
        GlassPanel(strong = true, radius = 16.dp) {
            Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    if (tr) "Bu özellik sadece Xtream hesapları içindir." else "This feature is only for Xtream accounts.",
                    color = LocalGlass.current.text,
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    "${src.name}  ·  ${src.kind}",
                    color = LocalGlass.current.muted,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
        return
    }

    var snap by remember(src.id) { mutableStateOf<XtreamAccountSnapshot?>(null) }
    var loading by remember(src.id) { mutableStateOf(true) }
    var failed by remember(src.id) { mutableStateOf(false) }
    var reload by remember(src.id) { mutableIntStateOf(0) }

    LaunchedEffect(src.id, reload) {
        loading = true
        failed = false
        val got = runCatching { vm.fetchXtreamAccount(src) }.getOrNull()
        snap = got
        failed = got == null
        loading = false
    }

    Column(
        Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        when {
            loading -> Text(if (tr) "Yükleniyor…" else "Loading…", color = LocalGlass.current.muted)
            failed -> {
                Text(
                    if (tr) "Hesap bilgileri sunucudan alınamadı." else "Could not load account info from the server.",
                    color = LocalGlass.current.muted
                )
                GlassButton(if (tr) "Tekrar dene" else "Retry") { reload++ }
            }
            snap != null -> XtreamAccountBody(
                source = src,
                snap = snap!!,
                tr = tr,
                onCopied = { vm.toast.value = it }
            )
        }
    }
}

@Composable
private fun XtreamAccountBody(
    source: SourceEntity,
    snap: XtreamAccountSnapshot,
    tr: Boolean,
    onCopied: (String) -> Unit
) {
    val g = LocalGlass.current
    val user = snap.user
    val server = snap.server
    var passwordVisible by remember { mutableStateOf(false) }
    val username = user?.username?.takeIf { it.isNotBlank() } ?: source.username
    val remaining = remainingDaysLabel(user?.expiryEpochSec, tr)
    val remainingColor = remainingColor(user?.expiryEpochSec)
    val statusText = when {
        user == null -> if (tr) "Bilinmiyor" else "Unknown"
        user.status.isBlank() -> if (tr) "Bilinmiyor" else "Unknown"
        else -> user.status.uppercase(Locale.getDefault())
    }

    HeroHeader(
        username = username,
        statusText = statusText,
        statusColor = statusColor(user?.status.orEmpty()),
        subtitle = if (user?.expiryEpochSec == null) {
            if (tr) "Süresiz" else "Unlimited"
        } else remaining,
        subtitleColor = remainingColor
    )

    if (user != null) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            if (remaining != null) {
                StatChip(
                    modifier = Modifier.weight(1f),
                    icon = Icons.Filled.HourglassBottom,
                    label = if (tr) "Kalan" else "Remaining",
                    value = remaining,
                    color = remainingColor
                )
            } else {
                StatChip(
                    modifier = Modifier.weight(1f),
                    icon = Icons.Filled.AllInclusive,
                    label = if (tr) "Bitiş Tarihi" else "Expires",
                    value = if (tr) "Süresiz" else "Unlimited",
                    color = OkGreen
                )
            }
            StatChip(
                modifier = Modifier.weight(1f),
                icon = Icons.Filled.Devices,
                label = if (tr) "Aktif / Maks. Bağlantı" else "Active / Max connections",
                value = "${user.activeCons}/${user.maxCons}"
            )
            StatChip(
                modifier = Modifier.weight(1f),
                icon = if (user.isTrial) Icons.Filled.Science else Icons.Filled.WorkspacePremium,
                label = if (tr) "Deneme Hesabı" else "Trial account",
                value = if (user.isTrial) {
                    if (tr) "Evet" else "Yes"
                } else {
                    if (tr) "Hayır" else "No"
                },
                color = if (user.isTrial) WarnYellow else OkGreen
            )
        }
    }

    GroupCard(
        icon = Icons.Filled.WorkspacePremium,
        title = if (tr) "ABONELİK" else "SUBSCRIPTION"
    ) {
        if (user != null) {
            DetailRow(
                icon = Icons.Filled.Person,
                label = if (tr) "Kullanıcı" else "Username",
                value = username,
                copyable = true,
                tr = tr,
                onCopied = onCopied
            )
            PasswordRow(
                label = if (tr) "Şifre" else "Password",
                value = user.password.ifBlank { source.password },
                visible = passwordVisible,
                onToggle = { passwordVisible = !passwordVisible },
                tr = tr,
                onCopied = onCopied
            )
            DetailRow(
                icon = Icons.Filled.VerifiedUser,
                label = if (tr) "Doğrulama" else "Auth",
                value = when (user.auth) {
                    1 -> if (tr) "Başarılı" else "Success"
                    0 -> if (tr) "Başarısız" else "Failed"
                    else -> if (tr) "Bilinmiyor" else "Unknown"
                },
                valueColor = when (user.auth) {
                    1 -> OkGreen
                    0 -> BadRed
                    else -> null
                },
                tr = tr,
                onCopied = onCopied
            )
            if (user.message.isNotBlank()) {
                DetailRow(
                    icon = Icons.Filled.Campaign,
                    label = if (tr) "Mesaj" else "Message",
                    value = user.message,
                    tr = tr,
                    onCopied = onCopied
                )
            }
            if (user.createdEpochSec != null) {
                DetailRow(
                    icon = Icons.Filled.EventAvailable,
                    label = if (tr) "Oluşturuldu" else "Created",
                    value = fmtDate(user.createdEpochSec),
                    tr = tr,
                    onCopied = onCopied
                )
            }
            DetailRow(
                icon = Icons.Filled.EventBusy,
                label = if (tr) "Bitiş Tarihi" else "Expires",
                value = if (user.expiryEpochSec == null) {
                    if (tr) "Süresiz" else "Unlimited"
                } else fmtDate(user.expiryEpochSec),
                tr = tr,
                onCopied = onCopied
            )
            if (user.allowedOutputs.isNotEmpty()) {
                DetailRow(
                    icon = Icons.Filled.HighQuality,
                    label = if (tr) "İzinli Formatlar" else "Allowed formats",
                    value = user.allowedOutputs.joinToString(", ").uppercase(Locale.getDefault()),
                    tr = tr,
                    onCopied = onCopied
                )
            }
        } else {
            DetailRow(
                icon = Icons.Filled.Info,
                label = if (tr) "Kullanıcı bilgisi alınamadı" else "User info unavailable",
                value = "—",
                tr = tr,
                onCopied = onCopied
            )
        }
    }

    GroupCard(
        icon = Icons.Filled.Dns,
        title = if (tr) "SUNUCU" else "SERVER"
    ) {
        DetailRow(
            icon = Icons.Filled.Link,
            label = if (tr) "Bağlantı URL" else "Connection URL",
            value = source.baseUrl,
            copyable = true,
            tr = tr,
            onCopied = onCopied
        )
        if (server != null && !server.isEmpty) {
            if (server.url.isNotBlank()) {
                DetailRow(
                    icon = Icons.Filled.Dns,
                    label = if (tr) "Sunucu" else "Host",
                    value = server.url,
                    copyable = true,
                    tr = tr,
                    onCopied = onCopied
                )
            }
            if (server.protocol.isNotBlank()) {
                DetailRow(
                    icon = Icons.Filled.Security,
                    label = if (tr) "Protokol" else "Protocol",
                    value = server.protocol.uppercase(Locale.getDefault()),
                    tr = tr,
                    onCopied = onCopied
                )
            }
            if (server.port.isNotBlank()) {
                DetailRow(
                    icon = Icons.Filled.SettingsEthernet,
                    label = if (tr) "HTTP Port" else "HTTP port",
                    value = server.port,
                    tr = tr,
                    onCopied = onCopied
                )
            }
            if (server.httpsPort.isNotBlank()) {
                DetailRow(
                    icon = Icons.Filled.Lock,
                    label = if (tr) "HTTPS Port" else "HTTPS port",
                    value = server.httpsPort,
                    tr = tr,
                    onCopied = onCopied
                )
            }
            if (server.rtmpPort.isNotBlank()) {
                DetailRow(
                    icon = Icons.Filled.Cast,
                    label = if (tr) "RTMP Port" else "RTMP port",
                    value = server.rtmpPort,
                    tr = tr,
                    onCopied = onCopied
                )
            }
            if (server.timezone.isNotBlank()) {
                DetailRow(
                    icon = Icons.Filled.Public,
                    label = if (tr) "Saat Dilimi" else "Timezone",
                    value = server.timezone,
                    tr = tr,
                    onCopied = onCopied
                )
            }
            val timeLabel = server.timeNow.takeIf { it.isNotBlank() }
                ?: server.timestampNow?.let { fmtDateTime(it) }
            if (timeLabel != null) {
                DetailRow(
                    icon = Icons.Filled.Schedule,
                    label = if (tr) "Sunucu Saati" else "Server time",
                    value = timeLabel,
                    tr = tr,
                    onCopied = onCopied
                )
            }
            if (server.process != null) {
                DetailRow(
                    icon = Icons.Filled.Bolt,
                    label = if (tr) "Servis Durumu" else "Service status",
                    value = if (server.process) {
                        if (tr) "Aktif" else "On"
                    } else {
                        if (tr) "Pasif" else "Off"
                    },
                    valueColor = if (server.process) OkGreen else BadRed,
                    tr = tr,
                    onCopied = onCopied
                )
            }
            if (server.revision.isNotBlank()) {
                DetailRow(
                    icon = Icons.Filled.Tag,
                    label = if (tr) "Sürüm" else "Revision",
                    value = server.revision,
                    tr = tr,
                    onCopied = onCopied
                )
            }
        } else {
            DetailRow(
                icon = Icons.Filled.Info,
                label = if (tr) "Sunucu bilgisi alınamadı" else "Server info unavailable",
                value = "—",
                tr = tr,
                onCopied = onCopied
            )
        }
    }

    Text(
        "${source.name}  ·  Xtream",
        color = g.muted,
        style = MaterialTheme.typography.bodySmall
    )
}

@Composable
private fun HeroHeader(
    username: String,
    statusText: String,
    statusColor: Color?,
    subtitle: String?,
    subtitleColor: Color?
) {
    val g = LocalGlass.current
    val initial = username.trim().firstOrNull()?.uppercaseChar()?.toString() ?: "?"
    Box(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(
                Brush.linearGradient(
                    listOf(g.accent.copy(alpha = 0.28f), g.accent.copy(alpha = 0.08f))
                )
            )
            .border(1.dp, g.accent.copy(alpha = 0.35f), RoundedCornerShape(18.dp))
            .padding(14.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier
                    .size(54.dp)
                    .background(
                        Brush.linearGradient(
                            listOf(g.accent.copy(alpha = 0.85f), g.accent.copy(alpha = 0.45f))
                        ),
                        CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    initial,
                    color = Color.White,
                    style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.ExtraBold)
                )
            }
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    username,
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.ExtraBold)
                )
                Spacer(Modifier.height(6.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    StatusPill(statusText, statusColor)
                    if (!subtitle.isNullOrBlank()) {
                        Spacer(Modifier.width(8.dp))
                        Text(
                            subtitle,
                            color = subtitleColor ?: Color.White.copy(alpha = 0.7f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun StatusPill(text: String, color: Color?) {
    val c = color ?: Color.White.copy(alpha = 0.7f)
    Row(
        Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(c.copy(alpha = 0.18f))
            .border(1.dp, c.copy(alpha = 0.55f), RoundedCornerShape(20.dp))
            .padding(horizontal = 10.dp, vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(Modifier.size(7.dp).background(c, CircleShape))
        Spacer(Modifier.width(6.dp))
        Text(
            text,
            color = c,
            fontSize = 11.5.sp,
            fontWeight = FontWeight.ExtraBold
        )
    }
}

@Composable
private fun StatChip(
    modifier: Modifier = Modifier,
    icon: ImageVector,
    label: String,
    value: String,
    color: Color? = null
) {
    val c = color ?: Color.White.copy(alpha = 0.92f)
    Column(
        modifier
            .clip(RoundedCornerShape(14.dp))
            .background(Color.White.copy(alpha = 0.05f))
            .border(1.dp, Color.White.copy(alpha = 0.10f), RoundedCornerShape(14.dp))
            .padding(horizontal = 10.dp, vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(icon, null, tint = c, modifier = Modifier.size(18.dp))
        Spacer(Modifier.height(6.dp))
        Text(
            value,
            color = c,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            fontWeight = FontWeight.ExtraBold,
            fontSize = 13.5.sp
        )
        Spacer(Modifier.height(2.dp))
        Text(
            label,
            color = Color.White.copy(alpha = 0.55f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            fontWeight = FontWeight.SemiBold,
            fontSize = 10.5.sp
        )
    }
}

@Composable
private fun GroupCard(icon: ImageVector, title: String, content: @Composable () -> Unit) {
    val g = LocalGlass.current
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Color.White.copy(alpha = 0.04f))
            .border(1.dp, Color.White.copy(alpha = 0.10f), RoundedCornerShape(16.dp))
    ) {
        Row(
            Modifier.padding(start = 14.dp, top = 12.dp, end = 14.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                Modifier
                    .size(28.dp)
                    .background(g.accent.copy(alpha = 0.20f), CircleShape)
                    .border(1.dp, g.accent.copy(alpha = 0.45f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, null, tint = Color.White, modifier = Modifier.size(15.dp))
            }
            Spacer(Modifier.width(10.dp))
            Text(
                title,
                color = Color.White,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 14.sp
            )
        }
        Box(Modifier.fillMaxWidth().height(1.dp).background(Color.White.copy(alpha = 0.07f)))
        Column(Modifier.padding(start = 14.dp, top = 6.dp, end = 10.dp, bottom = 10.dp)) {
            content()
        }
    }
}

@Composable
private fun DetailRow(
    icon: ImageVector,
    label: String,
    value: String,
    tr: Boolean,
    onCopied: (String) -> Unit,
    valueColor: Color? = null,
    copyable: Boolean = false
) {
    val clip = LocalClipboardManager.current
    Row(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.Top
    ) {
        Icon(icon, null, tint = Color.White.copy(alpha = 0.55f), modifier = Modifier.padding(top = 1.dp).size(16.dp))
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            Text(label, color = Color.White.copy(alpha = 0.55f), fontSize = 11.5.sp, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(2.dp))
            Text(
                value,
                color = valueColor ?: Color.White.copy(alpha = 0.95f),
                fontSize = 13.5.sp,
                fontWeight = FontWeight.Bold
            )
        }
        if (copyable) {
            GlassButton("", compact = true, icon = Icons.Filled.ContentCopy) {
                clip.setText(AnnotatedString(value))
                onCopied(if (tr) "Kopyalandı" else "Copied")
            }
        }
    }
}

@Composable
private fun PasswordRow(
    label: String,
    value: String,
    visible: Boolean,
    onToggle: () -> Unit,
    tr: Boolean,
    onCopied: (String) -> Unit
) {
    val clip = LocalClipboardManager.current
    val masked = "•".repeat(value.length.coerceIn(6, 14))
    Row(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.Top
    ) {
        Icon(Icons.Filled.Lock, null, tint = Color.White.copy(alpha = 0.55f), modifier = Modifier.padding(top = 1.dp).size(16.dp))
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            Text(label, color = Color.White.copy(alpha = 0.55f), fontSize = 11.5.sp, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(2.dp))
            Text(
                if (visible) value else masked,
                color = Color.White.copy(alpha = 0.95f),
                fontSize = 13.5.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = if (visible) FontFamily.Monospace else FontFamily.Default
            )
        }
        GlassButton(
            "",
            compact = true,
            icon = if (visible) Icons.Filled.VisibilityOff else Icons.Filled.Visibility
        ) { onToggle() }
        GlassButton("", compact = true, icon = Icons.Filled.ContentCopy) {
            clip.setText(AnnotatedString(value))
            onCopied(if (tr) "Kopyalandı" else "Copied")
        }
    }
}

private fun remainingDaysLabel(expiryEpochSec: Long?, tr: Boolean): String? {
    if (expiryEpochSec == null) return null
    val diffMs = expiryEpochSec * 1000L - System.currentTimeMillis()
    val days = kotlin.math.abs(diffMs / 86_400_000L).coerceAtMost(99_999)
    return if (diffMs < 0) {
        if (tr) "$days gün önce sona erdi" else "expired $days days ago"
    } else {
        if (tr) "$days gün" else "$days days"
    }
}

private fun remainingColor(expiryEpochSec: Long?): Color? {
    if (expiryEpochSec == null) return null
    val diffMs = expiryEpochSec * 1000L - System.currentTimeMillis()
    if (diffMs < 0) return BadRed
    val days = diffMs / 86_400_000L
    return if (days <= 7) WarnYellow else OkGreen
}

private fun statusColor(status: String): Color? {
    val s = status.lowercase(Locale.getDefault()).trim()
    if (s == "active") return OkGreen
    if (s.contains("expir") || s.contains("disabled") || s.contains("banned")) return BadRed
    return null
}

private fun fmtDate(epochSec: Long): String {
    val fmt = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault())
    return fmt.format(Date(epochSec * 1000L))
}

private fun fmtDateTime(epochSec: Long): String {
    val fmt = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault())
    fmt.timeZone = TimeZone.getDefault()
    return fmt.format(Date(epochSec * 1000L))
}
