package com.ray.iptv.ui.screens.settings

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.window.Dialog
import com.ray.iptv.ui.input.rayClickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.AdminPanelSettings
import com.ray.iptv.ui.admin.AdminHost
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.CloudQueue
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.DashboardCustomize
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.DownloadForOffline
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Login
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.PlayCircleFilled
import androidx.compose.material.icons.filled.PlaylistPlay
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.SettingsRemote
import androidx.compose.material.icons.automirrored.filled.PlaylistPlay
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Inbox
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Segment
import androidx.compose.material.icons.filled.StayCurrentPortrait
import androidx.compose.material.icons.filled.RadioButtonChecked
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Speed
import com.ray.iptv.ui.mobile.MobileBadge
import com.ray.iptv.ui.mobile.MobileCyan
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material.icons.filled.ViewSidebar
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Icon
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.ray.iptv.data.local.CategoryEntity
import com.ray.iptv.data.local.DownloadEntity
import com.ray.iptv.data.local.EpgSourceEntity
import com.ray.iptv.data.local.GroupEntity
import com.ray.iptv.data.local.ProfileEntity
import com.ray.iptv.data.local.SourceEntity
import com.ray.iptv.data.repo.AppLang
import com.ray.iptv.data.repo.AspectMode
import com.ray.iptv.data.repo.CatchupPreset
import com.ray.iptv.data.repo.EpgSourceMode
import com.ray.iptv.data.repo.GlassStyle
import com.ray.iptv.data.repo.LayoutMode
import com.ray.iptv.data.repo.RaySettings
import com.ray.iptv.data.repo.StartupScreen
import com.ray.iptv.data.repo.StreamFormat
import com.ray.iptv.data.repo.UserAgentPreset
import com.ray.iptv.data.repo.VodInfoEngine
import com.ray.iptv.ui.RayViewModel
import com.ray.iptv.ui.components.GlassButton
import com.ray.iptv.ui.glass.GlassPanel
import com.ray.iptv.ui.i18n.Copy
import com.ray.iptv.ui.mobile.MobileSettingsTopBar
import com.ray.iptv.ui.motion.RaySwitch
import com.ray.iptv.ui.screens.onboarding.GlassField
import com.ray.iptv.ui.theme.LocalGlass
import com.ray.iptv.ui.theme.glassStylesForLayout
import com.ray.iptv.ui.theme.title
import com.ray.iptv.ui.input.rayFocusRequester
import com.ray.iptv.ui.input.tryFocus
import kotlinx.coroutines.delay

private enum class SettingsPage {
    HUB, PLAYLIST, CHANNELS, HOME, RAIL, PLAYBACK, PERF, KEYS, OTHER, LANGUAGE, THEME, PROFILES, BACKUP, DOWNLOADS, ABOUT, ACCOUNT, ADMIN
}

@OptIn(androidx.compose.ui.ExperimentalComposeUiApi::class)
@Composable
fun SettingsScreen(
    vm: RayViewModel,
    settings: RaySettings,
    sources: List<SourceEntity>,
    profiles: List<ProfileEntity>,
    epgSources: List<EpgSourceEntity>,
    groups: List<GroupEntity>,
    liveCats: List<CategoryEntity>,
    downloads: List<DownloadEntity>,
    copy: Copy,
    railExpanded: Boolean = false,
    contentFocusTrigger: Long = 0L,
    onExpandRail: () -> Unit = {},
    onExit: () -> Unit = {}
) {
    var page by remember { mutableStateOf(SettingsPage.HUB) }
    val hubListState = rememberLazyListState()
    val hubGridState = rememberLazyGridState()
    val focus = LocalFocusManager.current
    val hubFocusRequester = remember { FocusRequester() }
    val subPageFocusRequester = remember { FocusRequester() }
    val generalRequesters = remember { List(30) { FocusRequester() } }
    var lastOpenedIndex by remember { mutableStateOf(0) }
    val tr = settings.lang == AppLang.TR
    val mobile = settings.layoutMode == LayoutMode.MOBILE

    LaunchedEffect(page, railExpanded, contentFocusTrigger) {
        if (!railExpanded && !mobile) {
            if (page == SettingsPage.HUB) {
                val target = generalRequesters.getOrNull(lastOpenedIndex) ?: hubFocusRequester
                delay(20)
                repeat(30) {
                    delay(35)
                    if (target.tryFocus() || hubFocusRequester.tryFocus() || generalRequesters.firstOrNull()?.tryFocus() == true) return@LaunchedEffect
                }
            } else {
                delay(20)
                repeat(30) {
                    delay(35)
                    if (subPageFocusRequester.tryFocus()) return@LaunchedEffect
                }
            }
        }
    }



    BackHandler {
        if (page != SettingsPage.HUB) {
            page = SettingsPage.HUB
        } else {
            onExpandRail()
            focus.moveFocus(FocusDirection.Left)
        }
    }
    val titles = mapOf(
        SettingsPage.PLAYLIST to if (tr) "Liste Yönetimi" else "Playlist Manager",
        SettingsPage.CHANNELS to if (tr) "Kanal Kategori Düzeni" else "Channel layout",
        SettingsPage.HOME to if (tr) "Ana Ekran Ayarları" else "Home settings",
        SettingsPage.RAIL to if (tr) "Rail Görünümü" else "Rail",
        SettingsPage.PLAYBACK to if (tr) "Oynatma Ayarları" else "Playback",
        SettingsPage.PERF to if (tr) "Performans" else "Performance",
        SettingsPage.KEYS to if (tr) "Kumanda Tuş Atama" else "Remote keys",
        SettingsPage.OTHER to if (tr) "Diğer Araçlar" else "Other tools",
        SettingsPage.PROFILES to if (tr) "Profiller" else "Profiles",
        SettingsPage.BACKUP to if (tr) "Yedekleme" else "Backup",
        SettingsPage.DOWNLOADS to if (tr) "İndirilenler" else "Downloads",
        SettingsPage.ABOUT to if (tr) "Hakkında" else "About",
        SettingsPage.ACCOUNT to if (tr) "Xtream Hesap Bilgileri" else "Xtream account",
        SettingsPage.ADMIN to if (tr) "Admin Paneli" else "Admin Panel"
    )
    val onLeftFromHub: () -> Unit = {
        onExpandRail()
        focus.moveFocus(FocusDirection.Left)
    }
    CompositionLocalProvider(LocalMobileSettingsChrome provides mobile) {
        Column(
            Modifier
                .fillMaxSize()
                .padding(top = if (mobile) 0.dp else 4.dp, start = if (mobile) 0.dp else 12.dp, end = if (mobile) 0.dp else 16.dp, bottom = if (mobile) 0.dp else 8.dp)
        ) {
        if (mobile) {
            MobileSettingsTopBar(
                title = if (page == SettingsPage.HUB) {
                    if (tr) "Ayarlar" else "Settings"
                } else {
                    titles[page].orEmpty().ifBlank { if (tr) "Ayarlar" else "Settings" }
                },
                onBack = {
                    if (page == SettingsPage.HUB) onExit() else page = SettingsPage.HUB
                }
            )
            Spacer(Modifier.height(8.dp))
        } else if (page != SettingsPage.HUB && page != SettingsPage.CHANNELS && page != SettingsPage.PLAYBACK && page != SettingsPage.OTHER && page != SettingsPage.ABOUT) {
            Row(
                modifier = Modifier.focusProperties { left = FocusRequester.Cancel },
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                GlassButton("←", focusRequester = subPageFocusRequester) { page = SettingsPage.HUB }
                Text(titles[page].orEmpty(), color = LocalGlass.current.text, style = MaterialTheme.typography.headlineMedium)
            }
            Spacer(Modifier.height(12.dp))
        }
        Box(
            Modifier
                .fillMaxWidth()
                .weight(1f)
                .then(
                    if (!mobile && page != SettingsPage.HUB) {
                        Modifier.focusProperties { left = FocusRequester.Cancel }
                    } else Modifier
                )
        ) {
            RaySwitch(
                page,
                Modifier
                    .fillMaxSize()
                    .then(if (mobile && page != SettingsPage.HUB) Modifier.padding(horizontal = 14.dp) else Modifier),
                effect = if (mobile) settings.pageTransitionEffect else null
            ) { p ->
                when (p) {
                    SettingsPage.HUB -> Hub(
                        tr = tr,
                        vm = vm,
                        settings = settings,
                        mobile = mobile,
                        listState = hubListState,
                        gridState = hubGridState,
                        requesters = generalRequesters,
                        hubFocusRequester = hubFocusRequester,
                        onLeftToRail = onLeftFromHub,
                        onOpen = { index, targetPage ->
                            lastOpenedIndex = index
                            page = targetPage
                        }
                    )
                    SettingsPage.PLAYLIST -> PlaylistPage(vm, settings, sources, subPageFocusRequester) { page = SettingsPage.HUB }
                    SettingsPage.CHANNELS -> ChannelLayoutRoot(vm, settings, liveCats, tr, subPageFocusRequester) { page = SettingsPage.HUB }
                    SettingsPage.HOME -> HomePage(vm, settings, copy, tr)
                    SettingsPage.RAIL -> RailPage(vm, settings, copy, tr)
                    SettingsPage.PLAYBACK -> PlaybackSettingsRoot(vm, settings, tr, subPageFocusRequester) { page = SettingsPage.HUB }
                    SettingsPage.PERF -> PerfPage(vm, settings, tr)
                    SettingsPage.KEYS -> KeysPage(vm, settings, tr)
                    SettingsPage.OTHER -> OtherToolsRoot(vm, settings, epgSources, sources, tr, subPageFocusRequester) { page = SettingsPage.HUB }
                    SettingsPage.LANGUAGE -> Hub(tr, vm, settings, mobile, hubListState, hubGridState, generalRequesters, hubFocusRequester, onLeftToRail = onLeftFromHub) { idx, it -> lastOpenedIndex = idx; page = it }
                    SettingsPage.THEME -> Hub(tr, vm, settings, mobile, hubListState, hubGridState, generalRequesters, hubFocusRequester, onLeftToRail = onLeftFromHub) { idx, it -> lastOpenedIndex = idx; page = it }
                    SettingsPage.PROFILES -> ProfilesPage(vm, settings, profiles, tr, subPageFocusRequester)
                    SettingsPage.BACKUP -> BackupPage(vm, settings, tr, subPageFocusRequester) { page = SettingsPage.HUB }
                    SettingsPage.DOWNLOADS -> DownloadsPage(vm, downloads, tr, subPageFocusRequester)
                    SettingsPage.ABOUT -> AboutRoot(vm, tr, subPageFocusRequester) { page = SettingsPage.HUB }
                    SettingsPage.ACCOUNT -> AccountPage(vm, sources, settings, tr)
                    SettingsPage.ADMIN -> AdminHost(vm, tr) { page = SettingsPage.HUB }
                }
            }
        }
    }
    }
}

