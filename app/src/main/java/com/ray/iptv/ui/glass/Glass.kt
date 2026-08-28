package com.ray.iptv.ui.glass

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
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
import com.ray.iptv.ui.input.rayClickable
import com.ray.iptv.ui.motion.rayFocusTween
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
    val top = if (hasArt) minOf(overlayTop, scrimTop) else overlayTop
    val bottom = if (hasArt) minOf(overlayBottom, scrimBottom) else overlayBottom
    Box(
        modifier
            .fillMaxSize()
            .background(g.wallpaperDark)
    ) {
        if (g.wallpaperRes != 0) {
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
            // Düşük güç modunda: Tema duvar kağıdı korunur, GPU yükü için tek katmanlı hafif mat karartma uygulanır
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
    radius: Dp = 18.dp,
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
    val scale = if (focused && scaleOnFocus && !g.reduceEffects) {
        val s by animateFloatAsState(
            targetValue = 1.018f,
            animationSpec = rayFocusTween(),
            label = "glass-scale"
        )
        s
    } else 1f

    val click = if (onClick != null) Modifier.rayClickable(onClick, onLongClick) else Modifier
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
                Color.Black.copy(alpha = (if (strong) 0.16f else 0.08f) * a)
            )
        )
        else -> Brush.verticalGradient(
            listOf(
                Color.White.copy(alpha = (if (g.isLight) 0.72f else if (strong) 0.14f else 0.08f) * a),
                Color.White.copy(alpha = (if (g.isLight) 0.38f else if (strong) 0.04f else 0.02f) * a)
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
                Color.White.copy(alpha = if (g.isLight) 0.50f else if (g.frostDark) 0.32f else 0.22f),
                Color.White.copy(alpha = if (g.isLight) 0.15f else if (g.frostDark) 0.08f else 0.05f)
            )
        )
    }

    val boxModifier = modifier
        .then(click)
        .then(if (scale != 1f) Modifier.scale(scale) else Modifier)
        .then(
            if (focused && !g.reduceEffects) {
                Modifier.drawBehind {
                    val r = CornerRadius(radius.toPx(), radius.toPx())
                    drawRoundRect(
                        color = g.accent.copy(alpha = 0.22f),
                        topLeft = Offset(-2.dp.toPx(), -1.dp.toPx()),
                        size = size.copy(width = size.width + 4.dp.toPx(), height = size.height + 6.dp.toPx()),
                        cornerRadius = r,
                        style = Stroke(width = 6.dp.toPx())
                    )
                }
            } else Modifier
        )
        .clip(shape)
        .background(baseColor)
        .then(if (fill !is SolidColor) Modifier.background(fill) else Modifier)
        .border(width = if (focused) 1.5.dp else 1.dp, brush = edge, shape = shape)

    Box(
        modifier = boxModifier,
        content = content
    )
}
