package com.ray.iptv.ui.screens.settings

import android.content.Intent
import android.content.pm.PackageManager
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.AspectRatio
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.HighQuality
import androidx.compose.material.icons.filled.LabelOff
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.MovieFilter
import androidx.compose.material.icons.filled.Opacity
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.PictureInPicture
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.Subtitles
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material.icons.filled.TravelExplore
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.tv.material3.Icon
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.ray.iptv.data.repo.AspectMode
import com.ray.iptv.data.repo.PlaybackEngine
import com.ray.iptv.data.repo.RaySettings
import com.ray.iptv.data.repo.StreamFormat
import androidx.compose.ui.focus.FocusRequester
import com.ray.iptv.data.repo.UserAgentPreset
import com.ray.iptv.data.repo.VodInfoEngine
import com.ray.iptv.ui.RayViewModel
import com.ray.iptv.ui.components.GlassButton
import com.ray.iptv.ui.glass.GlassPanel
import com.ray.iptv.ui.screens.onboarding.GlassField
import androidx.activity.compose.BackHandler
import androidx.compose.runtime.LaunchedEffect
import kotlinx.coroutines.delay
import com.ray.iptv.ui.input.tryFocus
import com.ray.iptv.ui.theme.LocalGlass

private enum class PlaybackSub { LIST, SUBTITLES }

@Composable
internal fun PlaybackSettingsRoot(
    vm: RayViewModel,
    settings: RaySettings,
    tr: Boolean,
    focusRequester: FocusRequester? = null,
    onBack: () -> Unit
) {
    var sub by remember { mutableStateOf(PlaybackSub.LIST) }
    val subItemFocusRequester = remember { FocusRequester() }

    BackHandler {
        if (sub != PlaybackSub.LIST) {
            sub = PlaybackSub.LIST
        } else {
            onBack()
        }
    }

    LaunchedEffect(sub) {
        if (sub == PlaybackSub.LIST) {
            repeat(8) {
                delay(30)
                if (focusRequester?.tryFocus() == true) return@LaunchedEffect
            }
        } else {
            repeat(8) {
                delay(35)
                if (subItemFocusRequester.tryFocus()) return@LaunchedEffect
            }
        }
    }

    when (sub) {
        PlaybackSub.LIST -> PlaybackSettingsList(vm, settings, tr, onBack, focusRequester) { sub = PlaybackSub.SUBTITLES }
        PlaybackSub.SUBTITLES -> SubtitleOptionsPage(vm, settings, tr) { sub = PlaybackSub.LIST }
    }
}