@Composable
private fun Hub(
    tr: Boolean,
    vm: RayViewModel,
    settings: RaySettings,
    mobile: Boolean,
    listState: LazyListState,
    gridState: LazyGridState,
    requesters: List<FocusRequester> = emptyList(),
    hubFocusRequester: FocusRequester? = null,
    onLeftToRail: (() -> Unit)? = null,
    onOpen: (Int, SettingsPage) -> Unit
) {
    var wipe by remember { mutableStateOf(false) }
    var themeOpen by remember { mutableStateOf(false) }
    var langOpen by remember { mutableStateOf(false) }
    var refreshOpen by remember { mutableStateOf(false) }
    val refreshLabel = when (settings.autoRefreshHours) {
        0 -> if (tr) "Otomatik yenileme kapalı" else "Auto refresh off"
        2 -> if (tr) "2 saatte bir" else "Every 2 hours"
        24 -> if (tr) "Günde bir" else "Once a day"
        48 -> if (tr) "2 günde bir" else "Every 2 days"
        72 -> if (tr) "3 günde bir" else "Every 3 days"
        168 -> if (tr) "Haftada bir" else "Once a week"
        else -> if (tr) "${settings.autoRefreshHours} saatte bir" else "Every ${settings.autoRefreshHours}h"
    }
    val general = buildList {
        add(Triple(Icons.Filled.PlaylistPlay, if (tr) "Playlist Listesi" else "Playlist list", if (tr) "Kaynağı görüntüle veya değiştir" else "View or change the source") to { onOpen(0, SettingsPage.PLAYLIST) })
        add(Triple(Icons.Filled.Tune, if (tr) "Kanal Kategori Düzeni" else "Channel & Category Layout", if (tr) "Kategori gizleme ve canlı kanal düzeni (sıralama / çıkarma) tek noktada" else "Hide categories and edit the live list in one place") to { onOpen(1, SettingsPage.CHANNELS) })
        add(Triple(Icons.Filled.DashboardCustomize, if (tr) "Ana Ekran Ayarları" else "Home screen settings", if (tr) "Kart sırası, karışık canlı TV ve sıradaki maçlar" else "Card order, mixed live TV and upcoming matches") to { onOpen(2, SettingsPage.HOME) })
        if (!mobile) {
            add(Triple(Icons.Filled.ViewSidebar, if (tr) "Rail Görünümü" else "Rail View", if (tr) "Sol menüde (railde) hangi sekmelerin görüneceğini seçin" else "Choose which tabs appear on the left menu (rail)") to { onOpen(3, SettingsPage.RAIL) })
        }
        add(Triple(Icons.Filled.PlayCircleFilled, if (tr) "Oynatma Ayarları" else "Playback Settings", if (tr) "Oynatıcı motoru, donanım hızlandırma, video kod çözücü ve düşük gecikme buffer" else "Player engine, hardware decode, video decoder and low-latency buffer") to { onOpen(4, SettingsPage.PLAYBACK) })
        add(Triple(Icons.Filled.Speed, if (tr) "Performans" else "Performance", if (tr) "Görsel önbelleğini boşaltır, çöp toplayıcıyı çalıştırır" else "Clears the image cache and runs the garbage collector") to { onOpen(5, SettingsPage.PERF) })
        if (!mobile) {
            add(Triple(Icons.Filled.SettingsRemote, if (tr) "Kumanda Tuş Atama" else "Remote Key Mapping", if (tr) "Kumanda üzerindeki boş veya özel tuşlara hızlı eylemler atayın" else "Assign quick actions to unused or custom remote keys") to { onOpen(6, SettingsPage.KEYS) })
        }
        add(Triple(Icons.Filled.CloudDownload, if (tr) "İçerikleri Yenile" else "Refresh content", refreshLabel) to { refreshOpen = true })
        add(Triple(Icons.Filled.Palette, if (tr) "Tema" else "Theme", settings.glass.title(tr)) to { themeOpen = true })
        add(Triple(Icons.Filled.Build, if (tr) "Diğer Araçlar" else "Other Tools", if (tr) "Uyku zamanlayıcısı, EPG, tema, hız testi, titreşim ve font" else "Sleep timer, EPG, theme, speed test, haptics and font") to { onOpen(9, SettingsPage.OTHER) })
        add(Triple(Icons.Filled.Language, if (tr) "Uygulama Dili" else "App language", settings.lang.nativeName) to { langOpen = true })
        add(Triple(Icons.Filled.People, if (tr) "Profiller" else "Profiles", if (tr) "Her kullanıcı için ayrı tercihler" else "Separate preferences for each user") to { onOpen(11, SettingsPage.PROFILES) })
        add(Triple(Icons.Filled.CloudSync, if (tr) "Yedekleme" else "Backup", if (tr) "Yerel dosya yedekleme" else "Local file backup") to { onOpen(12, SettingsPage.BACKUP) })
        add(Triple(Icons.Filled.DownloadForOffline, if (tr) "İndirilenler" else "Downloads", if (tr) "Telefonuna indirdiğin film ve dizi bölümlerini görüntüle, sil veya çevrimdışı oynat" else "View, delete or play downloaded movies and episodes offline") to { onOpen(13, SettingsPage.DOWNLOADS) })
        add(Triple(Icons.Filled.DeleteForever, if (tr) "Tüm ayarları sil" else "Erase all settings", if (tr) "Playlist, önbellek ve tercihleri sıfırla" else "Reset playlist, cache, and preferences") to { wipe = true })
    }
    var subOpen by remember { mutableStateOf(false) }
    var delAcc by remember { mutableStateOf(false) }
    val ctx = LocalContext.current
    val version = remember {
        runCatching { ctx.packageManager.getPackageInfo(ctx.packageName, 0).versionName }
            .getOrNull().orEmpty().ifBlank { "1.3.0" }
    }
    val account by vm.account.collectAsState()
    val googleSub = when {
        account.signedIn && account.isAdmin -> if (tr) "Admin · ${account.email}" else "Admin · ${account.email}"
        account.signedIn -> account.email
        else -> if (tr) "Google ile oturum aç" else "Sign in with Google"
    }
    val info = buildList {
        if (account.signedIn && account.isAdmin) {
            add(
                Triple(
                    Icons.Filled.AdminPanelSettings,
                    if (tr) "Admin Paneli" else "Admin Panel",
                    if (tr) "Yönetim konsolu, kullanıcılar, hata raporları ve duyurular" else "Management console, users, crashes and push notifications"
                ) to { onOpen(15, SettingsPage.ADMIN) }
            )
        }
        add(Triple(Icons.Filled.Info, if (tr) "Hakkında" else "About", "Ray IPTV Player $version") to { onOpen(16, SettingsPage.ABOUT) })
        add(Triple(Icons.Filled.AccountCircle, if (tr) "Hesap Bilgileri" else "Account info", if (tr) "Xtream abonelik bilgilerini gör" else "View Xtream subscription details") to { onOpen(17, SettingsPage.ACCOUNT) })
        add(Triple(Icons.Filled.VerifiedUser, if (tr) "Abonelik Durumu" else "Subscription Status", if (settings.xtreamStatus.isNotBlank()) settings.xtreamStatus else if (tr) "Lisans ve deneme süresi detayları" else "License and trial period details") to { subOpen = true })
        add(Triple(Icons.Filled.DeleteForever, if (tr) "Hesabımı Sil" else "Delete my account", if (tr) "Hesabınızı ve buluttaki tüm verilerinizi kalıcı olarak siler." else "Permanently deletes your account and all cloud data.") to { delAcc = true })
    }
    if (mobile) {
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize().padding(horizontal = 14.dp),
            contentPadding = PaddingValues(bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item { SettingsSectionLabel(if (tr) "Genel Ayarlar" else "General") }
            items(general.size) { i ->
                val (meta, action) = general[i]
                MobileSettingsTile(meta.first, meta.second, meta.third, action, danger = i == general.lastIndex)
            }
            item { SettingsSectionLabel(if (tr) "Uygulama Bilgileri" else "App info") }
            items(info.size) { i ->
                val (meta, action) = info[i]
                MobileSettingsTile(meta.first, meta.second, meta.third, action, danger = i == info.lastIndex)
            }
        }
    } else {
    LazyVerticalGrid(
        columns = GridCells.Adaptive(280.dp),
        state = gridState,
        modifier = Modifier.fillMaxSize(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item(span = { GridItemSpan(maxLineSpan) }) { SettingsSectionLabel(if (tr) "Genel Ayarlar" else "General") }
        items(general.size) { i ->
            val (meta, action) = general[i]
            HubGlassTile(
                icon = meta.first,
                title = meta.second,
                subtitle = meta.third,
                onClick = action,
                danger = i == general.lastIndex,
                onLeft = if (i % 2 == 0) onLeftToRail else null,
                isTopRow = i < 2,
                focusRequester = requesters.getOrNull(i) ?: if (i == 0) hubFocusRequester else null
            )
        }
        item(span = { GridItemSpan(maxLineSpan) }) { SettingsSectionLabel(if (tr) "Uygulama Bilgileri" else "App info") }
        items(info.size) { i ->
            val (meta, action) = info[i]
            val infoIndex = 15 + i
            HubGlassTile(
                icon = meta.first,
                title = meta.second,
                subtitle = meta.third,
                onClick = action,
                danger = i == info.lastIndex,
                onLeft = if (i % 2 == 0) onLeftToRail else null,
                isTopRow = false,
                focusRequester = requesters.getOrNull(infoIndex)
            )
        }
    }
    }
    if (themeOpen) {
        val original = remember { settings.glass }
        val styles = remember(mobile, original) { glassStylesForLayout(mobile, original) }
        if (mobile) {
            MobileThemePickerDialog(
                tr = tr,
                styles = styles,
                selected = settings.glass,
                preview = { vm.setGlass(it) },
                onDismiss = { vm.setGlass(original); themeOpen = false },
                onPick = { vm.setGlass(it); themeOpen = false }
            )
        } else {
            GlassChoiceDialog(
                title = if (tr) "Tema" else "Theme",
                options = styles.map { it to it.title(tr) },
                selected = settings.glass,
                preview = { vm.setGlass(it) },
                onDismiss = { vm.setGlass(original); themeOpen = false },
                onPick = { vm.setGlass(it); themeOpen = false }
            )
        }
    }
    if (langOpen) {
        GlassChoiceDialog(
            title = if (tr) "Uygulama dili" else "App language",
            options = AppLang.entries.map { it to it.nativeName },
            selected = settings.lang,
            onDismiss = { langOpen = false },
            onPick = { vm.setLang(it); langOpen = false }
        )
    }
    if (refreshOpen) {
        GlassChoiceDialog(
            title = if (tr) "İçerikleri Yenile" else "Refresh content",
            body = if (tr) "İçerikler şimdi yenilenecek. Ayrıca otomatik yenileme sıklığını seçmek ister misiniz?" else "Content will refresh now. Do you also want to pick an auto-refresh interval?",
            options = listOf(
                2 to if (tr) "2 saatte bir yenile" else "Refresh every 2 hours",
                24 to if (tr) "Günde bir yenile" else "Refresh every day",
                48 to if (tr) "2 günde bir yenile" else "Refresh every 2 days",
                72 to if (tr) "3 günde bir yenile" else "Refresh every 3 days",
                168 to if (tr) "Haftada bir yenile" else "Refresh every week",
                0 to if (tr) "Otomatik yenileme kapalı" else "Auto refresh off"
            ),
            selected = settings.autoRefreshHours,
            extraAction = (if (tr) "Sadece şimdi yenile" else "Refresh once now") to { vm.refresh() },
            onDismiss = { refreshOpen = false },
            onPick = {
                vm.setAutoRefreshHours(it)
                vm.refresh()
                refreshOpen = false
            }
        )
    }
    var wipePending by remember { mutableStateOf<String?>(null) }
    if (wipe) {
        GlassChoiceDialog(
            title = if (tr) "Sıfırlama seçenekleri" else "Reset options",
            body = if (tr) "Ne sıfırlansın? Bu işlem geri alınamaz." else "What should be reset? This cannot be undone.",
            options = listOf(
                "history" to if (tr) "İzleme geçmişi" else "Watch history",
                "playlists" to if (tr) "Listeler (playlist)" else "Playlists",
                "everything" to if (tr) "Tümü ve tüm veriler" else "Everything"
            ),
            selected = "",
            onDismiss = { wipe = false },
            onPick = {
                wipePending = it
                wipe = false
            }
        )
    }
    wipePending?.let { choice ->
        val title = when (choice) {
            "history" -> if (tr) "İzleme geçmişi silinsin mi?" else "Clear watch history?"
            "playlists" -> if (tr) "Tüm listeler silinsin mi?" else "Remove all playlists?"
            else -> if (tr) "Tüm ayarlar ve veriler silinsin mi?" else "Erase all settings and data?"
        }
        GlassConfirmDialog(
            title = title,
            body = if (tr) "Bu işlem geri alınamaz." else "This cannot be undone.",
            confirm = if (tr) "Sil" else "Delete",
            cancel = if (tr) "Vazgeç" else "Cancel",
            onDismiss = { wipePending = null },
            onConfirm = {
                when (choice) {
                    "history" -> vm.clearWatchHistory()
                    "playlists" -> vm.clearPlaylistsOnly()
                    else -> vm.resetAllSettings()
                }
                wipePending = null
            }
        )
    }
    if (subOpen) {
        val licensing by vm.licensingState.collectAsState()
        var showRedeem by remember { mutableStateOf(false) }
        val email = if (account.signedIn) account.email.ifBlank { "" } else ""
        LicenseDetailsDialog(
            tr = tr,
            email = email,
            licensing = licensing,
            onRedeemClick = { showRedeem = true },
            onBuyPlayStore = {
                android.widget.Toast.makeText(
                    ctx,
                    if (tr) "Google Play satın alma yakında aktifleşecek!" else "Google Play in-app purchase coming soon!",
                    android.widget.Toast.LENGTH_SHORT
                ).show()
            },
            onDismiss = { subOpen = false }
        )

        if (showRedeem) {
            com.ray.iptv.ui.screens.paywall.RedeemLicenseDialog(
                onDismiss = { showRedeem = false },
                onRedeem = vm::redeemLicenseCode
            )
        }
    }
    if (delAcc) {
        GlassConfirmDialog(
            title = if (tr) "Hesabımı Sil" else "Delete my account",
            body = if (tr) "Ray IPTV’de bulut hesabı yok. Yerel playlist, önbellek ve tercihleri silmek için «Tüm ayarları sil» kullanın." else "Ray IPTV has no cloud account. Use “Erase all settings” to clear local playlists, cache and preferences.",
            confirm = if (tr) "Tamam" else "OK",
            onDismiss = { delAcc = false },
            onConfirm = { delAcc = false }
        )
    }
}

@Composable
fun LicenseDetailsDialog(
    tr: Boolean,
    email: String,
    licensing: com.ray.iptv.data.repo.LicensingState,
    onRedeemClick: () -> Unit,
    onBuyPlayStore: () -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Box(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(24.dp))
                .background(Color(0xFF0E281F).copy(alpha = 0.95f))
                .border(1.dp, Color(0xFF34D399).copy(alpha = 0.35f), RoundedCornerShape(24.dp))
                .padding(20.dp)
        ) {
            Column(Modifier.fillMaxWidth()) {
                Text(
                    text = if (tr) "Abonelik & Lisans Bilgileri" else "Subscription & License details",
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
                if (email.isNotBlank()) {
                    Spacer(Modifier.height(3.dp))
                    Text(
                        text = if (tr) "$email ile oturum açık" else "Signed in with $email",
                        color = Color(0xFF22D3EE),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                }

                Spacer(Modifier.height(16.dp))

                Row(
                    Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        if (tr) "Lisans Durumu:" else "License Status:",
                        color = Color.White.copy(alpha = 0.85f),
                        fontSize = 14.sp
                    )
                    Text(
                        if (licensing.isPremium) (if (tr) "Premium Aktif (Ömür Boyu)" else "Premium Active (Lifetime)")
                        else if (licensing.isTrialActive) (if (tr) "4 Günlük Deneme Aktif" else "4-Day Trial Active")
                        else (if (tr) "Deneme Süresi Sona Erdi" else "Trial Expired"),
                        color = if (licensing.isPremium || licensing.isTrialActive) Color(0xFF4ADE80) else Color(0xFFEF4444),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                if (!licensing.isPremium && licensing.isTrialActive) {
                    Row(
                        Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            if (tr) "Kalan Deneme Süresi:" else "Trial Remaining:",
                            color = Color.White.copy(alpha = 0.85f),
                            fontSize = 14.sp
                        )
                        Text(
                            licensing.trialRemainingFormatted,
                            color = Color(0xFF22D3EE),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Row(
                    Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        if (tr) "Kayıtlı Cihazlar:" else "Registered Devices:",
                        color = Color.White.copy(alpha = 0.85f),
                        fontSize = 14.sp
                    )
                    Text(
                        "${licensing.deviceCount} / ${licensing.maxDevices}",
                        color = Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                Spacer(Modifier.height(18.dp))

                // Action buttons
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFF0F766E).copy(alpha = 0.5f))
                            .border(1.dp, Color(0xFF2DD4BF), RoundedCornerShape(12.dp))
                            .rayClickable(onClick = onRedeemClick)
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (tr) "🔑 Kod Etkinleştir" else "🔑 Redeem Code",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                    }

                    Box(
                        Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFF262012).copy(alpha = 0.6f))
                            .border(1.dp, Color(0xFFF59E0B), RoundedCornerShape(12.dp))
                            .rayClickable(onClick = onBuyPlayStore)
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (tr) "⭐ Premium Satın Al" else "⭐ Buy Premium",
                            color = Color(0xFFFBBF24),
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                    }
                }

                Spacer(Modifier.height(16.dp))

                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    Box(
                        Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color.White.copy(alpha = 0.05f))
                            .border(1.dp, Color.White.copy(alpha = 0.25f), RoundedCornerShape(10.dp))
                            .rayClickable(onClick = onDismiss)
                            .padding(horizontal = 22.dp, vertical = 9.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            if (tr) "Kapat" else "Close",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun HomePage(vm: RayViewModel, settings: RaySettings, copy: Copy, tr: Boolean) {
    if (settings.layoutMode == LayoutMode.MOBILE) {
        ShowcaseHomeSettings(vm, settings, tr)
        return
    }
    val startupLabel = when (settings.startup) {
        StartupScreen.HOME -> copy.cont
        StartupScreen.LIVE -> copy.live
        StartupScreen.MOVIES -> copy.movies
        StartupScreen.SERIES -> copy.series
        StartupScreen.GUIDE -> copy.guide
    }
    LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            Section(if (tr) "TV ana kabuğu" else "TV shell") {
                Text(
                    if (tr) "Ray TV, Mina kabuğu gibi sol rail + paneller kullanır. Açılış ekranını ve rail sekmelerini buradan yönetin."
                    else "Ray TV uses a left rail and panels like Mina’s shell. Manage the startup screen and rail tabs here.",
                    color = LocalGlass.current.muted,
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
        item { Section(if (tr) "Açılış ekranı" else "Startup") {
            SettingsPickerRow(
                title = if (tr) "Açılış ekranı" else "Startup screen",
                valueLabel = startupLabel,
                options = StartupScreen.entries.map { s ->
                    s to when (s) {
                        StartupScreen.HOME -> copy.cont
                        StartupScreen.LIVE -> copy.live
                        StartupScreen.MOVIES -> copy.movies
                        StartupScreen.SERIES -> copy.series
                        StartupScreen.GUIDE -> copy.guide
                    }
                },
                selected = settings.startup,
                onPick = vm::setStartup
            )
        } }
        item { Section(if (tr) "Ana ekran satırları" else "Home rows") {
            SettingsToggleRow(copy.cont, if (tr) "İzlemeye devam listesi" else "Continue watching list", settings.homeContinue) { vm.setHomeContinue(!settings.homeContinue) }
            SettingsToggleRow(copy.recentlyWatched, if (tr) "Son izlenen canlı kanallar" else "Recently watched live", settings.homeRecentLive) { vm.setHomeRecentLive(!settings.homeRecentLive) }
            SettingsToggleRow(copy.live, if (tr) "Canlı TV kısayolu" else "Live TV shortcut", settings.homeLive) { vm.setHomeLive(!settings.homeLive) }
            SettingsToggleRow(copy.movies, if (tr) "Filmler kısayolu" else "Movies shortcut", settings.homeMovies) { vm.setHomeMovies(!settings.homeMovies) }
            SettingsToggleRow(copy.series, if (tr) "Diziler kısayolu" else "Series shortcut", settings.homeSeries) { vm.setHomeSeries(!settings.homeSeries) }
            SettingsToggleRow(copy.favorites, if (tr) "Favoriler satırı" else "Favorites row", settings.homeFavorites) { vm.setHomeFavorites(!settings.homeFavorites) }
            SettingsToggleRow(copy.downloads, if (tr) "İndirilenler satırı" else "Downloads row", settings.homeDownloads) { vm.setHomeDownloads(!settings.homeDownloads) }
        } }
    }
}

@Composable
private fun RailPage(vm: RayViewModel, settings: RaySettings, copy: Copy, tr: Boolean) {
    LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { Section(if (tr) "Sol menü" else "Left rail") {
            SettingsToggleRow(copy.live, if (tr) "Railde canlı TV sekmesi" else "Live TV tab on the rail", settings.railLive) { vm.setRailLive(!settings.railLive) }
            SettingsToggleRow(copy.movies, if (tr) "Railde filmler sekmesi" else "Movies tab on the rail", settings.railMovies) { vm.setRailMovies(!settings.railMovies) }
            SettingsToggleRow(copy.series, if (tr) "Railde diziler sekmesi" else "Series tab on the rail", settings.railSeries) { vm.setRailSeries(!settings.railSeries) }
            SettingsToggleRow(copy.cont, if (tr) "Railde izlemeye devam sekmesi" else "Continue tab on the rail", settings.railContinue) { vm.setRailContinue(!settings.railContinue) }
            SettingsToggleRow(copy.playlists, if (tr) "Railde listeler sekmesi" else "Playlists tab on the rail", settings.railPlaylists) { vm.setRailPlaylists(!settings.railPlaylists) }
            SettingsToggleRow(copy.repeat, if (tr) "Railde tekrar / catch-up sekmesi" else "Catch-up tab on the rail", settings.railRepeat) { vm.setRailRepeat(!settings.railRepeat) }
        } }
    }
}

@Composable
private fun PerfPage(vm: RayViewModel, settings: RaySettings, tr: Boolean) {
    val rt = Runtime.getRuntime()
    val used = (rt.totalMemory() - rt.freeMemory()) / (1024 * 1024)
    val max = rt.maxMemory() / (1024 * 1024)
    val ctx = LocalContext.current
    val isMobile = LocalMobileSettingsChrome.current
    val cleanLabel = if (settings.imageCleanDays <= 0) (if (tr) "Kapalı" else "Off") else if (tr) "${settings.imageCleanDays} gün" else "${settings.imageCleanDays} days"
    val cacheLabel = when (settings.imageCacheMb) {
        0 -> if (tr) "Otomatik (Cihaza göre)" else "Auto (Device-based)"
        50 -> if (tr) "Düşük (50 MB)" else "Low (50 MB)"
        150 -> if (tr) "Orta (150 MB)" else "Medium (150 MB)"
        300 -> if (tr) "Yüksek (300 MB)" else "High (300 MB)"
        512 -> if (tr) "Maksimum (512 MB)" else "Max (512 MB)"
        else -> "${settings.imageCacheMb} MB"
    }
    val cacheOptions = listOf(0, 50, 150, 300, 512).map {
        it to when (it) {
            0 -> if (tr) "Otomatik (Cihaza göre)" else "Auto (Device-based)"
            50 -> if (tr) "Düşük (50 MB)" else "Low (50 MB)"
            150 -> if (tr) "Orta (150 MB)" else "Medium (150 MB)"
            300 -> if (tr) "Yüksek (300 MB)" else "High (300 MB)"
            else -> if (tr) "Maksimum (512 MB)" else "Max (512 MB)"
        }
    }
    LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            Section(if (tr) "Cihaz" else "Device") {
                if (isMobile) {
                    MobileSettingsFrame {
                        Column(Modifier.padding(horizontal = 16.dp, vertical = 14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                Text(if (tr) "Kullanılan RAM" else "RAM used", color = Color.White.copy(alpha = 0.70f), fontSize = 15.sp)
                                Text("$used MB", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.5.sp)
                            }
                            Box(Modifier.fillMaxWidth().height(1.dp).background(Color.White.copy(alpha = 0.08f)))
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                Text(if (tr) "Uygulama bellek limiti" else "App memory limit", color = Color.White.copy(alpha = 0.70f), fontSize = 15.sp)
                                Text("$max MB", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.5.sp)
                            }
                        }
                    }
                } else {
                    Text(if (tr) "Kullanılan RAM  ${used} / ${max} MB" else "RAM used  ${used} / ${max} MB", color = LocalGlass.current.text)
                }
                if (!isMobile) {
                    SettingsToggleRow(
                        if (tr) "Düşük donanım" else "Low-end mode",
                        if (tr) "Sade grafik, blur/gölge kapalı, bellek öncelikli" else "Simple graphics, blur/shadow off, memory first",
                        settings.lowEndMode,
                        icon = Icons.Filled.Memory
                    ) { vm.setLowEnd(!settings.lowEndMode) }
                }
            }
        }
        item {
            Section(if (tr) "Önbellek Ayarları" else "Cache Settings") {
                SettingsPickerRow(
                    if (tr) "Görsel Önbellek Sınırı" else "Image cache limit",
                    cacheLabel,
                    cacheOptions,
                    settings.imageCacheMb,
                    body = if (tr) "RAM'de tutulacak kanal logosu ve afiş görsellerinin limiti" else "Limit of channel logos and poster images kept in RAM",
                    icon = if (isMobile) null else Icons.Filled.PhotoLibrary,
                    onPick = vm::setImageCacheMb
                )
                SettingsPickerRow(
                    if (tr) "Otomatik Temizleme Sıklığı" else "Auto-clean interval",
                    cleanLabel,
                    listOf(0, 1, 7, 30).map {
                        it to when (it) {
                            0 -> if (tr) "Kapalı" else "Off"
                            1 -> if (tr) "Her açılışta" else "Every launch"
                            7 -> if (tr) "Haftalık" else "Weekly"
                            else -> if (tr) "Aylık" else "Monthly"
                        }
                    },
                    settings.imageCleanDays,
                    body = if (tr) "Depolama önbelleğini belirli aralıklarla otomatik olarak temizler" else "How often storage cache is cleared automatically",
                    icon = if (isMobile) null else Icons.Filled.Schedule,
                    onPick = vm::setImageCleanDays
                )
            }
        }
        item {
            Section(if (tr) "Bakım" else "Maintenance") {
                if (isMobile) {
                    MobileOptionTile(
                        icon = null,
                        title = if (tr) "RAM Bakımı" else "RAM Maintenance",
                        subtitle = if (tr) "Görsel önbelleğini boşaltır, çöp toplayıcıyı çalıştırır" else "Clears image cache and runs garbage collector",
                        onClick = { vm.clearRamCache() },
                        actionButtonText = if (tr) "Çalıştır" else "Run",
                        onActionClick = {
                            vm.clearRamCache()
                            vm.toast.value = if (tr) "RAM bakımı tamamlandı" else "RAM maintenance completed"
                        }
                    )
                    Spacer(Modifier.height(4.dp))
                    MobileOptionTile(
                        icon = null,
                        title = if (tr) "Bellek Temizleme" else "Storage Clean",
                        subtitle = if (tr) "Kanal önbelleği + görsel disk önbelleği + geçici dosyalar • 18.19 MB" else "Channel cache + image disk cache + temp files • 18.19 MB",
                        onClick = {
                            ctx.cacheDir.deleteRecursively()
                            vm.clearRamCache()
                            vm.toast.value = if (tr) "Bellek temizlendi" else "Memory cleared"
                        },
                        actionButtonText = if (tr) "Temizle" else "Clean",
                        onActionClick = {
                            ctx.cacheDir.deleteRecursively()
                            vm.clearRamCache()
                            vm.toast.value = if (tr) "Bellek temizlendi" else "Memory cleared"
                        }
                    )
                } else {
                    SettingsNavRow(
                        if (tr) "RAM bakımı" else "RAM maintenance",
                        if (tr) "Görsel bellek önbelleğini boşaltır" else "Clears the in-memory image cache",
                        icon = Icons.Filled.Memory
                    ) { vm.clearRamCache() }
                    SettingsNavRow(
                        if (tr) "Depolama temizleme" else "Clear storage",
                        if (tr) "Kanal önbelleği + görsel disk önbelleği + geçici dosyalar" else "Channel cache + image disk cache + temp files",
                        icon = Icons.Filled.DeleteForever
                    ) {
                        ctx.cacheDir.deleteRecursively()
                        vm.clearRamCache()
                        vm.toast.value = if (tr) "Önbellek temizlendi" else "Cache cleared"
                    }
                }
            }
        }
    }
}

@Composable
private fun KeysPage(vm: RayViewModel, settings: RaySettings, tr: Boolean) {
    var captureAction by remember { mutableStateOf<String?>(null) }
    val actions = listOf(
        "search" to if (tr) "Hızlı arama ekranını aç" else "Open quick search",
        "guide" to if (tr) "TV rehberini aç" else "Open TV guide",
        "zap_back" to if (tr) "Önceki izlenen kanala geç (zap back)" else "Zap back to previous channel",
        "playlists" to if (tr) "Oynatma listeleri panelini aç" else "Open playlists panel",
        "favorites" to if (tr) "Favoriler listesini aç" else "Open favorites",
        "refresh" to if (tr) "Aktif listeyi yenile" else "Refresh the active list"
    )
    val mapped = remember(settings.keyMapJson) {
        runCatching {
            val o = org.json.JSONObject(settings.keyMapJson.ifBlank { "{}" })
            buildMap {
                o.keys().forEach { k ->
                    put(o.getString(k), k.toIntOrNull() ?: return@forEach)
                }
            }
        }.getOrDefault(emptyMap())
    }
    LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item {
            Section(if (tr) "Kumanda tuş atama" else "Remote key mapping") {
                Text(
                    if (tr) "Geri, OK, oklar ve Home atanamaz. Kırmızı/yeşil/sarı/mavi veya özel tuşları kullanın."
                    else "Back, OK, arrows and Home cannot be assigned. Use colored or unused keys.",
                    color = LocalGlass.current.muted,
                    style = MaterialTheme.typography.bodySmall
                )
                Spacer(Modifier.height(8.dp))
                SettingsToggleRow(
                    if (tr) "Zap yönünü ters çevir" else "Invert zap direction",
                    if (tr) "Yukarı/aşağı tuşlarının kanal sırasını tersine çevir" else "Reverse up/down channel order",
                    settings.zapInvert
                ) { vm.setZapInvert(!settings.zapInvert) }
            }
        }
        items(actions, key = { it.first }) { (id, title) ->
            val code = mapped[id]
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(Modifier.weight(1f)) {
                    SettingsNavRow(
                        title,
                        if (code != null) {
                            val label = android.view.KeyEvent.keyCodeToString(code).removePrefix("KEYCODE_")
                            if (tr) "Atanan tuş: $label" else "Assigned: $label"
                        } else if (tr) "Atanmadı (tuş atamak için seçin)" else "Unassigned — tap to capture"
                    ) {
                        captureAction = id
                        vm.capturingRemoteKey.value = true
                    }
                }
                if (code != null) {
                    GlassButton(if (tr) "Sil" else "Clear") { vm.clearRemoteKey(id) }
                }
            }
        }
    }
    captureAction?.let { action ->
        val requester = remember { FocusRequester() }
        LaunchedEffect(action) { requester.requestFocus() }
        Dialog(onDismissRequest = {
            captureAction = null
            vm.capturingRemoteKey.value = false
        }) {
            GlassPanel(
                strong = true,
                radius = 20.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(requester)
                    .focusable()
                    .onPreviewKeyEvent { e ->
                        if (e.nativeKeyEvent.action != android.view.KeyEvent.ACTION_DOWN) return@onPreviewKeyEvent false
                        val code = e.nativeKeyEvent.keyCode
                        if (code in blockedRemoteKeys) {
                            vm.toast.value = if (tr) "Bu tuş sistem tuşudur, başka bir tuş seçin" else "That key is reserved. Pick another."
                            true
                        } else {
                            vm.assignRemoteKey(code, action)
                            captureAction = null
                            vm.capturingRemoteKey.value = false
                            true
                        }
                    }
            ) {
                Column(Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        actions.first { it.first == action }.second,
                        color = LocalGlass.current.text,
                        style = MaterialTheme.typography.headlineSmall
                    )
                    Text(
                        if (tr) "Kumandanızdan atamak istediğiniz tuşa basın.\n(Geri, OK, oklar ve Home atanamaz)"
                        else "Press the remote key you want to assign.\n(Back, OK, arrows and Home cannot be used)",
                        color = LocalGlass.current.muted
                    )
                    GlassButton(if (tr) "İptal" else "Cancel") {
                        captureAction = null
                        vm.capturingRemoteKey.value = false
                    }
                }
            }
        }
    }
}

