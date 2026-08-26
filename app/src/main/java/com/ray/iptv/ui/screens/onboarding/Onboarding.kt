package com.ray.iptv.ui.screens.onboarding

import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.LiveTv
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.outlined.Circle
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Icon
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.ray.iptv.data.repo.AppLang
import com.ray.iptv.data.repo.GlassStyle
import com.ray.iptv.data.repo.PlaybackEngine
import com.ray.iptv.data.repo.RaySettings
import com.ray.iptv.ui.RayViewModel
import com.ray.iptv.ui.components.GlassButton
import com.ray.iptv.ui.glass.GlassPanel
import com.ray.iptv.ui.glass.RayWallpaper
import com.ray.iptv.ui.i18n.Copy
import com.ray.iptv.ui.input.LocalTouchUi
import com.ray.iptv.ui.motion.RaySwitch
import com.ray.iptv.ui.theme.LocalGlass
import com.ray.iptv.ui.theme.subtitle
import com.ray.iptv.ui.theme.title

private enum class SetupStep { LANGUAGE, THEME, PLAYER, FEATURES, SOURCE }

@Composable
fun OnboardingFlow(vm: RayViewModel, copy: Copy) {
    val g = LocalGlass.current
    val settings by vm.settings.collectAsState()
    val sources by vm.sources.collectAsState()
    val sync by vm.catalog.sync.collectAsState()
    val touch = LocalTouchUi.current
    val tr = settings.lang == AppLang.TR
    val s = remember(tr) { setupCopy(tr) }
    val pages = remember(touch) {
        if (touch) {
            listOf(
                SetupStep.LANGUAGE,
                SetupStep.THEME,
                SetupStep.PLAYER,
                SetupStep.FEATURES,
                SetupStep.SOURCE
            )
        } else {
            listOf(SetupStep.THEME, SetupStep.PLAYER, SetupStep.SOURCE)
        }
    }
    var index by remember { mutableIntStateOf(0) }
    var waitingSource by remember { mutableStateOf(false) }
    val step = pages.getOrElse(index) { pages.last() }
    val last = index >= pages.lastIndex
    val canFinish = sources.isNotEmpty()
    val nextFocus = remember { FocusRequester() }

    LaunchedEffect(sources.size, waitingSource, sync.running, sync.error) {
        if (waitingSource && sources.isNotEmpty() && !sync.running && sync.error.isBlank()) {
            waitingSource = false
            vm.completeSetup()
        }
    }

    fun goNext() {
        when {
            index < pages.lastIndex -> index++
            canFinish -> vm.completeSetup()
            else -> vm.toast.value = s.needSource
        }
    }

    Box(
        Modifier
            .fillMaxSize()
            .then(if (touch) Modifier.systemBarsPadding() else Modifier)
    ) {
        RayWallpaper()
        Column(
            Modifier
                .fillMaxSize()
                .padding(if (touch) 20.dp else 32.dp)
                .widthIn(max = if (touch) 720.dp else 880.dp)
                .align(Alignment.Center)
        ) {
            Text("Ray", style = MaterialTheme.typography.displayLarge, color = g.accent)
            Text(s.welcome, style = MaterialTheme.typography.headlineLarge, color = g.text)
            Spacer(Modifier.height(6.dp))
            Text(stepTitle(step, s), style = MaterialTheme.typography.bodyLarge, color = g.muted)
            Spacer(Modifier.height(12.dp))
            SetupProgress((index + 1f) / pages.size)
            Spacer(Modifier.height(12.dp))
            GlassPanel(
                strong = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                Box(
                    Modifier
                        .fillMaxSize()
                        .clipToBounds()
                        .padding(if (touch) 18.dp else 24.dp)
                ) {
                    RaySwitch(step, Modifier.fillMaxSize()) { current ->
                        when (current) {
                            SetupStep.LANGUAGE -> LanguageStep(settings, vm, nextFocus) { goNext() }
                            SetupStep.THEME -> ThemeStep(
                                settings = settings,
                                tr = tr,
                                tv = !touch,
                                nextFocus = nextFocus,
                                onPick = { style ->
                                    vm.setGlass(style)
                                    goNext()
                                }
                            )
                            SetupStep.PLAYER -> PlayerStep(
                                settings = settings,
                                s = s,
                                tv = !touch,
                                nextFocus = nextFocus,
                                onLive = { engine ->
                                    vm.setLiveEngine(engine)
                                    if (!touch) {
                                        vm.setVodPlaybackEngine(engine)
                                        goNext()
                                    }
                                },
                                onVod = { engine ->
                                    vm.setVodPlaybackEngine(engine)
                                    if (touch) goNext()
                                }
                            )
                            SetupStep.FEATURES -> FeaturesStep(settings, vm, s, nextFocus)
                            SetupStep.SOURCE -> SourceStep(
                                s = s,
                                syncing = sync.running,
                                error = sync.error,
                                onLoad = { waitingSource = true },
                                vm = vm
                            )
                        }
                    }
                }
            }
            Spacer(Modifier.height(12.dp))
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (index > 0) GlassButton(s.back) { index-- }
                else Spacer(Modifier.width(8.dp))
                GlassButton(
                    if (last) s.finish else s.next,
                    primary = true,
                    modifier = Modifier.focusRequester(nextFocus)
                ) { goNext() }
            }
        }
    }
}

