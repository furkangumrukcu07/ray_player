package com.ray.iptv.ui.screens.settings

import android.content.Intent
import android.content.res.Configuration
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.PlaylistPlay
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.tv.material3.Icon
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.ray.iptv.data.local.SourceEntity
import com.ray.iptv.data.repo.AppLang
import com.ray.iptv.data.repo.CatalogRepository
import com.ray.iptv.data.repo.RaySettings
import com.ray.iptv.ui.RayViewModel
import com.ray.iptv.ui.components.GlassButton
import com.ray.iptv.ui.glass.GlassPanel
import com.ray.iptv.ui.screens.onboarding.GlassField
import com.ray.iptv.ui.theme.LocalGlass

private enum class PlaylistEditorKind { M3U_URL, M3U_FILE, XTREAM, STALKER }

@Composable
internal fun PlaylistPage(
    vm: RayViewModel,
    settings: RaySettings,
    sources: List<SourceEntity>
) {
    val tr = settings.lang == AppLang.TR
    val g = LocalGlass.current
    val sync by vm.catalog.sync.collectAsState()
    val portrait = LocalConfiguration.current.orientation == Configuration.ORIENTATION_PORTRAIT
    val enabled = sources.filter { it.enabled }
    var editor by remember { mutableStateOf<SourceEntity?>(null) }
    var adding by remember { mutableStateOf(false) }
    var pendingDelete by remember { mutableStateOf<SourceEntity?>(null) }
    var picker by remember { mutableStateOf(false) }
    val mobileChrome = LocalMobileSettingsChrome.current

    if (adding || editor != null) {
        BackHandler { adding = false; editor = null }
        PlaylistEditorPage(
            existing = editor,
            slot = editor?.let { sources.indexOf(it) + 1 }?.coerceAtLeast(1) ?: (sources.size + 1),
            tr = tr,
            vm = vm,
            onClose = { adding = false; editor = null }
        )
        return
    }

    LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        if (!mobileChrome) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    if (tr) "Liste Yönetimi" else "Playlist Manager",
                    color = g.text,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    if (tr) "İstediğin kadar liste ekle" else "Add as many playlists as you want",
                    color = g.muted,
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
        }
        if (enabled.size >= 2) {
            item {
                val current = enabled.firstOrNull { it.id == settings.activeSourceId } ?: enabled.first()
                val label = current.name.ifBlank { slotTitle(sources.indexOf(current) + 1, tr) }
                if (LocalMobileSettingsChrome.current) {
                    MobileOptionTile(
                        icon = Icons.Filled.PlaylistPlay,
                        title = if (tr) "Liste Seç" else "Select List",
                        subtitle = label,
                        onClick = { picker = true }
                    )
                } else {
                GlassPanel(
                    strong = true,
                    radius = 16.dp,
                    onClick = { picker = true },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(
                                if (tr) "Liste Seç" else "Select List",
                                color = g.muted,
                                style = MaterialTheme.typography.labelLarge
                            )
                            Text(
                                label,
                                color = g.text,
                                style = MaterialTheme.typography.titleMedium
                            )
                        }
                        Icon(Icons.Filled.ChevronRight, contentDescription = null, tint = g.muted)
                    }
                }
                }
            }
        }
        if (sync.running && !sync.catalog) {
            item {
                Text(
                    if (tr) "İçerik güncelleniyor…" else "Updating content…",
                    color = g.accent,
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
        itemsIndexed(sources, key = { _, src -> src.id }) { index, src ->
            PlaylistSlotCard(
                src = src,
                slot = index + 1,
                active = src.id == settings.activeSourceId,
                canMoveUp = index > 0,
                canMoveDown = index < sources.lastIndex,
                portrait = portrait,
                tr = tr,
                onToggle = { vm.toggleSourceEnabled(src.id) },
                onRefresh = { vm.refreshSource(src.id) },
                onEdit = { editor = src },
                onDelete = { pendingDelete = src },
                onMoveUp = { vm.moveSource(src.id, -1) },
                onMoveDown = { vm.moveSource(src.id, 1) },
                onActivate = { if (src.enabled) vm.selectSource(src.id) }
            )
        }
        if (sources.size < CatalogRepository.MAX_PLAYLIST_SLOTS) {
            item {
                AddPlaylistCard(slot = sources.size + 1, tr = tr) { adding = true }
            }
        }
        item { Spacer(Modifier.height(8.dp)) }
    }

    pendingDelete?.let { src ->
        val n = sources.indexOf(src) + 1
        GlassConfirmDialog(
            title = if (tr) "Listeyi sil" else "Delete list",
            body = if (tr) "$n. listeyi kaldırmak istediğinize emin misiniz?" else "Remove list #$n?",
            confirm = if (tr) "Sil" else "Delete",
            cancel = if (tr) "Vazgeç" else "Cancel",
            onDismiss = { pendingDelete = null },
            onConfirm = {
                vm.removeSource(src.id)
                pendingDelete = null
            }
        )
    }
    if (picker) {
        PlaylistPickerDialog(
            sources = enabled,
            activeId = settings.activeSourceId,
            tr = tr,
            onDismiss = { picker = false },
            onPick = {
                vm.selectSource(it)
                picker = false
            }
        )
    }
}

@Composable
private fun PlaylistSlotCard(
    src: SourceEntity,
    slot: Int,
    active: Boolean,
    canMoveUp: Boolean,
    canMoveDown: Boolean,
    portrait: Boolean,
    tr: Boolean,
    onToggle: () -> Unit,
    onRefresh: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
    onActivate: () -> Unit
) {
    val g = LocalGlass.current
    val customName = src.name.isNotBlank() && src.name !in listOf("Xtream", "M3U", "Yerel M3U", "Stalker")
    val title = if (customName) src.name else slotTitle(slot, tr)
    val body: @Composable () -> Unit = {
        Column(
            Modifier
                .alpha(if (src.enabled) 1f else 0.48f)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(verticalAlignment = Alignment.Top) {
                Box(
                    Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(if (slot == 1) g.accent.copy(alpha = 0.25f) else g.text.copy(alpha = 0.08f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text("$slot", color = g.text, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                }
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            title,
                            color = g.text,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false)
                        )
                        KindBadge(src.kind, tr)
                        if (!src.enabled) DisabledBadge(tr)
                    }
                    if (customName) {
                        Text(slotTitle(slot, tr), color = g.muted, style = MaterialTheme.typography.labelMedium)
                    }
                    Spacer(Modifier.height(4.dp))
                    Text(
                        slotSummary(src, tr),
                        color = g.muted,
                        style = MaterialTheme.typography.bodySmall,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                if (!portrait) {
                    Spacer(Modifier.width(8.dp))
                    PlaylistSwitch(src.enabled, onToggle)
                }
            }
            if (portrait) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Spacer(Modifier.weight(1f))
                    PlaylistSwitch(src.enabled, onToggle)
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                IconGlass(Icons.Filled.KeyboardArrowUp, enabled = canMoveUp, onClick = onMoveUp)
                IconGlass(Icons.Filled.KeyboardArrowDown, enabled = canMoveDown, onClick = onMoveDown)
                if (src.kind != "M3U_FILE") {
                    IconGlass(Icons.Filled.Refresh, onClick = onRefresh)
                }
                IconGlass(Icons.Filled.Edit, onClick = onEdit)
                IconGlass(Icons.Filled.Delete, tint = g.danger, onClick = onDelete)
            }
        }
    }
    if (LocalMobileSettingsChrome.current) {
        MobileSettingsFrame(onClick = onActivate) { body() }
    } else {
        GlassPanel(strong = active && src.enabled, radius = 16.dp, onClick = onActivate, modifier = Modifier.fillMaxWidth()) { body() }
    }
}

@Composable
private fun AddPlaylistCard(slot: Int, tr: Boolean, onClick: () -> Unit) {
    val g = LocalGlass.current
    GlassPanel(
        radius = 16.dp,
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .drawBehind {
                drawRoundRect(
                    color = g.accent.copy(alpha = 0.55f),
                    cornerRadius = CornerRadius(16.dp.toPx(), 16.dp.toPx()),
                    style = Stroke(
                        width = 1.4.dp.toPx(),
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(16f, 10f))
                    )
                )
            }
    ) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(g.accent.copy(alpha = 0.18f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Filled.Add, contentDescription = null, tint = g.accent)
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    if (tr) "Yeni liste ekle" else "Add new playlist",
                    color = g.text,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    if (tr) {
                        "M3U URL, M3U dosyası veya Xtream — $slot. slot olarak eklenir"
                    } else {
                        "M3U URL, M3U file or Xtream — added as slot #$slot"
                    },
                    color = g.muted,
                    style = MaterialTheme.typography.bodySmall
                )
            }
            Icon(Icons.Filled.ChevronRight, contentDescription = null, tint = g.muted)
        }
    }
}

