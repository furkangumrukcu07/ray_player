package com.ray.iptv.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.tv.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ray.iptv.R
import com.ray.iptv.data.repo.SplashStyle
import kotlinx.coroutines.delay

@Composable
fun RaySplashScreen(
    tr: Boolean,
    style: SplashStyle = SplashStyle.NATURE_GLASS,
    onFinished: () -> Unit
) {
    val loadingMessages = remember(tr) {
        if (tr) {
            listOf(
                "Kanal listesi hazırlanıyor...",
                "EPG TV rehberi eşitleniyor...",
                "Filmler ve diziler yükleniyor...",
                "Medya motoru başlatılıyor..."
            )
        } else {
            listOf(
                "Preparing channel list...",
                "Synchronizing EPG TV guide...",
                "Loading movies and series...",
                "Starting media engine..."
            )
        }
    }

    var currentMessageIndex by remember { mutableIntStateOf(0) }
    var isClosing by remember { mutableStateOf(false) }

    // Style-specific accent color
    val accentColor = remember(style) {
        when (style) {
            SplashStyle.NATURE_GLASS -> Color(0xFF64D2FF)
            SplashStyle.CYBER_CINEMA -> Color(0xFF00F0FF)
            SplashStyle.OBSIDIAN_AURORA -> Color(0xFF4EECD2)
        }
    }

    // Pulse animation for the app icon
    val infiniteTransition = rememberInfiniteTransition(label = "splash-pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.94f,
        targetValue = 1.06f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "icon-scale"
    )
    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.25f,
        targetValue = 0.70f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "halo-glow"
    )

    // Message rotation
    LaunchedEffect(Unit) {
        val totalSteps = loadingMessages.size
        for (i in 0 until totalSteps) {
            currentMessageIndex = i
            delay(600)
        }
        delay(350)
        isClosing = true
        delay(350)
        onFinished()
    }

    val screenAlpha by animateFloatAsState(
        targetValue = if (isClosing) 0f else 1f,
        animationSpec = tween(350, easing = FastOutSlowInEasing),
        label = "splash-fade-out"
    )
    val screenScale by animateFloatAsState(
        targetValue = if (isClosing) 1.04f else 1f,
        animationSpec = tween(350, easing = FastOutSlowInEasing),
        label = "splash-scale-out"
    )

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .graphicsLayer {
                alpha = screenAlpha
                scaleX = screenScale
                scaleY = screenScale
            },
        contentAlignment = Alignment.Center
    ) {
        val isLandscape = maxWidth > maxHeight
        val isCompactHeight = maxHeight < 520.dp
        val isLargeScreen = maxWidth >= 900.dp || maxHeight >= 900.dp

        // Dynamic responsive dimensions
        val logoBoxSize = when {
            isCompactHeight -> 72.dp
            isLargeScreen -> 140.dp
            else -> 110.dp
        }
        val logoIconSize = when {
            isCompactHeight -> 52.dp
            isLargeScreen -> 100.dp
            else -> 80.dp
        }
        val auraSize = when {
            isCompactHeight -> 68.dp
            isLargeScreen -> 130.dp
            else -> 100.dp
        }
        val cardPaddingH = when {
            isCompactHeight -> 24.dp
            isLargeScreen -> 48.dp
            else -> 32.dp
        }
        val cardPaddingV = when {
            isCompactHeight -> 14.dp
            isLargeScreen -> 36.dp
            else -> 28.dp
        }
        val titleSize = when {
            isCompactHeight -> 18.sp
            isLargeScreen -> 26.sp
            else -> 22.sp
        }
        val subtitleSize = when {
            isCompactHeight -> 9.5.sp
            isLargeScreen -> 12.5.sp
            else -> 11.sp
        }
        val pillPaddingH = when {
            isCompactHeight -> 14.dp
            isLargeScreen -> 22.dp
            else -> 18.dp
        }
        val pillPaddingV = when {
            isCompactHeight -> 6.dp
            isLargeScreen -> 11.dp
            else -> 9.dp
        }
        val messageSize = when {
            isCompactHeight -> 11.5.sp
            isLargeScreen -> 14.5.sp
            else -> 13.sp
        }
        val footerPaddingBottom = when {
            isCompactHeight -> 8.dp
            isLargeScreen -> 28.dp
            else -> 20.dp
        }

        // Style Background with ContentScale.Crop for all aspect ratios
        Image(
            painter = painterResource(id = style.drawableRes),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )

        // Subtle Vignette Layer for Depth
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            Color.Transparent,
                            Color(0x40000000),
                            Color(0x80090A0E)
                        )
                    )
                )
        )

        // Center Apple Dark Glass Card
        Box(
            modifier = Modifier
                .padding(horizontal = if (isCompactHeight) 16.dp else 24.dp)
                .clip(RoundedCornerShape(if (isCompactHeight) 22.dp else 30.dp))
                .background(Color(0xD910131B))
                .border(
                    width = 1.5.dp,
                    brush = Brush.linearGradient(
                        listOf(
                            accentColor.copy(alpha = 0.65f),
                            Color(0x22FFFFFF),
                            accentColor.copy(alpha = 0.35f)
                        )
                    ),
                    shape = RoundedCornerShape(if (isCompactHeight) 22.dp else 30.dp)
                )
                .padding(horizontal = cardPaddingH, vertical = cardPaddingV),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                // Pulsing Halo Glow & App Icon
                Box(
                    modifier = Modifier.size(logoBoxSize),
                    contentAlignment = Alignment.Center
                ) {
                    // Glowing neon aura
                    Box(
                        modifier = Modifier
                            .size(auraSize)
                            .scale(pulseScale * 1.15f)
                            .clip(CircleShape)
                            .background(accentColor.copy(alpha = glowAlpha * 0.45f))
                            .blur(18.dp)
                    )

                    // App Icon with breathing pulse
                    Image(
                        painter = painterResource(id = R.drawable.ic_ray_splash_logo),
                        contentDescription = null,
                        modifier = Modifier
                            .size(logoIconSize)
                            .scale(pulseScale)
                            .clip(RoundedCornerShape(if (isCompactHeight) 14.dp else 20.dp))
                    )
                }

                Spacer(Modifier.height(if (isCompactHeight) 8.dp else 14.dp))

                // Title: RAY PLAYER
                Text(
                    text = "RAY PLAYER",
                    color = Color.White,
                    fontSize = titleSize,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 1.5.sp
                )

                Spacer(Modifier.height(if (isCompactHeight) 2.dp else 3.dp))

                // Subtitle
                Text(
                    text = if (tr) "GELİŞMİŞ MEDYA MOTORU" else "NEXT-GEN MEDIA ENGINE",
                    color = accentColor.copy(alpha = 0.85f),
                    fontSize = subtitleSize,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 2.sp
                )

                Spacer(Modifier.height(if (isCompactHeight) 10.dp else 18.dp))

                // Dynamic Status Loading Pill
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(24.dp))
                        .background(Color(0xFF141822).copy(alpha = 0.88f))
                        .border(1.dp, accentColor.copy(alpha = 0.40f), RoundedCornerShape(24.dp))
                        .padding(horizontal = pillPaddingH, vertical = pillPaddingV)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        // Pulsing neon indicator dot
                        Box(
                            modifier = Modifier
                                .size(if (isCompactHeight) 6.dp else 8.dp)
                                .scale(pulseScale)
                                .clip(CircleShape)
                                .background(accentColor)
                        )

                        Spacer(Modifier.width(if (isCompactHeight) 7.dp else 10.dp))

                        // Dynamic text with animated crossfade
                        AnimatedContent(
                            targetState = loadingMessages[currentMessageIndex],
                            transitionSpec = {
                                fadeIn(animationSpec = tween(220, easing = FastOutSlowInEasing)) togetherWith
                                    fadeOut(animationSpec = tween(180, easing = FastOutSlowInEasing))
                            },
                            label = "splash-message-transition"
                        ) { msg ->
                            Text(
                                text = msg,
                                color = Color.White.copy(alpha = 0.92f),
                                fontSize = messageSize,
                                fontWeight = FontWeight.Medium,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }
        }

        // Bottom version footer
        Text(
            text = "v${com.ray.iptv.BuildConfig.VERSION_NAME}",
            color = Color.White.copy(alpha = 0.40f),
            fontSize = if (isCompactHeight) 9.5.sp else 11.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = footerPaddingBottom)
        )
    }
}
