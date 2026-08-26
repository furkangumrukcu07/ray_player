package com.ray.iptv.player

import android.app.ActivityManager
import android.content.Context
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.os.Build
import android.util.Log
import java.util.Locale

/** Mina `DevicePlaybackSegment`. */
enum class DevicePlaybackSegment { LOW, MID, HIGH }

/** Mina `ChallengedTvSubclass`. */
enum class ChallengedTvSubclass {
    NONE,
    BUDGET_SOC,
    AMAZON_FIRE,
    LOW_END_SMART_TV,
    CAPABLE_TWO_GIB,
    GENERIC_WEAK
}

/**
 * Mina `AndroidPlaybackSocHints` + native `mediaKitSoCProfile`.
 * Exo tamponu, 720p tavanı ve zayıf-kutu TS tercihi buradan seçilir.
 */
class AndroidPlaybackSocHints private constructor(context: Context) {

    val totalRamBytes: Long
    val cores: Int
    val androidTv: Boolean
    val amlogicLike: Boolean
    val allwinnerLike: Boolean
    val rockchipLike: Boolean
    val genericBudgetBoxLike: Boolean
    val budgetTvBoxSoc: Boolean
    val amazonFireLike: Boolean
    val lowEndSmartTvLike: Boolean
    val capableTwoGiBTvBox: Boolean
    val mediatekLike: Boolean
    val playbackChallengedTv: Boolean
    val weakMpvDevice: Boolean
    val playbackSegment: DevicePlaybackSegment
    val model: String

    val oneGiBRamClass: Boolean
    val twoGiBRamClass: Boolean
    val budgetTwoGiBRamClass: Boolean
    val mediatekMobileLike: Boolean
    val challengedTvSubclass: ChallengedTvSubclass
    val needsTextureSafeExoOverlay: Boolean
    val capableChallengedTvForVod: Boolean