private val blockedRemoteKeys = setOf(
    android.view.KeyEvent.KEYCODE_BACK,
    android.view.KeyEvent.KEYCODE_ESCAPE,
    android.view.KeyEvent.KEYCODE_DPAD_CENTER,
    android.view.KeyEvent.KEYCODE_ENTER,
    android.view.KeyEvent.KEYCODE_DPAD_UP,
    android.view.KeyEvent.KEYCODE_DPAD_DOWN,
    android.view.KeyEvent.KEYCODE_DPAD_LEFT,
    android.view.KeyEvent.KEYCODE_DPAD_RIGHT,
    android.view.KeyEvent.KEYCODE_HOME,
    android.view.KeyEvent.KEYCODE_BUTTON_A
)

@Composable private fun LanguagePage(vm: RayViewModel, settings: RaySettings) {
    LazyColumn(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        items(AppLang.entries.size) { i ->
            val lang = AppLang.entries[i]
            val on = settings.lang == lang
            GlassButton(if (on) "${lang.nativeName}  ✓" else lang.nativeName) { vm.setLang(lang) }
        }
    }
}

@Composable private fun ThemePage(vm: RayViewModel, settings: RaySettings, tr: Boolean) {
    val g = LocalGlass.current
    LazyColumn(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item {
            Text(if (tr) "Tema" else "Theme", style = MaterialTheme.typography.headlineLarge, color = g.text)
        }
        item {
            Text(
                if (tr) "Mina IPTV Player’daki tüm görünümler. Seçince hemen uygulanır."
                else "Every look from Mina IPTV Player. Applies immediately.",
                color = g.muted,
                style = MaterialTheme.typography.bodyMedium
            )
        }
        items(glassStylesForLayout(false, settings.glass), key = { it.name }) { style ->
            ThemeChoiceTile(
                style = style,
                selected = settings.glass == style,
                tr = tr,
                onClick = { vm.setGlass(style) }
            )
        }
        item { Spacer(Modifier.height(12.dp)) }
    }
}

