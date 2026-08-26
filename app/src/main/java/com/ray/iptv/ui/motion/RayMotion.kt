package com.ray.iptv.ui.motion

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import com.ray.iptv.data.repo.PageTransitionEffect
import com.ray.iptv.ui.Dest
import com.ray.iptv.ui.theme.LocalGlass

private const val PanelMs = 240
private const val FadeMs = 200
private const val OverlayMs = 220

/** iOS Cupertino deceleration’a yakın eğri. */
private val IosEasing = CubicBezierEasing(0.32f, 0.72f, 0f, 1f)
private val SoftEasing = CubicBezierEasing(0.4f, 0f, 0.2f, 1f)

@Composable
private fun animMs(normal: Int): Int =
    if (LocalGlass.current.reduceEffects) 0 else normal

/** Rail / ekran / kategori geçişi: solma + çok hafif yatay kayma. */
@Composable
fun rayPanelEnter(): EnterTransition {
    val ms = animMs(PanelMs)
    val spec = tween<Float>(ms, easing = FastOutSlowInEasing)
    val slide = tween<IntOffset>(ms, easing = FastOutSlowInEasing)
    return fadeIn(spec) + slideInHorizontally(slide) { w -> (w / 32).coerceAtLeast(8) }
}

@Composable
fun rayPanelExit(): ExitTransition {
    val ms = animMs(160)
    return fadeOut(tween(ms, easing = FastOutSlowInEasing)) +
        slideOutHorizontally(tween<IntOffset>(ms, easing = FastOutSlowInEasing)) { w -> -(w / 48).coerceAtLeast(6) }
}

@Composable
fun rayOverlayEnter(): EnterTransition {
    val ms = animMs(OverlayMs)
    return fadeIn(tween(ms, easing = FastOutSlowInEasing)) +
        scaleIn(tween(ms, easing = FastOutSlowInEasing), initialScale = 0.98f)
}

@Composable
fun rayOverlayExit(): ExitTransition {
    val ms = animMs(160)
    return fadeOut(tween(ms, easing = FastOutSlowInEasing)) +
        scaleOut(tween(ms, easing = FastOutSlowInEasing), targetScale = 0.98f)
}

@Composable
fun rayFocusTween() = tween<Float>(animMs(180), easing = FastOutSlowInEasing)

@Composable
fun rayRailEnter(): EnterTransition {
    val ms = animMs(240)
    return expandHorizontally(tween<IntSize>(ms, easing = FastOutSlowInEasing), expandFrom = Alignment.Start) +
        fadeIn(tween(animMs(200), easing = FastOutSlowInEasing))
}

@Composable
fun rayRailExit(): ExitTransition {
    val ms = animMs(200)
    return shrinkHorizontally(tween<IntSize>(ms, easing = FastOutSlowInEasing), shrinkTowards = Alignment.Start) +
        fadeOut(tween(ms, easing = FastOutSlowInEasing))
}

/** Bölüm / sayfa değişimi. */
@Composable
fun <S> RaySwitch(
    targetState: S,
    modifier: Modifier = Modifier,
    effect: PageTransitionEffect? = null,
    content: @Composable (S) -> Unit
) {
    val reduce = LocalGlass.current.reduceEffects
    val enter = rayPanelEnter()
    val exit = rayPanelExit()
    val clipPages = effect == PageTransitionEffect.IOS
    AnimatedContent(
        targetState = targetState,
        modifier = modifier.then(if (clipPages) Modifier.clipToBounds() else Modifier),
        contentAlignment = Alignment.TopStart,
        transitionSpec = {
            val transform = if (effect == null || reduce) {
                enter togetherWith exit
            } else {
                pageTransition(effect, isPop(initialState, targetState))
            }
            transform.using(SizeTransform(clip = clipPages) { _, _ -> snap() })
        },
        label = "ray-switch"
    ) { state ->
        Box(Modifier.fillMaxSize()) {
            if (effect == PageTransitionEffect.JELLY && !reduce) {
                JellyEnterLayer { content(state) }
            } else {
                content(state)
            }
        }
    }
}

