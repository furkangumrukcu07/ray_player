package com.ray.iptv.ui.screens.settings

import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.Devices
import androidx.compose.material.icons.filled.Dns
import androidx.compose.material.icons.filled.FontDownload
import androidx.compose.material.icons.filled.LiveTv
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.NetworkCheck
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Update
import androidx.compose.material.icons.filled.Vibration
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Icon
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.ray.iptv.data.epg.GlobalEpg
import com.ray.iptv.data.local.EpgSourceEntity
import com.ray.iptv.data.local.SourceEntity
import com.ray.iptv.data.repo.CatchupPreset
import com.ray.iptv.data.repo.EpgSourceMode
import com.ray.iptv.data.repo.LayoutMode
import com.ray.iptv.data.repo.RaySettings
import com.ray.iptv.ui.RayViewModel
import com.ray.iptv.ui.components.GlassButton
import com.ray.iptv.ui.glass.GlassPanel
import com.ray.iptv.ui.input.AdaptiveHaptics
import com.ray.iptv.ui.screens.onboarding.GlassField
import com.ray.iptv.ui.theme.LocalGlass
import com.ray.iptv.ui.theme.appFontOption
import com.ray.iptv.ui.theme.appFontOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.InetSocketAddress
import java.net.Socket
import java.net.URI
import androidx.compose.ui.focus.FocusRequester
import java.net.URL
import kotlin.math.roundToInt

private enum class OtherToolsSub { LIST, EPG, SOURCES, SPEED }

@Composable
internal fun OtherToolsRoot(
    vm: RayViewModel,
    settings: RaySettings,
    epgSources: List<EpgSourceEntity>,
    sources: List<SourceEntity>,
    tr: Boolean,
    focusRequester: FocusRequester? = null,
    onBack: () -> Unit
) {
    var sub by remember { mutableStateOf(OtherToolsSub.LIST) }
    when (sub) {
        OtherToolsSub.LIST -> OtherToolsList(vm, settings, tr, onBack, focusRequester, { sub = OtherToolsSub.EPG }, { sub = OtherToolsSub.SPEED })
        OtherToolsSub.EPG -> EpgSettingsPage(vm, settings, sources, tr, { sub = OtherToolsSub.LIST }, { sub = OtherToolsSub.SOURCES })
        OtherToolsSub.SOURCES -> EpgSourcesPage(vm, settings, epgSources, tr) { sub = OtherToolsSub.EPG }
        OtherToolsSub.SPEED -> SpeedTestPage(sources, tr) { sub = OtherToolsSub.LIST }
    }
}

