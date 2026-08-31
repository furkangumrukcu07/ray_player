package com.ray.iptv.ui.screens.player

import com.ray.iptv.ui.player.playerGestureDrag
import com.ray.iptv.ui.player.unifiedPlayerGestures

import android.graphics.Typeface
import android.graphics.Color as AndroidColor
import android.util.TypedValue
import android.view.KeyEvent
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.activity.compose.BackHandler
import androidx.annotation.OptIn
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.ui.graphics.Brush
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.drag
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.systemGestures
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.scale
import androidx.compose.ui.platform.LocalContext
import coil.request.ImageRequest
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Audiotrack
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.ClosedCaption
import androidx.compose.material.icons.filled.FastForward
import androidx.compose.material.icons.filled.FastRewind
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.FitScreen
import androidx.compose.material.icons.filled.HighQuality
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.StayCurrentPortrait
import com.ray.iptv.ui.player.setPortrait
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.ViewTimeline
import androidx.compose.material.icons.automirrored.filled.ViewSidebar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import com.ray.iptv.ui.input.rayFocusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import androidx.media3.common.util.UnstableApi
import androidx.media3.ui.CaptionStyleCompat
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import androidx.tv.material3.Icon
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import coil.compose.AsyncImage
import com.ray.iptv.data.local.CategoryEntity
import com.ray.iptv.data.local.ChannelEntity
import com.ray.iptv.data.local.EpgEntity
import com.ray.iptv.data.repo.AspectMode
import com.ray.iptv.data.repo.PlaybackEngine
import com.ray.iptv.data.repo.StreamFormat
import com.ray.iptv.player.MediaKitSurfaceHost
import com.ray.iptv.player.RayPlayer
import com.ray.iptv.player.StreamHints
import com.ray.iptv.player.TrackOption
import com.ray.iptv.player.XtreamStreamUrls
import com.ray.iptv.ui.Playback
import com.ray.iptv.ui.NextUpPrompt
import com.ray.iptv.ui.RayViewModel
import com.ray.iptv.ui.glass.GlassPanel
import com.ray.iptv.ui.i18n.Copy
import com.ray.iptv.ui.input.LocalTouchUi
import com.ray.iptv.ui.input.playerTouch
import com.ray.iptv.ui.input.isTelevisionDevice
import com.ray.iptv.ui.screens.guide.GuideScreen
import com.ray.iptv.ui.theme.LocalGlass
import kotlinx.coroutines.delay

@OptIn(UnstableApi::class)
@Composable
fun RayPlayerRoute(vm: RayViewModel, strings: Copy) {
    val playback = vm.playback.collectAsState().value ?: return
    val settings by vm.settings.collectAsState()
    val zap by vm.zapBuffer.collectAsState()
    val channels by vm.liveChannels.collectAsState()
    val liveCats by vm.liveCategories.collectAsState()
    val profile by vm.activeProfile.collectAsState()
    val kids = profile?.isKids == true
    val categories = remember(liveCats, settings.hideAdult, settings.hideLocked, kids) {
        vm.visibleCats(liveCats)
    }
    val favs by vm.favorites.collectAsState()
    val nowNext by vm.nowNext.collectAsState()
    val pendingNext by vm.pendingNext.collectAsState()
    PlayerScreen(
        playback = playback,
        rayPlayer = vm.player,
        zap = zap,
        copy = strings,
        aspect = settings.aspect,
        channels = channels,
        allChannels = channels,
        categories = categories,
        loadNowMap = { ids -> vm.nowMap(ids) },
        now = nowNext.first,
        next = nowNext.second,
        onBack = vm::backFromPlayer,
        onDigit = vm::zapDigit,
        onZapUp = { vm.zapRelative(-1) },
        onZapDown = { vm.zapRelative(1) },
        onRewind = vm::rewindLive,
        onChannel = vm::playChannel,
        onGuideCatchup = { ch, p -> vm.playCatchup(ch, p) },
        loadGuide = { ch ->
            val now = System.currentTimeMillis()
            vm.guideFor(ch.id, now - 6L * 3600_000, now + 18L * 3600_000)
        },
        onImportGuide = vm::refreshGuide,
        onAspect = {
            vm.setAspect(
                when (settings.aspect) {
                    AspectMode.FIT -> AspectMode.ZOOM
                    AspectMode.ZOOM -> AspectMode.FILL
                    AspectMode.FILL -> AspectMode.STRETCH
                    AspectMode.STRETCH -> AspectMode.FIT
                }
            )
        },
        onSpeed = {
            val speeds = listOf(0.75f, 1f, 1.25f, 1.5f, 2f)
            val i = speeds.indexOfFirst { kotlin.math.abs(it - settings.speed) < 0.01f }
            vm.setSpeed(speeds[(i + 1) % speeds.size])
        },
        onExternal = vm::openExternal,
        favorite = favs.any { it.mediaId == playback.mediaId },
        onFavorite = { vm.toggleFav(playback.mediaId, playback.kind) },
        speed = settings.speed,
        pendingNext = pendingNext,
        onConfirmNext = vm::confirmNextEpisode,
        onCancelNext = vm::cancelNextEpisode,
        onSkipIntro = vm::skipIntro,
        introTargetMs = vm.introTargetMs(),
        onLearnIntro = vm::learnIntroSkip,
        osdHideMs = maxOf(7, settings.osdHideSeconds) * 1000L,
        osdScale = when (settings.osdSizeTier.coerceIn(0, 3)) {
            0 -> 0.85f
            1 -> 1f
            2 -> 1.18f
            else -> 1.35f
        },
        osdOpacity = settings.osdOpacity,
        subtitleSize = settings.subtitleSize,
        subtitleOutline = settings.subtitleOutline,
        subtitleColor = settings.subtitleColor,
        subtitleFont = settings.subtitleFont,
        streamFormat = settings.effectiveStreamFormat(),
        onSelectSubtitle = vm::pickSubtitle
    )
}

