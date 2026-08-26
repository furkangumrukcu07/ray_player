package com.ray.iptv.ui.player

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.pm.ActivityInfo
import android.content.res.Configuration
import android.media.AudioManager
import android.provider.Settings
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeDown
import androidx.compose.material.icons.automirrored.filled.VolumeMute
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Brightness6
import androidx.compose.material.icons.filled.BrightnessMedium
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.FullscreenExit
import androidx.compose.material.icons.filled.WbSunny
import androidx.tv.material3.Icon
import androidx.tv.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlin.math.roundToInt

private val MobileCyan = Color(0xFF00E5FF)

enum class PlayerGestureType {
    BRIGHTNESS,
    VOLUME,
    ORIENTATION_TOGGLE
}

fun Context.findActivity(): Activity? {
    var ctx = this
    while (ctx is ContextWrapper) {
        if (ctx is Activity) return ctx
        ctx = ctx.baseContext
    }
    return null
}

fun Context.getScreenBrightness(): Float {
    val activity = findActivity() ?: return 0.5f
    val lp = activity.window.attributes
    return if (lp.screenBrightness < 0f) {
        try {
            Settings.System.getInt(contentResolver, Settings.System.SCREEN_BRIGHTNESS) / 255f
        } catch (e: Exception) {
            0.5f
        }
    } else {
        lp.screenBrightness
    }
}

fun Context.setScreenBrightness(valFloat: Float): Float {
    val activity = findActivity() ?: return valFloat
    val clamped = valFloat.coerceIn(0.01f, 1f)
    val lp = activity.window.attributes
    lp.screenBrightness = clamped
    activity.window.attributes = lp
    return clamped
}

fun Context.getStreamVolumeFraction(): Float {
    val audioManager = getSystemService(Context.AUDIO_SERVICE) as? AudioManager ?: return 0.5f
    val max = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC).toFloat().coerceAtLeast(1f)
    val cur = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC).toFloat()
    return (cur / max).coerceIn(0f, 1f)
}

fun Context.setStreamVolumeFraction(frac: Float): Float {
    val audioManager = getSystemService(Context.AUDIO_SERVICE) as? AudioManager ?: return frac
    val max = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
    if (max <= 0) return frac
    val targetVol = (frac.coerceIn(0f, 1f) * max).roundToInt()
    try {
        audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, targetVol, 0)
    } catch (_: Exception) {}
    return (targetVol.toFloat() / max.toFloat()).coerceIn(0f, 1f)
}

fun Context.toggleScreenOrientation(): Boolean {
    val activity = findActivity() ?: return false
    val currentOrientation = activity.resources.configuration.orientation
    val isLandscape = currentOrientation == Configuration.ORIENTATION_LANDSCAPE
    if (isLandscape) {
        activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
    } else {
        activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
    }
    return !isLandscape
}

class PlayerGestureState(val context: Context) {
    var isVisible by mutableStateOf(false)
    var gestureType by mutableStateOf(PlayerGestureType.BRIGHTNESS)
    var level by mutableFloatStateOf(0.5f)
    var isNowLandscape by mutableStateOf(false)

    private var startLevel = 0.5f
    private var totalDy = 0f
    private var isCenterGesture = false
    private var orientationToggledInCurrentDrag = false

    fun onDragStart(xFraction: Float) {
        totalDy = 0f
        orientationToggledInCurrentDrag = false
        if (xFraction < 0.30f) {
            isCenterGesture = false
            gestureType = PlayerGestureType.BRIGHTNESS
            startLevel = context.getScreenBrightness()
            level = startLevel
            isVisible = true
        } else if (xFraction > 0.70f) {
            isCenterGesture = false
            gestureType = PlayerGestureType.VOLUME
            startLevel = context.getStreamVolumeFraction()
            level = startLevel
            isVisible = true
        } else {
            isCenterGesture = true
            gestureType = PlayerGestureType.ORIENTATION_TOGGLE
        }
    }

