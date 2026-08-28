package com.ray.iptv.ui.screens.settings

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Dns
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Router
import androidx.compose.material.icons.filled.Tv
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Icon
import androidx.tv.material3.Text
import com.ray.iptv.net.SpeedTestState
import com.ray.iptv.net.SpeedTestStatus
import com.ray.iptv.ui.input.rayClickable
import com.ray.iptv.ui.theme.LocalGlass

@Composable
fun SpeedTestScreen(
    tr: Boolean,
    state: SpeedTestState,
    onStartTest: () -> Unit,
    onStopTest: () -> Unit,
    onBack: () -> Unit
) {
    val isRunning = state.status == SpeedTestStatus.RUNNING
    val isDone = state.status == SpeedTestStatus.COMPLETED

    val animProgress by animateFloatAsState(
        targetValue = state.progress,
        animationSpec = tween(durationMillis = 300),
        label = "speedProgress"
    )

    Column(
        Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .verticalScroll(rememberScrollState())
    ) {
        // Top Bar
        Row(
            Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.12f))
                    .border(1.dp, Color.White.copy(alpha = 0.22f), CircleShape)
                    .rayClickable(onClick = onBack),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(22.dp)
                )
            }
            Spacer(Modifier.width(14.dp))
            Column {
                Text(
                    text = if (tr) "İnternet & IPTV Hız Testi" else "Speed & IPTV Latency Test",
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = if (tr) "IPTV sunucu gecikmesi ve 4K yayın uygunluğu analizi" else "IPTV latency & stream quality analysis",
                    color = Color(0xFF22D3EE),
                    fontSize = 11.5.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }

        Spacer(Modifier.height(24.dp))

        // Speedometer Center Card
        Box(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(24.dp))
                .background(Color(0xFF16382B).copy(alpha = 0.38f))
                .border(1.dp, Color(0xFF34D399).copy(alpha = 0.25f), RoundedCornerShape(24.dp))
                .padding(vertical = 32.dp, horizontal = 20.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Box(
                    Modifier.size(160.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Canvas(Modifier.fillMaxSize()) {
                        val stroke = 12.dp.toPx()
                        drawArc(
                            color = Color.White.copy(alpha = 0.08f),
                            startAngle = 135f,
                            sweepAngle = 270f,
                            useCenter = false,
                            style = Stroke(stroke, cap = StrokeCap.Round)
                        )
                        val sweep = (if (isRunning) animProgress else if (isDone) 1f else 0f) * 270f
                        drawArc(
                            color = Color(0xFF22D3EE),
                            startAngle = 135f,
                            sweepAngle = sweep,
                            useCenter = false,
                            style = Stroke(stroke, cap = StrokeCap.Round)
                        )
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "%.1f".format(state.downloadSpeedMbps),
                            color = Color.White,
                            fontSize = 38.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = (-1).sp
                        )
                        Text(
                            text = "Mbps",
                            color = Color(0xFF22D3EE),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Spacer(Modifier.height(20.dp))

                // Test Action Button
                Box(
                    Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(if (isRunning) Color(0xFFEF4444) else Color.White)
                        .rayClickable(onClick = { if (isRunning) onStopTest() else onStartTest() })
                        .padding(horizontal = 24.dp, vertical = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            if (isRunning) Icons.Filled.Refresh else Icons.Filled.PlayArrow,
                            contentDescription = null,
                            tint = if (isRunning) Color.White else Color(0xFF0F172A),
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = if (isRunning) {
                                if (tr) "Testi Durdur" else "Stop Test"
                            } else if (isDone) {
                                if (tr) "Yeniden Test Et" else "Test Again"
                            } else {
                                if (tr) "Testi Başlat" else "Start Speed Test"
                            },
                            color = if (isRunning) Color.White else Color(0xFF0F172A),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(16.dp))

        // Latency Metrics Cards (General Ping vs IPTV Server Ping)
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // General Network Ping
            Box(
                Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(18.dp))
                    .background(Color(0xFF16382B).copy(alpha = 0.30f))
                    .border(1.dp, Color(0xFF34D399).copy(alpha = 0.20f), RoundedCornerShape(18.dp))
                    .padding(14.dp)
            ) {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.Router, null, tint = Color(0xFF22D3EE), modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(
                            if (tr) "Genel Ping" else "Latency",
                            color = Color.White.copy(alpha = 0.70f),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = if (state.generalLatencyMs > 0) "${state.generalLatencyMs} ms" else "-- ms",
                        color = Color.White,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                }
            }

            // Active IPTV Server Ping
            Box(
                Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(18.dp))
                    .background(Color(0xFF16382B).copy(alpha = 0.30f))
                    .border(1.dp, Color(0xFF34D399).copy(alpha = 0.20f), RoundedCornerShape(18.dp))
                    .padding(14.dp)
            ) {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.Dns, null, tint = Color(0xFFF59E0B), modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(
                            if (tr) "IPTV Sunucu Ping" else "IPTV Ping",
                            color = Color.White.copy(alpha = 0.70f),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = if (state.iptvServerLatencyMs != null) "${state.iptvServerLatencyMs} ms" else "-- ms",
                        color = Color(state.iptvServerHealth.second),
                        fontSize = 20.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                }
            }
        }

        Spacer(Modifier.height(16.dp))

        // Analysis Cards (Recommended Quality + IPTV Server Health)
        Column(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .background(Color(0xFF16382B).copy(alpha = 0.38f))
                .border(1.dp, Color(0xFF34D399).copy(alpha = 0.25f), RoundedCornerShape(20.dp))
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Quality Recommendation
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.Tv, null, tint = Color(0xFF34D399), modifier = Modifier.size(24.dp))
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        text = if (tr) "Önerilen Yayın Çözünürlüğü" else "Recommended Stream Quality",
                        color = Color.White.copy(alpha = 0.70f),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = state.recommendedQuality,
                        color = Color(0xFF22D3EE),
                        fontSize = 15.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                }
            }

            // IPTV Server Status Message
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Filled.CheckCircle,
                    null,
                    tint = Color(state.iptvServerHealth.second),
                    modifier = Modifier.size(24.dp)
                )
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        text = if (tr) "IPTV Sunucu Durumu & Donma Riski" else "IPTV Server Health",
                        color = Color.White.copy(alpha = 0.70f),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = state.iptvServerHealth.first,
                        color = Color.White,
                        fontSize = 13.5.sp,
                        fontWeight = FontWeight.Bold
                    )
                    if (state.iptvServerHost != null) {
                        Text(
                            text = "Sunucu: ${state.iptvServerHost}",
                            color = Color.White.copy(alpha = 0.45f),
                            fontSize = 11.sp
                        )
                    }
                }
            }
        }
    }
}