@OptIn(UnstableApi::class)
@Composable
fun PlayerScreen(
    playback: Playback,
    rayPlayer: RayPlayer,
    zap: String,
    copy: Copy,
    aspect: AspectMode,
    channels: List<ChannelEntity>,
    allChannels: List<ChannelEntity> = emptyList(),
    categories: List<CategoryEntity> = emptyList(),
    loadNowMap: suspend (List<String>) -> Map<String, EpgEntity> = { emptyMap() },
    now: EpgEntity?,
    next: EpgEntity?,
    onBack: () -> Unit,
    onDigit: (Char) -> Unit,
    onZapUp: () -> Unit,
    onZapDown: () -> Unit,
    onRewind: (Long) -> Unit,
    onChannel: (ChannelEntity) -> Unit,
    onGuideCatchup: (ChannelEntity, EpgEntity) -> Unit,
    loadGuide: suspend (ChannelEntity) -> List<EpgEntity>,
    onImportGuide: () -> Unit,
    onAspect: () -> Unit,
    onSpeed: () -> Unit,
    onExternal: () -> Unit,
    favorite: Boolean = false,
    onFavorite: () -> Unit = {},
    speed: Float = 1f,
    pendingNext: NextUpPrompt? = null,
    onConfirmNext: () -> Unit = {},
    onCancelNext: () -> Unit = {},
    onSkipIntro: () -> Unit = {},
    introTargetMs: Long = 0L,
    onLearnIntro: (Long) -> Unit = {},
    osdHideMs: Long = 7_000L,
    osdScale: Float = 1f,
    osdOpacity: Int = 70,
    subtitleSize: Int = 22,
    subtitleOutline: Boolean = true,
    subtitleColor: String = "white",
    subtitleFont: String = "sans",
    streamFormat: StreamFormat = StreamFormat.AUTO,
    onSelectSubtitle: (String) -> Unit = {}
) {
    val g = LocalGlass.current
    val touch = LocalTouchUi.current
    val context = androidx.compose.ui.platform.LocalContext.current
    val st by rayPlayer.state.collectAsState()
    val gestureState = com.ray.iptv.ui.player.rememberPlayerGestureState()
    var overlay by remember { mutableStateOf(true) }
    var peek by remember { mutableStateOf(false) }
    var guide by remember { mutableStateOf(false) }
    var tick by remember { mutableStateOf(rayPlayer.snapshot()) }
    var skipMs by remember(playback.mediaId) { mutableLongStateOf(introTargetMs) }
    var scrubbing by remember { mutableStateOf(false) }
    var sheet by remember { mutableStateOf<OsdSheet?>(null) }
    var hideGen by remember { mutableIntStateOf(0) }
    val playPauseFocusRequester = remember { FocusRequester() }
    val seekbarFocusRequester = remember { FocusRequester() }
    val screenFocusRequester = remember { FocusRequester() }
    var isSeekbarFocused by remember { mutableStateOf(false) }
    val hideAfterMs = osdHideMs.coerceAtLeast(7_000L)
    fun showOsd() {
        overlay = true
        hideGen += 1
    }
    LaunchedEffect(overlay, peek, guide) {
        if (overlay) {
            delay(50)
            runCatching { playPauseFocusRequester.requestFocus() }
        } else if (!peek && !guide) {
            delay(50)
            runCatching { screenFocusRequester.requestFocus() }
        }
    }
    LaunchedEffect(overlay, peek, guide, scrubbing, sheet, hideGen, hideAfterMs) {
        if (overlay && !peek && !guide && !scrubbing && sheet == null) {
            delay(hideAfterMs)
            overlay = false
        }
    }
    LaunchedEffect(playback.url) {
        showOsd()
        sheet = null
    }
    LaunchedEffect(pendingNext?.title) {
        if (pendingNext == null) return@LaunchedEffect
        delay(5_000)
        onConfirmNext()
    }
    LaunchedEffect(overlay, peek, guide) {
        if (!overlay && !peek && !guide) return@LaunchedEffect
        while (true) {
            tick = rayPlayer.snapshot()
            delay(500)
        }
    }
    val resize = when (aspect) {
        AspectMode.FIT -> AspectRatioFrameLayout.RESIZE_MODE_FIT
        AspectMode.ZOOM -> AspectRatioFrameLayout.RESIZE_MODE_ZOOM
        AspectMode.FILL, AspectMode.STRETCH -> AspectRatioFrameLayout.RESIZE_MODE_FILL
    }
    val leavePlayer = {
        peek = false
        guide = false
        overlay = false
        onBack()
    }
    var lastBackHandledMs by remember { mutableLongStateOf(0L) }
    val handleBack: () -> Boolean = {
        val now = System.currentTimeMillis()
        if (now - lastBackHandledMs < 400L) {
            true
        } else {
            lastBackHandledMs = now
            when {
                sheet != null -> {
                    sheet = null
                    showOsd()
                }
                peek -> {
                    peek = false
                    showOsd()
                }
                guide -> {
                    guide = false
                    showOsd()
                }
                overlay -> {
                    overlay = false
                }
                else -> {
                    leavePlayer()
                }
            }
            true
        }
    }
    BackHandler {
        handleBack()
    }
    Box(
        Modifier
            .fillMaxSize()
            .focusRequester(screenFocusRequester)
            .focusable()
            .onPreviewKeyEvent { ev ->
                val code = ev.nativeKeyEvent.keyCode
                if (code == KeyEvent.KEYCODE_BACK) {
                    if (ev.nativeKeyEvent.action == KeyEvent.ACTION_UP) {
                        handleBack()
                    }
                    return@onPreviewKeyEvent true
                }
                if (ev.nativeKeyEvent.action != KeyEvent.ACTION_DOWN) return@onPreviewKeyEvent false
                if (overlay) {
                    showOsd()
                    if ((code == KeyEvent.KEYCODE_DPAD_UP || code == KeyEvent.KEYCODE_CHANNEL_UP) && (playback.live || playback.kind == "LIVE") && sheet == null && !guide && !peek) {
                        onZapUp()
                        return@onPreviewKeyEvent true
                    }
                    if ((code == KeyEvent.KEYCODE_DPAD_DOWN || code == KeyEvent.KEYCODE_CHANNEL_DOWN) && (playback.live || playback.kind == "LIVE") && sheet == null && !guide && !peek) {
                        onZapDown()
                        return@onPreviewKeyEvent true
                    }
                    if (code == KeyEvent.KEYCODE_MEDIA_NEXT && (playback.live || playback.kind == "LIVE") && sheet == null && !guide && !peek) {
                        onZapUp()
                        return@onPreviewKeyEvent true
                    }
                    if (code == KeyEvent.KEYCODE_MEDIA_PREVIOUS && (playback.live || playback.kind == "LIVE") && sheet == null && !guide && !peek) {
                        onZapDown()
                        return@onPreviewKeyEvent true
                    }
                    if (code in KeyEvent.KEYCODE_0..KeyEvent.KEYCODE_9 && (playback.live || playback.kind == "LIVE") && sheet == null && !guide && !peek) {
                        onDigit('0' + (code - KeyEvent.KEYCODE_0))
                        return@onPreviewKeyEvent true
                    }
                    if (code == KeyEvent.KEYCODE_DPAD_UP && !playback.live && playback.kind != "LIVE" && sheet == null && !guide && !peek && !isSeekbarFocused) {
                        runCatching { seekbarFocusRequester.requestFocus() }
                        return@onPreviewKeyEvent true
                    }
                    return@onPreviewKeyEvent false
                }
                when (code) {
                    KeyEvent.KEYCODE_DPAD_UP, KeyEvent.KEYCODE_CHANNEL_UP -> {
                        if (peek) return@onPreviewKeyEvent false
                        if (playback.live || playback.kind == "LIVE") {
                            showOsd(); onZapUp(); true
                        } else {
                            showOsd(); true
                        }
                    }
                    KeyEvent.KEYCODE_DPAD_DOWN, KeyEvent.KEYCODE_CHANNEL_DOWN -> {
                        if (peek) return@onPreviewKeyEvent false
                        if (playback.live || playback.kind == "LIVE") {
                            showOsd(); onZapDown(); true
                        } else {
                            showOsd(); true
                        }
                    }
                    KeyEvent.KEYCODE_DPAD_LEFT -> {
                        if (peek) return@onPreviewKeyEvent false
                        if (playback.live || playback.kind == "LIVE") {
                            peek = true
                            overlay = false
                        } else {
                            showOsd()
                            onRewind(-SkipMs)
                        }
                        true
                    }
                    KeyEvent.KEYCODE_DPAD_RIGHT -> {
                        if (peek) return@onPreviewKeyEvent false
                        if (playback.live || playback.kind == "LIVE") {
                            showOsd()
                        } else {
                            showOsd()
                            onRewind(SkipMs)
                        }
                        true
                    }
                    KeyEvent.KEYCODE_MEDIA_REWIND, KeyEvent.KEYCODE_MEDIA_STEP_BACKWARD -> {
                        showOsd(); onRewind(-SkipMs); true
                    }
                    KeyEvent.KEYCODE_MEDIA_FAST_FORWARD, KeyEvent.KEYCODE_MEDIA_STEP_FORWARD -> {
                        showOsd(); onRewind(SkipMs); true
                    }
                    KeyEvent.KEYCODE_MEDIA_PLAY -> {
                        showOsd(); rayPlayer.resume(); true
                    }
                    KeyEvent.KEYCODE_MEDIA_PAUSE -> {
                        showOsd(); rayPlayer.pause(); true
                    }
                    KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE, KeyEvent.KEYCODE_HEADSETHOOK -> {
                        showOsd()
                        if (st.playing) rayPlayer.pause() else rayPlayer.resume()
                        true
                    }
                    KeyEvent.KEYCODE_DPAD_CENTER, KeyEvent.KEYCODE_ENTER, KeyEvent.KEYCODE_NUMPAD_ENTER -> {
                        if (peek) return@onPreviewKeyEvent false
                        if (!overlay) {
                            showOsd()
                        } else {
                            if (st.playing) rayPlayer.pause() else rayPlayer.resume()
                        }
                        true
                    }
                    KeyEvent.KEYCODE_MEDIA_NEXT -> {
                        if (playback.live || playback.kind == "LIVE") {
                            showOsd(); onZapUp(); true
                        } else {
                            showOsd(); true
                        }
                    }
                    KeyEvent.KEYCODE_MEDIA_PREVIOUS -> {
                        if (playback.live || playback.kind == "LIVE") {
                            showOsd(); onZapDown(); true
                        } else {
                            showOsd(); true
                        }
                    }
                    KeyEvent.KEYCODE_CAPTIONS -> {
                        sheet = OsdSheet.SUBS; showOsd(); true
                    }
                    KeyEvent.KEYCODE_PROG_RED -> {
                        sheet = OsdSheet.AUDIO; showOsd(); true
                    }
                    KeyEvent.KEYCODE_PROG_GREEN -> {
                        sheet = OsdSheet.SUBS; showOsd(); true
                    }
                    KeyEvent.KEYCODE_PROG_YELLOW -> {
                        sheet = OsdSheet.QUALITY; showOsd(); true
                    }
                    KeyEvent.KEYCODE_GUIDE, KeyEvent.KEYCODE_INFO, KeyEvent.KEYCODE_WINDOW -> {
                        guide = !guide; showOsd(); true
                    }
                    in KeyEvent.KEYCODE_0..KeyEvent.KEYCODE_9 -> {
                        showOsd()
                        onDigit('0' + (code - KeyEvent.KEYCODE_0))
                        true
                    }
                    else -> false
                }
            }
    ) {
        key(st.engine) {
            if (st.engine == PlaybackEngine.MEDIA_KIT) {
                AndroidView(
                    factory = { ctx ->
                        MediaKitSurfaceHost(ctx).apply {
                            player = rayPlayer
                            keepScreenOn = true
                            layoutParams = FrameLayout.LayoutParams(
                                ViewGroup.LayoutParams.MATCH_PARENT,
                                ViewGroup.LayoutParams.MATCH_PARENT
                            )
                        }
                    },
                    update = { view ->
                        view.player = rayPlayer
                        view.keepScreenOn = true
                        rayPlayer.applyVideoLayout(
                            fill = aspect == AspectMode.FILL || aspect == AspectMode.STRETCH,
                            zoom = aspect == AspectMode.ZOOM
                        )
                    },
                    onRelease = { view ->
                        view.player?.detachMpvSurface()
                        view.player = null
                    },
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                AndroidView(
                    factory = { ctx ->
                        PlayerView(ctx).apply {
                            useController = false
                            keepScreenOn = true
                            resizeMode = resize
                            player = rayPlayer.exo
                            isClickable = false
                            isFocusable = false
                            isFocusableInTouchMode = false
                            layoutParams = FrameLayout.LayoutParams(
                                ViewGroup.LayoutParams.MATCH_PARENT,
                                ViewGroup.LayoutParams.MATCH_PARENT
                            )
                        }
                    },
                    update = { view ->
                        view.keepScreenOn = true
                        view.resizeMode = resize
                        view.player = rayPlayer.exo

                        view.subtitleView?.setFixedTextSize(TypedValue.COMPLEX_UNIT_SP, subtitleSize.toFloat())
                        view.subtitleView?.setStyle(
                            CaptionStyleCompat(
                                when (subtitleColor.lowercase()) {
                                    "yellow" -> AndroidColor.YELLOW
                                    "cyan" -> AndroidColor.CYAN
                                    "green" -> AndroidColor.GREEN
                                    "orange" -> AndroidColor.rgb(255, 152, 0)
                                    "pink" -> AndroidColor.MAGENTA
                                    else -> AndroidColor.WHITE
                                },
                                AndroidColor.TRANSPARENT,
                                AndroidColor.TRANSPARENT,
                                if (subtitleOutline) CaptionStyleCompat.EDGE_TYPE_OUTLINE else CaptionStyleCompat.EDGE_TYPE_NONE,
                                AndroidColor.BLACK,
                                when (subtitleFont) {
                                    "serif" -> Typeface.SERIF
                                    "mono" -> Typeface.MONOSPACE
                                    else -> Typeface.SANS_SERIF
                                }
                            )
                        )
                    },
                    onRelease = { it.player = null },
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
        val inPip = com.ray.iptv.LocalPipMode.current
        if (!inPip) {
            val chromeInsets = if (touch) WindowInsets.systemBars.union(WindowInsets.systemGestures) else WindowInsets(0.dp, 0.dp, 0.dp, 0.dp)
            if (!guide && !overlay && !peek) {
                Box(
                    Modifier
                        .fillMaxSize()
                        .windowInsetsPadding(chromeInsets)
                        .unifiedPlayerGestures(
                            state = gestureState,
                            onTap = { showOsd() },
                            onSwipeLeft = {
                                if (playback.kind == "LIVE" || playback.live) { peek = true; overlay = false }
                                else { showOsd(); onRewind(-SkipMs) }
                            },
                            onSwipeRight = { showOsd(); onRewind(SkipMs) }
                        )
                )
            }
            com.ray.iptv.ui.player.PlayerGlassLevelOverlay(gestureState)
            if (peek) {
                QuickChannelMenu(
                    channels = allChannels.ifEmpty { channels },
                    categories = categories,
                    playingId = playback.mediaId,
                    playingUrl = playback.url,
                    copy = copy,
                    loadNowMap = loadNowMap,
                    onPick = onChannel,
                    onClose = { peek = false; showOsd() }
                )
            }
            if (guide) {
                Box(
                    Modifier
                        .fillMaxSize()
                        .padding(24.dp)
                ) {
                    GlassPanel(strong = true, radius = 20.dp, modifier = Modifier.fillMaxSize()) {
                        Box(Modifier.padding(16.dp)) {
                            GuideScreen(
                                copy = copy,
                                channels = channels,
                                load = loadGuide,
                                onPlay = onChannel,
                                onCatchup = onGuideCatchup,
                                onImport = onImportGuide
                            )
                        }
                    }
                }
            }
            val skipVisible = !playback.live && skipMs > 0L &&
                tick.position in 4_000L until skipMs && pendingNext == null && !guide && !peek
            if (skipVisible) {
                Box(Modifier.fillMaxSize().padding(end = 18.dp, bottom = if (overlay) 140.dp else 16.dp), contentAlignment = Alignment.BottomEnd) {
                    GlassPanel(strong = true, radius = 28.dp, onClick = onSkipIntro) {
                        Row(
                            Modifier.padding(horizontal = 18.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(Icons.Filled.FastForward, null, tint = Color.White, modifier = Modifier.size(18.dp))
                            Text(copy.skipIntro, color = Color.White, fontWeight = FontWeight.ExtraBold, style = MaterialTheme.typography.labelLarge)
                        }
                    }
                }
            }
            if (pendingNext != null && !guide && !peek) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.BottomCenter) {
                    GlassPanel(strong = true, radius = 20.dp, modifier = Modifier.padding(24.dp).fillMaxWidth(0.55f)) {
                        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Text(
                                copy.nextEpisode,
                                color = Color.White,
                                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                            )
                            Text(pendingNext.title, color = Color.White.copy(alpha = 0.82f), maxLines = 2, overflow = TextOverflow.Ellipsis)
                            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                GlassPanel(strong = true, accentFill = true, radius = 10.dp, onClick = onConfirmNext) {
                                    Text(copy.playNow, color = Color.White, modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp), fontWeight = FontWeight.Bold)
                                }
                                GlassPanel(strong = true, radius = 10.dp, onClick = onCancelNext) {
                                    Text(copy.cancel, color = Color.White, modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp))
                                }
                            }
                        }
                    }
                }
            }
            // 📺 Network Dalgalanması & Tamponlama Kanal Logosu Efekti (Buffering Pulse Logo Overlay)
            BufferingLogoIndicator(
                playback = playback,
                channel = channels.find { it.id == playback.mediaId },
                buffering = tick.buffering && !guide && !peek
            )
            if ((overlay || st.error.isNotBlank() || zap.isNotBlank()) && !guide && !peek) {
                Box(
                    Modifier
                        .fillMaxSize()
                        .windowInsetsPadding(chromeInsets)
                        .graphicsLayer {
                            scaleX = osdScale
                            scaleY = osdScale
                            transformOrigin = TransformOrigin(0.5f, 1f)
                        }
                ) {
                    Box(
                        Modifier
                            .fillMaxSize()
                            .unifiedPlayerGestures(
                                state = gestureState,
                                onTap = { overlay = false },
                                onSwipeLeft = {
                                    if (playback.kind == "LIVE" || playback.live) {
                                        peek = true
                                        overlay = false
                                    } else {
                                        showOsd()
                                        onRewind(-SkipMs)
                                    }
                                },
                                onSwipeRight = { showOsd(); onRewind(SkipMs) }
                            )
                    )
                    if (zap.isNotBlank()) {
                        GlassPanel(
                            strong = true,
                            radius = 14.dp,
                            modifier = Modifier.align(Alignment.TopStart).padding(12.dp)
                        ) {
                            Text(" $zap ", color = g.accent, style = MaterialTheme.typography.headlineLarge, modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp))
                        }
                    }
                    Column(
                        Modifier
                            .fillMaxWidth()
                            .align(Alignment.BottomCenter)
                            .padding(horizontal = if (touch) 12.dp else 16.dp, vertical = if (touch) 12.dp else 4.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        if (st.error.isNotBlank()) {
                            GlassPanel(strong = true, radius = 16.dp) {
                                Text(st.error, color = g.danger, modifier = Modifier.padding(16.dp), style = MaterialTheme.typography.bodyLarge)
                            }
                        }
                        if (!playback.live && playback.kind != "LIVE" && tick.duration > 0) {
                            VodSeekRow(
                                positionMs = tick.position,
                                durationMs = tick.duration,
                                buffering = tick.buffering,
                                fillAlpha = (osdOpacity.coerceIn(20, 100)) / 100f,
                                focusRequester = seekbarFocusRequester,
                                onFocusChanged = { isSeekbarFocused = it },
                                onSeek = { ms ->
                                    showOsd()
                                    rayPlayer.seek(ms)
                                },
                                onScrub = { active ->
                                    scrubbing = active
                                    if (active) showOsd()
                                },
                                onDownToPlayPause = {
                                    showOsd()
                                    runCatching { playPauseFocusRequester.requestFocus() }
                                },
                                onTogglePlayPause = {
                                    showOsd()
                                    if (tick.playing) rayPlayer.pause() else rayPlayer.resume()
                                },
                                onCloseOsd = {
                                    overlay = false
                                }
                            )
                        }
                        MinaOsdBar(
                            playback = playback,
                            channel = channels.find { it.id == playback.mediaId },
                            now = now,
                            videoSize = tick.videoSize,
                            engine = st.engine,
                            favorite = favorite,
                            playing = tick.playing,
                            speed = speed,
                            streamFormat = streamFormat,
                            fillAlpha = (osdOpacity.coerceIn(20, 100)) / 100f,
                            playPauseFocus = playPauseFocusRequester,
                            onRewind = {
                                showOsd()
                                if (playback.live) onZapUp() else onRewind(-SkipMs)
                            },
                            onPlayPause = {
                                showOsd()
                                if (tick.playing) rayPlayer.pause() else rayPlayer.resume()
                            },
                            onForward = {
                                showOsd()
                                if (playback.live) onZapDown() else {
                                    if (tick.position < 180_000L) {
                                        val at = tick.position + SkipMs
                                        onLearnIntro(at)
                                        skipMs = at
                                    }
                                    onRewind(SkipMs)
                                }
                            },
                            onChannels = {
                                if (playback.kind == "LIVE" || playback.live) {
                                    overlay = false
                                    peek = true
                                }
                            },
                            onAspect = { showOsd(); onAspect() },
                            onEpg = {
                                if (playback.live || playback.kind == "LIVE") {
                                    overlay = false
                                    guide = true
                                }
                            },
                            onSubtitles = {
                                showOsd()
                                sheet = OsdSheet.SUBS
                            },
                            onFavorite = { showOsd(); onFavorite() },
                            onQuality = {
                                showOsd()
                                sheet = OsdSheet.QUALITY
                            },
                            onAudio = {
                                showOsd()
                                sheet = OsdSheet.AUDIO
                            },
                            onSpeed = { showOsd(); onSpeed() },
                            onExternal = { showOsd(); onExternal() },
                            onPortrait = if (touch && !context.isTelevisionDevice()) ({
                                showOsd()
                                context.setPortrait()
                            }) else null
                        )
                    }
                    val openSheet = sheet
                    if (openSheet != null) {
                        val tracks = when (openSheet) {
                            OsdSheet.QUALITY -> {
                                val video = st.videoTracks
                                if (st.engine == PlaybackEngine.MEDIA_KIT) {
                                    listOf(TrackOption("auto", copy.autoQuality, video.none { it.selected })) + video
                                } else video
                            }
                            OsdSheet.AUDIO -> st.audioTracks
                            OsdSheet.SUBS -> listOf(TrackOption("no", copy.trackOff, st.textTracks.none { it.selected })) + st.textTracks
                        }
                        val title = when (openSheet) {
                            OsdSheet.QUALITY -> copy.quality
                            OsdSheet.AUDIO -> copy.audio
                            OsdSheet.SUBS -> copy.subs
                        }
                        val empty = when (openSheet) {
                            OsdSheet.QUALITY -> copy.qualityNone
                            OsdSheet.AUDIO -> copy.qualityNone
                            OsdSheet.SUBS -> copy.trackOff
                        }
                        OsdTrackSheet(
                            title = title,
                            empty = empty,
                            tracks = tracks,
                            onPick = { id ->
                                when (openSheet) {
                                    OsdSheet.QUALITY -> rayPlayer.selectVideo(id)
                                    OsdSheet.AUDIO -> rayPlayer.selectAudio(id)
                                    OsdSheet.SUBS -> onSelectSubtitle(id)
                                }
                                sheet = null
                                showOsd()
                            },
                            onDismiss = { sheet = null }
                        )
                    }
                }
            }
        }
    }
}