@Composable
private fun PlaylistEditorPage(
    existing: SourceEntity?,
    slot: Int,
    tr: Boolean,
    vm: RayViewModel,
    onClose: () -> Unit
) {
    val g = LocalGlass.current
    val clip = LocalClipboardManager.current
    val ctx = LocalContext.current
    var name by remember { mutableStateOf(existing?.name.orEmpty()) }
    var kind by remember {
        mutableStateOf(
            when (existing?.kind) {
                "XTREAM" -> PlaylistEditorKind.XTREAM
                "STALKER" -> PlaylistEditorKind.STALKER
                "M3U_FILE" -> PlaylistEditorKind.M3U_FILE
                else -> PlaylistEditorKind.M3U_URL
            }
        )
    }
    var url by remember { mutableStateOf(if (existing?.kind == "M3U") existing.baseUrl else "") }
    var fileUri by remember { mutableStateOf(if (existing?.kind == "M3U_FILE") existing.baseUrl else "") }
    var server by remember {
        mutableStateOf(if (existing?.kind == "XTREAM" || existing?.kind == "STALKER") existing.baseUrl else "")
    }
    var user by remember { mutableStateOf(if (existing?.kind == "XTREAM") existing.username else "") }
    var pass by remember { mutableStateOf(if (existing?.kind == "XTREAM") existing.password else "") }
    var mac by remember { mutableStateOf(if (existing?.kind == "STALKER") existing.username else "") }
    val pick = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            runCatching {
                ctx.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            fileUri = uri.toString()
        }
    }
    val title = when {
        existing == null -> if (tr) "Yeni liste ekle" else "Add new playlist"
        slot <= 1 -> if (tr) "Birincil listeyi düzenle" else "Edit primary playlist"
        else -> if (tr) "$slot. listeyi düzenle" else "Edit list #$slot"
    }
    Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(title, color = g.text, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        GlassField(if (tr) "Liste adı (opsiyonel)" else "List name (optional)", name) { name = it }
        Text(if (tr) "Örn: Spor Paketi" else "e.g. Sports Pack", color = g.muted, style = MaterialTheme.typography.labelMedium)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            KindChip("M3U URL", kind == PlaylistEditorKind.M3U_URL) { kind = PlaylistEditorKind.M3U_URL }
            KindChip(if (tr) "M3U Dosya" else "M3U File", kind == PlaylistEditorKind.M3U_FILE) { kind = PlaylistEditorKind.M3U_FILE }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            KindChip("Xtream", kind == PlaylistEditorKind.XTREAM) { kind = PlaylistEditorKind.XTREAM }
            KindChip("Stalker", kind == PlaylistEditorKind.STALKER) { kind = PlaylistEditorKind.STALKER }
        }
        when (kind) {
            PlaylistEditorKind.M3U_URL -> {
                GlassField("M3U URL", url) { url = it }
                GlassButton(if (tr) "Yapıştır" else "Paste") {
                    val t = clip.getText()?.text.orEmpty()
                    if (t.isNotBlank()) url = t
                    else vm.toast.value = if (tr) "Panoda metin yok" else "Clipboard is empty"
                }
            }
            PlaylistEditorKind.M3U_FILE -> {
                Text(
                    if (fileUri.isBlank()) {
                        if (tr) "Henüz dosya seçilmedi" else "No file selected"
                    } else {
                        fileUri.substringAfterLast('/')
                    },
                    color = g.muted,
                    style = MaterialTheme.typography.bodySmall
                )
                GlassButton(
                    if (fileUri.isBlank()) {
                        if (tr) "Dosya seç" else "Choose file"
                    } else {
                        if (tr) "Başka dosya seç" else "Choose another file"
                    }
                ) { pick.launch(arrayOf("*/*")) }
            }
            PlaylistEditorKind.XTREAM -> {
                GlassField(if (tr) "Sunucu Adresi" else "Server URL", server) { server = it }
                GlassField(if (tr) "Kullanıcı Adı" else "Username", user) { user = it }
                GlassField(if (tr) "Şifre" else "Password", pass) { pass = it }
            }
            PlaylistEditorKind.STALKER -> {
                GlassField("Portal URL", server) { server = it }
                GlassField("MAC", mac) { mac = it }
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            GlassButton(if (tr) "Vazgeç" else "Cancel") { onClose() }
            GlassButton(if (tr) "Kaydet" else "Save") {
                val label = name.trim()
                val id = existing?.id
                when (kind) {
                    PlaylistEditorKind.M3U_URL -> vm.saveM3u(id, label.ifBlank { "M3U" }, url)
                    PlaylistEditorKind.M3U_FILE -> {
                        val uri = fileUri.ifBlank { existing?.takeIf { it.kind == "M3U_FILE" }?.baseUrl.orEmpty() }
                        if (uri.isBlank()) {
                            vm.toast.value = if (tr) "Önce bir M3U dosyası seçin" else "Pick an M3U file first"
                            return@GlassButton
                        }
                        vm.saveLocalM3u(id, label.ifBlank { "Yerel M3U" }, uri)
                    }
                    PlaylistEditorKind.XTREAM -> vm.saveXtream(id, label.ifBlank { "Xtream" }, server, user, pass)
                    PlaylistEditorKind.STALKER -> vm.saveStalker(id, label.ifBlank { "Stalker" }, server, mac)
                }
                onClose()
            }
        }
        Spacer(Modifier.height(20.dp))
    }
}

