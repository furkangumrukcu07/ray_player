@file:OptIn(androidx.media3.common.util.UnstableApi::class)

package com.ray.iptv.player

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.util.Log
import android.view.Surface
import android.view.SurfaceHolder
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.Tracks
import androidx.media3.common.MimeTypes
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.datasource.HttpDataSource
import androidx.media3.datasource.okhttp.OkHttpDataSource
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.dash.DashMediaSource
import androidx.media3.exoplayer.hls.HlsMediaSource
import androidx.media3.exoplayer.mediacodec.MediaCodecSelector
import androidx.media3.exoplayer.rtsp.RtspMediaSource
import androidx.media3.exoplayer.source.ProgressiveMediaSource
import androidx.media3.exoplayer.trackselection.DefaultTrackSelector
import androidx.media3.extractor.DefaultExtractorsFactory
import com.ray.iptv.data.repo.PlaybackEngine
import com.ray.iptv.data.repo.StreamFormat
import com.ray.iptv.net.PlaybackIdentity
import dagger.hilt.android.qualifiers.ApplicationContext
import `is`.xyz.mpv.MPV
import `is`.xyz.mpv.MPVNode
import java.io.File
import java.util.Locale
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import okhttp3.OkHttpClient
import javax.inject.Inject
import javax.inject.Singleton

data class TrackOption(
    val id: String,
    val label: String,
    val selected: Boolean,
    val language: String = ""
)
enum class PlayErrorKind { NONE, FORBIDDEN, MISSING, NETWORK, SOURCE, DECODER }

data class PlayerUiState(
    val playing: Boolean = false,
    val buffering: Boolean = false,
    val playWhenReady: Boolean = true,
    val position: Long = 0,
    val duration: Long = 0,
    val live: Boolean = false,
    val error: String = "",
    val errorKind: PlayErrorKind = PlayErrorKind.NONE,
    val videoSize: String = "",
    val audioTracks: List<TrackOption> = emptyList(),
    val textTracks: List<TrackOption> = emptyList(),
    val videoTracks: List<TrackOption> = emptyList(),
    val ended: Boolean = false,
    val engine: PlaybackEngine = PlaybackEngine.BETTER
)