@Composable
private fun OtherToolsList(
    vm: RayViewModel,
    settings: RaySettings,
    tr: Boolean,
    onBack: () -> Unit,
    focusRequester: FocusRequester? = null,
    onEpg: () -> Unit,
    onSpeed: () -> Unit
) {
    var sleepOpen by remember { mutableStateOf(false) }
    var fontOpen by remember { mutableStateOf(false) }
    var layoutOpen by remember { mutableStateOf(false) }
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(settings.sleepUntilMs) {
        while (settings.sleepUntilMs > System.currentTimeMillis()) {
            now = System.currentTimeMillis()
            delay(15_000)
        }
        now = System.currentTimeMillis()
    }
    val font = appFontOption(settings.appFontKey)
    val mobileChrome = LocalMobileSettingsChrome.current
    val hapticCtx = LocalContext.current
    LazyColumn(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item {
            ToolsHeader(
                title = if (tr) "Diğer Araçlar" else "Other Tools",
                hint = if (tr) {
                    "Daha seyrek kullanılan yardımcı araçlar tek bir yerde toplandı."
                } else {
                    "Less frequently used helper tools are gathered in one place."
                },
                onBack = onBack,
                focusRequester = focusRequester
            )
        }
        item {
            ToolsTile(
                icon = Icons.Filled.Devices,
                title = if (tr) "Yerleşim" else "Layout",
                subtitle = when (settings.layoutMode) {
                    LayoutMode.MOBILE -> if (tr) "Mobil" else "Mobile"
                    LayoutMode.TV -> "TV"
                },
                onClick = { layoutOpen = true }
            )
        }
        item {
            ToolsTile(
                icon = Icons.Filled.Bedtime,
                title = if (tr) "Uyku zamanlayıcısı" else "Sleep timer",
                subtitle = sleepSubtitle(settings.sleepUntilMs, now, tr),
                onClick = { sleepOpen = true }
            )
        }
        item {
            ToolsTile(
                icon = Icons.Filled.PowerSettingsNew,
                title = if (tr) "Cihaz Açıldığında Başlat" else "Launch when device starts",
                subtitle = if (settings.launchOnBoot) (if (tr) "Aktif" else "On") else (if (tr) "Pasif" else "Off"),
                showSwitch = true,
                checked = settings.launchOnBoot,
                onClick = { vm.setLaunchOnBoot(!settings.launchOnBoot) }
            )
        }
        item {
            ToolsTile(
                icon = Icons.Filled.CalendarMonth,
                title = "EPG",
                subtitle = if (tr) "Rehber, kaynak ve eşleştirme" else "Guide, source and matching",
                onClick = onEpg
            )
        }
        item {
            ToolsTile(
                icon = Icons.Filled.NetworkCheck,
                title = if (tr) "Hız Testi" else "Speed test",
                subtitle = if (tr) "İnternet hızını ölç" else "Measure internet speed",
                onClick = onSpeed
            )
        }
        if (mobileChrome) {
            item {
                ToolsTile(
                    icon = Icons.Filled.Vibration,
                    title = if (tr) "Adaptif titreşim" else "Adaptive haptics",
                    subtitle = if (settings.adaptiveHaptics) {
                        if (tr) "Kaydırma ve dokunmada hafif titreşim açık" else "Light vibration on scroll and tap"
                    } else {
                        if (tr) "Kapalı" else "Off"
                    },
                    showSwitch = true,
                    checked = settings.adaptiveHaptics,
                    onClick = {
                        val enable = !settings.adaptiveHaptics
                        vm.setAdaptiveHaptics(enable)
                        if (enable) AdaptiveHaptics.selection(hapticCtx, force = true)
                    }
                )
            }
        } else {
            item {
                ToolsTile(
                    icon = Icons.Filled.Memory,
                    title = if (tr) "Düşük donanım" else "Low-end mode",
                    subtitle = if (settings.lowEndMode) {
                        if (tr) "Açık — sade grafik, blur/gölge kapalı, bellek öncelikli"
                        else "On — simple graphics, blur/shadow off, memory first"
                    } else {
                        if (tr) "Kapalı — tam görsel efektler (normal performans)"
                        else "Off — full visual effects (normal performance)"
                    },
                    showSwitch = true,
                    checked = settings.lowEndMode,
                    onClick = { vm.setLowEnd(!settings.lowEndMode) }
                )
            }
        }
        item {
            ToolsTile(
                icon = Icons.Filled.FontDownload,
                title = if (tr) "Uygulama Fontu" else "App font",
                subtitle = font.label,
                onClick = { fontOpen = true }
            )
        }
        item { Spacer(Modifier.height(12.dp)) }
    }
    if (layoutOpen) {
        GlassChoiceDialog(
            title = if (tr) "Cihaz modu" else "Device mode",
            body = if (tr) {
                "Mobil: dokunmatik vitrin. TV: kumanda ve büyük yazı."
            } else {
                "Mobile: touch showcase. TV: remote and large type."
            },
            options = listOf(
                LayoutMode.MOBILE to if (tr) {
                    "Mobil  ·  Telefon için dokunma ve kompakt liste"
                } else {
                    "Mobile  ·  Touch-friendly compact lists"
                },
                LayoutMode.TV to if (tr) {
                    "TV  ·  Kumanda ve uzak izleme için büyük yazı"
                } else {
                    "TV  ·  Large text and focus for remotes"
                }
            ),
            selected = settings.layoutMode,
            onDismiss = { layoutOpen = false },
            onPick = { vm.setLayoutMode(it); layoutOpen = false }
        )
    }
    if (sleepOpen) {
        GlassChoiceDialog(
            title = if (tr) "Uyku zamanlayıcısı" else "Sleep timer",
            options = listOf(0, 15, 30, 45, 60, 90, 120).map { m ->
                m to if (m == 0) (if (tr) "Kapalı" else "Off") else if (tr) "$m dakika" else "$m minutes"
            },
            selected = settings.sleepMinutes,
            onDismiss = { sleepOpen = false },
            onPick = { vm.setSleepMinutes(it); sleepOpen = false }
        )
    }
    if (fontOpen) {
        GlassChoiceDialog(
            title = if (tr) "Uygulama Fontu" else "App font",
            body = if (tr) "Tüm uygulama arayüzü için font seçimi." else "Font choice for the entire app interface.",
            options = appFontOptions.map { it.key to "${it.label}  ·  ${it.preview}" },
            selected = settings.appFontKey,
            onDismiss = { fontOpen = false },
            onPick = { vm.setAppFontKey(it); fontOpen = false }
        )
    }
}