@Composable
private fun SetupProgress(fraction: Float) {
    val g = LocalGlass.current
    Box(
        Modifier
            .fillMaxWidth()
            .height(5.dp)
            .clip(RoundedCornerShape(6.dp))
            .background(g.text.copy(alpha = 0.12f))
    ) {
        Box(
            Modifier
                .fillMaxWidth(fraction.coerceIn(0.08f, 1f))
                .fillMaxHeight()
                .background(g.accent)
        )
    }
}

@Composable
private fun LanguageStep(
    settings: RaySettings,
    vm: RayViewModel,
    nextFocus: FocusRequester,
    onPicked: () -> Unit
) {
    val langs = AppLang.entries
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        itemsIndexed(langs, key = { _, lang -> lang.name }) { i, lang ->
            val on = settings.lang == lang
            GlassButton(
                if (on) "${lang.nativeName}  ✓" else lang.nativeName,
                modifier = if (i == langs.lastIndex) {
                    Modifier.focusProperties { down = nextFocus }
                } else Modifier
            ) {
                vm.setLang(lang)
                onPicked()
            }
        }
    }
}

private val wizardThemes = listOf(
    GlassStyle.TV_LITE,
    GlassStyle.MACOS_TV,
    GlassStyle.DARK,
    GlassStyle.AMOLED,
    GlassStyle.FLY_UI,
    GlassStyle.SEMC,
    GlassStyle.DARK_FLAT,
    GlassStyle.FLAT_BLACK,
    GlassStyle.GLASS_GRI
)

@Composable
private fun ThemeStep(
    settings: RaySettings,
    tr: Boolean,
    tv: Boolean,
    nextFocus: FocusRequester,
    onPick: (GlassStyle) -> Unit
) {
    val themes = remember(tv) {
        wizardThemes.filter { style ->
            when {
                tv && style in setOf(GlassStyle.FLY_UI) -> false
                !tv && style == GlassStyle.TV_LITE -> false
                else -> true
            }
        }
    }
    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        themes.forEachIndexed { i, style ->
            ChoiceCard(
                title = style.title(tr),
                subtitle = style.subtitle(tr),
                selected = settings.glass == style,
                modifier = if (i == themes.lastIndex) {
                    Modifier.focusProperties { down = nextFocus }
                } else Modifier,
                onClick = { onPick(style) }
            )
        }
    }
}

