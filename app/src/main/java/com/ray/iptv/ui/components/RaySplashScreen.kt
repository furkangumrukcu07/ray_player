package com.ray.iptv.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
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
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
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
    style: SplashStyle = SplashStyle.DARK_GLASS_CARD,
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
            SplashStyle.DARK_GLASS_CARD -> Color(0xFF64D2FF)
            SplashStyle.MINIMAL_NEON_PULSE -> Color(0xFF00F0FF)
            SplashStyle.GLASS_CAPSULE_PROGRESS -> Color(0xFF4EECD2)
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
    val pulseRingScale by infiniteTransition.animateFloat(
        initialValue = 1.0f,
        targetValue = 1.45f,
        animationSpec = infiniteRepeatable(
            animation = tween(1600, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "ring-scale"
    )
    val pulseRingAlpha by infiniteTransition.animateFloat(
        initialValue = 0.65f,
        targetValue = 0.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1600, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "ring-alpha"
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

    val progressTarget = when (currentMessageIndex) {
        0 -> 0.25f
        1 -> 0.55f
        2 -> 0.85f
        else -> 1.0f
    }
    val animatedProgress by animateFloatAsState(
        targetValue = progressTarget,
        animationSpec = tween(550, easing = LinearOutSlowInEasing),
        label = "splash-progress"
    )

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

        // 1. Common Natural Blurred Leaf Wallpaper for all 3 styles
        Image(
            painter = painterResource(id = R.drawable.splash_nature_bg),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )

        // 2. Subtle Vignette Layer for cinematic depth
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            Color.Transparent,
                            Color(0x35000000),
                            Color(0x75090A0E)
                        )
                    )
                )
        )

        // 3. Concept Specific UI Presentation
        when (style) {
            // ==========================================
            // KONSEPT 1: DARK GLASS CARD (VARSAYILAN)
            // ==========================================
            SplashStyle.DARK_GLASS_CARD -> {
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
                            Box(
                                modifier = Modifier
                                    .size(auraSize)
                                    .scale(pulseScale * 1.15f)
                                    .clip(CircleShape)
                                    .background(accentColor.copy(alpha = glowAlpha * 0.45f))
                                    .blur(18.dp)
                            )
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

                        Text(
                            text = "RAY PLAYER",
                            color = Color.White,
                            fontSize = titleSize,
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = 1.5.sp
                        )

                        Spacer(Modifier.height(if (isCompactHeight) 2.dp else 3.dp))

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
                                Box(
                                    modifier = Modifier
                                        .size(if (isCompactHeight) 6.dp else 8.dp)
                                        .scale(pulseScale)
                                        .clip(CircleShape)
                                        .background(accentColor)
                                )
                                Spacer(Modifier.width(if (isCompactHeight) 7.dp else 10.dp))
                                AnimatedContent(
                                    targetState = loadingMessages[currentMessageIndex],
                                    transitionSpec = {
                                        fadeIn(animationSpec = tween(220, easing = FastOutSlowInEasing)) togetherWith
                                            fadeOut(animationSpec = tween(180, easing = FastOutSlowInEasing))
                                    },
                                    label = "splash-msg-1"
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
            }

            // ==========================================
            // KONSEPT 2: MINIMALIST NEON (KARTSIZ / SERBEST)
            // ==========================================
            SplashStyle.MINIMAL_NEON_PULSE -> {
                Column(
                    modifier = Modifier.padding(horizontal = 24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    // Floating App Logo with Expanding Concentric Neon Rings
                    Box(
                        modifier = Modifier.size(logoBoxSize * 1.3f),
                        contentAlignment = Alignment.Center
                    ) {
                        // Expanding Ring 1
                        Box(
                            modifier = Modifier
                                .size(auraSize * 1.25f)
                                .scale(pulseRingScale)
                                .clip(CircleShape)
                                .border(1.5.dp, accentColor.copy(alpha = pulseRingAlpha), CircleShape)
                        )
                        // Glowing Aura
                        Box(
                            modifier = Modifier
                                .size(auraSize)
                                .scale(pulseScale * 1.2f)
                                .clip(CircleShape)
                                .background(accentColor.copy(alpha = glowAlpha * 0.55f))
                                .blur(22.dp)
                        )
                        // App Icon
                        Image(
                            painter = painterResource(id = R.drawable.ic_ray_splash_logo),
                            contentDescription = null,
                            modifier = Modifier
                                .size(logoIconSize * 1.1f)
                                .scale(pulseScale)
                                .clip(RoundedCornerShape(if (isCompactHeight) 16.dp else 24.dp))
                        )
                    }

                    Spacer(Modifier.height(if (isCompactHeight) 10.dp else 18.dp))

                    Text(
                        text = "RAY PLAYER",
                        color = Color.White,
                        fontSize = (titleSize.value * 1.15f).sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 3.sp
                    )

                    Spacer(Modifier.height(if (isCompactHeight) 3.dp else 4.dp))

                    Text(
                        text = if (tr) "GELİŞMİŞ MEDYA MOTORU" else "NEXT-GEN MEDIA ENGINE",
                        color = accentColor.copy(alpha = 0.90f),
                        fontSize = subtitleSize,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 2.5.sp
                    )

                    Spacer(Modifier.height(if (isCompactHeight) 16.dp else 24.dp))

                    // Minimal Typography Status (No Card Box)
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(if (isCompactHeight) 6.dp else 8.dp)
                                .scale(pulseScale)
                                .clip(CircleShape)
                                .background(accentColor)
                        )
                        Spacer(Modifier.width(10.dp))
                        AnimatedContent(
                            targetState = loadingMessages[currentMessageIndex],
                            transitionSpec = {
                                fadeIn(animationSpec = tween(220, easing = FastOutSlowInEasing)) togetherWith
                                    fadeOut(animationSpec = tween(180, easing = FastOutSlowInEasing))
                            },
                            label = "splash-msg-2"
                        ) { msg ->
                            Text(
                                text = msg,
                                color = Color.White.copy(alpha = 0.95f),
                                fontSize = messageSize,
                                fontWeight = FontWeight.SemiBold,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }

            // ==========================================
            // KONSEPT 3: GLASS CAPSULE & GLOW PROGRESS
            // ==========================================
            SplashStyle.GLASS_CAPSULE_PROGRESS -> {
                Column(
                    modifier = Modifier.padding(horizontal = 24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    // Floating App Logo
                    Box(
                        modifier = Modifier.size(logoBoxSize),
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(auraSize)
                                .scale(pulseScale * 1.15f)
                                .clip(CircleShape)
                                .background(accentColor.copy(alpha = glowAlpha * 0.50f))
                                .blur(20.dp)
                        )
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

                    Text(
                        text = "RAY PLAYER",
                        color = Color.White,
                        fontSize = titleSize,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 2.sp
                    )

                    Spacer(Modifier.height(if (isCompactHeight) 2.dp else 3.dp))

                    Text(
                        text = if (tr) "GELİŞMİŞ MEDYA MOTORU" else "NEXT-GEN MEDIA ENGINE",
                        color = accentColor.copy(alpha = 0.85f),
                        fontSize = subtitleSize,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 2.sp
                    )

                    Spacer(Modifier.height(if (isCompactHeight) 14.dp else 22.dp))

                    // Wide Frosted Glass Capsule with Integrated Glow Progress Bar
                    Box(
                        modifier = Modifier
                            .widthIn(min = if (isCompactHeight) 240.dp else 290.dp, max = 420.dp)
                            .clip(RoundedCornerShape(26.dp))
                            .background(Color(0xE010141E))
                            .border(
                                width = 1.2.dp,
                                brush = Brush.horizontalGradient(
                                    listOf(
                                        accentColor.copy(alpha = 0.60f),
                                        Color(0x33FFFFFF),
                                        accentColor.copy(alpha = 0.35f)
                                    )
                                ),
                                shape = RoundedCornerShape(26.dp)
                            )
                    ) {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Row(
                                modifier = Modifier
                                    .padding(horizontal = pillPaddingH + 4.dp, vertical = pillPaddingV + 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(if (isCompactHeight) 7.dp else 9.dp)
                                        .scale(pulseScale)
                                        .clip(CircleShape)
                                        .background(accentColor)
                                )
                                Spacer(Modifier.width(10.dp))
                                AnimatedContent(
                                    targetState = loadingMessages[currentMessageIndex],
                                    transitionSpec = {
                                        fadeIn(animationSpec = tween(220, easing = FastOutSlowInEasing)) togetherWith
                                            fadeOut(animationSpec = tween(180, easing = FastOutSlowInEasing))
                                    },
                                    label = "splash-msg-3"
                                ) { msg ->
                                    Text(
                                        text = msg,
                                        color = Color.White.copy(alpha = 0.95f),
                                        fontSize = messageSize,
                                        fontWeight = FontWeight.Medium,
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }

                            // Glowing Progress Line at the bottom of the capsule
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(3.dp)
                                    .background(Color.White.copy(alpha = 0.08f))
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth(animatedProgress)
                                        .fillMaxHeight()
                                        .background(
                                            Brush.horizontalGradient(
                                                listOf(
                                                    accentColor.copy(alpha = 0.40f),
                                                    accentColor,
                                                    Color.White
                                                )
                                            )
                                        )
                                )
                            }
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