private enum class OsdSheet { QUALITY, AUDIO, SUBS }

private const val SkipMs = 15_000L

private fun fmt(ms: Long): String {
    val s = (ms / 1000).coerceAtLeast(0)
    val m = s / 60
    val sec = s % 60
    val h = m / 60
    return if (h > 0) "%d:%02d:%02d".format(h, m % 60, sec) else "%d:%02d".format(m, sec)
}

private val SeekTeal = Color(0xFF4EC4D4)

@Composable
private fun VodSeekRow(
    positionMs: Long,
    durationMs: Long,
    buffering: Boolean,
    fillAlpha: Float,
    onSeek: (Long) -> Unit,
    onScrub: (Boolean) -> Unit,
    onFocusChanged: (Boolean) -> Unit = {},
    onDownToPlayPause: () -> Unit = {},
    onTogglePlayPause: () -> Unit = {},
    onCloseOsd: () -> Unit = {},
    focusRequester: FocusRequester? = null
) {
    val total = durationMs.coerceAtLeast(1L)
    var dragMs by remember { mutableStateOf<Long?>(null) }
    var settleMs by remember { mutableStateOf<Long?>(null) }
    var focused by remember { mutableStateOf(false) }
    val live = positionMs.coerceIn(0L, total)
    LaunchedEffect(live, settleMs) {
        val settle = settleMs ?: return@LaunchedEffect
        if (kotlin.math.abs(settle - live) <= 1_200L) settleMs = null
    }
    DisposableEffect(Unit) {
        onDispose { onScrub(false) }
    }
    val shown = (dragMs ?: settleMs ?: live).coerceIn(0L, total)
    val timeStyle = MaterialTheme.typography.labelLarge.copy(
        fontWeight = FontWeight.SemiBold,
        fontSize = 12.sp,
        fontFeatureSettings = "tnum"
    )
    val borderModifier = if (focused) {
        Modifier.border(
            width = 2.dp,
            brush = Brush.horizontalGradient(
                listOf(Color(0xFF00F0FF), Color(0xFF38BDF8), Color(0xFF67E8F9))
            ),
            shape = RoundedCornerShape(14.dp)
        )
    } else {
        Modifier.border(
            width = 0.8.dp,
            color = Color.White.copy(alpha = 0.15f),
            shape = RoundedCornerShape(14.dp)
        )
    }
    GlassPanel(
        strong = true,
        radius = 14.dp,
        fillAlpha = if (focused) 0.95f else fillAlpha,
        modifier = Modifier
            .fillMaxWidth()
            .then(borderModifier)
    ) {
        Row(
            Modifier.fillMaxWidth().padding(start = 14.dp, end = 14.dp, top = 6.dp, bottom = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                fmt(shown),
                color = if (focused) Color(0xFF00F0FF) else Color.White,
                style = timeStyle,
                maxLines = 1,
                modifier = Modifier.width(68.dp)
            )
            VodSeekTrack(
                fraction = shown.toFloat() / total.toFloat(),
                label = fmt(shown),
                enabled = durationMs > 0,
                isFocused = focused,
                focusRequester = focusRequester,
                onFocusChanged = { f ->
                    focused = f
                    onFocusChanged(f)
                },
                onScrubFraction = { f ->
                    onScrub(true)
                    dragMs = (f * total).toLong().coerceIn(0L, total)
                },
                onScrubEnd = { f ->
                    val ms = (f * total).toLong().coerceIn(0L, total)
                    onSeek(ms)
                    settleMs = ms
                    dragMs = null
                    onScrub(false)
                },
                onKeySeek = { delta ->
                    val ms = (shown + delta).coerceIn(0L, total)
                    onSeek(ms)
                    settleMs = ms
                    dragMs = null
                },
                onDown = onDownToPlayPause,
                onCenter = onTogglePlayPause,
                onBack = onCloseOsd,
                modifier = Modifier.weight(1f).height(28.dp)
            )
            Text(
                fmt(durationMs),
                color = Color.White.copy(alpha = if (buffering) 0.7f else 1f),
                style = timeStyle,
                maxLines = 1,
                modifier = Modifier.width(68.dp),
                textAlign = TextAlign.End
            )
        }
    }
}