    init {
        val hw = Build.HARDWARE.lowercase(Locale.US)
        val board = Build.BOARD.lowercase(Locale.US)
        val man = Build.MANUFACTURER.lowercase(Locale.US)
        val brand = Build.BRAND.lowercase(Locale.US)
        val modelRaw = Build.MODEL
        val modelLc = modelRaw.lowercase(Locale.US)
        val device = Build.DEVICE.lowercase(Locale.US)
        val blob = "$hw $board $man $brand $modelLc $device"
        model = modelRaw

        val am = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val mi = ActivityManager.MemoryInfo()
        am.getMemoryInfo(mi)
        totalRamBytes = mi.totalMem
        cores = Runtime.getRuntime().availableProcessors()
        androidTv = isAndroidTvOrTvBox(context)

        val digipollLike = listOf("digipoll").any { man.contains(it) || brand.contains(it) || modelLc.contains(it) }
        amlogicLike = blob.contains("amlogic") || blob.contains("meson")
        val tclLike = listOf("tcl").any { man.contains(it) || brand.contains(it) || modelLc.contains(it) }
        val philipsLike = listOf("philips", "tpv", "pfl", "pus").any {
            man.contains(it) || brand.contains(it) || modelLc.contains(it)
        }
        val toshibaLike = listOf("toshiba", "regza").any {
            man.contains(it) || brand.contains(it) || modelLc.contains(it)
        }
        val hisenseLike = listOf("hisense", "vidaa").any {
            man.contains(it) || brand.contains(it) || modelLc.contains(it)
        }
        val vestelLike = listOf("vestel", "regal", "finlux").any {
            man.contains(it) || brand.contains(it) || modelLc.contains(it)
        }
        mediatekLike = listOf("mediatek", "mtk").any { blob.contains(it) } || hw.startsWith("mt")
        val realtekLike = blob.contains("realtek") || hw.startsWith("rtd")
        allwinnerLike = blob.contains("allwinner") ||
            hw.startsWith("sun") || board.startsWith("sun") || board.startsWith("exdroid")
        rockchipLike = blob.contains("rockchip") || hw.startsWith("rk3") || board.startsWith("rk3")
        genericBudgetBoxLike = listOf(
            "x96", "x98", "t95", "t96", "t98", "h96", "h98", "h616", "h618",
            "tanix", "mxq", "tx3", "tx6", "transpeed", "bqeel", "vontar",
            "atlas", "next star", "nextstar"
        ).any { modelLc.contains(it) || brand.contains(it) || device.contains(it) }
        budgetTvBoxSoc = allwinnerLike || rockchipLike || genericBudgetBoxLike

        capableTwoGiBTvBox = totalRamBytes >= TWO_GIB && totalRamBytes < THREE_GIB && cores >= 4 &&
            !budgetTvBoxSoc &&
            listOf(
                "google", "chromecast", "sabrina", "oneday", "xiaomi", "mi box",
                "mitv", "mi tv stick", "onn", "mecool", "km9", "km2", "nvidia",
                "shield", "tivo", "formuler"
            ).any { blob.contains(it) }

        weakMpvDevice = (totalRamBytes < TWO_HALF_GIB || cores <= 4) && !capableTwoGiBTvBox
        amazonFireLike = androidTv &&
            listOf("amazon", "fire tv", "aft", "sheldon", "mantis").any {
                man.contains(it) || brand.contains(it) || modelLc.contains(it) || device.contains(it)
            }
        lowEndSmartTvLike = androidTv &&
            (tclLike || philipsLike || toshibaLike || hisenseLike || vestelLike ||
                realtekLike || (mediatekLike && totalRamBytes < FOUR_GIB))
        playbackChallengedTv = androidTv &&
            (amlogicLike || tclLike || philipsLike || toshibaLike || hisenseLike ||
                vestelLike || mediatekLike || realtekLike ||
                allwinnerLike || rockchipLike || genericBudgetBoxLike ||
                digipollLike || amazonFireLike || lowEndSmartTvLike || weakMpvDevice)

        oneGiBRamClass = totalRamBytes < ONE_GIB_CLASS_MAX
        twoGiBRamClass = totalRamBytes >= TWO_GIB && totalRamBytes < THREE_GIB
        budgetTwoGiBRamClass = twoGiBRamClass && budgetTvBoxSoc
        mediatekMobileLike = mediatekLike && !androidTv

        playbackSegment = deriveSegment(totalRamBytes, cores, weakMpvDevice, playbackChallengedTv)

        capableChallengedTvForVod = when {
            capableTwoGiBTvBox -> true
            !playbackChallengedTv -> false
            budgetTvBoxSoc -> false
            else -> totalRamBytes >= TWO_GIB && cores >= 4
        }

        challengedTvSubclass = when {
            budgetTvBoxSoc || oneGiBRamClass -> ChallengedTvSubclass.BUDGET_SOC
            amazonFireLike -> ChallengedTvSubclass.AMAZON_FIRE
            lowEndSmartTvLike -> ChallengedTvSubclass.LOW_END_SMART_TV
            capableTwoGiBTvBox || capableChallengedTvForVod -> ChallengedTvSubclass.CAPABLE_TWO_GIB
            playbackChallengedTv -> ChallengedTvSubclass.GENERIC_WEAK
            else -> ChallengedTvSubclass.NONE
        }
        needsTextureSafeExoOverlay = when (challengedTvSubclass) {
            ChallengedTvSubclass.BUDGET_SOC,
            ChallengedTvSubclass.AMAZON_FIRE,
            ChallengedTvSubclass.LOW_END_SMART_TV,
            ChallengedTvSubclass.GENERIC_WEAK -> true
            ChallengedTvSubclass.CAPABLE_TWO_GIB,
            ChallengedTvSubclass.NONE -> false
        }

        Log.i(
            TAG,
            "segment=${playbackSegment.name.lowercase()} " +
                "ram=${"%.1f".format(Locale.US, totalRamBytes / (1024.0 * 1024.0 * 1024.0))}GiB " +
                "cores=$cores tv=$androidTv subclass=${challengedTvSubclass.name.lowercase()} " +
                "oneGiB=$oneGiBRamClass budgetSoc=$budgetTvBoxSoc fire=$amazonFireLike " +
                "lowEndTv=$lowEndSmartTvLike mtkMobile=$mediatekMobileLike model=$model"
        )
    }

