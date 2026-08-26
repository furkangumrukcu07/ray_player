package com.ray.iptv.ui.input

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.view.HapticFeedbackConstants
import java.util.concurrent.atomic.AtomicReference

/**
 * Mobil adaptif titreşim.
 *
 * Samsung One UI, [android.view.View.performHapticFeedback] (CLOCK_TICK) çağrısını
 * yok sayar. Motor [Vibrator]/[VibratorManager] ile doğrudan sürülür.
 * Xiaomi / HyperOS gibi OEM’lerde EFFECT_TICK sessiz kalabildiği için orada
 * kısa one-shot kullanılır.
 */
object AdaptiveHaptics {
    enum class Kind { TICK, SELECTION, LONG }

    private val main = Handler(Looper.getMainLooper())
    private val vibratorRef = AtomicReference<Vibrator?>(null)
    private val tapGate = HapticTapGate()

    fun selection(context: Context, force: Boolean = false) {
        if (!force && !tapGate.allow(System.currentTimeMillis())) return
        emit(context, Kind.SELECTION)
    }

    fun tick(context: Context) {
        emit(context, Kind.TICK)
    }

    fun longPress(context: Context) {
        emit(context, Kind.LONG)
    }

    private fun emit(context: Context, kind: Kind) {
        val app = context.applicationContext
        val vibrator = vibrator(app)
        val played = vibrator != null && vibrate(vibrator, kind)
        if (!played) fallbackView(context, kind)
    }

    private fun vibrator(app: Context): Vibrator? {
        vibratorRef.get()?.let { return it }
        val resolved = resolveVibrator(app) ?: return null
        vibratorRef.compareAndSet(null, resolved)
        return vibratorRef.get() ?: resolved
    }

    private fun resolveVibrator(app: Context): Vibrator? = try {
        val found = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val manager = app.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
            manager?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            app.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        }
        found?.takeIf { it.hasVibrator() }
    } catch (_: Throwable) {
        null
    }

    private fun vibrate(vibrator: Vibrator, kind: Kind): Boolean {
        val effect = effectFor(vibrator, kind) ?: return false
        onMain {
            val played = runCatching { play(vibrator, effect) }.isSuccess
            if (!played) {
                runCatching { play(vibrator, oneShot(vibrator, kind)) }
            }
        }
        return true
    }

    private fun effectFor(vibrator: Vibrator, kind: Kind): VibrationEffect? {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && canUsePredefined()) {
            runCatching {
                return VibrationEffect.createPredefined(predefined(kind))
            }
        }
        return runCatching { oneShot(vibrator, kind) }.getOrNull()
    }

    private fun canUsePredefined(): Boolean =
        !HapticOem.prefersOneShot(Build.MANUFACTURER, Build.BRAND)

    private fun play(vibrator: Vibrator, effect: VibrationEffect) {
        vibrator.vibrate(effect)
    }

    private fun oneShot(vibrator: Vibrator, kind: Kind): VibrationEffect {
        val (targetMs, amp) = when (kind) {
            Kind.TICK -> 4L to 28
            Kind.SELECTION -> 8L to 48
            Kind.LONG -> 18L to 85
        }
        val ms = if (vibrator.hasAmplitudeControl()) targetMs else minOf(targetMs, 5L)
        val amplitude = if (vibrator.hasAmplitudeControl()) amp else 45
        return VibrationEffect.createOneShot(ms, amplitude)
    }

    private fun predefined(kind: Kind): Int = when (kind) {
        Kind.LONG -> VibrationEffect.EFFECT_HEAVY_CLICK
        Kind.SELECTION -> VibrationEffect.EFFECT_CLICK
        Kind.TICK -> VibrationEffect.EFFECT_TICK
    }

    private fun fallbackView(context: Context, kind: Kind) {
        val view = context.findActivity()?.window?.decorView ?: return
        val constant = when (kind) {
            Kind.LONG -> HapticFeedbackConstants.LONG_PRESS
            Kind.TICK -> if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) HapticFeedbackConstants.TEXT_HANDLE_MOVE else HapticFeedbackConstants.KEYBOARD_TAP
            Kind.SELECTION -> HapticFeedbackConstants.KEYBOARD_TAP
        }
        view.isHapticFeedbackEnabled = true
        onMain {
            view.performHapticFeedback(constant)
        }
    }

    private fun onMain(block: () -> Unit) {
        if (Looper.myLooper() == Looper.getMainLooper()) {
            block()
        } else {
            main.post { runCatching { block() } }
        }
    }

    private fun Context.findActivity(): Activity? {
        var ctx: Context = this
        while (ctx is ContextWrapper) {
            if (ctx is Activity) return ctx
            ctx = ctx.baseContext
        }
        return null
    }

    /** Gizli View sabiti: sistem dokunma titreşimi kapalı olsa da dene. */
    private const val FLAG_IGNORE_GLOBAL = 0x0002
}