/**
 * Mina `DynamicPageTransition`:
 * - iOS: Cupertino sağdan kaydırma (geri = soldan)
 * - Yumuşak: fade + 0.96→1 scale
 * - Sallanan: elastic scale + hafif dönüş + alttan oturma
 */
private fun pageTransition(effect: PageTransitionEffect, pop: Boolean): ContentTransform {
    return when (effect) {
        PageTransitionEffect.IOS -> {
            val ms = 340
            val slide = tween<IntOffset>(ms, easing = IosEasing)
            val fade = tween<Float>(ms, easing = IosEasing)
            val enter = if (pop) {
                slideInHorizontally(slide) { w -> -w } + fadeIn(fade)
            } else {
                slideInHorizontally(slide) { w -> w } + fadeIn(fade)
            }
            val exit = if (pop) {
                slideOutHorizontally(slide) { w -> w / 3 } + fadeOut(fade)
            } else {
                slideOutHorizontally(slide) { w -> -w / 3 } + fadeOut(fade)
            }
            (enter togetherWith exit).also { it.targetContentZIndex = 1f }
        }
        PageTransitionEffect.FADE_SCALE -> {
            val ms = 300
            val spec = tween<Float>(ms, easing = SoftEasing)
            (fadeIn(spec) + scaleIn(spec, initialScale = 0.96f, transformOrigin = TransformOrigin.Center) togetherWith
                fadeOut(tween(220, easing = SoftEasing)))
                .also { it.targetContentZIndex = 1f }
        }
        PageTransitionEffect.JELLY -> {
            // Giriş `JellyEnterLayer` içinde (elastic + rotasyon). Eski sayfa kısa fade.
            (EnterTransition.None togetherWith fadeOut(tween(160, easing = FastOutSlowInEasing)))
                .also { it.targetContentZIndex = 1f }
        }
    }
}

@Composable
private fun JellyEnterLayer(content: @Composable () -> Unit) {
    val progress = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        progress.animateTo(
            1f,
            spring(
                dampingRatio = 0.42f,
                stiffness = 170f,
                visibilityThreshold = 0.001f
            )
        )
    }
    val t = progress.value
    Box(
        Modifier
            .fillMaxSize()
            .graphicsLayer {
                val s = 0.85f + 0.15f * t
                scaleX = s
                scaleY = s
                rotationZ = -10.8f * (1f - t)
                translationY = size.height * 0.10f * (1f - t)
                alpha = t.coerceIn(0f, 1f)
                transformOrigin = TransformOrigin.Center
            }
    ) {
        content()
    }
}

private fun isPop(from: Any?, to: Any?): Boolean = pageRank(to) < pageRank(from)

private fun pageRank(v: Any?): Int = when (v) {
    Dest.CONTINUE -> 0
    Dest.LIVE, Dest.MOVIES, Dest.SERIES, Dest.PLAYLISTS, Dest.WRAPPED, Dest.EPG_MIX -> 1
    Dest.SETTINGS, Dest.CHAT, Dest.ADMIN -> 2
    Dest.PLAYER -> 3
    is Enum<*> -> v.ordinal
    else -> 1
}

/** Poster / özet / kanal bilgisi değişimi — yalnız solma. */
@Composable
fun <S> RayCrossfade(
    targetState: S,
    modifier: Modifier = Modifier,
    content: @Composable (S) -> Unit
) {
    val msIn = animMs(FadeMs)
    val msOut = animMs(140)
    AnimatedContent(
        targetState = targetState,
        modifier = modifier,
        transitionSpec = {
            fadeIn(tween(msIn, easing = FastOutSlowInEasing)) togetherWith
                fadeOut(tween(msOut, easing = FastOutSlowInEasing))
        },
        label = "ray-crossfade"
    ) { state ->
        content(state)
    }
}

@Composable
fun RayOverlay(
    visible: Boolean,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    AnimatedVisibility(
        visible = visible,
        modifier = modifier,
        enter = rayOverlayEnter(),
        exit = rayOverlayExit()
    ) {
        content()
    }
}
