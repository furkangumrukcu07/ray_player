package com.ray.iptv.player

import android.util.Log
import `is`.xyz.mpv.MPV

/**
 * Mina `applyMediaKitLibmpvPlaybackOptions` + crispy lavf / canlı zap profili.
 * Yalnızca gerçek libmpv (MediaKit) yolunda kullanılır.
 */
internal object MediaKitMpvOptions {
    private const val TAG = "RayMediaKit"
    private const val VOLUME_MAX = "130"

    fun hwdecValue(
        hints: AndroidPlaybackSocHints,
        software: Boolean,
        lowPower: Boolean,
        uhd: Boolean
    ): String {
        if (software || hints.isSamsungSmT530) return "no"
        return "mediacodec"
    }

    fun applyBeforeInit(mpv: MPV, hwdec: String) {
        set(mpv, "vo", "gpu")
        set(mpv, "gpu-context", "android")
        set(mpv, "opengl-es", "yes")
        set(mpv, "hwdec", hwdec)
        set(mpv, "hwdec-codecs", "h264,hevc,mpeg4,mpeg2video,vp8,vp9,av1,vc1")
        set(mpv, "ao", "audiotrack")
        set(mpv, "keep-open", "yes")
        set(mpv, "force-window", "no")
        set(mpv, "tls-verify", "no")
    }

    fun applyPlayback(
        mpv: MPV,
        hints: AndroidPlaybackSocHints,
        live: Boolean,
        url: String,
        software: Boolean,
        lowPower: Boolean,
        ignoreSsl: Boolean,
        userBufferSec: Int,
        userAgent: String,
        referer: String
    ) {
        val sw = software || hints.isSamsungSmT530
        val hwdec = hwdecValue(hints, sw, lowPower, uhd = false)
        setProp(mpv, "volume-max", VOLUME_MAX)
        setProp(mpv, "colormatrix", "auto")
        if (!live) {
            setProp(mpv, "sub-auto", "no")
            setProp(mpv, "sid", "no")
        }
        val lavf = buildDemuxerLavfOpts(live, ignoreSsl)
        setProp(mpv, "demuxer-lavf-o", lavf)
        if (ignoreSsl) {
            setProp(mpv, "tls-verify", "no")
            setProp(mpv, "stream-lavf-o", "reconnect=1,reconnect_streamed=1,reconnect_delay_max=10,tls_verify=0")
        } else {
            setProp(mpv, "stream-lavf-o", "reconnect=1,reconnect_streamed=1,reconnect_delay_max=10")
        }
        if (userAgent.isNotBlank()) setProp(mpv, "user-agent", userAgent)
        if (referer.isNotBlank()) setProp(mpv, "referrer", referer)

        val cores = hints.cores.coerceAtLeast(1)
        val genericThreads = cores.coerceIn(1, 4)
        val deviceSeg = hints.playbackSegment
        val segment = if (deviceSeg == DevicePlaybackSegment.LOW && hints.capableChallengedTvForVod) {
            DevicePlaybackSegment.MID
        } else deviceSeg
        val isLowSeg = segment == DevicePlaybackSegment.LOW
        val isHighSeg = segment == DevicePlaybackSegment.HIGH
        setProp(mpv, "framedrop", if (isLowSeg) "yes" else "vo")
        val lavcThreads = when {
            isLowSeg -> genericThreads.coerceAtMost(2)
            isHighSeg -> cores.coerceIn(1, 8)
            else -> genericThreads
        }
        setProp(mpv, "vd-lavc-threads", "$lavcThreads")
        setProp(mpv, "profile", "fast")
        setProp(mpv, "vd-lavc-skiploopfilter", if (sw || isLowSeg) "all" else "nonref")
        setProp(mpv, "interpolation", "no")

        val challenged = hints.playbackChallengedTv
        val rawTs = live && StreamHints.mpegTs(url)
        val longHls = live && ExoDeviceBuffers.urlLooksUhd(url)
        val cache = liveCacheProfile(
            live = live,
            longHls = longHls,
            rawTs = rawTs,
            high = isHighSeg,
            low = isLowSeg,
            challengedTv = challenged,
            androidTv = hints.androidTv,
            userBufferSec = userBufferSec
        )
        setProp(mpv, "cache", "yes")
        setProp(mpv, "demuxer-readahead-secs", cache.readaheadSecs)
        setProp(mpv, "min-cache-percent", "0")
        setProp(mpv, "cache-secs", cache.cacheSecs)
        setProp(mpv, "stream-buffer-size", cache.streamBufferSize)
        setProp(mpv, "ffmpeg-fast", "yes")
        setProp(mpv, "vd-lavc-fast", "yes")
        setProp(mpv, "audio-buffer", cache.audioBuffer)

        if (live) {
            setProp(mpv, "cache-pause-initial", "no")
            setProp(mpv, "cache-pause-wait", "0")
            applyLiveZap(mpv, hints, cache)
            setProp(mpv, "video-sync", "display-resample")
            setProp(mpv, "untimed", "yes")
            setProp(mpv, "force-seekable", "yes")
        } else {
            setProp(mpv, "untimed", "no")
            setProp(mpv, "hr-seek", "no")
            setProp(mpv, "force-seekable", "yes")
            setProp(mpv, "cache-pause", "yes")
            setProp(mpv, "cache-pause-initial", "no")
            setProp(mpv, "cache-pause-wait", "1")
            val timeout = if (hints.androidTv || hints.playbackChallengedTv) "25" else "30"
            setProp(mpv, "network-timeout", timeout)
            setProp(mpv, "video-latency-hacks", "no")
            val fwd = maxOf(hints.vodDemuxerForwardMiB(), 64)
            setProp(mpv, "demuxer-max-bytes", "${fwd}M")
            setProp(mpv, "demuxer-max-back-bytes", "${fwd / 2}M")
            setProp(mpv, "demuxer-readahead-secs", "60")
            if (hints.amlogicLike) setProp(mpv, "sws-fast", "yes")
            setProp(mpv, "video-sync", "audio")
        }

        if (sw) {
            setProp(mpv, "hwdec", "no")
            setProp(mpv, "colormatrix", "auto")
            setProp(mpv, "sws-scaler", "fast-bilinear")
        }

        setProp(mpv, "scale", "bilinear")
        setProp(mpv, "cscale", "bilinear")
        setProp(mpv, "dscale", "bilinear")
        if (shouldApplyHlsBitrate(url)) {
            setProp(mpv, "hls-bitrate", if (forceMinHlsBitrate(hints)) "min" else "no")
        }
        Log.i(
            TAG,
            "mpv live=$live hwdec=$hwdec sw=$sw seg=${segment.name} " +
                "threads=$lavcThreads cache=${cache.cacheSecs}s model=${hints.model}"
        )
    }