    fun isTotalRamBelowBytes(maxBytes: Long): Boolean = totalRamBytes < maxBytes

    fun exoLivePlaybackSegment(): DevicePlaybackSegment = when {
        oneGiBRamClass -> DevicePlaybackSegment.LOW
        twoGiBRamClass && !budgetTvBoxSoc -> DevicePlaybackSegment.MID
        else -> playbackSegment
    }

    fun exoVodPlaybackSegment(): DevicePlaybackSegment = when {
        oneGiBRamClass -> DevicePlaybackSegment.LOW
        playbackSegment == DevicePlaybackSegment.LOW && capableChallengedTvForVod -> DevicePlaybackSegment.MID
        twoGiBRamClass && !budgetTvBoxSoc -> DevicePlaybackSegment.MID
        else -> playbackSegment
    }

    /** Mina `adaptiveMaxVideoHeightHint` — null = tavan yok. */
    fun adaptiveMaxVideoHeightHint(): Int? = when (challengedTvSubclass) {
        ChallengedTvSubclass.BUDGET_SOC -> 720
        ChallengedTvSubclass.AMAZON_FIRE,
        ChallengedTvSubclass.LOW_END_SMART_TV,
        ChallengedTvSubclass.GENERIC_WEAK -> 1080
        ChallengedTvSubclass.CAPABLE_TWO_GIB,
        ChallengedTvSubclass.NONE -> if (oneGiBRamClass) 720 else null
    }

    /** ~1 GiB veya ucuz kutu SoC: Exo 720p tavan (Mina `setTrackParameters(1280, 720, 0)`). */
    fun needs720pCap(): Boolean =
        oneGiBRamClass || challengedTvSubclass == ChallengedTvSubclass.BUDGET_SOC

    fun shouldForceTsLiveFormat(): Boolean =
        weakMpvDevice || playbackChallengedTv || androidTv || playbackSegment == DevicePlaybackSegment.LOW

    val preferDirectMediacodecHwdec: Boolean
        get() = amlogicLike || when (challengedTvSubclass) {
            ChallengedTvSubclass.BUDGET_SOC,
            ChallengedTvSubclass.AMAZON_FIRE,
            ChallengedTvSubclass.LOW_END_SMART_TV,
            ChallengedTvSubclass.GENERIC_WEAK -> true
            ChallengedTvSubclass.CAPABLE_TWO_GIB,
            ChallengedTvSubclass.NONE -> false
        }

    val isSamsungSmT530: Boolean
        get() {
            val m = model.lowercase(Locale.US)
            return m.contains("sm-t530") || m.contains("smt530")
        }

    fun vodDemuxerForwardMiB(): Int {
        val ram = totalRamBytes
        val giB = 1024L * 1024L * 1024L
        if (ram >= 4 * giB) return 96
        if (ram >= 3 * giB) return 64
        if (ram >= 2 * giB) return if (budgetTvBoxSoc) 16 else 24
        if (ram < ONE_GIB_CLASS_MAX) return 12
        return 16
    }