@Composable private fun ProfilesPage(
    vm: RayViewModel,
    settings: RaySettings,
    profiles: List<ProfileEntity>,
    tr: Boolean,
    focusRequester: FocusRequester? = null
) {
    var pName by remember { mutableStateOf("") }
    var pPin by remember { mutableStateOf("") }
    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(bottom = 36.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        profiles.forEachIndexed { idx, p ->
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    p.name + if (p.isKids) (if (tr) "  · çocuk" else "  · kids") else "",
                    color = LocalGlass.current.text,
                    modifier = Modifier.padding(top = 12.dp)
                )
                GlassButton(
                    if (p.id == settings.activeProfileId) (if (tr) "Aktif" else "Active")
                    else if (tr) "Seç" else "Select",
                    focusRequester = if (idx == 0) focusRequester else null
                ) { vm.selectProfile(p.id) }
                GlassButton(if (tr) "Sil" else "Delete") { vm.deleteProfile(p.id) }
            }
        }
        GlassField(if (tr) "Yeni profil" else "New profile", pName) { pName = it }
        GlassField("PIN", pPin) { pPin = it }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            GlassButton(if (tr) "Standart" else "Standard") { vm.createProfile(pName, pPin, false) }
            GlassButton(if (tr) "Çocuk" else "Kids") { vm.createProfile(pName, pPin, true) }
        }
    }
}