@Composable
private fun PlaybackSettingsList(
    vm: RayViewModel,
    settings: RaySettings,
    tr: Boolean,
    onBack: () -> Unit,
    focusRequester: FocusRequester? = null,
    onSubtitles: () -> Unit
) {
    var engineOpen by remember { mutableStateOf(false) }
    var smartOpen by remember { mutableStateOf(false) }
    var formatOpen by remember { mutableStateOf(false) }
    var extOpen by remember { mutableStateOf(false) }
    var vodOpen by remember { mutableStateOf(false) }
    var uaOpen by remember { mutableStateOf(false) }
    var bufferOpen by remember { mutableStateOf(false) }
    var osdHideOpen by remember { mutableStateOf(false) }
    var aspectOpen by remember { mutableStateOf(false) }
    var prefixConfirm by remember { mutableStateOf(false) }
    val liveName = engineName(settings.liveEngine)
    val vodName = engineName(settings.vodPlaybackEngine)
    val fmtSub = when (settings.streamFormat) {
        StreamFormat.TS -> if (tr) "MPEG-TS (.ts) · Hızlı" else "MPEG-TS (.ts) · Fast"
        else -> if (tr) "HLS (.m3u8) · Kararlı (Varsayılan)" else "HLS (.m3u8) · Stable (Default)"
    }
    val bufferSub = if (settings.liveBufferSeconds == 3) {
        if (tr) "3 saniye · Varsayılan" else "3 seconds · Default"
    } else if (settings.liveBufferSeconds == 0) {
        if (tr) "Otomatik" else "Auto"
    } else if (tr) "${settings.liveBufferSeconds} saniye" else "${settings.liveBufferSeconds} seconds"
    val extSub = when {
        !settings.externalPlayerEnabled -> if (tr) "Kapalı — uygulama içi oynatıcı" else "Off — in-app player"
        settings.externalPlayerPackage.isBlank() -> if (tr) "Açık · her seferinde sor" else "On · ask every time"
        else -> settings.externalPlayerLabel.ifBlank { settings.externalPlayerPackage }
    }
    val vodSub = when (settings.vodInfoEngine) {
        VodInfoEngine.AUTO -> if (tr) "Otomatik" else "Auto"
        VodInfoEngine.XTREAM_ONLY -> if (tr) "Xtream Bilgileri" else "Xtream info"
        VodInfoEngine.TMDB_OMDB_ONLY -> if (tr) "TMDB/OMDB Bilgileri" else "TMDB/OMDB info"
    }
    val subSummary = buildString {
        append("${settings.subtitleSize} pt · ${subtitleColorLabel(settings.subtitleColor, tr)} · ${subtitleFontLabel(settings.subtitleFont, tr)}")
        val remembered = settings.preferredSubtitleToken
        if (remembered.isNotBlank()) append(" · $remembered")
        else if (settings.subtitleAuto) append(if (tr) " · otomatik" else " · auto")
    }
    LazyColumn(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item {
            PlaybackHeader(
                title = if (tr) "Oynatma Ayarları" else "Playback Settings",
                hint = if (tr) {
                    "Oynatıcı motoru ve düşük seviye video ayarları. Yayın takılırsa motoru değiştirip veya kod çözücüyü yazılıma alıp deneyebilirsin."
                } else {
                    "Player engine and low-level video settings. If a stream stalls, switch engine or try the software decoder."
                },
                onBack = onBack,
                focusRequester = focusRequester
            )
        }
        item {
            PlaybackTile(
                icon = Icons.Filled.PlayCircle,
                title = if (tr) "Oynatıcı motoru tercihleri" else "Player engine preferences",
                subtitle = if (tr) "Canlı: $liveName · Film/Dizi: $vodName" else "Live: $liveName · Movies/Series: $vodName",
                onClick = { engineOpen = true }
            )
        }
        item {
            PlaybackTile(
                icon = Icons.Filled.AutoAwesome,
                title = if (tr) "Akıllı Oynatıcı Seçimi" else "Smart player selection",
                subtitle = if (settings.smartPlayerSelection) {
                    if (tr) "Açık — MediaKit ile açılan kanal sonraki seferde hatırlanır"
                    else "On — channels that opened on MediaKit are remembered"
                } else {
                    if (tr) "Kapalı — her seferinde seçilen motorla başlar (Better yedeği sürer)"
                    else "Off — always starts with the selected engine (Better fallback remains)"
                },
                showSwitch = true,
                checked = settings.smartPlayerSelection,
                onClick = { smartOpen = true }
            )
        }
        item {
            PlaybackTile(
                icon = Icons.Filled.HighQuality,
                title = if (tr) "Donanım hızlandırma (MediaKit)" else "Hardware acceleration (MediaKit)",
                subtitle = if (settings.mediaKitLowPowerHwdec) {
                    if (tr) "Düşük güç / eski TV kutusu — mediacodec" else "Low power / older boxes — mediacodec"
                } else {
                    if (tr) "Dengeli — mediacodec-copy (önerilen)" else "Balanced — mediacodec-copy (recommended)"
                },
                onClick = { vm.setMediaKitLowPowerHwdec(!settings.mediaKitLowPowerHwdec) }
            )
        }
        item {
            PlaybackTile(
                icon = Icons.Filled.Memory,
                title = if (tr) "Video kod çözücü (Android)" else "Video decoder (Android)",
                subtitle = if (settings.softwareDecoder) {
                    if (tr) "Yazılım — ExoPlayer yazılım decoder. MediaKit: hwdec=no."
                    else "Software — ExoPlayer software decoder. MediaKit: hwdec=no."
                } else {
                    if (tr) "Donanım — ExoPlayer donanım decoder. MediaKit: mediacodec-copy / mediacodec."
                    else "Hardware — ExoPlayer hardware decoder. MediaKit: mediacodec-copy / mediacodec."
                },
                onClick = { vm.setSoftwareDecoder(!settings.softwareDecoder) }
            )
        }
        item {
            PlaybackTile(
                icon = Icons.Filled.SwapHoriz,
                title = if (tr) "Yayın formatı" else "Stream format",
                subtitle = fmtSub,
                onClick = { formatOpen = true }
            )
        }
        item {
            PlaybackTile(
                icon = Icons.Filled.OpenInNew,
                title = if (tr) "Harici Oynatıcı" else "External player",
                subtitle = extSub,
                showSwitch = true,
                checked = settings.externalPlayerEnabled,
                onClick = {
                    if (!settings.externalPlayerEnabled) vm.setExternalPlayerEnabled(true)
                    extOpen = true
                }
            )
        }
        item {
            PlaybackTile(
                icon = Icons.Filled.MovieFilter,
                title = if (tr) "Film Dizi Bilgi Motoru" else "Movie & series info engine",
                subtitle = vodSub,
                onClick = { vodOpen = true }
            )
        }
        item {
            PlaybackTile(
                icon = Icons.Filled.TravelExplore,
                title = "User Agent",
                subtitle = userAgentLabel(settings, tr),
                onClick = { uaOpen = true }
            )
        }
        item {
            PlaybackTile(
                icon = Icons.Filled.Speed,
                title = if (tr) "Düşük Gecikme (Buffer)" else "Low latency (buffer)",
                subtitle = bufferSub,
                onClick = { bufferOpen = true }
            )
        }
        item {
            PlaybackTile(
                icon = Icons.Filled.Timer,
                title = if (tr) "OSD panel gizleme süresi" else "OSD auto-hide duration",
                subtitle = if (tr) "${settings.osdHideSeconds} saniye" else "${settings.osdHideSeconds} seconds",
                onClick = { osdHideOpen = true }
            )
        }
        item {
            PlaybackTile(
                icon = Icons.Filled.PlayArrow,
                title = if (tr) "Arka Planda Oynatma" else "Background playback",
                subtitle = if (settings.backgroundPlayback) (if (tr) "Aktif" else "On") else if (tr) "Pasif" else "Off",
                showSwitch = true,
                checked = settings.backgroundPlayback,
                onClick = { vm.setBackgroundPlayback(!settings.backgroundPlayback) }
            )
        }
        item {
            PlaybackTile(
                icon = Icons.Filled.PictureInPicture,
                title = if (tr) "Resim İçinde Resim (PiP)" else "Picture-in-Picture (PiP)",
                subtitle = if (settings.pipMode) {
                    if (tr) "Aktif — ana ekrana dönüldüğünde küçük pencerede oynat" else "On — floating window on home screen"
                } else if (tr) "Kapalı" else "Off",
                showSwitch = true,
                checked = settings.pipMode,
                onClick = { vm.setPipMode(!settings.pipMode) }
            )
        }
        item {
            PlaybackTile(
                icon = Icons.Filled.Visibility,
                title = if (tr) "Yayın önizlemesi" else "Stream preview",
                subtitle = if (settings.previewLive) {
                    if (tr) "Açık — liste detayında sessiz önizleme" else "On — silent preview in list detail"
                } else if (tr) "Kapalı" else "Off",
                showSwitch = true,
                checked = settings.previewLive,
                onClick = { vm.setPreview(!settings.previewLive) }
            )
        }
        item {
            PlaybackTile(
                icon = Icons.Filled.SkipNext,
                title = if (tr) "Otomatik sonraki bölüm" else "Auto next episode",
                subtitle = if (settings.autoplayNext) {
                    if (tr) "Bölüm bitince sonrakini oynat" else "Play the next item when an episode ends"
                } else if (tr) "Kapalı" else "Off",
                showSwitch = true,
                checked = settings.autoplayNext,
                onClick = { vm.setAutoplay(!settings.autoplayNext) }
            )
        }
        item {
            PlaybackTile(
                icon = Icons.Filled.AspectRatio,
                title = if (tr) "Görüntü oranı" else "Aspect ratio",
                subtitle = when (settings.aspect) {
                    AspectMode.FIT -> if (tr) "Sığdır" else "Fit"
                    AspectMode.ZOOM -> if (tr) "Yakınlaştır" else "Zoom"
                    AspectMode.FILL -> if (tr) "Doldur" else "Fill"
                    AspectMode.STRETCH -> if (tr) "Uzat" else "Stretch"
                },
                onClick = { aspectOpen = true }
            )
        }
        item {
            PlaybackTile(
                icon = Icons.Filled.VerifiedUser,
                title = if (tr) "SSL/TLS doğrulamasını yoksay" else "Ignore SSL/TLS verification",
                subtitle = if (settings.ignoreSsl) {
                    if (tr) "Açık — geçersiz sertifikalı yayınlara izin ver" else "On — allow streams with invalid certificates"
                } else if (tr) "Kapalı — yalnızca geçerli sertifika" else "Off — valid certificates only",
                showSwitch = true,
                checked = settings.ignoreSsl,
                onClick = { vm.setIgnoreSsl(!settings.ignoreSsl) }
            )
        }
        item {
            PlaybackTile(
                icon = Icons.Filled.Translate,
                title = if (tr) "Google Translate" else "Google Translate",
                subtitle = if (settings.translateMeta) {
                    if (tr) "Xtream boşsa özet ve türleri uygulama diline çevir" else "Translate plot and genres when Xtream has none"
                } else if (tr) "Kapalı" else "Off",
                showSwitch = true,
                checked = settings.translateMeta,
                onClick = { vm.setTranslateMeta(!settings.translateMeta) }
            )
        }
        item {
            PlaybackTile(
                icon = Icons.Filled.LabelOff,
                title = if (tr) "Kanal ön eki" else "Channel prefix",
                subtitle = if (settings.stripChannelPrefix) {
                    if (tr) "TR:/BR:/EN:/US: gibi ülke önekleri gizleniyor (kalite kalır)"
                    else "Country prefixes like TR:/BR:/EN:/US: are hidden (quality kept)"
                } else if (tr) "Playlist adları olduğu gibi" else "Playlist names as-is",
                showSwitch = true,
                checked = settings.stripChannelPrefix,
                onClick = {
                    if (settings.stripChannelPrefix) vm.setStripPrefix(false) else prefixConfirm = true
                }
            )
        }
        item {
            PlaybackTile(
                icon = Icons.Filled.Subtitles,
                title = if (tr) "Altyazı seçenekleri" else "Subtitle options",
                subtitle = subSummary,
                onClick = onSubtitles
            )
        }
        item {
            PlaybackTile(
                icon = Icons.Filled.Sync,
                title = if (tr) "Arka Planda Sessiz Senkronizasyon" else "Silent background sync",
                subtitle = if (settings.silentSync) {
                    if (tr) "Açık (Günde bir kez güncellenir)" else "On (updates once a day)"
                } else if (tr) "Kapalı" else "Off",
                showSwitch = true,
                checked = settings.silentSync,
                onClick = { vm.setSilentSync(!settings.silentSync) }
            )
        }
        item {
            PlaybackTile(
                icon = Icons.Filled.LabelOff,
                title = if (tr) "+18 içerikleri gizle" else "Hide adult content",
                subtitle = if (tr) "Yetişkin kanalları listeden çıkar" else "Remove adult channels from the list",
                showSwitch = true,
                checked = settings.hideAdult,
                onClick = { vm.setHideAdult(!settings.hideAdult) }
            )
        }
        item { Spacer(Modifier.height(12.dp)) }
    }
    if (engineOpen) {
        EnginePrefsDialog(
            settings = settings,
            tr = tr,
            onDismiss = { engineOpen = false },
            onLive = vm::setLiveEngine,
            onVod = vm::setVodPlaybackEngine
        )
    }
    if (smartOpen) {
        SmartPlayerDialog(
            on = settings.smartPlayerSelection,
            tr = tr,
            onToggle = { vm.setSmartPlayerSelection(!settings.smartPlayerSelection) },
            onDismiss = { smartOpen = false }
        )
    }
    if (formatOpen) {
        GlassChoiceDialog(
            title = if (tr) "Canlı yayın formatı" else "Live stream format",
            body = if (tr) {
                "HLS (.m3u8) parçalı veri transferi sayesinde dalgalı internet hızlarında donma ve kopmaları engeller ve en kararlı akışı sağlar. MPEG-TS (.ts) ise ham akış olup hızlı kanal geçişi sunar."
            } else {
                "HLS (.m3u8) prevents buffering and drops on fluctuating connections via chunked streaming. MPEG-TS (.ts) is raw stream for faster channel zapping."
            },
            options = listOf(
                StreamFormat.HLS to if (tr) "HLS (.m3u8) — Kararlı (Varsayılan)" else "HLS (.m3u8) — Stable (Default)",
                StreamFormat.TS to if (tr) "MPEG-TS (.ts) — Hızlı" else "MPEG-TS (.ts) — Fast"
            ),
            selected = if (settings.streamFormat == StreamFormat.TS) StreamFormat.TS else StreamFormat.HLS,
            onDismiss = { formatOpen = false },
            onPick = { vm.setStreamFormat(it); formatOpen = false }
        )
    }
    if (extOpen) {
        ExternalPlayerDialog(
            settings = settings,
            tr = tr,
            onDismiss = { extOpen = false },
            onEnabled = vm::setExternalPlayerEnabled,
            onPick = { pkg, label -> vm.setExternalPlayer(pkg, label); extOpen = false }
        )
    }
    if (vodOpen) {
        GlassChoiceDialog(
            title = if (tr) "Film Dizi Bilgi Motoru" else "Movie & series info engine",
            body = if (tr) "Otomatik: Xtream boşsa TMDB, OMDb ve Google Translate ile uygulama dilinde doldurulur." else "Auto: if Xtream has no info, TMDB, OMDb and Google Translate fill details in the app language.",
            options = listOf(
                VodInfoEngine.AUTO to if (tr) "Otomatik" else "Auto",
                VodInfoEngine.XTREAM_ONLY to if (tr) "Xtream Bilgileri" else "Xtream info",
                VodInfoEngine.TMDB_OMDB_ONLY to if (tr) "TMDB/OMDB Bilgileri" else "TMDB/OMDB info"
            ),
            selected = settings.vodInfoEngine,
            onDismiss = { vodOpen = false },
            onPick = { vm.setVodInfoEngine(it); vodOpen = false }
        )
    }
    if (uaOpen) {
        UserAgentDialog(
            settings = settings,
            tr = tr,
            onDismiss = { uaOpen = false },
            onPick = { preset, custom ->
                vm.setUserAgent(preset)
                if (preset == UserAgentPreset.CUSTOM) vm.setCustomUserAgent(custom)
                uaOpen = false
            }
        )
    }
    if (bufferOpen) {
        val opts = listOf(3, 5, 8, 10, 15, 20, 30, 2, 1, 0)
            .let { base -> if (settings.liveBufferSeconds in base) base else (base + settings.liveBufferSeconds) }
        GlassChoiceDialog(
            title = if (tr) "Canlı yayın tamponu" else "Live buffer",
            body = if (tr) "3 saniye en dengeli varsayılandır (hızlı kanal geçişi + donma koruması). Yüksek değerler dalgalı hatlarda donmayı tamamen keser."
            else "3 seconds is the balanced default (fast zapping + drop protection). Higher values prevent stutter on fluctuating connections.",
            options = opts.map {
                it to when (it) {
                    3 -> if (tr) "3 saniye (Önerilen / Varsayılan)" else "3 seconds (Recommended / Default)"
                    5 -> if (tr) "5 saniye (Yüksek Koruma)" else "5 seconds (High Protection)"
                    0 -> if (tr) "Otomatik" else "Auto"
                    else -> if (tr) "$it saniye" else "$it seconds"
                }
            },
            selected = settings.liveBufferSeconds,
            onDismiss = { bufferOpen = false },
            onPick = { vm.setLiveBuffer(it); bufferOpen = false }
        )
    }
    if (osdHideOpen) {
        val opts = listOf(3, 5, 7, 10, 15, 20)
            .let { base -> if (settings.osdHideSeconds in base) base else (base + settings.osdHideSeconds).sorted() }
        GlassChoiceDialog(
            title = if (tr) "OSD panel gizleme süresi" else "OSD auto-hide duration",
            body = if (tr) {
                "Dikey modda ve TV/tablet kumanda ekranında OSD ile alt kanal çubuğu bu süre sonunda gizlenir."
            } else {
                "In portrait and on TV/tablet remote screens, the OSD and channel bar hide after this time."
            },
            options = opts.map { it to if (tr) "$it saniye" else "$it seconds" },
            selected = settings.osdHideSeconds,
            onDismiss = { osdHideOpen = false },
            onPick = { vm.setOsdHide(it); osdHideOpen = false }
        )
    }
    if (aspectOpen) {
        GlassChoiceDialog(
            title = if (tr) "Görüntü oranı" else "Aspect ratio",
            options = listOf(
                AspectMode.FIT to if (tr) "Sığdır" else "Fit",
                AspectMode.ZOOM to if (tr) "Yakınlaştır" else "Zoom",
                AspectMode.FILL to if (tr) "Doldur" else "Fill",
                AspectMode.STRETCH to if (tr) "Uzat" else "Stretch"
            ),
            selected = settings.aspect,
            onDismiss = { aspectOpen = false },
            onPick = { vm.setAspect(it); aspectOpen = false }
        )
    }
    if (prefixConfirm) {
        GlassConfirmDialog(
            title = if (tr) "Kanal ön ekini kaldır" else "Strip channel prefix",
            body = if (tr) {
                "Canlı TV kanal listelerinde, EPG satırlarında ve ilgili şeritlerde ülke kodu önekleri (TR:, BR:, EN:, DE: vb.) gösterilmez. Yalnızca kanal adı kalır.\n\nÖrnek dönüşümler:\nTR: FX  →  FX\nBR: Globo  →  Globo"
            } else {
                "Country-code prefixes (TR:, BR:, EN:, DE:, etc.) are hidden in live lists, EPG rows and related rails. Only the channel name remains.\n\nExamples:\nTR: FX  →  FX\nBR: Globo  →  Globo"
            },
            confirm = if (tr) "Kaldır" else "Strip",
            onDismiss = { prefixConfirm = false },
            onConfirm = { vm.setStripPrefix(true); prefixConfirm = false }
        )
    }
}