@Composable
private fun EpgSettingsPage(
    vm: RayViewModel,
    settings: RaySettings,
    sources: List<SourceEntity>,
    tr: Boolean,
    onBack: () -> Unit,
    onSources: () -> Unit
) {
    val stats by vm.epgStats.collectAsState()
    val sync by vm.catalog.sync.collectAsState()
    val xtream = sources.any { it.kind.equals("XTREAM", true) }
    var sourceOpen by remember { mutableStateOf(false) }
    var daysOpen by remember { mutableStateOf(false) }
    var clockOpen by remember { mutableStateOf(false) }
    var offsetOpen by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { vm.refreshEpgStats() }
    val loading = sync.running
    val statusSub = when {
        loading -> if (tr) "Yükleniyor…" else "Loading…"
        stats.channels == 0 && stats.programmes == 0 && stats.backupChannels == 0 -> if (tr) "Rehber yüklenmedi" else "Guide not loaded"
        tr -> {
            val primary = "${stats.channels} kanal · ${stats.programmes} program"
            if (stats.backupChannels > 0) "$primary · yedek rehber ${stats.backupChannels} kanal"
            else primary
        }
        else -> {
            val primary = "${stats.channels} channels · ${stats.programmes} programmes"
            if (stats.backupChannels > 0) "$primary · backup guide ${stats.backupChannels} channels"
            else primary
        }
    }
    val sourceSub = when (settings.epgSourceMode) {
        EpgSourceMode.AUTO -> if (tr) "Otomatik: Xtream + EPGShare01 yedek" else "Auto: Xtream + EPGShare01 backup"
        EpgSourceMode.XTREAM -> if (tr) "Sadece Xtream sunucusu" else "Xtream server only"
        EpgSourceMode.XMLTV -> if (tr) "XMLTV / EPGShare01 (Xtream yok)" else "XMLTV / EPGShare01 (no Xtream)"
    }
    val daysSub = if (settings.epgRefreshDays <= 0) {
        if (tr) "Otomatik yenileme kapalı (yalnızca bir kez)" else "Auto refresh off (once only)"
    } else if (tr) "${settings.epgRefreshDays} gün" else "${settings.epgRefreshDays} days"
    val clockSub = if (settings.epg24h) (if (tr) "24 saat" else "24-hour") else (if (tr) "12 saat (AM/PM)" else "12-hour (AM/PM)")
    val offsetSub = if (settings.epgOffsetHours == 0) {
        if (tr) "Ofset yok (UTC±0)" else "No offset (UTC±0)"
    } else if (tr) "%+d saat".format(settings.epgOffsetHours) else "%+d hours".format(settings.epgOffsetHours)
    val on = settings.epgEnabled
    val g = LocalGlass.current
    LazyColumn(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item { ToolsHeader(if (tr) "EPG ayarları" else "EPG settings", null, onBack) }
        item {
            ToolsTile(
                icon = Icons.Filled.LiveTv,
                title = if (tr) "EPG'yi aç" else "Enable EPG",
                subtitle = if (on) {
                    if (tr) "TV rehberi yenileniyor; canlı program bilgisi gösterilir."
                    else "TV guide refreshes; live programme info is shown."
                } else {
                    if (tr) "EPG kapalı. İndirme yapılmaz, canlı program bilgisi gizlenir."
                    else "EPG is off. Nothing is downloaded and live programme info is hidden."
                },
                showSwitch = true,
                checked = on,
                onClick = { vm.setEpgEnabled(!on) }
            )
        }
        item {
            Box {
                Column(Modifier.alpha(if (on) 1f else 0.38f), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    ToolsTile(
                        icon = Icons.Filled.LiveTv,
                        title = if (tr) "TV rehberi durumu" else "TV guide status",
                        subtitle = statusSub,
                        onClick = { if (on) vm.refreshGuide() }
                    )
                    if (xtream) {
                        ToolsTile(
                            icon = when (settings.epgSourceMode) {
                                EpgSourceMode.AUTO -> Icons.Filled.AutoAwesome
                                EpgSourceMode.XTREAM -> Icons.Filled.Dns
                                EpgSourceMode.XMLTV -> Icons.Filled.CloudDownload
                            },
                            title = if (tr) "EPG Kaynağı" else "EPG source",
                            subtitle = sourceSub,
                            onClick = { if (on) sourceOpen = true }
                        )
                    }
                    ToolsTile(
                        icon = Icons.Filled.Update,
                        title = if (tr) "Rehber güncelleme sıklığı" else "Guide refresh frequency",
                        subtitle = daysSub,
                        onClick = { if (on) daysOpen = true }
                    )
                    ToolsTile(
                        icon = Icons.Filled.Schedule,
                        title = if (tr) "Saat formatı" else "Time format",
                        subtitle = clockSub,
                        onClick = { if (on) clockOpen = true }
                    )
                    ToolsTile(
                        icon = Icons.Filled.Schedule,
                        title = if (tr) "EPG zaman ofseti" else "EPG time offset",
                        subtitle = offsetSub,
                        onClick = { if (on) offsetOpen = true }
                    )
                    ToolsTile(
                        icon = Icons.Filled.Tune,
                        title = if (tr) "EPG kaynaklarını yönet" else "Manage EPG sources",
                        subtitle = if (tr) "XMLTV URL ve kanal eşleştirme" else "XMLTV URL and channel matching",
                        onClick = { if (on) onSources() }
                    )
                }
                if (!on) {
                    Box(
                        Modifier
                            .align(Alignment.TopCenter)
                            .padding(top = 6.dp)
                            .clip(RoundedCornerShape(999.dp))
                            .background(g.accent.copy(alpha = 0.16f))
                            .border(1.dp, g.accent.copy(alpha = 0.45f), RoundedCornerShape(999.dp))
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text(
                            if (tr) "EPG KAPALI" else "EPG OFF",
                            color = g.accent,
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }
                }
            }
        }
        item { Spacer(Modifier.height(12.dp)) }
    }
    if (sourceOpen) {
        GlassChoiceDialog(
            title = if (tr) "EPG Kaynağı" else "EPG source",
            body = if (tr) "Canlı TV rehberi nereden gelsin?" else "Where should the live TV guide come from?",
            options = listOf(
                EpgSourceMode.AUTO to if (tr) "Otomatik: Xtream + EPGShare01 yedek" else "Auto: Xtream + EPGShare01 backup",
                EpgSourceMode.XTREAM to if (tr) "Sadece Xtream sunucusu" else "Xtream server only",
                EpgSourceMode.XMLTV to if (tr) "Yalnızca XMLTV / EPGShare01 yedek" else "XMLTV / EPGShare01 backup only"
            ),
            selected = settings.epgSourceMode,
            onDismiss = { sourceOpen = false },
            onPick = { vm.setEpgSourceMode(it); sourceOpen = false }
        )
    }
    if (daysOpen) {
        GlassChoiceDialog(
            title = if (tr) "Rehber güncelleme sıklığı" else "Guide refresh frequency",
            options = listOf(0, 1, 2, 3, 7).map {
                it to if (it == 0) {
                    if (tr) "Otomatik yenileme kapalı (yalnızca bir kez)" else "Auto refresh off (once only)"
                } else if (tr) "$it gün" else "$it days"
            },
            selected = settings.epgRefreshDays,
            onDismiss = { daysOpen = false },
            onPick = { vm.setEpgRefreshDays(it); daysOpen = false }
        )
    }
    if (clockOpen) {
        GlassChoiceDialog(
            title = if (tr) "Saat formatı" else "Time format",
            options = listOf(
                true to if (tr) "24 saat" else "24-hour",
                false to if (tr) "12 saat (AM/PM)" else "12-hour (AM/PM)"
            ),
            selected = settings.epg24h,
            onDismiss = { clockOpen = false },
            onPick = { vm.setEpg24h(it); clockOpen = false }
        )
    }
    if (offsetOpen) {
        GlassChoiceDialog(
            title = if (tr) "EPG zaman ofseti" else "EPG time offset",
            options = (-12..12).map {
                it to if (it == 0) {
                    if (tr) "Ofset yok (UTC±0)" else "No offset (UTC±0)"
                } else if (tr) "%+d saat".format(it) else "%+d hours".format(it)
            },
            selected = settings.epgOffsetHours,
            onDismiss = { offsetOpen = false },
            onPick = { vm.setEpgOffset(it); offsetOpen = false }
        )
    }
}

@Composable
private fun EpgSourcesPage(
    vm: RayViewModel,
    settings: RaySettings,
    epgSources: List<EpgSourceEntity>,
    tr: Boolean,
    onBack: () -> Unit
) {
    var name by remember { mutableStateOf("") }
    var url by remember { mutableStateOf("") }
    var catchupOpen by remember { mutableStateOf(false) }
    var fineOpen by remember { mutableStateOf(false) }
    var tpl by remember { mutableStateOf(settings.catchupTemplate) }
    LazyColumn(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item {
            ToolsHeader(
                if (tr) "EPG kaynaklarını yönet" else "Manage EPG sources",
                if (tr) "XMLTV URL ve kanal eşleştirme" else "XMLTV URL and channel matching",
                onBack
            )
        }
        epgSources.forEach { src ->
            item(key = src.id) {
                ToolsTile(
                    icon = Icons.Filled.CloudDownload,
                    title = src.name,
                    subtitle = if (tr) "Silmek için seçin · ${src.url}" else "Select to remove · ${src.url}",
                    onClick = { vm.removeEpgSource(src.id) }
                )
            }
        }
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                GlassField(if (tr) "XMLTV adı" else "XMLTV name", name) { name = it }
                GlassField("XMLTV URL", url) { url = it }
                GlassButton(if (tr) "XMLTV ekle" else "Add XMLTV") { vm.addEpgSource(name, url) }
                Text(
                    if (tr) "Yedek EPG (EPGShare01) — birincil rehber boşsa kanal adına göre doldurulur"
                    else "Backup EPG (EPGShare01) — fills by channel name when primary is empty",
                    color = LocalGlass.current.muted,
                    style = MaterialTheme.typography.bodySmall
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    GlobalEpg.countries.take(5).forEach { (code, _) ->
                        GlassButton(code) { vm.addGlobalEpg(code) }
                    }
                }
            }
        }
        item {
            ToolsTile(
                icon = Icons.Filled.AutoAwesome,
                title = if (tr) "Akıllı EPG eşle" else "Smart EPG match",
                subtitle = if (tr) "Kanalları rehberle otomatik eşleştir" else "Match channels to the guide automatically",
                onClick = { vm.autoMatchEpg() }
            )
        }
        item {
            ToolsTile(
                icon = Icons.Filled.Schedule,
                title = if (tr) "İnce ofset" else "Fine offset",
                subtitle = if (tr) "${settings.epgOffsetMinutes} dakika" else "${settings.epgOffsetMinutes} minutes",
                onClick = { fineOpen = true }
            )
        }
        item {
            ToolsTile(
                icon = Icons.Filled.Schedule,
                title = if (tr) "Catch-up saat dilimi" else "Catch-up timezone",
                subtitle = if (settings.catchupTimezoneDevice) (if (tr) "Cihaz saati" else "Device time") else "UTC",
                showSwitch = true,
                checked = settings.catchupTimezoneDevice,
                onClick = { vm.setCatchupTz(!settings.catchupTimezoneDevice) }
            )
        }
        item {
            ToolsTile(
                icon = Icons.Filled.Tune,
                title = if (tr) "EPG catch-up (panel şablonu)" else "EPG catch-up template",
                subtitle = when (settings.catchupPreset) {
                    CatchupPreset.OFF -> if (tr) "Kapalı" else "Off"
                    CatchupPreset.XTREAM_PATH -> if (tr) "Klasik timeshift yolu (çoğu Xtream)" else "Classic timeshift path (most Xtream)"
                    CatchupPreset.TIMESHIFT_PHP -> if (tr) "timeshift.php sorgusu" else "timeshift.php query"
                    CatchupPreset.CUSTOM -> if (tr) "Özel şablon" else "Custom template"
                },
                onClick = { catchupOpen = true }
            )
        }
        if (settings.catchupPreset == CatchupPreset.CUSTOM) {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    GlassField("{server}/timeshift/…", tpl) { tpl = it }
                    GlassButton(if (tr) "Şablonu kaydet" else "Save template") { vm.setCatchupTemplate(tpl) }
                }
            }
        }
        item {
            ToolsTile(
                icon = Icons.Filled.Update,
                title = if (tr) "Listeyi güncelle" else "Refresh guide",
                subtitle = if (tr) "Rehberi şimdi içe aktar" else "Import the guide now",
                onClick = { vm.refreshGuide() }
            )
        }
        item { Spacer(Modifier.height(12.dp)) }
    }
    if (catchupOpen) {
        GlassChoiceDialog(
            title = if (tr) "EPG catch-up (panel şablonu)" else "EPG catch-up template",
            options = listOf(
                CatchupPreset.OFF to if (tr) "Kapalı" else "Off",
                CatchupPreset.XTREAM_PATH to if (tr) "Klasik timeshift yolu (çoğu Xtream)" else "Classic timeshift path (most Xtream)",
                CatchupPreset.TIMESHIFT_PHP to if (tr) "timeshift.php sorgusu" else "timeshift.php query",
                CatchupPreset.CUSTOM to if (tr) "Özel şablon" else "Custom template"
            ),
            selected = settings.catchupPreset,
            onDismiss = { catchupOpen = false },
            onPick = { vm.setCatchupPreset(it); catchupOpen = false }
        )
    }
    if (fineOpen) {
        GlassChoiceDialog(
            title = if (tr) "İnce ofset" else "Fine offset",
            options = listOf(-30, -15, 0, 15, 30).map { it to if (tr) "$it dk" else "$it min" },
            selected = settings.epgOffsetMinutes,
            onDismiss = { fineOpen = false },
            onPick = { vm.setEpgOffsetMinutes(it); fineOpen = false }
        )
    }
}

