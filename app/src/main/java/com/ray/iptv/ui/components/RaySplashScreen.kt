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
import kotlinx.coroutines.delay

@Composable
fun RaySplashScreen(
    tr: Boolean,
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
            delay(650)
        }
        delay(350)
        isClosing = true
        delay(300)
        onFinished()
    }

    val screenAlpha by animateFloatAsState(
        targetValue = if (isClosing) 0f else 1f,
        animationSpec = tween(300, easing = FastOutSlowInEasing),
        label = "splash-fade-out"
    )
    val screenScale by animateFloatAsState(
        targetValue = if (isClosing) 1.04f else 1f,
        animationSpec = tween(300, easing = FastOutSlowInEasing),
        label = "splash-scale-out"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .graphicsLayer {
                alpha = screenAlpha
                scaleX = screenScale
                scaleY = screenScale
            },
        contentAlignment = Alignment.Center
    ) {
        // Blurred Nature Background
        Image(
            painter = painterResource(id = R.drawable.splash_nature_bg),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )

        // Dark Smoked Glass Vignette Layer
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            Color(0x9910131B),
                            Color(0xE6090A0E)
                        )
                    )
                )
        )

        // Center Apple Dark Glass Card
        Box(
            modifier = Modifier
                .padding(horizontal = 24.dp)
                .clip(RoundedCornerShape(32.dp))
                .background(Color(0xCC10131B))
                .border(
                    width = 1.5.dp,
                    brush = Brush.linearGradient(
                        listOf(
                            Color(0x8064D2FF),
                            Color(0x22FFFFFF),
                            Color(0x4064D2FF)
                        )
                    ),
                    shape = RoundedCornerShape(32.dp)
                )
                .padding(horizontal = 32.dp, vertical = 36.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                // Pulsing Halo Glow & App Icon
                Box(
                    modifier = Modifier.size(130.dp),
                    contentAlignment = Alignment.Center
                ) {
                    // Glowing cyan neon aura
                    Box(
                        modifier = Modifier
                            .size(120.dp)
                            .scale(pulseScale * 1.15f)
                            .clip(CircleShape)
                            .background(Color(0xFF64D2FF).copy(alpha = glowAlpha * 0.45f))
                            .blur(18.dp)
                    )

                    // App Icon with breathing pulse
                    Image(
                        painter = painterResource(id = R.mipmap.ic_launcher),
                        contentDescription = null,
                        modifier = Modifier
                            .size(90.dp)
                            .scale(pulseScale)
                            .clip(RoundedCornerShape(20.dp))
                    )
                }

                Spacer(Modifier.height(16.dp))

                // Title: RAY PLAYER
                Text(
                    text = "RAY PLAYER",
                    color = Color.White,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 1.5.sp
                )

                Spacer(Modifier.height(4.dp))

                // Subtitle
                Text(
                    text = if (tr) "GELİŞMİŞ MEDYA MOTORU" else "NEXT-GEN MEDIA ENGINE",
                    color = Color(0xFF64D2FF).copy(alpha = 0.85f),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 2.sp
                )

                Spacer(Modifier.height(24.dp))

                // Dynamic Status Loading Pill
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(24.dp))
                        .background(Color(0xFF141822).copy(alpha = 0.85f))
                        .border(1.dp, Color(0xFF64D2FF).copy(alpha = 0.35f), RoundedCornerShape(24.dp))
                        .padding(horizontal = 18.dp, vertical = 10.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        // Pulsing cyan neon indicator dot
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .scale(pulseScale)
                                .clip(CircleShape)
                                .background(Color(0xFF64D2FF))
                        )

                        Spacer(Modifier.width(10.dp))

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
                                fontSize = 13.sp,
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
            text = "v1.3.15",
            color = Color.White.copy(alpha = 0.40f),
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 24.dp)
        )
    }
}