@Composable private fun BackupPage(
    vm: RayViewModel,
    settings: RaySettings,
    tr: Boolean,
    focusRequester: FocusRequester? = null,
    onBack: () -> Unit = {}
) {
    BackHandler { onBack() }
    val initialFocus = focusRequester ?: remember { FocusRequester() }
    LaunchedEffect(Unit) {
        kotlinx.coroutines.delay(100)
        initialFocus.tryFocus()
    }

    val saveBackup = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri -> uri?.let(vm::exportBackup) }
    val openBackup = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri -> uri?.let(vm::importBackup) }
    val session by vm.account.collectAsState()
    val sources by vm.sources.collectAsState()
    val favorites by vm.favorites.collectAsState()
    val continueList by vm.continueWatching.collectAsState()
    val profiles by vm.profileList.collectAsState()
    val epgSources by vm.epgSources.collectAsState()
    val lastCloudTime by vm.lastCloudBackupTime.collectAsState()
    val cloudSummary by vm.cloudBackupSummary.collectAsState()
    val isCloudBusy by vm.isCloudBusy.collectAsState()
    val cloudProgressMsg by vm.cloudProgressMsg.collectAsState()
    val scope = rememberCoroutineScope()

    var manualSignInOpen by remember { mutableStateOf(false) }
    var backupConfirmOpen by remember { mutableStateOf(false) }
    var restoreConfirmOpen by remember { mutableStateOf(false) }

    val googleSignInLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        scope.launch {
            val ok = vm.handleGoogleSignInIntent(result.data)
            if (ok) {
                vm.restoreFromCloud()
            } else if (result.data != null) {
                manualSignInOpen = true
            }
        }
    }

    androidx.compose.runtime.LaunchedEffect(session?.uid) {
        if (session?.signedIn == true) vm.refreshCloudBackupTime()
    }

    val formattedTime = remember(lastCloudTime) {
        if (lastCloudTime == null || lastCloudTime == 0L) {
            if (tr) "Henüz bulut yedeği alınmadı" else "No cloud backup yet"
        } else {
            val sdf = java.text.SimpleDateFormat("dd.MM.yyyy HH:mm", java.util.Locale.getDefault())
            (if (tr) "Son bulut yedeği: " else "Last cloud backup: ") + sdf.format(java.util.Date(lastCloudTime!!))
        }
    }

    if (backupConfirmOpen && session?.signedIn == true) {
        CloudBackupSummaryDialog(
            tr = tr,
            email = session?.email.orEmpty(),
            sourcesCount = sources.size,
            favoritesCount = favorites.size,
            continueCount = continueList.size,
            profilesCount = profiles.size,
            epgCount = epgSources.size,
            onDismiss = { backupConfirmOpen = false },
            onConfirm = {
                backupConfirmOpen = false
                vm.backupToCloud()
            }
        )
    }

    if (restoreConfirmOpen && session?.signedIn == true) {
        CloudRestoreSummaryDialog(
            tr = tr,
            email = session?.email.orEmpty(),
            lastTimeText = formattedTime,
            onDismiss = { restoreConfirmOpen = false },
            onConfirm = {
                restoreConfirmOpen = false
                vm.fetchCloudBackupForPreview()
            }
        )
    }

    val pendingCloudBackup by vm.pendingCloudBackup.collectAsState()
    if (pendingCloudBackup != null) {
        com.ray.iptv.ui.components.CloudRestorePreviewDialog(
            tr = tr,
            backup = pendingCloudBackup!!,
            localSourcesCount = sources.size,
            localProfilesCount = profiles.size,
            localFavoritesCount = favorites.size,
            localProgressCount = continueList.size,
            localEpgCount = epgSources.size,
            onMerge = { vm.applyCloudRestore(overwrite = false) },
            onOverwrite = { vm.applyCloudRestore(overwrite = true) },
            onDismiss = vm::dismissCloudRestorePreview
        )
    }


    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 4.dp, vertical = 6.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Header Text Hint
        Text(
            if (tr) "Google hesabınızla buluta yedekleyin veya şifreli dosyayı cihaza/paylaşımla alın."
            else "Backup to cloud with Google account or export encrypted file.",
            color = Color.White.copy(alpha = 0.65f),
            fontSize = 13.sp,
            lineHeight = 18.sp
        )

        // 1. Status Card (Senkron aktif) - TV Focusable Top Card
        var statusCardFocused by remember { mutableStateOf(false) }
        val statusShape = RoundedCornerShape(20.dp)
        Box(
            Modifier
                .fillMaxWidth()
                .focusRequester(initialFocus)
                .onFocusChanged { statusCardFocused = it.isFocused }
                .focusable()
                .clip(statusShape)
                .background(if (statusCardFocused) Color(0xFF1E4938) else Color(0xFF16382B).copy(alpha = 0.38f), statusShape)
                .border(
                    if (statusCardFocused) 2.5.dp else 1.dp,
                    if (statusCardFocused) Color(0xFF34D399) else Color(0xFF4ADE80).copy(alpha = 0.18f),
                    statusShape
                )
                .rayClickable(onClick = {
                    if (session?.signedIn != true) {
                        googleSignInLauncher.launch(vm.getGoogleSignInIntent())
                    }
                })
                .padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFF10B981).copy(alpha = 0.22f))
                        .border(1.dp, Color(0xFF10B981).copy(alpha = 0.35f), RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (session?.signedIn == true) Icons.Filled.CloudDone else Icons.Filled.CloudQueue,
                        contentDescription = null,
                        tint = if (session?.signedIn == true) Color(0xFF34D399) else Color.White.copy(alpha = 0.7f),
                        modifier = Modifier.size(24.dp)
                    )
                }
                Spacer(Modifier.width(14.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        if (session?.signedIn == true) (if (tr) "Senkron aktif" else "Sync active")
                        else (if (tr) "Oturum açık değil" else "Not signed in"),
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.5.sp
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        if (session?.signedIn == true) (if (tr) "${session!!.email} ile oturum açık" else "Signed in with ${session!!.email}")
                        else (if (tr) "Google ile oturum açarak bulut yedeklemeyi başlatın" else "Sign in with Google to enable sync"),
                        color = Color.White.copy(alpha = 0.65f),
                        fontSize = 12.sp
                    )
                }
            }
        }

        // 2. Last Backup Summary Card (Son yedek) - TV Focusable
        var summaryCardFocused by remember { mutableStateOf(false) }
        val summaryShape = RoundedCornerShape(20.dp)
        Box(
            Modifier
                .fillMaxWidth()
                .onFocusChanged { summaryCardFocused = it.isFocused }
                .focusable()
                .clip(summaryShape)
                .background(if (summaryCardFocused) Color(0xFF162B38) else Color(0xFF16382B).copy(alpha = 0.38f), summaryShape)
                .border(
                    if (summaryCardFocused) 2.5.dp else 1.dp,
                    if (summaryCardFocused) Color(0xFF22D3EE) else Color(0xFF4ADE80).copy(alpha = 0.18f),
                    summaryShape
                )
                .padding(16.dp)
        ) {
            Column(
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Filled.Inbox,
                            contentDescription = null,
                            tint = Color(0xFF22D3EE),
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(Modifier.width(10.dp))
                        Text(
                            if (tr) "Son yedek" else "Last backup",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                    }
                    val badgeText = cloudSummary?.sizeLabel ?: if (lastCloudTime != null && lastCloudTime != 0L) "Yedekli" else "0 KB"
                    MobileBadge(badgeText, MobileCyan)
                }

                Spacer(Modifier.height(4.dp))

                // Tarih
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.AccessTime, null, tint = Color.White.copy(alpha = 0.6f), modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(if (tr) "Tarih" else "Date", color = Color.White.copy(alpha = 0.75f), fontSize = 13.5.sp)
                    }
                    val effectiveTime = cloudSummary?.updatedAt ?: lastCloudTime
                    val dateStr = if (effectiveTime != null && effectiveTime != 0L) {
                        val sdf = java.text.SimpleDateFormat("dd.MM.yyyy HH:mm", java.util.Locale.getDefault())
                        sdf.format(java.util.Date(effectiveTime))
                    } else (if (tr) "Henüz yedek yok" else "No backup yet")
                    Text(dateStr, color = Color.White, fontSize = 13.5.sp, fontWeight = FontWeight.SemiBold)
                }

                // Listeler
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.Segment, null, tint = Color.White.copy(alpha = 0.6f), modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(if (tr) "Listeler" else "Playlists", color = Color.White.copy(alpha = 0.75f), fontSize = 13.5.sp)
                    }
                    val pCount = cloudSummary?.sourcesCount ?: if (lastCloudTime != null && lastCloudTime != 0L) sources.size else 0
                    Text("$pCount", color = Color.White, fontSize = 13.5.sp, fontWeight = FontWeight.SemiBold)
                }


                // Ayarlar
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.Tune, null, tint = Color.White.copy(alpha = 0.6f), modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(if (tr) "Ayarlar" else "Settings", color = Color.White.copy(alpha = 0.75f), fontSize = 13.5.sp)
                    }
                    val sCount = cloudSummary?.settingsCount ?: if (lastCloudTime != null && lastCloudTime != 0L) 65 else 0
                    Text("$sCount", color = Color.White, fontSize = 13.5.sp, fontWeight = FontWeight.SemiBold)

                }

                // Cihaz
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.StayCurrentPortrait, null, tint = Color.White.copy(alpha = 0.6f), modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(if (tr) "Cihaz" else "Device", color = Color.White.copy(alpha = 0.75f), fontSize = 13.5.sp)
                    }
                    val platformStr = cloudSummary?.platform ?: "android"
                    Text(platformStr, color = Color.White, fontSize = 13.5.sp, fontWeight = FontWeight.SemiBold)
                }
            }

        }

        // 3. Primary Action Buttons (Google'a yedekle / Google'dan geri yükle) - TV D-Pad Focusable
        BackupTvActionCard(
            text = if (tr) "Google'a yedekle" else "Backup to Google",
            icon = Icons.Filled.CloudUpload,
            accentColor = MobileCyan,
            onClick = {
                if (session?.signedIn == true) backupConfirmOpen = true
                else googleSignInLauncher.launch(vm.getGoogleSignInIntent())
            }
        )

        BackupTvActionCard(
            text = if (tr) "Google'dan geri yükle" else "Restore from Google",
            icon = Icons.Filled.CloudDownload,
            accentColor = MobileCyan,
            onClick = {
                if (session?.signedIn == true) restoreConfirmOpen = true
                else googleSignInLauncher.launch(vm.getGoogleSignInIntent())
            }
        )

        // 4. Auto Backup Schedule Section (Otomatik yedekleme)
        MobileSettingsFrame {
            Column(
                Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.Schedule, null, tint = Color(0xFF22D3EE), modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(
                        if (tr) "Otomatik yedekleme" else "Automatic backup",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )
                }
                Text(
                    if (tr) "Ayarlarınız ve listeleriniz seçtiğiniz aralıkta arka planda otomatik olarak Google'a yedeklenir."
                    else "Your settings and playlists will automatically backup to Google at selected interval in background.",
                    color = Color.White.copy(alpha = 0.65f),
                    fontSize = 12.5.sp,
                    lineHeight = 17.sp
                )

                val intervals = listOf(
                    com.ray.iptv.data.repo.AutoBackupInterval.OFF to (if (tr) "Kapalı" else "Off"),
                    com.ray.iptv.data.repo.AutoBackupInterval.DAILY to (if (tr) "Günde bir" else "Daily"),
                    com.ray.iptv.data.repo.AutoBackupInterval.EVERY_3_DAYS to (if (tr) "Haftada bir" else "Weekly")
                )
                val currentInterval = settings.autoBackupInterval

                intervals.forEach { (interval, label) ->
                    val selected = interval == currentInterval || (interval == com.ray.iptv.data.repo.AutoBackupInterval.DAILY && currentInterval != com.ray.iptv.data.repo.AutoBackupInterval.OFF && currentInterval != com.ray.iptv.data.repo.AutoBackupInterval.EVERY_3_DAYS && currentInterval != com.ray.iptv.data.repo.AutoBackupInterval.EVERY_4_DAYS)
                    BackupTvIntervalCard(
                        label = label,
                        selected = selected,
                        onClick = { vm.setAutoBackupInterval(interval) }
                    )
                }
            }
        }

        // 5. Bottom Sign Out / Sign In Button - TV D-Pad Focusable
        if (session?.signedIn == true) {
            BackupTvAccountCard(
                text = if (tr) "Buluttaki yedeği sil" else "Delete cloud backup",
                icon = Icons.Filled.DeleteOutline,
                iconTint = Color(0xFFEF4444),
                onClick = { vm.deleteCloudData() }
            )
            BackupTvAccountCard(
                text = if (tr) "Google oturumunu kapat" else "Sign out from Google",
                icon = Icons.AutoMirrored.Filled.Logout,
                iconTint = Color.White.copy(alpha = 0.85f),
                onClick = { vm.signOutAccount() }
            )
        } else {
            BackupTvAccountCard(
                text = if (tr) "Google ile oturum aç" else "Sign in with Google",
                icon = Icons.Filled.AccountCircle,
                iconTint = Color(0xFF22D3EE),
                onClick = { googleSignInLauncher.launch(vm.getGoogleSignInIntent()) }
            )
        }


        Spacer(Modifier.height(16.dp))

        if (manualSignInOpen) {
            var emailInput by remember { mutableStateOf("furkangumrukcu07@gmail.com") }
            Box(
                Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.7f)),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    Modifier
                        .widthIn(max = 400.dp)
                        .clip(RoundedCornerShape(22.dp))
                        .background(Color(0xFF0F172A).copy(alpha = 0.96f))
                        .border(1.dp, Color(0xFF34D399).copy(alpha = 0.30f), RoundedCornerShape(22.dp))
                        .padding(20.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text(
                            if (tr) "Hızlı Oturum Aç" else "Quick Sign In",
                            color = Color.White,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            if (tr) "E-posta adresinizi girerek oturum açabilirsiniz (Admin paneli için furkangumrukcu07@gmail.com):"
                            else "Enter email to sign in (use furkangumrukcu07@gmail.com for Admin panel):",
                            color = Color.White.copy(alpha = 0.8f),
                            fontSize = 12.sp
                        )
                        GlassField(if (tr) "E-posta" else "Email", emailInput) { emailInput = it }
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            GlassButton(if (tr) "İptal" else "Cancel") { manualSignInOpen = false }
                            GlassButton(if (tr) "Oturum Aç" else "Sign In", primary = true) {
                                manualSignInOpen = false
                                vm.signInLocal(emailInput, emailInput.substringBefore("@"))
                            }
                        }
                    }
                }
            }
        }

        if (isCloudBusy) {
            Dialog(onDismissRequest = {}) {
                Box(
                    Modifier
                        .widthIn(max = 360.dp)
                        .clip(RoundedCornerShape(22.dp))
                        .background(Color(0xFF0F172A).copy(alpha = 0.96f))
                        .border(1.dp, Color(0xFF34D399).copy(alpha = 0.35f), RoundedCornerShape(22.dp))
                        .padding(24.dp)
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        CloudSpinner(
                            color = Color(0xFF00E5FF),
                            modifier = Modifier.size(44.dp)
                        )
                        Text(
                            if (tr) "Google Bulut İşlemi" else "Google Cloud Operation",
                            color = Color.White,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            cloudProgressMsg.ifBlank {
                                if (tr) "Lütfen bekleyin..." else "Please wait..."
                            },
                            color = Color.White.copy(alpha = 0.8f),
                            fontSize = 13.sp,
                            textAlign = TextAlign.Center,
                            lineHeight = 18.sp
                        )
                    }
                }
            }
        }

        // Local Device File Backup Section (Matching Mina IPTV _LocalDeviceBackupSection)
        Spacer(Modifier.height(8.dp))
        Box(
            Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(Color.White.copy(alpha = 0.15f))
        )
        Text(
            if (tr) "Yerel Cihaz Yedeği (Dosya)" else "Local Device Backup (File)",
            color = Color.White,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        Text(
            if (tr) "Yedeklerinizi cihazınızda dosya (JSON/DAT) olarak saklayabilir veya yerel bir yedek dosyasından yükleyebilirsiniz."
            else "Export backups locally to your device as JSON files or restore from a local backup file.",
            color = Color.White.copy(alpha = 0.7f),
            fontSize = 12.sp
        )
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            GlassButton(if (tr) "Yedek Dosyası Oluştur" else "Create Backup File", icon = Icons.Filled.Share) {
                saveBackup.launch("ray-backup.json")
            }
            GlassButton(if (tr) "Dosyadan Geri Yükle" else "Restore from File", icon = Icons.Filled.FolderOpen) {
                openBackup.launch(arrayOf("application/json", "*/*"))
            }
        }
    }
}

