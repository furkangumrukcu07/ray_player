package com.ray.iptv.ui.input

import android.app.Activity
import android.os.Build
import android.view.View
import android.view.ViewTreeObserver
import android.view.WindowManager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.ray.iptv.MainActivity

/**
 * Immersive & Screen Keep-Awake controller.
 * Keeps screen on when [active] is true (during portrait or landscape video playback).
 * Hides system status/navigation bars when [hideSystemBars] is true (landscape full screen).
 */
@Composable
fun ImmersivePlayback(active: Boolean, hideSystemBars: Boolean = active) {
    val context = LocalContext.current
    val view = LocalView.current
    val lifecycle = LocalLifecycleOwner.current
    DisposableEffect(active, hideSystemBars, view, lifecycle) {
        val activity = context as? Activity
        fun apply() {
            (activity as? MainActivity)?.playbackImmersive = active
            activity?.setPlaybackImmersive(active, hideSystemBars)
        }
        apply()
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) apply()
        }
        lifecycle.lifecycle.addObserver(observer)
        val focusListener = ViewTreeObserver.OnWindowFocusChangeListener { hasFocus ->
            if (hasFocus) apply()
        }
        view.viewTreeObserver.addOnWindowFocusChangeListener(focusListener)
        onDispose {
            lifecycle.lifecycle.removeObserver(observer)
            if (view.viewTreeObserver.isAlive) {
                view.viewTreeObserver.removeOnWindowFocusChangeListener(focusListener)
            }
            (activity as? MainActivity)?.playbackImmersive = false
            activity?.setPlaybackImmersive(false, false)
        }
    }
}

fun Activity.setPlaybackImmersive(on: Boolean, hideBars: Boolean = on) {
    val tv = isTelevisionDevice()
    val hide = hideBars || tv
    WindowCompat.setDecorFitsSystemWindows(window, false)
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
        window.attributes = window.attributes.apply {
            layoutInDisplayCutoutMode =
                WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
        }
    }
    val bars = WindowInsetsControllerCompat(window, window.decorView)
    bars.isAppearanceLightStatusBars = false
    bars.isAppearanceLightNavigationBars = false

    if (on) {
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    } else {
        window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    }

    if (hide) {
        window.addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN)
        bars.systemBarsBehavior =
            WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        bars.hide(WindowInsetsCompat.Type.statusBars())
        bars.hide(WindowInsetsCompat.Type.navigationBars())
        bars.hide(WindowInsetsCompat.Type.systemBars())
        @Suppress("DEPRECATION")
        window.decorView.systemUiVisibility = (
            View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                or View.SYSTEM_UI_FLAG_FULLSCREEN
                or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
            )
    } else {
        window.clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN)
        @Suppress("DEPRECATION")
        window.decorView.systemUiVisibility = (
            View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
            )
        bars.show(WindowInsetsCompat.Type.systemBars())
        bars.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_DEFAULT
    }
}
