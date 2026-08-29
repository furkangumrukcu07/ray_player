package com.ray.iptv.ui.glass

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.ray.iptv.data.repo.GlassStyle
import com.ray.iptv.ui.input.rayClickable
import com.ray.iptv.ui.motion.rayFocusTween
import com.ray.iptv.ui.theme.DarkGlassPopup
import com.ray.iptv.ui.theme.LocalGlass
import com.ray.iptv.ui.theme.wallpaperScrim

@Composable
fun RayWallpaper(
    modifier: Modifier = Modifier,
    overlayTop: Float = 0.22f,
    overlayBottom: Float = 0.48f
) {
    val g = LocalGlass.current
    val hasArt = g.wallpaperRes != 0
    val (scrimTop, scrimBottom) = g.wallpaperScrim()
    val top = if (hasArt) overlayTop else scrimTop
    val bottom = if (hasArt) overlayBottom else scrimBottom

    Box(
        modifier
            .fillMaxSize()
            .background(g.wallpaperDark)
    ) {
        if (hasArt) {
            AsyncImage(
                model = g.wallpaperRes,
                contentDescription = null,
                modifier = Modifier
                    .fillMaxSize()
                    .then(if (g.reduceEffects) Modifier.alpha(0.38f) else Modifier),
                contentScale = ContentScale.Crop
            )
        }
        if (g.reduceEffects) {
            Box(
                Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.60f))
            )
        } else if (!g.flatWallpaper) {
            Box(
                Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            listOf(
                                Color.Black.copy(alpha = top),
                                Color.Black.copy(alpha = bottom)
                            )
                        )
                    )
            )
        }
    }
}


@Composable
fun GlassPanel(
    modifier: Modifier = Modifier,
    radius: Dp = 12.dp,
    strong: Boolean = false,
    focused: Boolean = false,
    accentFill: Boolean = false,
    scaleOnFocus: Boolean = true,
    fillAlpha: Float = 1f,
    onClick: (() -> Unit)? = null,
    onLongClick: (() -> Unit)? = null,
    content: @Composable BoxScope.() -> Unit
) {
    val g = LocalGlass.current
    val a = fillAlpha.coerceIn(0.12f, 1f)
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val targetScale = when {
        isPressed && scaleOnFocus -> 0.965f
        focused && scaleOnFocus && !g.reduceEffects -> 1.025f
        else -> 1f
    }
    val scale = if (scaleOnFocus && !g.reduceEffects) {
        val s by animateFloatAsState(
            targetValue = targetScale,
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessLow
            ),
            label = "glass-tactile-scale"
        )
        s
    } else 1f

    val click = if (onClick != null) {
        Modifier.rayClickable(onClick, onLongClick, interactionSource = interactionSource)
    } else Modifier

    val shape = RoundedCornerShape(radius)
    val basePanel = if (strong) g.panelStrong else g.panel
    val baseColor = basePanel.copy(alpha = basePanel.alpha * a)

    val fill = when {
        accentFill -> Brush.verticalGradient(
            listOf(g.accent.copy(alpha = 0.48f * a), g.accent.copy(alpha = 0.22f * a))
        )
        g.flatWallpaper || g.reduceEffects -> SolidColor(baseColor)
        g.frostDark -> Brush.verticalGradient(
            listOf(
                Color.White.copy(alpha = (if (strong) 0.08f else 0.04f) * a),
                Color.White.copy(alpha = (if (strong) 0.015f else 0.005f) * a)
            )
        )
        else -> Brush.verticalGradient(
            listOf(
                Color.White.copy(alpha = (if (g.isLight) 0.72f else if (strong) 0.12f else 0.06f) * a),
                Color.White.copy(alpha = (if (g.isLight) 0.38f else if (strong) 0.02f else 0.01f) * a)
            )
        )
    }

    val edge = if (focused) {
        SolidColor(g.strokeFocus)
    } else if (g.reduceEffects) {
        SolidColor(Color.White.copy(alpha = if (g.isLight) 0.25f else 0.08f))
    } else {
        Brush.verticalGradient(
            listOf(
                Color.White.copy(alpha = if (g.isLight) 0.50f else 0.22f),
                Color.White.copy(alpha = if (g.isLight) 0.15f else 0.05f)
            )
        )
    }

    val boxModifier = modifier
        .then(click)
        .then(if (scale != 1f) Modifier.scale(scale) else Modifier)
        .clip(shape)
        .background(baseColor)
        .then(if (fill !is SolidColor) Modifier.background(fill) else Modifier)
        .then(if (isPressed) Modifier.background(Color.White.copy(alpha = 0.08f)) else Modifier)
        .border(width = if (focused) 1.5.dp else 1.dp, brush = edge, shape = shape)

    Box(
        modifier = boxModifier,
        content = content
    )
}

/**
 * Wraps popup and dialog windows in the Apple TV / Mac Dark Glass theme:
 * Semi-transparent dark anthracite (#10131B 40%), cyan neon (#64D2FF) accent, and crisp white text.
 * Applies a signature Apple TV / iOS tactile fluid spring entrance animation (scale + subtle float-up + fade).
 */
@Composable
fun DarkGlassPopupTheme(content: @Composable () -> Unit) {
    CompositionLocalProvider(LocalGlass provides DarkGlassPopup) {
        var entered by remember { mutableStateOf(false) }
        LaunchedEffect(Unit) {
            entered = true
        }

        val scale by animateFloatAsState(
            targetValue = if (entered) 1f else 0.88f,
            animationSpec = spring(
                dampingRatio = 0.72f,
                stiffness = Spring.StiffnessMediumLow
            ),
            label = "glass-popup-scale"
        )
        val alpha by animateFloatAsState(
            targetValue = if (entered) 1f else 0f,
            animationSpec = tween(durationMillis = 220, easing = FastOutSlowInEasing),
            label = "glass-popup-alpha"
        )
        val translationY by animateFloatAsState(
            targetValue = if (entered) 0f else 32f,
            animationSpec = spring(
                dampingRatio = 0.74f,
                stiffness = Spring.StiffnessMediumLow
            ),
            label = "glass-popup-y"
        )

        Box(
            modifier = Modifier.graphicsLayer {
                this.scaleX = scale
                this.scaleY = scale
                this.alpha = alpha
                this.translationY = translationY
            }
        ) {
            content()
        }
    }
}