@Composable
private fun SubtitleOptionsPage(
    vm: RayViewModel,
    settings: RaySettings,
    tr: Boolean,
    onBack: () -> Unit
) {
    val previewColor = when (settings.subtitleColor.lowercase()) {
        "yellow" -> Color(0xFFFFE082)
        "cyan" -> Color(0xFF80DEEA)
        "green" -> Color(0xFFA5D6A7)
        "orange" -> Color(0xFFFFCC80)
        "pink" -> Color(0xFFF48FB1)
        else -> Color.White
    }
    val family = when (settings.subtitleFont) {
        "serif" -> FontFamily.Serif
        "mono" -> FontFamily.Monospace
        else -> FontFamily.SansSerif
    }
    LazyColumn(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item {
            PlaybackHeader(
                title = if (tr) "Altyazı seçenekleri" else "Subtitle options",
                hint = if (tr) {
                    "Görünüm film ve dizide geçerli. OSD’den seçilen dil sonraki yayınlarda hatırlanır."
                } else {
                    "Appearance applies to movies and series. The language picked in the OSD is remembered for the next titles."
                },
                onBack = onBack
            )
        }
        item {
            Box(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color.Black.copy(alpha = 0.55f))
                    .padding(18.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    if (tr) "Altyazı önizlemesi" else "Subtitle preview",
                    color = previewColor,
                    fontFamily = family,
                    fontWeight = FontWeight.Bold,
                    fontSize = settings.subtitleSize.sp,
                    style = androidx.compose.ui.text.TextStyle(
                        shadow = if (settings.subtitleOutline) {
                            Shadow(color = Color.Black, offset = Offset(1.2f, 1.2f), blurRadius = 3f)
                        } else {
                            null
                        }
                    )
                )
            }
        }
        item {
            SettingsPickerRow(
                if (tr) "Boyut" else "Size",
                "${settings.subtitleSize} pt",
                listOf(16, 18, 20, 22, 24, 28, 32, 36).map { it to "$it pt" },
                settings.subtitleSize,
                onPick = vm::setSubtitleSize
            )
        }
        item {
            SettingsPickerRow(
                if (tr) "Renk" else "Color",
                subtitleColorLabel(settings.subtitleColor, tr),
                listOf(
                    "white" to if (tr) "Beyaz" else "White",
                    "yellow" to if (tr) "Sarı" else "Yellow",
                    "cyan" to if (tr) "Camgöbeği" else "Cyan",
                    "green" to if (tr) "Yeşil" else "Green",
                    "orange" to if (tr) "Turuncu" else "Orange",
                    "pink" to if (tr) "Pembe" else "Pink"
                ),
                settings.subtitleColor,
                onPick = vm::setSubtitleColor
            )
        }
        item {
            SettingsPickerRow(
                if (tr) "Yazı tipi" else "Font",
                subtitleFontLabel(settings.subtitleFont, tr),
                listOf(
                    "sans" to if (tr) "Sans (varsayılan)" else "Sans (default)",
                    "serif" to "Serif",
                    "mono" to if (tr) "Eş aralıklı" else "Monospace"
                ),
                settings.subtitleFont,
                body = if (tr) "Altyazı metninde kullanılacak font." else "Font used for subtitle text.",
                onPick = vm::setSubtitleFont
            )
        }
        item {
            SettingsToggleRow(
                if (tr) "Kontur (çerçeve)" else "Outline",
                if (tr) "Arka planda okunabilirlik için siyah kenarlık." else "Black edge for readability on busy backgrounds.",
                settings.subtitleOutline
            ) { vm.setSubtitleOutline(!settings.subtitleOutline) }
        }
        item {
            SettingsToggleRow(
                if (tr) "Otomatik altyazı" else "Auto subtitles",
                if (tr) {
                    "Hatırlanan dil yoksa ve parça varsa ilk altyazıyı aç. OSD’den seçilen dil her zaman önceliklidir."
                } else {
                    "If no remembered language, turn on the first available track. The OSD language always wins when present."
                },
                settings.subtitleAuto
            ) { vm.setSubtitleAuto(!settings.subtitleAuto) }
        }
        item {
            val token = settings.preferredSubtitleToken
            SettingsToggleRow(
                if (tr) "Hatırlanan dil" else "Remembered language",
                if (token.isBlank()) {
                    if (tr) "Yok — film/dizide OSD’den seçince kaydedilir" else "None — saved when you pick a language in the OSD"
                } else {
                    if (tr) "Şu dil seçilecek: $token  (kapatmak için OSD’den Altyazı Kapalı)" else "Will select: $token  (choose Off in the OSD to clear)"
                },
                token.isNotBlank()
            ) {
                if (token.isNotBlank()) vm.clearPreferredSubtitle()
            }
        }
    }
}