@Composable
private fun PlayerStep(
    settings: RaySettings,
    s: SetupCopy,
    tv: Boolean,
    nextFocus: FocusRequester,
    onLive: (PlaybackEngine) -> Unit,
    onVod: (PlaybackEngine) -> Unit
) {
    if (tv) {
        Column(
            Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            EngineCard(s.betterTitle, s.betterSub, settings.liveEngine == PlaybackEngine.BETTER, Icons.Filled.Bolt) {
                onLive(PlaybackEngine.BETTER)
            }
            EngineCard(
                s.mediaKitTitle,
                s.mediaKitSub,
                settings.liveEngine == PlaybackEngine.MEDIA_KIT,
                Icons.Filled.Memory,
                modifier = Modifier.focusProperties { down = nextFocus }
            ) {
                onLive(PlaybackEngine.MEDIA_KIT)
            }
        }
    } else {
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text(s.liveEngine, color = LocalGlass.current.text, fontWeight = FontWeight.Bold)
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                EngineCard(
                    s.betterTitle, s.betterSub, settings.liveEngine == PlaybackEngine.BETTER, Icons.Filled.LiveTv,
                    Modifier.weight(1f)
                ) { onLive(PlaybackEngine.BETTER) }
                EngineCard(
                    s.mediaKitTitle, s.mediaKitSub, settings.liveEngine == PlaybackEngine.MEDIA_KIT, Icons.Filled.Memory,
                    Modifier.weight(1f)
                ) { onLive(PlaybackEngine.MEDIA_KIT) }
            }
            Text(s.vodEngine, color = LocalGlass.current.text, fontWeight = FontWeight.Bold)
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                EngineCard(
                    s.betterTitle, s.betterSub, settings.vodPlaybackEngine == PlaybackEngine.BETTER, Icons.Filled.Movie,
                    Modifier.weight(1f)
                ) { onVod(PlaybackEngine.BETTER) }
                EngineCard(
                    s.mediaKitTitle, s.mediaKitSub, settings.vodPlaybackEngine == PlaybackEngine.MEDIA_KIT, Icons.Filled.Memory,
                    Modifier.weight(1f).focusProperties { down = nextFocus }
                ) { onVod(PlaybackEngine.MEDIA_KIT) }
            }
        }
    }
}

@Composable
private fun FeaturesStep(settings: RaySettings, vm: RayViewModel, s: SetupCopy, nextFocus: FocusRequester) {
    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        ChoiceCard(s.featContinue, s.featContinueSub, settings.homeContinue) {
            vm.setHomeContinue(!settings.homeContinue)
        }
        ChoiceCard(s.featPreview, s.featPreviewSub, settings.previewLive) {
            vm.setPreview(!settings.previewLive)
        }
        ChoiceCard(s.featStrip, s.featStripSub, settings.stripChannelPrefix) {
            vm.setStripPrefix(!settings.stripChannelPrefix)
        }
        ChoiceCard(s.featBoot, s.featBootSub, settings.launchOnBoot) {
            vm.setLaunchOnBoot(!settings.launchOnBoot)
        }
        ChoiceCard(s.featEpg, s.featEpgSub, settings.epgEnabled, Modifier.focusProperties { down = nextFocus }) {
            vm.setEpgEnabled(!settings.epgEnabled)
        }
    }
}

