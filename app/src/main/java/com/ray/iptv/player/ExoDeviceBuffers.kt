package com.ray.iptv.player

/**
 * Mina `IptvBetterPlayerConfig` Exo [DefaultLoadControl] profilleri —
 * RAM sınıfı, SoC alt sınıfı, ham TS / uzun HLS / UHD ve kullanıcı tamponu.
 */
data class ExoBufferProfile(
    val minMs: Int,
    val maxMs: Int,
    val playbackMs: Int,
    val rebufferMs: Int,
    val targetBytes: Int,
    val prioritizeTime: Boolean
)

object ExoDeviceBuffers {
    const val BYTES_TS_LIVE = 16 * 1024 * 1024
    const val BYTES_LONG_HLS = 32 * 1024 * 1024
    const val BYTES_UHD = 32 * 1024 * 1024
    const val BYTES_VOD = 32 * 1024 * 1024
    const val BYTES_LOW_RAM_LIVE = 8 * 1024 * 1024
    const val BYTES_TWO_GIB_LIVE = 10 * 1024 * 1024
    const val BYTES_LOW_RAM_VOD = 12 * 1024 * 1024
    const val BYTES_TWO_GIB_VOD = 14 * 1024 * 1024

    private val liveTvMid = ExoBufferProfile(15_000, 50_000, 3_000, 5_000, BYTES_VOD, true)
    private val liveLow = ExoBufferProfile(15_000, 50_000, 3_000, 5_000, BYTES_VOD, true)
    private val liveHigh = ExoBufferProfile(20_000, 60_000, 2_800, 5_000, BYTES_VOD, true)
    private val liveLongHls = ExoBufferProfile(20_000, 60_000, 3_000, 6_000, BYTES_LONG_HLS, true)
    private val liveUhdHls = ExoBufferProfile(20_000, 60_000, 3_500, 6_000, BYTES_UHD, true)
    private val liveUhdWeak = ExoBufferProfile(15_000, 35_000, 3_000, 6_000, 16 * 1024 * 1024, true)
    private val liveOneGiB = ExoBufferProfile(15_000, 35_000, 2_800, 5_000, BYTES_LOW_RAM_LIVE, true)
    private val liveBudgetTwoGiB = ExoBufferProfile(15_000, 40_000, 2_800, 5_000, BYTES_TWO_GIB_LIVE, true)
    private val liveLowEndSmartTv = ExoBufferProfile(15_000, 40_000, 3_000, 5_500, BYTES_TWO_GIB_LIVE, true)
    private val liveCapableTwoGiB = ExoBufferProfile(15_000, 45_000, 2_800, 5_000, BYTES_TWO_GIB_LIVE, true)
    private val liveMediatekMobile = ExoBufferProfile(15_000, 40_000, 2_500, 4_500, BYTES_VOD, true)

    private val vodMid = ExoBufferProfile(15_000, 60_000, 3_500, 6_000, BYTES_VOD, false)
    private val vodLow = ExoBufferProfile(12_000, 40_000, 3_500, 6_000, BYTES_VOD, true)
    private val vodHigh = ExoBufferProfile(20_000, 80_000, 3_000, 5_000, BYTES_VOD, false)
    private val vodOneGiB = ExoBufferProfile(12_000, 32_000, 3_200, 5_500, BYTES_LOW_RAM_VOD, true)
    private val vodBudgetTwoGiB = ExoBufferProfile(12_000, 36_000, 3_500, 5_800, BYTES_TWO_GIB_VOD, true)
    private val vodCapableTwoGiB = ExoBufferProfile(15_000, 56_000, 3_500, 6_000, BYTES_TWO_GIB_VOD, false)
    private val vodMediatekMobile = ExoBufferProfile(14_000, 50_000, 3_000, 6_000, BYTES_VOD, true)

    fun resolve(
        hints: AndroidPlaybackSocHints,
        live: Boolean,
        userSec: Int,
        rawTs: Boolean,
        uhdHls: Boolean,
        longSegmentHls: Boolean = false
    ): ExoBufferProfile {
        var base = if (live) {
            liveBufferingForDevice(hints, longSegmentHls, rawTs, uhdHls)
        } else {
            vodBufferingForDevice(hints)
        }
        if (hints.needsTextureSafeExoOverlay) {
            base = textureSafeOverlay(base)
        }
        if (live && userSec > 0) {
            base = userLiveSecondsOverlay(base, userSec)
        }
        return sanitize(base)
    }