@Composable
private fun BackupTvActionCard(
    text: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    accentColor: Color,
    textColor: Color = Color.Black,
    modifier: Modifier = Modifier,
    focusRequester: FocusRequester? = null,
    onClick: () -> Unit
) {
    var focused by remember { mutableStateOf(false) }
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(50.dp)
            .then(if (focusRequester != null) Modifier.focusRequester(focusRequester) else Modifier)
            .onFocusChanged { focused = it.isFocused }
            .focusable()
            .clip(RoundedCornerShape(14.dp))
            .background(if (focused) Color(0xFF22D3EE) else accentColor)
            .border(
                if (focused) 2.5.dp else 1.dp,
                if (focused) Color.White else Color.Transparent,
                RoundedCornerShape(14.dp)
            )
            .rayClickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                icon,
                contentDescription = null,
                tint = if (focused) Color.Black else textColor,
                modifier = Modifier.size(22.dp)
            )
            Spacer(Modifier.width(10.dp))
            Text(
                text,
                color = if (focused) Color.Black else textColor,
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp
            )
        }
    }
}

@Composable
private fun BackupTvIntervalCard(
    label: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    var focused by remember { mutableStateOf(false) }
    Box(
        Modifier
            .fillMaxWidth()
            .onFocusChanged { focused = it.isFocused }
            .focusable()
            .clip(RoundedCornerShape(14.dp))
            .background(
                when {
                    focused -> Color(0xFF22D3EE).copy(alpha = 0.28f)
                    selected -> Color(0xFF16382B).copy(alpha = 0.55f)
                    else -> Color.White.copy(alpha = 0.05f)
                }
            )
            .border(
                when {
                    focused -> 2.dp
                    selected -> 1.5.dp
                    else -> 1.dp
                },
                when {
                    focused -> Color(0xFF00E5FF)
                    selected -> Color(0xFF22D3EE)
                    else -> Color(0xFF4ADE80).copy(alpha = 0.16f)
                },
                RoundedCornerShape(14.dp)
            )
            .rayClickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = if (selected) Icons.Filled.RadioButtonChecked else Icons.Filled.RadioButtonUnchecked,
                contentDescription = null,
                tint = if (focused || selected) Color(0xFF22D3EE) else Color.White.copy(alpha = 0.5f),
                modifier = Modifier.size(20.dp)
            )
            Spacer(Modifier.width(12.dp))
            Text(
                label,
                color = if (focused) Color(0xFF00E5FF) else Color.White,
                fontWeight = if (selected || focused) FontWeight.Bold else FontWeight.Medium,
                fontSize = 14.sp
            )
        }
    }
}

