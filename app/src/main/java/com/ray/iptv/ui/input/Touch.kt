package com.ray.iptv.ui.input

import android.app.UiModeManager
import android.content.Context
import android.content.pm.PackageManager
import android.content.res.Configuration
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.changedToUpIgnoreConsumed
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import kotlin.math.abs

val LocalTouchUi = staticCompositionLocalOf { false }

val LocalAdaptiveHaptics = compositionLocalOf { false }

fun Context.isTelevisionDevice(): Boolean {
    val ui = getSystemService(Context.UI_MODE_SERVICE) as UiModeManager
    if (ui.currentModeType == Configuration.UI_MODE_TYPE_TELEVISION) return true
    if (packageManager.hasSystemFeature(PackageManager.FEATURE_LEANBACK)) return true
    return packageManager.hasSystemFeature(PackageManager.FEATURE_TELEVISION)
}

fun Context.isTouchDevice(): Boolean {
    if (isTelevisionDevice()) return false
    val cfg = resources.configuration
    val finger = cfg.touchscreen == Configuration.TOUCHSCREEN_FINGER
    val tablet = cfg.smallestScreenWidthDp >= 600
    val hasTouch = packageManager.hasSystemFeature(PackageManager.FEATURE_TOUCHSCREEN)
    return finger || tablet || hasTouch
}

@Composable
fun rememberTouchUi(): Boolean {
    val ctx = LocalContext.current
    return remember(ctx) { ctx.isTouchDevice() }
}

fun Modifier.rayFocusRequester(requester: FocusRequester?): Modifier =
    if (requester != null) focusRequester(requester) else this

fun FocusRequester.tryFocus(): Boolean = try {
    requestFocus()
    true
} catch (_: Throwable) {
    false
}

fun Modifier.rayClickable(
    onClick: () -> Unit,
    onLongClick: (() -> Unit)? = null,
    interactionSource: MutableInteractionSource? = null
): Modifier = composed {
    val src = interactionSource ?: remember { MutableInteractionSource() }
    val hapticsOn = LocalAdaptiveHaptics.current
    val context = LocalContext.current
    val tap = {
        if (hapticsOn) AdaptiveHaptics.selection(context)
        onClick()
    }
    val longTap = onLongClick?.let { inner ->
        {
            if (hapticsOn) AdaptiveHaptics.longPress(context)
            inner()
        }
    }
    if (longTap != null) {
        @OptIn(ExperimentalFoundationApi::class)
        combinedClickable(
            interactionSource = src,
            indication = null,
            onClick = tap,
            onLongClick = longTap
        )
    } else {
        clickable(interactionSource = src, indication = null, onClick = tap)
    }
}

fun Modifier.rayHapticScroll(): Modifier = composed {
    val on = LocalAdaptiveHaptics.current
    val context = LocalContext.current
    val gate = remember { HapticScrollGate() }
    nestedScroll(
        remember(on, context) {
            object : NestedScrollConnection {
                override fun onPostScroll(
                    consumed: Offset,
                    available: Offset,
                    source: NestedScrollSource
                ): Offset {
                    if (!on) return Offset.Zero
                    if (source != NestedScrollSource.UserInput) return Offset.Zero
                    val travel = abs(consumed.x) + abs(consumed.y)
                    if (gate.onDelta(travel, System.currentTimeMillis())) {
                        AdaptiveHaptics.tick(context)
                    }
                    return Offset.Zero
                }

                override suspend fun onPostFling(consumed: Velocity, available: Velocity): Velocity {
                    gate.reset()
                    return Velocity.Zero
                }
            }
        }
    )
}

fun Modifier.playerTouch(
    onTap: () -> Unit,
    onSwipeUp: () -> Unit,
    onSwipeDown: () -> Unit,
    onSwipeLeft: () -> Unit,
    onSwipeRight: () -> Unit
): Modifier = pointerInput(onTap, onSwipeUp, onSwipeDown, onSwipeLeft, onSwipeRight) {
    val slop = 24.dp.toPx()
    val swipe = 64.dp.toPx()
    awaitEachGesture {
        val down = awaitFirstDown(requireUnconsumed = false)
        val start = down.position
        val edge = 32.dp.toPx()
        if (start.x <= edge || start.x >= size.width - edge) {
            while (true) {
                val event = awaitPointerEvent(PointerEventPass.Final)
                val change = event.changes.firstOrNull() ?: break
                if (change.changedToUpIgnoreConsumed() || !change.pressed) break
            }
            return@awaitEachGesture
        }
        down.consume()
        var end = start
        var dragged = false
        while (true) {
            val event = awaitPointerEvent()
            val change = event.changes.firstOrNull() ?: break
            end = change.position
            if ((end - start).getDistance() > slop) dragged = true
            if (change.changedToUpIgnoreConsumed()) break
            if (!change.pressed) break
        }
        val d = end - start
        if (!dragged) {
            onTap()
            return@awaitEachGesture
        }
        if (abs(d.y) > abs(d.x) && abs(d.y) > swipe) {
            if (d.y < 0) onSwipeUp() else onSwipeDown()
        } else if (abs(d.x) > swipe) {
            if (d.x < 0) onSwipeLeft() else onSwipeRight()
        }
    }
}