    private fun liveBufferingForDevice(
        hints: AndroidPlaybackSocHints,
        longSegmentHls: Boolean,
        rawTs: Boolean,
        uhdHls: Boolean
    ): ExoBufferProfile {
        if (uhdHls) {
            val weakUhd = hints.budgetTwoGiBRamClass ||
                hints.oneGiBRamClass ||
                hints.playbackSegment == DevicePlaybackSegment.LOW ||
                hints.isTotalRamBelowBytes(2560L * 1024L * 1024L)
            return if (weakUhd) liveUhdWeak else liveUhdHls
        }
        if (rawTs) {
            val weakRam = hints.oneGiBRamClass ||
                hints.budgetTwoGiBRamClass ||
                hints.challengedTvSubclass == ChallengedTvSubclass.BUDGET_SOC ||
                hints.challengedTvSubclass == ChallengedTvSubclass.AMAZON_FIRE
            val midWeak = hints.challengedTvSubclass == ChallengedTvSubclass.LOW_END_SMART_TV ||
                hints.challengedTvSubclass == ChallengedTvSubclass.GENERIC_WEAK ||
                hints.twoGiBRamClass
            val bytes = when {
                weakRam -> BYTES_LOW_RAM_LIVE
                midWeak -> 16 * 1024 * 1024
                else -> BYTES_VOD
            }
            return ExoBufferProfile(15_000, 50_000, 2_000, 4_000, bytes, true)
        }
        if (longSegmentHls) return liveLongHls
        val base = when {
            hints.oneGiBRamClass -> liveOneGiB
            hints.budgetTwoGiBRamClass -> liveBudgetTwoGiB
            hints.mediatekMobileLike -> liveMediatekMobile
            hints.lowEndSmartTvLike -> liveLowEndSmartTv
            hints.twoGiBRamClass && (hints.capableTwoGiBTvBox || !hints.budgetTvBoxSoc) -> liveCapableTwoGiB
            else -> liveBufferingForSegment(hints.exoLivePlaybackSegment())
        }
        return handheldLiveJitterOverlay(hints, base)
    }

    private fun vodBufferingForDevice(hints: AndroidPlaybackSocHints): ExoBufferProfile = when {
        hints.oneGiBRamClass -> vodOneGiB
        hints.budgetTwoGiBRamClass -> vodBudgetTwoGiB
        hints.mediatekMobileLike -> vodMediatekMobile
        hints.twoGiBRamClass && (hints.capableTwoGiBTvBox || !hints.budgetTvBoxSoc) -> vodCapableTwoGiB
        else -> vodBufferingForSegment(hints.exoVodPlaybackSegment())
    }

    private fun liveBufferingForSegment(seg: DevicePlaybackSegment): ExoBufferProfile = when (seg) {
        DevicePlaybackSegment.LOW -> liveLow
        DevicePlaybackSegment.HIGH -> liveHigh
        DevicePlaybackSegment.MID -> liveTvMid
    }

    private fun vodBufferingForSegment(seg: DevicePlaybackSegment): ExoBufferProfile = when (seg) {
        DevicePlaybackSegment.LOW -> vodLow
        DevicePlaybackSegment.HIGH -> vodHigh
        DevicePlaybackSegment.MID -> vodMid
    }

    private fun handheldLiveJitterOverlay(
        hints: AndroidPlaybackSocHints,
        base: ExoBufferProfile
    ): ExoBufferProfile {
        if (hints.androidTv) return base
        val afterMs = maxOf(base.rebufferMs, 4_000)
        val minMs = maxOf(base.minMs, afterMs + 1_500)
        val maxMs = maxOf(base.maxMs, minMs * 2)
        val startMs = base.playbackMs.coerceIn(1_500, 3_000)
        return base.copy(minMs = minMs, maxMs = maxMs, playbackMs = startMs, rebufferMs = afterMs)
    }

    private fun textureSafeOverlay(base: ExoBufferProfile): ExoBufferProfile {
        val startMs = if (base.playbackMs < 2_500) 3_000 else base.playbackMs.coerceIn(2_500, 3_500)
        val afterMs = maxOf(base.rebufferMs, 5_000)
        return base.copy(
            minMs = maxOf(base.minMs, afterMs + 1_000),
            playbackMs = startMs,
            rebufferMs = afterMs,
            prioritizeTime = true
        )
    }

    /** Mina `iptvBetterPlayerDataSource` kullanıcı saniye katmanı. */
    private fun userLiveSecondsOverlay(base: ExoBufferProfile, userSec: Int): ExoBufferProfile {
        val userMs = (userSec * 1_000).coerceIn(800, 120_000)
        val playbackMs = maxOf(base.playbackMs, userMs)
        var afterMs = maxOf(base.rebufferMs, userMs, playbackMs)
        afterMs = if (base.rebufferMs > 8_000) {
            maxOf(afterMs, base.rebufferMs)
        } else {
            afterMs.coerceAtMost(maxOf(15_000, userMs)).coerceAtLeast(userMs)
        }
        val minMs = maxOf(base.minMs, playbackMs, afterMs)
        val maxMs = maxOf(base.maxMs, minMs * 2, minMs)
        return base.copy(minMs = minMs, maxMs = maxMs, playbackMs = playbackMs, rebufferMs = afterMs)
    }

    private fun sanitize(p: ExoBufferProfile): ExoBufferProfile {
        val playback = p.playbackMs.coerceAtLeast(0)
        val rebuffer = p.rebufferMs.coerceAtLeast(0)
        val minMs = maxOf(p.minMs, playback, rebuffer)
        val maxMs = maxOf(p.maxMs, minMs)
        return p.copy(minMs = minMs, maxMs = maxMs, playbackMs = playback, rebufferMs = rebuffer)
    }

    fun urlLooksUhd(url: String): Boolean {
        val u = url.lowercase()
        return u.contains("2160") || u.contains("uhd") || u.contains("4k") || u.contains("3840")
    }
}
