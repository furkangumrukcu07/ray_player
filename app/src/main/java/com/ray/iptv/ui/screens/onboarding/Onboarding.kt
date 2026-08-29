package com.ray.iptv.ui.screens.onboarding

import android.content.Intent
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.focusable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.LiveTv
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlaylistPlay
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.outlined.Circle
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.ray.iptv.data.repo.BackupFile
import com.ray.iptv.ui.input.rayClickable

import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.delay
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

private class SourceInputState {
    var sourceName by mutableStateOf("")
    var kind by mutableIntStateOf(0)
    var server by mutableStateOf("")
    var user by mutableStateOf("")
    var pass by mutableStateOf("")
    var m3u by mutableStateOf("")
    var mac by mutableStateOf("")
    var fileUri by mutableStateOf("")

    fun hasValidInput(): Boolean {
        return when (kind) {
            0 -> m3u.isNotBlank()
            1 -> server.isNotBlank() && user.isNotBlank() && pass.isNotBlank()
            2 -> fileUri.isNotBlank()
            3 -> server.isNotBlank() && mac.isNotBlank()
            4 -> true
            else -> false
        }
    }

    fun executeLoad(vm: RayViewModel, s: SetupCopy, onLoad: () -> Unit) {
        onLoad()
        when (kind) {
            0 -> vm.addM3u(sourceName.ifBlank { "M3U" }, m3u)
            1 -> vm.addXtream(sourceName.ifBlank { "Xtream" }, server, user, pass)
            2 -> if (fileUri.isNotBlank()) vm.addLocalM3u(sourceName.ifBlank { s.localM3u }, fileUri)
            3 -> vm.addStalker(sourceName.ifBlank { "Stalker" }, server, mac)
            else -> vm.addDemoPlaylist()
        }
    }
}


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
    val sourceState = remember { SourceInputState() }
    val step = pages.getOrElse(index) { pages.last() }
    val last = index >= pages.lastIndex
    val canFinish = sources.isNotEmpty()
    val nextFocus = remember { FocusRequester() }

    LaunchedEffect(sources.size, waitingSource, sync.running, sync.error) {
        if (waitingSource && sources.isNotEmpty() && !sync.running && sync.error.isBlank()) {
            waitingSource = false
            vm.completeSetup()
        } else if (waitingSource && sync.error.isNotBlank()) {
            waitingSource = false
        }
    }

    fun goNext() {
        when {
            index < pages.lastIndex -> index++
            canFinish -> vm.completeSetup()
            sourceState.hasValidInput() -> {
                sourceState.executeLoad(vm, s) { waitingSource = true }
            }
            else -> vm.toast.value = s.needSource
        }
    }

    Box(
        Modifier.fillMaxSize()
    ) {
        RayWallpaper()
        Column(
            Modifier
                .fillMaxSize()
                .then(if (touch) Modifier.systemBarsPadding() else Modifier)
                .padding(if (touch) 12.dp else 16.dp)
                .widthIn(max = if (touch) 740.dp else 960.dp)
                .align(Alignment.Center)
        ) {
            // macOS Window Header
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 6.dp, vertical = 2.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    if (touch) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(Modifier.size(10.dp).clip(CircleShape).background(Color(0xFFFF5F56)))
                            Box(Modifier.size(10.dp).clip(CircleShape).background(Color(0xFFFFBD2E)))
                            Box(Modifier.size(10.dp).clip(CircleShape).background(Color(0xFF27C93F)))
                        }
                        Spacer(Modifier.width(2.dp))
                    }
                    Text("Ray IPTV", style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.ExtraBold), color = g.accent)
                    Box(
                        Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(Color.White.copy(alpha = 0.08f))
                            .padding(horizontal = 8.dp, vertical = 2.5.dp)
                    ) {
                        Text(s.welcome, style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold), color = g.text.copy(alpha = 0.85f))
                    }
                }
                Box(
                    Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(Color.White.copy(alpha = 0.08f))
                        .padding(horizontal = 8.dp, vertical = 2.5.dp)
                ) {
                    Text(
                        "${stepTitle(step, s)}  (${index + 1}/${pages.size})",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Medium),
                        color = g.muted
                    )
                }
            }
            Spacer(Modifier.height(8.dp))
            SetupProgressSegments(total = pages.size, current = index)
            Spacer(Modifier.height(10.dp))
            GlassPanel(
                strong = true,
                radius = 14.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                Box(
                    Modifier
                        .fillMaxSize()
                        .clipToBounds()
                        .padding(if (touch) 16.dp else 20.dp)
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
                                vm = vm,
                                state = sourceState
                            )
                        }
                    }
                }
            }
            Spacer(Modifier.height(10.dp))
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (index > 0) GlassButton(s.back) { index-- }
                else Spacer(Modifier.width(8.dp))
                GlassButton(
                    if (last && (waitingSource || sync.running)) s.loading else if (last) s.finish else s.next,
                    primary = true,
                    modifier = Modifier.focusRequester(nextFocus)
                ) {
                    if (!waitingSource && !sync.running) {
                        goNext()
                    }
                }
            }
        }
    }
}