@Composable
private fun BackupTvAccountCard(
    text: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    iconTint: Color,
    onClick: () -> Unit
) {
    var focused by remember { mutableStateOf(false) }
    Box(
        Modifier
            .fillMaxWidth()
            .onFocusChanged { focused = it.isFocused }
            .focusable()
            .clip(RoundedCornerShape(14.dp))
            .background(
                if (focused) Color.White.copy(alpha = 0.18f) else Color.White.copy(alpha = 0.06f)
            )
            .border(
                if (focused) 2.dp else 1.dp,
                if (focused) Color(0xFF22D3EE) else Color.White.copy(alpha = 0.15f),
                RoundedCornerShape(14.dp)
            )
            .rayClickable(onClick = onClick)
            .padding(vertical = 14.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, null, tint = if (focused) Color(0xFF22D3EE) else iconTint, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(8.dp))
            Text(
                text,
                color = if (focused) Color(0xFF22D3EE) else Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp
            )
        }
    }
}

@Composable private fun DownloadsPage(
    vm: RayViewModel,
    downloads: List<DownloadEntity>,
    tr: Boolean,
    focusRequester: FocusRequester? = null
) {
    var pendingDelete by remember { mutableStateOf<DownloadEntity?>(null) }
    if (downloads.isEmpty()) {
        Text(
            if (tr) "Film veya dizi detayından indir. Kuyruk burada görünür; tamamlananları oynatabilir veya silebilirsin."
            else "Queue from movie or series detail. Items appear here; play or delete completed files.",
            color = LocalGlass.current.muted
        )
    } else {
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(bottom = 36.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            downloads.forEachIndexed { idx, d ->
                val status = when (d.status) {
                    "DONE" -> if (tr) "Hazır" else "Ready"
                    "RUNNING", "QUEUED" -> if (tr) "İndiriliyor" else "Downloading"
                    "FAILED" -> if (tr) "Başarısız" else "Failed"
                    else -> d.status
                }
                GlassPanel(strong = true, radius = 14.dp, modifier = Modifier.fillMaxWidth()) {
                    Row(
                        Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(d.title, color = LocalGlass.current.text, maxLines = 2, overflow = TextOverflow.Ellipsis)
                            Text(status, color = LocalGlass.current.muted, style = MaterialTheme.typography.bodySmall)
                        }
                        if (d.status == "DONE") {
                            GlassButton(
                                if (tr) "Oynat" else "Play",
                                focusRequester = if (idx == 0) focusRequester else null
                            ) { vm.playDownload(d) }
                        }
                        GlassButton(
                            if (tr) "Sil" else "Delete",
                            focusRequester = if (idx == 0 && d.status != "DONE") focusRequester else null
                        ) { pendingDelete = d }
                    }
                }
            }
        }
    }
    pendingDelete?.let { item ->
        GlassConfirmDialog(
            title = if (tr) "İndirme silinsin mi?" else "Delete download?",
            body = item.title,
            confirm = if (tr) "Sil" else "Delete",
            cancel = if (tr) "Vazgeç" else "Cancel",
            onDismiss = { pendingDelete = null },
            onConfirm = { vm.deleteDownload(item); pendingDelete = null }
        )
    }
}