@Composable
private fun SourceStep(
    s: SetupCopy,
    syncing: Boolean,
    error: String,
    onLoad: () -> Unit,
    vm: RayViewModel
) {
    var sourceName by remember { mutableStateOf("") }
    var kind by remember { mutableIntStateOf(4) }
    var server by remember { mutableStateOf("") }
    var user by remember { mutableStateOf("") }
    var pass by remember { mutableStateOf("") }
    var m3u by remember { mutableStateOf("") }
    var mac by remember { mutableStateOf("") }
    var fileUri by remember { mutableStateOf("") }
    val ctx = LocalContext.current
    val clip = LocalClipboardManager.current
    val pickM3u = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            runCatching {
                ctx.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            fileUri = uri.toString()
        }
    }
    fun load() {
        if (syncing) return
        onLoad()
        when (kind) {
            0 -> vm.addM3u(sourceName.ifBlank { "M3U" }, m3u)
            1 -> if (fileUri.isNotBlank()) vm.addLocalM3u(sourceName.ifBlank { s.localM3u }, fileUri)
            2 -> vm.addXtream(sourceName.ifBlank { "Xtream" }, server, user, pass)
            3 -> vm.addStalker(sourceName.ifBlank { "Stalker" }, server, mac)
            else -> vm.addDemoPlaylist()
        }
    }
    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text(s.sourceHint, color = LocalGlass.current.muted, style = MaterialTheme.typography.bodyLarge)

        val scope = rememberCoroutineScope()
        val tr = vm.settings.collectAsState().value.lang == AppLang.TR

        val googleSignInLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            scope.launch {
                val ok = vm.handleGoogleSignInIntent(result.data)
                if (ok) vm.restoreFromCloud()
            }
        }

        Box(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(androidx.compose.ui.graphics.Color.White.copy(alpha = 0.08f))
                .padding(12.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier
                        .size(36.dp)
                        .clip(androidx.compose.foundation.shape.CircleShape)
                        .background(androidx.compose.ui.graphics.Color.White),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "G",
                        color = androidx.compose.ui.graphics.Color(0xFF4285F4),
                        fontWeight = FontWeight.Black,
                        style = MaterialTheme.typography.titleMedium
                    )
                }
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        if (tr) "Google ile Oturum Aç & Buluttan Yükle" else "Sign in with Google & Restore from Cloud",
                        color = androidx.compose.ui.graphics.Color.White,
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        if (tr) "Kayıtlı listelerinizi ve ayarlarınızı buluttan otomatik geri yükleyin."
                        else "Automatically restore your saved playlists & settings from cloud.",
                        color = androidx.compose.ui.graphics.Color.White.copy(alpha = 0.7f),
                        style = MaterialTheme.typography.bodySmall
                    )
                }
                Spacer(Modifier.width(8.dp))
                GlassButton(if (tr) "Oturum Aç" else "Sign In", primary = true) {
                    googleSignInLauncher.launch(vm.getGoogleSignInIntent())
                }
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            GlassButton(if (kind == 4) "${s.demo} ✓" else s.demo) { kind = 4 }
            GlassButton(if (kind == 0) "M3U URL ✓" else "M3U URL") { kind = 0 }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            GlassButton(if (kind == 1) "${s.m3uFile} ✓" else s.m3uFile) { kind = 1 }
            GlassButton(if (kind == 2) "Xtream ✓" else "Xtream") { kind = 2 }
            GlassButton(if (kind == 3) "Stalker ✓" else "Stalker") { kind = 3 }
        }
        if (kind != 4) GlassField(s.listName, sourceName) { sourceName = it }
        when (kind) {
            0 -> {
                GlassField("M3U URL", m3u) { m3u = it }
                GlassButton(s.paste) {
                    val t = clip.getText()?.text.orEmpty()
                    if (t.isNotBlank()) m3u = t else vm.toast.value = s.clipboardEmpty
                }
            }
            1 -> {
                Text(
                    if (fileUri.isBlank()) s.noFile else fileUri.substringAfterLast('/'),
                    color = LocalGlass.current.muted,
                    style = MaterialTheme.typography.bodySmall
                )
                GlassButton(if (fileUri.isBlank()) s.pickFile else s.pickOther) {
                    pickM3u.launch(arrayOf("*/*"))
                }
            }
            2 -> {
                GlassField(s.server, server) { server = it }
                GlassField(s.username, user) { user = it }
                GlassField(s.password, pass) { pass = it }
            }
            3 -> {
                GlassField("Portal URL", server) { server = it }
                GlassField("MAC", mac) { mac = it }
            }
            else -> Text(s.demoHint, color = LocalGlass.current.muted, style = MaterialTheme.typography.bodyLarge)
        }
        if (error.isNotBlank()) {
            Text(error, color = LocalGlass.current.danger, style = MaterialTheme.typography.bodySmall)
        }
        GlassButton(if (syncing) s.loading else s.loadList, primary = true) { load() }
    }
}

@Composable
private fun ChoiceCard(
    title: String,
    subtitle: String,
    selected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val g = LocalGlass.current
    var focused by remember { mutableStateOf(false) }
    GlassPanel(
        strong = selected,
        focused = focused,
        accentFill = selected,
        radius = 16.dp,
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .onFocusChanged { focused = it.isFocused }
    ) {
        Row(
            Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text(title, color = g.text, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                if (subtitle.isNotBlank()) {
                    Spacer(Modifier.height(4.dp))
                    Text(subtitle, color = g.muted, style = MaterialTheme.typography.bodySmall)
                }
            }
            Icon(
                imageVector = if (selected) Icons.Filled.CheckCircle else Icons.Outlined.Circle,
                contentDescription = null,
                tint = if (selected) g.accent else g.muted,
                modifier = Modifier.size(22.dp)
            )
        }
    }
}

