package com.ray.iptv.ui.motion

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.LocalOverscrollConfiguration
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import com.ray.iptv.ui.theme.LocalGlass
import kotlin.math.abs
import kotlin.math.sign
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

/**
 * Mina `BouncingScrollPhysics`: mobilde kenarda lastik-bandı yay.
 * Android varsayılanı kenarda kilitlenir; bu modifier içeriği çeker ve geri oturtur.
 */
fun Modifier.rayBounceOverscroll(enabled: Boolean = true): Modifier = composed {
    if (!enabled) return@composed this
    val reduce = LocalGlass.current.reduceEffects
    val rangePx = with(LocalDensity.current) { 120.dp.toPx() }
    val scope = rememberCoroutineScope()
    val anim = remember { Animatable(0f) }
    var shown by remember { mutableFloatStateOf(0f) }
    val springSpec = remember(reduce) {
        if (reduce) spring<Float>(dampingRatio = 1f, stiffness = 420f)
        else spring(dampingRatio = 0.72f, stiffness = 320f)
    }

    val connection = remember(rangePx, springSpec) {
        object : NestedScrollConnection {
            var raw = 0f
            var settleJob: Job? = null

            fun rubber(value: Float): Float {
                if (value == 0f || rangePx <= 0f) return 0f
                val x = abs(value)
                return sign(value) * rangePx * (1f - 1f / (x / rangePx + 1f))
            }

            fun apply(nextRaw: Float) {
                raw = nextRaw
                shown = rubber(raw)
            }

            fun cancelSettle() {
                settleJob?.cancel()
                settleJob = null
            }

            fun settle(kick: Float = 0f) {
                cancelSettle()
                if (kick != 0f) apply(raw + kick)
                val start = shown
                if (abs(start) < 0.5f) {
                    raw = 0f
                    shown = 0f
                    return
                }
                settleJob = scope.launch {
                    anim.snapTo(start)
                    raw = 0f
                    anim.animateTo(0f, springSpec) { shown = value }
                    shown = 0f
                }
            }

            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                if (source != NestedScrollSource.UserInput) return Offset.Zero
                val y = available.y
                if (y == 0f || raw == 0f) return Offset.Zero
                val pullingBack = (raw > 0f && y < 0f) || (raw < 0f && y > 0f)
                if (!pullingBack) return Offset.Zero
                cancelSettle()
                val next = if (raw > 0f) (raw + y).coerceAtLeast(0f) else (raw + y).coerceAtMost(0f)
                val consumed = next - raw
                apply(next)
                return Offset(0f, consumed)
            }

            override fun onPostScroll(
                consumed: Offset,
                available: Offset,
                source: NestedScrollSource
            ): Offset {
                if (source != NestedScrollSource.UserInput) return Offset.Zero
                val y = available.y
                if (abs(y) < 0.5f) return Offset.Zero
                // Yatay şerit veya liste ortasındaki artan Y, tüm sayfayı lastik-bandı yapmasın.
                if (raw == 0f && (consumed.y != 0f || abs(consumed.x) > 0.5f)) return Offset.Zero
                cancelSettle()
                apply(raw + y)
                return Offset(0f, y)
            }

            override suspend fun onPreFling(available: Velocity): Velocity {
                if (raw == 0f && shown == 0f) return Velocity.Zero
                settle(kick = available.y / 22f)
                return available
            }

            override suspend fun onPostFling(consumed: Velocity, available: Velocity): Velocity {
                val edge = available.y
                if (abs(edge) > 80f || shown != 0f || raw != 0f) {
                    settle(kick = edge / 18f)
                    return Velocity(0f, edge)
                }
                return Velocity.Zero
            }
        }
    }

    clipToBounds().nestedScroll(connection).graphicsLayer { translationY = shown }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun RayMobileOverscroll(content: @Composable () -> Unit) {
    CompositionLocalProvider(LocalOverscrollConfiguration provides null) {
        content()
    }
}