@Composable
private fun VodSeekTrack(
    fraction: Float,
    label: String,
    enabled: Boolean,
    isFocused: Boolean,
    focusRequester: FocusRequester? = null,
    onFocusChanged: (Boolean) -> Unit,
    onScrubFraction: (Float) -> Unit,
    onScrubEnd: (Float) -> Unit,
    onKeySeek: (Long) -> Unit,
    onDown: () -> Unit,
    onCenter: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val thumb = if (isFocused) 10.dp else 8.dp
    val trackH = if (isFocused) 6.dp else 5.dp
    var dragging by remember { mutableStateOf(false) }
    val density = LocalDensity.current
    BoxWithConstraints(
        modifier
            .then(if (focusRequester != null) Modifier.focusRequester(focusRequester) else Modifier)
            .onFocusChanged { onFocusChanged(it.isFocused) }
            .focusable(enabled)
    ) {
        val thumbPx = with(density) { thumb.toPx() }
        val usable = (constraints.maxWidth.toFloat() - thumbPx * 2f).coerceAtLeast(1f)
        val f = fraction.coerceIn(0f, 1f)
        Box(
            Modifier
                .fillMaxSize()
                .onPreviewKeyEvent { ev ->
                    if (!enabled) return@onPreviewKeyEvent false
                    val code = ev.nativeKeyEvent.keyCode
                    if (code == KeyEvent.KEYCODE_BACK) {
                        if (ev.nativeKeyEvent.action == KeyEvent.ACTION_UP) {
                            onBack()
                        }
                        return@onPreviewKeyEvent true
                    }
                    if (ev.nativeKeyEvent.action != KeyEvent.ACTION_DOWN) return@onPreviewKeyEvent false
                    when (code) {
                        KeyEvent.KEYCODE_DPAD_LEFT, KeyEvent.KEYCODE_MEDIA_REWIND, KeyEvent.KEYCODE_MEDIA_STEP_BACKWARD -> {
                            onKeySeek(-SkipMs)
                            true
                        }
                        KeyEvent.KEYCODE_DPAD_RIGHT, KeyEvent.KEYCODE_MEDIA_FAST_FORWARD, KeyEvent.KEYCODE_MEDIA_STEP_FORWARD -> {
                            onKeySeek(SkipMs)
                            true
                        }
                        KeyEvent.KEYCODE_DPAD_DOWN -> {
                            onDown()
                            true
                        }
                        KeyEvent.KEYCODE_DPAD_CENTER, KeyEvent.KEYCODE_ENTER, KeyEvent.KEYCODE_NUMPAD_ENTER -> {
                            onCenter()
                            true
                        }
                        else -> false
                    }
                }
                .pointerInput(enabled) {
                    if (!enabled) return@pointerInput
                    awaitEachGesture {
                        val down = awaitFirstDown()
                        down.consume()
                        val barUsable = (size.width - thumbPx * 2f).coerceAtLeast(1f)
                        fun frac(x: Float) = ((x - thumbPx) / barUsable).coerceIn(0f, 1f)
                        var last = frac(down.position.x)
                        dragging = true
                        onScrubFraction(last)
                        try {
                            drag(down.id) { change ->
                                change.consume()
                                last = frac(change.position.x)
                                onScrubFraction(last)
                            }
                        } finally {
                            onScrubEnd(last)
                            dragging = false
                        }
                    }
                }
        ) {
            Canvas(Modifier.fillMaxSize()) {
                val cy = size.height / 2f
                val start = thumbPx
                val end = size.width - thumbPx
                drawLine(
                    color = Color.White.copy(alpha = 0.22f),
                    start = Offset(start, cy),
                    end = Offset(end, cy),
                    strokeWidth = trackH.toPx(),
                    cap = StrokeCap.Round
                )
                val x = start + f * (end - start)
                drawLine(
                    color = if (isFocused) Color(0xFF00F0FF) else Color.White,
                    start = Offset(start, cy),
                    end = Offset(x, cy),
                    strokeWidth = trackH.toPx(),
                    cap = StrokeCap.Round
                )
                if (isFocused) {
                    drawCircle(color = Color(0xFF00F0FF).copy(alpha = 0.35f), radius = thumbPx * 2.2f, center = Offset(x, cy))
                    drawCircle(color = Color(0xFF00F0FF), radius = thumbPx * 1.25f, center = Offset(x, cy))
                    drawCircle(color = Color.White, radius = thumbPx * 0.6f, center = Offset(x, cy))
                } else {
                    drawCircle(color = SeekTeal.copy(alpha = 0.18f), radius = thumbPx * 2f, center = Offset(x, cy))
                    drawCircle(color = SeekTeal, radius = thumbPx, center = Offset(x, cy))
                }
            }
            if (dragging || isFocused) {
                val bubbleWpx = with(density) { 92.dp.roundToPx() }
                val gapPx = with(density) { 42.dp.roundToPx() }
                val thumbX = thumbPx + f * usable
                val xOff = (thumbX - bubbleWpx / 2f).toInt()
                Popup(
                    alignment = Alignment.TopStart,
                    offset = IntOffset(xOff, -gapPx)
                ) {
                    Text(
                        label,
                        color = Color.White,
                        style = MaterialTheme.typography.labelLarge.copy(
                            fontWeight = FontWeight.Bold,
                            fontFeatureSettings = "tnum"
                        ),
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isFocused) Color(0xFF0A192F).copy(alpha = 0.92f) else Color.Black.copy(alpha = 0.85f))
                            .border(if (isFocused) 1.dp else 0.dp, if (isFocused) Color(0xFF00F0FF) else Color.Transparent, RoundedCornerShape(8.dp))
                            .padding(horizontal = 12.dp, vertical = 7.dp),
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}

@Composable
private fun MinaOsdBar(
    playback: Playback,
    channel: ChannelEntity?,
    now: EpgEntity?,
    videoSize: String,
    engine: PlaybackEngine,
    favorite: Boolean,
    playing: Boolean,
    speed: Float,
    streamFormat: StreamFormat = StreamFormat.AUTO,
    onRewind: () -> Unit,
    onPlayPause: () -> Unit,
    onForward: () -> Unit,
    onChannels: () -> Unit,
    onAspect: () -> Unit,
    onEpg: () -> Unit,
    onSubtitles: () -> Unit,
    onFavorite: () -> Unit,
    onQuality: () -> Unit,
    onAudio: () -> Unit,
    onSpeed: () -> Unit,
    onExternal: () -> Unit,
    onPortrait: (() -> Unit)? = null,
    fillAlpha: Float = 1f,
    playPauseFocus: FocusRequester? = null
) {
    val g = LocalGlass.current
    val logo = channel?.logo?.ifBlank { playback.poster } ?: playback.poster
    val title = channel?.name?.ifBlank { playback.title } ?: playback.title
    val clock = remember {
        java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault())
    }
    val epgLine = when {
        now != null && now.startMs > 0 && now.endMs > now.startMs -> {
            val span = "${clock.format(java.util.Date(now.startMs))}–${clock.format(java.util.Date(now.endMs))}"
            if (now.title.isNotBlank()) "$span · ${now.title}" else span
        }
        playback.subtitle.isNotBlank() -> playback.subtitle
        now != null -> now.title
        else -> ""
    }
    val hd = qualityLabel(videoSize)
    val transport = transportLabel(playback.url, streamFormat)
    val kindLabel = when {
        playback.live || playback.kind == "LIVE" -> "CANLI"
        playback.kind == "SERIES" || playback.kind == "EPISODE" -> "DİZİ"
        else -> "FİLM"
    }
    val kindColor = when {
        playback.live || playback.kind == "LIVE" -> Color(0xFFE74C3C)
        playback.kind == "SERIES" || playback.kind == "EPISODE" -> Color(0xFF8E44AD)
        else -> Color(0xFF2980B9)
    }

    Row(
        Modifier.fillMaxWidth().height(IntrinsicSize.Min),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        GlassPanel(strong = true, radius = 16.dp, fillAlpha = fillAlpha, modifier = Modifier.fillMaxHeight()) {
            Box(Modifier.fillMaxHeight()) {
                Row(
                    Modifier.fillMaxHeight().padding(start = 10.dp, end = 32.dp, top = 5.dp, bottom = 5.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        Modifier
                            .size(38.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color.White.copy(alpha = 0.08f)),
                        contentAlignment = Alignment.Center
                    ) {
                        if (logo.isNotBlank()) {
                            AsyncImage(
                                model = logo,
                                contentDescription = null,
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Fit
                            )
                        } else {
                            Text(
                                title.take(1).uppercase(),
                                color = g.text,
                                style = MaterialTheme.typography.titleLarge
                            )
                        }
                    }
                    Column(Modifier.widthIn(max = 220.dp)) {
                        Text(
                            title,
                            color = Color.White,
                            style = MaterialTheme.typography.titleSmall,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (epgLine.isNotBlank()) {
                            Text(
                                epgLine,
                                color = Color.White.copy(alpha = 0.75f),
                                style = MaterialTheme.typography.labelSmall,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        Spacer(Modifier.height(3.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            OsdChip(kindLabel, kindColor, filled = true)
                            if (transport.isNotBlank()) {
                                OsdChip(transport, Color(0xFF8E44AD), filled = transport == "TS")
                            }
                            OsdEngineBadge(engine)
                        }
                    }
                }
                if (hd.isNotBlank()) {
                    Text(
                        hd,
                        color = Color.White,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.ExtraBold,
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(6.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .border(0.8.dp, Color.White.copy(alpha = 0.55f), RoundedCornerShape(6.dp))
                            .padding(horizontal = 5.dp, vertical = 1.5.dp)
                    )
                }
            }
        }
        Spacer(Modifier.weight(1f))
        GlassPanel(strong = true, radius = 16.dp, fillAlpha = fillAlpha, modifier = Modifier.fillMaxHeight()) {
            Row(
                Modifier
                    .fillMaxHeight()
                    .padding(horizontal = 8.dp, vertical = 5.dp)
                    .horizontalScroll(rememberScrollState()),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                OsdIconBtn(Icons.Filled.FastRewind, onRewind)
                OsdIconBtn(
                    if (playing) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                    onPlayPause,
                    primary = true,
                    focusRequester = playPauseFocus
                )
                OsdIconBtn(Icons.Filled.FastForward, onForward)
                Box(
                    Modifier
                        .padding(horizontal = 4.dp)
                        .width(1.dp)
                        .height(28.dp)
                        .background(Color.White.copy(alpha = 0.25f))
                )
                val live = playback.live || playback.kind == "LIVE"
                if (live) OsdIconBtn(Icons.AutoMirrored.Filled.ViewSidebar, onChannels)
                OsdIconBtn(Icons.Filled.FitScreen, onAspect)
                if (live) OsdIconBtn(Icons.Filled.ViewTimeline, onEpg)
                OsdIconBtn(if (favorite) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder, onFavorite)
                OsdIconBtn(Icons.Filled.HighQuality, onQuality)
                OsdIconBtn(Icons.Filled.Audiotrack, onAudio)
                if (onPortrait != null) {
                    OsdIconBtn(Icons.Filled.StayCurrentPortrait, onPortrait)
                }
                if (!live) {
                    OsdIconBtn(Icons.Filled.ClosedCaption, onSubtitles)
                    if (kotlin.math.abs(speed - 1f) < 0.01f) {
                        OsdIconBtn(Icons.Filled.Speed, onSpeed)
                    } else {
                        OsdLetterBtn("${speed}x".trimEnd('0').trimEnd('.'), onSpeed)
                    }
                    OsdIconBtn(Icons.Filled.OpenInNew, onExternal)
                }
            }
        }
    }
}

@Composable
private fun OsdChip(text: String, color: Color, filled: Boolean = false) {
    Text(
        text,
        color = Color.White,
        style = MaterialTheme.typography.labelSmall,
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(if (filled) color else Color.White.copy(alpha = 0.14f))
            .padding(horizontal = 6.dp, vertical = 3.dp)
    )
}

@Composable
private fun OsdEngineBadge(engine: PlaybackEngine) {
    val mediaKit = engine == PlaybackEngine.MEDIA_KIT
    val fill = if (mediaKit) Color(0xFF16A085) else Color(0xFF2E86DE)
    val shape = RoundedCornerShape(5.dp)
    Row(
        modifier = Modifier
            .clip(shape)
            .background(fill)
            .border(0.7.dp, Color.White.copy(alpha = 0.45f), shape)
            .padding(start = 4.dp, end = 6.dp, top = 3.dp, bottom = 3.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(3.dp)
    ) {
        Icon(
            imageVector = if (mediaKit) Icons.Filled.Memory else Icons.Filled.Bolt,
            contentDescription = null,
            tint = Color.White,
            modifier = Modifier.size(11.dp)
        )
        Text(
            text = if (mediaKit) "MediaKit" else "Better",
            color = Color.White,
            fontSize = 9.5.sp,
            fontWeight = FontWeight.ExtraBold,
            lineHeight = 10.sp,
            maxLines = 1
        )
    }
}

@Composable
private fun OsdIconBtn(
    icon: ImageVector,
    onClick: () -> Unit,
    primary: Boolean = false,
    focusRequester: FocusRequester? = null
) {
    var focused by remember { mutableStateOf(false) }
    val teal = Color(0xFF21E6EB)
    val shape = RoundedCornerShape(12.dp)
    Box(
        Modifier
            .size(44.dp)
            .onFocusChanged { focused = it.isFocused }
            .clip(shape)
            .background(
                when {
                    focused -> Color.White.copy(alpha = 0.22f)
                    else -> Color.White.copy(alpha = 0.08f)
                }
            )
            .border(if (primary) 1.8.dp else 0.dp, if (primary) teal else Color.Transparent, shape),
        contentAlignment = Alignment.Center
    ) {
        GlassPanel(
            focused = focused,
            strong = primary,
            radius = 12.dp,
            onClick = onClick,
            modifier = Modifier.size(44.dp).rayFocusRequester(focusRequester)
        ) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Icon(icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(22.dp))
            }
        }
    }
}

@Composable
private fun OsdLetterBtn(text: String, onClick: () -> Unit) {
    var focused by remember { mutableStateOf(false) }
    GlassPanel(
        focused = focused,
        strong = true,
        radius = 8.dp,
        onClick = onClick,
        modifier = Modifier
            .size(44.dp)
            .onFocusChanged { focused = it.isFocused }
    ) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(
                text,
                color = Color.White,
                style = MaterialTheme.typography.labelLarge,
                modifier = Modifier
                    .clip(RoundedCornerShape(4.dp))
                    .background(Color.Transparent)
                    .padding(horizontal = 4.dp, vertical = 2.dp)
            )
        }
    }
}

private fun qualityLabel(size: String): String {
    val h = size.substringAfter('x', "").toIntOrNull() ?: size.substringBefore('x').toIntOrNull() ?: 0
    val w = size.substringBefore('x').toIntOrNull() ?: 0
    val m = maxOf(w, h)
    return when {
        m >= 2160 -> "4K"
        m >= 1080 -> "HD"
        m >= 720 -> "HD"
        m > 0 -> "SD"
        else -> ""
    }
}

private fun transportLabel(url: String, format: StreamFormat = StreamFormat.AUTO): String {
    val effectiveUrl = if (format != StreamFormat.AUTO) {
        XtreamStreamUrls.applyFormat(url, format)
    } else {
        url
    }.lowercase()
    val clean = effectiveUrl.substringBefore('?')
    return when {
        clean.endsWith(".ts") || "output=ts" in effectiveUrl || "format=ts" in effectiveUrl || ".ts?" in effectiveUrl -> "TS"
        clean.endsWith(".m3u8") || "output=m3u8" in effectiveUrl || "type=m3u8" in effectiveUrl || "/hls/" in effectiveUrl || ".m3u8?" in effectiveUrl -> "HLS"
        clean.endsWith(".mpd") || ".mpd?" in effectiveUrl -> "DASH"
        format == StreamFormat.TS -> "TS"
        format == StreamFormat.HLS -> "HLS"
        StreamHints.mpegTs(effectiveUrl) -> "TS"
        StreamHints.hls(effectiveUrl) -> "HLS"
        StreamHints.dash(effectiveUrl) -> "DASH"
        else -> ""
    }
}

@Composable
private fun OsdTrackSheet(
    title: String,
    empty: String,
    tracks: List<TrackOption>,
    onPick: (String) -> Unit,
    onDismiss: () -> Unit
) {
    Box(
        Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Box(
            Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.45f))
                .clickable(onClick = onDismiss)
        )
        GlassPanel(
            strong = true,
            radius = 18.dp,
            modifier = Modifier
                .fillMaxWidth(0.46f)
                .padding(24.dp)
        ) {
            Column(
                Modifier.padding(18.dp).heightIn(max = 360.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    title,
                    color = Color.White,
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                )
                if (tracks.isEmpty()) {
                    Text(empty, color = Color.White.copy(alpha = 0.75f), style = MaterialTheme.typography.bodyLarge)
                } else {
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(tracks, key = { it.id }) { track ->
                            GlassPanel(
                                strong = track.selected,
                                accentFill = track.selected,
                                radius = 10.dp,
                                onClick = { onPick(track.id) }
                            ) {
                                Text(
                                    track.label,
                                    color = Color.White,
                                    fontWeight = if (track.selected) FontWeight.Bold else FontWeight.Medium,
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun BufferingLogoIndicator(
    playback: Playback,
    channel: ChannelEntity?,
    buffering: Boolean
) {
    AnimatedVisibility(
        visible = buffering,
        enter = fadeIn(animationSpec = tween(220)),
        exit = fadeOut(animationSpec = tween(260))
    ) {
        val infiniteTransition = rememberInfiniteTransition(label = "bufferingPulse")
        val pulseAlpha by infiniteTransition.animateFloat(
            initialValue = 0.35f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(850, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "pulseAlpha"
        )
        val pulseScale by infiniteTransition.animateFloat(
            initialValue = 0.94f,
            targetValue = 1.06f,
            animationSpec = infiniteRepeatable(
                animation = tween(850, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "pulseScale"
        )
        val g = LocalGlass.current
        val logoUrl = playback.poster.ifBlank { channel?.logo.orEmpty() }
        val name = playback.title.ifBlank { channel?.name.orEmpty() }

        Box(
            Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            // Dış Işıma Halkası (Pulsating Halo Ring)
            Box(
                Modifier
                    .size(92.dp)
                    .scale(pulseScale)
                    .clip(CircleShape)
                    .background(g.accent.copy(alpha = 0.12f * pulseAlpha))
                    .border(
                        width = 1.5.dp,
                        color = g.accent.copy(alpha = 0.65f * pulseAlpha),
                        shape = CircleShape
                    )
            )
            // İç Buzlu Cam Logo Dairesi (Inner Frosted Glass Circle)
            Box(
                Modifier
                    .size(76.dp)
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.68f))
                    .border(
                        width = 1.dp,
                        color = Color.White.copy(alpha = 0.25f + 0.35f * pulseAlpha),
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                if (logoUrl.isNotBlank()) {
                    AsyncImage(
                        model = ImageRequest.Builder(LocalContext.current)
                            .data(logoUrl)
                            .crossfade(true)
                            .build(),
                        contentDescription = name,
                        contentScale = ContentScale.Fit,
                        modifier = Modifier
                            .size(48.dp)
                            .padding(4.dp)
                            .graphicsLayer { alpha = 0.65f + 0.35f * pulseAlpha }
                    )
                } else {
                    Text(
                        text = name.take(2).uppercase(),
                        color = Color.White.copy(alpha = 0.65f + 0.35f * pulseAlpha),
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                }
            }
        }
    }
}