@Composable
private fun SpeedTestPage(sources: List<SourceEntity>, tr: Boolean, onBack: () -> Unit) {
    val g = LocalGlass.current
    val scope = rememberCoroutineScope()
    var testing by remember { mutableStateOf(false) }
    var speed by remember { mutableFloatStateOf(0f) }
    var result by remember { mutableStateOf<Float?>(null) }
    var ping by remember { mutableStateOf<Int?>(null) }
    var iptvPing by remember { mutableStateOf<Int?>(null) }
    var iptvHost by remember { mutableStateOf<String?>(null) }
    var error by remember { mutableStateOf("") }
    val analysis = result?.let { speedAnalysis(it, tr) }
    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .background(Color(0xFF0D1117))
            .padding(bottom = 16.dp)
    ) {
        ToolsHeader(if (tr) "Hız Testi" else "Speed test", if (tr) "İnternet hızını ölç" else "Measure internet speed", onBack)
        Box(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(24.dp))
                .background(Color(0xE6141821))
                .border(1.dp, Color.White.copy(alpha = 0.12f), RoundedCornerShape(24.dp))
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            SpeedGauge(if (testing) speed else (result ?: 0f), testing)
        }
        Spacer(Modifier.height(18.dp))
        GlassButton(
            if (testing) (if (tr) "Test Ediliyor..." else "Testing...")
            else if (tr) "Testi Başlat" else "Start test"
        ) {
            if (testing) return@GlassButton
            testing = true
            error = ""
            result = null
            speed = 0f
            scope.launch {
                runCatching {
                    val raw = sources.firstOrNull { it.enabled }?.baseUrl.orEmpty()
                    iptvHost = raw.takeIf { it.isNotBlank() }?.let { runCatching { URI(it).host }.getOrNull() ?: it }
                    ping = withContext(Dispatchers.IO) { pingHost("1.1.1.1", 443) }
                    iptvPing = iptvHost?.let { h -> withContext(Dispatchers.IO) { pingHost(h, 80) } }
                    val mbps = withContext(Dispatchers.IO) { downloadMbps { live -> speed = live } }
                    result = mbps
                    speed = mbps
                }.onFailure {
                    error = it.message.orEmpty().ifBlank { if (tr) "Test başarısız oldu" else "Test failed" }
                }
                testing = false
            }
        }
        if (error.isNotBlank()) {
            Spacer(Modifier.height(12.dp))
            Text(error, color = Color(0xFFFF5252), style = MaterialTheme.typography.bodyMedium)
        }
        result?.let { mbps ->
            Spacer(Modifier.height(18.dp))
            Text(if (tr) "İndirme Hızı" else "Download speed", color = g.muted, style = MaterialTheme.typography.bodySmall)
            Text(String.format("%.1f Mbps", mbps), color = g.text, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.ExtraBold)
            analysis?.let { a ->
                Spacer(Modifier.height(6.dp))
                Text(a.first, color = Color(a.second), fontWeight = FontWeight.Bold)
                Text(a.third, color = g.muted, style = MaterialTheme.typography.bodySmall)
            }
        }
        Spacer(Modifier.height(16.dp))
        SpeedCard(if (tr) "IPTV Sunucu & Hat Kalite Analizi" else "IPTV server & line quality") {
            Text(
                iptvHost?.let { if (tr) "Sunucu: $it" else "Server: $it" }
                    ?: if (tr) "Aktif IPTV sunucusu algılanmadı" else "No active IPTV server detected",
                color = Color.White.copy(alpha = 0.7f),
                style = MaterialTheme.typography.bodySmall
            )
            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                MetricChip(if (tr) "Sunucu gecikmesi" else "Server latency", iptvPing?.let { "$it ms" } ?: "—", Modifier.weight(1f))
                MetricChip(if (tr) "Önerilen kalite" else "Suggested quality", suggestedQuality(result, tr), Modifier.weight(1f))
            }
        }
        Spacer(Modifier.height(12.dp))
        SpeedCard(if (tr) "Hız Sınırları" else "Speed thresholds") {
            ThresholdLine(Color(0xFFFF5252), if (tr) "Çok Yavaş — donmalar ve takılmalar yaşayabilirsiniz" else "Very slow — you may see freezes and stalls")
            ThresholdLine(Color(0xFFFFC107), if (tr) "Sınırda — HD yayınlarda anlık takılmalar olabilir" else "Borderline — HD streams may stall briefly")
            ThresholdLine(Color(0xFF4CAF50), if (tr) "Harika — kesintisiz yayın izleyebilirsiniz" else "Excellent — you can watch without interruption")
        }
        ping?.let {
            Spacer(Modifier.height(8.dp))
            Text(if (tr) "Genel ping: $it ms" else "General ping: $it ms", color = g.muted, style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
private fun SpeedGauge(speed: Float, testing: Boolean) {
    val accent = LocalGlass.current.accent
    Box(Modifier.size(220.dp), contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val stroke = Stroke(width = 16.dp.toPx(), cap = StrokeCap.Round)
            drawArc(Color.White.copy(alpha = 0.12f), 150f, 240f, false, style = stroke)
            drawArc(accent, 150f, 240f * (speed / 100f).coerceIn(0f, 1f), false, style = stroke)
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(String.format("%.1f", speed), color = Color.White, style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.ExtraBold)
            Text("Mbps", color = Color.White.copy(alpha = 0.7f))
            if (testing) {
                Spacer(Modifier.height(4.dp))
                Text(if (testing) "…" else "", color = accent, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun SpeedCard(title: String, content: @Composable () -> Unit) {
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0xE6141821))
            .border(1.dp, Color.White.copy(alpha = 0.14f), RoundedCornerShape(16.dp))
            .padding(16.dp)
    ) {
        Text(title, color = Color.White, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(10.dp))
        content()
    }
}

@Composable
private fun MetricChip(title: String, value: String, modifier: Modifier = Modifier) {
    Column(
        modifier
            .clip(RoundedCornerShape(12.dp))
            .background(Color.White.copy(alpha = 0.05f))
            .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(12.dp))
            .padding(12.dp)
    ) {
        Text(title, color = Color.White.copy(alpha = 0.7f), style = MaterialTheme.typography.labelLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Spacer(Modifier.height(4.dp))
        Text(value, color = Color.White, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun ThresholdLine(color: Color, text: String) {
    Row(Modifier.padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(10.dp).clip(CircleShape).background(color))
        Spacer(Modifier.width(8.dp))
        Text(text, color = Color.White.copy(alpha = 0.85f), style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
private fun ToolsHeader(
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
private fun ToolsTile(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    showSwitch: Boolean = false,
    checked: Boolean = false
) {
    if (LocalMobileSettingsChrome.current) {
        MobileOptionTile(
            icon = icon,
            title = title,
            subtitle = subtitle,
            onClick = onClick,
            showSwitch = showSwitch,
            checked = checked
        )
        return
    }
    val g = LocalGlass.current
    var focused by remember { mutableStateOf(false) }
    GlassPanel(
        focused = focused,
        radius = 18.dp,
        onClick = onClick,
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
                if (subtitle.isNotBlank()) {
                    Spacer(Modifier.height(2.dp))
                    Text(subtitle, color = g.muted, style = MaterialTheme.typography.bodySmall, maxLines = 2, overflow = TextOverflow.Ellipsis)
                }
            }
            if (showSwitch) {
                Box(
                    Modifier
                        .width(42.dp)
                        .height(24.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (checked) g.accent else Color.White.copy(alpha = 0.18f))
                )
            } else {
                Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, tint = g.muted)
            }
        }
    }
}

private fun sleepSubtitle(untilMs: Long, now: Long, tr: Boolean): String {
    if (untilMs <= 0L || untilMs <= now) return if (tr) "Kapalı" else "Off"
    val min = ((untilMs - now) / 60_000L).coerceAtLeast(1)
    return if (tr) "Yaklaşık $min dk kaldı" else "About $min min left"
}

private fun speedAnalysis(mbps: Float, tr: Boolean): Triple<String, Long, String> = when {
    mbps < 8f -> Triple(
        if (tr) "Çok Düşük Hız" else "Very low speed",
        0xFFFF5252,
        if (tr) "İnternet hızınız çok düşük. Donmalar yaşamanız normaldir. Lütfen ağınızı kontrol edin."
        else "Your internet is very slow. Stalls are expected. Please check your network."
    )
    mbps < 25f -> Triple(
        if (tr) "Sınır Hız" else "Borderline speed",
        0xFFFFC107,
        if (tr) "İnternet hızınız sınırda. HD yayınlarda anlık takılmalar olabilir."
        else "Your internet is borderline. HD streams may stall briefly."
    )
    else -> Triple(
        if (tr) "Harika Hız" else "Excellent speed",
        0xFF4CAF50,
        if (tr) "İnternet hızınız harika. Kesintisiz yayın izleyebilirsiniz."
        else "Your internet is excellent. You can watch without interruption."
    )
}

private fun suggestedQuality(mbps: Float?, tr: Boolean): String {
    val v = mbps ?: return if (tr) "Test bekleniyor" else "Waiting for test"
    return when {
        v < 5f -> "SD"
        v < 15f -> "HD"
        v < 35f -> "FHD"
        else -> "4K"
    }
}

private fun pingHost(host: String, port: Int): Int = runCatching {
    val t = System.nanoTime()
    Socket().use { it.connect(InetSocketAddress(host, port), 3000) }
    ((System.nanoTime() - t) / 1_000_000L).toInt()
}.getOrDefault(0)

private fun downloadMbps(onLive: (Float) -> Unit): Float {
    val conn = (URL("https://speed.cloudflare.com/__down?bytes=25000000").openConnection() as HttpURLConnection).apply {
        connectTimeout = 8000
        readTimeout = 15000
        requestMethod = "GET"
        setRequestProperty("User-Agent", "RayIPTVPlayer/1.1")
    }
    val started = System.nanoTime()
    var bytes = 0L
    conn.inputStream.use { input ->
        val buf = ByteArray(64 * 1024)
        val deadline = started + 7_000_000_000L
        while (System.nanoTime() < deadline) {
            val n = input.read(buf)
            if (n <= 0) break
            bytes += n
            val sec = (System.nanoTime() - started) / 1_000_000_000.0
            if (sec > 0.2) onLive((((bytes * 8.0) / (sec * 1_000_000.0)).toFloat() * 10).roundToInt() / 10f)
        }
    }
    conn.disconnect()
    val sec = (System.nanoTime() - started) / 1_000_000_000.0
    val mbps = if (sec > 0.2 && bytes > 0) ((bytes * 8.0) / (sec * 1_000_000.0)).toFloat() else 0.5f
    return (mbps.coerceAtLeast(0.5f) * 10).roundToInt() / 10f
}