@Composable
private fun EnginePrefsDialog(
    settings: RaySettings,
    tr: Boolean,
    onDismiss: () -> Unit,
    onLive: (PlaybackEngine) -> Unit,
    onVod: (PlaybackEngine) -> Unit
) {
    val g = LocalGlass.current
    Dialog(onDismissRequest = onDismiss) {
        GlassPanel(strong = true, radius = 20.dp, modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    if (tr) "Oynatıcı motoru tercihleri" else "Player engine preferences",
                    color = g.text,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.ExtraBold
                )
                Text(
                    if (tr) {
                        "Her içerik tipi için ana motoru seçin (Better veya MediaKit). Better seçiliyken açılmayan yayınlarda HLS↔TS ve MediaKit yedeği otomatik denenir. «Akıllı Oynatıcı Seçimi» yalnızca başarılı MediaKit kanalını hatırlar."
                    } else {
                        "Pick the primary engine per content type (Better or MediaKit). When Better is selected, failed streams retry HLS↔TS then MediaKit. Smart selection only remembers successful MediaKit channels."
                    },
                    color = g.muted,
                    style = MaterialTheme.typography.bodySmall
                )
                Text(if (tr) "Canlı yayın motoru" else "Live engine", color = g.text, fontWeight = FontWeight.Bold)
                EnginePair(settings.liveEngine, onLive)
                Text(if (tr) "Film / dizi oynatma" else "Movies / series", color = g.text, fontWeight = FontWeight.Bold)
                EnginePair(settings.vodPlaybackEngine, onVod)
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    GlassButton(if (tr) "Kapat" else "Close") { onDismiss() }
                }
            }
        }
    }
}