@Composable
private fun KindChip(label: String, selected: Boolean, onClick: () -> Unit) {
    val g = LocalGlass.current
    GlassPanel(strong = selected, accentFill = selected, radius = 12.dp, onClick = onClick) {
        Text(label, color = g.text, modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp))
    }
}

@Composable
private fun KindBadge(kind: String, tr: Boolean) {
    val g = LocalGlass.current
    val label = when (kind) {
        "XTREAM" -> "Xtream"
        "STALKER" -> "Stalker"
        "M3U_FILE" -> if (tr) "M3U · Dosya" else "M3U · File"
        else -> "M3U"
    }
    Box(
        Modifier
            .clip(RoundedCornerShape(10.dp))
            .background(g.accent.copy(alpha = 0.18f))
            .padding(horizontal = 8.dp, vertical = 2.dp)
    ) {
        Text(label, color = g.text, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun DisabledBadge(tr: Boolean) {
    val g = LocalGlass.current
    Box(
        Modifier
            .clip(RoundedCornerShape(10.dp))
            .background(g.danger.copy(alpha = 0.16f))
            .padding(horizontal = 8.dp, vertical = 2.dp)
    ) {
        Text(
            if (tr) "Devre dışı" else "Disabled",
            color = g.danger,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun PlaylistSwitch(on: Boolean, onToggle: () -> Unit) {
    GlassPanel(
        modifier = Modifier.width(46.dp).height(28.dp),
        radius = 14.dp,
        strong = on,
        accentFill = on,
        onClick = onToggle
    ) {
        Box(
            Modifier
                .fillMaxWidth()
                .height(28.dp)
                .padding(3.dp)
        ) {
            Box(
                Modifier
                    .align(if (on) Alignment.CenterEnd else Alignment.CenterStart)
                    .size(22.dp)
                    .clip(CircleShape)
                    .background(Color.White)
            )
        }
    }
}

@Composable
private fun IconGlass(
    icon: ImageVector,
    enabled: Boolean = true,
    tint: Color? = null,
    onClick: () -> Unit
) {
    val g = LocalGlass.current
    GlassPanel(
        radius = 12.dp,
        onClick = if (enabled) onClick else null,
        modifier = Modifier.alpha(if (enabled) 1f else 0.35f)
    ) {
        Icon(
            icon,
            contentDescription = null,
            tint = tint ?: g.text,
            modifier = Modifier.padding(8.dp).size(20.dp)
        )
    }
}

@Composable
private fun PlaylistPickerDialog(
    sources: List<SourceEntity>,
    activeId: String,
    tr: Boolean,
    onDismiss: () -> Unit,
    onPick: (String) -> Unit
) {
    val g = LocalGlass.current
    Dialog(onDismissRequest = onDismiss) {
        GlassPanel(strong = true, radius = 20.dp, modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    if (tr) "Liste Seç" else "Select List",
                    color = g.text,
                    style = MaterialTheme.typography.headlineSmall
                )
                sources.forEachIndexed { i, src ->
                    val on = src.id == activeId
                    GlassPanel(
                        strong = on,
                        accentFill = on,
                        radius = 12.dp,
                        onClick = { onPick(src.id) },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text(src.name.ifBlank { slotTitle(i + 1, tr) }, color = g.text)
                                Text(playlistKindLabel(src.kind), color = g.muted, style = MaterialTheme.typography.bodySmall)
                            }
                            if (on) {
                                Text(
                                    if (tr) "Aktif" else "Active",
                                    color = g.text,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
                GlassButton(if (tr) "Vazgeç" else "Cancel") { onDismiss() }
            }
        }
    }
}

private fun slotTitle(slot: Int, tr: Boolean): String =
    if (slot <= 1) {
        if (tr) "Birincil liste" else "Primary playlist"
    } else {
        if (tr) "$slot. liste" else "List #$slot"
    }

private fun slotSummary(src: SourceEntity, tr: Boolean): String = when (src.kind) {
    "M3U_FILE" -> if (tr) "Yerel M3U" else "Local M3U"
    else -> src.baseUrl
}

private fun playlistKindLabel(kind: String): String = when (kind) {
    "XTREAM" -> "Xtream"
    "M3U" -> "M3U"
    "M3U_FILE" -> "M3U · File"
    "STALKER" -> "Stalker"
    else -> kind
}
