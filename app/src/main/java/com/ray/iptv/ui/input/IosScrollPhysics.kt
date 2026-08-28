package com.ray.iptv.ui.input

import android.content.Context
import android.view.ViewConfiguration

/**
 * Override Android's global scroll friction to produce iOS-like fling physics.
 *
 * Android default friction: 0.015  → fast deceleration, short scroll distance
 * iOS-like friction:        0.008  → slower deceleration, longer coast / momentum
 *
 * This uses reflection to set the static field once per process. The effect
 * applies to every View and Compose scroll surface in the app.
 *
 * Must be called once from Application.onCreate() or Activity.onCreate().
 */
object IosScrollPhysics {

    private const val IOS_SCROLL_FRICTION = 0.008f  // half of Android default
    private var applied = false

    @JvmStatic
    fun apply(context: Context) {
        if (applied) return
        applied = true
        // Only needed on mobile/touch screens for finger momentum fling
        if (context.isTelevisionDevice()) return
        try {
            val vc = ViewConfiguration.get(context)
            val field = ViewConfiguration::class.java.getDeclaredField("mScrollFriction")
            field.isAccessible = true
            field.setFloat(vc, IOS_SCROLL_FRICTION)
        } catch (_: Throwable) {
            // Gracefully ignore on ROMs where this field is hidden
        }
    }
}