@Composable
private fun EnginePair(selected: PlaybackEngine, onPick: (PlaybackEngine) -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        EngineCard("Better Player", selected == PlaybackEngine.BETTER, Modifier.weight(1f)) {
            onPick(PlaybackEngine.BETTER)
        }
        EngineCard("MediaKit", selected == PlaybackEngine.MEDIA_KIT, Modifier.weight(1f)) {
            onPick(PlaybackEngine.MEDIA_KIT)
        }
    }
}

@Composable
private fun EngineCard(label: String, selected: Boolean, modifier: Modifier, onClick: () -> Unit) {
    val g = LocalGlass.current
    var focused by remember { mutableStateOf(false) }
    GlassPanel(
        focused = focused || selected,
        strong = selected,
        radius = 14.dp,
        onClick = onClick,
        modifier = modifier.onFocusChanged { focused = it.isFocused }
    ) {
        Box(Modifier.fillMaxWidth().padding(14.dp), contentAlignment = Alignment.Center) {
            Text(label, color = g.text, fontWeight = FontWeight.ExtraBold)
        }
    }
}

@Composable
private fun SmartPlayerDialog(on: Boolean, tr: Boolean, onToggle: () -> Unit, onDismiss: () -> Unit) {
    val g = LocalGlass.current
    Dialog(onDismissRequest = onDismiss) {
        GlassPanel(strong = true, radius = 20.dp, modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    if (tr) "Akıllı Oynatıcı Seçimi" else "Smart player selection",
                    color = g.text,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.ExtraBold
                )
                Text(
                    if (tr) {
                        "Better seçiliyken yayın her zaman Better → HLS/TS ↔ TS/HLS → gerekirse MediaKit sırasıyla denenir. Bu ayar açılırsa MediaKit ile başarıyla açılan kanal hafızaya alınır ve sonraki açılışta doğrudan MediaKit ile başlar. Kapalıyken her seferinde seçtiğiniz motorla başlar; kanal motoru hatırlanmaz."
                    } else {
                        "When Better is selected, playback always tries Better → HLS/TS ↔ TS/HLS → MediaKit if needed. If this setting is on, a channel that opened on MediaKit is remembered and starts on MediaKit next time. When off, playback always starts with your chosen engine."
                    },
                    color = g.muted,
                    style = MaterialTheme.typography.bodySmall
                )
                PlaybackTile(
                    icon = Icons.Filled.AutoAwesome,
                    title = if (on) {
                        if (tr) "Kanal hafızası açık" else "Channel memory on"
                    } else if (tr) "Kanal hafızası kapalı" else "Channel memory off",
                    subtitle = "",
                    showSwitch = true,
                    checked = on,
                    onClick = onToggle
                )
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    GlassButton(if (tr) "Kapat" else "Close") { onDismiss() }
                }
            }
        }
    }
}

