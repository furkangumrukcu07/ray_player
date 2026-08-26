package com.ray.iptv.ui.input

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class HapticScrollGateTest {
    @Test
    fun ignoresTinyPerFrameDeltasUntilAccumulated() {
        val gate = HapticScrollGate(minDeltaPx = 58f, minIntervalMs = 118L)
        assertFalse(gate.onDelta(16f, 1_000L))
        assertFalse(gate.onDelta(16f, 1_008L))
        assertFalse(gate.onDelta(16f, 1_016L))
        assertTrue(gate.onDelta(16f, 1_024L))
    }

    @Test
    fun respectsMinimumIntervalAfterFire() {
        val gate = HapticScrollGate(minDeltaPx = 58f, minIntervalMs = 118L)
        assertTrue(gate.onDelta(80f, 1_000L))
        assertFalse(gate.onDelta(80f, 1_050L))
        assertTrue(gate.onDelta(80f, 1_130L))
    }

    @Test
    fun resetClearsAccumulatedDistance() {
        val gate = HapticScrollGate(minDeltaPx = 58f, minIntervalMs = 118L)
        assertFalse(gate.onDelta(40f, 1_000L))
        gate.reset()
        assertFalse(gate.onDelta(40f, 1_200L))
        assertTrue(gate.onDelta(40f, 1_200L))
    }

    @Test
    fun highRefreshTinyFramesEventuallyFire() {
        val gate = HapticScrollGate(minDeltaPx = 58f, minIntervalMs = 118L)
        var fired = 0
        var t = 1_000L
        repeat(40) {
            if (gate.onDelta(2f, t)) fired++
            t += 8L
        }
        assertTrue(fired >= 1)
    }

    @Test
    fun keepsAccumulatingWhileWaitingForInterval() {
        val gate = HapticScrollGate(minDeltaPx = 58f, minIntervalMs = 118L)
        assertTrue(gate.onDelta(60f, 1_000L))
        assertFalse(gate.onDelta(60f, 1_040L))
        assertTrue(gate.onDelta(1f, 1_120L))
    }
}

class HapticTapGateTest {
    @Test
    fun throttlesRapidTaps() {
        val gate = HapticTapGate(minIntervalMs = 95L)
        assertTrue(gate.allow(1_000L))
        assertFalse(gate.allow(1_040L))
        assertTrue(gate.allow(1_100L))
    }
}

class HapticOemTest {
    @Test
    fun chineseOemsPreferOneShot() {
        assertTrue(HapticOem.prefersOneShot("Xiaomi", "Redmi"))
        assertTrue(HapticOem.prefersOneShot("HUAWEI", "HONOR"))
        assertTrue(HapticOem.prefersOneShot("OPPO", "realme"))
        assertTrue(HapticOem.prefersOneShot("vivo", "iQOO"))
        assertTrue(HapticOem.prefersOneShot("OnePlus", "OnePlus"))
    }

    @Test
    fun samsungPixelSonyUsePredefined() {
        assertFalse(HapticOem.prefersOneShot("samsung", "samsung"))
        assertFalse(HapticOem.prefersOneShot("Google", "pixel"))
        assertFalse(HapticOem.prefersOneShot("Sony", "sony"))
        assertFalse(HapticOem.prefersOneShot("Nothing", "Nothing"))
    }
}
