package com.ray.iptv.ui.screens.settings

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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.ChildCare
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Sort
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.tv.material3.Icon
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import androidx.compose.ui.focus.FocusRequester
import com.ray.iptv.data.local.CategoryEntity
import com.ray.iptv.data.local.ChannelEntity
import com.ray.iptv.data.repo.ProfileRepository
import com.ray.iptv.data.repo.RaySettings
import com.ray.iptv.ui.RayViewModel
import com.ray.iptv.ui.components.GlassButton
import com.ray.iptv.ui.glass.GlassPanel
import com.ray.iptv.ui.screens.onboarding.GlassField
import com.ray.iptv.ui.theme.LocalGlass

private enum class ChannelLayoutSub { HUB, HIDE, LIVE, PARENTAL }

@Composable
internal fun ChannelLayoutRoot(
    vm: RayViewModel,
    settings: RaySettings,
    liveCats: List<CategoryEntity>,
    tr: Boolean,
    focusRequester: FocusRequester? = null,
    onBack: () -> Unit
) {
    var sub by remember { mutableStateOf(ChannelLayoutSub.HUB) }
    val movieCats by vm.movieCategories.collectAsState()
    val seriesCats by vm.seriesCategories.collectAsState()
    val hasPin = settings.parentalPinHash.isNotBlank()
    when (sub) {
        ChannelLayoutSub.HUB -> ChannelLayoutHub(
            tr = tr,
            hasPin = hasPin,
            onBack = onBack,
            focusRequester = focusRequester,
            onHide = { sub = if (hasPin) ChannelLayoutSub.PARENTAL else ChannelLayoutSub.HIDE },
            onLive = { sub = ChannelLayoutSub.LIVE },
            onParental = { sub = ChannelLayoutSub.PARENTAL }
        )
        ChannelLayoutSub.HIDE -> CategoryHidePage(
            vm, liveCats, movieCats, seriesCats, tr,
            onBack = { sub = ChannelLayoutSub.HUB },
            embedded = false
        )
        ChannelLayoutSub.LIVE -> LiveChannelEditorPage(
            vm, liveCats, tr,
            onBack = { sub = ChannelLayoutSub.HUB }
        )
        ChannelLayoutSub.PARENTAL -> ParentalControlPage(
            vm, settings, liveCats, movieCats, seriesCats, tr,
            onBack = { sub = ChannelLayoutSub.HUB }
        )
    }
}

@Composable
private fun ChannelLayoutHub(
    tr: Boolean,
    hasPin: Boolean,
    onBack: () -> Unit,
    focusRequester: FocusRequester? = null,
    onHide: () -> Unit,
    onLive: () -> Unit,
    onParental: () -> Unit
) {
    Column(Modifier.fillMaxSize()) {
        LayoutHeader(
            title = if (tr) "Kanal Kategori Düzeni" else "Channel & Category Layout",
            hint = if (tr) {
                "Kategorileri gizleyebilir ve canlı kanal listenin sırasını/içeriğini düzenleyebilirsin. Her seçenek kendi düzenleyicisini açar; geri dönüldüğünde değişiklikler ana ekrana yansır."
            } else {
                "Hide categories and edit the order/contents of your live channel list. Each row opens its own editor; changes are reflected on the home screen when you go back."
            },
            onBack = onBack,
            focusRequester = focusRequester
        )
        Spacer(Modifier.height(12.dp))
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            ChannelLayoutNavRow(
                icon = if (hasPin) Icons.Filled.Lock else Icons.Filled.VisibilityOff,
                iconTint = if (hasPin) Color(0xFFFFC107) else null,
                title = if (tr) "Kategori göster / gizle" else "Show / hide categories",
                subtitle = buildString {
                    append(if (tr) "Kanallar, filmler ve diziler için kategori göster / gizle" else "Show or hide categories for channels, movies and series")
                    if (hasPin) append("  🔒")
                },
                onClick = onHide
            )
            ChannelLayoutNavRow(
                icon = Icons.AutoMirrored.Filled.Sort,
                title = if (tr) "Canlı kanal düzeni" else "Live channel layout",
                subtitle = if (tr) "Yalnızca canlı TV: kategori seçip kanalları sıralayın veya çıkarın" else "Live TV only: pick a category and reorder or remove channels",
                onClick = onLive
            )
            ChannelLayoutNavRow(
                icon = Icons.Filled.ChildCare,
                title = if (tr) "Ebeveyn denetimi" else "Parental controls",
                subtitle = if (tr) "PIN ile koruma; Xtream kategorilerini gizleyin (canlı, film, dizi)" else "PIN protection; hide Xtream categories (live, movies, series)",
                onClick = onParental
            )
        }
    }
}