@Composable
private fun ExternalPlayerDialog(
    settings: RaySettings,
    tr: Boolean,
    onDismiss: () -> Unit,
    onEnabled: (Boolean) -> Unit,
    onPick: (String, String) -> Unit
) {
    val g = LocalGlass.current
    val ctx = LocalContext.current
    val apps = remember {
        val pm = ctx.packageManager
        val intent = Intent(Intent.ACTION_VIEW).setType("video/*")
        pm.queryIntentActivities(intent, PackageManager.MATCH_DEFAULT_ONLY)
            .map { it.activityInfo.applicationInfo }
            .distinctBy { it.packageName }
            .filter { it.packageName != ctx.packageName }
            .map { it.packageName to pm.getApplicationLabel(it).toString() }
    }
    Dialog(onDismissRequest = onDismiss) {
        GlassPanel(strong = true, radius = 20.dp, modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    if (tr) "Harici Oynatıcı" else "External player",
                    color = g.text,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.ExtraBold
                )
                Text(
                    if (tr) "Yayını VLC, MX Player veya Just Player gibi yüklü bir uygulamada aç."
                    else "Open the stream in an installed app such as VLC, MX Player or Just Player.",
                    color = g.muted,
                    style = MaterialTheme.typography.bodySmall
                )
                SettingsToggleRow(
                    if (tr) "Harici oynatıcıyı kullan" else "Use external player",
                    if (settings.externalPlayerEnabled) (if (tr) "Aktif" else "On") else if (tr) "Pasif" else "Off",
                    settings.externalPlayerEnabled
                ) { onEnabled(!settings.externalPlayerEnabled) }
                if (settings.externalPlayerEnabled) {
                    GlassButton(if (tr) "Her seferinde sor (Android seçici)" else "Ask every time (Android chooser)") {
                        onPick("", if (tr) "Sistem seçici" else "System chooser")
                    }
                    if (apps.isEmpty()) {
                        Text(if (tr) "Yüklü oynatıcı bulunamadı" else "No installed player found", color = g.muted)
                    } else {
                        apps.forEach { (pkg, label) ->
                            val on = settings.externalPlayerPackage == pkg
                            GlassButton((if (on) "●  " else "") + label) { onPick(pkg, label) }
                        }
                    }
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    GlassButton(if (tr) "Kapat" else "Close") { onDismiss() }
                }
            }
        }
    }
}

