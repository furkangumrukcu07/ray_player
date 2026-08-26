package com.ray.iptv.ui.input

import kotlin.math.abs

/** Kaydırma titreşimi: kare başına değil, biriken mesafe + minimum aralık. */
class HapticScrollGate(
    private val minDeltaPx: Float = 58f,
    private val minIntervalMs: Long = 118L
) {
    private var accum = 0f
    private var lastAt = 0L

    fun onDelta(deltaPx: Float, nowMs: Long): Boolean {
        accum += abs(deltaPx)
        if (accum < minDeltaPx) return false
        if (nowMs - lastAt < minIntervalMs) return false
        accum = 0f
        lastAt = nowMs
        return true
    }

    fun reset() {
        accum = 0f
        lastAt = 0L
    }
}

/** Dokunma titreşimi: peş peşe tıklamalarda motoru boğmamak için. */
class HapticTapGate(
    private val minIntervalMs: Long = 95L
) {
    private var lastAt = 0L

    fun allow(nowMs: Long): Boolean {
        if (nowMs - lastAt < minIntervalMs) return false
        lastAt = nowMs
        return true
    }
}

/**
 * EFFECT_TICK / CLICK bazı Çin OEM’lerinde (HyperOS, ColorOS, Funtouch)
 * “destekleniyor” görünüp motoru hiç sürmez. Bu cihazlarda one-shot kullanılır.
 */
object HapticOem {
    fun prefersOneShot(manufacturer: String, brand: String): Boolean {
        val maker = manufacturer.lowercase()
        val make = brand.lowercase()
        val names = arrayOf(
            "xiaomi", "redmi", "poco", "blackshark",
            "huawei", "honor",
            "oppo", "realme", "oneplus",
            "vivo", "iqoo",
            "meizu", "tecno", "infinix"
        )
        return names.any { maker.contains(it) || make.contains(it) }
    }
}