    fun applySubtitles(
        mpv: MPV,
        sizePt: Int,
        color: String,
        outline: Boolean,
        font: String
    ) {
        val scale = (sizePt.coerceIn(10, 40) / 14.0).coerceIn(0.35, 2.85)
        setProp(mpv, "sub-font", mpvFont(font))
        setProp(mpv, "sub-scale", "%.4f".format(java.util.Locale.US, scale))
        setProp(mpv, "sub-color", mpvColor(color))
        if (outline) {
            setProp(mpv, "sub-border-color", "#000000")
            setProp(mpv, "sub-border-size", "2")
        } else {
            setProp(mpv, "sub-border-size", "0")
        }
    }

    private fun applyLiveZap(mpv: MPV, hints: AndroidPlaybackSocHints, cache: MpvLiveCacheProfile) {
        setProp(mpv, "cache", "yes")
        var step = 0
        val tvLive = hints.androidTv || hints.playbackChallengedTv
        if (tvLive || hints.playbackSegment == DevicePlaybackSegment.LOW) step = 1
        val buf = hints.liveMpvBufferStep(step)
        var cacheKiB = buf.cacheSizeKiB
        var demuxerMax = buf.demuxerMaxBytes
        var demuxerBack = buf.demuxerMaxBackBytes
        val lock = hints.oneGiBRamClass || hints.budgetTwoGiBRamClass ||
            hints.challengedTvSubclass == ChallengedTvSubclass.BUDGET_SOC ||
            hints.challengedTvSubclass == ChallengedTvSubclass.AMAZON_FIRE
        if (!lock) {
            val streamKiB = parseKiB(cache.streamBufferSize)
            if (streamKiB > cacheKiB) cacheKiB = streamKiB
            val minDemuxer = maxOf(demuxerMax, streamKiB * 1024 * 2)
            demuxerMax = minDemuxer
            demuxerBack = maxOf(demuxerBack, minDemuxer / 2)
        }
        setProp(mpv, "cache-size", "${cacheKiB}KiB")
        setProp(mpv, "cache-pause", "no")
        setProp(mpv, "cache-pause-initial", "no")
        setProp(mpv, "cache-pause-wait", "0")
        setProp(mpv, "demuxer-max-bytes", "$demuxerMax")
        setProp(mpv, "demuxer-max-back-bytes", "$demuxerBack")
        setProp(mpv, "hr-seek", "yes")
        val timeout = if (tvLive) "20" else "18"
        setProp(mpv, "network-timeout", timeout)
        setProp(mpv, "initial-audio-sync", if (tvLive) "no" else "yes")
        if (hints.amlogicLike) setProp(mpv, "video-latency-hacks", "yes")
    }