@Composable
private fun UserAgentDialog(
    settings: RaySettings,
    tr: Boolean,
    onDismiss: () -> Unit,
    onPick: (UserAgentPreset, String) -> Unit
) {
    var selected by remember { mutableStateOf(settings.userAgentPreset) }
    var custom by remember { mutableStateOf(settings.customUserAgent) }
    val g = LocalGlass.current
    Dialog(onDismissRequest = onDismiss) {
        GlassPanel(strong = true, radius = 20.dp, modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("User Agent", color = g.text, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.ExtraBold)
                Text(
                    if (tr) "Bazı paneller belirli bir tarayıcı/oynatıcı kimliği bekler."
                    else "Some panels require a specific browser/player identity.",
                    color = g.muted,
                    style = MaterialTheme.typography.bodySmall
                )
                UserAgentPreset.entries.forEach { preset ->
                    val on = selected == preset
                    GlassButton((if (on) "●  " else "") + userAgentPresetLabel(preset, tr)) { selected = preset }
                }
                if (selected == UserAgentPreset.CUSTOM) {
                    GlassField("User-Agent", custom) { custom = it }
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End, verticalAlignment = Alignment.CenterVertically) {
                    GlassButton(if (tr) "Vazgeç" else "Cancel") { onDismiss() }
                    Spacer(Modifier.width(8.dp))
                    GlassButton(if (tr) "Kaydet" else "Save") { onPick(selected, custom) }
                }
            }
        }
    }
}