@Composable
private fun ChannelLayoutNavRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    iconTint: Color? = null
) {
    if (LocalMobileSettingsChrome.current) {
        MobileOptionTile(
            icon = icon,
            title = title,
            subtitle = subtitle,
            onClick = onClick,
            accent = iconTint
        )
        return
    }
    val g = LocalGlass.current
    var focused by remember { mutableStateOf(false) }
    val tint = iconTint ?: g.accent
    GlassPanel(
        focused = focused,
        radius = 18.dp,
        onClick = onClick,
        modifier = Modifier.fillMaxWidth().onFocusChanged { focused = it.isFocused }
    ) {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier.size(42.dp).clip(CircleShape).background(tint.copy(alpha = 0.18f)).border(1.dp, tint.copy(alpha = 0.40f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(22.dp))
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(title, color = g.text, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.ExtraBold)
                Spacer(Modifier.height(2.dp))
                Text(subtitle, color = g.muted, style = MaterialTheme.typography.bodySmall, maxLines = 2, overflow = TextOverflow.Ellipsis)
            }
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, tint = g.muted)
        }
    }
}

@Composable
private fun LayoutHeader(
    title: String,
    hint: String? = null,
    onBack: (() -> Unit)?,
    focusRequester: FocusRequester? = null,
    trailing: @Composable (() -> Unit)? = null
) {
    val g = LocalGlass.current
    Column(Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            if (onBack != null && !LocalMobileSettingsChrome.current) GlassButton("←", focusRequester = focusRequester) { onBack() }
            if (!LocalMobileSettingsChrome.current) {
                Text(title, color = g.text, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.ExtraBold, modifier = Modifier.weight(1f))
            } else {
                Spacer(Modifier.weight(1f))
            }
            trailing?.invoke()
        }
        if (!hint.isNullOrBlank()) {
            Spacer(Modifier.height(8.dp))
            Text(hint, color = g.muted, style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
private fun CategoryHidePage(
    vm: RayViewModel,
    liveCats: List<CategoryEntity>,
    movieCats: List<CategoryEntity>,
    seriesCats: List<CategoryEntity>,
    tr: Boolean,
    onBack: (() -> Unit)?,
    embedded: Boolean
) {
    val g = LocalGlass.current
    var tab by remember { mutableIntStateOf(0) }
    val liveRows = remember { mutableStateListOf<CategoryEntity>() }
    val movieRows = remember { mutableStateListOf<CategoryEntity>() }
    val seriesRows = remember { mutableStateListOf<CategoryEntity>() }
    val sourceKey = (liveCats + movieCats + seriesCats).joinToString("|") { it.id }
    LaunchedEffect(sourceKey) {
        fun SnapshotStateList<CategoryEntity>.reload(src: List<CategoryEntity>) {
            clear()
            addAll(src.sortedWith(compareBy({ it.sortOrder }, { it.name })))
        }
        liveRows.reload(liveCats)
        movieRows.reload(movieCats)
        seriesRows.reload(seriesCats)
    }
    val rows = when (tab) {
        0 -> liveRows
        1 -> movieRows
        else -> seriesRows
    }
    val empty = when (tab) {
        0 -> if (tr) "Canlı kategori listesi boş." else "No live categories."
        1 -> if (tr) "Film kategorisi listesi boş." else "No movie categories."
        else -> if (tr) "Dizi kategorisi listesi boş." else "No series categories."
    }
    val unavailable = liveCats.isEmpty() && movieCats.isEmpty() && seriesCats.isEmpty()
    val save: () -> Unit = { vm.saveCategoryLayout(liveRows + movieRows + seriesRows) }
    Column(Modifier.fillMaxSize()) {
        if (!embedded) {
            LayoutHeader(
                title = if (tr) "Kategori göster / gizle" else "Show / hide categories",
                onBack = onBack,
                trailing = { if (!unavailable) GlassButton(if (tr) "Kaydet" else "Save", onClick = save) }
            )
            Spacer(Modifier.height(10.dp))
        } else {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                if (!unavailable) GlassButton(if (tr) "Kaydet" else "Save", onClick = save)
            }
            Spacer(Modifier.height(8.dp))
        }
        if (unavailable) {
            GlassPanel(strong = true, radius = 18.dp, modifier = Modifier.fillMaxWidth()) {
                Text(
                    if (tr) "Önce bir oynatma listesi yükleyin (Xtream veya M3U)." else "Load a playlist first (Xtream or M3U).",
                    color = g.muted,
                    modifier = Modifier.padding(24.dp)
                )
            }
        } else {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(if (tr) "Canlı" else "Live", if (tr) "Filmler" else "Movies", if (tr) "Diziler" else "Series").forEachIndexed { i, label ->
                    GlassButton(label, modifier = Modifier.weight(1f)) { tab = i }
                }
            }
            Spacer(Modifier.height(8.dp))
            Text(if (tr) "Sıralamayı değiştirmek için sağdaki okları kullanın." else "Use the arrows on the right to change the order.", color = g.muted, style = MaterialTheme.typography.labelMedium)
            Text(if (tr) "Anahtar açık = kategori görünür, kapalı = gizli." else "Switch on = category visible, off = hidden.", color = g.muted, style = MaterialTheme.typography.labelMedium)
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                GlassButton(if (tr) "Tümünü Gizle" else "Hide all", modifier = Modifier.weight(1f)) {
                    rows.indices.forEach { rows[it] = rows[it].copy(hidden = true) }
                }
                GlassButton(if (tr) "Tümünü Göster" else "Show all", modifier = Modifier.weight(1f)) {
                    rows.indices.forEach { rows[it] = rows[it].copy(hidden = false) }
                }
            }
            Spacer(Modifier.height(10.dp))
            if (rows.isEmpty()) {
                Text(empty, color = g.muted, modifier = Modifier.padding(24.dp))
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxSize()) {
                    itemsIndexed(rows, key = { _, c -> c.id }) { index, cat ->
                        CategoryHideRow(
                            cat = cat,
                            tr = tr,
                            canUp = index > 0,
                            canDown = index < rows.lastIndex,
                            onToggle = { rows[index] = cat.copy(hidden = !cat.hidden) },
                            onMove = { delta -> rows.move(index, index + delta) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CategoryHideRow(
    cat: CategoryEntity,
    tr: Boolean,
    canUp: Boolean,
    canDown: Boolean,
    onToggle: () -> Unit,
    onMove: (Int) -> Unit
) {
    val g = LocalGlass.current
    var focused by remember { mutableStateOf(false) }
    GlassPanel(focused = focused, radius = 14.dp, modifier = Modifier.fillMaxWidth().onFocusChanged { focused = it.isFocused }) {
        Row(Modifier.padding(horizontal = 12.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(cat.name.ifBlank { cat.remoteId }, color = g.text, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(if (tr) "Kimlik: ${cat.remoteId}" else "ID: ${cat.remoteId}", color = g.muted, style = MaterialTheme.typography.labelSmall)
            }
            GlassSwitch(on = !cat.hidden, onToggle = onToggle)
            Spacer(Modifier.width(6.dp))
            GlassButton("▲") { if (canUp) onMove(-1) }
            GlassButton("▼") { if (canDown) onMove(1) }
        }
    }
}

@Composable
private fun LiveChannelEditorPage(
    vm: RayViewModel,
    liveCats: List<CategoryEntity>,
    tr: Boolean,
    onBack: () -> Unit
) {
    val g = LocalGlass.current
    val lists = remember { mutableMapOf<String, SnapshotStateList<ChannelEntity>>() }
    val hiddenIds = remember { mutableStateListOf<String>() }
    var selected by remember { mutableStateOf(liveCats.firstOrNull()?.id.orEmpty()) }
    var pendingRemove by remember { mutableStateOf<ChannelEntity?>(null) }
    LaunchedEffect(selected, liveCats) {
        val id = selected.ifBlank { liveCats.firstOrNull()?.id.orEmpty() }
        if (id.isBlank() || lists.containsKey(id)) return@LaunchedEffect
        val loaded = vm.layoutChannels(id)
        val row = mutableStateListOf<ChannelEntity>()
        row.addAll(
            loaded.sortedWith(compareBy({ if (it.layoutSort >= 0) 0 else 1 }, { it.layoutSort }, { it.number }))
        )
        lists[id] = row
        if (selected.isBlank()) selected = id
    }
    val current = lists[selected]
    Column(Modifier.fillMaxSize()) {
        LayoutHeader(
            title = if (tr) "Canlı kanal düzeni" else "Live channel layout",
            hint = if (tr) "Üstten canlı kategori seçin. Kumanda: ▲ ▼ sıra · Sil ile kanalı çıkarın." else "Pick a live category above. Remote: ▲ ▼ reorder · Delete removes the channel.",
            onBack = onBack,
            trailing = {
                if (liveCats.isNotEmpty()) {
                    GlassButton(if (tr) "Kaydet" else "Save") {
                        vm.saveLiveChannelLayout(hiddenIds.toSet(), lists.mapValues { e -> e.value.map { it.id } })
                    }
                }
            }
        )
        Spacer(Modifier.height(10.dp))
        if (liveCats.isEmpty()) {
            GlassPanel(strong = true, radius = 18.dp, modifier = Modifier.fillMaxWidth()) {
                Text(if (tr) "Önce bir oynatma listesi yükleyin (Xtream veya M3U)." else "Load a playlist first (Xtream or M3U).", color = g.muted, modifier = Modifier.padding(24.dp))
            }
        } else {
            Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                liveCats.forEach { cat ->
                    val on = cat.id == selected
                    GlassButton((if (on) "● " else "") + cat.name.ifBlank { cat.remoteId }) { selected = cat.id }
                }
            }
            Spacer(Modifier.height(10.dp))
            if (current.isNullOrEmpty()) {
                Text(if (tr) "Bu kategoride kanal yok." else "No channels in this category.", color = g.muted, modifier = Modifier.padding(24.dp))
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxSize()) {
                    itemsIndexed(current, key = { _, ch -> ch.id }) { index, ch ->
                        LiveChannelRow(
                            channel = ch,
                            tr = tr,
                            canUp = index > 0,
                            canDown = index < current.lastIndex,
                            onMove = { delta -> current.move(index, index + delta) },
                            onRemove = { pendingRemove = ch }
                        )
                    }
                }
            }
        }
    }
    pendingRemove?.let { ch ->
        GlassConfirmDialog(
            title = if (tr) "Kanalı listeden çıkar" else "Remove channel from list",
            body = if (tr) "Kanal uygulama listesinde görünmez. Listeyi yeniden yüklemek veya tüm ayarları sıfırlamak geri alabilir." else "The channel will be hidden in the app. Reload the playlist or reset settings to restore.",
            confirm = if (tr) "Sil" else "Delete",
            onDismiss = { pendingRemove = null },
            onConfirm = {
                lists[selected]?.removeAll { it.id == ch.id }
                if (ch.id !in hiddenIds) hiddenIds.add(ch.id)
                pendingRemove = null
            }
        )
    }
}

@Composable
private fun LiveChannelRow(
    channel: ChannelEntity,
    tr: Boolean,
    canUp: Boolean,
    canDown: Boolean,
    onMove: (Int) -> Unit,
    onRemove: () -> Unit
) {
    val g = LocalGlass.current
    var focused by remember { mutableStateOf(false) }
    GlassPanel(focused = focused, radius = 14.dp, modifier = Modifier.fillMaxWidth().onFocusChanged { focused = it.isFocused }) {
        Row(Modifier.padding(horizontal = 10.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(channel.name, color = g.text, fontWeight = FontWeight.SemiBold, maxLines = 2, overflow = TextOverflow.Ellipsis)
                Text(if (tr) "Kimlik: ${channel.remoteId}" else "ID: ${channel.remoteId}", color = g.muted, style = MaterialTheme.typography.labelSmall)
            }
            GlassButton("▲") { if (canUp) onMove(-1) }
            GlassButton("▼") { if (canDown) onMove(1) }
            GlassButton(if (tr) "Sil" else "Del") { onRemove() }
        }
    }
}

@Composable
private fun ParentalControlPage(
    vm: RayViewModel,
    settings: RaySettings,
    liveCats: List<CategoryEntity>,
    movieCats: List<CategoryEntity>,
    seriesCats: List<CategoryEntity>,
    tr: Boolean,
    onBack: () -> Unit
) {
    val g = LocalGlass.current
    var creating by remember { mutableStateOf(settings.parentalPinHash.isBlank()) }
    var phase by remember { mutableIntStateOf(if (settings.parentalPinHash.isBlank()) 0 else 2) }
    var recoveryStep by remember { mutableStateOf(false) }
    var unlocked by remember { mutableStateOf(false) }
    var entered by remember { mutableStateOf("") }
    var firstPin by remember { mutableStateOf("") }
    var error by remember { mutableStateOf("") }
    var success by remember { mutableStateOf("") }
    var recoveryWord by remember { mutableStateOf("") }
    var resetOpen by remember { mutableStateOf(false) }
    var recoveryPrompt by remember { mutableStateOf(false) }
    var recoveryInput by remember { mutableStateOf("") }

    fun digit(d: String) {
        if (entered.length >= 6) return
        entered += d
        error = ""
    }
    fun backspace() {
        if (entered.isNotEmpty()) entered = entered.dropLast(1)
        error = ""
    }
    fun clearPin() {
        entered = ""
        error = ""
    }
    fun submit() {
        if (entered.length < 4) {
            error = if (tr) "PIN 4–6 rakam olmalıdır." else "PIN must be 4–6 digits."
            return
        }
        if (creating) {
            if (phase == 0) {
                firstPin = entered
                entered = ""
                phase = 1
                error = ""
                return
            }
            if (entered != firstPin) {
                error = if (tr) "PIN’ler eşleşmiyor." else "PINs do not match."
                entered = ""
                phase = 0
                firstPin = ""
                return
            }
            firstPin = entered
            entered = ""
            recoveryStep = true
            error = ""
            recoveryWord = ""
            return
        }
        if (settings.parentalPinHash != ProfileRepository.hashPin(entered)) {
            error = if (tr) "PIN yanlış." else "Incorrect PIN."
            entered = ""
            return
        }
        unlocked = true
        entered = ""
    }
    fun saveRecovery() {
        val word = recoveryWord.trim()
        if (word.length < 3) {
            error = if (tr) "Kurtarma kelimesi en az 3 karakter olmalı." else "Recovery word must be at least 3 characters."
            return
        }
        vm.saveParentalPin(firstPin, word)
        success = if (tr) "PIN kaydedildi." else "PIN saved."
        entered = ""
        firstPin = ""
        recoveryStep = false
        creating = false
        unlocked = true
        error = ""
        recoveryWord = ""
    }

    val title = when {
        unlocked -> if (tr) "Kategori göster / gizle" else "Show / hide categories"
        recoveryStep -> if (tr) "Kurtarma kelimesi belirle" else "Set a recovery word"
        creating && phase == 0 -> if (tr) "PIN oluştur" else "Create PIN"
        creating && phase == 1 -> if (tr) "PIN’i doğrula" else "Confirm PIN"
        else -> if (tr) "PIN’i gir" else "Enter PIN"
    }
    val intro = when {
        recoveryStep -> if (tr) "PIN’i unutursan sıfırlamak için bu gizli kelimeyi gireceksin. Kimseyle paylaşma." else "If you forget your PIN, you will enter this secret word to reset it. Do not share it."
        creating && phase == 0 -> if (tr) "4–6 haneli bir PIN belirleyin. Kategori gizleme bu PIN ile açılır." else "Choose a 4–6 digit PIN. Category hiding opens with this PIN."
        creating && phase == 1 -> if (tr) "Aynı PIN’i bir kez daha girin." else "Enter the same PIN once more."
        else -> if (tr) "Kategori ayarlarını açmak için PIN’inizi girin." else "Enter your PIN to open category settings."
    }

    Column(
        Modifier
            .fillMaxSize()
            .onPreviewKeyEvent { ev ->
                if (unlocked || recoveryStep || ev.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
                ev.key.digitOrNull()?.let { digit(it.toString()); return@onPreviewKeyEvent true }
                if (ev.key == Key.Backspace) { backspace(); return@onPreviewKeyEvent true }
                false
            }
    ) {
        LayoutHeader(
            title = title,
            onBack = onBack,
            trailing = {
                if (unlocked || !creating) GlassButton(if (tr) "Sıfırla" else "Reset") {
                    if (settings.pinRecoveryHash.isNotBlank()) recoveryPrompt = true else resetOpen = true
                }
            }
        )
        Spacer(Modifier.height(10.dp))
        if (unlocked) {
            SettingsToggleRow(
                if (tr) "Kilitli kategorileri gizle" else "Hide locked categories",
                if (tr) "PIN ile kilitlenen kategoriler listede görünmez" else "PIN-locked categories stay out of the list",
                settings.hideLocked
            ) { vm.setHideLocked(!settings.hideLocked) }
            Spacer(Modifier.height(10.dp))
            CategoryHidePage(vm, liveCats, movieCats, seriesCats, tr, onBack = null, embedded = true)
        } else {
            Column(
                Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(bottom = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                GlassPanel(strong = true, radius = 22.dp, modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(intro, color = g.muted, style = MaterialTheme.typography.bodySmall)
                        Spacer(Modifier.height(16.dp))
                        if (recoveryStep) {
                            GlassField(if (tr) "Gizli kurtarma kelimesi" else "Secret recovery word", recoveryWord) { recoveryWord = it; error = "" }
                            if (error.isNotBlank()) {
                                Spacer(Modifier.height(8.dp))
                                StatusBanner(error, Color(0xFFEF4444))
                            }
                            Spacer(Modifier.height(14.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                GlassButton(if (tr) "Vazgeç" else "Cancel") {
                                    recoveryStep = false
                                    phase = 0
                                    entered = ""
                                    firstPin = ""
                                    error = ""
                                    recoveryWord = ""
                                }
                                GlassButton(if (tr) "PIN’i kaydet" else "Save PIN") { saveRecovery() }
                            }
                        } else {
                            PinDots(entered, g.accent)
                            if (error.isNotBlank()) {
                                Spacer(Modifier.height(10.dp))
                                StatusBanner(error, Color(0xFFEF4444))
                            }
                            if (success.isNotBlank()) {
                                Spacer(Modifier.height(10.dp))
                                StatusBanner(success, Color(0xFF22C55E))
                            }
                            Spacer(Modifier.height(14.dp))
                            PinPad(onDigit = ::digit, onBackspace = ::backspace, onClear = ::clearPin)
                            Spacer(Modifier.height(12.dp))
                            GlassButton(
                                when {
                                    creating && phase == 0 -> if (tr) "İleri" else "Next"
                                    creating -> if (tr) "PIN’i kaydet" else "Save PIN"
                                    else -> if (tr) "Devam" else "Continue"
                                }
                            ) { submit() }
                            if (!creating) {
                                Spacer(Modifier.height(10.dp))
                                GlassButton(if (tr) "PIN’i sıfırla" else "Reset PIN") {
                                    if (settings.pinRecoveryHash.isNotBlank()) recoveryPrompt = true else resetOpen = true
                                }
                            }
                        }
                    }
                }
            }
        }
    }
    if (resetOpen) {
        GlassConfirmDialog(
                title = if (tr) "PIN’i sıfırla" else "Reset PIN",
                body = if (tr) "Mevcut PIN silinecek ve yeniden oluşturmanız gerekecek. Devam etmek istiyor musunuz?" else "The current PIN will be deleted and you will need to create a new one. Continue?",
                confirm = if (tr) "Tamam" else "OK",
                onDismiss = { resetOpen = false },
                onConfirm = {
                    vm.clearParental()
                    creating = true
                    phase = 0
                    unlocked = false
                    recoveryStep = false
                    entered = ""
                    firstPin = ""
                    error = ""
                    resetOpen = false
                }
            )
    }
    if (recoveryPrompt) {
        Dialog(onDismissRequest = { recoveryPrompt = false }) {
            GlassPanel(strong = true, radius = 20.dp, modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(if (tr) "Kurtarma kelimesini gir" else "Enter recovery word", color = g.text, style = MaterialTheme.typography.headlineSmall)
                    Text(if (tr) "PIN’i sıfırlamak için belirlediğin gizli kurtarma kelimesini gir." else "Enter the secret recovery word you set to reset the PIN.", color = g.muted, style = MaterialTheme.typography.bodyMedium)
                    GlassField(if (tr) "Gizli kurtarma kelimesi" else "Secret recovery word", recoveryInput) { recoveryInput = it }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.align(Alignment.End)) {
                        GlassButton(if (tr) "Vazgeç" else "Cancel") { recoveryPrompt = false; recoveryInput = "" }
                        GlassButton(if (tr) "Tamam" else "OK") {
                            if (vm.recoverPin(recoveryInput)) {
                                creating = true
                                phase = 0
                                unlocked = false
                                recoveryStep = false
                                entered = ""
                                firstPin = ""
                                error = ""
                            } else {
                                error = if (tr) "Kurtarma kelimesi yanlış. PIN sıfırlanamadı." else "Wrong recovery word. PIN was not reset."
                            }
                            recoveryPrompt = false
                            recoveryInput = ""
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PinDots(entered: String, accent: Color) {
    Row(horizontalArrangement = Arrangement.Center, modifier = Modifier.fillMaxWidth()) {
        repeat(6) { i ->
            val filled = i < entered.length
            val active = i == entered.length
            Box(
                Modifier
                    .padding(horizontal = 6.dp)
                    .size(if (filled) 14.dp else 12.dp)
                    .clip(CircleShape)
                    .background(when { filled -> accent.copy(alpha = 0.95f); active -> Color.White.copy(alpha = 0.32f); else -> Color.White.copy(alpha = 0.16f) })
                    .border(1.dp, Color.White.copy(alpha = if (filled) 0.55f else 0.28f), CircleShape)
            )
        }
    }
}

@Composable
private fun PinPad(onDigit: (String) -> Unit, onBackspace: () -> Unit, onClear: () -> Unit) {
    val keys = listOf(listOf("1", "2", "3"), listOf("4", "5", "6"), listOf("7", "8", "9"), listOf("⌫", "0", "C"))
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        keys.forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                row.forEach { key ->
                    GlassButton(key, modifier = Modifier.weight(1f)) {
                        when (key) {
                            "⌫" -> onBackspace()
                            "C" -> onClear()
                            else -> onDigit(key)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StatusBanner(text: String, color: Color) {
    Box(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(color.copy(alpha = 0.16f))
            .border(1.dp, color.copy(alpha = 0.55f), RoundedCornerShape(14.dp))
            .padding(horizontal = 12.dp, vertical = 9.dp)
    ) {
        Text(text, color = color, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
private fun GlassSwitch(on: Boolean, onToggle: () -> Unit) {
    val g = LocalGlass.current
    var focused by remember { mutableStateOf(false) }
    GlassPanel(
        focused = focused,
        radius = 16.dp,
        onClick = onToggle,
        modifier = Modifier.width(52.dp).height(30.dp).onFocusChanged { focused = it.isFocused }
    ) {
        Box(Modifier.fillMaxSize().padding(3.dp)) {
            Box(
                Modifier
                    .size(24.dp)
                    .align(if (on) Alignment.CenterEnd else Alignment.CenterStart)
                    .clip(CircleShape)
                    .background(if (on) g.accent else Color.White.copy(alpha = 0.45f))
            )
        }
    }
}

private fun <T> SnapshotStateList<T>.move(from: Int, to: Int) {
    if (from !in indices || to !in indices || from == to) return
    add(to, removeAt(from))
}

private fun Key.digitOrNull(): Char? = when (this) {
    Key.Zero, Key.NumPad0 -> '0'
    Key.One, Key.NumPad1 -> '1'
    Key.Two, Key.NumPad2 -> '2'
    Key.Three, Key.NumPad3 -> '3'
    Key.Four, Key.NumPad4 -> '4'
    Key.Five, Key.NumPad5 -> '5'
    Key.Six, Key.NumPad6 -> '6'
    Key.Seven, Key.NumPad7 -> '7'
    Key.Eight, Key.NumPad8 -> '8'
    Key.Nine, Key.NumPad9 -> '9'
    else -> null
}