    private fun liveCacheProfile(
        live: Boolean,
        longHls: Boolean,
        rawTs: Boolean,
        high: Boolean,
        low: Boolean,
        challengedTv: Boolean,
        androidTv: Boolean,
        userBufferSec: Int
    ): MpvLiveCacheProfile {
        if (!live) {
            val streamBuf = if (high) "65536KiB" else if (low) "24576KiB" else "49152KiB"
            val readahead = if (high) "60" else if (low) "35" else "50"
            val cacheSec = if (high) "45" else if (low) "25" else "35"
            return MpvLiveCacheProfile(
                cacheSecs = cacheSec,
                readaheadSecs = readahead,
                streamBufferSize = streamBuf,
                audioBuffer = if (low) "0.6" else if (high) "0.2" else "0.4"
            )
        }
        val handheld = !challengedTv && !androidTv && !longHls && !rawTs
        if (handheld) {
            val cacheSec = if (userBufferSec > 0) userBufferSec.coerceIn(2, 15) else if (high) 5 else 4
            val readahead = if (userBufferSec > 0) {
                maxOf(cacheSec * 2, if (high) 12 else 10).coerceIn(8, 28)
            } else if (high) 12 else 10
            return MpvLiveCacheProfile(
                cacheSecs = "$cacheSec",
                readaheadSecs = "$readahead",
                streamBufferSize = if (high) "16384KiB" else "12288KiB",
                audioBuffer = "0.3"
            )
        }
        val playbackMs = if (userBufferSec > 0) (userBufferSec * 1000).coerceIn(800, 120_000) else if (high) 2500 else 5000
        val cacheSecNum = if (low) {
            maxOf(2, (playbackMs / 1000)).coerceIn(2, 12)
        } else {
            maxOf(2, (playbackMs / 1000)).coerceIn(2, 6)
        }
        val readahead = when {
            rawTs -> 40
            longHls -> maxOf(35, cacheSecNum * 3)
            high -> 20
            else -> 15
        }
        val streamKiB = when {
            rawTs -> if (high) 16384 else 8192
            longHls -> maxOf(16384, cacheSecNum * 1024)
            high -> 16384
            low -> 4096
            else -> 8192
        }
        val audio = when {
            low -> "0.6"
            longHls -> "0.7"
            high -> "0.2"
            else -> "0.4"
        }
        return MpvLiveCacheProfile(
            cacheSecs = "$cacheSecNum",
            readaheadSecs = "$readahead",
            streamBufferSize = "${streamKiB}KiB",
            audioBuffer = audio
        )
    }

    private fun buildDemuxerLavfOpts(live: Boolean, ignoreSsl: Boolean): String {
        val ext = if (ignoreSsl) "allowed_extensions=ALL,tls_verify=0" else "allowed_extensions=ALL"
        return "reconnect=1,reconnect_streamed=1,reconnect_delay_max=10,$ext"
    }

    fun shouldApplyHlsBitrate(url: String): Boolean =
        StreamHints.hls(url) || StreamHints.extensionlessWebManifest(url)

    private fun forceMinHlsBitrate(hints: AndroidPlaybackSocHints): Boolean {
        if (hints.oneGiBRamClass || hints.budgetTwoGiBRamClass) return true
        return when (hints.challengedTvSubclass) {
            ChallengedTvSubclass.BUDGET_SOC,
            ChallengedTvSubclass.AMAZON_FIRE,
            ChallengedTvSubclass.LOW_END_SMART_TV -> true
            else -> false
        }
    }

    private fun parseKiB(raw: String): Int {
        val t = raw.trim().lowercase()
        if (t.endsWith("kib")) return t.removeSuffix("kib").trim().toIntOrNull() ?: 0
        if (t.endsWith("mb")) return (t.removeSuffix("mb").trim().toIntOrNull() ?: 0) * 1024
        return t.toIntOrNull() ?: 0
    }

    private fun mpvFont(key: String) = when (key.lowercase()) {
        "serif" -> "serif"
        "mono", "monospace" -> "monospace"
        else -> "sans-serif"
    }

    private fun mpvColor(name: String) = when (name.lowercase()) {
        "yellow" -> "#FFFF00"
        "cyan" -> "#00FFFF"
        "green" -> "#00FF00"
        "orange" -> "#FF9800"
        "pink" -> "#FF00FF"
        else -> "#FFFFFF"
    }

    private fun set(mpv: MPV, key: String, value: String) {
        runCatching { mpv.setOptionString(key, value) }
    }

    private fun setProp(mpv: MPV, key: String, value: String) {
        runCatching { mpv.setPropertyString(key, value) }
    }
}

internal data class MpvLiveCacheProfile(
    val cacheSecs: String,
    val readaheadSecs: String,
    val streamBufferSize: String,
    val audioBuffer: String
)