@Composable
private fun EngineCard(
    title: String,
    subtitle: String,
    selected: Boolean,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val g = LocalGlass.current
    var focused by remember { mutableStateOf(false) }
    GlassPanel(
        strong = selected,
        focused = focused,
        accentFill = selected,
        radius = 16.dp,
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .onFocusChanged { focused = it.isFocused }
    ) {
        Column(Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(icon, contentDescription = null, tint = if (selected) g.accent else g.muted, modifier = Modifier.size(22.dp))
                Spacer(Modifier.width(10.dp))
                Text(title, color = g.text, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                Icon(
                    imageVector = if (selected) Icons.Filled.CheckCircle else Icons.Outlined.Circle,
                    contentDescription = null,
                    tint = if (selected) g.accent else g.muted,
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(Modifier.height(6.dp))
            Text(subtitle, color = g.muted, style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
fun GlassField(label: String, value: String, onValue: (String) -> Unit) {
    val g = LocalGlass.current
    Column(Modifier.fillMaxWidth()) {
        Text(label, color = g.muted, style = MaterialTheme.typography.labelLarge)
        Spacer(Modifier.height(6.dp))
        GlassPanel(radius = 12.dp, modifier = Modifier.fillMaxWidth()) {
            BasicTextField(
                value = value,
                onValueChange = onValue,
                textStyle = MaterialTheme.typography.bodyLarge.copy(color = g.text),
                cursorBrush = SolidColor(g.accent),
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 12.dp)
            )
        }
    }
}

@Composable
fun ProfileGate(vm: RayViewModel) {
    val g = LocalGlass.current
    val list by vm.profileList.collectAsState(emptyList())
    Box(Modifier.fillMaxSize()) {
        RayWallpaper()
        Column(
            Modifier.align(Alignment.Center),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("Who's watching?", style = MaterialTheme.typography.displayLarge, color = g.text)
            Spacer(Modifier.height(28.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(18.dp)) {
                list.forEach { p ->
                    GlassButton(p.name + if (p.isKids) "  · kids" else "") {
                        vm.selectProfile(p.id)
                    }
                }
            }
        }
    }
}

private fun stepTitle(step: SetupStep, s: SetupCopy) = when (step) {
    SetupStep.LANGUAGE -> s.stepLanguage
    SetupStep.THEME -> s.stepTheme
    SetupStep.PLAYER -> s.stepPlayer
    SetupStep.FEATURES -> s.stepFeatures
    SetupStep.SOURCE -> s.stepSource
}

private data class SetupCopy(
    val welcome: String,
    val stepLanguage: String,
    val stepTheme: String,
    val stepPlayer: String,
    val stepFeatures: String,
    val stepSource: String,
    val next: String,
    val back: String,
    val finish: String,
    val needSource: String,
    val betterTitle: String,
    val betterSub: String,
    val mediaKitTitle: String,
    val mediaKitSub: String,
    val liveEngine: String,
    val vodEngine: String,
    val featContinue: String,
    val featContinueSub: String,
    val featPreview: String,
    val featPreviewSub: String,
    val featStrip: String,
    val featStripSub: String,
    val featBoot: String,
    val featBootSub: String,
    val featEpg: String,
    val featEpgSub: String,
    val sourceHint: String,
    val demo: String,
    val demoHint: String,
    val m3uFile: String,
    val listName: String,
    val paste: String,
    val clipboardEmpty: String,
    val noFile: String,
    val pickFile: String,
    val pickOther: String,
    val localM3u: String,
    val server: String,
    val username: String,
    val password: String,
    val loadList: String,
    val loading: String
)

private fun setupCopy(tr: Boolean) = if (tr) SetupCopy(
    welcome = "Hoş geldiniz",
    stepLanguage = "Dil",
    stepTheme = "Görünüm",
    stepPlayer = "Oynatıcı",
    stepFeatures = "Özellikler",
    stepSource = "Oynatma listesi",
    next = "İlerle",
    back = "Geri",
    finish = "Kurulumu bitir",
    needSource = "Önce oynatma listesini yükleyin (M3U veya Xtream).",
    betterTitle = "Better / ExoPlayer",
    betterSub = "Düşük gecikme, Android önerisi; çoğu canlı ve film için uygundur.",
    mediaKitTitle = "MediaKit (mpv)",
    mediaKitSub = "Daha ağır akışlarda / özel altyapıda alternatif; ayarlardan değiştirilebilir.",
    liveEngine = "Canlı TV oynatıcısı",
    vodEngine = "Film / dizi oynatıcısı",
    featContinue = "İzlemeye devam",
    featContinueSub = "Yarıda kalan film ve diziler ana ekranda görünür.",
    featPreview = "Yayın önizlemesi",
    featPreviewSub = "Liste detayında sessiz canlı önizleme.",
    featStrip = "Kanal önekini kaldır",
    featStripSub = "Canlı kanal adlarındaki ülke / kalite öneklerini gizler.",
    featBoot = "Açılışta başlat",
    featBootSub = "Cihaz açılınca Ray otomatik başlar.",
    featEpg = "TV rehberi (EPG)",
    featEpgSub = "Program bilgilerini kanalların üzerinde göster.",
    sourceHint = "M3U veya Xtream bilgilerinizi girin; listeyi yükledikten sonra uygulama açılır.",
    demo = "Demo",
    demoHint = "Önceden tanımlı demo kanallarla uygulamayı hemen deneyin; sunucu bilgisi gerekmez.",
    m3uFile = "M3U Dosya",
    listName = "Liste adı (opsiyonel)",
    paste = "Yapıştır",
    clipboardEmpty = "Panoda metin yok",
    noFile = "Henüz dosya seçilmedi",
    pickFile = "Dosya seç",
    pickOther = "Başka dosya seç",
    localM3u = "Yerel M3U",
    server = "Sunucu Adresi",
    username = "Kullanıcı Adı",
    password = "Şifre",
    loadList = "Listeyi Yükle",
    loading = "Yükleniyor…"
) else SetupCopy(
    welcome = "Welcome",
    stepLanguage = "Language",
    stepTheme = "Appearance",
    stepPlayer = "Player",
    stepFeatures = "Features",
    stepSource = "Playlist",
    next = "Continue",
    back = "Back",
    finish = "Finish setup",
    needSource = "Load a playlist first (M3U or Xtream).",
    betterTitle = "Better / ExoPlayer",
    betterSub = "Low latency, Android default; best for most live and VOD streams.",
    mediaKitTitle = "MediaKit (mpv)",
    mediaKitSub = "Alternative for heavier streams or custom stacks; can be changed later in settings.",
    liveEngine = "Live TV player",
    vodEngine = "Movies / series player",
    featContinue = "Continue watching",
    featContinueSub = "Unfinished movies and series appear on home.",
    featPreview = "Stream preview",
    featPreviewSub = "Silent live preview in list detail.",
    featStrip = "Strip channel prefix",
    featStripSub = "Hide country / quality prefixes from live channel names.",
    featBoot = "Launch on boot",
    featBootSub = "Start Ray automatically when the device boots.",
    featEpg = "TV guide (EPG)",
    featEpgSub = "Show programme info over channels.",
    sourceHint = "Enter your M3U or Xtream details; the app opens after the list loads.",
    demo = "Demo",
    demoHint = "Try the app right away with built-in demo channels; no server credentials needed.",
    m3uFile = "M3U File",
    listName = "List name (optional)",
    paste = "Paste",
    clipboardEmpty = "Clipboard is empty",
    noFile = "No file selected",
    pickFile = "Choose file",
    pickOther = "Choose another file",
    localM3u = "Local M3U",
    server = "Server URL",
    username = "Username",
    password = "Password",
    loadList = "Load list",
    loading = "Loading…"
)