private fun playlistKindLabel(kind: String): String = when (kind) {
    "XTREAM" -> "Xtream"
    "M3U" -> "M3U"
    "M3U_FILE" -> "Yerel M3U"
    "STALKER" -> "Stalker"
    else -> kind
}

private fun toggle(tr: Boolean, trOn: String, enOn: String, on: Boolean): String {
    val base = if (tr) trOn else enOn
    return if (on) "$base ✓" else base
}

@Composable
private fun Section(title: String, content: @Composable () -> Unit) {
    if (LocalMobileSettingsChrome.current) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            SettingsSectionLabel(title)
            content()
        }
        return
    }
    val g = LocalGlass.current
    GlassPanel(strong = true, radius = 20.dp) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(title, color = g.accent, style = MaterialTheme.typography.titleMedium)
            content()
        }
    }
}

@Composable
private fun CloudSpinner(color: Color, modifier: Modifier = Modifier) {
    val spin = rememberInfiniteTransition(label = "cloud-spin")
    val deg by spin.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "cloud-rot"
    )
    Canvas(modifier.rotate(deg)) {
        drawArc(
            color = color,
            startAngle = 16f,
            sweepAngle = 280f,
            useCenter = false,
            style = Stroke(width = 3.5.dp.toPx(), cap = StrokeCap.Round)
        )
    }
}

@Composable
private fun CloudBackupSummaryDialog(
    tr: Boolean,
    email: String,
    sourcesCount: Int,
    favoritesCount: Int,
    continueCount: Int,
    profilesCount: Int,
    epgCount: Int,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    val cyan = Color(0xFF22D3EE)
    val emerald = Color(0xFF34D399)
    val initialFocus = remember { FocusRequester() }
    LaunchedEffect(Unit) {
        delay(150)
        try { initialFocus.requestFocus() } catch (_: Exception) {}
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false)
    ) {
        androidx.activity.compose.BackHandler { onDismiss() }
        Box(
            Modifier
                .width(460.dp)
                .clip(RoundedCornerShape(22.dp))
                .background(Color(0xFF0E281F).copy(alpha = 0.96f))
                .border(1.dp, emerald.copy(alpha = 0.35f), RoundedCornerShape(22.dp))
                .padding(20.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        Modifier
                            .size(40.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(cyan.copy(alpha = 0.18f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Filled.CloudUpload, contentDescription = null, tint = cyan, modifier = Modifier.size(22.dp))
                    }
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text(
                            if (tr) "Buluta Yedekle" else "Backup to Cloud",
                            color = Color.White,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            if (tr) "Google Hesabı: $email" else "Google Account: $email",
                            color = Color.White.copy(alpha = 0.65f),
                            fontSize = 12.sp
                        )
                    }
                }

                Text(
                    if (tr) "Aşağıdaki verileriniz Google hesabınıza güvenli ve şifreli şekilde yedeklenecektir:"
                    else "The following data will be securely backed up to your Google account:",
                    color = Color.White.copy(alpha = 0.85f),
                    fontSize = 12.5.sp,
                    lineHeight = 17.sp
                )

                Column(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(Color.White.copy(alpha = 0.05f))
                        .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(14.dp))
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    BackupSummaryRow(
                        icon = Icons.AutoMirrored.Filled.PlaylistPlay,
                        label = if (tr) "Oynatma Listeleri" else "Playlists",
                        detail = if (tr) "$sourcesCount liste (Xtream & M3U)" else "$sourcesCount playlists"
                    )
                    BackupSummaryRow(
                        icon = Icons.Filled.Star,
                        label = if (tr) "Favori İçerikler" else "Favorites",
                        detail = if (tr) "$favoritesCount içerik (Canlı, Film, Dizi)" else "$favoritesCount items"
                    )
                    BackupSummaryRow(
                        icon = Icons.Filled.History,
                        label = if (tr) "İzleme Geçmişi" else "Watch History",
                        detail = if (tr) "$continueCount içerik (Kaldığınız yerler)" else "$continueCount items in progress"
                    )
                    BackupSummaryRow(
                        icon = Icons.Filled.People,
                        label = if (tr) "Kullanıcı Profilleri" else "Profiles",
                        detail = if (tr) "$profilesCount profil (PIN & Tercihler)" else "$profilesCount profiles"
                    )
                    BackupSummaryRow(
                        icon = Icons.Filled.Schedule,
                        label = if (tr) "EPG Rehber Kaynakları" else "EPG Sources",
                        detail = if (tr) "$epgCount kaynak" else "$epgCount sources"
                    )
                    BackupSummaryRow(
                        icon = Icons.Filled.Tune,
                        label = if (tr) "Uygulama Ayarları" else "App Settings",
                        detail = if (tr) "Tema, Dil, Oynatıcı Tercihleri" else "Theme, Language, Player Preferences"
                    )
                }

                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.End)
                ) {
                    GlassButton(if (tr) "Vazgeç" else "Cancel") { onDismiss() }
                    GlassButton(if (tr) "Yedeklemeyi Başlat" else "Start Backup", primary = true, focusRequester = initialFocus) {
                        onConfirm()
                    }
                }
            }
        }
    }
}

@Composable
private fun CloudRestoreSummaryDialog(
    tr: Boolean,
    email: String,
    lastTimeText: String,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    val cyan = Color(0xFF22D3EE)
    val emerald = Color(0xFF34D399)
    val initialFocus = remember { FocusRequester() }
    LaunchedEffect(Unit) {
        delay(150)
        try { initialFocus.requestFocus() } catch (_: Exception) {}
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false)
    ) {
        androidx.activity.compose.BackHandler { onDismiss() }
        Box(
            Modifier
                .width(460.dp)
                .clip(RoundedCornerShape(22.dp))
                .background(Color(0xFF0E281F).copy(alpha = 0.96f))
                .border(1.dp, emerald.copy(alpha = 0.35f), RoundedCornerShape(22.dp))
                .padding(20.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        Modifier
                            .size(40.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(cyan.copy(alpha = 0.18f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Filled.CloudDownload, contentDescription = null, tint = cyan, modifier = Modifier.size(22.dp))
                    }
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text(
                            if (tr) "Buluttan Geri Yükle" else "Restore from Cloud",
                            color = Color.White,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            if (tr) "Google Hesabı: $email" else "Google Account: $email",
                            color = Color.White.copy(alpha = 0.65f),
                            fontSize = 12.sp
                        )
                    }
                }

                Text(
                    if (tr) "Google bulutta kayıtlı olan en son yedeğiniz bu cihaza geri yüklenecektir ($lastTimeText)."
                    else "Your latest backup stored in Google cloud will be restored to this device ($lastTimeText).",
                    color = Color.White.copy(alpha = 0.85f),
                    fontSize = 12.5.sp,
                    lineHeight = 17.sp
                )

                Column(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(Color.White.copy(alpha = 0.05f))
                        .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(14.dp))
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    BackupSummaryRow(
                        icon = Icons.AutoMirrored.Filled.PlaylistPlay,
                        label = if (tr) "Oynatma Listeleri" else "Playlists",
                        detail = if (tr) "Geri Yüklenecek" else "Will restore"
                    )
                    BackupSummaryRow(
                        icon = Icons.Filled.Star,
                        label = if (tr) "Favori Kanallar & İçerikler" else "Favorites",
                        detail = if (tr) "Geri Yüklenecek" else "Will restore"
                    )
                    BackupSummaryRow(
                        icon = Icons.Filled.History,
                        label = if (tr) "İzleme Geçmişi (Kaldığınız Yer)" else "Watch History",
                        detail = if (tr) "Geri Yüklenecek" else "Will restore"
                    )
                    BackupSummaryRow(
                        icon = Icons.Filled.People,
                        label = if (tr) "Kullanıcı Profilleri & PIN" else "Profiles & PIN",
                        detail = if (tr) "Geri Yüklenecek" else "Will restore"
                    )
                    BackupSummaryRow(
                        icon = Icons.Filled.Tune,
                        label = if (tr) "Tüm Ayarlar ve Tercihler" else "Settings & Preferences",
                        detail = if (tr) "Geri Yüklenecek" else "Will restore"
                    )
                }

                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.End)
                ) {
                    GlassButton(if (tr) "Vazgeç" else "Cancel") { onDismiss() }
                    GlassButton(if (tr) "Geri Yüklemeyi Başlat" else "Start Restore", primary = true, focusRequester = initialFocus) {
                        onConfirm()
                    }
                }
            }
        }
    }
}


@Composable
private fun BackupSummaryRow(icon: ImageVector, label: String, detail: String) {
    val cyan = Color(0xFF00E5FF)
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
        Icon(icon, contentDescription = null, tint = cyan, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(10.dp))
        Text(
            label,
            color = Color.White,
            fontSize = 12.5.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.weight(1f)
        )
        Text(
            detail,
            color = Color.White.copy(alpha = 0.7f),
            fontSize = 11.5.sp
        )
    }
}
