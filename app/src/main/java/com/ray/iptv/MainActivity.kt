package com.ray.iptv

import android.app.PictureInPictureParams
import android.content.pm.ActivityInfo
import android.content.res.Configuration
import android.os.Build
import android.os.Bundle
import android.util.Rational
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.ray.iptv.ui.RayRoot
import com.ray.iptv.ui.input.isTelevisionDevice
import com.ray.iptv.ui.input.setPlaybackImmersive
import com.ray.iptv.ui.input.IosScrollPhysics
import dagger.hilt.android.AndroidEntryPoint

val LocalPipMode = staticCompositionLocalOf { false }

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    val isInPipModeState = mutableStateOf(false)
    var pipEligible: (() -> Boolean)? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        IosScrollPhysics.apply(this)
        enableEdgeToEdge()
        WindowCompat.setDecorFitsSystemWindows(window, false)
        applyDeviceChrome()
        setContent {
            val inPip by isInPipModeState
            CompositionLocalProvider(LocalPipMode provides inPip) {
                RayRoot()
            }
        }
    }

    override fun onPictureInPictureModeChanged(
        isInPictureInPictureMode: Boolean,
        newConfig: Configuration
    ) {
        super.onPictureInPictureModeChanged(isInPictureInPictureMode, newConfig)
        isInPipModeState.value = isInPictureInPictureMode
        if (!isInPictureInPictureMode && (playbackImmersive || isTelevisionDevice())) {
            setPlaybackImmersive(playbackImmersive || isTelevisionDevice())
        }
    }

    override fun onUserLeaveHint() {
        super.onUserLeaveHint()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            if (!isTelevisionDevice() && pipEligible?.invoke() == true) {
                enterPipMode()
            }
        }
    }

    fun enterPipMode(aspectRatio: Float = 16f / 9f): Boolean {
        if (isTelevisionDevice()) return false
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            return runCatching {
                val rational = try {
                    val num = (aspectRatio * 100).toInt().coerceIn(41, 239)
                    Rational(num, 100)
                } catch (_: Exception) {
                    Rational(16, 9)
                }
                val params = PictureInPictureParams.Builder()
                    .setAspectRatio(rational)
                    .build()
                enterPictureInPictureMode(params)
            }.getOrDefault(false)
        }
        return false
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus && playbackImmersive) setPlaybackImmersive(true)
    }

    override fun onResume() {
        super.onResume()
        if (playbackImmersive || isTelevisionDevice()) {
            setPlaybackImmersive(playbackImmersive || isTelevisionDevice())
        } else {
            requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_FULL_USER
        }
    }


    var playbackImmersive: Boolean = false

    private fun applyDeviceChrome() {
        val tv = isTelevisionDevice()
        requestedOrientation = if (tv) {
            ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
        } else {
            ActivityInfo.SCREEN_ORIENTATION_FULL_USER
        }
        val bars = WindowInsetsControllerCompat(window, window.decorView)
        if (tv) {
            window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            bars.hide(WindowInsetsCompat.Type.systemBars())
            bars.systemBarsBehavior =
                WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        } else {
            window.clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN)
            bars.show(WindowInsetsCompat.Type.systemBars())
            bars.systemBarsBehavior =
                WindowInsetsControllerCompat.BEHAVIOR_DEFAULT
        }
    }
}