@Composable
private fun SetupProgressSegments(total: Int, current: Int) {
    val g = LocalGlass.current
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        for (i in 0 until total) {
            val isCompleted = i <= current
            Box(
                Modifier
                    .weight(1f)
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(
                        if (isCompleted) g.accent else g.text.copy(alpha = 0.12f)
                    )
            )
        }
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
    GlassStyle.DARK,
    GlassStyle.TV_LITE,
    GlassStyle.MACOS_TV,
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
    vm: RayViewModel,
    state: SourceInputState
) {
    val ctx = LocalContext.current
    val clip = LocalClipboardManager.current
    val pickM3u = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            runCatching {
                ctx.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            state.fileUri = uri.toString()
        }
    }
    fun load() {
        if (syncing) return
        state.executeLoad(vm, s, onLoad)
    }
    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text(s.sourceHint, color = LocalGlass.current.muted, style = MaterialTheme.typography.bodyLarge)

        val tr = vm.settings.collectAsState().value.lang == AppLang.TR

        val isOnboardingRestoring by vm.isOnboardingRestoring.collectAsState()
        val restoreProgressStage by vm.onboardingRestoreProgress.collectAsState()

        val googleSignInLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            vm.handleGoogleSignInAndAutoRestore(result.data)
        }

        // Realtime Progress Indicator Dialog during Check or Restore
        if (isOnboardingRestoring || restoreProgressStage != null) {
            OnboardingRestoreProgressDialog(
                tr = tr,
                statusText = restoreProgressStage ?: if (tr) "Google hesabı kontrol ediliyor ve bulut taranıyor..." else "Checking Google account and cloud backup..."
            )
        }



        Box(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(
                    androidx.compose.ui.graphics.Brush.horizontalGradient(
                        listOf(
                            androidx.compose.ui.graphics.Color.White.copy(alpha = 0.10f),
                            androidx.compose.ui.graphics.Color.White.copy(alpha = 0.05f)
                        )
                    )
                )
                .border(1.dp, androidx.compose.ui.graphics.Color.White.copy(alpha = 0.18f), RoundedCornerShape(12.dp))
                .padding(14.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier
                        .size(38.dp)
                        .clip(androidx.compose.foundation.shape.CircleShape)
                        .background(androidx.compose.ui.graphics.Color.White),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "G",
                        color = androidx.compose.ui.graphics.Color(0xFF4285F4),
                        fontWeight = FontWeight.Black,
                        style = MaterialTheme.typography.titleLarge.copy(fontSize = 20.sp)
                    )
                }
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        if (tr) "Google ile Oturum Aç & Buluttan Yükle" else "Sign in with Google & Restore from Cloud",
                        color = androidx.compose.ui.graphics.Color.White,
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleSmall.copy(fontSize = 14.sp)
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        if (tr) "Kayıtlı listelerinizi ve ayarlarınızı buluttan otomatik geri yükleyin."
                        else "Automatically restore your saved playlists & settings from cloud.",
                        color = androidx.compose.ui.graphics.Color.White.copy(alpha = 0.72f),
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.5.sp)
                    )
                }
                Spacer(Modifier.width(8.dp))
                GlassButton(if (tr) "Oturum Aç" else "Sign In", primary = true) {
                    googleSignInLauncher.launch(vm.getGoogleSignInIntent())
                }
            }
        }
        Row(
            Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            GlassButton(if (state.kind == 0) "M3U URL ✓" else "M3U URL") { state.kind = 0 }
            GlassButton(if (state.kind == 1) "Xtream ✓" else "Xtream") { state.kind = 1 }
            GlassButton(if (state.kind == 2) "${s.m3uFile} ✓" else s.m3uFile) { state.kind = 2 }
            GlassButton(if (state.kind == 3) "Stalker ✓" else "Stalker") { state.kind = 3 }
            GlassButton(if (state.kind == 4) "${s.demo} ✓" else s.demo) { state.kind = 4 }
        }
        if (state.kind != 4) GlassField(s.listName, state.sourceName) { state.sourceName = it }
        when (state.kind) {
            0 -> {
                GlassField("M3U URL", state.m3u) { state.m3u = it }
                GlassButton(s.paste) {
                    val t = clip.getText()?.text.orEmpty()
                    if (t.isNotBlank()) state.m3u = t else vm.toast.value = s.clipboardEmpty
                }
            }
            1 -> {
                GlassField(s.server, state.server) { state.server = it }
                GlassField(s.username, state.user) { state.user = it }
                GlassField(s.password, state.pass) { state.pass = it }
            }
            2 -> {
                Text(
                    if (state.fileUri.isBlank()) s.noFile else state.fileUri.substringAfterLast('/'),
                    color = LocalGlass.current.muted,
                    style = MaterialTheme.typography.bodySmall
                )
                GlassButton(if (state.fileUri.isBlank()) s.pickFile else s.pickOther) {
                    pickM3u.launch(arrayOf("*/*"))
                }
            }
            3 -> {
                GlassField("Portal URL", state.server) { state.server = it }
                GlassField("MAC", state.mac) { state.mac = it }
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
        radius = 12.dp,
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .onFocusChanged { focused = it.isFocused }
    ) {
        Row(
            Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    title,
                    color = if (selected) Color.White else g.text,
                    fontWeight = if (selected) FontWeight.Bold else FontWeight.SemiBold,
                    style = MaterialTheme.typography.titleMedium.copy(fontSize = 15.sp)
                )
                if (subtitle.isNotBlank()) {
                    Spacer(Modifier.height(3.dp))
                    Text(
                        subtitle,
                        color = if (selected) Color.White.copy(alpha = 0.78f) else g.muted,
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp)
                    )
                }
            }
            Box(
                Modifier
                    .size(22.dp)
                    .clip(CircleShape)
                    .background(if (selected) g.accent else Color.White.copy(alpha = 0.12f))
                    .border(1.dp, if (selected) g.accent else Color.White.copy(alpha = 0.25f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                if (selected) {
                    Icon(
                        imageVector = Icons.Filled.Check,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }
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
        radius = 12.dp,
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .onFocusChanged { focused = it.isFocused }
    ) {
        Column(Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier
                        .size(32.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (selected) g.accent.copy(alpha = 0.35f) else Color.White.copy(alpha = 0.08f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        icon,
                        contentDescription = null,
                        tint = if (selected) Color.White else g.muted,
                        modifier = Modifier.size(18.dp)
                    )
                }
                Spacer(Modifier.width(10.dp))
                Text(
                    title,
                    color = if (selected) Color.White else g.text,
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleMedium.copy(fontSize = 15.sp),
                    modifier = Modifier.weight(1f)
                )
                Box(
                    Modifier
                        .size(20.dp)
                        .clip(CircleShape)
                        .background(if (selected) g.accent else Color.White.copy(alpha = 0.12f))
                        .border(1.dp, if (selected) g.accent else Color.White.copy(alpha = 0.25f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    if (selected) {
                        Icon(
                            imageVector = Icons.Filled.Check,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(13.dp)
                        )
                    }
                }
            }
            Spacer(Modifier.height(8.dp))
            Text(
                subtitle,
                color = if (selected) Color.White.copy(alpha = 0.80f) else g.muted,
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp)
            )
        }
    }
}

@Composable
fun GlassField(label: String, value: String, onValue: (String) -> Unit) {
    val g = LocalGlass.current
    var focused by remember { mutableStateOf(false) }
    Column(Modifier.fillMaxWidth()) {
        Text(
            label,
            color = if (focused) g.accent else g.muted,
            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
        )
        Spacer(Modifier.height(5.dp))
        GlassPanel(
            radius = 10.dp,
            focused = focused,
            scaleOnFocus = false,
            fillAlpha = 0.85f,
            modifier = Modifier.fillMaxWidth()
        ) {
            BasicTextField(
                value = value,
                onValueChange = onValue,
                textStyle = MaterialTheme.typography.bodyMedium.copy(color = Color.White, fontSize = 14.sp),
                cursorBrush = SolidColor(g.accent),
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .onFocusChanged { focused = it.isFocused }
                    .padding(horizontal = 14.dp, vertical = 11.dp)
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

@Composable
private fun OnboardingCloudRestoreDialog(
    tr: Boolean,
    backup: BackupFile,
    onRestore: () -> Unit,
    onDismiss: () -> Unit
) {
    val restoreFocus = remember { FocusRequester() }
    var restoreFocused by remember { mutableStateOf(false) }
    var dismissFocused by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        delay(150)
        runCatching { restoreFocus.requestFocus() }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        BackHandler { onDismiss() }

        Box(
            Modifier
                .width(480.dp)
                .clip(RoundedCornerShape(24.dp))
                .background(Color(0xFF0E281F).copy(alpha = 0.98f))
                .border(1.dp, Color(0xFF34D399).copy(alpha = 0.40f), RoundedCornerShape(24.dp))
                .padding(22.dp)
        ) {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        Modifier
                            .size(44.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFF22D3EE).copy(alpha = 0.15f))
                            .border(1.dp, Color(0xFF22D3EE).copy(alpha = 0.35f), RoundedCornerShape(12.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Filled.CloudDownload,
                            contentDescription = null,
                            tint = Color(0xFF22D3EE),
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(
                            text = if (tr) "Google Bulut Yedeği Bulundu" else "Google Cloud Backup Found",
                            color = Color.White,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = if (tr) "Hesabınızdaki kayıtlı veriler ve oynatma listeleri:" else "Saved playlists & settings in your account:",
                            color = Color.White.copy(alpha = 0.65f),
                            fontSize = 12.sp
                        )
                    }
                }

                Spacer(Modifier.height(16.dp))

                // Backup Summary Items
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color.White.copy(alpha = 0.05f))
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OnboardingBackupRowItem(
                        icon = Icons.Filled.PlaylistPlay,
                        title = if (tr) "Oynatma Listeleri" else "Playlists",
                        value = "${backup.sources.size} ${if (tr) "Liste" else "Sources"}" +
                                if (backup.sources.isNotEmpty()) " (${backup.sources.take(2).joinToString { it.name }})" else ""
                    )
                    OnboardingBackupRowItem(
                        icon = Icons.Filled.Tune,
                        title = if (tr) "Uygulama & Sistem Ayarları" else "App & System Settings",
                        value = if (tr) "Tüm Yapılandırma" else "All Configuration"
                    )
                    OnboardingBackupRowItem(
                        icon = Icons.Filled.Favorite,
                        title = if (tr) "Favoriler & Geçmiş" else "Favorites & History",
                        value = "${backup.favorites.size} ${if (tr) "Favori" else "Favorites"} · ${backup.progress.size} ${if (tr) "Kaldığın Yer" else "Progress"}"
                    )
                    if (backup.profiles.isNotEmpty()) {
                        OnboardingBackupRowItem(
                            icon = Icons.Filled.Person,
                            title = if (tr) "Kullanıcı Profilleri" else "Profiles",
                            value = "${backup.profiles.size} ${if (tr) "Profil" else "Profiles"}"
                        )
                    }
                }

                Spacer(Modifier.height(20.dp))

                // Action Buttons
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Restore & Start Button
                    Box(
                        Modifier
                            .weight(1.3f)
                            .height(46.dp)
                            .focusRequester(restoreFocus)
                            .onFocusChanged { restoreFocused = it.isFocused }
                            .focusable()
                            .clip(RoundedCornerShape(14.dp))
                            .background(
                                if (restoreFocused) Color(0xFF059669) else Color(0xFF10B981),
                                RoundedCornerShape(14.dp)
                            )
                            .border(
                                if (restoreFocused) 2.5.dp else 1.dp,
                                if (restoreFocused) Color.White else Color(0xFF34D399),
                                RoundedCornerShape(14.dp)
                            )
                            .rayClickable(onClick = onRestore),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (tr) "Geri Yükle & Başlat" else "Restore & Start",
                            color = Color.White,
                            fontSize = 13.5.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    // Dismiss Button
                    Box(
                        Modifier
                            .weight(1f)
                            .height(46.dp)
                            .onFocusChanged { dismissFocused = it.isFocused }
                            .focusable()
                            .clip(RoundedCornerShape(14.dp))
                            .background(
                                if (dismissFocused) Color.White.copy(alpha = 0.20f) else Color.White.copy(alpha = 0.08f),
                                RoundedCornerShape(14.dp)
                            )
                            .border(
                                if (dismissFocused) 2.dp else 1.dp,
                                if (dismissFocused) Color.White else Color.White.copy(alpha = 0.15f),
                                RoundedCornerShape(14.dp)
                            )
                            .rayClickable(onClick = onDismiss),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (tr) "Manuel Ekle" else "Add Manually",
                            color = Color.White.copy(alpha = 0.85f),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun OnboardingBackupRowItem(
    icon: ImageVector,
    title: String,
    value: String
) {
    Row(
        Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            icon,
            contentDescription = null,
            tint = Color(0xFF34D399),
            modifier = Modifier.size(18.dp)
        )
        Spacer(Modifier.width(10.dp))
        Text(
            text = title,
            color = Color.White.copy(alpha = 0.85f),
            fontSize = 12.5.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.weight(1f)
        )
        Text(
            text = value,
            color = Color(0xFF22D3EE),
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun OnboardingRestoreProgressDialog(
    tr: Boolean,
    statusText: String
) {
    Dialog(
        onDismissRequest = {},
        properties = DialogProperties(
            dismissOnBackPress = false,
            dismissOnClickOutside = false,
            usePlatformDefaultWidth = false
        )
    ) {
        Box(
            Modifier
                .width(420.dp)
                .clip(RoundedCornerShape(22.dp))
                .background(Color(0xFF0E281F).copy(alpha = 0.98f))
                .border(1.2.dp, Color(0xFF22D3EE).copy(alpha = 0.50f), RoundedCornerShape(22.dp))
                .padding(26.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Box(
                    Modifier
                        .size(52.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF22D3EE).copy(alpha = 0.15f))
                        .border(1.dp, Color(0xFF22D3EE).copy(alpha = 0.40f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Filled.Sync,
                        contentDescription = null,
                        tint = Color(0xFF22D3EE),
                        modifier = Modifier.size(28.dp)
                    )
                }

                Text(
                    text = if (tr) "Google Bulut Geri Yükleme" else "Google Cloud Restore",
                    color = Color.White,
                    fontSize = 16.5.sp,
                    fontWeight = FontWeight.Bold
                )

                Text(
                    text = statusText,
                    color = Color(0xFF34D399),
                    fontSize = 13.5.sp,
                    fontWeight = FontWeight.SemiBold,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )

                Text(
                    text = if (tr) "Lütfen bekleyin, ayarlarınız ve listeleriniz hazırlanıyor..." else "Please wait, preparing your playlists and settings...",
                    color = Color.White.copy(alpha = 0.50f),
                    fontSize = 11.5.sp,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
            }
        }
    }
}