@Singleton
class RayPlayer @Inject constructor(
    @ApplicationContext private val context: Context,
    private val okHttp: OkHttpClient
) {
    private var softwareDecoder = false
    private var liveBufferSeconds = 0
    private var subtitleAuto = false
    private var preferredSubtitleToken = ""
    private var subtitleUserPicked = false
    private var lastAppliedSubtitleId: String? = null
    private var forLive = true
    private var lowEnd = false
    private var engine = PlaybackEngine.BETTER
    private var mediaKitLowPower = false
    private var ignoreSsl = true
    private var subtitleSize = 22
    private var subtitleOutline = true
    private var subtitleColor = "white"
    private var subtitleFont = "sans"
    private var liveRawTs = false
    private var liveUhdHls = false
    private val socHints = AndroidPlaybackSocHints.get(context)
    private val trackSelector = DefaultTrackSelector(context)
    private var player: ExoPlayer? = buildPlayer()
    val exo: ExoPlayer? get() = player
    val usingMediaKit: Boolean get() = engine == PlaybackEngine.MEDIA_KIT

    private var mpv: MPV? = null
    private var pendingMpvUrl: String? = null
    private var pendingMpvStartMs = 0L
    private var mpvSurfaceBound = false
    private var mpvUhdPromoted = false
    private var lastMpvUa = ""
    private var lastMpvReferer = ""
    private val mainHandler = Handler(Looper.getMainLooper())
    private var lastMpvPosPostMs = 0L
    private val mpvObserver = object : MPV.EventObserver {
        override fun eventProperty(property: String) {
            if (property.startsWith("track-list")) postUi { refreshMpvTracks() }
        }
        override fun eventProperty(property: String, value: Long) {
            when (property) {
                "video-params/w", "video-params/h", "video-params/dw", "video-params/dh" -> postUi {
                    val w = mpv?.getPropertyInt("video-params/dw") ?: mpv?.getPropertyInt("video-params/w") ?: 0
                    val h = mpv?.getPropertyInt("video-params/dh") ?: mpv?.getPropertyInt("video-params/h") ?: 0
                    if (w > 0 && h > 0) {
                        _state.value = _state.value.copy(videoSize = "${w}x$h")
                        maybePromoteUhd(h)
                    }
                }
                "track-list/count" -> postUi { refreshMpvTracks() }
            }
        }
        override fun eventProperty(property: String, value: Boolean) {
            when (property) {
                "pause" -> postUi {
                    _state.value = _state.value.copy(playing = !value && _state.value.playWhenReady)
                }
                "paused-for-cache" -> postUi { _state.value = _state.value.copy(buffering = value) }
                "eof-reached" -> if (value) postUi {
                    _state.value = _state.value.copy(ended = true, playing = false)
                }
                "seeking" -> postUi {
                    _state.value = _state.value.copy(buffering = value || _state.value.buffering)
                }
            }
        }
        override fun eventProperty(property: String, value: String) = Unit
        override fun eventProperty(property: String, value: Double) {
            when (property) {
                "time-pos" -> {
                    val now = SystemClock.uptimeMillis()
                    if (now - lastMpvPosPostMs < 250L) return
                    lastMpvPosPostMs = now
                    postUi {
                        _state.value = _state.value.copy(position = (value * 1000).toLong())
                    }
                }
                "duration" -> if (value.isFinite() && value > 0) postUi {
                    _state.value = _state.value.copy(duration = (value * 1000).toLong(), live = false)
                }
            }
        }
        override fun eventProperty(property: String, value: MPVNode) = Unit
        override fun event(eventId: Int, data: MPVNode) {
            when (eventId) {
                MPV.mpvEvent.MPV_EVENT_FILE_LOADED -> postUi {
                    _state.value = _state.value.copy(
                        error = "",
                        errorKind = PlayErrorKind.NONE,
                        ended = false,
                        buffering = false
                    )
                    refreshMpvTracks()
                    applyMpvSubtitles()
                }
                MPV.mpvEvent.MPV_EVENT_END_FILE -> postUi { onMpvEndFile(data) }
                MPV.mpvEvent.MPV_EVENT_PLAYBACK_RESTART -> postUi {
                    _state.value = _state.value.copy(
                        buffering = false,
                        playing = mpv?.getPropertyBoolean("pause") != true && _state.value.playWhenReady
                    )
                }
            }
        }
    }

    private val _state = MutableStateFlow(PlayerUiState())
    val state: StateFlow<PlayerUiState> = _state

    init {
        attachListener()
    }

    fun configure(
        software: Boolean,
        bufferSec: Int,
        autoSubs: Boolean,
        live: Boolean = true,
        lowEnd: Boolean = false,
        engine: PlaybackEngine = PlaybackEngine.BETTER,
        mediaKitLowPower: Boolean = false,
        ignoreSsl: Boolean = true,
        subtitleSize: Int = 22,
        subtitleOutline: Boolean = true,
        subtitleColor: String = "white",
        subtitleFont: String = "sans",
        preferredSubtitle: String = ""
    ) {
        val sameCore = software == softwareDecoder &&
            bufferSec == liveBufferSeconds &&
            live == forLive &&
            lowEnd == this.lowEnd &&
            engine == this.engine &&
            mediaKitLowPower == this.mediaKitLowPower &&
            ignoreSsl == this.ignoreSsl
        this.subtitleAuto = autoSubs
        this.preferredSubtitleToken = preferredSubtitle.trim().lowercase()
        this.subtitleSize = subtitleSize
        this.subtitleOutline = subtitleOutline
        this.subtitleColor = subtitleColor
        this.subtitleFont = subtitleFont
        this.ignoreSsl = ignoreSsl
        if (sameCore) {
            if (engine == PlaybackEngine.MEDIA_KIT) applyMpvSubtitles()
            return
        }
        val hwChanged = software != softwareDecoder || mediaKitLowPower != this.mediaKitLowPower
        val engineChanged = engine != this.engine
        softwareDecoder = software
        liveBufferSeconds = bufferSec
        forLive = live
        this.lowEnd = lowEnd
        this.engine = engine
        this.mediaKitLowPower = mediaKitLowPower
        _state.value = _state.value.copy(engine = engine)
        if (!live) {
            liveRawTs = false
            liveUhdHls = false
        }
        if (engine == PlaybackEngine.MEDIA_KIT) {
            releaseExoForMediaKit()
            if (engineChanged || hwChanged) destroyMpv()
        } else {
            destroyMpv()
            rebuildPlayerKeepingReady()
        }
    }

    fun play(
        url: String,
        startMs: Long = 0L,
        userAgent: String = "",
        referer: String = "",
        speed: Float = 1f,
        format: StreamFormat = StreamFormat.AUTO,
        externalSubtitleUri: String = ""
    ) {
        _state.value = _state.value.copy(
            error = "",
            errorKind = PlayErrorKind.NONE,
            ended = false,
            engine = engine
        )
        subtitleUserPicked = false
        lastAppliedSubtitleId = null
        val playUrl = rewrite(url, format)
        runCatching {
            if (engine == PlaybackEngine.MEDIA_KIT) {
                playMpv(playUrl, startMs, userAgent, referer, speed)
                return
            }
            destroyMpv()
            val kind = StreamHints.kind(playUrl, format)
            val raw = forLive && kind == StreamHints.Kind.TS
            val uhd = forLive && kind == StreamHints.Kind.HLS && ExoDeviceBuffers.urlLooksUhd(playUrl)
            if (raw != liveRawTs || uhd != liveUhdHls) {
                liveRawTs = raw
                liveUhdHls = uhd
                rebuildPlayerKeepingReady()
            }
            if (!forLive) disableExoTextTracks()
            val httpFactory = OkHttpDataSource.Factory(okHttp)
                .setUserAgent(userAgent.ifBlank { PlaybackIdentity.userAgent })
                .apply {
                    if (referer.isNotBlank()) setDefaultRequestProperties(mapOf("Referer" to referer))
                }
            val factory = DefaultDataSource.Factory(context, httpFactory)
            val item = mediaItem(playUrl, kind, externalSubtitleUri)
            val start = startMs.takeIf { it > 0 } ?: C.TIME_UNSET
            val retry = IptvLoadErrorPolicy(forLive || StreamHints.liveIptv(playUrl))
            val source = when (kind) {
                StreamHints.Kind.RTSP -> RtspMediaSource.Factory().createMediaSource(item)
                StreamHints.Kind.HLS ->
                    HlsMediaSource.Factory(factory)
                        .setAllowChunklessPreparation(true)
                        .setLoadErrorHandlingPolicy(retry)
                        .createMediaSource(item)
                StreamHints.Kind.DASH ->
                    DashMediaSource.Factory(factory)
                        .setLoadErrorHandlingPolicy(retry)
                        .createMediaSource(item)
                StreamHints.Kind.TS, StreamHints.Kind.OTHER -> {
                    val extractors = DefaultExtractorsFactory()
                        .setTsExtractorFlags(1)
                        .setTsExtractorTimestampSearchBytes(1500 * 188)
                    ProgressiveMediaSource.Factory(factory, extractors)
                        .setLoadErrorHandlingPolicy(retry)
                        .createMediaSource(item)
                }
            }
            val exoPlayer = ensureExo()
            exoPlayer.setMediaSource(source, start)
            exoPlayer.setPlaybackSpeed(speed)
            exoPlayer.prepare()
            exoPlayer.playWhenReady = true
        }.onFailure { err ->
            Log.e(TAG, "play failed", err)
            _state.value = _state.value.copy(
                error = err.message ?: "Playback failed",
                errorKind = PlayErrorKind.SOURCE,
                playing = false,
                buffering = false
            )
        }
    }

    fun pause() {
        if (engine == PlaybackEngine.MEDIA_KIT) {
            mpv?.setPropertyBoolean("pause", true)
            _state.value = _state.value.copy(playing = false, playWhenReady = false)
        } else {
            player?.pause()
        }
    }
    fun resume() {
        if (engine == PlaybackEngine.MEDIA_KIT) {
            mpv?.setPropertyBoolean("pause", false)
            _state.value = _state.value.copy(playWhenReady = true, playing = true)
        } else {
            player?.play()
        }
    }
    fun stop() {
        if (engine == PlaybackEngine.MEDIA_KIT) {
            runCatching { mpv?.setPropertyBoolean("pause", true) }
            runCatching { mpv?.command("stop") }
            detachMpvSurface()
            _state.value = _state.value.copy(playing = false, playWhenReady = false)
        } else {
            player?.playWhenReady = false
            player?.stop()
            player?.clearVideoSurface()
            _state.value = _state.value.copy(playing = false, playWhenReady = false)
        }
    }
    fun seek(ms: Long) {
        if (engine == PlaybackEngine.MEDIA_KIT) {
            mpv?.command("seek", (ms / 1000.0).toString(), "absolute")
        } else {
            player?.seekTo(ms)
        }
    }
    fun seekBy(delta: Long) {
        if (engine == PlaybackEngine.MEDIA_KIT) {
            mpv?.command("seek", (delta / 1000.0).toString(), "relative")
        } else {
            val p = player ?: return
            p.seekTo((p.currentPosition + delta).coerceAtLeast(0))
        }
    }
    fun setSpeed(speed: Float) {
        if (engine == PlaybackEngine.MEDIA_KIT) mpv?.setPropertyDouble("speed", speed.toDouble())
        else player?.setPlaybackSpeed(speed)
    }

    fun snapshot(): PlayerUiState {
        if (engine == PlaybackEngine.MEDIA_KIT) {
            val pos = ((mpv?.getPropertyDouble("time-pos") ?: 0.0) * 1000).toLong()
            val dur = ((mpv?.getPropertyDouble("duration") ?: 0.0) * 1000).toLong().coerceAtLeast(0)
            val pause = mpv?.getPropertyBoolean("pause") == true
            return _state.value.copy(
                position = pos,
                duration = dur,
                playing = !pause && _state.value.playWhenReady,
                live = forLive || dur <= 0
            )
        }
        val p = player ?: return _state.value
        val vs = p.videoSize
        return _state.value.copy(
            position = p.currentPosition,
            duration = p.duration.coerceAtLeast(0),
            playing = p.isPlaying,
            live = p.isCurrentMediaItemLive,
            videoSize = if (vs.width > 0 && vs.height > 0) "${vs.width}x${vs.height}" else _state.value.videoSize
        )
    }

    fun selectAudio(id: String) {
        if (engine == PlaybackEngine.MEDIA_KIT) mpv?.setPropertyString("aid", id) else select(C.TRACK_TYPE_AUDIO, id)
    }
    fun selectText(id: String, fromUser: Boolean = false) {
        if (fromUser) subtitleUserPicked = true
        lastAppliedSubtitleId = id
        if (engine == PlaybackEngine.MEDIA_KIT) {
            if (id == "no") mpv?.setPropertyString("sid", "no") else mpv?.setPropertyString("sid", id)
        } else if (id == "no") {
            disableExoTextTracks()
        } else {
            player?.trackSelectionParameters = player?.trackSelectionParameters
                ?.buildUpon()
                ?.setTrackTypeDisabled(C.TRACK_TYPE_TEXT, false)
                ?.build() ?: return
            select(C.TRACK_TYPE_TEXT, id)
        }
    }
    fun selectVideo(id: String) {
        if (engine == PlaybackEngine.MEDIA_KIT) {
            mpv?.setPropertyString("vid", id)
        } else {
            select(C.TRACK_TYPE_VIDEO, id)
        }
    }

    fun release() {
        destroyMpv()
        player?.release()
        player = null
    }

    fun attachMpvSurface(holder: SurfaceHolder) {
        val surface = holder.surface ?: return
        val w = holder.surfaceFrame.width()
        val h = holder.surfaceFrame.height()
        attachMpvSurface(surface, w, h, preferCopyHwdec = false)
    }

    fun attachMpvSurface(surface: Surface, width: Int, height: Int, preferCopyHwdec: Boolean = false) {
        if (!surface.isValid) return
        ensureMpv()
        val m = mpv ?: return
        runCatching { m.attachSurface(surface) }
        m.setPropertyString("force-window", "yes")
        m.setPropertyString("vo", "gpu")
        if (preferCopyHwdec && !softwareDecoder) {
            val hw = m.getPropertyString("hwdec").orEmpty()
            if (hw == "mediacodec" || hw.isBlank()) m.setPropertyString("hwdec", "mediacodec-copy")
        }
        mpvSurfaceBound = true
        if (width > 0 && height > 0) m.setPropertyString("android-surface-size", "${width}x${height}")
        val queued = pendingMpvUrl
        if (queued != null) {
            pendingMpvUrl = null
            loadMpvFile(queued, pendingMpvStartMs)
        }
    }

    fun detachMpvSurface() {
        mpvSurfaceBound = false
        val m = mpv ?: return
        runCatching {
            m.setPropertyString("vo", "null")
            m.setPropertyString("force-window", "no")
            m.detachSurface()
        }
    }

    fun resizeMpvSurface(width: Int, height: Int) {
        if (width > 0 && height > 0) {
            mpv?.setPropertyString("android-surface-size", "${width}x$height")
        }
    }

    fun applyVideoLayout(fill: Boolean, zoom: Boolean) {
        if (engine != PlaybackEngine.MEDIA_KIT) return
        val m = mpv ?: return
        when {
            fill -> {
                m.setPropertyString("keepaspect", "no")
                m.setPropertyString("panscan", "0.0")
            }
            zoom -> {
                m.setPropertyString("keepaspect", "yes")
                m.setPropertyString("panscan", "1.0")
            }
            else -> {
                m.setPropertyString("keepaspect", "yes")
                m.setPropertyString("panscan", "0.0")
            }
        }
    }

    private fun playMpv(url: String, startMs: Long, userAgent: String, referer: String, speed: Float) {
        lastMpvUa = userAgent.ifBlank { PlaybackIdentity.userAgent }
        lastMpvReferer = referer
        ensureMpv()
        val m = mpv ?: return
        MediaKitMpvOptions.applyPlayback(
            mpv = m,
            hints = socHints,
            live = forLive,
            url = url,
            software = softwareDecoder,
            lowPower = mediaKitLowPower,
            ignoreSsl = ignoreSsl,
            userBufferSec = liveBufferSeconds,
            userAgent = lastMpvUa,
            referer = lastMpvReferer
        )
        applyMpvSubtitles()
        m.setPropertyDouble("speed", speed.toDouble())
        _state.value = _state.value.copy(
            playWhenReady = true,
            playing = false,
            buffering = true,
            live = forLive,
            engine = PlaybackEngine.MEDIA_KIT
        )
        if (mpvSurfaceBound) loadMpvFile(url, startMs) else {
            pendingMpvUrl = url
            pendingMpvStartMs = startMs
        }
    }

    private fun loadMpvFile(url: String, startMs: Long) {
        val m = mpv ?: return
        val result = runCatching {
            if (startMs > 0) {
                val start = String.format(Locale.US, "%.3f", startMs / 1000.0)
                m.command("loadfile", url, "replace", "start=$start")
            } else {
                m.command("loadfile", url, "replace")
            }
            m.setPropertyBoolean("pause", false)
        }
        result.exceptionOrNull()?.let { err ->
            Log.e(TAG, "libmpv loadfile failed", err)
            _state.value = _state.value.copy(
                error = err.message ?: "MediaKit load failed",
                errorKind = PlayErrorKind.SOURCE,
                playing = false,
                buffering = false
            )
        }
    }

    private fun ensureMpv() {
        if (mpv != null) return
        val created = runCatching {
            val configDir = File(context.filesDir, "mpv").apply { mkdirs() }.absolutePath
            val cacheDir = File(context.cacheDir, "mpv").apply { mkdirs() }.absolutePath
            val inst = MPV()
            inst.create(context)
            inst.setOptionString("config", "yes")
            inst.setOptionString("config-dir", configDir)
            inst.setOptionString("gpu-shader-cache-dir", cacheDir)
            inst.setOptionString("icc-cache-dir", cacheDir)
            val hwdec = MediaKitMpvOptions.hwdecValue(
                hints = socHints,
                software = softwareDecoder,
                lowPower = mediaKitLowPower,
                uhd = false
            )
            MediaKitMpvOptions.applyBeforeInit(inst, hwdec)
            inst.setOptionString("idle", "yes")
            inst.init()
            inst.setOptionString("force-window", "no")
            inst.addObserver(mpvObserver)
            observeMpv(inst)
            Log.i(TAG, "libmpv ready hwdec=$hwdec")
            inst
        }.getOrElse { err ->
            Log.e(TAG, "libmpv init failed", err)
            _state.value = _state.value.copy(
                error = err.message ?: "MediaKit failed to start",
                errorKind = PlayErrorKind.DECODER,
                playing = false,
                buffering = false
            )
            return
        }
        mpv = created
        mpvUhdPromoted = false
    }

    private fun observeMpv(m: MPV) {
        val fmt = MPV.mpvFormat
        m.observeProperty("pause", fmt.MPV_FORMAT_FLAG)
        m.observeProperty("time-pos", fmt.MPV_FORMAT_DOUBLE)
        m.observeProperty("duration", fmt.MPV_FORMAT_DOUBLE)
        m.observeProperty("eof-reached", fmt.MPV_FORMAT_FLAG)
        m.observeProperty("paused-for-cache", fmt.MPV_FORMAT_FLAG)
        m.observeProperty("seeking", fmt.MPV_FORMAT_FLAG)
        m.observeProperty("track-list/count", fmt.MPV_FORMAT_INT64)
        m.observeProperty("video-params/w", fmt.MPV_FORMAT_INT64)
        m.observeProperty("video-params/h", fmt.MPV_FORMAT_INT64)
        m.observeProperty("video-params/dw", fmt.MPV_FORMAT_INT64)
        m.observeProperty("video-params/dh", fmt.MPV_FORMAT_INT64)
    }

    private fun destroyMpv() {
        pendingMpvUrl = null
        pendingMpvStartMs = 0L
        mpvSurfaceBound = false
        mpvUhdPromoted = false
        val m = mpv ?: return
        mpv = null
        runCatching { m.removeObserver(mpvObserver) }
        runCatching {
            m.setPropertyString("vo", "null")
            m.detachSurface()
        }
        runCatching { m.destroy() }
    }

    private fun applyMpvSubtitles() {
        val m = mpv ?: return
        MediaKitMpvOptions.applySubtitles(m, subtitleSize, subtitleColor, subtitleOutline, subtitleFont)
    }

    private fun disableExoTextTracks() {
        if (engine == PlaybackEngine.MEDIA_KIT) return
        val p = player ?: return
        p.trackSelectionParameters = p.trackSelectionParameters
            .buildUpon()
            .setTrackTypeDisabled(C.TRACK_TYPE_TEXT, true)
            .clearOverridesOfType(C.TRACK_TYPE_TEXT)
            .build()
    }

    private fun applyVodSubtitlePolicy() {
        if (forLive || subtitleUserPicked) return
        val tracks = _state.value.textTracks
        if (tracks.isEmpty()) return
        val preferred = preferredSubtitleToken
        val match = if (preferred.isNotBlank()) {
            tracks.firstOrNull { SubtitleLanguages.matches(it.language, it.label, preferred) }
        } else {
            null
        }
        val want = when {
            match != null -> match.id
            subtitleAuto -> tracks.first().id
            else -> "no"
        }
        if (want == lastAppliedSubtitleId) return
        selectText(want, fromUser = false)
    }

    private fun refreshMpvTracks() {
        val m = mpv ?: return
        val n = m.getPropertyInt("track-list/count") ?: 0
        val audio = mutableListOf<TrackOption>()
        val text = mutableListOf<TrackOption>()
        val video = mutableListOf<TrackOption>()
        for (i in 0 until n) {
            val type = m.getPropertyString("track-list/$i/type") ?: continue
            if (m.getPropertyBoolean("track-list/$i/albumart") == true) continue
            val id = m.getPropertyString("track-list/$i/id") ?: continue
            val lang = m.getPropertyString("track-list/$i/lang").orEmpty()
            val title = m.getPropertyString("track-list/$i/title").orEmpty()
            val selected = m.getPropertyBoolean("track-list/$i/selected") == true
            val w = m.getPropertyInt("track-list/$i/demux-w") ?: 0
            val h = m.getPropertyInt("track-list/$i/demux-h") ?: 0
            val dims = if (w > 0 && h > 0) " (${w}×${h})" else ""
            val label = title.ifBlank { lang.ifBlank { "Track $id" } } + dims
            when (type) {
                "audio" -> audio += TrackOption(id, label, selected, lang)
                "sub" -> text += TrackOption(id, label, selected, lang)
                "video" -> video += TrackOption(id, label, selected, lang)
            }
        }
        _state.value = _state.value.copy(audioTracks = audio, textTracks = text, videoTracks = video)
        applyVodSubtitlePolicy()
    }

    private fun maybePromoteUhd(height: Int) {
        if (mpvUhdPromoted || softwareDecoder || height < 2160) return
        mpvUhdPromoted = true
        mpv?.setPropertyString("hwdec", "mediacodec")
        Log.i(TAG, "MediaKit UHD promote hwdec=mediacodec h=$height")
    }

    private fun onMpvEndFile(data: MPVNode) {
        val reason = data["reason"]?.asInt() ?: data.asInt() ?: 0L
        when (reason) {
            4L -> {
                val msg = mpv?.getPropertyString("error-string")
                    ?: mpv?.getPropertyString("path")
                    ?: "MediaKit playback failed"
                _state.value = _state.value.copy(
                    error = msg,
                    errorKind = classifyMpv(msg),
                    ended = false,
                    playing = false
                )
            }
            0L -> _state.value = _state.value.copy(ended = true, playing = false)
        }
    }

    private fun classifyMpv(msg: String): PlayErrorKind {
        val t = msg.lowercase(Locale.US)
        if (t.contains("403") || t.contains("401") || t.contains("forbidden")) return PlayErrorKind.FORBIDDEN
        if (t.contains("404") || t.contains("not found")) return PlayErrorKind.MISSING
        if (t.contains("timeout") || t.contains("network") || t.contains("connection") || t.contains("tls") || t.contains("ssl")) {
            return PlayErrorKind.NETWORK
        }
        if (t.contains("decoder") || t.contains("hwdec") || t.contains("codec") || t.contains("mediacodec")) {
            return PlayErrorKind.DECODER
        }
        return PlayErrorKind.SOURCE
    }

    private fun postUi(block: () -> Unit) {
        if (Looper.myLooper() == Looper.getMainLooper()) block()
        else mainHandler.post(block)
    }

    private fun rebuildPlayerKeepingReady() {
        val current = player
        val ready = current?.playWhenReady ?: true
        runCatching {
            current?.release()
            val created = buildPlayer()
            player = created
            attachListener()
            created.playWhenReady = ready
        }.onFailure { err ->
            Log.e(TAG, "rebuild ExoPlayer failed", err)
            runCatching {
                val created = buildPlayer()
                player = created
                attachListener()
            }
            _state.value = _state.value.copy(
                error = err.message ?: "Player restart failed",
                errorKind = PlayErrorKind.DECODER,
                playing = false,
                buffering = false
            )
        }
    }

    private fun ensureExo(): ExoPlayer {
        player?.let { return it }
        val created = buildPlayer()
        player = created
        attachListener()
        return created
    }

    private fun releaseExoForMediaKit() {
        val p = player ?: return
        runCatching {
            p.stop()
            p.playWhenReady = false
            p.clearMediaItems()
        }
        if (socHints.androidTv || socHints.playbackChallengedTv || socHints.oneGiBRamClass) {
            runCatching { p.release() }
            player = null
        }
    }

    private fun buildPlayer(): ExoPlayer {
        val buf = ExoDeviceBuffers.resolve(
            hints = socHints,
            live = forLive,
            userSec = liveBufferSeconds,
            rawTs = liveRawTs,
            uhdHls = liveUhdHls
        )
        Log.i(
            TAG,
            "exo ${if (forLive) "live" else "vod"} " +
                "min=${buf.minMs} max=${buf.maxMs} start=${buf.playbackMs} " +
                "rebuffer=${buf.rebufferMs} bytes=${buf.targetBytes / (1024 * 1024)}MB " +
                "time=${buf.prioritizeTime} ts=$liveRawTs uhd=$liveUhdHls " +
                "seg=${socHints.playbackSegment} subclass=${socHints.challengedTvSubclass}"
        )
        val min = buf.minMs
        val max = buf.maxMs
        val playback = buf.playbackMs
        val rebuffer = buf.rebufferMs
        val bytes = buf.targetBytes
        val renderers = DefaultRenderersFactory(context)
            .setEnableDecoderFallback(true)
            .setEnableAudioTrackPlaybackParams(true)
            .setAllowedVideoJoiningTimeMs(5000)
            .setExtensionRendererMode(DefaultRenderersFactory.EXTENSION_RENDERER_MODE_PREFER)
            .setMediaCodecSelector { mime, secure, tunnel ->
                val all = runCatching {
                    androidx.media3.exoplayer.mediacodec.MediaCodecUtil.getDecoderInfos(mime, secure, false)
                }.getOrDefault(emptyList())
                if (softwareDecoder) all.filter { !it.hardwareAccelerated }.ifEmpty { all } else all
            }
        val trackParams = trackSelector.buildUponParameters()
            .setTunnelingEnabled(false)
            .setPreferredAudioMimeTypes(
                MimeTypes.AUDIO_E_AC3,
                MimeTypes.AUDIO_E_AC3_JOC,
                MimeTypes.AUDIO_AC3,
                MimeTypes.AUDIO_AC4,
                MimeTypes.AUDIO_DTS,
                MimeTypes.AUDIO_DTS_HD,
                MimeTypes.AUDIO_TRUEHD,
                MimeTypes.AUDIO_AAC,
                MimeTypes.AUDIO_MPEG,
                MimeTypes.AUDIO_OPUS,
                MimeTypes.AUDIO_FLAC
            )
        val maxH = socHints.adaptiveMaxVideoHeightHint()
        if (maxH != null) {
            val maxW = if (maxH <= 720) 1280 else 1920
            trackParams.setMaxVideoSize(maxW, maxH)
        } else {
            trackParams.clearVideoSizeConstraints()
        }
        trackSelector.parameters = trackParams.build()
        return ExoPlayer.Builder(context, renderers)
            .setTrackSelector(trackSelector)
            .setLoadControl(
                DefaultLoadControl.Builder()
                    .setBufferDurationsMs(min, max, playback, rebuffer)
                    .setTargetBufferBytes(bytes)
                    .setPrioritizeTimeOverSizeThresholds(buf.prioritizeTime)
                    .build()
            )
            .setSeekBackIncrementMs(15_000)
            .setSeekForwardIncrementMs(15_000)
            .setWakeMode(C.WAKE_MODE_NETWORK)
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(C.USAGE_MEDIA)
                    .setContentType(C.AUDIO_CONTENT_TYPE_MOVIE)
                    .build(),
                true
            )
            .setHandleAudioBecomingNoisy(true)
            .build()
    }

    private var autoRetryCount = 0

    private fun attachListener() {
        val host = player ?: return
        host.addListener(object : Player.Listener {
            override fun onIsPlayingChanged(isPlaying: Boolean) {
                if (engine == PlaybackEngine.MEDIA_KIT) return
                if (isPlaying) autoRetryCount = 0
                _state.value = _state.value.copy(playing = isPlaying)
            }

            override fun onPlayWhenReadyChanged(playWhenReady: Boolean, reason: Int) {
                if (engine == PlaybackEngine.MEDIA_KIT) return
                _state.value = _state.value.copy(playWhenReady = playWhenReady)
            }

            override fun onPlaybackStateChanged(playbackState: Int) {
                if (engine == PlaybackEngine.MEDIA_KIT) return
                val p = player ?: return
                if (playbackState == Player.STATE_READY) autoRetryCount = 0
                _state.value = _state.value.copy(
                    buffering = playbackState == Player.STATE_BUFFERING,
                    live = p.isCurrentMediaItemLive,
                    ended = playbackState == Player.STATE_ENDED,
                    playWhenReady = p.playWhenReady
                )
            }

            override fun onPlayerError(error: PlaybackException) {
                if (engine == PlaybackEngine.MEDIA_KIT) return
                val p = player ?: return
                if (forLive || autoRetryCount < 3) {
                    autoRetryCount++
                    Log.w(TAG, "ExoPlayer stream error (${error.errorCodeName}), auto-recovering attempt #$autoRetryCount...")
                    mainHandler.postDelayed({
                        runCatching {
                            p.seekToDefaultPosition()
                            p.prepare()
                            p.play()
                        }
                    }, (500L * autoRetryCount).coerceAtMost(2000L))
                    return
                }
                _state.value = _state.value.copy(
                    error = humanize(error),
                    errorKind = classify(error),
                    ended = false
                )
            }

            override fun onTracksChanged(tracks: Tracks) {
                if (engine == PlaybackEngine.MEDIA_KIT) return
                _state.value = _state.value.copy(
                    audioTracks = collect(tracks, C.TRACK_TYPE_AUDIO),
                    textTracks = collect(tracks, C.TRACK_TYPE_TEXT),
                    videoTracks = collect(tracks, C.TRACK_TYPE_VIDEO)
                )
                applyVodSubtitlePolicy()
            }
        })
    }

    private fun mediaItem(url: String, kind: StreamHints.Kind, subtitleUri: String = ""): MediaItem {
        val builder = MediaItem.Builder().setUri(url)
        when (kind) {
            StreamHints.Kind.HLS -> builder.setMimeType(MimeTypes.APPLICATION_M3U8)
            StreamHints.Kind.DASH -> builder.setMimeType(MimeTypes.APPLICATION_MPD)
            StreamHints.Kind.TS -> {
                builder.setMimeType(MimeTypes.VIDEO_MP2T)
                if (StreamHints.liveIptv(url)) {
                    builder.setLiveConfiguration(MediaItem.LiveConfiguration.Builder().build())
                }
            }
            else -> Unit
        }
        if (subtitleUri.isNotBlank()) {
            val subConfig = MediaItem.SubtitleConfiguration.Builder(android.net.Uri.parse(subtitleUri))
                .setMimeType(MimeTypes.APPLICATION_SUBRIP)
                .setSelectionFlags(C.SELECTION_FLAG_DEFAULT or C.SELECTION_FLAG_FORCED)
                .build()
            builder.setSubtitleConfigurations(listOf(subConfig))
        }
        return builder.build()
    }

    private fun rewrite(url: String, format: StreamFormat): String =
        XtreamStreamUrls.applyFormat(url, format)

    private fun classify(err: PlaybackException): PlayErrorKind {
        val status = httpStatus(err)
        if (status == 401 || status == 403) return PlayErrorKind.FORBIDDEN
        if (status == 404) return PlayErrorKind.MISSING
        val msg = buildString {
            append(err.message.orEmpty())
            append(' ')
            append(err.cause?.message.orEmpty())
        }.lowercase()
        if (status in 500..599 ||
            err.errorCode == PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_FAILED ||
            err.errorCode == PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_TIMEOUT ||
            msg.contains("timeout") ||
            msg.contains("connection reset") ||
            msg.contains("connection closed") ||
            msg.contains("unexpected end of stream")
        ) return PlayErrorKind.NETWORK
        if (err.errorCode == PlaybackException.ERROR_CODE_DECODER_INIT_FAILED ||
            err.errorCode == PlaybackException.ERROR_CODE_DECODING_FAILED ||
            msg.contains("mediacodec") ||
            msg.contains("decoder")
        ) return PlayErrorKind.DECODER
        return PlayErrorKind.SOURCE
    }

    private fun httpStatus(err: PlaybackException): Int {
        var c: Throwable? = err.cause
        while (c != null) {
            if (c is HttpDataSource.InvalidResponseCodeException) return c.responseCode
            c = c.cause
        }
        return -1
    }

    private fun collect(tracks: Tracks, type: Int): List<TrackOption> {
        val out = mutableListOf<TrackOption>()
        tracks.groups.filter { it.type == type }.forEach { group ->
            for (i in 0 until group.length) {
                val fmt = group.getTrackFormat(i)
                val dims = if (fmt.height > 0) "${fmt.height}p" else ""
                val base = fmt.label ?: fmt.language ?: dims.ifBlank { "Track ${i + 1}" }
                val label = if (dims.isNotBlank() && fmt.label == null && fmt.language == null) dims
                else if (dims.isNotBlank() && !base.contains(dims)) "$base ($dims)"
                else base
                out += TrackOption(
                    id = "${group.mediaTrackGroup.id}:$i",
                    label = label,
                    selected = group.isTrackSelected(i),
                    language = fmt.language.orEmpty()
                )
            }
        }
        return out
    }

    private fun select(type: Int, id: String) {
        val p = player ?: return
        val groupId = id.substringBefore(':')
        val index = id.substringAfter(':').toIntOrNull() ?: return
        val groups = p.currentTracks.groups.filter { it.type == type }
        val group = groups.firstOrNull { it.mediaTrackGroup.id == groupId } ?: groups.firstOrNull() ?: return
        if (index in 0 until group.length) {
            p.trackSelectionParameters = p.trackSelectionParameters
                .buildUpon()
                .setTrackTypeDisabled(type, false)
                .setOverrideForType(
                    androidx.media3.common.TrackSelectionOverride(group.mediaTrackGroup, listOf(index))
                )
                .build()
        }
    }

    private fun humanize(err: PlaybackException): String {
        val code = err.errorCode
        val cause = err.cause?.message.orEmpty()
        return when {
            cause.contains("403") -> "Access denied (403). The source rejected this stream."
            cause.contains("404") -> "Stream not found (404)."
            cause.contains("509") -> "Bandwidth limit (509)."
            cause.contains("SSL", true) -> "TLS/SSL handshake failed."
            code == PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_FAILED -> "Network connection failed."
            code == PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_TIMEOUT -> "Connection timed out."
            code == PlaybackException.ERROR_CODE_DECODER_INIT_FAILED -> "This device cannot decode the stream codec."
            else -> err.message ?: "Playback failed ($code)"
        }
    }

    private companion object {
        const val TAG = "RayPlayer"
    }
}