    fun onDrag(dragAmountPx: Float, heightPx: Float) {
        if (isCenterGesture) {
            totalDy += dragAmountPx
            if (totalDy > 80f && !orientationToggledInCurrentDrag) {
                orientationToggledInCurrentDrag = true
                isNowLandscape = context.toggleScreenOrientation()
                isVisible = true
            }
        } else {
            val safeHeight = heightPx.coerceAtLeast(200f)
            val deltaFraction = -dragAmountPx / (safeHeight * 0.75f)
            val nextLevel = (level + deltaFraction).coerceIn(0f, 1f)
            if (gestureType == PlayerGestureType.BRIGHTNESS) {
                level = context.setScreenBrightness(nextLevel)
            } else if (gestureType == PlayerGestureType.VOLUME) {
                level = context.setStreamVolumeFraction(nextLevel)
            }
            isVisible = true
        }
    }

    fun onDragEnd() {
        totalDy = 0f
        orientationToggledInCurrentDrag = false
    }
}

@Composable
fun rememberPlayerGestureState(): PlayerGestureState {
    val context = LocalContext.current
    val state = remember(context) { PlayerGestureState(context) }
    LaunchedEffect(state.isVisible, state.level, state.gestureType, state.isNowLandscape) {
        if (state.isVisible) {
            delay(1300)
            state.isVisible = false
        }
    }
    return state
}

fun playerGestureDrag(state: PlayerGestureState): Modifier = Modifier.pointerInput(state) {
    detectVerticalDragGestures(
        onDragStart = { offset ->
            val xFraction = offset.x / size.width.toFloat()
            state.onDragStart(xFraction)
        },
        onDragEnd = { state.onDragEnd() },
        onDragCancel = { state.onDragEnd() },
        onVerticalDrag = { change, dragAmount ->
            change.consume()
            state.onDrag(dragAmount, size.height.toFloat())
        }
    )
}

@Composable
fun PlayerGlassLevelOverlay(
    state: PlayerGestureState,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = state.isVisible,
        enter = fadeIn(tween(140)) + scaleIn(tween(140), initialScale = 0.85f),
        exit = fadeOut(tween(220)) + scaleOut(tween(220), targetScale = 0.85f),
        modifier = modifier
    ) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(
                Modifier
                    .width(160.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(Color.Black.copy(alpha = 0.78f))
                    .border(1.dp, Color.White.copy(alpha = 0.18f), RoundedCornerShape(20.dp))
                    .padding(horizontal = 18.dp, vertical = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                when (state.gestureType) {
                    PlayerGestureType.BRIGHTNESS -> {
                        val level = state.level
                        val icon = when {
                            level > 0.7f -> Icons.Filled.WbSunny
                            level > 0.3f -> Icons.Filled.Brightness6
                            else -> Icons.Filled.BrightnessMedium
                        }
                        Icon(icon, contentDescription = null, tint = MobileCyan, modifier = Modifier.size(32.dp))
                        Text("Parlaklık %${(level * 100).toInt()}", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        Box(Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)).background(Color.White.copy(alpha = 0.20f))) {
                            Box(Modifier.fillMaxWidth(level.coerceIn(0f, 1f)).fillMaxHeight().clip(RoundedCornerShape(3.dp)).background(Brush.horizontalGradient(listOf(MobileCyan, Color(0xFF38BDF8)))))
                        }
                    }
                    PlayerGestureType.VOLUME -> {
                        val level = state.level
                        val icon = when {
                            level <= 0.01f -> Icons.AutoMirrored.Filled.VolumeMute
                            level < 0.5f -> Icons.AutoMirrored.Filled.VolumeDown
                            else -> Icons.AutoMirrored.Filled.VolumeUp
                        }
                        Icon(icon, contentDescription = null, tint = MobileCyan, modifier = Modifier.size(32.dp))
                        Text("Ses %${(level * 100).toInt()}", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        Box(Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)).background(Color.White.copy(alpha = 0.20f))) {
                            Box(Modifier.fillMaxWidth(level.coerceIn(0f, 1f)).fillMaxHeight().clip(RoundedCornerShape(3.dp)).background(Brush.horizontalGradient(listOf(MobileCyan, Color(0xFF38BDF8)))))
                        }
                    }
                    PlayerGestureType.ORIENTATION_TOGGLE -> {
                        val isLandscape = state.isNowLandscape
                        val icon = if (isLandscape) Icons.Filled.Fullscreen else Icons.Filled.FullscreenExit
                        val label = if (isLandscape) "Tam Ekran" else "Dikey Mod"
                        Icon(icon, contentDescription = null, tint = MobileCyan, modifier = Modifier.size(36.dp))
                        Text(label, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    }
                }
            }
        }
    }
}