@Composable
private fun PlaybackHeader(
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
private fun PlaybackTile(
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

private fun engineName(engine: PlaybackEngine) = when (engine) {
    PlaybackEngine.BETTER -> "Better Player"
    PlaybackEngine.MEDIA_KIT -> "MediaKit"
}

private fun userAgentLabel(settings: RaySettings, tr: Boolean): String {
    if (settings.userAgentPreset == UserAgentPreset.CUSTOM) {
        val v = settings.customUserAgent.trim()
        if (v.isBlank()) return if (tr) "Özel (boş — varsayılan kullanılıyor)" else "Custom (empty — using default)"
        val short = if (v.length <= 48) v else v.take(48) + "…"
        return if (tr) "Özel: $short" else "Custom: $short"
    }
    return userAgentPresetLabel(settings.userAgentPreset, tr)
}

private fun userAgentPresetLabel(preset: UserAgentPreset, tr: Boolean) = when (preset) {
    UserAgentPreset.DEFAULT -> if (tr) "Mozilla / Chrome (Varsayılan)" else "Mozilla / Chrome (Default)"
    UserAgentPreset.CHROME -> "Chrome"
    UserAgentPreset.VLC -> "VLC"
    UserAgentPreset.EXOPLAYER -> "ExoPlayer"
    UserAgentPreset.KODI -> "Kodi"
    UserAgentPreset.TIZEN -> "Samsung Smart TV"
    UserAgentPreset.WEBOS -> "LG Smart TV"
    UserAgentPreset.ANDROID_TV -> "Android TV"
    UserAgentPreset.APPLE_TV -> "Apple TV"
    UserAgentPreset.ROKU -> "Roku"
    UserAgentPreset.OKHTTP -> "okhttp"
    UserAgentPreset.SAFARI -> "Safari"
    UserAgentPreset.CUSTOM -> if (tr) "Özel" else "Custom"
}

private fun subtitleColorLabel(color: String, tr: Boolean) = when (color.lowercase()) {
    "yellow" -> if (tr) "Sarı" else "Yellow"
    "cyan" -> if (tr) "Camgöbeği" else "Cyan"
    "green" -> if (tr) "Yeşil" else "Green"
    "orange" -> if (tr) "Turuncu" else "Orange"
    "pink" -> if (tr) "Pembe" else "Pink"
    else -> if (tr) "Beyaz" else "White"
}

private fun subtitleFontLabel(font: String, tr: Boolean) = when (font) {
    "serif" -> "Serif"
    "mono" -> if (tr) "Eş aralıklı" else "Monospace"
    else -> if (tr) "Sans" else "Sans"
}