    fun liveMpvBufferStep(step: Int): MediaKitLiveMpvBufferStep {
        val s = step.coerceIn(0, 2)
        val ram = totalRamBytes
        val giB = 1024L * 1024L * 1024L
        if (ram < ONE_GIB_CLASS_MAX) {
            return when (s) {
                0 -> MediaKitLiveMpvBufferStep(2048, 2 * 1024 * 1024, 1024 * 1024)
                1 -> MediaKitLiveMpvBufferStep(8192, 8 * 1024 * 1024, 4 * 1024 * 1024)
                else -> MediaKitLiveMpvBufferStep(16384, 12 * 1024 * 1024, 6 * 1024 * 1024)
            }
        }
        if (ram >= 2 * giB && ram < 3 * giB) {
            if (budgetTvBoxSoc) {
                return when (s) {
                    0 -> MediaKitLiveMpvBufferStep(2048, 2 * 1024 * 1024, 1024 * 1024)
                    1 -> MediaKitLiveMpvBufferStep(8192, 10 * 1024 * 1024, 4 * 1024 * 1024)
                    else -> MediaKitLiveMpvBufferStep(16384, 14 * 1024 * 1024, 8 * 1024 * 1024)
                }
            }
            return when (s) {
                0 -> MediaKitLiveMpvBufferStep(4096, 4 * 1024 * 1024, 2 * 1024 * 1024)
                1 -> MediaKitLiveMpvBufferStep(12288, 12 * 1024 * 1024, 6 * 1024 * 1024)
                else -> MediaKitLiveMpvBufferStep(20480, 16 * 1024 * 1024, 8 * 1024 * 1024)
            }
        }
        return when (s) {
            0 -> MediaKitLiveMpvBufferStep(4096, 4 * 1024 * 1024, 2 * 1024 * 1024)
            1 -> MediaKitLiveMpvBufferStep(16384, 16 * 1024 * 1024, 8 * 1024 * 1024)
            else -> MediaKitLiveMpvBufferStep(65536, 64 * 1024 * 1024, 32 * 1024 * 1024)
        }
    }

    companion object {
        private const val TAG = "RaySocHints"
        const val ONE_GIB_CLASS_MAX = 1536L * 1024L * 1024L
        const val TWO_GIB = 2L * 1024L * 1024L * 1024L
        const val TWO_HALF_GIB = (2.5 * 1024 * 1024 * 1024).toLong()
        const val THREE_GIB = 3L * 1024L * 1024L * 1024L
        const val FOUR_GIB = 4L * 1024L * 1024L * 1024L
        const val SIX_GIB = 6L * 1024L * 1024L * 1024L
        private const val LOW_RAM_FOR_SEGMENT = THREE_GIB

        @Volatile
        private var instance: AndroidPlaybackSocHints? = null

        fun get(context: Context): AndroidPlaybackSocHints {
            instance?.let { return it }
            return synchronized(this) {
                instance ?: AndroidPlaybackSocHints(context.applicationContext).also { instance = it }
            }
        }

        fun isAndroidTvOrTvBox(context: Context): Boolean {
            val pm = context.packageManager
            if (pm.hasSystemFeature(PackageManager.FEATURE_LEANBACK)) return true
            if (pm.hasSystemFeature(PackageManager.FEATURE_TELEVISION)) return true
            val ui = context.resources.configuration.uiMode and Configuration.UI_MODE_TYPE_MASK
            return ui == Configuration.UI_MODE_TYPE_TELEVISION
        }

        private fun deriveSegment(
            ramBytes: Long,
            cores: Int,
            weakMpv: Boolean,
            playbackChallengedTv: Boolean
        ): DevicePlaybackSegment {
            if (weakMpv || playbackChallengedTv || cores <= 4 || ramBytes < LOW_RAM_FOR_SEGMENT) {
                return DevicePlaybackSegment.LOW
            }
            if (ramBytes >= SIX_GIB && cores >= 8) return DevicePlaybackSegment.HIGH
            return DevicePlaybackSegment.MID
        }
    }
}

data class MediaKitLiveMpvBufferStep(
    val cacheSizeKiB: Int,
    val demuxerMaxBytes: Int,
    val demuxerMaxBackBytes: Int
)
